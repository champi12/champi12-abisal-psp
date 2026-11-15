/*
 * Java threads run on native threads but only one executes Java code at a time: a global
 * lock is held while running translated code and released around blocking operations
 * (sleep, monitor contention, waiting). Before releasing it a thread spills its registers
 * and records its stack pointer so the collector can scan it conservatively.
 */
#include <setjmp.h>
#include <stdio.h>
#include <stdlib.h>
#include "j2c_gen.h"
#include "plat.h"

struct JThread {
    JThread *next;
    void *stack_base;  /* highest address of interest (stacks grow down) */
    void *stack_sp;    /* recorded while not holding the global lock */
    jmp_buf regs;
    JObject *jthread;  /* java.lang.Thread instance (may be NULL for the main thread) */
    JObject *saved_exc;
    int running;       /* holds the global lock */
};

static plat_lock *gil;
static JThread *threads;
static JThread *current;

JThread *jthread_current(void) { return current; }

static void register_thread(JThread *t) {
    t->next = threads;
    threads = t;
}

static void unregister_thread(JThread *t) {
    for (JThread **p = &threads; *p; p = &(*p)->next) {
        if (*p == t) {
            *p = t->next;
            return;
        }
    }
}

void jthread_init_main(void *stack_base) {
    gil = plat_lock_create();
    plat_lock_acquire(gil);
    JThread *t = calloc(1, sizeof(JThread));
    t->stack_base = stack_base;
    t->running = 1;
    register_thread(t);
    current = t;
}

static JThread *release(void) __attribute__((noinline));
static JThread *release(void) {
    JThread *t = current;
    volatile char marker = 0;
    setjmp(t->regs);
    t->stack_sp = (void *)&marker;
    t->saved_exc = jvm_exc;
    t->running = 0;
    current = NULL;
    plat_lock_release(gil);
    return t;
}

static void reacquire(JThread *t) {
    plat_lock_acquire(gil);
    current = t;
    t->running = 1;
    jvm_exc = t->saved_exc;
    t->saved_exc = NULL;
}

JThread *jvm_blocking_begin(void) { return release(); }

void jvm_blocking_end(JThread *t) { reacquire(t); }

void jvm_sleep_ms(int64_t ms) {
    JThread *t = release();
    plat_sleep_us(ms <= 0 ? 0 : ms * 1000);
    reacquire(t);
}

void jvm_yield(void) {
    JThread *t = release();
    plat_sleep_us(0);
    reacquire(t);
}

int64_t jvm_time_ms(void) { return plat_time_ms(); }

void jthread_visit_stacks(jvm_stack_visitor fn) {
    jmp_buf here;
    volatile char marker = 0;
    setjmp(here);
    for (JThread *t = threads; t; t = t->next) {
        if (t == current) {
            fn((void *)&here, (char *)&here + sizeof here);
            fn((void *)&marker, t->stack_base);
        } else {
            fn((void *)&t->regs, (char *)&t->regs + sizeof t->regs);
            fn(t->stack_sp, t->stack_base);
            if (t->saved_exc) fn((void *)&t->saved_exc, (char *)&t->saved_exc + sizeof(void *));
        }
        if (t->jthread) fn((void *)&t->jthread, (char *)&t->jthread + sizeof(void *));
    }
}

/* ---- monitors ------------------------------------------------------------------
 * The lock word holds an owner token (index into a small table) and a recursion count.
 * Contention only happens when the owner blocked while holding the monitor.
 */
#define MAX_OWNERS 64
static JThread *owner_table[MAX_OWNERS];

static uint32_t owner_token(JThread *t) {
    for (uint32_t i = 1; i < MAX_OWNERS; i++) {
        if (owner_table[i] == t) return i;
    }
    for (uint32_t i = 1; i < MAX_OWNERS; i++) {
        if (!owner_table[i]) {
            owner_table[i] = t;
            return i;
        }
    }
    return 0;
}

void jmonitor_enter(JObject *o) {
    uint32_t me = owner_token(current);
    for (;;) {
        uint32_t w = o->lock;
        if (w == 0) {
            o->lock = (me << 24) | 1;
            return;
        }
        if ((w >> 24) == me) {
            o->lock = w + 1;
            return;
        }
        jvm_sleep_ms(1);
    }
}

void jmonitor_exit(JObject *o) {
    uint32_t w = o->lock;
    if ((w & 0xffffff) <= 1) {
        o->lock = 0;
    } else {
        o->lock = w - 1;
    }
}

/* Object.wait/notify: waiting releases the monitor and polls a per-object notify counter. */
typedef struct WaitEntry {
    JObject *obj;
    uint32_t notifies;
} WaitEntry;

#define MAX_WAIT 64
static WaitEntry wait_table[MAX_WAIT];

static WaitEntry *wait_entry(JObject *o, int create) {
    for (int i = 0; i < MAX_WAIT; i++) {
        if (wait_table[i].obj == o) return &wait_table[i];
    }
    if (!create) return NULL;
    for (int i = 0; i < MAX_WAIT; i++) {
        if (!wait_table[i].obj) {
            wait_table[i].obj = o;
            wait_table[i].notifies = 0;
            return &wait_table[i];
        }
    }
    return NULL;
}

void M_java_lang_Object__wait__J__V(JObject *self, int64_t timeout) {
    uint32_t w = self->lock;
    WaitEntry *e = wait_entry(self, 1);
    uint32_t start = e ? e->notifies : 0;
    self->lock = 0;
    int64_t t0 = plat_time_ms();
    for (;;) {
        jvm_sleep_ms(2);
        if (e && e->notifies != start) break;
        if (timeout > 0 && plat_time_ms() - t0 >= timeout) break;
    }
    while (self->lock != 0) jvm_sleep_ms(1);
    self->lock = w;
}

void M_java_lang_Object__notify____V(JObject *self) {
    WaitEntry *e = wait_entry(self, 0);
    if (e) e->notifies++;
}

void M_java_lang_Object__notifyAll____V(JObject *self) {
    M_java_lang_Object__notify____V(self);
}

/* ---- java.lang.Thread ------------------------------------------------------------- */

static void thread_main(void *arg) {
    JThread *t = arg;
    volatile char base;
    t->stack_base = (void *)(&base + 64);
    plat_lock_acquire(gil);
    current = t;
    t->running = 1;
    jvm_exc = NULL;
    M_java_lang_Thread__entry__Ljava_lang_Thread_2__V(t->jthread);
    jvm_report_exception("thread");
    unregister_thread(t);
    for (uint32_t i = 1; i < MAX_OWNERS; i++) {
        if (owner_table[i] == t) owner_table[i] = NULL;
    }
    current = NULL;
    plat_lock_release(gil);
    free(t);
}

void M_java_lang_Thread__start0____V(JObject *self) {
    JThread *t = calloc(1, sizeof(JThread));
    t->jthread = self;
    /* not running until it acquires the lock; until then nothing is on its stack */
    t->stack_base = NULL;
    t->stack_sp = NULL;
    register_thread(t);
    ((S_java_lang_Thread *)self)->F_vmThread = (int64_t)(intptr_t)t;
    if (!plat_thread_start(thread_main, t, 256)) {
        unregister_thread(t);
        free(t);
        ((S_java_lang_Thread *)self)->F_alive = 0;
    }
}

JObject *M_java_lang_Thread__currentThread____Ljava_lang_Thread_2(void) {
    JThread *t = current;
    if (!t->jthread) {
        JObject *th = jnew(&C_java_lang_Thread);
        if (!th) return NULL;
        M_java_lang_Thread___init____V(th);
        ((S_java_lang_Thread *)th)->F_alive = 1;
        ((S_java_lang_Thread *)th)->F_started = 1;
        t->jthread = th;
    }
    return t->jthread;
}

void M_java_lang_Thread__sleep__J__V(int64_t ms) { jvm_sleep_ms(ms); }

void M_java_lang_Thread__yield____V(void) { jvm_yield(); }
