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

/* IMA/DVI 4-bit ADPCM tables: identical to the ones the build-time encoder used. */
static const int ima_index_tab[16] = { -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8 };
static const int ima_step_tab[89] = {
    7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66,
    73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449,
    494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272,
    2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442,
    11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 };

/* Decode mono IMA ADPCM blocks straight into 16-bit PCM (the first nibble of a byte is the
 * low one, matching the build-time encoder). Returns the number of frames written. */
static int decode_ima(const uint8_t *d, int len, int block, int spb, int16_t *out, int max) {
    int o = 0;
    for (int b = 0; b + block <= len && o < max; b += block) {
        const uint8_t *p = d + b;
        int pred = (int16_t)(p[0] | (p[1] << 8));
        int idx = p[2] <= 88 ? p[2] : 88;
        out[o++] = (int16_t)pred;
        for (int i = 4; i < block && o < max; i++) {
            int byte = p[i];
            for (int half = 0; half < 2 && o < max; half++) {
                int nib = half == 0 ? (byte & 0x0F) : (byte >> 4);
                int step = ima_step_tab[idx];
                int diff = step >> 3;
                if (nib & 1) diff += step >> 2;
                if (nib & 2) diff += step >> 1;
                if (nib & 4) diff += step;
                if (nib & 8) {
                    pred -= diff;
                } else {
                    pred += diff;
                }
                if (pred > 32767) pred = 32767;
                if (pred < -32768) pred = -32768;
                idx += ima_index_tab[nib];
                if (idx < 0) idx = 0;
                if (idx > 88) idx = 88;
                out[o++] = (int16_t)pred;
            }
        }
    }
    return o;
}

/* Parse a WAV header from a file; small files are loaded, large ones are streamed.
 * Accepts 16-bit PCM and mono IMA ADPCM (format 0x0011), which is decoded to PCM here so
 * the mixer only ever sees plain samples. */
static plat_sound *load_wav_file(const char *path) {
    void *f = plat_file_open(path);
    if (!f) return NULL;
    uint8_t hdr[4096];
    int n = plat_file_pread(f, hdr, sizeof hdr, 0);
    plat_sound *s = NULL;
    if (n >= 44 && !memcmp(hdr, "RIFF", 4) && !memcmp(hdr + 8, "WAVE", 4)) {
        int p = 12, channels = 0, rate = 0, bits = 0, tag = 0, block_align = 0, spb = 0, fact = 0;
        while (p + 8 <= n) {
            uint32_t len = rd32(hdr + p + 4);
            if (!memcmp(hdr + p, "fmt ", 4) && p + 24 <= n) {
                tag = rd16(hdr + p + 8);
                channels = rd16(hdr + p + 10);
                rate = (int)rd32(hdr + p + 12);
                block_align = rd16(hdr + p + 20);
                bits = rd16(hdr + p + 22);
                if (tag == 0x11 && p + 28 <= n) spb = rd16(hdr + p + 26);
            } else if (!memcmp(hdr + p, "fact", 4) && p + 12 <= n) {
                fact = (int)rd32(hdr + p + 8);
            } else if (!memcmp(hdr + p, "data", 4)) {
                // PARCHE: validar que el tamano de datos es razonable
                if (len > 10u * 1024u * 1024u) {
                    devlog("load_wav_file: %s data len=%u demasiado grande, saltando", path, (unsigned)len);
                    break;
                }
                long off = p + 8;
                if (tag == 1 && bits == 16 && channels >= 1 && channels <= 2) {
                    s = calloc(1, sizeof *s);
                    if (!s) break;
                    s->channels = channels;
                    s->rate = rate;
                    s->frames = (int)(len / (2u * channels));
                    if (len > STREAM_THRESHOLD) {
                        s->path = strdup(path);
                        s->data_offset = off;
                    } else {
                        size_t bytes = (size_t)s->frames * channels * 2 + 2;
                        s->samples = malloc(bytes);
                        if (!s->samples) {
                            devlog("load_wav_file: %s malloc(%u) fallo (heap C fragmentado)", path,
                                   (unsigned)bytes);
                            free(s);
                            s = NULL;
                            break;
                        }
                        int got = plat_file_pread(f, s->samples, s->frames * channels * 2, off);
                        if (got < 0) {
                            devlog("load_wav_file: %s pread fallo (%d)", path, got);
                            free(s->samples);
                            free(s);
                            s = NULL;
                            break;
                        }
                        s->frames = got / (2 * channels);
                    }
                } else if (tag == 0x11 && bits == 4 && channels == 1 && block_align > 0 && spb > 0
                        && len <= (uint32_t)STREAM_THRESHOLD) {
                    // IMA ADPCM: decode once at load. Only for sounds that fit in RAM anyway,
                    // so memory use stays the same and only the data folder shrinks.
                    int blocks = (int)(len / (uint32_t)block_align);
                    int total = blocks * spb;
                    int want = (fact > 0 && fact < total) ? fact : total;
                    uint8_t *tmp = malloc(len);
                    int16_t *pcm = malloc((size_t)want * 2 + 2);
                    if (!tmp || !pcm) {
                        free(tmp);
                        free(pcm);
                        devlog("load_wav_file: %s sin memoria para ADPCM", path);
                        break;
                    }
                    int got = plat_file_pread(f, tmp, (int)len, off);
                    if (got <= 0) {
                        free(tmp);
                        free(pcm);
                        break;
                    }
                    int frames = decode_ima(tmp, got, block_align, spb, pcm, want);
                    free(tmp);
                    s = calloc(1, sizeof *s);
                    if (!s) {
                        free(pcm);
                        break;
                    }
                    s->channels = 1;
                    s->rate = rate;
                    s->frames = frames;
                    s->samples = pcm;
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
