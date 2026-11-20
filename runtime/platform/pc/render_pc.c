/* PC screen renderer: the software rasterizer drawing into a 480x272 framebuffer. */
#include <stdlib.h>
#include "jvm.h"
#include "plat.h"
#include "render.h"

static uint32_t framebuffer[SCREEN_W * SCREEN_H];
static float depthbuffer[SCREEN_W * SCREEN_H];
static SwSurface screen = { framebuffer, SCREEN_W, SCREEN_W, SCREEN_H, depthbuffer, 0, 0, SCREEN_W, SCREEN_H };
static int64_t last_present;

void pc_frame_presented(const uint32_t *pix, int w, int h);

void r_init(void) {
    for (int i = 0; i < SCREEN_W * SCREEN_H; i++) framebuffer[i] = 0xff000000u;
}

void plat_gpu_sync(void) {}

void r_clip(int x, int y, int w, int h) { sw_clip(&screen, x, y, w, h); }
void r_fill_rect(int x, int y, int w, int h, uint32_t c) { sw_fill_rect(&screen, x, y, w, h, c); }
void r_line(int x1, int y1, int x2, int y2, uint32_t c) { sw_line(&screen, x1, y1, x2, y2, c); }
void r_triangle(int x1, int y1, int x2, int y2, int x3, int y3, uint32_t c) {
    sw_triangle(&screen, x1, y1, x2, y2, x3, y3, c);
}
void r_image(const uint32_t *pix, int stride, int iw, int ih, int sx, int sy, int sw, int sh, int t, int dx, int dy,
             int alpha) {
    (void)iw;
    (void)ih;
    sw_image(&screen, pix, stride, sx, sy, sw, sh, t, dx, dy, alpha);
}
void r_rgb(const int32_t *argb, int off, int scan, int x, int y, int w, int h, int alpha) {
    sw_rgb(&screen, argb, off, scan, x, y, w, h, alpha);
}
void r_text(const uint16_t *text, int n, int x, int y, uint32_t c, int scale) {
    sw_text(&screen, text, n, x, y, c, scale);
}
void r_pixels_changed(const void *p, int bytes) {
    (void)p;
    (void)bytes;
}

void r_present(void) {
    pc_frame_presented(framebuffer, SCREEN_W, SCREEN_H);
    /* emulate a 60 Hz display */
    int64_t now = plat_ticks_us();
    int64_t wait = last_present + 16667 - now;
    if (wait > 0) {
        JThread *t = jvm_blocking_begin();
        plat_sleep_us(wait);
        jvm_blocking_end(t);
    }
    last_present = plat_ticks_us();
}

static int clip3_x, clip3_y, clip3_w, clip3_h;

void r3_begin(int vx, int vy, int vw, int vh, int cx, int cy, int cw, int ch) {
    clip3_x = cx;
    clip3_y = cy;
    clip3_w = cw;
    clip3_h = ch;
    sw_clip(&screen, cx, cy, cw, ch);
    sw3_begin(&screen, vx, vy, vw, vh);
}
void r3_viewport(int x, int y, int w, int h) { sw3_begin(&screen, x, y, w, h); }
void r3_end(void) {}
void r3_clear(uint32_t argb, int color, int depth) {
    sw_clip(&screen, clip3_x, clip3_y, clip3_w, clip3_h);
    sw3_clear(&screen, argb, color, depth);
}
void r3_projection(const float *m) { sw3_projection(m); }
void r3_lights(int n, const float *d) { sw3_lights(n, d); }
void r3_draw(const int32_t *verts, int vcount, const uint16_t *idx, int icount, const float *mv,
             const int32_t *state, const uint32_t *tex, int tw, int th) {
    sw_clip(&screen, clip3_x, clip3_y, clip3_w, clip3_h);
    sw3_draw(&screen, verts, vcount, idx, icount, mv, state, tex, tw, th);
}

/* the PC renderer always keeps the canvas; the flag only matters on the PSP */
int preserve_frame = 1;
