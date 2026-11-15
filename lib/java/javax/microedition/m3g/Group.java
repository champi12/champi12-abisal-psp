package javax.microedition.m3g;

import java.util.Vector;

public class Group extends Node {
    Node[] children = new Node[0];
    int childCount;

    public Group() {
    }

    public void addChild(Node child) {
        if (child == null) {
            throw new NullPointerException();
        }
        if (child == this || child instanceof World) {
            throw new IllegalArgumentException();
        }
        if (child.parent == this) {
            return;
        }
        if (child.parent != null) {
            throw new IllegalArgumentException("node already has a parent");
        }
        if (childCount == children.length) {
            Node[] n = new Node[children.length * 2 + 2];
            System.arraycopy(children, 0, n, 0, childCount);
            children = n;
        }
        children[childCount++] = child;
        child.parent = this;
    }

    public void removeChild(Node child) {
        for (int i = 0; i < childCount; i++) {
            if (children[i] == child) {
                System.arraycopy(children, i + 1, children, i, childCount - i - 1);
                children[--childCount] = null;
                child.parent = null;
                return;
            }
        }
    }

    public int getChildCount() {
        return childCount;
    }

    public Node getChild(int i) {
        if (i < 0 || i >= childCount) {
            throw new IndexOutOfBoundsException();
        }
        return children[i];
    }

    public boolean pick(int scope, float x, float y, Camera camera, RayIntersection ri) {
        return false;
    }

    public boolean pick(int scope, float ox, float oy, float oz, float dx, float dy, float dz, RayIntersection ri) {
        return false;
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        for (int i = 0; i < childCount; i++) {
            v.addElement(children[i]);
        }
    }

    Object3D duplicateImpl() {
        Group g = new Group();
        copyGroup(g);
        return g;
    }

    void copyGroup(Group g) {
        g.copyNode(this);
        for (int i = 0; i < childCount; i++) {
            g.addChild((Node) children[i].duplicate());
        }
    }

    void render(RenderQueue q, float[] world, float alpha, int scopeMask) {
        if (!renderingEnabled) {
            return;
        }
        float[] local = new float[16];
        for (int i = 0; i < childCount; i++) {
            Node c = children[i];
            if (!c.renderingEnabled) {
                continue;
            }
            if (c.isIdentity()) {
                c.render(q, world, alpha * c.alphaFactor, scopeMask);
            } else {
                c.compose(local);
                Transform.mulInto(world, local, local);
                c.render(q, local, alpha * c.alphaFactor, scopeMask);
            }
        }
    }
}
