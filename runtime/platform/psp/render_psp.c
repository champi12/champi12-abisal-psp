/*
 * PSP screen renderer on the GE (sceGu).
 *
 * 2D drawing uses through-mode primitives; textures are read straight from the
 * Java heap (pixel arrays are 16-byte aligned ABGR). M3G meshes use hardware
 * transform and lighting with the interleaved vertex layout produced by
 * javax.microedition.m3g.VertexBuffer, which matches the GE vertex order
 * (texture, color, normal, position).
 *
 * The draw buffer is kept across frames (copied back after each flip) because
 * MIDP's GameCanvas guarantees that its off-screen buffer persists.
 */
#include <pspdisplay.h>
#include <pspgu.h>
#include <pspgum.h>
#include <pspkernel.h>
#include <psputils.h>
#include <string.h>
#include "jvm.h"
#include "plat.h"
#include "render.h"

#define BUF_W 512
#define FB_BYTES (BUF_W * SCREEN_H * 4)
#define LIST_BYTES (1024 * 1024)

static unsigned int __attribute__((aligned(64))) gu_list[LIST_BYTES / 4];
static void *fbp0, *fbp1, *zbp; /* VRAM-relative */
static void *draw_vram;          /* VRAM-relative address of the current draw buffer */
static int clip_empty;
static int list_open;

static inline void *vram_abs(void *rel) { return (void *)(0x04000000 + (uintptr_t)rel); }

static void list_begin(void) {
    sceGuStart(GU_DIRECT, gu_list);
    list_open = 1;
}

static void list_end_sync(void) {
    if (!list_open) return;
    sceKernelDcacheWritebackAll();
    sceGuFinish();
    sceGuSync(0, 0);
    list_open = 0;
}

/* make sure the display list never overflows: flush long frames in pieces */
static void ensure_space(void) {
    if (sceGuCheckList() > LIST_BYTES - 64 * 1024) {
        list_end_sync();
        list_begin();
    }
}

void plat_gpu_sync(void) {
    list_end_sync();
    list_begin();
}

void r_init(void) {
    fbp0 = (void *)0;
    fbp1 = (void *)FB_BYTES;
    zbp = (void *)(2 * FB_BYTES);
    sceGuInit();
    list_begin();
    sceGuDrawBuffer(GU_PSM_8888, fbp0, BUF_W);
    sceGuDispBuffer(SCREEN_W, SCREEN_H, fbp1, BUF_W);
    sceGuDepthBuffer(zbp, BUF_W);
    sceGuOffset(2048 - (SCREEN_W / 2), 2048 - (SCREEN_H / 2));
    sceGuViewport(2048, 2048, SCREEN_W, SCREEN_H);
    sceGuDepthRange(65535, 0);
    sceGuScissor(0, 0, SCREEN_W, SCREEN_H);
    sceGuEnable(GU_SCISSOR_TEST);
    sceGuDepthFunc(GU_GEQUAL);
    sceGuShadeModel(GU_SMOOTH);
    sceGuEnable(GU_CLIP_PLANES);
    sceGuClearColor(0xff000000);
    sceGuClearDepth(0);
    sceGuClear(GU_COLOR_BUFFER_BIT | GU_DEPTH_BUFFER_BIT);
    list_end_sync();
    sceDisplayWaitVblankStart();
    sceGuDisplay(GU_TRUE);
    draw_vram = fbp0;
    list_begin();
}

/* ---- 2D ------------------------------------------------------------------------- */

typedef struct {
    unsigned int color;
    short x, y, z;
} CVert;

typedef struct {
    float u, v;
    float x, y, z;
} TVert;

static void state_2d(void) {
    sceGuDisable(GU_DEPTH_TEST);
    sceGuDepthMask(GU_TRUE);
    sceGuDisable(GU_LIGHTING);
    sceGuDisable(GU_CULL_FACE);
    sceGuDisable(GU_ALPHA_TEST);
    sceGuDisable(GU_FOG);
    sceGuPixelMask(0);
    sceGuShadeModel(GU_FLAT);
    sceGuEnable(GU_BLEND);
    sceGuBlendFunc(GU_ADD, GU_SRC_ALPHA, GU_ONE_MINUS_SRC_ALPHA, 0, 0);
}

void r_clip(int x, int y, int w, int h) {
    int x2 = x + w, y2 = y + h;
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x2 > SCREEN_W) x2 = SCREEN_W;
    if (y2 > SCREEN_H) y2 = SCREEN_H;
    clip_empty = x2 <= x || y2 <= y;
    if (!clip_empty) sceGuScissor(x, y, x2, y2);
}

void r_fill_rect(int x, int y, int w, int h, uint32_t argb) {
    if (clip_empty || w <= 0 || h <= 0) return;
    ensure_space();
    state_2d();
    sceGuDisable(GU_TEXTURE_2D);
    CVert *v = sceGuGetMemory(2 * sizeof(CVert));
    unsigned int c = argb_to_abgr(argb);
    v[0].color = c;
    v[0].x = (short)x;
    v[0].y = (short)y;
    v[0].z = 0;
    v[1].color = c;
    v[1].x = (short)(x + w);
    v[1].y = (short)(y + h);
    v[1].z = 0;
    sceGuDrawArray(GU_SPRITES, GU_COLOR_8888 | GU_VERTEX_16BIT | GU_TRANSFORM_2D, 2, 0, v);
}

void r_line(int x1, int y1, int x2, int y2, uint32_t argb) {
    if (clip_empty) return;
    ensure_space();
    state_2d();
    sceGuDisable(GU_TEXTURE_2D);
    CVert *v = sceGuGetMemory(2 * sizeof(CVert));
    unsigned int c = argb_to_abgr(argb);
    v[0].color = c;
    v[0].x = (short)x1;
    v[0].y = (short)y1;
    v[0].z = 0;
    v[1].color = c;
    v[1].x = (short)x2;
    v[1].y = (short)y2;
    v[1].z = 0;
    sceGuDrawArray(GU_LINES, GU_COLOR_8888 | GU_VERTEX_16BIT | GU_TRANSFORM_2D, 2, 0, v);
    /* lines omit their last pixel; MIDP includes it */
    r_fill_rect(x2, y2, 1, 1, argb);
}

void r_triangle(int x1, int y1, int x2, int y2, int x3, int y3, uint32_t argb) {
    if (clip_empty) return;
    ensure_space();
    state_2d();
    sceGuDisable(GU_TEXTURE_2D);
    CVert *v = sceGuGetMemory(3 * sizeof(CVert));
    unsigned int c = argb_to_abgr(argb);
    int xs[3] = { x1, x2, x3 }, ys[3] = { y1, y2, y3 };
    for (int i = 0; i < 3; i++) {
        v[i].color = c;
        v[i].x = (short)xs[i];
        v[i].y = (short)ys[i];
        v[i].z = 0;
    }
    sceGuDrawArray(GU_TRIANGLES, GU_COLOR_8888 | GU_VERTEX_16BIT | GU_TRANSFORM_2D, 3, 0, v);
}

static int pow2(int v) {
    int p = 1;
    while (p < v) p <<= 1;
    return p;
}

static void bind_texture(const uint32_t *pix, int stride, int w, int h, int linear) {
    sceGuEnable(GU_TEXTURE_2D);
    sceGuTexMode(GU_PSM_8888, 0, 0, 0);
    sceGuTexImage(0, pow2(w), pow2(h), stride, pix);
    sceGuTexFunc(GU_TFX_REPLACE, GU_TCC_RGBA);
    sceGuTexFilter(linear ? GU_LINEAR : GU_NEAREST, linear ? GU_LINEAR : GU_NEAREST);
    sceGuTexWrap(GU_CLAMP, GU_CLAMP);
    sceGuTexScale(1.0f, 1.0f);
    sceGuTexOffset(0.0f, 0.0f);
    sceGuTexFlush();
}

/* continuous version of map_transform for quad corners */
static void corner_uv(int t, int w, int h, float a, float b, float *u, float *v) {
    switch (t) {
    case 0: *u = a; *v = b; break;
    case 2: *u = w - a; *v = b; break;
    case 1: *u = a; *v = h - b; break;
    case 3: *u = w - a; *v = h - b; break;
    case 5: *u = a; *v = h - b; break;
    case 6: *u = w - a; *v = b; break;
    case 7: *u = w - a; *v = h - b; break;
    default: *u = a; *v = b; break;
    }
}

/* draw a region of at most 512x512 texels */
static void image_piece(const uint32_t *pix, int stride, int sx, int sy, int w, int h, int t, int dx, int dy,
                        int alpha) {
    /* texture origin at (sx & ~3, sy) keeps the base 16-byte aligned */
    int ox = sx & ~3;
    const uint32_t *base = pix + (size_t)sy * stride + ox;
    int tw = (sx - ox) + w;
    bind_texture(base, stride, tw, h, 0);
    if (alpha) sceGuEnable(GU_BLEND);
    else sceGuDisable(GU_BLEND);
    int swap = (t & 4) != 0;
    int dw = swap ? h : w, dh = swap ? w : h;
    TVert *v = sceGuGetMemory(4 * sizeof(TVert));
    static const int ca[4][2] = { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } };
    for (int i = 0; i < 4; i++) {
        float da = ca[i][0] * dw, db = ca[i][1] * dh;
        float a = swap ? db : da, b = swap ? da : db, u, vv;
        corner_uv(t, w, h, a, b, &u, &vv);
        v[i].u = u + (sx - ox);
        v[i].v = vv;
        v[i].x = (float)(dx + da);
        v[i].y = (float)(dy + db);
        v[i].z = 0;
    }
    if (t == 0) {
        /* axis aligned: a sprite is cheaper and exact */
        v[1] = v[3];
        sceGuDrawArray(GU_SPRITES, GU_TEXTURE_32BITF | GU_VERTEX_32BITF | GU_TRANSFORM_2D, 2, 0, v);
    } else {
        sceGuDrawArray(GU_TRIANGLE_STRIP, GU_TEXTURE_32BITF | GU_VERTEX_32BITF | GU_TRANSFORM_2D, 4, 0, v);
    }
}

void r_image(const uint32_t *pix, int stride, int iw, int ih, int sx, int sy, int sw, int sh, int t, int dx, int dy,
             int alpha) {
    (void)iw;
    (void)ih;
    if (clip_empty || sw <= 0 || sh <= 0) return;
    ensure_space();
    state_2d();
    if (sw <= 508 && sh <= 512) {
        image_piece(pix, stride, sx, sy, sw, sh, t, dx, dy, alpha);
        return;
    }
    /* large images: only untransformed drawing is split into tiles */
    for (int ty = 0; ty < sh; ty += 256) {
        for (int tx = 0; tx < sw; tx += 256) {
            int w = sw - tx < 256 ? sw - tx : 256, h = sh - ty < 256 ? sh - ty : 256;
            image_piece(pix, stride, sx + tx, sy + ty, w, h, 0, dx + tx, dy + ty, alpha);
        }
    }
}

void r_rgb(const int32_t *argb, int off, int scan, int x, int y, int w, int h, int alpha) {
    if (clip_empty) return;
    for (int ty = 0; ty < h; ty += 256) {
        for (int tx = 0; tx < w; tx += 256) {
            int pw = w - tx < 256 ? w - tx : 256, ph = h - ty < 256 ? h - ty : 256;
            int stride = (pw + 3) & ~3;
            ensure_space();
            uint32_t *tex = sceGuGetMemory((unsigned)(stride * ph * 4 + 16));
            tex = (uint32_t *)(((uintptr_t)tex + 15) & ~(uintptr_t)15);
            for (int j = 0; j < ph; j++) {
                const int32_t *src = argb + off + (ty + j) * scan + tx;
                uint32_t *dst = tex + j * stride;
                for (int i = 0; i < pw; i++) {
                    uint32_t c = argb_to_abgr((uint32_t)src[i]);
                    dst[i] = alpha ? c : (c | 0xff000000u);
                }
            }
            state_2d();
            image_piece(tex, stride, 0, 0, pw, ph, 0, x + tx, y + ty, alpha);
        }
    }
}

void r_text(const uint16_t *text, int n, int x, int y, uint32_t argb, int scale) {
    /* debug text: rasterize the built-in font into a small texture */
    if (clip_empty || n <= 0) return;
    int w = n * 6 * scale, h = 8 * scale;
    int stride = (w + 3) & ~3;
    ensure_space();
    uint32_t *tex = sceGuGetMemory((unsigned)(stride * h * 4 + 16));
    tex = (uint32_t *)(((uintptr_t)tex + 15) & ~(uintptr_t)15);
    memset(tex, 0, (size_t)stride * h * 4);
    SwSurface s = { tex, stride, w, h, NULL, 0, 0, w, h };
    sw_text(&s, text, n, 0, 0, argb, scale);
    state_2d();
    image_piece(tex, stride, 0, 0, w, h, 0, x, y, 1);
}

void r_pixels_changed(const void *p, int bytes) {
    (void)p;
    (void)bytes; /* the whole data cache is written back before each GE submission */
}

int preserve_frame = 1;

void r_present(void) {
    /* The global lock stays held: other Java threads may draw too, and nothing may be
     * added to the display list between finishing it and starting the next one. */
    list_end_sync();
    sceDisplayWaitVblankStart();
    void *shown = draw_vram;
    draw_vram = sceGuSwapBuffers();
    list_begin();
    /* carry the frame over so the back buffer keeps the canvas contents (MIDP semantics);
     * games that redraw the whole screen every frame turn this off to save GE time */
    if (preserve_frame) sceGuCopyImage(GU_PSM_8888, 0, 0, SCREEN_W, SCREEN_H, BUF_W, vram_abs(shown), 0, 0, BUF_W,
                   vram_abs(draw_vram));
    sceGuTexSync();
}

/* ---- 3D ------------------------------------------------------------------------- */

static int vp_x, vp_y, vp_w, vp_h;
static int clip3[4];

static void to_gu_matrix(const float *rm, ScePspFMatrix4 *out) {
    float *o = (float *)out;
    for (int r = 0; r < 4; r++) {
        for (int c = 0; c < 4; c++) o[c * 4 + r] = rm[r * 4 + c];
    }
}

static void apply_viewport(void) {
    sceGuOffset(2048 - (SCREEN_W / 2), 2048 - (SCREEN_H / 2));
    sceGuViewport(2048 - SCREEN_W / 2 + vp_x + vp_w / 2, 2048 - SCREEN_H / 2 + vp_y + vp_h / 2, vp_w, vp_h);
}

void r3_begin(int vx, int vy, int vw, int vh, int cx, int cy, int cw, int ch) {
    vp_x = vx;
    vp_y = vy;
    vp_w = vw;
    vp_h = vh;
    clip3[0] = cx;
    clip3[1] = cy;
    clip3[2] = cw;
    clip3[3] = ch;
    ensure_space();
    apply_viewport();
    r_clip(cx, cy, cw, ch);
    ScePspFMatrix4 id;
    gumLoadIdentity(&id);
    sceGuSetMatrix(GU_VIEW, &id);
}

void r3_viewport(int x, int y, int w, int h) {
    vp_x = x;
    vp_y = y;
    vp_w = w;
    vp_h = h;
    apply_viewport();
}

void r3_end(void) {
    sceGuDisable(GU_DEPTH_TEST);
    sceGuDisable(GU_LIGHTING);
    sceGuDisable(GU_FOG);
    sceGuDisable(GU_CULL_FACE);
    sceGuDisable(GU_ALPHA_TEST);
}

void r3_clear(uint32_t argb, int color, int depth) {
    if (!color && !depth) return;
    ensure_space();
    r_clip(clip3[0], clip3[1], clip3[2], clip3[3]);
    if (clip_empty) return;
    sceGuClearColor(argb_to_abgr(argb));
    sceGuClearDepth(0);
    sceGuDepthMask(GU_FALSE);
    sceGuClear((color ? GU_COLOR_BUFFER_BIT : 0) | (depth ? GU_DEPTH_BUFFER_BIT : 0));
}

static float cur_proj[16];

void r3_projection(const float *m) {
    memcpy(cur_proj, m, sizeof cur_proj);
    ScePspFMatrix4 p;
    to_gu_matrix(m, &p);
    sceGuSetMatrix(GU_PROJECTION, &p);
}

static int n_lights;
static float light_data[8 * 16];

void r3_lights(int n, const float *d) {
    n_lights = n > 8 ? 8 : n;
    memcpy(light_data, d, (size_t)n_lights * 16 * sizeof(float));
}

static unsigned int rgb_scaled(float r, float g, float b, unsigned a) {
    int R = (int)(r * 255), G = (int)(g * 255), B = (int)(b * 255);
    R = R < 0 ? 0 : R > 255 ? 255 : R;
    G = G < 0 ? 0 : G > 255 ? 255 : G;
    B = B < 0 ? 0 : B > 255 ? 255 : B;
    return (a << 24) | ((unsigned)B << 16) | ((unsigned)G << 8) | (unsigned)R;
}

static void setup_lights(const int32_t *st) {
    float ar = 0, ag = 0, ab = 0;
    int hw = 0;
    for (int i = 0; i < 4; i++) sceGuDisable(GU_LIGHT0 + i);
    for (int i = 0; i < n_lights; i++) {
        const float *L = light_data + i * 16;
        int mode = (int)L[0];
        if (mode == M3G_LIGHT_AMBIENT) {
            ar += L[1];
            ag += L[2];
            ab += L[3];
            continue;
        }
        if (hw >= 4) continue;
        ScePspFVector3 pos;
        int type;
        if (mode == M3G_LIGHT_DIRECTIONAL) {
            type = GU_DIRECTIONAL;
            pos.x = -L[7];
            pos.y = -L[8];
            pos.z = -L[9];
        } else {
            type = mode == M3G_LIGHT_SPOT ? GU_SPOTLIGHT : GU_POINTLIGHT;
            pos.x = L[4];
            pos.y = L[5];
            pos.z = L[6];
        }
        sceGuLight(hw, type, GU_DIFFUSE_AND_SPECULAR, &pos);
        unsigned int c = rgb_scaled(L[1], L[2], L[3], 0xff);
        sceGuLightColor(hw, GU_DIFFUSE, c);
        sceGuLightColor(hw, GU_SPECULAR, c);
        sceGuLightColor(hw, GU_AMBIENT, 0);
        sceGuLightAtt(hw, L[10], L[11], L[12]);
        if (type == GU_SPOTLIGHT) {
            ScePspFVector3 dir = { L[7], L[8], L[9] };
            sceGuLightSpot(hw, &dir, L[14], cosf(L[13] * 3.14159265f / 180.0f));
        }
        sceGuEnable(GU_LIGHT0 + hw);
        hw++;
    }
    sceGuAmbient(rgb_scaled(ar, ag, ab, 0xff));
    (void)st;
}

static unsigned int m3g_to_abgr(uint32_t argb) { return argb_to_abgr(argb); }

/* ---- CPU clipping ------------------------------------------------------------------
 * The GE neither clips triangles against the near plane properly nor draws triangles
 * whose vertices fall outside its 4096-pixel coordinate range. Meshes that have such
 * vertices are clipped here, in object space, against the near plane and a guard band.
 */
typedef struct GVert {
    float u, v;
    uint32_t color;
    float nx, ny, nz;
    float x, y, z;
} GVert;

#define N_PLANES 5
/* guard band in NDC units, a little inside the GE's +-2048 pixel limit */
#define GUARD_X (1900.0f / (SCREEN_W / 2))
#define GUARD_Y (1900.0f / (SCREEN_H / 2))

static float planes[N_PLANES][4];
/* the visible screen (+10%), used only to cull whole meshes */
static float vis[4][4];

static void build_planes(const float *mv) {
    float m[16];
    for (int r = 0; r < 4; r++) {
        for (int c = 0; c < 4; c++) {
            m[r * 4 + c] = cur_proj[r * 4] * mv[c] + cur_proj[r * 4 + 1] * mv[4 + c] +
                           cur_proj[r * 4 + 2] * mv[8 + c] + cur_proj[r * 4 + 3] * mv[12 + c];
        }
    }
    const float *X = m, *Y = m + 4, *Z = m + 8, *W = m + 12;
    for (int c = 0; c < 4; c++) {
        planes[0][c] = Z[c] + W[c] * 1.0f;      /* near: z + w >= 0 */
        planes[1][c] = W[c] * GUARD_X - X[c];   /* x <= g w */
        planes[2][c] = W[c] * GUARD_X + X[c];   /* x >= -g w */
        planes[3][c] = W[c] * GUARD_Y - Y[c];
        planes[4][c] = W[c] * GUARD_Y + Y[c];
        vis[0][c] = W[c] * 1.1f - X[c];
        vis[1][c] = W[c] * 1.1f + X[c];
        vis[2][c] = W[c] * 1.1f - Y[c];
        vis[3][c] = W[c] * 1.1f + Y[c];
    }
    /* keep a small distance in front of the near plane */
    planes[0][3] -= 1e-3f;
}

static inline float plane_dist(int p, const GVert *v) {
    return planes[p][0] * v->x + planes[p][1] * v->y + planes[p][2] * v->z + planes[p][3];
}

static void lerp_vert(const GVert *a, const GVert *b, float t, GVert *o) {
    o->u = a->u + (b->u - a->u) * t;
    o->v = a->v + (b->v - a->v) * t;
    o->nx = a->nx + (b->nx - a->nx) * t;
    o->ny = a->ny + (b->ny - a->ny) * t;
    o->nz = a->nz + (b->nz - a->nz) * t;
    o->x = a->x + (b->x - a->x) * t;
    o->y = a->y + (b->y - a->y) * t;
    o->z = a->z + (b->z - a->z) * t;
    uint32_t ca = a->color, cb = b->color, c = 0;
    for (int sh = 0; sh < 32; sh += 8) {
        int ea = (ca >> sh) & 0xff, eb = (cb >> sh) & 0xff;
        c |= (uint32_t)(ea + (int)((eb - ea) * t)) << sh;
    }
    o->color = c;
}

/* Bounding-box test against the clip planes using the 6 floats stored after the vertices
 * (see VertexBuffer.vertices). Returns 0 = completely outside one plane (cull),
 * 1 = completely inside all planes (draw as is), 2 = crosses a plane (clip). */
static int box_test(const int32_t *verts, int vcount) {
    float b[6];
    memcpy(b, verts + vcount * VSTRIDE, sizeof b);
    if (!(b[0] <= b[3])) return 2; /* no box (empty or NaN): fall back to clipping */
    /* off screen to a side: nothing to draw */
    for (int p = 0; p < 4; p++) {
        int c;
        for (c = 0; c < 8; c++) {
            float x = b[(c & 1) ? 3 : 0], y = b[(c & 2) ? 4 : 1], z = b[(c & 4) ? 5 : 2];
            if (vis[p][0] * x + vis[p][1] * y + vis[p][2] * z + vis[p][3] >= 0) break;
        }
        if (c == 8) return 0;
    }
    int all_in = 1;
    for (int p = 0; p < N_PLANES; p++) {
        int in = 0;
        for (int c = 0; c < 8; c++) {
            GVert g;
            g.x = b[(c & 1) ? 3 : 0];
            g.y = b[(c & 2) ? 4 : 1];
            g.z = b[(c & 4) ? 5 : 2];
            if (plane_dist(p, &g) >= 0) in++;
        }
        if (in == 0) {
            /* the near plane and the guard band are wider than the screen, so this only
             * rejects what is really out of view */
            return 0;
        }
        if (in < 8) all_in = 0;
    }
    return all_in ? 1 : 2;
}

static int clip_polygon(GVert *poly, int n, GVert *tmp) {
    for (int p = 0; p < N_PLANES && n >= 3; p++) {
        int m = 0;
        for (int i = 0; i < n; i++) {
            const GVert *a = &poly[i], *b = &poly[(i + 1) % n];
            float da = plane_dist(p, a), db = plane_dist(p, b);
            if (da >= 0) tmp[m++] = *a;
            if ((da >= 0) != (db >= 0)) lerp_vert(a, b, da / (da - db), &tmp[m++]);
        }
        memcpy(poly, tmp, sizeof(GVert) * (size_t)m);
        n = m;
    }
    return n;
}

/* Clip an indexed mesh into a plain triangle list in display-list memory. */
static GVert *clip_mesh(const GVert *v, int vcount, const uint16_t *idx, int icount, int *out_count) {
    /* Room for the clipped vertices comes out of the display list: make sure it fits.
     * (Before, up to icount * 3 vertices were taken while only 64 KB were guaranteed free,
     * which overflowed the list with big meshes and crashed real hardware.) */
    int cap = icount * 2 + 64;
    unsigned need = (unsigned)(sizeof(GVert) * (size_t)cap) + 4096;
    if (need > LIST_BYTES / 2) {
        cap = (int)((LIST_BYTES / 2 - 4096) / sizeof(GVert));
        need = LIST_BYTES / 2;
    }
    if ((unsigned)sceGuCheckList() + need > LIST_BYTES - 16 * 1024) {
        list_end_sync();
        list_begin();
    }
    GVert *out = sceGuGetMemory((unsigned)(sizeof(GVert) * (size_t)cap));
    int n = 0;
    GVert poly[16], tmp[16];
    for (int i = 0; i + 2 < icount; i += 3) {
        if (idx[i] >= vcount || idx[i + 1] >= vcount || idx[i + 2] >= vcount) continue;
        const GVert *a = &v[idx[i]], *b = &v[idx[i + 1]], *c = &v[idx[i + 2]];
        int out_all = 0, in_all = 1;
        for (int p = 0; p < N_PLANES; p++) {
            float da = plane_dist(p, a), db = plane_dist(p, b), dc = plane_dist(p, c);
            if (da < 0 && db < 0 && dc < 0) {
                out_all = 1;
                break;
            }
            if (da < 0 || db < 0 || dc < 0) in_all = 0;
        }
        if (out_all) continue;
        if (in_all) {
            if (n + 3 > cap) break;
            out[n++] = *a;
            out[n++] = *b;
            out[n++] = *c;
            continue;
        }
        poly[0] = *a;
        poly[1] = *b;
        poly[2] = *c;
        int m = clip_polygon(poly, 3, tmp);
        for (int k = 1; k + 1 < m; k++) {
            if (n + 3 > cap) break;
            out[n++] = poly[0];
            out[n++] = poly[k];
            out[n++] = poly[k + 1];
        }
    }
    *out_count = n;
    return out;
}

void r3_draw(const int32_t *verts, int vcount, const uint16_t *idx, int icount, const float *mv,
             const int32_t *st, const uint32_t *tex, int tw, int th) {
    if (clip_empty || icount <= 0) return;
    ensure_space();
    r_clip(clip3[0], clip3[1], clip3[2], clip3[3]);
    if (clip_empty) return;
    int flags = st[ST_FLAGS];
    ScePspFMatrix4 m;
    to_gu_matrix(mv, &m);
    sceGuSetMatrix(GU_MODEL, &m);

    /* depth */
    if (flags & SF_DEPTH_TEST) sceGuEnable(GU_DEPTH_TEST);
    else sceGuDisable(GU_DEPTH_TEST);
    sceGuDepthFunc(GU_GEQUAL);
    sceGuDepthMask((flags & SF_DEPTH_WRITE) ? GU_FALSE : GU_TRUE);
    sceGuPixelMask((flags & SF_COLOR_WRITE) ? 0 : 0xffffffff);

    /* culling: sceGuFrontFace names the winding that is kept */
    int cull = st[ST_CULL];
    if (cull == M3G_CULL_NONE) {
        sceGuDisable(GU_CULL_FACE);
    } else {
        int ccw_front = st[ST_WINDING] != M3G_WINDING_CW;
        if (cull == M3G_CULL_FRONT) ccw_front = !ccw_front;
        sceGuFrontFace(ccw_front ? GU_CCW : GU_CW);
        sceGuEnable(GU_CULL_FACE);
    }
    sceGuShadeModel((flags & SF_SMOOTH) ? GU_SMOOTH : GU_FLAT);

    /* lighting */
    if (flags & SF_LIGHTING) {
        setup_lights(st);
        uint32_t dif = (uint32_t)st[ST_DIFFUSE];
        unsigned int amb = m3g_to_abgr(((uint32_t)st[ST_AMBIENT] & 0xffffff) | (dif & 0xff000000u));
        sceGuColorMaterial((flags & SF_TRACKING) ? (GU_AMBIENT | GU_DIFFUSE) : 0);
        sceGuModelColor(m3g_to_abgr((uint32_t)st[ST_EMISSIVE] | 0xff000000u), amb, m3g_to_abgr(dif),
                        m3g_to_abgr((uint32_t)st[ST_SPECULAR] | 0xff000000u));
        float shin;
        memcpy(&shin, &st[ST_SHININESS], 4);
        sceGuSpecular(shin);
        sceGuEnable(GU_LIGHTING);
    } else {
        sceGuDisable(GU_LIGHTING);
    }

    /* texture */
    if ((flags & SF_TEXTURE) && tex) {
        sceGuEnable(GU_TEXTURE_2D);
        sceGuTexMode(GU_PSM_8888, 0, 0, 0);
        sceGuTexImage(0, tw, th, tw, tex);
        int fn;
        switch (st[ST_TEXFUNC]) {
        case M3G_FUNC_REPLACE: fn = GU_TFX_REPLACE; break;
        case M3G_FUNC_DECAL: fn = GU_TFX_DECAL; break;
        case M3G_FUNC_BLEND: fn = GU_TFX_BLEND; break;
        case M3G_FUNC_ADD: fn = GU_TFX_ADD; break;
        default: fn = GU_TFX_MODULATE; break;
        }
        sceGuTexFunc(fn, st[ST_TEXALPHA] ? GU_TCC_RGBA : GU_TCC_RGB);
        sceGuTexEnvColor(m3g_to_abgr((uint32_t)st[ST_TEXBLENDCOLOR]));
        int filt = st[ST_FILTER] == M3G_FILTER_LINEAR ? GU_LINEAR : GU_NEAREST;
        sceGuTexFilter(filt, filt);
        sceGuTexWrap(st[ST_WRAPS] == M3G_WRAP_CLAMP ? GU_CLAMP : GU_REPEAT,
                     st[ST_WRAPT] == M3G_WRAP_CLAMP ? GU_CLAMP : GU_REPEAT);
        sceGuTexScale(1.0f, 1.0f);
        sceGuTexOffset(0.0f, 0.0f);
        sceGuTexFlush();
    } else {
        sceGuDisable(GU_TEXTURE_2D);
    }

    /* compositing */
    switch (st[ST_BLEND]) {
    case M3G_ALPHA:
        sceGuEnable(GU_BLEND);
        sceGuBlendFunc(GU_ADD, GU_SRC_ALPHA, GU_ONE_MINUS_SRC_ALPHA, 0, 0);
        break;
    case M3G_ALPHA_ADD:
        sceGuEnable(GU_BLEND);
        sceGuBlendFunc(GU_ADD, GU_SRC_ALPHA, GU_FIX, 0, 0xffffff);
        break;
    case M3G_MODULATE:
        sceGuEnable(GU_BLEND);
        sceGuBlendFunc(GU_ADD, GU_DST_COLOR, GU_FIX, 0, 0);
        break;
    case M3G_MODULATE_X2:
        sceGuEnable(GU_BLEND);
        sceGuBlendFunc(GU_ADD, GU_DST_COLOR, GU_SRC_COLOR, 0, 0);
        break;
    default:
        sceGuDisable(GU_BLEND);
        break;
    }
    if (st[ST_ALPHA_THRESHOLD] > 0) {
        sceGuAlphaFunc(GU_GEQUAL, st[ST_ALPHA_THRESHOLD], 0xff);
        sceGuEnable(GU_ALPHA_TEST);
    } else {
        sceGuDisable(GU_ALPHA_TEST);
    }

    /* fog */
    if (st[ST_FOGMODE]) {
        float fn, ff, fd;
        memcpy(&fn, &st[ST_FOGNEAR], 4);
        memcpy(&ff, &st[ST_FOGFAR], 4);
        memcpy(&fd, &st[ST_FOGDENSITY], 4);
        if (st[ST_FOGMODE] == M3G_FOG_EXP) {
            fn = 0;
            ff = fd > 0 ? 3.0f / fd : 1000.0f;
        }
        sceGuFog(fn, ff, m3g_to_abgr((uint32_t)st[ST_FOGCOLOR]));
        sceGuEnable(GU_FOG);
    } else {
        sceGuDisable(GU_FOG);
    }

    const void *vdata = verts;
    int af = st[ST_ALPHAFACTOR];
    if (af < 255) {
        /* node alpha factor: scale vertex alpha in a temporary copy */
        if ((unsigned)sceGuCheckList() + (unsigned)(vcount * VSTRIDE * 4) + 4096 > LIST_BYTES - 16 * 1024) {
            list_end_sync();
            list_begin();
        }
        int32_t *copy = sceGuGetMemory((unsigned)(vcount * VSTRIDE * 4));
        memcpy(copy, verts, (size_t)vcount * VSTRIDE * 4);
        for (int i = 0; i < vcount; i++) {
            uint32_t c = (uint32_t)copy[i * VSTRIDE + 2];
            uint32_t a = ((c >> 24) * (uint32_t)af) / 255;
            copy[i * VSTRIDE + 2] = (int32_t)((c & 0xffffff) | (a << 24));
        }
        vdata = copy;
        if (flags & SF_LIGHTING) {
            /* lit alpha comes from the material; fold the factor into it */
            uint32_t dif = (uint32_t)st[ST_DIFFUSE];
            uint32_t a = ((dif >> 24) * (uint32_t)af) / 255;
            unsigned int amb = m3g_to_abgr(((uint32_t)st[ST_AMBIENT] & 0xffffff) | (a << 24));
            sceGuModelColor(m3g_to_abgr((uint32_t)st[ST_EMISSIVE] | 0xff000000u), amb,
                            m3g_to_abgr((dif & 0xffffff) | (a << 24)),
                            m3g_to_abgr((uint32_t)st[ST_SPECULAR] | 0xff000000u));
        }
    }
    const int vfmt = GU_TEXTURE_32BITF | GU_COLOR_8888 | GU_NORMAL_32BITF | GU_VERTEX_32BITF | GU_TRANSFORM_3D;
    build_planes(mv);
    int caja = box_test(verts, vcount);
    if (caja == 0) return;
    if (caja == 1) {
        sceGuDrawArray(GU_TRIANGLES, vfmt | GU_INDEX_16BIT, icount, idx, vdata);
    } else {
        int n;
        GVert *clipped = clip_mesh((const GVert *)vdata, vcount, idx, icount, &n);
        if (n > 0) sceGuDrawArray(GU_TRIANGLES, vfmt, n, NULL, clipped);
    }
}
