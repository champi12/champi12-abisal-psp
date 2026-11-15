package javax.microedition.m3g;

import java.util.Vector;

public class Background extends Object3D {
    public static final int BORDER = 32;
    public static final int REPEAT = 33;

    int color;
    Image2D image;
    int modeX = BORDER;
    int modeY = BORDER;
    int cropX;
    int cropY;
    int cropW;
    int cropH;
    boolean colorClear = true;
    boolean depthClear = true;

    public Background() {
    }

    public void setColorClearEnable(boolean e) {
        colorClear = e;
    }

    public boolean isColorClearEnabled() {
        return colorClear;
    }

    public void setDepthClearEnable(boolean e) {
        depthClear = e;
    }

    public boolean isDepthClearEnabled() {
        return depthClear;
    }

    public void setColor(int argb) {
        color = argb;
    }

    public int getColor() {
        return color;
    }

    public void setImage(Image2D img) {
        image = img;
        if (img != null) {
            cropW = img.width;
            cropH = img.height;
        }
    }

    public Image2D getImage() {
        return image;
    }

    public void setImageMode(int mx, int my) {
        modeX = mx;
        modeY = my;
    }

    public int getImageModeX() {
        return modeX;
    }

    public int getImageModeY() {
        return modeY;
    }

    public void setCrop(int x, int y, int w, int h) {
        cropX = x;
        cropY = y;
        cropW = w;
        cropH = h;
    }

    public int getCropX() {
        return cropX;
    }

    public int getCropY() {
        return cropY;
    }

    public int getCropWidth() {
        return cropW;
    }

    public int getCropHeight() {
        return cropH;
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.COLOR) {
            color = Material.toARGB(v, color);
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        if (image != null) {
            v.addElement(image);
        }
    }

    Object3D duplicateImpl() {
        Background b = new Background();
        b.copyBase(this);
        b.color = color;
        b.image = image;
        b.modeX = modeX;
        b.modeY = modeY;
        b.cropX = cropX;
        b.cropY = cropY;
        b.cropW = cropW;
        b.cropH = cropH;
        b.colorClear = colorClear;
        b.depthClear = depthClear;
        return b;
    }
}
