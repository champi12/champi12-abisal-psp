/* Natives of j2me.Gfx and j2me.G3D: route drawing to the screen renderer or to an image. */
#include <png.h>
#include <stdlib.h>
#include <zlib.h>
#include "j2c_gen.h"
#include "plat.h"
#include "render.h"

static SwSurface img_target;
static int target_is_image;

int32_t M_j2me_Gfx__screenWidth____I(void) { return SCREEN_W; }
int32_t M_j2me_Gfx__screenHeight____I(void) { return SCREEN_H; }

void M_j2me_Gfx__bind___3IIIIIIII__V(JObject *pix, int32_t stride, int32_t w, int32_t h, int32_t cx, int32_t cy,
                                      int32_t cw, int32_t ch) {
    if (pix) {
        if (!target_is_image) plat_gpu_sync(); /* the GPU may still be reading this image */
        target_is_image = 1;
        img_target.pix = JARR(pix, uint32_t);
        img_target.stride = stride;
        img_target.w = w;
        img_target.h = h;
        img_target.depth = NULL;
        sw_clip(&img_target, cx, cy, cw, ch);
    } else {
        target_is_image = 0;
        r_clip(cx, cy, cw, ch);
    }
}

void M_j2me_Gfx__fillRect__IIIII__V(int32_t x, int32_t y, int32_t w, int32_t h, int32_t c) {
    if (target_is_image) sw_fill_rect(&img_target, x, y, w, h, (uint32_t)c);
    else r_fill_rect(x, y, w, h, (uint32_t)c);
}

void M_j2me_Gfx__drawLine__IIIII__V(int32_t x1, int32_t y1, int32_t x2, int32_t y2, int32_t c) {
    if (target_is_image) sw_line(&img_target, x1, y1, x2, y2, (uint32_t)c);
    else r_line(x1, y1, x2, y2, (uint32_t)c);
}

void M_j2me_Gfx__fillTriangle__IIIIIII__V(int32_t x1, int32_t y1, int32_t x2, int32_t y2, int32_t x3, int32_t y3,
                                           int32_t c) {
    if (target_is_image) sw_triangle(&img_target, x1, y1, x2, y2, x3, y3, (uint32_t)c);
    else r_triangle(x1, y1, x2, y2, x3, y3, (uint32_t)c);
}

void M_j2me_Gfx__arc__IIIIIIIZ__V(int32_t x, int32_t y, int32_t w, int32_t h, int32_t start, int32_t arc, int32_t c,
                                   int32_t fill) {
    if (w <= 0 || h <= 0 || arc == 0) return;
    if (arc > 360) arc = 360;
    if (arc < -360) arc = -360;
    float cx = x + w / 2.0f, cy = y + h / 2.0f, rx = w / 2.0f, ry = h / 2.0f;
    int segs = (abs(arc) * (w + h)) / 360 / 4 + 8;
    if (segs > 96) segs = 96;
    float a0 = start * 3.14159265f / 180.0f, da = arc * 3.14159265f / 180.0f / segs;
    int px = (int)(cx + rx * cosf(a0)), py = (int)(cy - ry * sinf(a0));
    for (int i = 1; i <= segs; i++) {
        float a = a0 + da * i;
        int nx = (int)(cx + rx * cosf(a)), ny = (int)(cy - ry * sinf(a));
        if (fill) M_j2me_Gfx__fillTriangle__IIIIIII__V((int)cx, (int)cy, px, py, nx, ny, c);
        else M_j2me_Gfx__drawLine__IIIII__V(px, py, nx, ny, c);
        px = nx;
        py = ny;
    }
}

void M_j2me_Gfx__drawImage___3IIIIIIIIIIIZ__V(JObject *pix, int32_t stride, int32_t iw, int32_t ih, int32_t sx,
                                               int32_t sy, int32_t sw, int32_t sh, int32_t t, int32_t dx, int32_t dy,
                                               int32_t alpha) {
    if (!pix) return;
    if (target_is_image) sw_image(&img_target, JARR(pix, uint32_t), stride, sx, sy, sw, sh, t, dx, dy, alpha);
    else r_image(JARR(pix, uint32_t), stride, iw, ih, sx, sy, sw, sh, t, dx, dy, alpha);
}

void M_j2me_Gfx__drawRGB___3IIIIIIIZ__V(JObject *rgb, int32_t off, int32_t scan, int32_t x, int32_t y, int32_t w,
                                         int32_t h, int32_t alpha) {
    JArray *a = (JArray *)rgb;
    if (off < 0 || (int64_t)off + (int64_t)(h - 1) * scan + w > a->length) {
        jthrow_aioobe(off);
        return;
    }
    if (target_is_image) sw_rgb(&img_target, JARR(rgb, int32_t), off, scan, x, y, w, h, alpha);
    else r_rgb(JARR(rgb, int32_t), off, scan, x, y, w, h, alpha);
}

void M_j2me_Gfx__drawText___3CIIII__V(JObject *text, int32_t x, int32_t y, int32_t c, int32_t size) {
    int n = ((JArray *)text)->length;
    int scale = size == 16 ? 2 : 1;
    if (target_is_image) sw_text(&img_target, JARR(text, uint16_t), n, x, y, (uint32_t)c, scale);
    else r_text(JARR(text, uint16_t), n, x, y, (uint32_t)c, scale);
}

void M_j2me_Gfx__pixelsChanged___3I__V(JObject *pix) {
    if (pix) r_pixels_changed(JARR(pix, uint32_t), ((JArray *)pix)->length * 4);
}

void M_j2me_Gfx__present____V(void) {
    target_is_image = 0;
    r_present();
}

int32_t M_j2me_Gfx__pollEvent____I(void) { return plat_poll_event(); }
int32_t M_j2me_Pad__analog____I(void) { return plat_analog(); }

/* ---- PNG decoding ------------------------------------------------------------------ */

typedef struct {
    const uint8_t *p;
    size_t left;
} PngSrc;

static void png_read_cb(png_structp png, png_bytep out, png_size_t n) {
    PngSrc *s = png_get_io_ptr(png);
    if (n > s->left) png_error(png, "truncated");
    memcpy(out, s->p, n);
    s->p += n;
    s->left -= n;
}

JObject *M_j2me_Gfx__decodeImage___3BII_3I___3I(JObject *data, int32_t off, int32_t len, JObject *wh) {
    JArray *d = (JArray *)data;
    if (off < 0 || len < 8 || off > d->length - len) return NULL;
    const uint8_t *bytes = JARR(data, uint8_t) + off;
    if (png_sig_cmp((png_const_bytep)bytes, 0, 8)) return NULL;
    png_structp png = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    png_infop info = png_create_info_struct(png);
    uint8_t *rgba = NULL;
    png_bytep *rows = NULL;
    JObject *result = NULL;
    if (setjmp(png_jmpbuf(png))) {
        png_destroy_read_struct(&png, &info, NULL);
        free(rgba);
        free(rows);
        return NULL;
    }
    PngSrc src = { bytes, (size_t)len };
    png_set_read_fn(png, &src, png_read_cb);
    png_read_info(png, info);
    png_uint_32 w = png_get_image_width(png, info), h = png_get_image_height(png, info);
    int ct = png_get_color_type(png, info);
    int bd = png_get_bit_depth(png, info);
    int has_alpha = (ct & PNG_COLOR_MASK_ALPHA) || png_get_valid(png, info, PNG_INFO_tRNS);
    if (bd == 16) png_set_strip_16(png);
    if (ct == PNG_COLOR_TYPE_PALETTE) png_set_palette_to_rgb(png);
    if (ct == PNG_COLOR_TYPE_GRAY && bd < 8) png_set_expand_gray_1_2_4_to_8(png);
    if (png_get_valid(png, info, PNG_INFO_tRNS)) png_set_tRNS_to_alpha(png);
    if (ct == PNG_COLOR_TYPE_GRAY || ct == PNG_COLOR_TYPE_GRAY_ALPHA) png_set_gray_to_rgb(png);
    png_set_filler(png, 0xff, PNG_FILLER_AFTER);
    png_read_update_info(png, info);
    if (w == 0 || h == 0 || w > 4096 || h > 4096) png_error(png, "bad size");
    rgba = malloc((size_t)w * h * 4);
    rows = malloc(sizeof(png_bytep) * h);
    for (png_uint_32 y = 0; y < h; y++) rows[y] = rgba + (size_t)y * w * 4;
    png_read_image(png, rows);
    png_destroy_read_struct(&png, &info, NULL);
    int stride = ((int)w + 3) & ~3;
    result = jnew_array(&C_arr_I, stride * (int)h);
    if (result) {
        uint32_t *px = JARR(result, uint32_t);
        for (png_uint_32 y = 0; y < h; y++) {
            /* RGBA bytes in memory are exactly the ABGR little-endian word */
            memcpy(px + (size_t)y * stride, rgba + (size_t)y * w * 4, (size_t)w * 4);
        }
        r_pixels_changed(px, stride * (int)h * 4);
        JArray *o = (JArray *)wh;
        if (o && o->length >= 3) {
            JARR(wh, int32_t)[0] = (int32_t)w;
            JARR(wh, int32_t)[1] = (int32_t)h;
            JARR(wh, int32_t)[2] = has_alpha;
        }
    }
    free(rgba);
    free(rows);
    return result;
}

/* ---- G3D --------------------------------------------------------------------------- */

void M_j2me_G3D__begin__IIIIIIII__V(int32_t vx, int32_t vy, int32_t vw, int32_t vh, int32_t cx, int32_t cy,
                                     int32_t cw, int32_t ch) {
    target_is_image = 0;
    r3_begin(vx, vy, vw, vh, cx, cy, cw, ch);
}

void M_j2me_G3D__viewport__IIII__V(int32_t x, int32_t y, int32_t w, int32_t h) { r3_viewport(x, y, w, h); }

void M_j2me_G3D__end____V(void) { r3_end(); }

void M_j2me_G3D__clear__IZZ__V(int32_t argb, int32_t color, int32_t depth) {
    r3_clear((uint32_t)argb, color, depth);
}

void M_j2me_G3D__setProjection___3F__V(JObject *m) { r3_projection(JARR(m, float)); }

void M_j2me_G3D__setLights__I_3F__V(int32_t n, JObject *d) { r3_lights(n, JARR(d, float)); }

void M_j2me_G3D__draw___3II_3CI_3F_3I_3III__V(JObject *verts, int32_t vcount, JObject *idx, int32_t icount,
                                               JObject *mv, JObject *state, JObject *tex, int32_t tw, int32_t th) {
    if (!verts || !idx || !mv || !state) return;
    if (((JArray *)verts)->length < vcount * VSTRIDE || ((JArray *)idx)->length < icount) return;
    if (tex && ((JArray *)tex)->length < tw * th) tex = NULL;
    r3_draw(JARR(verts, int32_t), vcount, JARR(idx, uint16_t), icount, JARR(mv, float), JARR(state, int32_t),
            tex ? JARR(tex, uint32_t) : NULL, tw, th);
}

JObject *M_j2me_G3D__inflate___3BIII___3B(JObject *data, int32_t off, int32_t len, int32_t outLen) {
    JArray *d = (JArray *)data;
    if (off < 0 || len < 0 || off > d->length - len || outLen < 0) return NULL;
    JObject *out = jnew_array(&C_arr_B, outLen);
    if (!out) return NULL;
    uLongf dl = (uLongf)outLen;
    int rc = uncompress(JARR(out, Bytef), &dl, JARR(data, Bytef) + off, (uLong)len);
    if (rc != Z_OK) return NULL;
    return out;
}
