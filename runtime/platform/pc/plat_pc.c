/*
 * PC test platform: runs the translated game headless with the software renderer.
 *   --data DIR     resource directory (default ./data)
 *   --save DIR     record store directory (default ./save)
 *   --frames N     exit after N presented frames
 *   --dump DIR     write every --every'th frame as DIR/frame_NNNNN.ppm
 *   --every K      (default 30)
 *   --keys FILE    scripted input, lines "<frame> <press|release> <keycode>" or
 *                  "<frame> axis <(x<<8)|y>" for the analog stick
 *   --wav FILE     record the mixed audio output
 */
#include <errno.h>
#include <execinfo.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>
#include "jvm.h"
#include "mixer.h"
#include "plat.h"
#include "render.h"

/* SDL window (enabled with --window), implemented in sdl_window.c */
void win_init(void);
int win_on(void);
void win_shutdown(void);
void win_present(const uint32_t *pix);
int win_audio_init(void);
void win_audio_feed(const int16_t *buf, int frames);
static int window_on;

volatile int plat_reanudaciones; /* the PC build never sleeps */
static const char *data_dir = "data";
static const char *save_dir = "save";
static const char *dump_dir;
static const char *wav_path;
static int dump_every = 30;
static long max_frames = -1;
long pc_frame_count;
static const uint32_t *last_frame;
static volatile int frame_ready;
static int audio_on;
static char devlog_path[600];

typedef struct KeyEv {
    long frame;
    int type;
    int code;
} KeyEv;
static KeyEv *key_script;
static int n_keys, key_pos;
static int ev_queue[64], ev_head, ev_tail;

void plat_push_event(int ev) {
    int n = (ev_tail + 1) % 64;
    if (n == ev_head) return;
    ev_queue[ev_tail] = ev;
    ev_tail = n;
}

static int pc_analog = (128 << 8) | 128;
int plat_analog(void) { return pc_analog; }
void plat_set_analog(int a) { pc_analog = a; }

int plat_poll_event(void) {
    while (key_pos < n_keys && key_script[key_pos].frame <= pc_frame_count) {
        if (key_script[key_pos].type == 7) {
            pc_analog = key_script[key_pos].code;
            key_pos++;
            continue;
        }
        plat_push_event((key_script[key_pos].type << 24) | (key_script[key_pos].code & 0xffff));
        key_pos++;
    }
    /* the SDL blit must run on the main thread (this event loop); the game thread only
     * renders into the framebuffer and flags a new frame */
    if (window_on && win_on() && frame_ready) {
        frame_ready = 0;
        win_present(last_frame);
    }
    if (ev_head == ev_tail) return 0;
    int e = ev_queue[ev_head];
    ev_head = (ev_head + 1) % 64;
    return e;
}

static void load_keys(const char *path) {
    FILE *f = fopen(path, "r");
    if (!f) {
        perror(path);
        return;
    }
    char line[128];
    while (fgets(line, sizeof line, f)) {
        long fr;
        char what[16];
        int code;
        if (line[0] == '#' || sscanf(line, "%ld %15s %d", &fr, what, &code) != 3) continue;
        key_script = realloc(key_script, sizeof(KeyEv) * (size_t)(n_keys + 1));
        key_script[n_keys].frame = fr;
        key_script[n_keys].type = !strcmp(what, "press") ? 1 : !strcmp(what, "release") ? 2 : !strcmp(what, "axis") ? 7 : 3;
        key_script[n_keys].code = code;
        n_keys++;
    }
    fclose(f);
}

void plat_init(int argc, char **argv) {
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--data") && i + 1 < argc) data_dir = argv[++i];
        else if (!strcmp(argv[i], "--save") && i + 1 < argc) save_dir = argv[++i];
        else if (!strcmp(argv[i], "--frames") && i + 1 < argc) max_frames = atol(argv[++i]);
        else if (!strcmp(argv[i], "--dump") && i + 1 < argc) dump_dir = argv[++i];
        else if (!strcmp(argv[i], "--every") && i + 1 < argc) dump_every = atoi(argv[++i]);
        else if (!strcmp(argv[i], "--keys") && i + 1 < argc) load_keys(argv[++i]);
        else if (!strcmp(argv[i], "--wav") && i + 1 < argc) wav_path = argv[++i];
        else if (!strcmp(argv[i], "--window")) window_on = 1;
    }
    mkdir(save_dir, 0755);
    if (dump_dir) mkdir(dump_dir, 0755);
    snprintf(devlog_path, sizeof devlog_path, "%s/devlog.txt", save_dir);
    remove(devlog_path);
    devlog("ABISAL devlog - arranque (version PC de prueba)");
    if (window_on) {
        win_init();
        if (win_on()) {
            devlog("ventana SDL abierta (480x272 a 2x)");
        } else {
            devlog("aviso: no se pudo abrir la ventana SDL");
        }
    }
}

void plat_exit(int code) {
    fflush(stdout);
    win_shutdown();
    { extern void gc_prof_dump(void); gc_prof_dump(); }
    _exit(code);
}

int64_t plat_time_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

int64_t plat_ticks_us(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000000 + ts.tv_nsec / 1000;
}

void plat_sleep_us(int64_t us) {
    if (us <= 0) {
        sched_yield();
        return;
    }
    struct timespec ts = { (time_t)(us / 1000000), (long)(us % 1000000) * 1000 };
    while (nanosleep(&ts, &ts) == -1 && errno == EINTR) {
    }
}

void plat_log(const char *s, int len) {
    fwrite(s, 1, (size_t)len, stdout);
    fflush(stdout);
    if (devlog_path[0]) {
        FILE *f = fopen(devlog_path, "a");
        if (f) {
            fwrite(s, 1, (size_t)len, f);
            fclose(f);
        }
    }
}

size_t plat_free_mem(void) { return 0; }
volatile int plat_frames;
volatile int plat_fase_java;
const char *volatile plat_fase_motor = "inicio";
int plat_fw_version(void) { return 0; }

const char *plat_data_dir(void) { return data_dir; }
const char *plat_save_dir(void) { return save_dir; }
size_t plat_heap_size(void) {
    /* ABISAL_HEAP_MB=12 reproduces the PSP-1000's Java heap */
    const char *e = getenv("ABISAL_HEAP_MB");
    int mb = e ? atoi(e) : 0;
    return (size_t)(mb > 0 ? mb : 24) << 20;
}

struct plat_lock {
    pthread_mutex_t m;
};

plat_lock *plat_lock_create(void) {
    plat_lock *l = malloc(sizeof *l);
    pthread_mutex_init(&l->m, NULL);
    return l;
}
void plat_lock_acquire(plat_lock *l) { pthread_mutex_lock(&l->m); }
void plat_lock_release(plat_lock *l) { pthread_mutex_unlock(&l->m); }

typedef struct {
    void (*fn)(void *);
    void *arg;
} Start;

static void *thread_tramp(void *p) {
    Start s = *(Start *)p;
    free(p);
    s.fn(s.arg);
    return NULL;
}

int plat_thread_start(void (*fn)(void *), void *arg, int stack_kb) {
    pthread_t t;
    pthread_attr_t at;
    pthread_attr_init(&at);
    pthread_attr_setstacksize(&at, (size_t)(stack_kb < 512 ? 512 : stack_kb) * 1024);
    pthread_attr_setdetachstate(&at, PTHREAD_CREATE_DETACHED);
    Start *s = malloc(sizeof *s);
    s->fn = fn;
    s->arg = arg;
    int ok = pthread_create(&t, &at, thread_tramp, s) == 0;
    pthread_attr_destroy(&at);
    return ok;
}

uint8_t *plat_read_file(const char *path, size_t *len) {
    FILE *f = fopen(path, "rb");
    if (!f) return NULL;
    fseek(f, 0, SEEK_END);
    long n = ftell(f);
    fseek(f, 0, SEEK_SET);
    uint8_t *b = malloc((size_t)n + 1);
    if (fread(b, 1, (size_t)n, f) != (size_t)n) {
        free(b);
        fclose(f);
        return NULL;
    }
    fclose(f);
    *len = (size_t)n;
    return b;
}

int plat_write_file(const char *path, const uint8_t *data, size_t len) {
    FILE *f = fopen(path, "wb");
    if (!f) return 0;
    int ok = fwrite(data, 1, len, f) == len;
    fclose(f);
    return ok;
}

int plat_delete_file(const char *path) { return unlink(path) == 0; }

void *plat_file_open(const char *path) { return fopen(path, "rb"); }

int plat_file_pread(void *f, void *buf, int len, long offset) {
    if (fseek((FILE *)f, offset, SEEK_SET) != 0) return -1;
    return (int)fread(buf, 1, (size_t)len, (FILE *)f);
}

void plat_file_close(void *f) { fclose((FILE *)f); }

/* ---- audio: a thread pulls the mixer in real time (optionally into a WAV file) ---- */

static void *audio_thread(void *arg) {
    (void)arg;
    FILE *wav = wav_path ? fopen(wav_path, "wb") : NULL;
    uint32_t total = 0;
    if (wav) fwrite("RIFF\0\0\0\0WAVEfmt \x10\0\0\0\x01\0\x02\0\x44\xac\0\0\x10\xb1\x02\0\x04\0\x10\0data\0\0\0\0", 1, 44, wav);
    int16_t buf[2 * 441];
    int64_t next = plat_ticks_us();
    for (;;) {
        mixer_render(buf, 441);
        if (audio_on) win_audio_feed(buf, 441);
        if (wav) {
            fwrite(buf, 4, 441, wav);
            total += 441 * 4;
            if ((total & 0xffff) < 441 * 4) {
                long pos = ftell(wav);
                uint32_t riff = total + 36;
                fseek(wav, 4, SEEK_SET);
                fwrite(&riff, 4, 1, wav);
                fseek(wav, 40, SEEK_SET);
                fwrite(&total, 4, 1, wav);
                fseek(wav, pos, SEEK_SET);
                fflush(wav);
            }
        }
        next += 10000;
        int64_t now = plat_ticks_us();
        if (next > now) plat_sleep_us(next - now);
    }
    return NULL;
}

void plat_audio_init(void) {
    mixer_init();
    if (window_on) {
        audio_on = win_audio_init();
    }
    pthread_t t;
    pthread_create(&t, NULL, audio_thread, NULL);
}

void M_javax_microedition_midlet_Launcher__main____V(void);

int main(int argc, char **argv) {
    volatile char base;
    plat_init(argc, argv);
    plat_audio_init();
    r_init();
    jvm_init((void *)(&base + 64), plat_heap_size());
    M_javax_microedition_midlet_Launcher__main____V();
    jvm_report_exception("main");
    plat_exit(0);
    return 0;
}

/* frame dumping, called by the PC renderer on present */
void pc_frame_presented(const uint32_t *pix, int w, int h) {
    pc_frame_count++;
    if (window_on) {
        last_frame = pix;
        frame_ready = 1; /* the main thread (event loop) does the SDL blit */
    }
    if (dump_dir && pc_frame_count % dump_every == 0) {
        char path[512];
        snprintf(path, sizeof path, "%s/frame_%05ld.ppm", dump_dir, pc_frame_count);
        FILE *f = fopen(path, "wb");
        if (f) {
            fprintf(f, "P6\n%d %d\n255\n", w, h);
            for (int i = 0; i < w * h; i++) {
                uint32_t c = pix[i];
                uint8_t rgb[3] = { (uint8_t)c, (uint8_t)(c >> 8), (uint8_t)(c >> 16) };
                fwrite(rgb, 1, 3, f);
            }
            fclose(f);
        }
    }
    if (max_frames >= 0 && pc_frame_count >= max_frames) plat_exit(0);
}

/* J2C_TRACE=1 prints a native backtrace whenever an exception is raised */
void plat_debug_trace(const char *what) {
    static int enabled = -1;
    if (enabled < 0) enabled = getenv("J2C_TRACE") != NULL;
    if (!enabled) return;
    void *frames[16];
    int n = backtrace(frames, 16);
    fprintf(stdout, "--- %s\n", what);
    fflush(stdout);
    backtrace_symbols_fd(frames + 1, n - 1, 1);
}
