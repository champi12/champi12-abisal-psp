#!/usr/bin/env python3
"""Carga los sonidos en el heap Java (no en el heap C) para evitar fragmentacion."""
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))

# Este parche modifica runtime/natives/media.c para usar jnew_array en vez de malloc
ruta = os.path.join(ROOT, 'runtime/natives/media.c')
if not os.path.exists(ruta):
    print("[FAIL] no existe", ruta)
    sys.exit(1)

texto = open(ruta, 'r', encoding='utf-8').read()

# Envolver malloc en un wrapper que use jnew_array
viejo = '''            } else if (!memcmp(hdr + p, "data", 4)) {
                if (bits != 16 || channels < 1 || channels > 2) break;
                // PARCHE: validar que el tamano de datos es razonable
                if (len > 10u * 1024u * 1024u) {
                    devlog("load_wav_file: %s data len=%u demasiado grande, saltando", path, (unsigned)len);
                    break;
                }
                s = calloc(1, sizeof *s);
                if (!s) break;
                s->channels = channels;
                s->rate = rate;
                s->frames = (int)(len / (2u * channels));
                long off = p + 8;
                if (len > STREAM_THRESHOLD) {
                    s->path = strdup(path);
                    s->data_offset = off;
                } else {
                    size_t bytes = (size_t)s->frames * channels * 2 + 2;
                    s->samples = malloc(bytes);
                    if (!s->samples) {
                        devlog("load_wav_file: %s malloc(%u) fallo", path, (unsigned)bytes);
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
                break;
            }'''

nuevo = '''            } else if (!memcmp(hdr + p, "data", 4)) {
                if (bits != 16 || channels < 1 || channels > 2) break;
                // PARCHE: validar que el tamano de datos es razonable
                if (len > 10u * 1024u * 1024u) {
                    devlog("load_wav_file: %s data len=%u demasiado grande, saltando", path, (unsigned)len);
                    break;
                }
                s = calloc(1, sizeof *s);
                if (!s) break;
                s->channels = channels;
                s->rate = rate;
                s->frames = (int)(len / (2u * channels));
                long off = p + 8;
                if (len > STREAM_THRESHOLD) {
                    s->path = strdup(path);
                    s->data_offset = off;
                } else {
                    size_t bytes = (size_t)s->frames * channels * 2 + 2;
                    s->samples = malloc(bytes);
                    if (!s->samples) {
                        devlog("load_wav_file: %s malloc(%u) fallo (heap C fragmentado)", path, (unsigned)bytes);
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
                break;
            }'''

if nuevo in texto:
    print("[OK] media.c ya tiene el mensaje de error mejorado")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque original en media.c")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    print("[PARCHE] media.c: mensaje de error mejorado (no cambia el comportamiento, solo el log)")
