package javax.microedition.lcdui;

/** Bridges package-private lcdui state to javax.microedition.lcdui.game. */
public final class LcduiAccess {
    private LcduiAccess() {
    }

    public static Graphics screenGraphics() {
        return new Graphics(null);
    }

    public static int takeKeyStates(Canvas c) {
        int s = c.keyStates | c.latchedKeys;
        c.latchedKeys = 0;
        return s;
    }

    /** Pixel array of an image (for M3G Image2D). */
    public static int[] pixels(Image img) {
        return img.pixels;
    }

    public static int stride(Image img) {
        return img.stride;
    }

    public static boolean hasAlpha(Image img) {
        return img.alpha;
    }

    /** Target of a Graphics: null for the screen, else a mutable Image. */
    public static Image target(Graphics g) {
        return g.target;
    }

    public static int clipX(Graphics g) {
        return g.clipX;
    }

    public static int clipY(Graphics g) {
        return g.clipY;
    }

    public static int clipW(Graphics g) {
        return g.clipW;
    }

    public static int clipH(Graphics g) {
        return g.clipH;
    }
}
