package javax.microedition.m3g;

public class TriangleStripArray extends IndexBuffer {
    private int[] stripLengths;

    public TriangleStripArray(int firstIndex, int[] stripLengths) {
        int total = 0;
        for (int i = 0; i < stripLengths.length; i++) {
            total += stripLengths[i];
        }
        int[] idx = new int[total];
        for (int i = 0; i < total; i++) {
            idx[i] = firstIndex + i;
        }
        build(idx, stripLengths);
    }

    public TriangleStripArray(int[] indices, int[] stripLengths) {
        build(indices, stripLengths);
    }

    private void build(int[] idx, int[] lengths) {
        if (lengths.length == 0) {
            throw new IllegalArgumentException();
        }
        // the strip lengths are not kept: only the triangle indices are needed to draw
        // (saves 4 bytes per triangle of every model)
        int tris = 0;
        for (int i = 0; i < lengths.length; i++) {
            if (lengths[i] < 3) {
                throw new IllegalArgumentException();
            }
            tris += lengths[i] - 2;
        }
        char[] t = new char[tris * 3];
        int p = 0;
        int base = 0;
        for (int s = 0; s < lengths.length; s++) {
            int len = lengths[s];
            for (int i = 0; i < len - 2; i++) {
                int a = idx[base + i];
                int b = idx[base + i + 1];
                int c = idx[base + i + 2];
                if ((i & 1) == 0) {
                    t[p++] = (char) a;
                    t[p++] = (char) b;
                    t[p++] = (char) c;
                } else {
                    t[p++] = (char) b;
                    t[p++] = (char) a;
                    t[p++] = (char) c;
                }
            }
            base += len;
        }
        triangles = t;
        triangleIndexCount = p;
    }

    private TriangleStripArray(TriangleStripArray f) {
        copyBase(f);
        stripLengths = f.stripLengths;
        triangles = f.triangles;
        triangleIndexCount = f.triangleIndexCount;
    }

    Object3D duplicateImpl() {
        return new TriangleStripArray(this);
    }
}
