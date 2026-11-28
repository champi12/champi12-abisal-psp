package javax.microedition.m3g;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.LcduiAccess;

/** Pixels are stored expanded to 32-bit ABGR (GE native order), one row per `width`. */
public class Image2D extends Object3D {
    public static final int ALPHA = 96;
    public static final int LUMINANCE = 97;
    public static final int LUMINANCE_ALPHA = 98;
    public static final int RGB = 99;
    public static final int RGBA = 100;

    final int format;
    final int width;
    final int height;
    final boolean mutable;
    int[] pixels;

    public Image2D(int format, int w, int h) {
        this(format, w, h, true);
    }

    private Image2D(int format, int w, int h, boolean mutable) {
        check(format, w, h);
        this.format = format;
        width = w;
        height = h;
        this.mutable = mutable;
        // big textures (the creature atlas, 512 KB) live in system memory outside the Java
        // heap, like the vertex caches: on the PSP the heap could not spare them at load time
        int[] nat = null;
        if (!mutable && w * h >= 65536) {
            try {
                nat = j2me.Gfx.arrayNativo(PLANTILLA, w * h);
            } catch (Throwable e) {
                nat = null;
            }
        }
        pixels = nat != null ? nat : new int[w * h];
    }

    private static final int[] PLANTILLA = new int[0];

    /**
     * Not in the M3G API: an RGB image read row by row from a raw stream (w * h * 3 bytes),
     * straight into its pixel array (outside the Java heap when big): no full-size byte copy
     * and no PNG decoding, so a 512 x 512 atlas loads on a PSP whose heap is nearly full.
     */
    public static Image2D desdeRGB(String base, int tiras, int w, int h) throws java.io.IOException {
        // the resource reader loads a whole file into the heap: the image comes in strips
        // (base_0.rgb ... base_N.rgb, h / tiras rows each) so no single big buffer is needed
        Image2D img = new Image2D(RGB, w, h, false);
        int filas = h / tiras;
        for (int t = 0; t < tiras; t++) {
            java.io.InputStream in = Image2D.class.getResourceAsStream(base + "_" + t + ".rgb");
            if (in == null) {
                throw new java.io.IOException(base + "_" + t + ".rgb");
            }
            byte[] fila = new byte[w * 3];
            for (int y = t * filas; y < (t + 1) * filas; y++) {
                int n = 0;
                while (n < fila.length) {
                    int r = in.read(fila, n, fila.length - n);
                    if (r <= 0) {
                        throw new java.io.EOFException();
                    }
                    n += r;
                }
                for (int x = 0; x < w; x++) {
                    img.pixels[y * w + x] = expand(RGB, fila, x * 3);
                }
            }
            in.close();
        }
        j2me.Gfx.pixelsChanged(img.pixels);
        return img;
    }

    public Image2D(int format, int w, int h, byte[] image) {
        this(format, w, h, false);
        int bpp = bpp(format);
        for (int i = 0; i < w * h; i++) {
            pixels[i] = expand(format, image, i * bpp);
        }
        j2me.Gfx.pixelsChanged(pixels);
    }

    public Image2D(int format, int w, int h, byte[] image, byte[] palette) {
        this(format, w, h, false);
        int bpp = bpp(format);
        for (int i = 0; i < w * h; i++) {
            int idx = image[i] & 0xff;
            pixels[i] = expand(format, palette, idx * bpp);
        }
        j2me.Gfx.pixelsChanged(pixels);
    }

    public Image2D(int format, Object image) {
        this(format, ((Image) image).getWidth(), ((Image) image).getHeight(), false);
        Image img = (Image) image;
        int[] src = LcduiAccess.pixels(img);
        int stride = LcduiAccess.stride(img);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = src[y * stride + x];
                int r = c & 0xff;
                int g = (c >> 8) & 0xff;
                int b = (c >> 16) & 0xff;
                int a = (c >>> 24);
                pixels[y * width + x] = convert(format, r, g, b, a);
            }
        }
        j2me.Gfx.pixelsChanged(pixels);
    }

    private static void check(int format, int w, int h) {
        if (format < ALPHA || format > RGBA || w <= 0 || h <= 0) {
            throw new IllegalArgumentException();
        }
    }

    static int bpp(int format) {
        switch (format) {
            case ALPHA:
            case LUMINANCE:
                return 1;
            case LUMINANCE_ALPHA:
                return 2;
            case RGB:
                return 3;
            default:
                return 4;
        }
    }

    /** Store an RGBA tuple as native ABGR according to the image format. */
    static int convert(int format, int r, int g, int b, int a) {
        switch (format) {
            case ALPHA:
                return (a << 24) | 0xffffff;
            case LUMINANCE: {
                int l = (r * 77 + g * 151 + b * 28) >> 8;
                return 0xff000000 | (l << 16) | (l << 8) | l;
            }
            case LUMINANCE_ALPHA: {
                int l = (r * 77 + g * 151 + b * 28) >> 8;
                return (a << 24) | (l << 16) | (l << 8) | l;
            }
            case RGB:
                return 0xff000000 | (b << 16) | (g << 8) | r;
            default:
                return (a << 24) | (b << 16) | (g << 8) | r;
        }
    }

    static int expand(int format, byte[] d, int o) {
        switch (format) {
            case ALPHA:
                return ((d[o] & 0xff) << 24) | 0xffffff;
            case LUMINANCE: {
                int l = d[o] & 0xff;
                return 0xff000000 | (l << 16) | (l << 8) | l;
            }
            case LUMINANCE_ALPHA: {
                int l = d[o] & 0xff;
                return ((d[o + 1] & 0xff) << 24) | (l << 16) | (l << 8) | l;
            }
            case RGB:
                return 0xff000000 | ((d[o + 2] & 0xff) << 16) | ((d[o + 1] & 0xff) << 8) | (d[o] & 0xff);
            default:
                return ((d[o + 3] & 0xff) << 24) | ((d[o + 2] & 0xff) << 16) | ((d[o + 1] & 0xff) << 8)
                        | (d[o] & 0xff);
        }
    }

    public void set(int x, int y, int w, int h, byte[] image) {
        if (!mutable) {
            throw new IllegalStateException();
        }
        int bpp = bpp(format);
        int[] np = new int[pixels.length];
        System.arraycopy(pixels, 0, np, 0, pixels.length);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                np[(y + j) * width + x + i] = expand(format, image, (j * w + i) * bpp);
            }
        }
        pixels = np;
        j2me.Gfx.pixelsChanged(pixels);
    }

    public boolean isMutable() {
        return mutable;
    }

    public int getFormat() {
        return format;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    boolean hasAlpha() {
        return format == ALPHA || format == LUMINANCE_ALPHA || format == RGBA;
    }

    Object3D duplicateImpl() {
        Image2D i = new Image2D(format, width, height, mutable);
        i.copyBase(this);
        System.arraycopy(pixels, 0, i.pixels, 0, pixels.length);
        return i;
    }
}
