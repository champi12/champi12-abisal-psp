/* Software mixer: platforms call mixer_render() from their audio thread (44.1 kHz stereo). */
#include <stdlib.h>
#include <string.h>
#include "mixer.h"
#include "plat.h"

#define MAX_VOICES 16

#define STREAM_FRAMES 8192

typedef struct Voice {
    const plat_sound *snd;
    void *fh;          /* streamed sounds */
    int16_t *sbuf;
    int sbuf_start, sbuf_frames;
    int id;
    uint32_t pos_fp;   /* frame position, 16.16 fixed point */
    uint32_t step_fp;
    int loops;         /* remaining extra plays; -1 = forever */
    int volume;        /* 0..100 */
    int active;
    int64_t played_frames;
} Voice;

static Voice voices[MAX_VOICES];
static int next_id = 1;
static plat_lock *mix_lock;

void mixer_init(void) { mix_lock = plat_lock_create(); }

void plat_audio_lock(void) { plat_lock_acquire(mix_lock); }
void plat_audio_unlock(void) { plat_lock_release(mix_lock); }

static void voice_release(Voice *v) {
    v->active = 0;
    if (v->fh) {
        plat_file_close(v->fh);
        v->fh = NULL;
    }
}

int plat_audio_play(const plat_sound *s, int loops, int volume) {
    if (!s || (!s->samples && !s->path) || s->frames <= 0) return 0;
    void *fh = NULL;
    if (!s->samples) {
        fh = plat_file_open(s->path);
        if (!fh) return 0;
    }
    plat_audio_lock();
    Voice *v = NULL;
    for (int i = 0; i < MAX_VOICES; i++) {
        if (!voices[i].active) {
            v = &voices[i];
            break;
        }
    }
    if (!v) {
        /* steal the voice that has played longest */
        v = &voices[0];
        for (int i = 1; i < MAX_VOICES; i++) {
            if (voices[i].played_frames > v->played_frames) v = &voices[i];
        }
    }
    voice_release(v);
    v->snd = s;
    v->fh = fh;
    if (fh && !v->sbuf) v->sbuf = malloc(sizeof(int16_t) * 2 * STREAM_FRAMES);
    v->sbuf_start = 0;
    v->sbuf_frames = 0;
    v->id = next_id++;
    if (next_id > 0x7fffff) next_id = 1;
    v->pos_fp = 0;
    v->step_fp = (uint32_t)(((uint64_t)s->rate << 16) / MIXER_RATE);
    v->loops = loops < 0 ? -1 : loops - 1;
    v->volume = volume;
    v->active = 1;
    v->played_frames = 0;
    int id = v->id;
    plat_audio_unlock();
    return id;
}

static Voice *find(int id) {
    for (int i = 0; i < MAX_VOICES; i++) {
        if (voices[i].active && voices[i].id == id) return &voices[i];
    }
    return NULL;
}

void plat_audio_stop(int id) {
    plat_audio_lock();
    Voice *v = find(id);
    if (v) voice_release(v);
    plat_audio_unlock();
}

int plat_audio_playing(int id) {
    plat_audio_lock();
    int r = find(id) != NULL;
    plat_audio_unlock();
    return r;
}

void plat_audio_volume(int id, int vol) {
    plat_audio_lock();
    Voice *v = find(id);
    if (v) v->volume = vol;
    plat_audio_unlock();
}

int plat_audio_position_ms(int id) {
    plat_audio_lock();
    Voice *v = find(id);
    int ms = v && v->snd->rate > 0 ? (int)(((int64_t)(v->pos_fp >> 16)) * 1000 / v->snd->rate) : 0;
    plat_audio_unlock();
    return ms;
}

void mixer_render(int16_t *out, int frames) {
    static int32_t acc[2 * 1024];
    while (frames > 0) {
        int n = frames > 1024 ? 1024 : frames;
        memset(acc, 0, sizeof(int32_t) * 2 * (size_t)n);
        plat_audio_lock();
        for (int vi = 0; vi < MAX_VOICES; vi++) {
            Voice *v = &voices[vi];
            if (!v->active) continue;
            const plat_sound *s = v->snd;
            if (s->frames <= 0 || (!s->samples && (!v->fh || !v->sbuf))) {
                voice_release(v);
                continue;
            }
            int vol = v->volume * 256 / 100;
            for (int i = 0; i < n; i++) {
                uint32_t f = v->pos_fp >> 16;
                if ((int)f >= s->frames) {
                    if (v->loops != 0) {
                        if (v->loops > 0) v->loops--;
                        v->pos_fp = 0;
                        f = 0;
                    } else {
                        voice_release(v);
                        break;
                    }
                }
                const int16_t *src = s->samples;
                uint32_t sf = f;
                if (!src) {
                    if ((int)f < v->sbuf_start || (int)f >= v->sbuf_start + v->sbuf_frames) {
                        int want = s->frames - (int)f < STREAM_FRAMES ? s->frames - (int)f : STREAM_FRAMES;
                        int bytes = want * s->channels * 2;
                        int got = plat_file_pread(v->fh, v->sbuf, bytes,
                                                  s->data_offset + (long)f * s->channels * 2);
                        v->sbuf_start = (int)f;
                        v->sbuf_frames = got > 0 ? got / (s->channels * 2) : 0;
                        if (v->sbuf_frames <= 0) {
                            voice_release(v);
                            break;
                        }
                    }
                    src = v->sbuf;
                    sf = f - (uint32_t)v->sbuf_start;
                }
                int l, r;
                if (s->channels == 2) {
                    l = src[sf * 2];
                    r = src[sf * 2 + 1];
                } else {
                    l = r = src[sf];
                }
                acc[i * 2] += (l * vol) >> 8;
                acc[i * 2 + 1] += (r * vol) >> 8;
                v->pos_fp += v->step_fp;
            }
            v->played_frames += n;
        }
        plat_audio_unlock();
        for (int i = 0; i < n * 2; i++) {
            int32_t x = acc[i];
            out[i] = (int16_t)(x > 32767 ? 32767 : (x < -32768 ? -32768 : x));
        }
        out += n * 2;
        frames -= n;
    }
}
