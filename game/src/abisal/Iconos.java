package abisal;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * The PSP button icons for the control hints, drawn once into small anti-aliased images:
 * each pixel is the average of 4 x 4 samples of the shapes (discs, rings, strokes), so the
 * buttons come out round and smooth instead of a few stair-stepped arcs.
 */
final class Iconos {
    static final int X = 0, CIRCULO = 1, CUADRADO = 2, TRIANGULO = 3, L = 4, R = 5, CRUCETA = 6, START = 7;
    private static final Image[] img = new Image[8];
    private static final int[] ANCHO = { 14, 14, 14, 14, 18, 18, 26, 30 };
    static final int ALTO = 14;

    private Iconos() {
    }

    /** draws icon b with its top-left at (x, y); returns its width */
    static int dibujar(Graphics g, int x, int y, int b) {
        if (img[b] == null) {
            img[b] = crear(b);
        }
        g.drawImage(img[b], x, y, Graphics.TOP | Graphics.LEFT);
        if (b == L || b == R || b == START) {
            g.setColor(0xffffff);
            g.drawString(b == L ? "L" : (b == R ? "R" : "START"), x + ANCHO[b] / 2 + 1, y + 3,
                    Graphics.TOP | Graphics.HCENTER);
        }
        return ANCHO[b];
    }

    private static Image crear(int b) {
        int w = ANCHO[b], h = ALTO;
        int[] px = new int[w * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                float a = 0, r = 0, gg = 0, bb = 0;
                for (int sj = 0; sj < 4; sj++) {
                    for (int si = 0; si < 4; si++) {
                        float x = i + (si + 0.5f) / 4, y = j + (sj + 0.5f) / 4;
                        int c = muestra(b, x, y, w, h);
                        int ca = c >>> 24;
                        if (ca == 0) {
                            continue;
                        }
                        a += ca;
                        r += ((c >> 16) & 255) * ca;
                        gg += ((c >> 8) & 255) * ca;
                        bb += (c & 255) * ca;
                    }
                }
                if (a > 0) {
                    int ia = (int) (a / 16);
                    px[j * w + i] = (ia << 24) | ((int) (r / a) << 16) | ((int) (gg / a) << 8) | (int) (bb / a);
                }
            }
        }
        return Image.createRGBImage(px, w, h, true);
    }

    /** the colour of icon b at point (x, y) (alpha 0 = empty) */
    private static int muestra(int b, float x, float y, int w, int h) {
        float cx = w / 2f, cy = h / 2f;
        if (b == L || b == R || b == START) {
            // a grey pill with a lighter rim (the letter is drawn on top as text)
            float d = pastilla(x, y, cx, cy, w / 2f - 0.6f, h / 2f - 1.2f);
            if (d > 0) {
                return 0;
            }
            return d > -1.1f ? 0xff8e9ca6 : 0xff46525c;
        }
        if (b == CRUCETA) {
            // a d-pad bar with a white arrow at each end
            if (flecha(x, y, 1.6f, cy, 8.4f, 1) || flecha(x, y, w - 1.6f, cy, w - 8.4f, -1)) {
                return 0xffeef6fa;
            }
            float d = pastilla(x, y, cx, cy, w / 2f - 3.5f, 2.6f);
            return d <= 0 ? 0xff46525c : 0;
        }
        // face buttons: a dark disc with a lighter rim and the coloured symbol
        float dx = x - cx, dy = y - cy;
        float rr = (float) Math.sqrt(dx * dx + dy * dy);
        if (rr > 6.6f) {
            return 0;
        }
        float t = 1.25f; // stroke half-width of the symbol
        switch (b) {
        case X:
            if (segmento(x, y, cx - 2.6f, cy - 2.6f, cx + 2.6f, cy + 2.6f) < t
                    || segmento(x, y, cx + 2.6f, cy - 2.6f, cx - 2.6f, cy + 2.6f) < t) {
                return 0xff8ab4ff;
            }
            break;
        case CIRCULO:
            if (Math.abs(rr - 3.2f) < 0.8f) {
                return 0xffff7a7a;
            }
            break;
        case CUADRADO: {
            float m = Math.max(Math.abs(dx), Math.abs(dy));
            if (Math.abs(m - 2.9f) < 0.75f) {
                return 0xffff9ae0;
            }
            break;
        }
        default: {
            float ax = cx, ay = cy - 3.2f, bx = cx - 3.1f, by = cy + 2.3f, qx = cx + 3.1f, qy = cy + 2.3f;
            if (segmento(x, y, ax, ay, bx, by) < t * 0.85f || segmento(x, y, bx, by, qx, qy) < t * 0.85f
                    || segmento(x, y, qx, qy, ax, ay) < t * 0.85f) {
                return 0xff7aff9a;
            }
            break;
        }
        }
        return rr > 5.7f ? 0xff5c6872 : 0xff222c36;
    }

    /** signed distance to a horizontal pill (rounded box) of half-sizes hw x hh */
    private static float pastilla(float x, float y, float cx, float cy, float hw, float hh) {
        float r = hh;
        float qx = Math.max(Math.abs(x - cx) - (hw - r), 0);
        float qy = Math.abs(y - cy);
        return (float) Math.sqrt(qx * qx + qy * qy) - r;
    }

    /** distance from (x, y) to the segment a-b */
    private static float segmento(float x, float y, float ax, float ay, float bx, float by) {
        float vx = bx - ax, vy = by - ay;
        float t = ((x - ax) * vx + (y - ay) * vy) / (vx * vx + vy * vy);
        t = Math.max(0, Math.min(1, t));
        float ex = ax + vx * t - x, ey = ay + vy * t - y;
        return (float) Math.sqrt(ex * ex + ey * ey);
    }

    /** a filled arrow pointing from its base (at xb) towards its tip (at xt) */
    private static boolean flecha(float x, float y, float xt, float cy, float xb, int sentido) {
        float largo = Math.abs(xb - xt);
        float k = sentido > 0 ? (x - xt) / largo : (xt - x) / largo; // 0 at the tip, 1 at the base
        if (k < 0 || k > 1) {
            return false;
        }
        return Math.abs(y - cy) <= 4.6f * k;
    }
}
