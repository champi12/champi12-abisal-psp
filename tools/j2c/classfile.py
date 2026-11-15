"""Minimal Java .class file parser (versions 45..52) used by the j2c translator."""
import struct

ACC_PUBLIC = 0x0001
ACC_PRIVATE = 0x0002
ACC_PROTECTED = 0x0004
ACC_STATIC = 0x0008
ACC_FINAL = 0x0010
ACC_SYNCHRONIZED = 0x0020
ACC_NATIVE = 0x0100
ACC_INTERFACE = 0x0200
ACC_ABSTRACT = 0x0400


class Reader:
    def __init__(self, data):
        self.d = data
        self.p = 0

    def u1(self):
        v = self.d[self.p]
        self.p += 1
        return v

    def u2(self):
        v = struct.unpack_from('>H', self.d, self.p)[0]
        self.p += 2
        return v

    def u4(self):
        v = struct.unpack_from('>I', self.d, self.p)[0]
        self.p += 4
        return v

    def bytes(self, n):
        v = self.d[self.p:self.p + n]
        self.p += n
        return v


def decode_mutf8(b):
    """Decode Java 'modified UTF-8' into a list of UTF-16 code units."""
    out = []
    i = 0
    n = len(b)
    while i < n:
        c = b[i]
        if c < 0x80:
            out.append(c)
            i += 1
        elif (c & 0xE0) == 0xC0:
            out.append(((c & 0x1F) << 6) | (b[i + 1] & 0x3F))
            i += 2
        else:
            out.append(((c & 0x0F) << 12) | ((b[i + 1] & 0x3F) << 6) | (b[i + 2] & 0x3F))
            i += 3
    return out


def units_to_str(units):
    return ''.join(chr(u) for u in units)


class ExceptionEntry:
    __slots__ = ('start', 'end', 'handler', 'catch_type')

    def __init__(self, start, end, handler, catch_type):
        self.start = start
        self.end = end
        self.handler = handler
        self.catch_type = catch_type  # class name or None (catch-all)


class Method:
    def __init__(self, cls, access, name, desc):
        self.cls = cls
        self.access = access
        self.name = name
        self.desc = desc
        self.code = None
        self.max_stack = 0
        self.max_locals = 0
        self.exceptions = []

    @property
    def is_static(self):
        return bool(self.access & ACC_STATIC)

    @property
    def is_abstract(self):
        return bool(self.access & ACC_ABSTRACT)

    @property
    def is_native(self):
        return bool(self.access & ACC_NATIVE)

    @property
    def is_private(self):
        return bool(self.access & ACC_PRIVATE)

    @property
    def is_final(self):
        return bool(self.access & ACC_FINAL)

    @property
    def is_synchronized(self):
        return bool(self.access & ACC_SYNCHRONIZED)

    def __repr__(self):
        return '%s.%s%s' % (self.cls.name, self.name, self.desc)


class Field:
    def __init__(self, cls, access, name, desc):
        self.cls = cls
        self.access = access
        self.name = name
        self.desc = desc
        self.const = None

    @property
    def is_static(self):
        return bool(self.access & ACC_STATIC)


class ClassFile:
    def __init__(self, data):
        r = Reader(data)
        if r.u4() != 0xCAFEBABE:
            raise ValueError('not a class file')
        self.minor = r.u2()
        self.major = r.u2()
        n = r.u2()
        cp = [None] * n
        i = 1
        while i < n:
            tag = r.u1()
            if tag == 1:
                ln = r.u2()
                raw = r.bytes(ln)
                cp[i] = ('Utf8', raw)
            elif tag == 3:
                cp[i] = ('Integer', struct.unpack('>i', r.bytes(4))[0])
            elif tag == 4:
                cp[i] = ('Float', r.bytes(4))
            elif tag == 5:
                cp[i] = ('Long', struct.unpack('>q', r.bytes(8))[0])
                i += 1
            elif tag == 6:
                cp[i] = ('Double', r.bytes(8))
                i += 1
            elif tag == 7:
                cp[i] = ('Class', r.u2())
            elif tag == 8:
                cp[i] = ('String', r.u2())
            elif tag in (9, 10, 11):
                kind = {9: 'Fieldref', 10: 'Methodref', 11: 'InterfaceMethodref'}[tag]
                cp[i] = (kind, r.u2(), r.u2())
            elif tag == 12:
                cp[i] = ('NameAndType', r.u2(), r.u2())
            elif tag == 15:
                cp[i] = ('MethodHandle', r.u1(), r.u2())
            elif tag == 16:
                cp[i] = ('MethodType', r.u2())
            elif tag == 18:
                cp[i] = ('InvokeDynamic', r.u2(), r.u2())
            else:
                raise ValueError('bad constant pool tag %d' % tag)
            i += 1
        self.cp = cp
        self.access = r.u2()
        self.name = self.class_name(r.u2())
        sup = r.u2()
        self.super_name = self.class_name(sup) if sup else None
        self.interfaces = [self.class_name(r.u2()) for _ in range(r.u2())]
        self.fields = []
        for _ in range(r.u2()):
            acc = r.u2()
            f = Field(self, acc, self.utf8(r.u2()), self.utf8(r.u2()))
            for _ in range(r.u2()):
                an = self.utf8(r.u2())
                ln = r.u4()
                body = r.bytes(ln)
                if an == 'ConstantValue':
                    f.const = struct.unpack('>H', body)[0]
            self.fields.append(f)
        self.methods = []
        for _ in range(r.u2()):
            acc = r.u2()
            m = Method(self, acc, self.utf8(r.u2()), self.utf8(r.u2()))
            for _ in range(r.u2()):
                an = self.utf8(r.u2())
                ln = r.u4()
                body = r.bytes(ln)
                if an == 'Code':
                    cr = Reader(body)
                    m.max_stack = cr.u2()
                    m.max_locals = cr.u2()
                    cl = cr.u4()
                    m.code = cr.bytes(cl)
                    for _ in range(cr.u2()):
                        s, e, h, ct = cr.u2(), cr.u2(), cr.u2(), cr.u2()
                        m.exceptions.append(ExceptionEntry(s, e, h, self.class_name(ct) if ct else None))
            self.methods.append(m)

    def utf8(self, idx):
        e = self.cp[idx]
        assert e[0] == 'Utf8'
        return units_to_str(decode_mutf8(e[1]))

    def utf16(self, idx):
        e = self.cp[idx]
        assert e[0] == 'Utf8'
        return decode_mutf8(e[1])

    def class_name(self, idx):
        e = self.cp[idx]
        assert e[0] == 'Class', e
        return self.utf8(e[1])

    def name_and_type(self, idx):
        e = self.cp[idx]
        assert e[0] == 'NameAndType'
        return self.utf8(e[1]), self.utf8(e[2])

    def member_ref(self, idx):
        e = self.cp[idx]
        assert e[0] in ('Fieldref', 'Methodref', 'InterfaceMethodref'), e
        cls = self.class_name(e[1])
        name, desc = self.name_and_type(e[2])
        return e[0], cls, name, desc

    @property
    def is_interface(self):
        return bool(self.access & ACC_INTERFACE)

    @property
    def is_abstract(self):
        return bool(self.access & ACC_ABSTRACT)

    @property
    def is_final(self):
        return bool(self.access & ACC_FINAL)


def parse_method_desc(desc):
    """Return (list of arg type descriptors, return descriptor)."""
    assert desc[0] == '('
    i = 1
    args = []
    while desc[i] != ')':
        j = i
        while desc[j] == '[':
            j += 1
        if desc[j] == 'L':
            j = desc.index(';', j)
        args.append(desc[i:j + 1])
        i = j + 1
    return args, desc[i + 1:]
