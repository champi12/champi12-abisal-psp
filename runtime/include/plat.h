/* Platform abstraction implemented by runtime/platform/psp and runtime/platform/pc. */
#ifndef PLAT_H
#define PLAT_H

#include <stdint.h>
#include <stddef.h>

/* ---- system ---- */
void plat_init(int argc, char **argv);
void plat_exit(int code);
int64_t plat_time_ms(void);     /* wall clock, for System.currentTimeMillis() */
int64_t plat_ticks_us(void);    /* monotonic */
void plat_sleep_us(int64_t us);
void plat_log(const char *s, int len); /* console + devlog.txt */
/* devlog: timestamped line written straight to devlog.txt (survives a crash) */
void devlog(const char *fmt, ...) __attribute__((format(printf, 1, 2)));
size_t plat_free_mem(void);     /* free system memory in bytes (0 if unknown) */
int plat_fw_version(void);      /* firmware as 0xMMmm0000 style number (0 if unknown) */
const char *plat_data_dir(void); /* directory holding the game resources (no trailing slash) */
const char *plat_save_dir(void); /* directory for record stores */
size_t plat_heap_size(void);    /* bytes to give the Java heap */

/* ---- threads: one global lock serialises all Java code ---- */
typedef struct plat_lock plat_lock;
plat_lock *plat_lock_create(void);
void plat_lock_acquire(plat_lock *l);
void plat_lock_release(plat_lock *l);
int plat_thread_start(void (*fn)(void *), void *arg, int stack_kb);

/* ---- input ---- */
/* Returns next event or 0: (type << 24) | (keycode & 0xffff); type 1 press, 2 release,
 * 3 repeat, 4 exit requested, 5 pause, 6 resume. */
int plat_poll_event(void);
/* Analog stick: (x << 8) | y, each 0..255 with 128 centred. */
int plat_analog(void);

/* ---- files ---- */
/* Reads a whole file; returns malloc'd buffer (caller frees) or NULL. */
uint8_t *plat_read_file(const char *path, size_t *len);
int plat_write_file(const char *path, const uint8_t *data, size_t len);
int plat_delete_file(const char *path);
/* random-access reads (used to stream long sounds) */
void *plat_file_open(const char *path);
int plat_file_pread(void *f, void *buf, int len, long offset);
void plat_file_close(void *f);

/* ---- audio: mixer of PCM voices ---- */
typedef struct plat_sound {
    int16_t *samples; /* interleaved stereo if channels == 2; NULL when streamed */
    int frames;       /* 0 once the sound has been freed */
    int channels;
    int rate;
    char *path;       /* streamed sounds: file holding the PCM data */
    long data_offset;
} plat_sound;
void plat_audio_init(void);
int plat_audio_play(const plat_sound *s, int loops, int volume); /* returns voice id > 0 */
void plat_audio_stop(int voice);
int plat_audio_playing(int voice);
void plat_audio_volume(int voice, int volume);
int plat_audio_position_ms(int voice);
void plat_audio_lock(void);
void plat_audio_unlock(void);

#endif
