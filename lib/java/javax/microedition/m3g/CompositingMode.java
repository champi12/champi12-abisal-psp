package javax.microedition.m3g;

public class CompositingMode extends Object3D {
    public static final int ALPHA = 64;
    public static final int ALPHA_ADD = 65;
    public static final int MODULATE = 66;
    public static final int MODULATE_X2 = 67;
    public static final int REPLACE = 68;

    int blending = REPLACE;
    float alphaThreshold;
    boolean depthTest = true;
    boolean depthWrite = true;
    boolean colorWrite = true;
    boolean alphaWrite = true;
    float offsetFactor;
    float offsetUnits;

    public CompositingMode() {
    }

    public void setBlending(int b) {
        if (b < ALPHA || b > REPLACE) {
            throw new IllegalArgumentException();
        }
        blending = b;
    }

    public int getBlending() {
        return blending;
    }

    public void setAlphaThreshold(float t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException();
        }
        alphaThreshold = t;
    }

    public float getAlphaThreshold() {
        return alphaThreshold;
    }

    public void setAlphaWriteEnable(boolean e) {
        alphaWrite = e;
    }

    public boolean isAlphaWriteEnabled() {
        return alphaWrite;
    }

    public void setColorWriteEnable(boolean e) {
        colorWrite = e;
    }

    public boolean isColorWriteEnabled() {
        return colorWrite;
    }

    public void setDepthWriteEnable(boolean e) {
        depthWrite = e;
    }

    public boolean isDepthWriteEnabled() {
        return depthWrite;
    }

    public void setDepthTestEnable(boolean e) {
        depthTest = e;
    }

    public boolean isDepthTestEnabled() {
        return depthTest;
    }

    public void setDepthOffset(float f, float u) {
        offsetFactor = f;
        offsetUnits = u;
    }

    public float getDepthOffsetFactor() {
        return offsetFactor;
    }

    public float getDepthOffsetUnits() {
        return offsetUnits;
    }

    Object3D duplicateImpl() {
        CompositingMode c = new CompositingMode();
        c.copyBase(this);
        c.blending = blending;
        c.alphaThreshold = alphaThreshold;
        c.depthTest = depthTest;
        c.depthWrite = depthWrite;
        c.colorWrite = colorWrite;
        c.alphaWrite = alphaWrite;
        c.offsetFactor = offsetFactor;
        c.offsetUnits = offsetUnits;
        return c;
    }
}
