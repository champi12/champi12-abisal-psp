package javax.microedition.m3g;

public class Material extends Object3D {
    public static final int AMBIENT = 1024;
    public static final int DIFFUSE = 2048;
    public static final int EMISSIVE = 4096;
    public static final int SPECULAR = 8192;

    int ambient = 0x333333;
    int diffuse = 0xffcccccc;
    int emissive;
    int specular;
    float shininess;
    boolean tracking;

    public Material() {
    }

    public void setColor(int target, int argb) {
        if ((target & AMBIENT) != 0) {
            ambient = argb & 0xffffff;
        }
        if ((target & DIFFUSE) != 0) {
            diffuse = argb;
        }
        if ((target & EMISSIVE) != 0) {
            emissive = argb & 0xffffff;
        }
        if ((target & SPECULAR) != 0) {
            specular = argb & 0xffffff;
        }
    }

    public int getColor(int target) {
        switch (target) {
            case AMBIENT:
                return ambient;
            case DIFFUSE:
                return diffuse;
            case EMISSIVE:
                return emissive;
            case SPECULAR:
                return specular;
            default:
                throw new IllegalArgumentException();
        }
    }

    public void setShininess(float s) {
        if (s < 0 || s > 128) {
            throw new IllegalArgumentException();
        }
        shininess = s;
    }

    public float getShininess() {
        return shininess;
    }

    public void setVertexColorTrackingEnable(boolean e) {
        tracking = e;
    }

    public boolean isVertexColorTrackingEnabled() {
        return tracking;
    }

    static int clamp8(float f) {
        int v = (int) (f * 255 + 0.5f);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    static int toRGB(float[] v) {
        return (clamp8(v[0]) << 16) | (clamp8(v.length > 1 ? v[1] : v[0]) << 8) | clamp8(v.length > 2 ? v[2] : v[0]);
    }

    static int toARGB(float[] v, int old) {
        int a = v.length > 3 ? clamp8(v[3]) : (old >>> 24);
        return (a << 24) | toRGB(v);
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.AMBIENT_COLOR) {
            ambient = toRGB(v);
        } else if (p == AnimationTrack.DIFFUSE_COLOR) {
            diffuse = toARGB(v, diffuse);
        } else if (p == AnimationTrack.EMISSIVE_COLOR) {
            emissive = toRGB(v);
        } else if (p == AnimationTrack.SPECULAR_COLOR) {
            specular = toRGB(v);
        } else if (p == AnimationTrack.SHININESS) {
            shininess = v[0];
        } else if (p == AnimationTrack.ALPHA) {
            diffuse = (diffuse & 0xffffff) | (clamp8(v[0]) << 24);
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    Object3D duplicateImpl() {
        Material m = new Material();
        m.copyBase(this);
        m.ambient = ambient;
        m.diffuse = diffuse;
        m.emissive = emissive;
        m.specular = specular;
        m.shininess = shininess;
        m.tracking = tracking;
        return m;
    }
}
