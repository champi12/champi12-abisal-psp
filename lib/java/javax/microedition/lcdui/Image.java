package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;

/**
 * Image backed by a pixel array in the GPU-native ABGR layout, one row every `stride`
 * pixels (stride is a multiple of 4 so the PSP GE can texture from it directly).
 */
public class Image {
    final int width;
    final int height;
    final int stride;
    final int[] pixels;
    final boolean mutable;
    boolean alpha;

    Image(int w, int h, boolean mutable) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException();
        }
        width = w;
        height = h;
        stride = (w + 3) & ~3;
        pixels = new int[stride * h];
        this.mutable = mutable;
    }

    private Image(int w, int h, int[] pix) {
        width = w;
        height = h;
        stride = (w + 3) & ~3;
        pixels = pix;
        mutable = false;
    }

    static int argbToNative(int c) {
        return (c & 0xff00ff00) | ((c >> 16) & 0xff) | ((c & 0xff) << 16);
    }

    static int nativeToArgb(int c) {
        return (c & 0xff00ff00) | ((c >> 16) & 0xff) | ((c & 0xff) << 16);
    }

    public static Image createImage(int w, int h) {
        Image img = new Image(w, h, true);
        int[] p = img.pixels;
        for (int i = 0; i < p.length; i++) {
            p[i] = 0xffffffff;
        }
        img.alpha = false;
        return img;
    }

    public static Image createImage(Image src) {
        if (!src.mutable) {
            return src;
        }
        return createImage(src, 0, 0, src.width, src.height, 0);
    }

    public static Image createImage(String name) throws IOException {
        InputStream in = Image.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException(name);
        }
        return createImage(in);
    }

    public static Image createImage(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b, 0, b.length)) > 0) {
            bo.write(b, 0, n);
        }
        byte[] d = bo.toByteArray();
        return createImage(d, 0, d.length);
    }

    public static Image createImage(byte[] data, int off, int len) {
        int[] wh = new int[3];
        int[] pix = j2me.Gfx.decodeImage(data, off, len, wh);
        if (pix == null) {
            throw new IllegalArgumentException("cannot decode image");
        }
        Image img = new Image(wh[0], wh[1], pix);
        img.alpha = wh[2] != 0;
        return img;
    }

    public static Image createImage(Image src, int x, int y, int w, int h, int transform) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > src.width || y + h > src.height) {
            throw new IllegalArgumentException();
        }
        boolean swap = (transform & 4) != 0;
        int dw = swap ? h : w;
        int dh = swap ? w : h;
        Image img = new Image(dw, dh, false);
        img.alpha = src.alpha;
        int[] sp = src.pixels;
        int[] dp = img.pixels;
        for (int dy = 0; dy < dh; dy++) {
            for (int dx = 0; dx < dw; dx++) {
                int u;
                int v;
                // map destination pixel to source region coordinates
                int a = swap ? dy : dx;
                int b = swap ? dx : dy;
                switch (transform) {
                    case 0: // NONE
                        u = a;
                        v = b;
                        break;
                    case 2: // MIRROR
                        u = w - 1 - a;
                        v = b;
                        break;
                    case 1: // MIRROR_ROT180
                        u = a;
                        v = h - 1 - b;
                        break;
                    case 3: // ROT180
                        u = w - 1 - a;
                        v = h - 1 - b;
                        break;
                    case 5: // ROT90
                        u = a;
                        v = h - 1 - b;
                        break;
                    case 6: // ROT270
                        u = w - 1 - a;
                        v = b;
                        break;
                    case 7: // MIRROR_ROT90
                        u = w - 1 - a;
                        v = h - 1 - b;
                        break;
                    default: // 4 MIRROR_ROT270
                        u = a;
                        v = b;
                        break;
                }
                dp[dy * img.stride + dx] = sp[(y + v) * src.stride + x + u];
            }
        }
        j2me.Gfx.pixelsChanged(dp);
        return img;
    }

    public static Image createRGBImage(int[] rgb, int w, int h, boolean processAlpha) {
        Image img = new Image(w, h, false);
        int[] dp = img.pixels;
        for (int y = 0; y < h; y++) {
            int s = y * w;
            int d = y * img.stride;
            for (int x = 0; x < w; x++) {
                int c = rgb[s + x];
                if (!processAlpha) {
                    c |= 0xff000000;
                }
                dp[d + x] = argbToNative(c);
            }
        }
        img.alpha = processAlpha;
        j2me.Gfx.pixelsChanged(dp);
        return img;
    }

    public Graphics getGraphics() {
        if (!mutable) {
            throw new IllegalStateException();
        }
        return new Graphics(this);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void getRGB(int[] out, int offset, int scan, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x + w > width || y + h > height) {
            throw new IllegalArgumentException();
        }
        for (int j = 0; j < h; j++) {
            int s = (y + j) * stride + x;
            int d = offset + j * scan;
            for (int i = 0; i < w; i++) {
                out[d + i] = nativeToArgb(pixels[s + i]);
            }
        }
    }
}
