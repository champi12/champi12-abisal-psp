/*
 * Natives of j2me.Media. Game audio (AMR/MIDI) is converted to 16-bit PCM WAV files at
 * build time and stored next to the original resource as "<name>.wav".
 */
#include <stdio.h>
#include <stdlib.h>
#include "j2c_gen.h"
#include "plat.h"

#define MAX_SOUNDS 128
static plat_sound *sounds[MAX_SOUNDS];

static uint32_t rd32(const uint8_t *p) { return p[0] | (p[1] << 8) | (p[2] << 16) | ((uint32_t)p[3] << 24); }
static uint16_t rd16(const uint8_t *p) { return (uint16_t)(p[0] | (p[1] << 8)); }

static plat_sound *parse_wav(const uint8_t *d, size_t n) {
    if (n < 44 || memcmp(d, "RIFF", 4) || memcmp(d + 8, "WAVE", 4)) return NULL;
    size_t p = 12;
    int channels = 0, rate = 0, bits = 0;
    while (p + 8 <= n) {
        uint32_t len = rd32(d + p + 4);
        const uint8_t *body = d + p + 8;
        if (p + 8 + len > n) len = (uint32_t)(n - p - 8);
        if (!memcmp(d + p, "fmt ", 4) && len >= 16) {
            if (rd16(body) != 1) return NULL; /* PCM only */
            channels = rd16(body + 2);
            rate = (int)rd32(body + 4);
            bits = rd16(body + 14);
        } else if (!memcmp(d + p, "data", 4)) {
            if (bits != 16 || channels < 1 || channels > 2) return NULL;
            plat_sound *s = calloc(1, sizeof *s);
            s->channels = channels;
            s->rate = rate;
            s->frames = (int)(len / (2u * channels));
            s->samples = malloc((size_t)s->frames * channels * 2 + 2);
            memcpy(s->samples, body, (size_t)s->frames * channels * 2);
            return s;
        }
        p += 8 + len + (len & 1);
    }
    return NULL;
}

static int store(plat_sound *s) {
    if (!s) return 0;
    for (int i = 1; i < MAX_SOUNDS; i++) {
        if (!sounds[i]) {
            sounds[i] = s;
            return i;
        }
    }
    free(s->samples);
    free(s);
    return 0;
}

/* Sounds longer than this are streamed from the memory stick instead of kept in RAM. */
#define STREAM_THRESHOLD (512 * 1024)

/* Parse a WAV header from a file; small files are loaded, large ones are streamed. */
static plat_sound *load_wav_file(const char *path) {
    void *f = plat_file_open(path);
    if (!f) return NULL;
    uint8_t hdr[4096];
    int n = plat_file_pread(f, hdr, sizeof hdr, 0);
    plat_sound *s = NULL;
    if (n >= 44 && !memcmp(hdr, "RIFF", 4) && !memcmp(hdr + 8, "WAVE", 4)) {
        int p = 12, channels = 0, rate = 0, bits = 0;
        while (p + 8 <= n) {
            uint32_t len = rd32(hdr + p + 4);
            if (!memcmp(hdr + p, "fmt ", 4) && p + 24 <= n) {
                if (rd16(hdr + p + 8) != 1) break;
                channels = rd16(hdr + p + 10);
                rate = (int)rd32(hdr + p + 12);
                bits = rd16(hdr + p + 22);
            } else if (!memcmp(hdr + p, "data", 4)) {
                if (bits != 16 || channels < 1 || channels > 2) break;
                s = calloc(1, sizeof *s);
                s->channels = channels;
                s->rate = rate;
                s->frames = (int)(len / (2u * channels));
                long off = p + 8;
                if (len > STREAM_THRESHOLD) {
                    s->path = strdup(path);
                    s->data_offset = off;
                } else {
                    s->samples = malloc((size_t)s->frames * channels * 2 + 2);
                    int got = plat_file_pread(f, s->samples, s->frames * channels * 2, off);
                    if (got < 0) got = 0;
                    s->frames = got / (2 * channels);
                }
                break;
            }
            p += 8 + (int)len + (int)(len & 1);
        }
    }
    plat_file_close(f);
    return s;
}

int32_t M_j2me_Media__load__Ljava_lang_String_2__I(JObject *path) {
    char rel[400], full[600];
    jstring_to_cstr(path, rel, sizeof rel);
    if (strstr(rel, "..")) return 0;
    snprintf(full, sizeof full, "%s/%s.wav", plat_data_dir(), rel);
    plat_sound *s = load_wav_file(full);
    if (!s) {
        snprintf(full, sizeof full, "%s/%s", plat_data_dir(), rel);
        s = load_wav_file(full);
    }
    return store(s);
}

int32_t M_j2me_Media__loadData___3B__I(JObject *data) {
    return store(parse_wav(JARR(data, uint8_t), (size_t)((JArray *)data)->length));
}

void M_j2me_Media__free__I__V(int32_t id) {
    if (id <= 0 || id >= MAX_SOUNDS || !sounds[id]) return;
    plat_sound *s = sounds[id];
    plat_audio_lock();
    /* voices referencing this sound are stopped by the mixer when it sees samples == NULL */
    sounds[id] = NULL;
    int16_t *samples = s->samples;
    s->samples = NULL;
    s->frames = 0;
    plat_audio_unlock();
    free(samples);
    free(s->path);
    s->path = NULL;
    /* the plat_sound struct itself is leaked deliberately: a voice may still point at it */
}

int32_t M_j2me_Media__play__III__I(int32_t id, int32_t loops, int32_t vol) {
    if (id <= 0 || id >= MAX_SOUNDS || !sounds[id]) return 0;
    return plat_audio_play(sounds[id], loops, vol);
}

void M_j2me_Media__stop__I__V(int32_t voice) { plat_audio_stop(voice); }

int32_t M_j2me_Media__playing__I__Z(int32_t voice) { return plat_audio_playing(voice); }

void M_j2me_Media__setVolume__II__V(int32_t voice, int32_t vol) { plat_audio_volume(voice, vol); }

int32_t M_j2me_Media__durationMs__I__I(int32_t id) {
    if (id <= 0 || id >= MAX_SOUNDS || !sounds[id] || sounds[id]->rate == 0) return 0;
    return (int32_t)((int64_t)sounds[id]->frames * 1000 / sounds[id]->rate);
}

int32_t M_j2me_Media__positionMs__I__I(int32_t voice) { return plat_audio_position_ms(voice); }
