package javax.microedition.m3g;

public class Fog extends Object3D {
    public static final int EXPONENTIAL = 80;
    public static final int LINEAR = 81;

    int mode = LINEAR;
    int color;
    float density = 1;
    float near;
    float far = 1;

    public Fog() {
    }

    public void setMode(int m) {
        mode = m;
    }

    public int getMode() {
        return mode;
    }

    public void setLinear(float n, float f) {
        near = n;
        far = f;
    }

    public float getNearDistance() {
        return near;
    }

    public float getFarDistance() {
        return far;
    }

    public void setDensity(float d) {
        density = d;
    }

    public float getDensity() {
        return density;
    }

    public void setColor(int rgb) {
        color = rgb & 0xffffff;
    }

    public int getColor() {
        return color;
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.COLOR) {
            color = Material.toRGB(v);
        } else if (p == AnimationTrack.DENSITY) {
            density = v[0];
        } else if (p == AnimationTrack.NEAR_DISTANCE) {
            near = v[0];
        } else if (p == AnimationTrack.FAR_DISTANCE) {
            far = v[0];
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    Object3D duplicateImpl() {
        Fog f = new Fog();
        f.copyBase(this);
        f.mode = mode;
        f.color = color;
        f.density = density;
        f.near = near;
        f.far = far;
        return f;
    }
}
