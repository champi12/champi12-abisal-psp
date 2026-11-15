/*
 * Core runtime interface shared by j2c-generated code and the hand-written natives.
 *
 * Object model: every object starts with a JObject header (class pointer + lock word).
 * Arrays add a length and keep their elements 16-byte aligned so the PSP GE can read
 * pixel/vertex arrays directly. Exceptions are propagated with a pending-exception
 * pointer (J_EXC) that generated code checks after every call that may throw.
 */
#ifndef JVM_H
#define JVM_H

#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include <math.h>

#define UNLIKELY(x) __builtin_expect(!!(x), 0)
#define LIKELY(x) __builtin_expect(!!(x), 1)

typedef struct JClass JClass;

typedef struct JObject {
    JClass *cls;
    uint32_t lock;
} JObject;

typedef struct JArray {
    JClass *cls;
    uint32_t lock;
    int32_t length;
    int32_t pad;
} JArray;

#define JARR_HDR 16
#define JARR(a, T) ((T *)((char *)(a) + JARR_HDR))

typedef struct JIMethod {
    int32_t id;
    void *fn;
} JIMethod;

typedef struct JStrLit {
    const uint16_t *chars;
    int32_t len;
} JStrLit;

enum {
    JCLASS_INTERFACE = 1,
    JCLASS_ABSTRACT = 2,
    JCLASS_ARRAY = 4,
    JCLASS_PRIMITIVE = 8,
};

struct JClass {
    const char *name;
    JClass *super;
    uint32_t flags;
    int32_t instance_size;
    void *const *vtable;
    int32_t vtable_len;
    JClass *const *interfaces;
    int32_t n_interfaces;
    const JIMethod *imethods;
    int32_t n_imethods;
    const uint16_t *ref_offsets;
    int32_t n_refs;
    void (*clinit)(void);
    volatile int32_t init_state; /* 0 = not initialized, 1 = running, 2 = done */
    JObject *(*new_instance)(void);
    /* arrays */
    JClass *component;
    JClass *array_class;
    int32_t elem_size;
    char elem_type; /* 'Z','B','C','S','I','J','F','D' or 'L' */
    /* runtime */
    JObject *class_obj;
};

/* ---- generated tables ---------------------------------------------------- */
extern JClass *const jvm_classes[];
extern JObject **const jvm_static_roots[];
extern const JStrLit jvm_string_lits[];
extern const int jvm_n_strings;
extern JObject *jvm_strings[];

extern JClass C_arr_Z, C_arr_B, C_arr_C, C_arr_S, C_arr_I, C_arr_J, C_arr_F, C_arr_D;

/* ---- exceptions ------------------------------------------------------------- */
extern JObject *jvm_exc;
#define J_EXC jvm_exc

static inline JObject *jvm_take_exc(void) {
    JObject *e = jvm_exc;
    jvm_exc = NULL;
    return e;
}

void jvm_throw(JObject *ex); /* NULL -> NullPointerException */
void jthrow_npe(void);
void jthrow_aioobe(int32_t index);
void jthrow_arith(void);
void jthrow_cce(JObject *obj, JClass *target);
void jthrow_nase(void);
void jthrow_ase(void);
void jthrow_oome(void);
void jvm_abstract_method(void);

#define NULL_CHK(o, L) \
    do { if (UNLIKELY(!(o))) { jthrow_npe(); goto L; } } while (0)
#define ARR_CHK(a, i, L) \
    do { \
        if (UNLIKELY(!(a))) { jthrow_npe(); goto L; } \
        if (UNLIKELY((uint32_t)(i) >= (uint32_t)((JArray *)(a))->length)) { jthrow_aioobe(i); goto L; } \
    } while (0)
#define AASTORE_CHK(a, v, L) \
    do { \
        if ((v) && UNLIKELY(!jinstanceof((v), ((JArray *)(a))->cls->component))) { jthrow_ase(); goto L; } \
    } while (0)

/* ---- classes ------------------------------------------------------------------ */
void jclinit(JClass *c);
#define JINIT(c, L) \
    do { if (UNLIKELY((c)->init_state == 0)) { jclinit(c); if (UNLIKELY(J_EXC)) goto L; } } while (0)

int jinstanceof(JObject *o, JClass *c);
int jclass_assignable(JClass *from, JClass *to);
JClass *jarray_class(JClass *component);
JObject *jclass_object(JClass *c);
JClass *jclass_from_object(JObject *classObj);
JClass *jclass_by_name(const char *internal_name);

#define JVCALL(o, idx) (((JObject *)(o))->cls->vtable[(idx)])
void *jfind_imethod(JObject *o, int32_t id);

/* ---- allocation ------------------------------------------------------------- */
JObject *jnew(JClass *c);
JObject *jnew_array(JClass *arrayClass, int32_t length);
JObject *jnew_multiarray(JClass *arrayClass, int dims, const int32_t *counts);
JObject *jarray_clone(JObject *a);

void gc_init(size_t heap_bytes);
void gc_collect(void);
size_t gc_free_bytes(void);
size_t gc_total_bytes(void);
/* Extra GC roots registered by natives (e.g. the current display). */
void gc_add_root(JObject **slot);
int gc_is_heap_object(const void *p);

/* ---- monitors / threads ------------------------------------------------------- */
void jmonitor_enter(JObject *o);
void jmonitor_exit(JObject *o);

typedef struct JThread JThread;
void jthread_init_main(void *stack_base);
JThread *jthread_current(void);
/* Release the global lock around a blocking native operation. */
JThread *jvm_blocking_begin(void);
void jvm_blocking_end(JThread *t);
void jvm_sleep_ms(int64_t ms);
void jvm_yield(void);
int64_t jvm_time_ms(void);
/* Called by GC to collect the stacks of all threads. */
typedef void (*jvm_stack_visitor)(void *lo, void *hi);
void jthread_visit_stacks(jvm_stack_visitor fn);

/* ---- strings ---------------------------------------------------------------- */
JObject *jstring_from_utf16(const uint16_t *chars, int32_t len);
JObject *jstring_from_cstr(const char *s);
/* Copies a Java string into buf as UTF-8 (truncating); returns buf. */
char *jstring_to_cstr(JObject *s, char *buf, size_t size);
int32_t jstring_length(JObject *s);

/* ---- startup ---------------------------------------------------------------- */
void jvm_init(void *stack_base, size_t heap_bytes);
void jvm_report_exception(const char *where);

/* ---- numeric helpers (Java semantics) ---------------------------------------- */
#define J_NANF (__builtin_nanf(""))
#define J_INFF (__builtin_inff())
#define J_NAN (__builtin_nan(""))
#define J_INF (__builtin_inf())

static inline int32_t j_f2i(float f) {
    if (f != f) return 0;
    if (f >= 2147483647.0f) return INT32_MAX;
    if (f <= -2147483648.0f) return INT32_MIN;
    return (int32_t)f;
}
static inline int64_t j_f2l(float f) {
    if (f != f) return 0;
    if (f >= 9223372036854775807.0f) return INT64_MAX;
    if (f <= -9223372036854775808.0f) return INT64_MIN;
    return (int64_t)f;
}
static inline int32_t j_d2i(double d) {
    if (d != d) return 0;
    if (d >= 2147483647.0) return INT32_MAX;
    if (d <= -2147483648.0) return INT32_MIN;
    return (int32_t)d;
}
static inline int64_t j_d2l(double d) {
    if (d != d) return 0;
    if (d >= 9223372036854775807.0) return INT64_MAX;
    if (d <= -9223372036854775808.0) return INT64_MIN;
    return (int64_t)d;
}

/* ---- platform hooks implemented per target (runtime/platform/<target>) ------- */
void plat_log(const char *s, int len);
void plat_gpu_sync(void); /* make sure the GPU no longer reads Java heap memory */
const char *plat_data_dir(void);
const char *plat_save_dir(void);
void plat_debug_trace(const char *what); /* optional diagnostics when an exception is raised */

#endif
