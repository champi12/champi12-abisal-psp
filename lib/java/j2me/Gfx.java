package j2me;

/** Native 2D rendering backend (PSP GE or the PC software renderer). */
public final class Gfx {
    private Gfx() {
    }

    public static native int screenWidth();

    public static native int screenHeight();

    /** Select the render target (pixels == null for the screen) and clip rectangle. */
    public static native void bind(int[] pixels, int stride, int w, int h, int cx, int cy, int cw, int ch);

    public static native void fillRect(int x, int y, int w, int h, int argb);

    public static native void drawLine(int x1, int y1, int x2, int y2, int argb);

    public static native void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argb);

    public static native void arc(int x, int y, int w, int h, int start, int arc, int argb, boolean fill);

    public static native void drawImage(int[] pix, int stride, int iw, int ih, int sx, int sy, int sw, int sh,
            int transform, int dx, int dy, boolean alpha);

    public static native void drawRGB(int[] rgb, int off, int scan, int x, int y, int w, int h, boolean alpha);

    public static native void drawText(char[] text, int x, int y, int argb, int size);

    /** same as drawText with only the first n characters (lets callers reuse one buffer) */
    public static native void drawTextN(char[] text, int n, int x, int y, int argb, int size);

    /** Decode PNG/JPEG data; fills wh with {width, height, hasAlpha}. */
    public static native int[] decodeImage(byte[] data, int off, int len, int[] wh);

    /** Tell the backend that a pixel array was modified by Java code. */
    public static native void pixelsChanged(int[] pix);

    /** Show the back buffer (keeps its contents) and wait for vertical sync. */
    public static native void present();

    /** Next input/system event or 0: (type << 24) | (keyCode & 0xffff). */
    public static native int pollEvent();
}
