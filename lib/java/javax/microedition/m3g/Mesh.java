package javax.microedition.m3g;

import java.util.Vector;

public class Mesh extends Node {
    VertexBuffer vertices;
    IndexBuffer[] submeshes;
    Appearance[] appearances;

    public Mesh(VertexBuffer vb, IndexBuffer ib, Appearance ap) {
        if (vb == null || ib == null) {
            throw new NullPointerException();
        }
        vertices = vb;
        submeshes = new IndexBuffer[1];
        submeshes[0] = ib;
        appearances = new Appearance[1];
        appearances[0] = ap;
    }

    public Mesh(VertexBuffer vb, IndexBuffer[] ibs, Appearance[] aps) {
        if (vb == null || ibs == null) {
            throw new NullPointerException();
        }
        if (ibs.length == 0 || (aps != null && aps.length < ibs.length)) {
            throw new IllegalArgumentException();
        }
        vertices = vb;
        submeshes = new IndexBuffer[ibs.length];
        System.arraycopy(ibs, 0, submeshes, 0, ibs.length);
        appearances = new Appearance[ibs.length];
        if (aps != null) {
            System.arraycopy(aps, 0, appearances, 0, ibs.length);
        }
    }

    Mesh() {
    }

    public void setAppearance(int i, Appearance a) {
        appearances[i] = a;
    }

    public Appearance getAppearance(int i) {
        return appearances[i];
    }

    public IndexBuffer getIndexBuffer(int i) {
        return submeshes[i];
    }

    public VertexBuffer getVertexBuffer() {
        return vertices;
    }

    public int getSubmeshCount() {
        return submeshes.length;
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        v.addElement(vertices);
        for (int i = 0; i < submeshes.length; i++) {
            v.addElement(submeshes[i]);
            if (appearances[i] != null) {
                v.addElement(appearances[i]);
            }
        }
    }

    Object3D duplicateImpl() {
        Mesh m = new Mesh();
        copyMesh(m);
        return m;
    }

    void copyMesh(Mesh m) {
        m.copyNode(this);
        m.vertices = vertices;
        m.submeshes = new IndexBuffer[submeshes.length];
        System.arraycopy(submeshes, 0, m.submeshes, 0, submeshes.length);
        m.appearances = new Appearance[appearances.length];
        System.arraycopy(appearances, 0, m.appearances, 0, appearances.length);
    }

    void render(RenderQueue q, float[] world, float alpha, int scopeMask) {
        if (!renderingEnabled || (scope & scopeMask) == 0) {
            return;
        }
        for (int i = 0; i < submeshes.length; i++) {
            if (appearances[i] != null) {
                q.add(this, i, world, alpha);
            }
        }
    }
}
