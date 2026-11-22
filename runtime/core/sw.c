/* Software rasterizer: 2D primitives and an M3G-style 3D pipeline on ABGR surfaces. */
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "render.h"

/* ---- helpers ------------------------------------------------------------------- */

static inline uint32_t blend_px(uint32_t dst, uint32_t src) {
    uint32_t a = src >> 24;
    if (a == 255) return src;
    if (a == 0) return dst;
    uint32_t ia = 255 - a;
    uint32_t rb = (((src & 0x00ff00ffu) * a + (dst & 0x00ff00ffu) * ia) >> 8) & 0x00ff00ffu;
    uint32_t g = (((src & 0x0000ff00u) * a + (dst & 0x0000ff00u) * ia) >> 8) & 0x0000ff00u;
    uint32_t da = dst >> 24;
    uint32_t oa = a + ((da * ia) >> 8);
    return (oa << 24) | rb | g;
}

void sw_clip(SwSurface *s, int x, int y, int w, int h) {
    int x2 = x + w, y2 = y + h;
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (x2 > s->w) x2 = s->w;
    if (y2 > s->h) y2 = s->h;
    s->cx = x;
    s->cy = y;
    s->cw = x2 > x ? x2 - x : 0;
    s->ch = y2 > y ? y2 - y : 0;
}

void sw_fill_rect(SwSurface *s, int x, int y, int w, int h, uint32_t argb) {
    int x1 = x, y1 = y, x2 = x + w, y2 = y + h;
    if (x1 < s->cx) x1 = s->cx;
    if (y1 < s->cy) y1 = s->cy;
    if (x2 > s->cx + s->cw) x2 = s->cx + s->cw;
    if (y2 > s->cy + s->ch) y2 = s->cy + s->ch;
    if (x1 >= x2 || y1 >= y2) return;
    uint32_t c = argb_to_abgr(argb);
    for (int yy = y1; yy < y2; yy++) {
        uint32_t *row = s->pix + (size_t)yy * s->stride;
        if ((c >> 24) == 255) {
            for (int xx = x1; xx < x2; xx++) row[xx] = c;
        } else {
            for (int xx = x1; xx < x2; xx++) row[xx] = blend_px(row[xx], c);
        }
    }
}

static inline void plot(SwSurface *s, int x, int y, uint32_t c) {
    if (x < s->cx || y < s->cy || x >= s->cx + s->cw || y >= s->cy + s->ch) return;
    uint32_t *p = s->pix + (size_t)y * s->stride + x;
    *p = blend_px(*p, c);
}

void sw_line(SwSurface *s, int x1, int y1, int x2, int y2, uint32_t argb) {
    uint32_t c = argb_to_abgr(argb);
    int dx = abs(x2 - x1), sx = x1 < x2 ? 1 : -1;
    int dy = -abs(y2 - y1), sy = y1 < y2 ? 1 : -1;
    int err = dx + dy;
    for (int guard = 0; guard < 8192; guard++) {
        plot(s, x1, y1, c);
        if (x1 == x2 && y1 == y2) break;
        int e2 = 2 * err;
        if (e2 >= dy) {
            err += dy;
            x1 += sx;
        }
        if (e2 <= dx) {
            err += dx;
            y1 += sy;
        }
    }
}

void sw_triangle(SwSurface *s, int x1, int y1, int x2, int y2, int x3, int y3, uint32_t argb) {
    uint32_t c = argb_to_abgr(argb);
    int minx = x1 < x2 ? (x1 < x3 ? x1 : x3) : (x2 < x3 ? x2 : x3);
    int maxx = x1 > x2 ? (x1 > x3 ? x1 : x3) : (x2 > x3 ? x2 : x3);
    int miny = y1 < y2 ? (y1 < y3 ? y1 : y3) : (y2 < y3 ? y2 : y3);
    int maxy = y1 > y2 ? (y1 > y3 ? y1 : y3) : (y2 > y3 ? y2 : y3);
    if (minx < s->cx) minx = s->cx;
    if (miny < s->cy) miny = s->cy;
    if (maxx > s->cx + s->cw - 1) maxx = s->cx + s->cw - 1;
    if (maxy > s->cy + s->ch - 1) maxy = s->cy + s->ch - 1;
    long area = (long)(x2 - x1) * (y3 - y1) - (long)(x3 - x1) * (y2 - y1);
    if (area == 0) {
        sw_line(s, x1, y1, x2, y2, argb);
        sw_line(s, x2, y2, x3, y3, argb);
        return;
    }
    for (int y = miny; y <= maxy; y++) {
        for (int x = minx; x <= maxx; x++) {
            long w0 = (long)(x2 - x1) * (y - y1) - (long)(y2 - y1) * (x - x1);
            long w1 = (long)(x3 - x2) * (y - y2) - (long)(y3 - y2) * (x - x2);
            long w2 = (long)(x1 - x3) * (y - y3) - (long)(y1 - y3) * (x - x3);
            if ((w0 >= 0 && w1 >= 0 && w2 >= 0) || (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                uint32_t *p = s->pix + (size_t)y * s->stride + x;
                *p = blend_px(*p, c);
            }
        }
    }
}

void sw_image(SwSurface *s, const uint32_t *pix, int stride, int sx, int sy, int w, int h, int t, int dx,
              int dy, int alpha) {
    int swap = (t & 4) != 0;
    int dw = swap ? h : w, dh = swap ? w : h;
    int x1 = dx, y1 = dy, x2 = dx + dw, y2 = dy + dh;
    if (x1 < s->cx) x1 = s->cx;
    if (y1 < s->cy) y1 = s->cy;
    if (x2 > s->cx + s->cw) x2 = s->cx + s->cw;
    if (y2 > s->cy + s->ch) y2 = s->cy + s->ch;
    for (int y = y1; y < y2; y++) {
        uint32_t *row = s->pix + (size_t)y * s->stride;
        for (int x = x1; x < x2; x++) {
            int a = x - dx, b = y - dy, u, v;
            if (swap) {
                int tmp = a;
                a = b;
                b = tmp;
            }
            map_transform(t, w, h, a, b, &u, &v);
            uint32_t c = pix[(size_t)(sy + v) * stride + sx + u];
            row[x] = alpha ? blend_px(row[x], c) : (c | 0xff000000u);
        }
    }
}

void sw_rgb(SwSurface *s, const int32_t *rgb, int off, int scan, int x, int y, int w, int h, int alpha) {
    for (int j = 0; j < h; j++) {
        int yy = y + j;
        if (yy < s->cy || yy >= s->cy + s->ch) continue;
        uint32_t *row = s->pix + (size_t)yy * s->stride;
        for (int i = 0; i < w; i++) {
            int xx = x + i;
            if (xx < s->cx || xx >= s->cx + s->cw) continue;
            uint32_t c = argb_to_abgr((uint32_t)rgb[off + j * scan + i]);
            row[xx] = alpha ? blend_px(row[xx], c) : (c | 0xff000000u);
        }
    }
}

/* tiny 4x6 built-in font for debug text: digits, uppercase letters and a few symbols */
static const char *font_chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ.:-/%() ,;!?+=[]\\'\"<>_*#&";
static const uint32_t font_bits[] = {
    0x699996, 0x262227, 0x69124F, 0xE16196, 0x99F111, 0xF8E11E, 0x68E996, 0xF12444, 0x696996, 0x699716,
    0x699F99, 0xE9E99E, 0x698896, 0xE9999E, 0xF8E88F, 0xF8E888, 0x698B96, 0x99F999, 0x722227, 0x711196,
    0x9ACA99, 0x88888F, 0x9FF999, 0x9DDBB9, 0x699996, 0xE99E88, 0x6999A5, 0xE99EA9, 0x786116, 0xF22222,
    0x999996, 0x999966, 0x999FF9, 0x996699, 0x996222, 0xF1248F, 0x000006, 0x060060, 0x00F000, 0x112448,
    0x912249, 0x244442, 0x422224, 0x000000,
    0x000048, 0x040048, 0x444404, 0x692404, 0x04E400, 0x0E0E00, 0xC8888C, 0x622226, 0x884422, 0x440000, 0xAA0000, 0x248420, 0x842480, 0x00000F, 0x0A4A00, 0xAFAFA0, 0x4A4B96,
};

/* Spanish letters: a base letter with a mark drawn in the free pixel row above it
 * (1 acute, 2 diaeresis, 3 tilde), and the inverted marks as glyphs of their own */
static int sw_marca(int ch, int *base) {
    switch (ch) {
    case 0xC1: case 0xE1: *base = 'A'; return 1;
    case 0xC9: case 0xE9: *base = 'E'; return 1;
    case 0xCD: case 0xED: *base = 'I'; return 1;
    case 0xD3: case 0xF3: *base = 'O'; return 1;
    case 0xDA: case 0xFA: *base = 'U'; return 1;
    case 0xDC: case 0xFC: *base = 'U'; return 2;
    case 0xD1: case 0xF1: *base = 'N'; return 3;
    default: return 0;
    }
}
static const uint32_t glifo_abre_pregunta = 0x404896;
static const uint32_t glifo_abre_exclamacion = 0x404444;

void sw_text(SwSurface *s, const uint16_t *text, int n, int x, int y, uint32_t argb, int scale) {
    uint32_t c = argb_to_abgr(argb);
    if (scale < 1) scale = 1;
    for (int i = 0; i < n; i++) {
        int ch = text[i];
        if (ch >= 'a' && ch <= 'z') ch -= 32;
        int base = ch, marca = ch > 127 ? sw_marca(ch, &base) : 0;
        if (marca) {
            /* the mark, one row above the letter */
            static const uint8_t fila[4] = { 0, 0x2, 0x9, 0x6 };
            for (int col = 0; col < 4; col++) {
                if (fila[marca] & (8 >> col)) {
                    for (int a = 0; a < scale; a++) {
                        for (int b = 0; b < scale; b++) plot(s, x + col * scale + b, y + a, c);
                    }
                }
            }
            ch = base;
        }
        const char *f = ch < 128 ? strchr(font_chars, ch) : NULL;
        uint32_t extra = ch == 0xBF ? glifo_abre_pregunta : (ch == 0xA1 ? glifo_abre_exclamacion : 0);
        if ((f && ch) || extra) {
            uint32_t bits = extra ? extra : font_bits[f - font_chars];
            for (int r = 0; r < 6; r++) {
                for (int col = 0; col < 4; col++) {
                    if (bits & (1u << (23 - (r * 4 + col)))) {
                        for (int a = 0; a < scale; a++) {
                            for (int b = 0; b < scale; b++) plot(s, x + col * scale + b, y + 1 + r * scale + a, c);
                        }
                    }
                }
            }
        }
        x += 6 * scale;
    }
}

/* ---- 3D pipeline ------------------------------------------------------------------ */

static float proj[16];
static int vp_x, vp_y, vp_w, vp_h;
static int n_lights;
static float lights[8 * 16];

void sw3_begin(SwSurface *s, int vx, int vy, int vw, int vh) {
    (void)s;
    vp_x = vx;
    vp_y = vy;
    vp_w = vw;
    vp_h = vh;
}

void sw3_clear(SwSurface *s, uint32_t argb, int color, int depth) {
    for (int y = s->cy; y < s->cy + s->ch; y++) {
        if (color) {
            uint32_t c = argb_to_abgr(argb);
            uint32_t *row = s->pix + (size_t)y * s->stride;
            for (int x = s->cx; x < s->cx + s->cw; x++) row[x] = c;
        }
        if (depth && s->depth) {
            float *d = s->depth + (size_t)y * s->w;
            for (int x = s->cx; x < s->cx + s->cw; x++) d[x] = 1.0f;
        }
    }
}

void sw3_projection(const float *m) {
    memcpy(proj, m, sizeof proj);
    static int dbg = -1;
    if (dbg < 0) dbg = getenv("J2C_PROJ") != NULL;
    if (dbg) {
        float a = m[10], b = m[11];
        printf("proj near=%g far=%g\n", b / (a - 1), b / (a + 1));
    }
}

void sw3_lights(int n, const float *data) {
    n_lights = n > 8 ? 8 : n;
    memcpy(lights, data, (size_t)n_lights * 16 * sizeof(float));
}

static inline float clampf(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

uint32_t m3g_light_vertex(const float *n, const float *p, uint32_t vcolor, const int32_t *st, int nl,
                          const float *ld) {
    uint32_t amb = (uint32_t)st[ST_AMBIENT], dif = (uint32_t)st[ST_DIFFUSE];
    uint32_t emi = (uint32_t)st[ST_EMISSIVE], spe = (uint32_t)st[ST_SPECULAR];
    float shin;
    memcpy(&shin, &st[ST_SHININESS], 4);
    float ar, ag, ab, dr, dg, db, da;
    if (st[ST_FLAGS] & SF_TRACKING) {
        /* vertex color (ABGR) replaces ambient and diffuse */
        ar = dr = (vcolor & 0xff) / 255.0f;
        ag = dg = ((vcolor >> 8) & 0xff) / 255.0f;
        ab = db = ((vcolor >> 16) & 0xff) / 255.0f;
        da = (vcolor >> 24) / 255.0f;
    } else {
        ar = ((amb >> 16) & 0xff) / 255.0f;
        ag = ((amb >> 8) & 0xff) / 255.0f;
        ab = (amb & 0xff) / 255.0f;
        dr = ((dif >> 16) & 0xff) / 255.0f;
        dg = ((dif >> 8) & 0xff) / 255.0f;
        db = (dif & 0xff) / 255.0f;
        da = (dif >> 24) / 255.0f;
    }
    float r = ((emi >> 16) & 0xff) / 255.0f, g = ((emi >> 8) & 0xff) / 255.0f, b = (emi & 0xff) / 255.0f;
    float sr = ((spe >> 16) & 0xff) / 255.0f, sg = ((spe >> 8) & 0xff) / 255.0f, sb = (spe & 0xff) / 255.0f;
    for (int i = 0; i < nl; i++) {
        const float *L = ld + i * 16;
        int mode = (int)L[0];
        float lr = L[1], lg = L[2], lb = L[3];
        if (mode == M3G_LIGHT_AMBIENT) {
            r += lr * ar;
            g += lg * ag;
            b += lb * ab;
            continue;
        }
        float lx, ly, lz, att = 1.0f;
        if (mode == M3G_LIGHT_DIRECTIONAL) {
            lx = -L[7];
            ly = -L[8];
            lz = -L[9];
        } else {
            lx = L[4] - p[0];
            ly = L[5] - p[1];
            lz = L[6] - p[2];
            float d = sqrtf(lx * lx + ly * ly + lz * lz);
            if (d > 0) {
                lx /= d;
                ly /= d;
                lz /= d;
            }
            float den = L[10] + L[11] * d + L[12] * d * d;
            att = den > 0 ? 1.0f / den : 1.0f;
            if (mode == M3G_LIGHT_SPOT) {
                float cosang = -(lx * L[7] + ly * L[8] + lz * L[9]);
                float cutoff = cosf(L[13] * 3.14159265f / 180.0f);
                if (cosang < cutoff) att = 0;
                else att *= powf(cosang, L[14]);
            }
        }
        float ndl = n[0] * lx + n[1] * ly + n[2] * lz;
        if (ndl > 0) {
            r += att * lr * dr * ndl;
            g += att * lg * dg * ndl;
            b += att * lb * db * ndl;
            if (shin > 0 && (spe & 0xffffff)) {
                float hx = lx, hy = ly, hz = lz + 1.0f;
                float hl = sqrtf(hx * hx + hy * hy + hz * hz);
                float ndh = hl > 0 ? (n[0] * hx + n[1] * hy + n[2] * hz) / hl : 0;
                if (ndh > 0) {
                    float sp = powf(ndh, shin) * att;
                    r += sp * lr * sr;
                    g += sp * lg * sg;
                    b += sp * lb * sb;
                }
            }
        }
    }
    uint32_t R = (uint32_t)(clampf(r, 0, 1) * 255 + 0.5f);
    uint32_t G = (uint32_t)(clampf(g, 0, 1) * 255 + 0.5f);
    uint32_t B = (uint32_t)(clampf(b, 0, 1) * 255 + 0.5f);
    uint32_t A = (uint32_t)(clampf(da, 0, 1) * 255 + 0.5f);
    return (A << 24) | (B << 16) | (G << 8) | R;
}

typedef struct CV {
    float x, y, z, w; /* clip space */
    float ez;         /* eye-space depth (positive distance) for fog */
    float u, v;
    float r, g, b, a;
} CV;

static inline uint32_t sample(const uint32_t *tex, int tw, int th, float u, float v, int wraps, int wrapt) {
    float fu = u * tw, fv = v * th;
    int iu = (int)floorf(fu), iv = (int)floorf(fv);
    if (wraps == M3G_WRAP_CLAMP) iu = iu < 0 ? 0 : (iu >= tw ? tw - 1 : iu);
    else iu &= tw - 1;
    if (wrapt == M3G_WRAP_CLAMP) iv = iv < 0 ? 0 : (iv >= th ? th - 1 : iv);
    else iv &= th - 1;
    return tex[iv * tw + iu];
}

static void raster_tri(SwSurface *s, const CV *a, const CV *b, const CV *c, const int32_t *st, const uint32_t *tex,
                       int tw, int th) {
    /* to screen */
    float ax = vp_x + (a->x / a->w * 0.5f + 0.5f) * vp_w, ay = vp_y + (0.5f - a->y / a->w * 0.5f) * vp_h;
    float bx = vp_x + (b->x / b->w * 0.5f + 0.5f) * vp_w, by = vp_y + (0.5f - b->y / b->w * 0.5f) * vp_h;
    float cx = vp_x + (c->x / c->w * 0.5f + 0.5f) * vp_w, cy = vp_y + (0.5f - c->y / c->w * 0.5f) * vp_h;
    float area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
    if (area == 0) return;
    /* screen y points down, so counter-clockwise in eye space gives negative area */
    int front = area < 0;
    if (st[ST_WINDING] == M3G_WINDING_CW) front = !front;
    int cull = st[ST_CULL];
    if ((cull == M3G_CULL_BACK && !front) || (cull == M3G_CULL_FRONT && front)) return;
    float iwa = 1.0f / a->w, iwb = 1.0f / b->w, iwc = 1.0f / c->w;
    float za = a->z * iwa * 0.5f + 0.5f, zb = b->z * iwb * 0.5f + 0.5f, zc = c->z * iwc * 0.5f + 0.5f;
    int minx = (int)floorf(fminf(ax, fminf(bx, cx))), maxx = (int)ceilf(fmaxf(ax, fmaxf(bx, cx)));
    int miny = (int)floorf(fminf(ay, fminf(by, cy))), maxy = (int)ceilf(fmaxf(ay, fmaxf(by, cy)));
    int clx0 = s->cx > vp_x ? s->cx : vp_x, cly0 = s->cy > vp_y ? s->cy : vp_y;
    int clx1 = s->cx + s->cw < vp_x + vp_w ? s->cx + s->cw : vp_x + vp_w;
    int cly1 = s->cy + s->ch < vp_y + vp_h ? s->cy + s->ch : vp_y + vp_h;
    if (minx < clx0) minx = clx0;
    if (miny < cly0) miny = cly0;
    if (maxx > clx1 - 1) maxx = clx1 - 1;
    if (maxy > cly1 - 1) maxy = cly1 - 1;
    if (minx > maxx || miny > maxy) return;
    int flags = st[ST_FLAGS];
    int blend = st[ST_BLEND];
    int athr = st[ST_ALPHA_THRESHOLD];
    int texfn = st[ST_TEXFUNC];
    uint32_t tbc = (uint32_t)st[ST_TEXBLENDCOLOR];
    int use_tex = (flags & SF_TEXTURE) && tex;
    int tex_alpha = st[ST_TEXALPHA];
    int fogmode = st[ST_FOGMODE];
    float fogd, fogn, fogf;
    memcpy(&fogd, &st[ST_FOGDENSITY], 4);
    memcpy(&fogn, &st[ST_FOGNEAR], 4);
    memcpy(&fogf, &st[ST_FOGFAR], 4);
    uint32_t fogc = (uint32_t)st[ST_FOGCOLOR];
    float fr = ((fogc >> 16) & 0xff) / 255.0f, fg = ((fogc >> 8) & 0xff) / 255.0f, fb = (fogc & 0xff) / 255.0f;
    float inv_area = 1.0f / area;
    for (int y = miny; y <= maxy; y++) {
        float py = y + 0.5f;
        uint32_t *row = s->pix + (size_t)y * s->stride;
        float *drow = s->depth ? s->depth + (size_t)y * s->w : NULL;
        for (int x = minx; x <= maxx; x++) {
            float px = x + 0.5f;
            float w0 = ((bx - px) * (cy - py) - (cx - px) * (by - py)) * inv_area;
            float w1 = ((cx - px) * (ay - py) - (ax - px) * (cy - py)) * inv_area;
            float w2 = 1.0f - w0 - w1;
            if (w0 < 0 || w1 < 0 || w2 < 0) continue;
            float z = w0 * za + w1 * zb + w2 * zc;
            if (drow && (flags & SF_DEPTH_TEST) && z > drow[x]) continue;
            float pw0 = w0 * iwa, pw1 = w1 * iwb, pw2 = w2 * iwc;
            float ps = 1.0f / (pw0 + pw1 + pw2);
            pw0 *= ps;
            pw1 *= ps;
            pw2 *= ps;
            float r = pw0 * a->r + pw1 * b->r + pw2 * c->r;
            float g = pw0 * a->g + pw1 * b->g + pw2 * c->g;
            float bl = pw0 * a->b + pw1 * b->b + pw2 * c->b;
            float al = pw0 * a->a + pw1 * b->a + pw2 * c->a;
            if (use_tex) {
                float u = pw0 * a->u + pw1 * b->u + pw2 * c->u;
                float v = pw0 * a->v + pw1 * b->v + pw2 * c->v;
                uint32_t t = sample(tex, tw, th, u, v, st[ST_WRAPS], st[ST_WRAPT]);
                float tr = (t & 0xff) / 255.0f, tg = ((t >> 8) & 0xff) / 255.0f, tb = ((t >> 16) & 0xff) / 255.0f;
                float ta = (t >> 24) / 255.0f;
                switch (texfn) {
                case M3G_FUNC_REPLACE:
                    r = tr;
                    g = tg;
                    bl = tb;
                    if (tex_alpha) al = ta;
                    break;
                case M3G_FUNC_DECAL:
                    r = r * (1 - ta) + tr * ta;
                    g = g * (1 - ta) + tg * ta;
                    bl = bl * (1 - ta) + tb * ta;
                    break;
                case M3G_FUNC_BLEND: {
                    float br = ((tbc >> 16) & 0xff) / 255.0f, bg = ((tbc >> 8) & 0xff) / 255.0f,
                          bb = (tbc & 0xff) / 255.0f;
                    r = r * (1 - tr) + br * tr;
                    g = g * (1 - tg) + bg * tg;
                    bl = bl * (1 - tb) + bb * tb;
                    al *= ta;
                    break;
                }
                case M3G_FUNC_ADD:
                    r = fminf(1, r + tr);
                    g = fminf(1, g + tg);
                    bl = fminf(1, bl + tb);
                    al *= ta;
                    break;
                default: /* MODULATE */
                    r *= tr;
                    g *= tg;
                    bl *= tb;
                    al *= ta;
                    break;
                }
            }
            if (fogmode) {
                float ez = pw0 * a->ez + pw1 * b->ez + pw2 * c->ez;
                float f;
                if (fogmode == M3G_FOG_LINEAR) f = fogf != fogn ? (fogf - ez) / (fogf - fogn) : 1;
                else f = expf(-fogd * ez);
                f = clampf(f, 0, 1);
                r = r * f + fr * (1 - f);
                g = g * f + fg * (1 - f);
                bl = bl * f + fb * (1 - f);
            }
            if (athr > 0 && al * 255.0f < (float)athr) continue;
            if (drow && (flags & SF_DEPTH_WRITE)) drow[x] = z;
            if (!(flags & SF_COLOR_WRITE)) continue;
            uint32_t d = row[x];
            float dr = (d & 0xff) / 255.0f, dg = ((d >> 8) & 0xff) / 255.0f, db = ((d >> 16) & 0xff) / 255.0f;
            switch (blend) {
            case M3G_ALPHA:
                r = r * al + dr * (1 - al);
                g = g * al + dg * (1 - al);
                bl = bl * al + db * (1 - al);
                break;
            case M3G_ALPHA_ADD:
                r = dr + r * al;
                g = dg + g * al;
                bl = db + bl * al;
                break;
            case M3G_MODULATE:
                r *= dr;
                g *= dg;
                bl *= db;
                break;
            case M3G_MODULATE_X2:
                r *= 2 * dr;
                g *= 2 * dg;
                bl *= 2 * db;
                break;
            default:
                break;
            }
            uint32_t R = (uint32_t)(clampf(r, 0, 1) * 255 + 0.5f), G = (uint32_t)(clampf(g, 0, 1) * 255 + 0.5f);
            uint32_t B = (uint32_t)(clampf(bl, 0, 1) * 255 + 0.5f);
            row[x] = (d & 0xff000000u) | (B << 16) | (G << 8) | R;
        }
    }
}

/* clip a triangle against the near plane (z > -w) and rasterize the pieces */
static void clip_tri(SwSurface *s, const CV *v0, const CV *v1, const CV *v2, const int32_t *st,
                     const uint32_t *tex, int tw, int th) {
    const CV *in[3] = { v0, v1, v2 };
    CV out[4];
    int n = 0;
    for (int i = 0; i < 3; i++) {
        const CV *a = in[i], *b = in[(i + 1) % 3];
        float da = a->z + a->w, db = b->z + b->w;
        if (da >= 0) out[n++] = *a;
        if ((da >= 0) != (db >= 0)) {
            float t = da / (da - db);
            CV m;
            float *pm = &m.x;
            const float *pa = &a->x, *pb = &b->x;
            for (int k = 0; k < (int)(sizeof(CV) / sizeof(float)); k++) pm[k] = pa[k] + (pb[k] - pa[k]) * t;
            out[n++] = m;
        }
    }
    for (int i = 0; i < n; i++) {
        if (out[i].w < 1e-5f) out[i].w = 1e-5f;
    }
    if (n >= 3) raster_tri(s, &out[0], &out[1], &out[2], st, tex, tw, th);
    if (n == 4) raster_tri(s, &out[0], &out[2], &out[3], st, tex, tw, th);
}

void sw3_draw(SwSurface *s, const int32_t *verts, int vcount, const uint16_t *idx, int icount, const float *mv,
              const int32_t *st, const uint32_t *tex, int tw, int th) {
    CV *cv = malloc(sizeof(CV) * (size_t)vcount);
    if (!cv) return;
    float m[16];
    /* combined = proj * mv */
    for (int r = 0; r < 4; r++) {
        for (int c = 0; c < 4; c++) {
            m[r * 4 + c] = proj[r * 4] * mv[c] + proj[r * 4 + 1] * mv[4 + c] + proj[r * 4 + 2] * mv[8 + c] +
                           proj[r * 4 + 3] * mv[12 + c];
        }
    }
    int flags = st[ST_FLAGS];
    float alpha_factor = st[ST_ALPHAFACTOR] / 255.0f;
    for (int i = 0; i < vcount; i++) {
        const int32_t *v = verts + i * VSTRIDE;
        float f[9];
        memcpy(f, v, sizeof f);
        float x = f[6], y = f[7], z = f[8];
        CV *o = &cv[i];
        o->x = m[0] * x + m[1] * y + m[2] * z + m[3];
        o->y = m[4] * x + m[5] * y + m[6] * z + m[7];
        o->z = m[8] * x + m[9] * y + m[10] * z + m[11];
        o->w = m[12] * x + m[13] * y + m[14] * z + m[15];
        float ex = mv[0] * x + mv[1] * y + mv[2] * z + mv[3];
        float ey = mv[4] * x + mv[5] * y + mv[6] * z + mv[7];
        float ez = mv[8] * x + mv[9] * y + mv[10] * z + mv[11];
        o->ez = -ez;
        o->u = f[0];
        o->v = f[1];
        uint32_t col = (uint32_t)v[2];
        if (flags & SF_LIGHTING) {
            float n[3] = { f[3], f[4], f[5] };
            float ne[3] = { mv[0] * n[0] + mv[1] * n[1] + mv[2] * n[2], mv[4] * n[0] + mv[5] * n[1] + mv[6] * n[2],
                            mv[8] * n[0] + mv[9] * n[1] + mv[10] * n[2] };
            float l = sqrtf(ne[0] * ne[0] + ne[1] * ne[1] + ne[2] * ne[2]);
            if (l > 0) {
                ne[0] /= l;
                ne[1] /= l;
                ne[2] /= l;
            }
            float pe[3] = { ex, ey, ez };
            col = m3g_light_vertex(ne, pe, col, st, n_lights, lights);
        }
        o->r = (col & 0xff) / 255.0f;
        o->g = ((col >> 8) & 0xff) / 255.0f;
        o->b = ((col >> 16) & 0xff) / 255.0f;
        o->a = (col >> 24) / 255.0f * alpha_factor;
    }
    for (int i = 0; i + 2 < icount; i += 3) {
        int a = idx[i], b = idx[i + 1], c = idx[i + 2];
        if (a >= vcount || b >= vcount || c >= vcount) continue;
        const CV *A = &cv[a], *B = &cv[b], *C = &cv[c];
        if (A->z < -A->w && B->z < -B->w && C->z < -C->w) continue;
        if (A->x > A->w && B->x > B->w && C->x > C->w) continue;
        if (A->x < -A->w && B->x < -B->w && C->x < -C->w) continue;
        if (A->y > A->w && B->y > B->w && C->y > C->w) continue;
        if (A->y < -A->w && B->y < -B->w && C->y < -C->w) continue;
        clip_tri(s, A, B, C, st, tex, tw, th);
    }
    free(cv);
}
