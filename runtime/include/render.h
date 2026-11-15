/*
 * Rendering interfaces.
 *
 * sw_*  software rasterizer over a 32-bit ABGR surface (always available; used for
 *       mutable images and as the whole renderer of the PC test build).
 * r_*   screen renderer of the current platform (PSP: GE via sceGu, PC: sw_* on a
 *       framebuffer).
 *
 * Pixels are 0xAABBGGRR (byte order R,G,B,A in memory), the PSP GE's native format.
 * The m3g draw call consumes the vertex/state layout produced by javax.microedition.m3g.
 */
#ifndef RENDER_H
#define RENDER_H

#include <stdint.h>

#define SCREEN_W 480
#define SCREEN_H 272

/* packed appearance state (see Graphics3D.drawSubmesh) */
enum {
    ST_BLEND = 0, ST_ALPHA_THRESHOLD, ST_FLAGS, ST_CULL, ST_WINDING,
    ST_AMBIENT, ST_DIFFUSE, ST_EMISSIVE, ST_SPECULAR, ST_SHININESS,
    ST_TEXFUNC, ST_TEXBLENDCOLOR, ST_WRAPS, ST_WRAPT, ST_FILTER,
    ST_ALPHAFACTOR, ST_VFLAGS, ST_TEXALPHA, ST_TEXFORMAT,
    ST_FOGMODE, ST_FOGCOLOR, ST_FOGDENSITY, ST_FOGNEAR, ST_FOGFAR,
};
enum {
    SF_DEPTH_TEST = 1, SF_DEPTH_WRITE = 2, SF_COLOR_WRITE = 4, SF_ALPHA_WRITE = 8,
    SF_LIGHTING = 16, SF_TRACKING = 32, SF_TWO_SIDED = 64, SF_SMOOTH = 128,
    SF_TEXTURE = 256, SF_PERSPECTIVE = 512,
};
enum { VF_NORMALS = 1, VF_COLORS = 2, VF_TEX = 4 };
#define VSTRIDE 9 /* u v color nx ny nz x y z */

/* M3G constants used by the renderers */
#define M3G_ALPHA 64
#define M3G_ALPHA_ADD 65
#define M3G_MODULATE 66
#define M3G_MODULATE_X2 67
#define M3G_REPLACE 68
#define M3G_CULL_BACK 160
#define M3G_CULL_FRONT 161
#define M3G_CULL_NONE 162
#define M3G_WINDING_CCW 168
#define M3G_WINDING_CW 169
#define M3G_FILTER_LINEAR 209
#define M3G_FUNC_ADD 224
#define M3G_FUNC_BLEND 225
#define M3G_FUNC_DECAL 226
#define M3G_FUNC_MODULATE 227
#define M3G_FUNC_REPLACE 228
#define M3G_WRAP_CLAMP 240
#define M3G_LIGHT_AMBIENT 128
#define M3G_LIGHT_DIRECTIONAL 129
#define M3G_LIGHT_OMNI 130
#define M3G_LIGHT_SPOT 131
#define M3G_FOG_EXP 80
#define M3G_FOG_LINEAR 81

static inline uint32_t argb_to_abgr(uint32_t c) {
    return (c & 0xff00ff00u) | ((c >> 16) & 0xffu) | ((c & 0xffu) << 16);
}

/* map destination pixel (a, b) of a transformed region of source size w x h to source (u, v) */
static inline void map_transform(int t, int w, int h, int a, int b, int *u, int *v) {
    switch (t) {
    case 0: *u = a; *v = b; break;                 /* NONE */
    case 2: *u = w - 1 - a; *v = b; break;         /* MIRROR */
    case 1: *u = a; *v = h - 1 - b; break;         /* MIRROR_ROT180 */
    case 3: *u = w - 1 - a; *v = h - 1 - b; break; /* ROT180 */
    case 5: *u = a; *v = h - 1 - b; break;         /* ROT90 (a,b swapped by caller) */
    case 6: *u = w - 1 - a; *v = b; break;         /* ROT270 */
    case 7: *u = w - 1 - a; *v = h - 1 - b; break; /* MIRROR_ROT90 */
    default: *u = a; *v = b; break;                /* MIRROR_ROT270 */
    }
}

/* ---- software surface ---- */
typedef struct SwSurface {
    uint32_t *pix;
    int stride, w, h;
    float *depth; /* NULL if no depth buffer */
    int cx, cy, cw, ch;
} SwSurface;

void sw_clip(SwSurface *s, int x, int y, int w, int h);
void sw_fill_rect(SwSurface *s, int x, int y, int w, int h, uint32_t argb);
void sw_line(SwSurface *s, int x1, int y1, int x2, int y2, uint32_t argb);
void sw_triangle(SwSurface *s, int x1, int y1, int x2, int y2, int x3, int y3, uint32_t argb);
void sw_image(SwSurface *s, const uint32_t *pix, int stride, int sx, int sy, int sw, int sh, int transform,
              int dx, int dy, int alpha);
void sw_rgb(SwSurface *s, const int32_t *argb, int off, int scan, int x, int y, int w, int h, int alpha);
void sw_text(SwSurface *s, const uint16_t *text, int n, int x, int y, uint32_t argb, int scale);

void sw3_begin(SwSurface *s, int vx, int vy, int vw, int vh);
void sw3_clear(SwSurface *s, uint32_t argb, int color, int depth);
void sw3_projection(const float *m);
void sw3_lights(int n, const float *data);
void sw3_draw(SwSurface *s, const int32_t *verts, int vcount, const uint16_t *idx, int icount, const float *mv,
              const int32_t *state, const uint32_t *tex, int tw, int th);

/* ---- platform screen renderer ---- */
void r_init(void);
void r_clip(int x, int y, int w, int h);
void r_fill_rect(int x, int y, int w, int h, uint32_t argb);
void r_line(int x1, int y1, int x2, int y2, uint32_t argb);
void r_triangle(int x1, int y1, int x2, int y2, int x3, int y3, uint32_t argb);
void r_image(const uint32_t *pix, int stride, int iw, int ih, int sx, int sy, int sw, int sh, int transform,
             int dx, int dy, int alpha);
void r_rgb(const int32_t *argb, int off, int scan, int x, int y, int w, int h, int alpha);
void r_text(const uint16_t *text, int n, int x, int y, uint32_t argb, int scale);
void r_present(void);
void r_pixels_changed(const void *p, int bytes);

void r3_begin(int vx, int vy, int vw, int vh, int cx, int cy, int cw, int ch);
void r3_viewport(int x, int y, int w, int h);
void r3_end(void);
void r3_clear(uint32_t argb, int color, int depth);
void r3_projection(const float *m);
void r3_lights(int n, const float *data);
void r3_draw(const int32_t *verts, int vcount, const uint16_t *idx, int icount, const float *mv,
             const int32_t *state, const uint32_t *tex, int tw, int th);

/* shared helper: per-vertex M3G lighting in eye space; returns ABGR */
uint32_t m3g_light_vertex(const float *n_eye, const float *p_eye, uint32_t vcolor, const int32_t *state,
                          int nlights, const float *lights);

#endif
