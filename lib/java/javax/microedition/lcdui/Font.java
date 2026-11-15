package javax.microedition.lcdui;

/** Fonts are rendered from a small built-in bitmap font scaled per size. */
public final class Font {
    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;
    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;
    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;
    public static final int FONT_STATIC_TEXT = 0;
    public static final int FONT_INPUT_TEXT = 1;

    private static Font defaultFont;
    final int face;
    final int style;
    final int size;

    private Font(int face, int style, int size) {
        this.face = face;
        this.style = style;
        this.size = size;
    }

    public static Font getDefaultFont() {
        if (defaultFont == null) {
            defaultFont = new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
        }
        return defaultFont;
    }

    public static Font getFont(int spec) {
        return getDefaultFont();
    }

    public static Font getFont(int face, int style, int size) {
        return new Font(face, style, size);
    }

    public int getFace() {
        return face;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public boolean isPlain() {
        return style == 0;
    }

    public boolean isBold() {
        return (style & STYLE_BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & STYLE_ITALIC) != 0;
    }

    public boolean isUnderlined() {
        return (style & STYLE_UNDERLINED) != 0;
    }

    int scale() {
        return size == SIZE_LARGE ? 2 : 1;
    }

    public int getHeight() {
        return 10 * scale();
    }

    public int getBaselinePosition() {
        return 8 * scale();
    }

    public int charWidth(char c) {
        return 6 * scale();
    }

    public int charsWidth(char[] c, int off, int len) {
        return len * 6 * scale();
    }

    public int stringWidth(String s) {
        return s.length() * 6 * scale();
    }

    public int substringWidth(String s, int off, int len) {
        return len * 6 * scale();
    }
}
