package javax.microedition.m3g;

public class PolygonMode extends Object3D {
    public static final int CULL_BACK = 160;
    public static final int CULL_FRONT = 161;
    public static final int CULL_NONE = 162;
    public static final int SHADE_FLAT = 164;
    public static final int SHADE_SMOOTH = 165;
    public static final int WINDING_CCW = 168;
    public static final int WINDING_CW = 169;

    int culling = CULL_BACK;
    int shading = SHADE_SMOOTH;
    int winding = WINDING_CCW;
    boolean twoSided;
    boolean localCamera;
    boolean perspectiveCorrection;

    public PolygonMode() {
    }

    public void setCulling(int c) {
        if (c < CULL_BACK || c > CULL_NONE) {
            throw new IllegalArgumentException();
        }
        culling = c;
    }

    public int getCulling() {
        return culling;
    }

    public void setWinding(int w) {
        if (w != WINDING_CCW && w != WINDING_CW) {
            throw new IllegalArgumentException();
        }
        winding = w;
    }

    public int getWinding() {
        return winding;
    }

    public void setShading(int s) {
        if (s != SHADE_FLAT && s != SHADE_SMOOTH) {
            throw new IllegalArgumentException();
        }
        shading = s;
    }

    public int getShading() {
        return shading;
    }

    public void setTwoSidedLightingEnable(boolean e) {
        twoSided = e;
    }

    public boolean isTwoSidedLightingEnabled() {
        return twoSided;
    }

    public void setLocalCameraLightingEnable(boolean e) {
        localCamera = e;
    }

    public boolean isLocalCameraLightingEnabled() {
        return localCamera;
    }

    public void setPerspectiveCorrectionEnable(boolean e) {
        perspectiveCorrection = e;
    }

    public boolean isPerspectiveCorrectionEnabled() {
        return perspectiveCorrection;
    }

    Object3D duplicateImpl() {
        PolygonMode p = new PolygonMode();
        p.copyBase(this);
        p.culling = culling;
        p.shading = shading;
        p.winding = winding;
        p.twoSided = twoSided;
        p.localCamera = localCamera;
        p.perspectiveCorrection = perspectiveCorrection;
        return p;
    }
}
