package javax.microedition.m3g;

import java.util.Vector;

public abstract class Node extends Transformable {
    public static final int NONE = 144;
    public static final int ORIGIN = 145;
    public static final int X_AXIS = 146;
    public static final int Y_AXIS = 147;
    public static final int Z_AXIS = 148;

    Node parent;
    boolean renderingEnabled = true;
    boolean pickingEnabled = true;
    float alphaFactor = 1.0f;
    int scope = -1;
    int zTarget = NONE;
    int yTarget = NONE;
    Node zRef;
    Node yRef;

    Node() {
    }

    public Node getParent() {
        return parent;
    }

    public void setRenderingEnable(boolean e) {
        renderingEnabled = e;
    }

    public boolean isRenderingEnabled() {
        return renderingEnabled;
    }

    public void setPickingEnable(boolean e) {
        pickingEnabled = e;
    }

    public boolean isPickingEnabled() {
        return pickingEnabled;
    }

    public void setAlphaFactor(float a) {
        if (a < 0 || a > 1) {
            throw new IllegalArgumentException();
        }
        alphaFactor = a;
    }

    public float getAlphaFactor() {
        return alphaFactor;
    }

    public void setScope(int s) {
        scope = s;
    }

    public int getScope() {
        return scope;
    }

    public void setAlignment(Node zr, int zt, Node yr, int yt) {
        zRef = zr;
        zTarget = zt;
        yRef = yr;
        yTarget = yt;
    }

    public void align(Node reference) {
    }

    public Node getAlignmentReference(int axis) {
        return axis == Z_AXIS ? zRef : yRef;
    }

    public int getAlignmentTarget(int axis) {
        return axis == Z_AXIS ? zTarget : yTarget;
    }

    /** Computes the transform from this node to the coordinate system of `target` (an ancestor or null). */
    public boolean getTransformTo(Node target, Transform out) {
        float[] a = new float[16];
        if (!toRoot(a)) {
            return false;
        }
        if (target == null) {
            out.set(a);
            return true;
        }
        float[] b = new float[16];
        target.toRoot(b);
        if (!Transform.invert(b, b)) {
            return false;
        }
        Transform.mul(b, a, out.m);
        return true;
    }

    boolean toRoot(float[] out) {
        compose(out);
        float[] t = new float[16];
        for (Node p = parent; p != null; p = p.parent) {
            p.compose(t);
            Transform.mul(t, out, out);
        }
        return true;
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.ALPHA) {
            float a = v[0];
            alphaFactor = a < 0 ? 0 : (a > 1 ? 1 : a);
        } else if (p == AnimationTrack.VISIBILITY) {
            renderingEnabled = v[0] >= 0.5f;
        } else if (p == AnimationTrack.PICKABILITY) {
            pickingEnabled = v[0] >= 0.5f;
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
    }

    void copyNode(Node f) {
        copyTransformable(f);
        renderingEnabled = f.renderingEnabled;
        pickingEnabled = f.pickingEnabled;
        alphaFactor = f.alphaFactor;
        scope = f.scope;
        zTarget = f.zTarget;
        yTarget = f.yTarget;
        zRef = f.zRef;
        yRef = f.yRef;
    }

    /** Queue renderable content; `world` is the node-to-world matrix (row-major). */
    void render(RenderQueue q, float[] world, float alpha, int scopeMask) {
    }
}
