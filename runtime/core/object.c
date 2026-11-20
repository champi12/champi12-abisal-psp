/* Classes, type checks, exceptions, strings and VM startup. */
#include <stdio.h>
#include <stdlib.h>
#include "j2c_gen.h"
#include "plat.h"

JObject *jvm_exc;

#define PRIM_ARRAY(T, SZ) \
    JClass C_arr_##T = { .name = "[" #T, .super = &C_java_lang_Object, .flags = JCLASS_ARRAY, \
                         .vtable = NULL, .init_state = 2, .elem_size = SZ, .elem_type = #T[0] };
PRIM_ARRAY(Z, 1)
PRIM_ARRAY(B, 1)
PRIM_ARRAY(C, 2)
PRIM_ARRAY(S, 2)
PRIM_ARRAY(I, 4)
PRIM_ARRAY(J, 8)
PRIM_ARRAY(F, 4)
PRIM_ARRAY(D, 8)

static JClass *const prim_arrays[] = { &C_arr_Z, &C_arr_B, &C_arr_C, &C_arr_S,
                                       &C_arr_I, &C_arr_J, &C_arr_F, &C_arr_D };

/* runtime-created array classes, kept for GC root scanning */
#define MAX_ARRAY_CLASSES 512
static JClass *array_classes[MAX_ARRAY_CLASSES];
static int n_array_classes;

/* ---- class init ---------------------------------------------------------------- */

void jclinit(JClass *c) {
    if (c->init_state != 0) return;
    c->init_state = 1;
    if (c->super && c->super->init_state == 0) {
        jclinit(c->super);
        if (J_EXC) return;
    }
    if (c->clinit) c->clinit();
    c->init_state = 2;
}

/* ---- type checks -------------------------------------------------------------- */

int jclass_assignable(JClass *from, JClass *to) {
    if (from == to || to == &C_java_lang_Object) return 1;
    if (to->flags & JCLASS_ARRAY) {
        if (!(from->flags & JCLASS_ARRAY)) return 0;
        if (to->elem_type != 'L' || from->elem_type != 'L') return from->elem_type == to->elem_type && from == to;
        return jclass_assignable(from->component, to->component);
    }
    if (to->flags & JCLASS_INTERFACE) {
        for (int i = 0; i < from->n_interfaces; i++) {
            if (from->interfaces[i] == to) return 1;
        }
        return 0;
    }
    for (JClass *c = from->super; c; c = c->super) {
        if (c == to) return 1;
    }
    return 0;
}

int jinstanceof(JObject *o, JClass *c) {
    if (!o) return 0;
    return jclass_assignable(o->cls, c);
}

void *jfind_imethod(JObject *o, int32_t id) {
    JClass *c = o->cls;
    const JIMethod *m = c->imethods;
    int lo = 0, hi = c->n_imethods - 1;
    while (lo <= hi) {
        int mid = (lo + hi) >> 1;
        if (m[mid].id == id) return m[mid].fn;
        if (m[mid].id < id) lo = mid + 1;
        else hi = mid - 1;
    }
    return (void *)jvm_abstract_method;
}

JClass *jarray_class(JClass *comp) {
    if (comp->array_class) return comp->array_class;
    JClass *a = calloc(1, sizeof(JClass));
    size_t n = strlen(comp->name);
    char *name = malloc(n + 4);
    if (comp->flags & JCLASS_ARRAY) {
        sprintf(name, "[%s", comp->name);
    } else {
        sprintf(name, "[L%s;", comp->name);
    }
    a->name = name;
    a->super = &C_java_lang_Object;
    a->flags = JCLASS_ARRAY;
    a->vtable = C_java_lang_Object.vtable;
    a->vtable_len = C_java_lang_Object.vtable_len;
    a->init_state = 2;
    a->component = comp;
    a->elem_size = sizeof(JObject *);
    a->elem_type = 'L';
    comp->array_class = a;
    if (n_array_classes < MAX_ARRAY_CLASSES) array_classes[n_array_classes++] = a;
    return a;
}

/* ---- java.lang.Class objects ---------------------------------------------------- */

JObject *jclass_object(JClass *c) {
    if (c->class_obj) return c->class_obj;
    JObject *o = jnew(&C_java_lang_Class);
    if (!o) return NULL;
    ((S_java_lang_Class *)o)->F_vmClass = (int64_t)(intptr_t)c;
    c->class_obj = o;
    return o;
}

JClass *jclass_from_object(JObject *o) {
    return (JClass *)(intptr_t)((S_java_lang_Class *)o)->F_vmClass;
}

JClass *jclass_by_name(const char *name) {
    for (JClass *const *c = jvm_classes; *c; c++) {
        if (strcmp((*c)->name, name) == 0) return *c;
    }
    if (name[0] == '[') {
        const char *e = name + 1;
        if (*e == 'L') {
            char buf[256];
            size_t n = strlen(e + 1);
            if (n == 0 || n >= sizeof buf) return NULL;
            memcpy(buf, e + 1, n - 1);
            buf[n - 1] = 0;
            JClass *c = jclass_by_name(buf);
            return c ? jarray_class(c) : NULL;
        }
        if (*e == '[') {
            JClass *c = jclass_by_name(e);
            return c ? jarray_class(c) : NULL;
        }
        for (int i = 0; i < 8; i++) {
            if (prim_arrays[i]->elem_type == *e && e[1] == 0) return prim_arrays[i];
        }
    }
    return NULL;
}

/* GC: class objects of all classes are roots */
JObject *jvm_runtime_roots_visit(void (*fn)(JObject *)) {
    for (JClass *const *c = jvm_classes; *c; c++) {
        if ((*c)->class_obj) fn((*c)->class_obj);
    }
    for (int i = 0; i < 8; i++) {
        if (prim_arrays[i]->class_obj) fn(prim_arrays[i]->class_obj);
    }
    for (int i = 0; i < n_array_classes; i++) {
        if (array_classes[i]->class_obj) fn(array_classes[i]->class_obj);
    }
    return NULL;
}

/* ---- exceptions ----------------------------------------------------------------- */

void jvm_throw(JObject *ex) {
    if (ex) plat_debug_trace(ex->cls->name);
    if (!ex) {
        jthrow_npe();
        return;
    }
    jvm_exc = ex;
}

static int creating_exception;

static void throw_new(JObject *(*make)(void)) {
    plat_debug_trace("runtime exception");
    if (creating_exception) {
        /* failure while building an exception object: give up on this one */
        return;
    }
    creating_exception = 1;
    JObject *saved = jvm_exc;
    jvm_exc = NULL;
    JObject *e = make();
    if (!jvm_exc) jvm_exc = e ? e : saved;
    creating_exception = 0;
}

void jthrow_npe(void) { throw_new(M_j2me_VM__npe____Ljava_lang_Throwable_2); }
void jthrow_arith(void) { throw_new(M_j2me_VM__arith____Ljava_lang_Throwable_2); }
void jthrow_nase(void) { throw_new(M_j2me_VM__nase____Ljava_lang_Throwable_2); }
void jthrow_ase(void) { throw_new(M_j2me_VM__ase____Ljava_lang_Throwable_2); }
void jvm_abstract_method(void) { throw_new(M_j2me_VM__ame____Ljava_lang_Throwable_2); }

static int32_t aioobe_index;
static JObject *make_aioobe(void) { return M_j2me_VM__aioobe__I__Ljava_lang_Throwable_2(aioobe_index); }
void jthrow_aioobe(int32_t index) {
    aioobe_index = index;
    throw_new(make_aioobe);
}

static char cce_msg[200];
static JObject *make_cce(void) {
    JObject *s = jstring_from_cstr(cce_msg);
    if (!s) return NULL;
    return M_j2me_VM__cce__Ljava_lang_String_2__Ljava_lang_Throwable_2(s);
}
void jthrow_cce(JObject *obj, JClass *target) {
    snprintf(cce_msg, sizeof cce_msg, "%s cannot be cast to %s", obj->cls->name, target->name);
    throw_new(make_cce);
}

/* preallocated at startup in jvm_init: no allocation is possible when the heap is full */
static JObject *oome_instance;

void jthrow_oome(void) {
    jvm_exc = oome_instance;
}

void jvm_report_exception(const char *where) {
    JObject *e = jvm_exc;
    if (!e) return;
    jvm_exc = NULL;
    char buf[300];
    JObject *s = M_j2me_VM__describe__Ljava_lang_Throwable_2__Ljava_lang_String_2(e);
    jvm_exc = NULL;
    if (s) {
        jstring_to_cstr(s, buf, sizeof buf);
    } else {
        snprintf(buf, sizeof buf, "%s", e->cls->name);
    }
    char line[400];
    int n = snprintf(line, sizeof line, "uncaught exception in %s: %s\n", where, buf);
    plat_log(line, n);
}

/* ---- strings -------------------------------------------------------------------- */

JObject *jstring_from_utf16(const uint16_t *chars, int32_t len) {
    JObject *v = jnew_array(&C_arr_C, len);
    if (!v) return NULL;
    memcpy(JARR(v, uint16_t), chars, (size_t)len * 2);
    JObject *s = jnew(&C_java_lang_String);
    if (!s) return NULL;
    S_java_lang_String *str = (S_java_lang_String *)s;
    str->F_value = v;
    str->F_offset = 0;
    str->F_count = len;
    return s;
}

JObject *jstring_from_cstr(const char *s) {
    size_t n = strlen(s);
    uint16_t stackbuf[256];
    uint16_t *u = n <= 256 ? stackbuf : malloc(n * 2);
    int32_t k = 0;
    for (size_t i = 0; i < n;) {
        unsigned char c = (unsigned char)s[i];
        if (c < 0x80) {
            u[k++] = c;
            i++;
        } else if ((c & 0xe0) == 0xc0 && i + 1 < n) {
            u[k++] = (uint16_t)(((c & 0x1f) << 6) | (s[i + 1] & 0x3f));
            i += 2;
        } else if ((c & 0xf0) == 0xe0 && i + 2 < n) {
            u[k++] = (uint16_t)(((c & 0x0f) << 12) | ((s[i + 1] & 0x3f) << 6) | (s[i + 2] & 0x3f));
            i += 3;
        } else {
            u[k++] = c;
            i++;
        }
    }
    JObject *r = jstring_from_utf16(u, k);
    if (u != stackbuf) free(u);
    return r;
}

int32_t jstring_length(JObject *s) {
    return ((S_java_lang_String *)s)->F_count;
}

char *jstring_to_cstr(JObject *s, char *buf, size_t size) {
    size_t p = 0;
    if (!s) {
        if (size) buf[0] = 0;
        return buf;
    }
    S_java_lang_String *str = (S_java_lang_String *)s;
    const uint16_t *c = JARR(str->F_value, uint16_t) + str->F_offset;
    for (int32_t i = 0; i < str->F_count; i++) {
        uint16_t ch = c[i];
        if (ch < 0x80) {
            if (p + 1 >= size) break;
            buf[p++] = (char)ch;
        } else if (ch < 0x800) {
            if (p + 2 >= size) break;
            buf[p++] = (char)(0xc0 | (ch >> 6));
            buf[p++] = (char)(0x80 | (ch & 0x3f));
        } else {
            if (p + 3 >= size) break;
            buf[p++] = (char)(0xe0 | (ch >> 12));
            buf[p++] = (char)(0x80 | ((ch >> 6) & 0x3f));
            buf[p++] = (char)(0x80 | (ch & 0x3f));
        }
    }
    if (size) buf[p] = 0;
    return buf;
}

/* ---- startup -------------------------------------------------------------------- */

void jvm_init(void *stack_base, size_t heap_bytes) {
    devlog("jvm_init: gc_init (%u KB)", (unsigned)(heap_bytes >> 10));
    gc_init(heap_bytes);
    devlog("jvm_init: gc listo, %u KB libres de %u KB", (unsigned)(gc_free_bytes() >> 10),
           (unsigned)(gc_total_bytes() >> 10));
    jthread_init_main(stack_base);
    devlog("jvm_init: hilo principal listo; creando %d cadenas", jvm_n_strings);
    for (int i = 0; i < jvm_n_strings; i++) {
        jvm_strings[i] = jstring_from_utf16(jvm_string_lits[i].chars, jvm_string_lits[i].len);
        if (!jvm_strings[i]) devlog("jvm_init: ERROR cadena %d no creada", i);
    }
    devlog("jvm_init: cadenas listas; jclinit(Object)");
    jclinit(&C_java_lang_Object);
    devlog("jvm_init: creando OutOfMemoryError");
    oome_instance = M_j2me_VM__oome____Ljava_lang_Throwable_2();
    gc_add_root(&oome_instance);
    jvm_exc = NULL;
    devlog("jvm_init: terminado");
}
