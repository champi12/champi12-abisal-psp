#!/usr/bin/env python3
"""j2c - ahead-of-time translator from Java bytecode (CLDC/MIDP class files) to C.

Usage: j2c.py -o OUTDIR [--main-class NAME] input.jar|classdir ...

Every class found in the inputs becomes C code that links against the runtime in
runtime/ (object model, garbage collector, threads, natives). Methods declared
`native` in the inputs must be provided by hand-written C in the runtime.

The translation is a direct stack-to-variables mapping: each JVM operand stack
slot and local variable becomes a typed C local, branches become gotos, and
exceptions are propagated through a pending-exception flag that is checked
after every operation that can throw.
"""
import argparse
import hashlib
import os
import struct
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from classfile import ClassFile, parse_method_desc, ACC_STATIC  # noqa: E402

# ---------------------------------------------------------------------------
# name mangling


def mangle(s):
    out = []
    for ch in s:
        o = ord(ch)
        if o < 128 and ch.isalnum():
            out.append(ch)
        elif ch == '/':
            out.append('_')
        elif ch == '_':
            out.append('_1')
        elif ch == ';':
            out.append('_2')
        elif ch == '[':
            out.append('_3')
        else:
            out.append('_0%04x' % o)
    return ''.join(out)


def mangle_mname(name):
    if name == '<init>':
        return '_init'
    if name == '<clinit>':
        return '_clinit'
    return mangle(name)


def cls_id(name):
    return mangle(name)


def method_cname(cls, name, desc):
    args, ret = parse_method_desc(desc)
    return 'M_%s__%s__%s__%s' % (cls_id(cls), mangle_mname(name), mangle(''.join(args)), mangle(ret))


def kind_of(desc):
    c = desc[0]
    if c in 'ZBCSI':
        return 'i'
    if c == 'J':
        return 'j'
    if c == 'F':
        return 'f'
    if c == 'D':
        return 'd'
    if c in 'L[':
        return 'a'
    if c == 'V':
        return 'V'
    raise ValueError(desc)


CTYPE = {'i': 'int32_t', 'j': 'int64_t', 'f': 'float', 'd': 'double', 'a': 'JObject*', 'V': 'void'}
FIELD_CTYPE = {'Z': 'int8_t', 'B': 'int8_t', 'C': 'uint16_t', 'S': 'int16_t', 'I': 'int32_t',
               'J': 'int64_t', 'F': 'float', 'D': 'double', 'L': 'JObject*', '[': 'JObject*'}
DEFAULT_RET = {'i': '0', 'j': '0', 'f': '0', 'd': '0', 'a': 'NULL'}


def ret_stmt_default(rk):
    if rk == 'V':
        return 'return;'
    return 'return %s;' % DEFAULT_RET[rk]


# ---------------------------------------------------------------------------
# program model


class ClassInfo:
    def __init__(self, cf):
        self.cf = cf
        self.name = cf.name
        self.super_name = cf.super_name
        self.interfaces = cf.interfaces
        self.is_interface = cf.is_interface
        self.is_abstract = cf.is_abstract
        self.is_final = cf.is_final
        self.methods = {(m.name, m.desc): m for m in cf.methods}
        self.fields = {(f.name, f.desc): f for f in cf.fields}
        self.super = None
        self.subclasses = []
        self.vtable = []       # list of [name, desc, impl Method or None]
        self.vindex = {}       # (name, desc) -> index
        self.all_ifaces = []
        self.field_cname = {}  # (name, desc) -> member/global name


class Program:
    def __init__(self):
        self.classes = {}
        self.strings = {}       # tuple(utf16) -> index
        self.string_list = []
        self.iface_ids = {}     # (name, desc) -> id

    def add(self, cf):
        if cf.name in self.classes:
            return
        self.classes[cf.name] = ClassInfo(cf)

    def string_index(self, units):
        key = tuple(units)
        if key not in self.strings:
            self.strings[key] = len(self.string_list)
            self.string_list.append(key)
        return self.strings[key]

    def iface_id(self, name, desc):
        k = (name, desc)
        if k not in self.iface_ids:
            self.iface_ids[k] = len(self.iface_ids) + 1
        return self.iface_ids[k]

    # -- hierarchy ---------------------------------------------------------
    def link(self):
        for c in self.classes.values():
            if c.super_name:
                if c.super_name not in self.classes:
                    raise SystemExit('missing superclass %s of %s' % (c.super_name, c.name))
                c.super = self.classes[c.super_name]
                c.super.subclasses.append(c)
            for i in c.interfaces:
                if i not in self.classes:
                    raise SystemExit('missing interface %s of %s' % (i, c.name))
        order = []
        seen = set()

        def visit(c):
            if c.name in seen:
                return
            seen.add(c.name)
            if c.super:
                visit(c.super)
            for i in c.interfaces:
                visit(self.classes[i])
            order.append(c)
        for c in sorted(self.classes.values(), key=lambda x: x.name):
            visit(c)
        self.order = order
        for c in order:
            s = []
            if c.super:
                s.extend(c.super.all_ifaces)
            for i in c.interfaces:
                ic = self.classes[i]
                for x in [i] + ic.all_ifaces:
                    if x not in s:
                        s.append(x)
            c.all_ifaces = s
        for c in order:
            self.layout_fields(c)
            if not c.is_interface:
                self.build_vtable(c)

    def layout_fields(self, c):
        names = {}
        for f in c.cf.fields:
            names[f.name] = names.get(f.name, 0) + 1
        for f in c.cf.fields:
            base = mangle(f.name) if names[f.name] == 1 else '%s_%s' % (mangle(f.name), mangle(f.desc))
            if f.is_static:
                c.field_cname[(f.name, f.desc)] = 'SF_%s__%s' % (cls_id(c.name), base)
            else:
                c.field_cname[(f.name, f.desc)] = 'F_' + base

    def iface_methods(self, iname):
        out = []
        for x in [iname] + self.classes[iname].all_ifaces:
            for m in self.classes[x].cf.methods:
                if m.name != '<clinit>' and (m.name, m.desc) not in out:
                    out.append((m.name, m.desc))
        return out

    def build_vtable(self, c):
        vt = [list(e) for e in c.super.vtable] if c.super else []
        vi = dict(c.super.vindex) if c.super else {}
        for m in c.cf.methods:
            if m.is_static or m.name in ('<init>', '<clinit>') or m.is_private:
                continue
            k = (m.name, m.desc)
            impl = None if m.is_abstract else m
            if k in vi:
                vt[vi[k]][2] = impl
            else:
                vi[k] = len(vt)
                vt.append([m.name, m.desc, impl])
        # miranda methods: interface methods not declared anywhere in the chain
        for i in c.all_ifaces:
            for k in self.iface_methods(i):
                if k not in vi:
                    vi[k] = len(vt)
                    vt.append([k[0], k[1], None])
        c.vtable = vt
        c.vindex = vi

    # -- resolution ----------------------------------------------------------
    def find_field(self, cname, name, desc):
        c = self.classes.get(cname)
        while c:
            if (name, desc) in c.fields:
                return c, c.fields[(name, desc)]
            for i in c.all_ifaces:
                ic = self.classes[i]
                if (name, desc) in ic.fields:
                    return ic, ic.fields[(name, desc)]
            c = c.super
        raise SystemExit('unresolved field %s.%s %s' % (cname, name, desc))

    def find_method(self, cname, name, desc):
        c = self.classes.get(cname)
        if c is None:
            raise SystemExit('unresolved class %s (method %s%s)' % (cname, name, desc))
        k = (name, desc)
        cc = c
        while cc:
            if k in cc.methods:
                return cc.methods[k]
            cc = cc.super
        for i in c.all_ifaces:
            ic = self.classes[i]
            if k in ic.methods:
                return ic.methods[k]
        raise SystemExit('unresolved method %s.%s%s' % (cname, name, desc))

    def is_subclass(self, c, sup):
        while c:
            if c is sup:
                return True
            c = c.super
        return False

    def overridden_below(self, c, k):
        """True if some subclass of c provides a different implementation of k."""
        base = c.vtable[c.vindex[k]][2]
        stack = list(c.subclasses)
        while stack:
            s = stack.pop()
            if s.is_interface:
                continue
            if s.vtable[s.vindex[k]][2] is not base:
                return True
            stack.extend(s.subclasses)
        return False

    def needs_init(self, c):
        while c:
            if (('<clinit>', '()V') in c.methods) or any(
                    f.is_static and f.const is not None and f.desc[0] == 'L' for f in c.cf.fields):
                return True
            c = c.super
        return False


# ---------------------------------------------------------------------------
# bytecode decoding

OPS = {}


def _ops():
    names = """nop aconst_null iconst_m1 iconst_0 iconst_1 iconst_2 iconst_3 iconst_4 iconst_5
    lconst_0 lconst_1 fconst_0 fconst_1 fconst_2 dconst_0 dconst_1 bipush sipush ldc ldc_w ldc2_w
    iload lload fload dload aload iload_0 iload_1 iload_2 iload_3 lload_0 lload_1 lload_2 lload_3
    fload_0 fload_1 fload_2 fload_3 dload_0 dload_1 dload_2 dload_3 aload_0 aload_1 aload_2 aload_3
    iaload laload faload daload aaload baload caload saload istore lstore fstore dstore astore
    istore_0 istore_1 istore_2 istore_3 lstore_0 lstore_1 lstore_2 lstore_3 fstore_0 fstore_1
    fstore_2 fstore_3 dstore_0 dstore_1 dstore_2 dstore_3 astore_0 astore_1 astore_2 astore_3
    iastore lastore fastore dastore aastore bastore castore sastore pop pop2 dup dup_x1 dup_x2
    dup2 dup2_x1 dup2_x2 swap iadd ladd fadd dadd isub lsub fsub dsub imul lmul fmul dmul idiv
    ldiv fdiv ddiv irem lrem frem drem ineg lneg fneg dneg ishl lshl ishr lshr iushr lushr iand
    land ior lor ixor lxor iinc i2l i2f i2d l2i l2f l2d f2i f2l f2d d2i d2l d2f i2b i2c i2s lcmp
    fcmpl fcmpg dcmpl dcmpg ifeq ifne iflt ifge ifgt ifle if_icmpeq if_icmpne if_icmplt if_icmpge
    if_icmpgt if_icmple if_acmpeq if_acmpne goto jsr ret tableswitch lookupswitch ireturn lreturn
    freturn dreturn areturn return getstatic putstatic getfield putfield invokevirtual
    invokespecial invokestatic invokeinterface invokedynamic new newarray anewarray arraylength
    athrow checkcast instanceof monitorenter monitorexit wide multianewarray ifnull ifnonnull
    goto_w jsr_w""".split()
    for i, n in enumerate(names):
        OPS[i] = n


_ops()


class Insn:
    __slots__ = ('pc', 'op', 'name', 'args', 'length')

    def __init__(self, pc, op, args, length):
        self.pc = pc
        self.op = op
        self.name = OPS[op]
        self.args = args
        self.length = length


def s1(b, p):
    return struct.unpack_from('>b', b, p)[0]


def s2(b, p):
    return struct.unpack_from('>h', b, p)[0]


def u2(b, p):
    return struct.unpack_from('>H', b, p)[0]


def s4(b, p):
    return struct.unpack_from('>i', b, p)[0]


def decode(code):
    insns = []
    pc = 0
    n = len(code)
    while pc < n:
        op = code[pc]
        name = OPS.get(op)
        if name is None:
            raise SystemExit('bad opcode %d at %d' % (op, pc))
        args = ()
        ln = 1
        if name in ('bipush',):
            args = (s1(code, pc + 1),)
            ln = 2
        elif name in ('sipush',):
            args = (s2(code, pc + 1),)
            ln = 3
        elif name == 'ldc':
            args = (code[pc + 1],)
            ln = 2
        elif name in ('ldc_w', 'ldc2_w', 'getstatic', 'putstatic', 'getfield', 'putfield',
                      'invokevirtual', 'invokespecial', 'invokestatic', 'new', 'anewarray',
                      'checkcast', 'instanceof'):
            args = (u2(code, pc + 1),)
            ln = 3
        elif name in ('iload', 'lload', 'fload', 'dload', 'aload', 'istore', 'lstore', 'fstore',
                      'dstore', 'astore', 'ret'):
            args = (code[pc + 1],)
            ln = 2
        elif name == 'iinc':
            args = (code[pc + 1], s1(code, pc + 2))
            ln = 3
        elif name.startswith('if') or name in ('goto', 'jsr'):
            args = (pc + s2(code, pc + 1),)
            ln = 3
        elif name in ('goto_w', 'jsr_w'):
            args = (pc + s4(code, pc + 1),)
            ln = 5
        elif name == 'tableswitch':
            p = (pc + 4) & ~3
            default = pc + s4(code, p)
            lo = s4(code, p + 4)
            hi = s4(code, p + 8)
            targets = [pc + s4(code, p + 12 + 4 * i) for i in range(hi - lo + 1)]
            args = (default, lo, targets)
            ln = p + 12 + 4 * (hi - lo + 1) - pc
        elif name == 'lookupswitch':
            p = (pc + 4) & ~3
            default = pc + s4(code, p)
            npairs = s4(code, p + 4)
            pairs = [(s4(code, p + 8 + 8 * i), pc + s4(code, p + 12 + 8 * i)) for i in range(npairs)]
            args = (default, pairs)
            ln = p + 8 + 8 * npairs - pc
        elif name == 'invokeinterface':
            args = (u2(code, pc + 1), code[pc + 3])
            ln = 5
        elif name == 'invokedynamic':
            raise SystemExit('invokedynamic not supported')
        elif name == 'newarray':
            args = (code[pc + 1],)
            ln = 2
        elif name == 'multianewarray':
            args = (u2(code, pc + 1), code[pc + 3])
            ln = 4
        elif name == 'wide':
            op2 = code[pc + 1]
            n2 = OPS[op2]
            if n2 == 'iinc':
                args = (u2(code, pc + 2), s2(code, pc + 4))
                ln = 6
            else:
                args = (u2(code, pc + 2),)
                ln = 4
            insns.append(Insn(pc, op2, args, ln))
            pc += ln
            continue
        insns.append(Insn(pc, op, args, ln))
        pc += ln
    return insns


# ---------------------------------------------------------------------------
# method translation

ARRAY_LOAD = {'iaload': ('i', 'int32_t'), 'laload': ('j', 'int64_t'), 'faload': ('f', 'float'),
              'daload': ('d', 'double'), 'aaload': ('a', 'JObject*'), 'baload': ('i', 'int8_t'),
              'caload': ('i', 'uint16_t'), 'saload': ('i', 'int16_t')}
ARRAY_STORE = {'iastore': ('i', 'int32_t'), 'lastore': ('j', 'int64_t'), 'fastore': ('f', 'float'),
               'dastore': ('d', 'double'), 'aastore': ('a', 'JObject*'), 'bastore': ('i', 'int8_t'),
               'castore': ('i', 'uint16_t'), 'sastore': ('i', 'int16_t')}
NEWARRAY_T = {4: 'Z', 5: 'C', 6: 'F', 7: 'D', 8: 'B', 9: 'S', 10: 'I', 11: 'J'}

BINOPS = {
    'iadd': ('i', '+'), 'isub': ('i', '-'), 'imul': ('i', '*'), 'iand': ('i', '&'), 'ior': ('i', '|'),
    'ixor': ('i', '^'),
    'ladd': ('j', '+'), 'lsub': ('j', '-'), 'lmul': ('j', '*'), 'land': ('j', '&'), 'lor': ('j', '|'),
    'lxor': ('j', '^'),
    'fadd': ('f', '+'), 'fsub': ('f', '-'), 'fmul': ('f', '*'), 'fdiv': ('f', '/'),
    'dadd': ('d', '+'), 'dsub': ('d', '-'), 'dmul': ('d', '*'), 'ddiv': ('d', '/'),
}
CONV = {
    'i2l': ('i', 'j', '(int64_t)%s'), 'i2f': ('i', 'f', '(float)%s'), 'i2d': ('i', 'd', '(double)%s'),
    'l2i': ('j', 'i', '(int32_t)%s'), 'l2f': ('j', 'f', '(float)%s'), 'l2d': ('j', 'd', '(double)%s'),
    'f2i': ('f', 'i', 'j_f2i(%s)'), 'f2l': ('f', 'j', 'j_f2l(%s)'), 'f2d': ('f', 'd', '(double)%s'),
    'd2i': ('d', 'i', 'j_d2i(%s)'), 'd2l': ('d', 'j', 'j_d2l(%s)'), 'd2f': ('d', 'f', '(float)%s'),
    'i2b': ('i', 'i', '(int32_t)(int8_t)%s'), 'i2c': ('i', 'i', '(int32_t)(uint16_t)%s'),
    'i2s': ('i', 'i', '(int32_t)(int16_t)%s'),
}
IFZ = {'ifeq': '==', 'ifne': '!=', 'iflt': '<', 'ifge': '>=', 'ifgt': '>', 'ifle': '<='}
IFCMP = {'if_icmpeq': '==', 'if_icmpne': '!=', 'if_icmplt': '<', 'if_icmpge': '>=', 'if_icmpgt': '>',
         'if_icmple': '<='}


def c_float(bits4):
    v = struct.unpack('>f', bits4)[0]
    if v != v:
        return 'J_NANF'
    if v in (float('inf'), float('-inf')):
        return 'J_INFF' if v > 0 else '(-J_INFF)'
    return '((float)%s)' % v.hex()


def c_double(bits8):
    v = struct.unpack('>d', bits8)[0]
    if v != v:
        return 'J_NAN'
    if v in (float('inf'), float('-inf')):
        return 'J_INF' if v > 0 else '(-J_INF)'
    return '(%s)' % v.hex()


def c_long(v):
    if v == -(1 << 63):
        return '((int64_t)INT64_MIN)'
    return 'INT64_C(%d)' % v


def c_int(v):
    if v == -(1 << 31):
        return '((int32_t)INT32_MIN)'
    return '%d' % v


class MethodTranslator:
    def __init__(self, prog, cls, m):
        self.prog = prog
        self.cls = cls
        self.m = m
        self.cf = cls.cf
        self.lines = []
        self.vars = set()
        self.exc_labels = {}   # handler tuple -> label
        self.label_targets = set()
        self.uses_dims = False

    # -- class-related expression helpers -----------------------------------
    def class_ref(self, name):
        """C expression yielding JClass* for an internal class name or array descriptor."""
        if name.startswith('['):
            comp = name[1:]
            if comp[0] == 'L':
                inner = '&C_%s' % cls_id(comp[1:-1])
                self.prog_need(comp[1:-1])
            elif comp[0] == '[':
                inner = self.class_ref(comp)
            else:
                return '&C_arr_%s' % comp
            return 'jarray_class(%s)' % inner
        self.prog_need(name)
        return '&C_%s' % cls_id(name)

    def prog_need(self, name):
        if name not in self.prog.classes:
            raise SystemExit('reference to unknown class %s from %s' % (name, self.cls.name))

    def init_check(self, target):
        """Emit class-init check if accessing `target` from the current class needs it."""
        if not self.prog.needs_init(target):
            return
        if self.prog.is_subclass(self.cls, target):
            return
        self.emit('JINIT(&C_%s, %s);' % (cls_id(target.name), self.exc_label_cur))

    def emit(self, s):
        self.lines.append('    ' + s)

    def var(self, name, kind):
        self.vars.add((name, kind))
        return name

    def sv(self, i, k):
        return self.var('s%d_%s' % (i, k), k)

    def lv(self, i, k):
        return self.var('l%d_%s' % (i, k), k)

    # -- analysis -----------------------------------------------------------
    def analyze(self):
        insns = decode(self.m.code)
        self.insns = insns
        self.by_pc = {i.pc: i for i in insns}
        self.next_pc = {}
        for idx, i in enumerate(insns):
            self.next_pc[i.pc] = insns[idx + 1].pc if idx + 1 < len(insns) else None
        stacks = {0: ()}
        work = [0]
        for e in self.m.exceptions:
            if e.handler not in stacks:
                stacks[e.handler] = ('a',)
                work.append(e.handler)
            self.label_targets.add(e.handler)
        while work:
            pc = work.pop()
            ins = self.by_pc[pc]
            st = stacks[pc]
            out, succ = self.flow(ins, st)
            for s in succ:
                if s not in stacks:
                    stacks[s] = out
                    work.append(s)
                elif stacks[s] != out:
                    raise SystemExit('stack mismatch in %s at %d: %r vs %r' % (self.m, s, stacks[s], out))
        self.stacks = stacks

    def flow(self, ins, st):
        """Return (stack after, successors) for type inference."""
        n = ins.name
        nxt = self.next_pc[ins.pc]
        st = list(st)
        pop = lambda k=1: [st.pop() for _ in range(k)]  # noqa: E731

        def ret(*succ):
            return tuple(st), list(succ)
        if n == 'nop':
            return ret(nxt)
        if n == 'aconst_null':
            st.append('a')
            return ret(nxt)
        if n.startswith('iconst') or n in ('bipush', 'sipush'):
            st.append('i')
            return ret(nxt)
        if n.startswith('lconst'):
            st.append('j')
            return ret(nxt)
        if n.startswith('fconst'):
            st.append('f')
            return ret(nxt)
        if n.startswith('dconst'):
            st.append('d')
            return ret(nxt)
        if n in ('ldc', 'ldc_w', 'ldc2_w'):
            e = self.cf.cp[ins.args[0]]
            st.append({'Integer': 'i', 'Float': 'f', 'Long': 'j', 'Double': 'd', 'String': 'a',
                       'Class': 'a'}[e[0]])
            return ret(nxt)
        if n[1:5] == 'load' and len(n) <= 7:
            st.append({'i': 'i', 'l': 'j', 'f': 'f', 'd': 'd', 'a': 'a'}[n[0]])
            return ret(nxt)
        if n in ARRAY_LOAD:
            pop(2)
            st.append(ARRAY_LOAD[n][0])
            return ret(nxt)
        if n[1:6] == 'store' and len(n) <= 8:
            pop()
            return ret(nxt)
        if n in ARRAY_STORE:
            pop(3)
            return ret(nxt)
        if n == 'pop':
            pop()
            return ret(nxt)
        if n == 'pop2':
            t = pop()[0]
            if t not in 'jd':
                pop()
            return ret(nxt)
        if n == 'dup':
            st.append(st[-1])
            return ret(nxt)
        if n == 'dup_x1':
            a, b = st.pop(), st.pop()
            st += [a, b, a]
            return ret(nxt)
        if n == 'dup_x2':
            a, b = st.pop(), st.pop()
            if b in 'jd':
                st += [a, b, a]
            else:
                c = st.pop()
                st += [a, c, b, a]
            return ret(nxt)
        if n == 'dup2':
            a = st[-1]
            if a in 'jd':
                st.append(a)
            else:
                st += [st[-2], st[-1]]
            return ret(nxt)
        if n == 'dup2_x1':
            a = st.pop()
            if a in 'jd':
                b = st.pop()
                st += [a, b, a]
            else:
                b, c = st.pop(), st.pop()
                st += [b, a, c, b, a]
            return ret(nxt)
        if n == 'dup2_x2':
            a = st.pop()
            if a in 'jd':
                b = st.pop()
                if b in 'jd':
                    st += [a, b, a]
                else:
                    c = st.pop()
                    st += [a, c, b, a]
            else:
                b = st.pop()
                c = st.pop()
                if c in 'jd':
                    st += [b, a, c, b, a]
                else:
                    d = st.pop()
                    st += [b, a, d, c, b, a]
            return ret(nxt)
        if n == 'swap':
            a, b = st.pop(), st.pop()
            st += [a, b]
            return ret(nxt)
        if n in BINOPS or n in ('idiv', 'irem', 'ldiv', 'lrem', 'frem', 'drem', 'ishl', 'ishr',
                                'iushr', 'lshl', 'lshr', 'lushr'):
            t = st[-2]
            pop(2)
            st.append(t)
            return ret(nxt)
        if n in ('ineg', 'lneg', 'fneg', 'dneg'):
            return ret(nxt)
        if n == 'iinc':
            return ret(nxt)
        if n in CONV:
            pop()
            st.append(CONV[n][1])
            return ret(nxt)
        if n in ('lcmp', 'fcmpl', 'fcmpg', 'dcmpl', 'dcmpg'):
            pop(2)
            st.append('i')
            return ret(nxt)
        if n in IFZ or n in ('ifnull', 'ifnonnull'):
            pop()
            return ret(nxt, ins.args[0])
        if n in IFCMP or n in ('if_acmpeq', 'if_acmpne'):
            pop(2)
            return ret(nxt, ins.args[0])
        if n in ('goto', 'goto_w'):
            return ret(ins.args[0])
        if n in ('jsr', 'jsr_w', 'ret'):
            raise SystemExit('jsr/ret not supported (%s)' % self.m)
        if n == 'tableswitch':
            pop()
            return ret(ins.args[0], *ins.args[2])
        if n == 'lookupswitch':
            pop()
            return ret(ins.args[0], *[t for _, t in ins.args[1]])
        if n.endswith('return'):
            return tuple(), []
        if n == 'athrow':
            return tuple(), []
        if n in ('getstatic', 'putstatic', 'getfield', 'putfield'):
            _, c, name, desc = self.cf.member_ref(ins.args[0])
            if n == 'putfield':
                pop(2)
            elif n == 'putstatic':
                pop()
            elif n == 'getfield':
                pop()
                st.append(kind_of(desc))
            else:
                st.append(kind_of(desc))
            return ret(nxt)
        if n.startswith('invoke'):
            _, c, name, desc = self.cf.member_ref(ins.args[0])
            args, r = parse_method_desc(desc)
            pop(len(args) + (0 if n == 'invokestatic' else 1))
            if r != 'V':
                st.append(kind_of(r))
            return ret(nxt)
        if n == 'new':
            st.append('a')
            return ret(nxt)
        if n in ('newarray', 'anewarray'):
            pop()
            st.append('a')
            return ret(nxt)
        if n == 'multianewarray':
            pop(ins.args[1])
            st.append('a')
            return ret(nxt)
        if n == 'arraylength':
            pop()
            st.append('i')
            return ret(nxt)
        if n == 'checkcast':
            return ret(nxt)
        if n == 'instanceof':
            pop()
            st.append('i')
            return ret(nxt)
        if n in ('monitorenter', 'monitorexit'):
            pop()
            return ret(nxt)
        raise SystemExit('unhandled opcode %s' % n)

    # -- exceptions ----------------------------------------------------------
    def handlers_at(self, pc):
        hs = []
        for e in self.m.exceptions:
            if e.start <= pc < e.end:
                hs.append((e.catch_type, e.handler))
        return tuple(hs)

    def exc_label_for(self, pc):
        key = self.handlers_at(pc)
        if key not in self.exc_labels:
            self.exc_labels[key] = 'EXC_%d' % len(self.exc_labels)
        return self.exc_labels[key]

    # -- emission --------------------------------------------------------------
    def translate(self):
        m = self.m
        self.analyze()
        args, r = parse_method_desc(m.desc)
        self.rk = kind_of(r)
        # parameters
        params = []
        slot = 0
        prologue = []
        if not m.is_static:
            params.append('JObject* p0')
            prologue.append('%s = p0;' % self.lv(0, 'a'))
            slot = 1
        for a in args:
            k = kind_of(a)
            params.append('%s p%d' % (CTYPE[k], slot))
            prologue.append('%s = p%d;' % (self.lv(slot, k), slot))
            slot += 2 if k in 'jd' else 1
        self.params = params
        for ins in self.insns:
            if ins.name in ('goto', 'goto_w') or ins.name.startswith('if'):
                self.label_targets.add(ins.args[0])
            elif ins.name == 'tableswitch':
                self.label_targets.add(ins.args[0])
                self.label_targets.update(ins.args[2])
            elif ins.name == 'lookupswitch':
                self.label_targets.add(ins.args[0])
                self.label_targets.update(t for _, t in ins.args[1])
        body = []
        for ins in self.insns:
            if ins.pc not in self.stacks:
                continue  # unreachable
            self.lines = []
            self.exc_label_cur = self.exc_label_for(ins.pc)
            self.gen(ins, self.stacks[ins.pc])
            if ins.pc in self.label_targets:
                body.append('L%d:;' % ins.pc)
            body.extend(self.lines)
        # exception dispatch blocks
        self.lines = []
        for key, label in self.exc_labels.items():
            self.lines.append('%s:' % label)
            for ct, h in key:
                if ct is None:
                    self.emit('%s = jvm_take_exc(); goto L%d;' % (self.sv(0, 'a'), h))
                    break
                self.emit('if (jinstanceof(J_EXC, %s)) { %s = jvm_take_exc(); goto L%d; }'
                          % (self.class_ref(ct), self.sv(0, 'a'), h))
            else:
                self.emit(ret_stmt_default(self.rk))
        body.extend(self.lines)
        decls = []
        for name, k in sorted(self.vars):
            decls.append('    %s %s;' % (CTYPE[k], name))
        if self.uses_dims:
            decls.append('    int32_t dims[8];')
        return params, decls, ['    ' + p for p in prologue], body

    def gen(self, ins, st):
        n = ins.name
        d = len(st)
        E = self.exc_label_cur
        emit = self.emit
        top = d - 1

        def S(i):
            return self.sv(i, st[i])
        if n == 'nop':
            return
        if n == 'aconst_null':
            emit('%s = NULL;' % self.sv(d, 'a'))
            return
        if n.startswith('iconst'):
            v = -1 if n == 'iconst_m1' else int(n[7:])
            emit('%s = %d;' % (self.sv(d, 'i'), v))
            return
        if n in ('bipush', 'sipush'):
            emit('%s = %d;' % (self.sv(d, 'i'), ins.args[0]))
            return
        if n.startswith('lconst'):
            emit('%s = %s;' % (self.sv(d, 'j'), n[7:]))
            return
        if n.startswith('fconst'):
            emit('%s = %s.0f;' % (self.sv(d, 'f'), n[7:]))
            return
        if n.startswith('dconst'):
            emit('%s = %s.0;' % (self.sv(d, 'd'), n[7:]))
            return
        if n in ('ldc', 'ldc_w', 'ldc2_w'):
            e = self.cf.cp[ins.args[0]]
            if e[0] == 'Integer':
                emit('%s = %s;' % (self.sv(d, 'i'), c_int(e[1])))
            elif e[0] == 'Float':
                emit('%s = %s;' % (self.sv(d, 'f'), c_float(e[1])))
            elif e[0] == 'Long':
                emit('%s = %s;' % (self.sv(d, 'j'), c_long(e[1])))
            elif e[0] == 'Double':
                emit('%s = %s;' % (self.sv(d, 'd'), c_double(e[1])))
            elif e[0] == 'String':
                idx = self.prog.string_index(self.cf.utf16(e[1]))
                emit('%s = jvm_strings[%d];' % (self.sv(d, 'a'), idx))
            elif e[0] == 'Class':
                emit('%s = jclass_object(%s);' % (self.sv(d, 'a'), self.class_ref(self.cf.utf8(e[1]))))
            return
        if n[1:5] == 'load' and len(n) <= 7:
            k = {'i': 'i', 'l': 'j', 'f': 'f', 'd': 'd', 'a': 'a'}[n[0]]
            idx = ins.args[0] if len(n) == 5 else int(n[6])
            emit('%s = %s;' % (self.sv(d, k), self.lv(idx, k)))
            return
        if n[1:6] == 'store' and len(n) <= 8:
            k = {'i': 'i', 'l': 'j', 'f': 'f', 'd': 'd', 'a': 'a'}[n[0]]
            idx = ins.args[0] if len(n) == 6 else int(n[7])
            emit('%s = %s;' % (self.lv(idx, k), S(top)))
            return
        if n in ARRAY_LOAD:
            k, ct = ARRAY_LOAD[n]
            arr, idx = S(top - 1), S(top)
            emit('ARR_CHK(%s, %s, %s);' % (arr, idx, E))
            emit('%s = JARR(%s, %s)[%s];' % (self.sv(top - 1, k), arr, ct, idx))
            return
        if n in ARRAY_STORE:
            k, ct = ARRAY_STORE[n]
            arr, idx, val = S(top - 2), S(top - 1), S(top)
            emit('ARR_CHK(%s, %s, %s);' % (arr, idx, E))
            if n == 'aastore':
                emit('AASTORE_CHK(%s, %s, %s);' % (arr, val, E))
            emit('JARR(%s, %s)[%s] = (%s)%s;' % (arr, ct, idx, ct, val))
            return
        if n in ('pop', 'pop2'):
            return
        if n == 'dup':
            emit('%s = %s;' % (self.sv(d, st[top]), S(top)))
            return
        if n in ('dup_x1', 'dup_x2', 'dup2', 'dup2_x1', 'dup2_x2', 'swap'):
            out, _ = self.flow(ins, tuple(st))
            # Determine the permutation: work on symbolic entries
            ent = [('s', i) for i in range(d)]
            sym = list(ent)
            cat = {('s', i): st[i] for i in range(d)}
            stack = list(sym)

            def popc():
                return stack.pop()
            if n == 'dup_x1':
                a, b = popc(), popc()
                stack += [a, b, a]
            elif n == 'dup_x2':
                a, b = popc(), popc()
                if cat[b] in 'jd':
                    stack += [a, b, a]
                else:
                    c = popc()
                    stack += [a, c, b, a]
            elif n == 'dup2':
                a = stack[-1]
                if cat[a] in 'jd':
                    stack.append(a)
                else:
                    stack += [stack[-2], stack[-1]]
            elif n == 'dup2_x1':
                a = popc()
                if cat[a] in 'jd':
                    b = popc()
                    stack += [a, b, a]
                else:
                    b, c = popc(), popc()
                    stack += [b, a, c, b, a]
            elif n == 'dup2_x2':
                a = popc()
                if cat[a] in 'jd':
                    b = popc()
                    if cat[b] in 'jd':
                        stack += [a, b, a]
                    else:
                        c = popc()
                        stack += [a, c, b, a]
                else:
                    b = popc()
                    c = popc()
                    if cat[c] in 'jd':
                        stack += [b, a, c, b, a]
                    else:
                        dd = popc()
                        stack += [b, a, dd, c, b, a]
            elif n == 'swap':
                a, b = popc(), popc()
                stack += [a, b]
            # assign via temporaries to avoid clobbering
            tmps = []
            for i, e in enumerate(stack):
                if e != ('s', i):
                    src = self.sv(e[1], cat[e])
                    t = self.var('t%d_%s' % (i, cat[e]), cat[e])
                    emit('%s = %s;' % (t, src))
                    tmps.append((i, cat[e], t))
            for i, k, t in tmps:
                emit('%s = %s;' % (self.sv(i, k), t))
            return
        if n in BINOPS:
            k, op = BINOPS[n]
            a, b = S(top - 1), S(top)
            if k in 'ij' and op in '+-*':
                ut = 'uint32_t' if k == 'i' else 'uint64_t'
                emit('%s = (%s)((%s)%s %s (%s)%s);' % (self.sv(top - 1, k), CTYPE[k], ut, a, op, ut, b))
            else:
                emit('%s = %s %s %s;' % (self.sv(top - 1, k), a, op, b))
            return
        if n in ('idiv', 'irem', 'ldiv', 'lrem'):
            k = 'i' if n[0] == 'i' else 'j'
            a, b = S(top - 1), S(top)
            emit('if (UNLIKELY(%s == 0)) { jthrow_arith(); goto %s; }' % (b, E))
            mn = 'INT32_MIN' if k == 'i' else 'INT64_MIN'
            if n.endswith('div'):
                emit('%s = (%s == -1) ? (%s)(0 - (%s)%s) : %s / %s;' % (
                    self.sv(top - 1, k), b, CTYPE[k], 'uint32_t' if k == 'i' else 'uint64_t', a, a, b))
            else:
                emit('%s = (%s == -1) ? 0 : %s %% %s;' % (self.sv(top - 1, k), b, a, b))
            _ = mn
            return
        if n in ('frem', 'drem'):
            k = n[0]
            a, b = S(top - 1), S(top)
            emit('%s = %s(%s, %s);' % (self.sv(top - 1, k), 'fmodf' if k == 'f' else 'fmod', a, b))
            return
        if n in ('ishl', 'ishr', 'iushr', 'lshl', 'lshr', 'lushr'):
            k = 'i' if n[0] == 'i' else 'j'
            a, b = S(top - 1), S(top)
            mask = 31 if k == 'i' else 63
            ut = 'uint32_t' if k == 'i' else 'uint64_t'
            if n.endswith('ushr'):
                expr = '(%s)((%s)%s >> (%s & %d))' % (CTYPE[k], ut, a, b, mask)
            elif n.endswith('shl'):
                expr = '(%s)((%s)%s << (%s & %d))' % (CTYPE[k], ut, a, b, mask)
            else:
                expr = '%s >> (%s & %d)' % (a, b, mask)
            emit('%s = %s;' % (self.sv(top - 1, k), expr))
            return
        if n in ('ineg', 'lneg'):
            k = n[0] if n[0] == 'i' else 'j'
            ut = 'uint32_t' if k == 'i' else 'uint64_t'
            emit('%s = (%s)(0 - (%s)%s);' % (S(top), CTYPE[k], ut, S(top)))
            return
        if n in ('fneg', 'dneg'):
            emit('%s = -%s;' % (S(top), S(top)))
            return
        if n == 'iinc':
            v = self.lv(ins.args[0], 'i')
            emit('%s = (int32_t)((uint32_t)%s + (uint32_t)%d);' % (v, v, ins.args[1]))
            return
        if n in CONV:
            fk, tk, fmt = CONV[n]
            emit('%s = %s;' % (self.sv(top, tk), fmt % S(top)))
            return
        if n == 'lcmp':
            a, b = S(top - 1), S(top)
            emit('%s = (%s > %s) - (%s < %s);' % (self.sv(top - 1, 'i'), a, b, a, b))
            return
        if n in ('fcmpl', 'fcmpg', 'dcmpl', 'dcmpg'):
            a, b = S(top - 1), S(top)
            nan = '-1' if n.endswith('l') else '1'
            emit('%s = (%s > %s) ? 1 : (%s == %s) ? 0 : (%s < %s) ? -1 : %s;' % (
                self.sv(top - 1, 'i'), a, b, a, b, a, b, nan))
            return
        if n in IFZ:
            emit('if (%s %s 0) goto L%d;' % (S(top), IFZ[n], ins.args[0]))
            return
        if n in IFCMP:
            emit('if (%s %s %s) goto L%d;' % (S(top - 1), IFCMP[n], S(top), ins.args[0]))
            return
        if n in ('if_acmpeq', 'if_acmpne'):
            emit('if (%s %s %s) goto L%d;' % (S(top - 1), '==' if n.endswith('eq') else '!=', S(top),
                                             ins.args[0]))
            return
        if n == 'ifnull':
            emit('if (%s == NULL) goto L%d;' % (S(top), ins.args[0]))
            return
        if n == 'ifnonnull':
            emit('if (%s != NULL) goto L%d;' % (S(top), ins.args[0]))
            return
        if n in ('goto', 'goto_w'):
            emit('goto L%d;' % ins.args[0])
            return
        if n == 'tableswitch':
            default, lo, targets = ins.args
            emit('switch (%s) {' % S(top))
            for i, t in enumerate(targets):
                if t != default:
                    emit('case %s: goto L%d;' % (c_int(lo + i), t))
            emit('default: goto L%d;' % default)
            emit('}')
            return
        if n == 'lookupswitch':
            default, pairs = ins.args
            emit('switch (%s) {' % S(top))
            for k, t in pairs:
                emit('case %s: goto L%d;' % (c_int(k), t))
            emit('default: goto L%d;' % default)
            emit('}')
            return
        if n.endswith('return'):
            if self.m.is_synchronized:
                pass  # handled by wrapper
            if n == 'return':
                emit('return;')
            else:
                emit('return %s;' % S(top))
            return
        if n == 'athrow':
            emit('jvm_throw(%s); goto %s;' % (S(top), E))
            return
        if n in ('getstatic', 'putstatic'):
            _, c, name, desc = self.cf.member_ref(ins.args[0])
            dc, f = self.prog.find_field(c, name, desc)
            self.init_check(dc)
            g = dc.field_cname[(name, desc)]
            if n == 'getstatic':
                emit('%s = %s;' % (self.sv(d, kind_of(desc)), g))
            else:
                emit('%s = (%s)%s;' % (g, FIELD_CTYPE[desc[0]], S(top)))
            return
        if n in ('getfield', 'putfield'):
            _, c, name, desc = self.cf.member_ref(ins.args[0])
            dc, f = self.prog.find_field(c, name, desc)
            mem = dc.field_cname[(name, desc)]
            obj = S(top) if n == 'getfield' else S(top - 1)
            self.null_check(obj, st, top if n == 'getfield' else top - 1, ins)
            acc = '((S_%s*)%s)->%s' % (cls_id(dc.name), obj, mem)
            if n == 'getfield':
                emit('%s = %s;' % (self.sv(top, kind_of(desc)), acc))
            else:
                emit('%s = (%s)%s;' % (acc, FIELD_CTYPE[desc[0]], S(top)))
            return
        if n.startswith('invoke'):
            self.gen_invoke(ins, st)
            return
        if n == 'new':
            cname = self.cf.class_name(ins.args[0])
            c = self.prog.classes.get(cname)
            if c is None:
                raise SystemExit('new of unknown class %s' % cname)
            self.init_check(c)
            emit('%s = jnew(&C_%s); if (UNLIKELY(!%s)) goto %s;' % (
                self.sv(d, 'a'), cls_id(cname), self.sv(d, 'a'), E))
            return
        if n == 'newarray':
            t = NEWARRAY_T[ins.args[0]]
            emit('%s = jnew_array(&C_arr_%s, %s); if (UNLIKELY(!%s)) goto %s;' % (
                self.sv(top, 'a'), t, S(top), self.sv(top, 'a'), E))
            return
        if n == 'anewarray':
            cname = self.cf.class_name(ins.args[0])
            cref = self.class_ref(cname)
            emit('%s = jnew_array(jarray_class(%s), %s); if (UNLIKELY(!%s)) goto %s;' % (
                self.sv(top, 'a'), cref, S(top), self.sv(top, 'a'), E))
            return
        if n == 'multianewarray':
            cname = self.cf.class_name(ins.args[0])
            dims = ins.args[1]
            cref = self.class_ref(cname)
            self.uses_dims = True
            for i in range(dims):
                emit('dims[%d] = %s;' % (i, S(d - dims + i)))
            emit('%s = jnew_multiarray(%s, %d, dims); if (UNLIKELY(!%s)) goto %s;' % (
                self.sv(d - dims, 'a'), cref, dims, self.sv(d - dims, 'a'), E))
            return
        if n == 'arraylength':
            self.null_check(S(top), st, top, ins)
            emit('%s = ((JArray*)%s)->length;' % (self.sv(top, 'i'), S(top)))
            return
        if n == 'checkcast':
            cname = self.cf.class_name(ins.args[0])
            emit('if (UNLIKELY(%s && !jinstanceof(%s, %s))) { jthrow_cce(%s, %s); goto %s; }' % (
                S(top), S(top), self.class_ref(cname), S(top), self.class_ref(cname), E))
            return
        if n == 'instanceof':
            cname = self.cf.class_name(ins.args[0])
            emit('%s = jinstanceof(%s, %s);' % (self.sv(top, 'i'), S(top), self.class_ref(cname)))
            return
        if n == 'monitorenter':
            self.null_check(S(top), st, top, ins)
            emit('jmonitor_enter(%s);' % S(top))
            return
        if n == 'monitorexit':
            self.null_check(S(top), st, top, ins)
            emit('jmonitor_exit(%s);' % S(top))
            return
        raise SystemExit('cannot translate %s' % n)

    def null_check(self, expr, st, idx, ins):
        self.emit('NULL_CHK(%s, %s);' % (expr, self.exc_label_cur))

    def gen_invoke(self, ins, st):
        n = ins.name
        d = len(st)
        E = self.exc_label_cur
        _, cname, name, desc = self.cf.member_ref(ins.args[0])
        args, r = parse_method_desc(desc)
        rk = kind_of(r)
        nargs = len(args) + (0 if n == 'invokestatic' else 1)
        base = d - nargs
        argv = [self.sv(base + i, st[base + i]) for i in range(nargs)]
        res = '' if rk == 'V' else '%s = ' % self.sv(base, rk)
        ctypes = ([] if n == 'invokestatic' else ['JObject*']) + [CTYPE[kind_of(a)] for a in args]
        fnptr_t = '%s (*)(%s)' % (CTYPE[rk], ', '.join(ctypes) or 'void')

        # array receiver methods (clone etc.)
        if cname.startswith('['):
            self.null_check(argv[0], st, base, ins)
            if name == 'clone':
                self.emit('%sjarray_clone(%s); if (UNLIKELY(J_EXC)) goto %s;' % (res, argv[0], E))
                return
            cname = 'java/lang/Object'

        if n == 'invokestatic':
            m = self.prog.find_method(cname, name, desc)
            self.init_check(m.cls_info)
            fn = method_cname(m.cls.name, m.name, m.desc)
            self.emit('%s%s(%s);' % (res, fn, ', '.join(argv)))
        elif n == 'invokespecial':
            m = self.prog.find_method(cname, name, desc)
            self.null_check(argv[0], st, base, ins)
            fn = method_cname(m.cls.name, m.name, m.desc)
            self.emit('%s%s(%s);' % (res, fn, ', '.join(argv)))
        elif n == 'invokevirtual' and not self.prog.classes[cname].is_interface:
            c = self.prog.classes[cname]
            k = (name, desc)
            self.null_check(argv[0], st, base, ins)
            if k not in c.vindex:
                # private or final method resolved directly
                m = self.prog.find_method(cname, name, desc)
                fn = method_cname(m.cls.name, m.name, m.desc)
                self.emit('%s%s(%s);' % (res, fn, ', '.join(argv)))
            else:
                impl = c.vtable[c.vindex[k]][2]
                if impl is not None and (impl.is_final or not self.prog.overridden_below(c, k)):
                    fn = method_cname(impl.cls.name, impl.name, impl.desc)
                    self.emit('%s%s(%s);' % (res, fn, ', '.join(argv)))
                else:
                    self.emit('%s((%s)JVCALL(%s, %d))(%s);' % (
                        res, fnptr_t, argv[0], c.vindex[k], ', '.join(argv)))
        else:  # invokeinterface (or invokevirtual on an interface type)
            if (name, desc) not in self.prog.iface_methods(cname):
                obj = self.prog.classes['java/lang/Object']
                self.null_check(argv[0], st, base, ins)
                self.emit('%s((%s)JVCALL(%s, %d))(%s);' % (
                    res, fnptr_t, argv[0], obj.vindex[(name, desc)], ', '.join(argv)))
                self.emit('if (UNLIKELY(J_EXC)) goto %s;' % E)
                return
            iid = self.prog.iface_id(name, desc)
            self.null_check(argv[0], st, base, ins)
            self.emit('%s((%s)jfind_imethod(%s, %d))(%s);' % (res, fnptr_t, argv[0], iid, ', '.join(argv)))
        self.emit('if (UNLIKELY(J_EXC)) goto %s;' % E)


# ---------------------------------------------------------------------------
# output generation


def c_string_units(units):
    return ', '.join(str(u) for u in units)


class Generator:
    def __init__(self, prog, outdir):
        self.prog = prog
        self.outdir = outdir

    def write(self, name, text):
        path = os.path.join(self.outdir, name)
        old = None
        if os.path.exists(path):
            with open(path, 'r', encoding='utf-8') as f:
                old = f.read()
        if old != text:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(text)

    def run(self):
        prog = self.prog
        os.makedirs(self.outdir, exist_ok=True)
        for c in prog.classes.values():
            for m in c.cf.methods:
                m.cls_info = c
        # translate all methods first (fills string pool / iface ids)
        bodies = {}
        for c in prog.order:
            for m in c.cf.methods:
                if m.code is None:
                    continue
                t = MethodTranslator(prog, c, m)
                bodies[(c.name, m.name, m.desc)] = t.translate()
        # interface ids for all interface methods
        for c in prog.order:
            if c.is_interface:
                for k in prog.iface_methods(c.name):
                    prog.iface_id(*k)
        self.gen_header()
        files = []
        for c in prog.order:
            fn = 'c_%s.c' % cls_id(c.name)
            self.gen_class(c, bodies, fn)
            files.append(fn)
        self.gen_tables()
        files.append('j2c_tables.c')
        self.write('files.mk', 'GEN_SRCS = ' + ' '.join(files) + '\n')
        natives = []
        for c in prog.order:
            for m in c.cf.methods:
                if m.is_native:
                    natives.append(self.proto(c, m) + ';')
        self.write('natives.txt', '\n'.join(natives) + '\n')

    def proto(self, c, m, name=None):
        args, r = parse_method_desc(m.desc)
        ps = [] if m.is_static else ['JObject* self']
        for i, a in enumerate(args):
            ps.append('%s a%d' % (CTYPE[kind_of(a)], i))
        return '%s %s(%s)' % (CTYPE[kind_of(r)], name or method_cname(c.name, m.name, m.desc),
                              ', '.join(ps) or 'void')

    def gen_header(self):
        prog = self.prog
        out = ['/* generated by j2c - do not edit */', '#ifndef J2C_GEN_H', '#define J2C_GEN_H',
               '#include "jvm.h"', '']
        for c in prog.order:
            out.append('extern JClass C_%s;' % cls_id(c.name))
        out.append('')
        # structs in hierarchy order
        for c in prog.order:
            if c.is_interface:
                continue
            cid = cls_id(c.name)
            out.append('typedef struct S_%s {' % cid)
            if c.super:
                out.append('    S_%s super;' % cls_id(c.super.name))
            else:
                out.append('    JObject hdr;')
            for f in c.cf.fields:
                if f.is_static:
                    continue
                out.append('    %s %s;' % (FIELD_CTYPE[f.desc[0]], c.field_cname[(f.name, f.desc)]))
            out.append('} S_%s;' % cid)
        out.append('')
        for c in prog.order:
            for f in c.cf.fields:
                if f.is_static:
                    out.append('extern %s %s;' % (FIELD_CTYPE[f.desc[0]], c.field_cname[(f.name, f.desc)]))
        out.append('')
        for c in prog.order:
            for m in c.cf.methods:
                if m.is_abstract and not m.is_native:
                    continue
                out.append(self.proto(c, m) + ';')
        out.append('')
        # vtable index macros for library use
        for c in prog.order:
            if c.is_interface:
                continue
            if not c.name.startswith(('java/', 'javax/', 'j2me/')):
                continue
            for i, (name, desc, impl) in enumerate(c.vtable):
                if c.super and (name, desc) in c.super.vindex:
                    continue
                out.append('#define VT_%s__%s__%s %d' % (cls_id(c.name), mangle_mname(name),
                                                        mangle(desc.replace('(', '').replace(')', '_')), i))
        for (name, desc), iid in sorted(prog.iface_ids.items(), key=lambda x: x[1]):
            out.append('#define IID_%s__%s %d' % (mangle_mname(name),
                                                 mangle(desc.replace('(', '').replace(')', '_')), iid))
        out.append('')
        out.append('#endif')
        self.write('j2c_gen.h', '\n'.join(out) + '\n')

    def gen_class(self, c, bodies, fn):
        prog = self.prog
        cid = cls_id(c.name)
        out = ['/* generated by j2c from %s - do not edit */' % c.name, '#include "j2c_gen.h"', '']
        # static fields
        for f in c.cf.fields:
            if not f.is_static:
                continue
            init = ''
            if f.const is not None and f.desc[0] != 'L':
                e = c.cf.cp[f.const]
                if e[0] == 'Integer':
                    init = ' = %s' % c_int(e[1])
                elif e[0] == 'Long':
                    init = ' = %s' % c_long(e[1])
                elif e[0] == 'Float':
                    init = ' = %s' % c_float(e[1])
                elif e[0] == 'Double':
                    init = ' = %s' % c_double(e[1])
            out.append('%s %s%s;' % (FIELD_CTYPE[f.desc[0]], c.field_cname[(f.name, f.desc)], init))
        out.append('')
        # methods
        for m in c.cf.methods:
            if m.code is None:
                continue
            params, decls, prologue, body = bodies[(c.name, m.name, m.desc)]
            name = method_cname(c.name, m.name, m.desc)
            args, r = parse_method_desc(m.desc)
            rk = kind_of(r)
            fname = name + '__body' if m.is_synchronized else name
            static = 'static ' if m.is_synchronized else ''
            out.append('%s%s %s(%s) {' % (static, CTYPE[rk], fname, ', '.join(params) or 'void'))
            out.extend(decls)
            out.extend(prologue)
            out.extend(body)
            out.append('}')
            if m.is_synchronized:
                lock = 'p0' if not m.is_static else 'jclass_object(&C_%s)' % cid
                pnames = ', '.join(p.split()[-1] for p in params)
                out.append('%s %s(%s) {' % (CTYPE[rk], name, ', '.join(params) or 'void'))
                out.append('    JObject* lk = %s;' % lock)
                out.append('    jmonitor_enter(lk);')
                if rk == 'V':
                    out.append('    %s(%s);' % (fname, pnames))
                    out.append('    jmonitor_exit(lk);')
                else:
                    out.append('    %s r = %s(%s);' % (CTYPE[rk], fname, pnames))
                    out.append('    jmonitor_exit(lk);')
                    out.append('    return r;')
                out.append('}')
            out.append('')
        # class init function
        clinit = ('<clinit>', '()V') in c.methods
        str_consts = [f for f in c.cf.fields if f.is_static and f.const is not None and f.desc[0] == 'L']
        initfn = 'NULL'
        if clinit or str_consts:
            initfn = 'CI_%s' % cid
            out.append('static void CI_%s(void) {' % cid)
            for f in str_consts:
                units = c.cf.utf16(c.cf.cp[f.const][1])
                idx = prog.string_index(units)
                out.append('    %s = jvm_strings[%d];' % (c.field_cname[(f.name, f.desc)], idx))
            if clinit:
                out.append('    %s();' % method_cname(c.name, '<clinit>', '()V'))
            out.append('}')
        # vtable
        vt = 'NULL'
        if not c.is_interface and c.vtable:
            vt = 'VT_%s' % cid
            out.append('static void* const VT_%s[] = {' % cid)
            for name, desc, impl in c.vtable:
                if impl is None or impl.is_abstract:
                    out.append('    (void*)jvm_abstract_method,')
                else:
                    out.append('    (void*)%s,' % method_cname(impl.cls.name, impl.name, impl.desc))
            out.append('};')
        # interfaces implemented
        ifs = 'NULL'
        if c.all_ifaces:
            ifs = 'IF_%s' % cid
            out.append('static JClass* const IF_%s[] = { %s };' % (
                cid, ', '.join('&C_%s' % cls_id(i) for i in c.all_ifaces)))
        # interface method table
        imt = 'NULL'
        nim = 0
        if not c.is_interface and c.all_ifaces:
            ents = {}
            for i in c.all_ifaces:
                for k in prog.iface_methods(i):
                    iid = prog.iface_id(*k)
                    impl = c.vtable[c.vindex[k]][2] if k in c.vindex else None
                    ents[iid] = impl
            imt = 'IM_%s' % cid
            nim = len(ents)
            out.append('static const JIMethod IM_%s[] = {' % cid)
            for iid in sorted(ents):
                impl = ents[iid]
                fnp = '(void*)jvm_abstract_method' if impl is None or impl.is_abstract else \
                    '(void*)%s' % method_cname(impl.cls.name, impl.name, impl.desc)
                out.append('    { %d, %s },' % (iid, fnp))
            out.append('};')
        # reference field offsets (including inherited)
        refs = []
        cc = c
        while cc and not c.is_interface:
            for f in cc.cf.fields:
                if not f.is_static and f.desc[0] in 'L[':
                    refs.append('offsetof(S_%s, %s)' % (cls_id(cc.name), cc.field_cname[(f.name, f.desc)]))
            cc = cc.super
        refv = 'NULL'
        if refs:
            refv = 'RO_%s' % cid
            out.append('static const uint16_t RO_%s[] = { %s };' % (cid, ', '.join(refs)))
        # factory
        factory = 'NULL'
        if not c.is_interface and not c.is_abstract and ('<init>', '()V') in c.methods:
            factory = 'NI_%s' % cid
            out.append('static JObject* NI_%s(void) {' % cid)
            out.append('    JObject* o = jnew(&C_%s);' % cid)
            out.append('    if (!o) return NULL;')
            out.append('    %s(o);' % method_cname(c.name, '<init>', '()V'))
            out.append('    return J_EXC ? NULL : o;')
            out.append('}')
        flags = []
        if c.is_interface:
            flags.append('JCLASS_INTERFACE')
        if c.is_abstract:
            flags.append('JCLASS_ABSTRACT')
        out.append('JClass C_%s = {' % cid)
        out.append('    .name = "%s",' % c.name)
        out.append('    .super = %s,' % ('&C_%s' % cls_id(c.super.name) if c.super else 'NULL'))
        out.append('    .flags = %s,' % (' | '.join(flags) or '0'))
        out.append('    .instance_size = %s,' % ('0' if c.is_interface else 'sizeof(S_%s)' % cid))
        out.append('    .vtable = %s, .vtable_len = %d,' % (vt, 0 if c.is_interface else len(c.vtable)))
        out.append('    .interfaces = %s, .n_interfaces = %d,' % (ifs, len(c.all_ifaces)))
        out.append('    .imethods = %s, .n_imethods = %d,' % (imt, nim))
        out.append('    .ref_offsets = %s, .n_refs = %d,' % (refv, len(refs)))
        out.append('    .clinit = %s, .init_state = %s,' % (initfn, '0' if prog.needs_init(c) else '2'))
        out.append('    .new_instance = %s,' % factory)
        out.append('};')
        self.write(fn, '\n'.join(out) + '\n')

    def gen_tables(self):
        prog = self.prog
        out = ['/* generated by j2c - do not edit */', '#include "j2c_gen.h"', '']
        # strings
        for i, units in enumerate(prog.string_list):
            out.append('static const uint16_t STR_%d[] = { %s };' % (i, c_string_units(units) or '0'))
        out.append('const JStrLit jvm_string_lits[] = {')
        for i, units in enumerate(prog.string_list):
            out.append('    { STR_%d, %d },' % (i, len(units)))
        out.append('    { NULL, 0 }')
        out.append('};')
        out.append('const int jvm_n_strings = %d;' % len(prog.string_list))
        out.append('JObject* jvm_strings[%d];' % max(1, len(prog.string_list)))
        # classes
        out.append('JClass* const jvm_classes[] = {')
        for c in prog.order:
            out.append('    &C_%s,' % cls_id(c.name))
        out.append('    NULL')
        out.append('};')
        # static roots
        out.append('JObject** const jvm_static_roots[] = {')
        for c in prog.order:
            for f in c.cf.fields:
                if f.is_static and f.desc[0] in 'L[':
                    out.append('    &%s,' % c.field_cname[(f.name, f.desc)])
        out.append('    NULL')
        out.append('};')
        self.write('j2c_tables.c', '\n'.join(out) + '\n')


# ---------------------------------------------------------------------------


def load_inputs(prog, paths):
    for p in paths:
        if os.path.isdir(p):
            for root, _, files in os.walk(p):
                for f in sorted(files):
                    if f.endswith('.class'):
                        with open(os.path.join(root, f), 'rb') as fh:
                            prog.add(ClassFile(fh.read()))
        else:
            with zipfile.ZipFile(p) as z:
                for n in sorted(z.namelist()):
                    if n.endswith('.class'):
                        prog.add(ClassFile(z.read(n)))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('-o', '--out', required=True)
    ap.add_argument('inputs', nargs='+', help='library class dir first, then the game jar')
    a = ap.parse_args()
    prog = Program()
    load_inputs(prog, a.inputs)
    prog.link()
    Generator(prog, a.out).run()
    print('j2c: %d classes, %d strings' % (len(prog.classes), len(prog.string_list)))


if __name__ == '__main__':
    main()
