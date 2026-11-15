/* Natives for java.lang.*, j2me.VM, j2me.ConsoleOutputStream and j2me.Storage. */
#include <stdio.h>
#include <stdlib.h>
#include "j2c_gen.h"
#include "plat.h"

/* ---- Object ------------------------------------------------------------------------ */

JObject *M_java_lang_Object__getClass____Ljava_lang_Class_2(JObject *self) {
    return jclass_object(self->cls);
}

int32_t M_java_lang_Object__hashCode____I(JObject *self) {
    uintptr_t p = (uintptr_t)self;
    return (int32_t)(p >> 4) ^ (int32_t)(p >> 20);
}

int32_t M_java_lang_System__identityHashCode__Ljava_lang_Object_2__I(JObject *o) {
    return o ? M_java_lang_Object__hashCode____I(o) : 0;
}

/* ---- System ------------------------------------------------------------------------ */

void M_java_lang_System__arraycopy__Ljava_lang_Object_2ILjava_lang_Object_2II__V(JObject *src, int32_t sp,
                                                                                    JObject *dst, int32_t dp,
                                                                                    int32_t n) {
    if (!src || !dst) {
        jthrow_npe();
        return;
    }
    JClass *sc = src->cls, *dc = dst->cls;
    if (!(sc->flags & JCLASS_ARRAY) || !(dc->flags & JCLASS_ARRAY)) {
        jthrow_ase();
        return;
    }
    JArray *s = (JArray *)src, *d = (JArray *)dst;
    if (n < 0 || sp < 0 || dp < 0 || sp > s->length - n || dp > d->length - n) {
        jthrow_aioobe(n < 0 ? n : (sp > s->length - n ? sp + n : dp + n));
        return;
    }
    if (sc->elem_type != dc->elem_type) {
        jthrow_ase();
        return;
    }
    if (sc->elem_type == 'L' && sc != dc && !jclass_assignable(sc->component, dc->component)) {
        JObject **se = JARR(src, JObject *) + sp, **de = JARR(dst, JObject *) + dp;
        for (int32_t i = 0; i < n; i++) {
            if (se[i] && !jinstanceof(se[i], dc->component)) {
                jthrow_ase();
                return;
            }
        }
    }
    size_t es = (size_t)sc->elem_size;
    memmove(JARR(dst, char) + (size_t)dp * es, JARR(src, char) + (size_t)sp * es, (size_t)n * es);
}

int64_t M_java_lang_System__currentTimeMillis____J(void) { return plat_time_ms(); }

void M_java_lang_System__gc____V(void) {
    /* games call System.gc() liberally; a full collection is only worth it now and then */
    static int64_t last;
    int64_t now = plat_ticks_us();
    if (now - last < 1000000) return;
    last = now;
    gc_collect();
}

void M_java_lang_Runtime__exit__I__V(JObject *self, int32_t code) {
    (void)self;
    plat_exit(code);
}

int64_t M_java_lang_Runtime__freeMemory____J(JObject *self) {
    (void)self;
    return (int64_t)gc_free_bytes();
}

int64_t M_java_lang_Runtime__totalMemory____J(JObject *self) {
    (void)self;
    return (int64_t)gc_total_bytes();
}

void M_javax_microedition_midlet_Launcher__exit____V(void) { plat_exit(0); }

/* ---- Float / Double / Math ---------------------------------------------------------- */

int32_t M_java_lang_Float__floatToIntBits__F__I(float f) {
    union { float f; int32_t i; } u;
    if (f != f) return 0x7fc00000;
    u.f = f;
    return u.i;
}

float M_java_lang_Float__intBitsToFloat__I__F(int32_t i) {
    union { float f; int32_t i; } u;
    u.i = i;
    return u.f;
}

int64_t M_java_lang_Double__doubleToLongBits__D__J(double d) {
    union { double d; int64_t l; } u;
    if (d != d) return INT64_C(0x7ff8000000000000);
    u.d = d;
    return u.l;
}

double M_java_lang_Double__longBitsToDouble__J__D(int64_t l) {
    union { double d; int64_t l; } u;
    u.l = l;
    return u.d;
}

double M_java_lang_Math__sin__D__D(double a) { return sin(a); }
double M_java_lang_Math__cos__D__D(double a) { return cos(a); }
double M_java_lang_Math__tan__D__D(double a) { return tan(a); }
double M_java_lang_Math__sqrt__D__D(double a) { return sqrt(a); }
double M_java_lang_Math__ceil__D__D(double a) { return ceil(a); }
double M_java_lang_Math__floor__D__D(double a) { return floor(a); }
double M_java_lang_Math__atan__D__D(double a) { return atan(a); }
double M_java_lang_Math__atan2__DD__D(double y, double x) { return atan2(y, x); }
double M_java_lang_Math__asin__D__D(double a) { return asin(a); }
double M_java_lang_Math__acos__D__D(double a) { return acos(a); }
double M_java_lang_Math__exp__D__D(double a) { return exp(a); }
double M_java_lang_Math__log__D__D(double a) { return log(a); }
double M_java_lang_Math__pow__DD__D(double a, double b) { return pow(a, b); }

/* Java-like formatting: shortest of %.Ng that round-trips, with ".0" for integers. */
JObject *M_j2me_VM__floatToString__DZ__Ljava_lang_String_2(double v, int32_t isDouble) {
    char buf[64];
    if (v != v) return jstring_from_cstr("NaN");
    if (v == J_INF) return jstring_from_cstr("Infinity");
    if (v == -J_INF) return jstring_from_cstr("-Infinity");
    if (v == 0) return jstring_from_cstr(signbit(v) ? "-0.0" : "0.0");
    int maxp = isDouble ? 17 : 9;
    for (int p = 1; p <= maxp; p++) {
        snprintf(buf, sizeof buf, "%.*g", p, v);
        double back = strtod(buf, NULL);
        if (isDouble ? back == v : (float)back == (float)v) break;
    }
    double av = fabs(v);
    if (av >= 1e-3 && av < 1e7) {
        /* plain notation */
        if (strchr(buf, 'e')) {
            double back = strtod(buf, NULL);
            snprintf(buf, sizeof buf, "%.*f", isDouble ? 10 : 6, back);
            char *e = buf + strlen(buf) - 1;
            while (*e == '0') *e-- = 0;
        }
        if (!strchr(buf, '.')) strcat(buf, ".0");
        else if (buf[strlen(buf) - 1] == '.') strcat(buf, "0");
    } else {
        /* scientific: d.dddE[-]x */
        char *e = strchr(buf, 'e');
        char mant[40], out[64];
        int ex = 0;
        if (e) {
            ex = atoi(e + 1);
            *e = 0;
        }
        snprintf(mant, sizeof mant, "%s", buf);
        if (!strchr(mant, '.')) strcat(mant, ".0");
        snprintf(out, sizeof out, "%sE%d", mant, ex);
        return jstring_from_cstr(out);
    }
    return jstring_from_cstr(buf);
}

double M_j2me_VM__parseDouble__Ljava_lang_String_2__D(JObject *s) {
    char buf[128];
    jstring_to_cstr(s, buf, sizeof buf);
    char *end;
    double d = strtod(buf, &end);
    while (*end == 'f' || *end == 'F' || *end == 'd' || *end == 'D') end++;
    if (end == buf || *end != 0) {
        /* NumberFormatException */
        JObject *msg = jstring_from_cstr(buf);
        JObject *e = jnew(&C_java_lang_NumberFormatException);
        if (e) {
            M_java_lang_NumberFormatException___init__Ljava_lang_String_2__V(e, msg);
            if (!J_EXC) jvm_throw(e);
        }
        return 0;
    }
    return d;
}

/* ---- Class -------------------------------------------------------------------------- */

JObject *M_java_lang_Class__forName0__Ljava_lang_String_2__Ljava_lang_Class_2(JObject *name) {
    char buf[256];
    jstring_to_cstr(name, buf, sizeof buf);
    JClass *c = jclass_by_name(buf);
    return c ? jclass_object(c) : NULL;
}

JObject *M_java_lang_Class__getName0____Ljava_lang_String_2(JObject *self) {
    return jstring_from_cstr(jclass_from_object(self)->name);
}

JObject *M_java_lang_Class__newInstance0____Ljava_lang_Object_2(JObject *self) {
    JClass *c = jclass_from_object(self);
    if (!c->new_instance) return NULL;
    if (c->init_state == 0) {
        jclinit(c);
        if (J_EXC) return NULL;
    }
    return c->new_instance();
}

int32_t M_java_lang_Class__isInstance__Ljava_lang_Object_2__Z(JObject *self, JObject *o) {
    return jinstanceof(o, jclass_from_object(self));
}

int32_t M_java_lang_Class__isAssignableFrom__Ljava_lang_Class_2__Z(JObject *self, JObject *other) {
    if (!other) {
        jthrow_npe();
        return 0;
    }
    return jclass_assignable(jclass_from_object(other), jclass_from_object(self));
}

int32_t M_java_lang_Class__isInterface____Z(JObject *self) {
    return (jclass_from_object(self)->flags & JCLASS_INTERFACE) != 0;
}

int32_t M_java_lang_Class__isArray____Z(JObject *self) {
    return (jclass_from_object(self)->flags & JCLASS_ARRAY) != 0;
}

/* ---- resources -------------------------------------------------------------------- */

static int safe_path(const char *p) {
    if (p[0] == '/' || strstr(p, "..")) return 0;
    return 1;
}

JObject *M_j2me_VM__readResource__Ljava_lang_String_2___3B(JObject *jpath) {
    char rel[512], full[768];
    jstring_to_cstr(jpath, rel, sizeof rel);
    if (!safe_path(rel)) return NULL;
    snprintf(full, sizeof full, "%s/%s", plat_data_dir(), rel);
    size_t len;
    uint8_t *data = plat_read_file(full, &len);
    if (!data) return NULL;
    JObject *arr = jnew_array(&C_arr_B, (int32_t)len);
    if (arr) memcpy(JARR(arr, uint8_t), data, len);
    free(data);
    return arr;
}

void M_j2me_ConsoleOutputStream__write0___3BII__V(JObject *b, int32_t off, int32_t len) {
    if (!b) return;
    JArray *a = (JArray *)b;
    if (off < 0 || len < 0 || off > a->length - len) return;
    plat_log((const char *)JARR(b, uint8_t) + off, len);
}

/* ---- record store files ------------------------------------------------------------ */

static void save_path(JObject *name, char *out, size_t size) {
    char n[128];
    jstring_to_cstr(name, n, sizeof n);
    snprintf(out, size, "%s/%s.rms", plat_save_dir(), n);
}

JObject *M_j2me_Storage__read__Ljava_lang_String_2___3B(JObject *name) {
    char path[512];
    save_path(name, path, sizeof path);
    size_t len;
    uint8_t *d = plat_read_file(path, &len);
    if (!d) return NULL;
    JObject *arr = jnew_array(&C_arr_B, (int32_t)len);
    if (arr) memcpy(JARR(arr, uint8_t), d, len);
    free(d);
    return arr;
}

int32_t M_j2me_Storage__write__Ljava_lang_String_2_3B__Z(JObject *name, JObject *data) {
    char path[512];
    save_path(name, path, sizeof path);
    return plat_write_file(path, JARR(data, uint8_t), (size_t)((JArray *)data)->length);
}

int32_t M_j2me_Storage__delete__Ljava_lang_String_2__Z(JObject *name) {
    char path[512];
    save_path(name, path, sizeof path);
    return plat_delete_file(path);
}
