/* PSP platform: kernel threads, Memory Stick files, controls, audio output and startup. */
#include <pspaudio.h>
#include <pspctrl.h>
#include <pspdisplay.h>
#include <pspkernel.h>
#include <psppower.h>
#include <psputils.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include "jvm.h"
#include "mixer.h"
#include "plat.h"
#include "render.h"

PSP_MODULE_INFO("Deep3D", PSP_MODULE_USER, 1, 0);
PSP_MAIN_THREAD_ATTR(THREAD_ATTR_USER | THREAD_ATTR_VFPU);
PSP_MAIN_THREAD_STACK_SIZE_KB(512);
/* leave a few MB outside the C heap for thread stacks and kernel allocations */
PSP_HEAP_SIZE_KB(-4096);

#define JAVA_THREAD_PRIO 0x20
#define AUDIO_THREAD_PRIO 0x12

static char base_dir[256] = "ms0:/PSP/GAME/ABISAL";
static char data_dir[300];
static char save_dir[300];

/* ---- events ----------------------------------------------------------------- */

static volatile int ev_queue[64];
static volatile int ev_head, ev_tail;

static void push_event(int ev) {
    int n = (ev_tail + 1) % 64;
    if (n == ev_head) return;
    ev_queue[ev_tail] = ev;
    ev_tail = n;
}

static int exit_callback(int arg1, int arg2, void *common) {
    (void)arg1;
    (void)arg2;
    (void)common;
    push_event(4 << 24);
    return 0;
}

static int callback_thread(SceSize args, void *argp) {
    (void)args;
    (void)argp;
    int cbid = sceKernelCreateCallback("Exit Callback", exit_callback, NULL);
    sceKernelRegisterExitCallback(cbid);
    sceKernelSleepThreadCB();
    return 0;
}

/*
 * Button mapping to MIDP key codes (see README for the in-game meaning):
 *   D-pad / analog stick -> directions, X -> fire/select, O -> right soft key (back),
 *   START -> left soft key (menu), TRIANGLE -> 0, SQUARE -> 9, L -> 1, R -> 3,
 *   SELECT -> #, SELECT+L -> 7, SELECT+R -> *.
 */
typedef struct KeyMap {
    unsigned int button;
    int code;
} KeyMap;

#define VBTN_UP 0x10000000
#define VBTN_DOWN 0x20000000
#define VBTN_LEFT 0x40000000
#define VBTN_RIGHT 0x80000000u

static const KeyMap keymap[] = {
    { PSP_CTRL_UP | VBTN_UP, -1 },
    { PSP_CTRL_DOWN | VBTN_DOWN, -2 },
    { PSP_CTRL_LEFT | VBTN_LEFT, -3 },
    { PSP_CTRL_RIGHT | VBTN_RIGHT, -4 },
    { PSP_CTRL_CROSS, -5 },
    { PSP_CTRL_START, -6 },
    { PSP_CTRL_CIRCLE, -7 },
    { PSP_CTRL_TRIANGLE, '0' },
    { PSP_CTRL_SQUARE, '9' },
};

static unsigned int prev_buttons;
static volatile int last_analog = (128 << 8) | 128;
static int prev_codes[16];
static int n_prev_codes;
static int64_t repeat_at[4];

static int mapped_codes(unsigned int b, int *codes) {
    int n = 0;
    for (unsigned i = 0; i < sizeof keymap / sizeof keymap[0]; i++) {
        if (b & keymap[i].button) codes[n++] = keymap[i].code;
    }
    if (b & PSP_CTRL_SELECT) {
        if (b & PSP_CTRL_LTRIGGER) codes[n++] = '7';
        else if (b & PSP_CTRL_RTRIGGER) codes[n++] = '*';
        else codes[n++] = '#';
    } else {
        if (b & PSP_CTRL_LTRIGGER) codes[n++] = '1';
        if (b & PSP_CTRL_RTRIGGER) codes[n++] = '3';
    }
    return n;
}

static void poll_controls(void) {
    SceCtrlData pad;
    sceCtrlPeekBufferPositive(&pad, 1);
    unsigned int b = pad.Buttons;
    /* the analog stick is read continuously by the game through plat_analog() */
    last_analog = (pad.Lx << 8) | pad.Ly;
    int codes[16];
    int n = mapped_codes(b, codes);
    /* releases */
    for (int i = 0; i < n_prev_codes; i++) {
        int still = 0;
        for (int j = 0; j < n; j++) still |= codes[j] == prev_codes[i];
        if (!still) push_event((2 << 24) | (prev_codes[i] & 0xffff));
    }
    /* presses and direction auto-repeat */
    int64_t now = plat_ticks_us();
    for (int j = 0; j < n; j++) {
        int was = 0;
        for (int i = 0; i < n_prev_codes; i++) was |= codes[j] == prev_codes[i];
        int dir = codes[j] >= -4 && codes[j] <= -1 ? -codes[j] - 1 : -1;
        if (!was) {
            push_event((1 << 24) | (codes[j] & 0xffff));
            if (dir >= 0) repeat_at[dir] = now + 400000;
        } else if (dir >= 0 && now >= repeat_at[dir]) {
            push_event((3 << 24) | (codes[j] & 0xffff));
            repeat_at[dir] = now + 120000;
        }
    }
    memcpy(prev_codes, codes, sizeof(int) * (size_t)n);
    n_prev_codes = n;
    prev_buttons = b;
}

int plat_analog(void) {
    SceCtrlData pad;
    sceCtrlPeekBufferPositive(&pad, 1);
    last_analog = (pad.Lx << 8) | pad.Ly;
    return last_analog;
}

int plat_poll_event(void) {
    if (ev_head == ev_tail) poll_controls();
    if (ev_head == ev_tail) return 0;
    int e = ev_queue[ev_head];
    ev_head = (ev_head + 1) % 64;
    return e;
}

/* ---- system ------------------------------------------------------------------- */

void plat_init(int argc, char **argv) {
    if (argc > 0 && argv[0]) {
        snprintf(base_dir, sizeof base_dir, "%s", argv[0]);
        char *slash = strrchr(base_dir, '/');
        if (slash) *slash = 0;
    }
    snprintf(data_dir, sizeof data_dir, "%s/data", base_dir);
    snprintf(save_dir, sizeof save_dir, "%s/save", base_dir);
    sceIoMkdir(save_dir, 0777);
    int th = sceKernelCreateThread("callbacks", callback_thread, 0x11, 0x1000, 0, NULL);
    if (th >= 0) sceKernelStartThread(th, 0, NULL);
    sceCtrlSetSamplingCycle(0);
    sceCtrlSetSamplingMode(PSP_CTRL_MODE_ANALOG);
    scePowerSetClockFrequency(333, 333, 166);
}

void plat_exit(int code) {
    (void)code;
    sceKernelExitGame();
}

int64_t plat_time_ms(void) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

int64_t plat_ticks_us(void) { return (int64_t)sceKernelGetSystemTimeWide(); }

void plat_sleep_us(int64_t us) { sceKernelDelayThread(us <= 0 ? 1 : (SceUInt)us); }

void plat_log(const char *s, int len) {
    /* stdout goes to the PSPLink/emulator console when present */
    fwrite(s, 1, (size_t)len, stdout);
    fflush(stdout);
}

const char *plat_data_dir(void) { return data_dir; }
const char *plat_save_dir(void) { return save_dir; }

size_t plat_heap_size(void) {
    /* take what the C heap can spare, keeping room for sounds, display lists and libc */
    size_t sz = 40u << 20;
    while (sz > (4u << 20)) {
        void *p = malloc(sz);
        if (p) {
            free(p);
            break;
        }
        sz -= 1u << 20;
    }
    size_t reserve = 5u << 20;
    return sz > reserve + (4u << 20) ? sz - reserve : (4u << 20);
}

void plat_debug_trace(const char *what) { (void)what; }

/* ---- locks and threads ------------------------------------------------------------ */

struct plat_lock {
    SceUID sema;
};

plat_lock *plat_lock_create(void) {
    plat_lock *l = malloc(sizeof *l);
    l->sema = sceKernelCreateSema("lock", 0, 1, 1, NULL);
    return l;
}

void plat_lock_acquire(plat_lock *l) { sceKernelWaitSema(l->sema, 1, NULL); }
void plat_lock_release(plat_lock *l) { sceKernelSignalSema(l->sema, 1); }

typedef struct {
    void (*fn)(void *);
    void *arg;
} Start;

static int thread_entry(SceSize args, void *argp) {
    (void)args;
    Start *s = argp;
    s->fn(s->arg);
    sceKernelExitDeleteThread(0);
    return 0;
}

int plat_thread_start(void (*fn)(void *), void *arg, int stack_kb) {
    SceUID th = sceKernelCreateThread("java", thread_entry, JAVA_THREAD_PRIO, stack_kb * 1024,
                                      PSP_THREAD_ATTR_USER | PSP_THREAD_ATTR_VFPU, NULL);
    if (th < 0) return 0;
    Start s = { fn, arg };
    return sceKernelStartThread(th, sizeof s, &s) >= 0;
}

/* ---- files ---------------------------------------------------------------------- */

uint8_t *plat_read_file(const char *path, size_t *len) {
    SceUID fd = sceIoOpen(path, PSP_O_RDONLY, 0);
    if (fd < 0) return NULL;
    SceOff n = sceIoLseek(fd, 0, PSP_SEEK_END);
    sceIoLseek(fd, 0, PSP_SEEK_SET);
    uint8_t *b = malloc((size_t)n + 1);
    if (!b) {
        sceIoClose(fd);
        return NULL;
    }
    int got = sceIoRead(fd, b, (SceSize)n);
    sceIoClose(fd);
    if (got != (int)n) {
        free(b);
        return NULL;
    }
    *len = (size_t)n;
    return b;
}

int plat_write_file(const char *path, const uint8_t *data, size_t len) {
    SceUID fd = sceIoOpen(path, PSP_O_WRONLY | PSP_O_CREAT | PSP_O_TRUNC, 0777);
    if (fd < 0) return 0;
    int ok = sceIoWrite(fd, data, (SceSize)len) == (int)len;
    sceIoClose(fd);
    return ok;
}

int plat_delete_file(const char *path) { return sceIoRemove(path) >= 0; }

void *plat_file_open(const char *path) {
    SceUID fd = sceIoOpen(path, PSP_O_RDONLY, 0);
    return fd < 0 ? NULL : (void *)(intptr_t)(fd + 1);
}

int plat_file_pread(void *f, void *buf, int len, long offset) {
    SceUID fd = (SceUID)(intptr_t)f - 1;
    if (sceIoLseek(fd, offset, PSP_SEEK_SET) < 0) return -1;
    return sceIoRead(fd, buf, (SceSize)len);
}

void plat_file_close(void *f) { sceIoClose((SceUID)(intptr_t)f - 1); }

/* ---- audio --------------------------------------------------------------------- */

#define AUDIO_FRAMES 1024
static int16_t audio_buf[2][AUDIO_FRAMES * 2] __attribute__((aligned(64)));

static int audio_thread(SceSize args, void *argp) {
    (void)args;
    (void)argp;
    int ch = sceAudioChReserve(PSP_AUDIO_NEXT_CHANNEL, AUDIO_FRAMES, PSP_AUDIO_FORMAT_STEREO);
    if (ch < 0) return 0;
    int cur = 0;
    for (;;) {
        mixer_render(audio_buf[cur], AUDIO_FRAMES);
        sceAudioOutputPannedBlocking(ch, PSP_AUDIO_VOLUME_MAX, PSP_AUDIO_VOLUME_MAX, audio_buf[cur]);
        cur ^= 1;
    }
    return 0;
}

void plat_audio_init(void) {
    mixer_init();
    SceUID th = sceKernelCreateThread("audio", audio_thread, AUDIO_THREAD_PRIO, 0x10000,
                                      PSP_THREAD_ATTR_USER, NULL);
    if (th >= 0) sceKernelStartThread(th, 0, NULL);
}

/* ---- entry point ------------------------------------------------------------------ */

void M_javax_microedition_midlet_Launcher__main____V(void);

int main(int argc, char **argv) {
    volatile char base;
    plat_init(argc, argv);
    r_init();
    plat_audio_init();
    jvm_init((void *)(&base + 64), plat_heap_size());
    M_javax_microedition_midlet_Launcher__main____V();
    jvm_report_exception("main");
    sceKernelExitGame();
    return 0;
}
