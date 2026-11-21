/*
 * Non-moving mark & sweep collector.
 *
 * The heap is one arena split into pages. A page either holds objects of one small size
 * class or is part of a large object. Roots are found conservatively on thread stacks
 * (any word pointing into a live object, including interior pointers) and precisely in
 * static fields, string literals and registered native roots. Heap objects are scanned
 * precisely using the per-class reference-offset tables emitted by j2c.
 */
#include <stdio.h>
#include <stdlib.h>
#include "jvm.h"
#include "plat.h"

#define PAGE_SHIFT 12
#define PAGE_SIZE (1u << PAGE_SHIFT)
#define GRAIN 16

enum { PG_FREE = 0, PG_SMALL = 1, PG_LARGE = 2, PG_LARGE_CONT = 3 };

static const uint16_t size_classes[] = {
    16, 32, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512,
    640, 768, 1024, 1360, 2048,
};
#define N_CLASSES ((int)(sizeof(size_classes) / sizeof(size_classes[0])))
#define SMALL_MAX 2048

typedef struct FreeObj {
    void *zero;            /* overlaps JObject.cls: NULL marks a free slot */
    struct FreeObj *next;
} FreeObj;

static uint8_t *heap_lo, *heap_hi;
static uint32_t n_pages;
static uint8_t *page_kind;
static uint8_t *page_class;
static uint32_t *page_info; /* large: number of pages (head) / head index (cont) */
static uint8_t *mark_bits;
static FreeObj *free_lists[N_CLASSES];
static uint8_t class_of_size[SMALL_MAX / GRAIN + 1];
static size_t bytes_free;
static uint32_t page_cursor;
static int gc_running;

#define MAX_EXTRA_ROOTS 256
static JObject **extra_roots[MAX_EXTRA_ROOTS];
static int n_extra_roots;

/* mark stack */
static JObject **mstack;
static size_t mstack_len, mstack_cap;
static int mstack_overflow;

static inline uint32_t page_of(const void *p) {
    return (uint32_t)(((const uint8_t *)p - heap_lo) >> PAGE_SHIFT);
}
static inline uint8_t *page_addr(uint32_t i) {
    return heap_lo + ((size_t)i << PAGE_SHIFT);
}
static inline size_t grain_index(const void *p) {
    return (size_t)((const uint8_t *)p - heap_lo) / GRAIN;
}
static inline int is_marked(const void *p) {
    size_t g = grain_index(p);
    return (mark_bits[g >> 3] >> (g & 7)) & 1;
}
static inline void set_mark(const void *p) {
    size_t g = grain_index(p);
    mark_bits[g >> 3] |= (uint8_t)(1u << (g & 7));
}

void gc_init(size_t heap_bytes) {
    heap_bytes &= ~(size_t)(PAGE_SIZE - 1);
    /* the tables around the heap also come from malloc: reserve room for them and, if the
     * arena cannot give that much, retry with a smaller heap instead of dying silently
     * (stderr is invisible on the PSP, so every failure goes to devlog.txt) */
    /* Reserve the heap AND its real tables, then check that ~768 KB are still free for
     * what the runtime allocates later (sounds, file buffers). If anything does not fit,
     * release it all and retry 1 MB smaller: a heap that only fits on its own is no use. */
    uint8_t *raw = NULL;
    size_t pedido = heap_bytes;
    int intentos = 0;
    for (;;) {
        intentos++;
        raw = malloc(heap_bytes + PAGE_SIZE);
        uint32_t np = (uint32_t)(heap_bytes >> PAGE_SHIFT);
        page_kind = raw ? calloc(np, 1) : NULL;
        page_class = page_kind ? calloc(np, 1) : NULL;
        page_info = page_class ? calloc(np, sizeof(uint32_t)) : NULL;
        mark_bits = page_info ? calloc(heap_bytes / GRAIN / 8 + 1, 1) : NULL;
        mstack_cap = 16384;
        mstack = mark_bits ? malloc(mstack_cap * sizeof(JObject *)) : NULL;
        void *holgura = mstack ? malloc(768u << 10) : NULL;
        if (holgura) {
            free(holgura);
            break;
        }
        free(mstack);
        free(mark_bits);
        free(page_info);
        free(page_class);
        free(page_kind);
        free(raw);
        mstack = NULL;
        mark_bits = NULL;
        page_info = NULL;
        page_class = NULL;
        page_kind = NULL;
        raw = NULL;
        if (heap_bytes <= (4u << 20)) {
            devlog("gc: ERROR FATAL: ni siquiera %u KB caben en memoria (pedidos %u KB)",
                   (unsigned)(heap_bytes >> 10), (unsigned)(pedido >> 10));
            plat_exit(1);
        }
        /* 1 MB steps: quiet (only mallocs), the result is logged once below */
        heap_bytes -= 1u << 20;
    }
    if (intentos > 1) {
        devlog("gc: %u KB no cabian; ajustado tras %d intentos", (unsigned)(pedido >> 10), intentos);
    }
    devlog("gc: heap reservado, %u KB en %p (tablas y 768 KB de margen comprobados)", (unsigned)(heap_bytes >> 10),
           (void *)raw);
    heap_lo = (uint8_t *)(((uintptr_t)raw + PAGE_SIZE - 1) & ~(uintptr_t)(PAGE_SIZE - 1));
    heap_hi = heap_lo + heap_bytes;
    n_pages = (uint32_t)(heap_bytes >> PAGE_SHIFT);
    for (int s = 0, c = 0; s <= SMALL_MAX; s += GRAIN) {
        while (size_classes[c] < s) c++;
        class_of_size[s / GRAIN] = (uint8_t)c;
    }
    bytes_free = heap_bytes;
}

size_t gc_free_bytes(void) { return bytes_free; }
size_t gc_total_bytes(void) { return (size_t)(heap_hi - heap_lo); }

void gc_add_root(JObject **slot) {
    if (n_extra_roots < MAX_EXTRA_ROOTS) extra_roots[n_extra_roots++] = slot;
}

int gc_is_heap_object(const void *p) {
    return (const uint8_t *)p >= heap_lo && (const uint8_t *)p < heap_hi;
}

/* ---- allocation -------------------------------------------------------------- */

static int find_free_pages(uint32_t n, uint32_t *out) {
    uint32_t start = page_cursor, run = 0, first = 0;
    for (uint32_t k = 0; k < n_pages * 2; k++) {
        uint32_t i = (start + k) % n_pages;
        if (i == 0) run = 0; /* runs cannot wrap around the end of the arena */
        if (page_kind[i] == PG_FREE) {
            if (run == 0) first = i;
            if (++run == n) {
                *out = first;
                page_cursor = first + n;
                if (page_cursor >= n_pages) page_cursor = 0;
                return 1;
            }
        } else {
            run = 0;
        }
    }
    return 0;
}

static int refill(int c) {
    uint32_t pg;
    if (!find_free_pages(1, &pg)) return 0;
    uint32_t sz = size_classes[c];
    page_kind[pg] = PG_SMALL;
    page_class[pg] = (uint8_t)c;
    uint8_t *base = page_addr(pg);
    uint32_t n = PAGE_SIZE / sz;
    FreeObj *head = free_lists[c];
    for (int32_t i = (int32_t)n - 1; i >= 0; i--) {
        FreeObj *f = (FreeObj *)(base + (size_t)i * sz);
        f->zero = NULL;
        f->next = head;
        head = f;
    }
    free_lists[c] = head;
    return 1;
}

static void *alloc_small(size_t size) {
    int c = class_of_size[(size + GRAIN - 1) / GRAIN];
    FreeObj *f = free_lists[c];
    if (!f) {
        if (!refill(c)) return NULL;
        f = free_lists[c];
    }
    free_lists[c] = f->next;
    bytes_free -= size_classes[c];
    memset(f, 0, size_classes[c]);
    return f;
}

static void *alloc_large(size_t size) {
    uint32_t n = (uint32_t)((size + PAGE_SIZE - 1) >> PAGE_SHIFT), pg;
    if (!find_free_pages(n, &pg)) return NULL;
    page_kind[pg] = PG_LARGE;
    page_info[pg] = n;
    for (uint32_t i = 1; i < n; i++) {
        page_kind[pg + i] = PG_LARGE_CONT;
        page_info[pg + i] = pg;
    }
    bytes_free -= (size_t)n << PAGE_SHIFT;
    void *p = page_addr(pg);
    memset(p, 0, (size_t)n << PAGE_SHIFT);
    return p;
}


#ifdef GC_PROF
/* PC-only allocation profiler: which C function (translated Java method) allocates */
#include <execinfo.h>
#define PROF_N 4096
static void *prof_pc[PROF_N];
static uint32_t prof_bytes[PROF_N], prof_cnt[PROF_N];
static int prof_on = -1;
static void prof_dump(void) {
    char **sym;
    for (int k = 0; k < 40; k++) {
        int best = -1;
        for (int i = 0; i < PROF_N; i++)
            if (prof_pc[i] && (best < 0 || prof_bytes[i] > prof_bytes[best])) best = i;
        if (best < 0) break;
        sym = backtrace_symbols(&prof_pc[best], 1);
        fprintf(stderr, "PROF %8u KB %8u allocs  %s\n", prof_bytes[best] >> 10, prof_cnt[best], sym ? sym[0] : "?");
        prof_bytes[best] = 0;
        prof_pc[best] = (void *)1 == prof_pc[best] ? 0 : prof_pc[best];
        prof_pc[best] = 0;
    }
}
static void prof_note(size_t size) {
    if (prof_on < 0) { prof_on = getenv("GC_PROF") != NULL; }
    if (!prof_on) return;
    void *bt[6];
    int n = backtrace(bt, 6);
    void *pc = n > 3 ? bt[3] : NULL;
    unsigned h = ((uintptr_t)pc >> 2) % PROF_N;
    while (prof_pc[h] && prof_pc[h] != pc) h = (h + 1) % PROF_N;
    prof_pc[h] = pc;
    prof_bytes[h] += (uint32_t)size;
    prof_cnt[h]++;
}
void gc_prof_dump(void) { if (prof_on > 0) prof_dump(); }
#else
#define prof_note(s) ((void)0)
void gc_prof_dump(void) {}
#endif

/* statistics for the devlog: collections, time spent and bytes allocated */
uint32_t gc_stat_count, gc_stat_ms_total, gc_stat_ms_max, gc_stat_kb_alloc;
static uint32_t gc_stat_bytes;

static void *gc_alloc(size_t size) {
    if (size < GRAIN) size = GRAIN;
    prof_note(size);
    gc_stat_bytes += (uint32_t)size;
    if (gc_stat_bytes >= 1024) {
        gc_stat_kb_alloc += gc_stat_bytes >> 10;
        gc_stat_bytes &= 1023;
    }
    void *p = size <= SMALL_MAX ? alloc_small(size) : alloc_large(size);
    if (!p && !gc_running) {
        gc_collect();
        p = size <= SMALL_MAX ? alloc_small(size) : alloc_large(size);
    }
    return p;
}

JObject *jnew(JClass *c) {
    JObject *o = gc_alloc((size_t)c->instance_size);
    if (UNLIKELY(!o)) {
        jthrow_oome();
        return NULL;
    }
    o->cls = c;
    return o;
}

JObject *jnew_array(JClass *ac, int32_t length) {
    if (UNLIKELY(length < 0)) {
        jthrow_nase();
        return NULL;
    }
    size_t bytes = JARR_HDR + (size_t)length * (size_t)ac->elem_size;
    if (UNLIKELY(bytes > (size_t)(heap_hi - heap_lo))) {
        jthrow_oome();
        return NULL;
    }
    JArray *a = gc_alloc(bytes);
    if (UNLIKELY(!a)) {
        jthrow_oome();
        return NULL;
    }
    a->cls = ac;
    a->length = length;
    return (JObject *)a;
}

JObject *jnew_multiarray(JClass *ac, int dims, const int32_t *counts) {
    for (int i = 0; i < dims; i++) {
        if (counts[i] < 0) {
            jthrow_nase();
            return NULL;
        }
    }
    JObject *a = jnew_array(ac, counts[0]);
    if (!a || dims == 1) return a;
    for (int32_t i = 0; i < counts[0]; i++) {
        /* `a` stays reachable from this C frame (conservative stack scan) */
        JObject *sub = jnew_multiarray(ac->component, dims - 1, counts + 1);
        if (!sub) return NULL;
        JARR(a, JObject *)[i] = sub;
    }
    return a;
}

JObject *jarray_clone(JObject *src) {
    JArray *s = (JArray *)src;
    JObject *d = jnew_array(s->cls, s->length);
    if (d) memcpy(JARR(d, char), JARR(src, char), (size_t)s->length * (size_t)s->cls->elem_size);
    return d;
}

/* ---- marking ----------------------------------------------------------------- */

/* Map an arbitrary address to the start of the live object containing it. */
static JObject *object_at(const void *p) {
    if ((const uint8_t *)p < heap_lo || (const uint8_t *)p >= heap_hi) return NULL;
    uint32_t pg = page_of(p);
    uint8_t k = page_kind[pg];
    uint8_t *start;
    if (k == PG_SMALL) {
        uint32_t sz = size_classes[page_class[pg]];
        uint8_t *base = page_addr(pg);
        size_t idx = (size_t)((const uint8_t *)p - base) / sz;
        if ((idx + 1) * sz > PAGE_SIZE) return NULL; /* page tail slack */
        start = base + idx * sz;
    } else if (k == PG_LARGE) {
        start = page_addr(pg);
    } else if (k == PG_LARGE_CONT) {
        start = page_addr(page_info[pg]);
    } else {
        return NULL;
    }
    JObject *o = (JObject *)start;
    return o->cls ? o : NULL;
}

static void push(JObject *o) {
    if (is_marked(o)) return;
    set_mark(o);
    if (mstack_len == mstack_cap) {
        JObject **n = realloc(mstack, mstack_cap * 2 * sizeof(JObject *));
        if (!n) {
            mstack_overflow = 1;
            return;
        }
        mstack = n;
        mstack_cap *= 2;
    }
    mstack[mstack_len++] = o;
}

static inline void mark_ref(JObject *r) {
    if (r && (uint8_t *)r >= heap_lo && (uint8_t *)r < heap_hi && r->cls) push(r);
}

static void scan_object(JObject *o) {
    JClass *c = o->cls;
    if (c->flags & JCLASS_ARRAY) {
        if (c->elem_type == 'L') {
            JArray *a = (JArray *)o;
            JObject **e = JARR(o, JObject *);
            for (int32_t i = 0; i < a->length; i++) mark_ref(e[i]);
        }
        return;
    }
    for (int32_t i = 0; i < c->n_refs; i++) mark_ref(*(JObject **)((char *)o + c->ref_offsets[i]));
}

static void drain(void) {
    while (mstack_len) scan_object(mstack[--mstack_len]);
}

static void scan_range(void *lo, void *hi) {
    uintptr_t a = ((uintptr_t)lo + sizeof(void *) - 1) & ~(uintptr_t)(sizeof(void *) - 1);
    for (void **p = (void **)a; (void *)(p + 1) <= hi; p++) {
        void *v = *p;
        if ((uint8_t *)v < heap_lo || (uint8_t *)v >= heap_hi) continue;
        JObject *o = object_at(v);
        if (o) push(o);
        /* a pointer just past the end of an object may be all that is left of it */
        if ((uint8_t *)v > heap_lo) {
            JObject *prev = object_at((uint8_t *)v - 1);
            if (prev && prev != o) push(prev);
        }
    }
}

static void rescan_overflow(void) {
    /* rare: mark stack could not grow; rescan marked objects until stable */
    while (mstack_overflow) {
        mstack_overflow = 0;
        for (uint32_t pg = 0; pg < n_pages; pg++) {
            if (page_kind[pg] == PG_SMALL) {
                uint32_t sz = size_classes[page_class[pg]];
                for (uint32_t off = 0; off + sz <= PAGE_SIZE; off += sz) {
                    JObject *o = (JObject *)(page_addr(pg) + off);
                    if (o->cls && is_marked(o)) {
                        scan_object(o);
                        drain();
                    }
                }
            } else if (page_kind[pg] == PG_LARGE) {
                JObject *o = (JObject *)page_addr(pg);
                if (o->cls && is_marked(o)) {
                    scan_object(o);
                    drain();
                }
            }
        }
    }
}

extern JObject *jvm_runtime_roots_visit(void (*fn)(JObject *));

static void root_cb(JObject *o) { mark_ref(o); }

static void mark_roots(void) {
    for (JObject **const *r = jvm_static_roots; *r; r++) mark_ref(**r);
    for (int i = 0; i < jvm_n_strings; i++) mark_ref(jvm_strings[i]);
    for (int i = 0; i < n_extra_roots; i++) mark_ref(*extra_roots[i]);
    mark_ref(jvm_exc);
    jvm_runtime_roots_visit(root_cb);
    drain();
    jthread_visit_stacks(scan_range);
    drain();
    rescan_overflow();
}

/* ---- sweeping ----------------------------------------------------------------- */

static void sweep(void) {
    for (int c = 0; c < N_CLASSES; c++) free_lists[c] = NULL;
    size_t freed_total = 0;
    for (uint32_t pg = 0; pg < n_pages; pg++) {
        uint8_t k = page_kind[pg];
        if (k == PG_SMALL) {
            int c = page_class[pg];
            uint32_t sz = size_classes[c];
            uint8_t *base = page_addr(pg);
            uint32_t n = PAGE_SIZE / sz, live = 0;
            for (uint32_t i = 0; i < n; i++) {
                JObject *o = (JObject *)(base + (size_t)i * sz);
                if (o->cls && is_marked(o)) live++;
            }
            if (live == 0) {
                page_kind[pg] = PG_FREE;
                continue;
            }
            for (int32_t i = (int32_t)n - 1; i >= 0; i--) {
                JObject *o = (JObject *)(base + (size_t)i * sz);
                if (o->cls && is_marked(o)) continue;
                FreeObj *f = (FreeObj *)o;
                f->zero = NULL;
                f->next = free_lists[c];
                free_lists[c] = f;
                freed_total += sz;
            }
        } else if (k == PG_LARGE) {
            JObject *o = (JObject *)page_addr(pg);
            if (!is_marked(o)) {
                uint32_t n = page_info[pg];
                o->cls = NULL;
                for (uint32_t i = 0; i < n; i++) page_kind[pg + i] = PG_FREE;
            }
        }
    }
    (void)freed_total;
    size_t used = 0;
    for (uint32_t pg = 0; pg < n_pages; pg++) {
        if (page_kind[pg] != PG_FREE) used += PAGE_SIZE;
    }
    for (int c = 0; c < N_CLASSES; c++) {
        for (FreeObj *f = free_lists[c]; f; f = f->next) used -= size_classes[c];
    }
    bytes_free = (size_t)(heap_hi - heap_lo) - used;
}

void gc_collect(void) {
    if (gc_running) return;
    gc_running = 1;
    int64_t t0 = plat_ticks_us();
    plat_gpu_sync();
    memset(mark_bits, 0, (size_t)(heap_hi - heap_lo) / GRAIN / 8 + 1);
    mark_roots();
    sweep();
    gc_running = 0;
    uint32_t ms = (uint32_t)((plat_ticks_us() - t0) / 1000);
    gc_stat_count++;
    gc_stat_ms_total += ms;
    if (ms > gc_stat_ms_max) gc_stat_ms_max = ms;
}
