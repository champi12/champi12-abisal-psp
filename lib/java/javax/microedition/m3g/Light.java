package javax.microedition.m3g;

public class Light extends Node {
    public static final int AMBIENT = 128;
    public static final int DIRECTIONAL = 129;
    public static final int OMNI = 130;
    public static final int SPOT = 131;

    int mode = DIRECTIONAL;
    int color = 0xffffff;
    float intensity = 1;
    float attConst = 1;
    float attLinear;
    float attQuad;
    float spotAngle = 45;
    float spotExponent;

    public Light() {
    }

    public void setMode(int m) {
        mode = m;
    }

    public int getMode() {
        return mode;
    }

    public void setIntensity(float i) {
        intensity = i;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setColor(int rgb) {
        color = rgb & 0xffffff;
    }

    public int getColor() {
        return color;
    }

    public void setSpotAngle(float a) {
        spotAngle = a;
    }

    public float getSpotAngle() {
        return spotAngle;
    }

    public void setSpotExponent(float e) {
        spotExponent = e;
    }

    public float getSpotExponent() {
        return spotExponent;
    }

    public void setAttenuation(float c, float l, float q) {
        attConst = c;
        attLinear = l;
        attQuad = q;
    }

    public float getConstantAttenuation() {
        return attConst;
    }

    public float getLinearAttenuation() {
        return attLinear;
    }

    public float getQuadraticAttenuation() {
        return attQuad;
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.INTENSITY) {
            intensity = v[0];
        } else if (p == AnimationTrack.COLOR) {
            color = Material.toRGB(v);
        } else if (p == AnimationTrack.SPOT_ANGLE) {
            spotAngle = v[0];
        } else if (p == AnimationTrack.SPOT_EXPONENT) {
            spotExponent = v[0];
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    Object3D duplicateImpl() {
        Light l = new Light();
        l.copyNode(this);
        l.mode = mode;
        l.color = color;
        l.intensity = intensity;
        l.attConst = attConst;
        l.attLinear = attLinear;
        l.attQuad = attQuad;
        l.spotAngle = spotAngle;
        l.spotExponent = spotExponent;
        return l;
    }
}
