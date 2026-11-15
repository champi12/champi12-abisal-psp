package javax.microedition.lcdui;

import j2me.Gfx;

/** 2D drawing context. Rendering is done by natives in j2me.Gfx. */
public class Graphics {
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    final Image target;
    int tx;
    int ty;
    int clipX;
    int clipY;
    int clipW;
    int clipH;
    int color = 0xff000000;
    int strokeStyle;
    Font font = Font.getDefaultFont();
    final int width;
    final int height;

    Graphics(Image target) {
        this.target = target;
        if (target == null) {
            width = Gfx.screenWidth();
            height = Gfx.screenHeight();
        } else {
            width = target.width;
            height = target.height;
        }
        clipW = width;
        clipH = height;
    }

    private void bind() {
        if (target == null) {
            Gfx.bind(null, 0, 0, 0, clipX, clipY, clipW, clipH);
        } else {
            Gfx.bind(target.pixels, target.stride, target.width, target.height, clipX, clipY, clipW, clipH);
        }
    }

    public void translate(int x, int y) {
        tx += x;
        ty += y;
    }

    public int getTranslateX() {
        return tx;
    }

    public int getTranslateY() {
        return ty;
    }

    public int getColor() {
        return color & 0xffffff;
    }

    public int getRedComponent() {
        return (color >> 16) & 0xff;
    }

    public int getGreenComponent() {
        return (color >> 8) & 0xff;
    }

    public int getBlueComponent() {
        return color & 0xff;
    }

    public int getGrayScale() {
        return (getRedComponent() * 77 + getGreenComponent() * 151 + getBlueComponent() * 28) >> 8;
    }

    public void setColor(int r, int g, int b) {
        if ((r | g | b) < 0 || r > 255 || g > 255 || b > 255) {
            throw new IllegalArgumentException();
        }
        color = 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public void setColor(int rgb) {
        color = 0xff000000 | rgb;
    }

    public void setGrayScale(int v) {
        setColor(v, v, v);
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font f) {
        font = f == null ? Font.getDefaultFont() : f;
    }

    public void setStrokeStyle(int s) {
        strokeStyle = s;
    }

    public int getStrokeStyle() {
        return strokeStyle;
    }

    public int getClipX() {
        return clipX - tx;
    }

    public int getClipY() {
        return clipY - ty;
    }

    public int getClipWidth() {
        return clipW;
    }

    public int getClipHeight() {
        return clipH;
    }

    public void clipRect(int x, int y, int w, int h) {
        x += tx;
        y += ty;
        int x2 = Math.min(x + w, clipX + clipW);
        int y2 = Math.min(y + h, clipY + clipH);
        clipX = Math.max(x, clipX);
        clipY = Math.max(y, clipY);
        clipW = Math.max(0, x2 - clipX);
        clipH = Math.max(0, y2 - clipY);
    }

    public void setClip(int x, int y, int w, int h) {
        x += tx;
        y += ty;
        int x2 = Math.min(x + w, width);
        int y2 = Math.min(y + h, height);
        clipX = Math.max(0, x);
        clipY = Math.max(0, y);
        clipW = Math.max(0, x2 - clipX);
        clipH = Math.max(0, y2 - clipY);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        bind();
        Gfx.drawLine(x1 + tx, y1 + ty, x2 + tx, y2 + ty, color);
    }

    public void fillRect(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        bind();
        Gfx.fillRect(x + tx, y + ty, w, h, color);
    }

    public void drawRect(int x, int y, int w, int h) {
        if (w < 0 || h < 0) {
            return;
        }
        bind();
        x += tx;
        y += ty;
        if (w == 0 || h == 0) {
            Gfx.fillRect(x, y, w + 1, h + 1, color);
            return;
        }
        Gfx.fillRect(x, y, w, 1, color);
        Gfx.fillRect(x + w, y, 1, h, color);
        Gfx.fillRect(x + 1, y + h, w, 1, color);
        Gfx.fillRect(x, y + 1, 1, h, color);
    }

    public void clearRect(int x, int y, int w, int h) {
        fillRect(x, y, w, h);
    }

    public void drawRoundRect(int x, int y, int w, int h, int aw, int ah) {
        drawRect(x, y, w, h);
    }

    public void fillRoundRect(int x, int y, int w, int h, int aw, int ah) {
        fillRect(x, y, w, h);
    }

    public void drawArc(int x, int y, int w, int h, int start, int arc) {
        bind();
        Gfx.arc(x + tx, y + ty, w, h, start, arc, color, false);
    }

    public void fillArc(int x, int y, int w, int h, int start, int arc) {
        bind();
        Gfx.arc(x + tx, y + ty, w, h, start, arc, color, true);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        bind();
        Gfx.fillTriangle(x1 + tx, y1 + ty, x2 + tx, y2 + ty, x3 + tx, y3 + ty, color);
    }

    public void drawString(String s, int x, int y, int anchor) {
        drawSubstring(s, 0, s.length(), x, y, anchor);
    }

    public void drawSubstring(String s, int off, int len, int x, int y, int anchor) {
        int w = font.substringWidth(s, off, len);
        x = anchorX(x, w, anchor);
        if ((anchor & BOTTOM) != 0) {
            y -= font.getHeight();
        } else if ((anchor & BASELINE) != 0) {
            y -= font.getBaselinePosition();
        } else if ((anchor & VCENTER) != 0) {
            y -= font.getHeight() / 2;
        }
        bind();
        char[] c = new char[len];
        s.getChars(off, off + len, c, 0);
        Gfx.drawText(c, x + tx, y + ty, color, font.size);
    }

    public void drawChar(char ch, int x, int y, int anchor) {
        drawString(String.valueOf(ch), x, y, anchor);
    }

    public void drawChars(char[] d, int off, int len, int x, int y, int anchor) {
        drawString(new String(d, off, len), x, y, anchor);
    }

    private static int anchorX(int x, int w, int anchor) {
        if ((anchor & HCENTER) != 0) {
            return x - w / 2;
        }
        if ((anchor & RIGHT) != 0) {
            return x - w;
        }
        return x;
    }

    private static int anchorY(int y, int h, int anchor) {
        if ((anchor & VCENTER) != 0) {
            return y - h / 2;
        }
        if ((anchor & BOTTOM) != 0) {
            return y - h;
        }
        return y;
    }

    public void drawImage(Image img, int x, int y, int anchor) {
        if (img == null) {
            throw new NullPointerException();
        }
        x = anchorX(x, img.width, anchor);
        y = anchorY(y, img.height, anchor);
        bind();
        Gfx.drawImage(img.pixels, img.stride, img.width, img.height, 0, 0, img.width, img.height, 0,
                x + tx, y + ty, img.alpha);
    }

    public void drawRegion(Image src, int sx, int sy, int w, int h, int transform, int x, int y, int anchor) {
        if (src == null) {
            throw new NullPointerException();
        }
        if (sx < 0 || sy < 0 || w < 0 || h < 0 || sx + w > src.width || sy + h > src.height) {
            throw new IllegalArgumentException();
        }
        int dw = w;
        int dh = h;
        if ((transform & 4) != 0) {
            dw = h;
            dh = w;
        }
        x = anchorX(x, dw, anchor);
        y = anchorY(y, dh, anchor);
        bind();
        Gfx.drawImage(src.pixels, src.stride, src.width, src.height, sx, sy, w, h, transform, x + tx, y + ty,
                src.alpha);
    }

    public void copyArea(int sx, int sy, int w, int h, int x, int y, int anchor) {
    }

    public void drawRGB(int[] rgb, int offset, int scan, int x, int y, int w, int h, boolean alpha) {
        if (rgb == null) {
            throw new NullPointerException();
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        bind();
        Gfx.drawRGB(rgb, offset, scan, x + tx, y + ty, w, h, alpha);
    }

    public int getDisplayColor(int c) {
        return c & 0xffffff;
    }
}
