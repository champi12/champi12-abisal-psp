#ifndef MIXER_H
#define MIXER_H
#include <stdint.h>

#define MIXER_RATE 44100

void mixer_init(void);
/* Mix `frames` stereo frames of all active voices into out (interleaved L/R). */
void mixer_render(int16_t *out, int frames);

#endif
