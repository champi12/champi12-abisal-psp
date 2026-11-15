package javax.microedition.m3g;

import java.util.Vector;

public class Texture2D extends Transformable {
    public static final int FILTER_BASE_LEVEL = 208;
    public static final int FILTER_LINEAR = 209;
    public static final int FILTER_NEAREST = 210;
    public static final int FUNC_ADD = 224;
    public static final int FUNC_BLEND = 225;
    public static final int FUNC_DECAL = 226;
    public static final int FUNC_MODULATE = 227;
    public static final int FUNC_REPLACE = 228;
    public static final int WRAP_CLAMP = 240;
    public static final int WRAP_REPEAT = 241;

    Image2D image;
    int blending = FUNC_MODULATE;
    int blendColor;
    int wrapS = WRAP_REPEAT;
    int wrapT = WRAP_REPEAT;
    int levelFilter = FILTER_BASE_LEVEL;
    int imageFilter = FILTER_NEAREST;

    public Texture2D(Image2D img) {
        setImage(img);
    }

    public void setImage(Image2D img) {
        if (img == null) {
            throw new NullPointerException();
        }
        int w = img.width;
        int h = img.height;
        if ((w & (w - 1)) != 0 || (h & (h - 1)) != 0) {
            throw new IllegalArgumentException("texture dimensions must be powers of two");
        }
        image = img;
    }

    public Image2D getImage() {
        return image;
    }

    public void setFiltering(int level, int img) {
        levelFilter = level;
        imageFilter = img;
    }

    public int getLevelFilter() {
        return levelFilter;
    }

    public int getImageFilter() {
        return imageFilter;
    }

    public void setWrapping(int s, int t) {
        wrapS = s;
        wrapT = t;
    }

    public int getWrappingS() {
        return wrapS;
    }

    public int getWrappingT() {
        return wrapT;
    }

    public void setBlending(int f) {
        blending = f;
    }

    public int getBlending() {
        return blending;
    }

    public void setBlendColor(int rgb) {
        blendColor = rgb & 0xffffff;
    }

    public int getBlendColor() {
        return blendColor;
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.COLOR) {
            blendColor = Material.toRGB(v);
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        v.addElement(image);
    }

    Object3D duplicateImpl() {
        Texture2D t = new Texture2D(image);
        t.copyTransformable(this);
        t.blending = blending;
        t.blendColor = blendColor;
        t.wrapS = wrapS;
        t.wrapT = wrapT;
        t.levelFilter = levelFilter;
        t.imageFilter = imageFilter;
        return t;
    }
}
