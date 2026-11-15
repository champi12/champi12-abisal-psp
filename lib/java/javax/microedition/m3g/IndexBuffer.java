package javax.microedition.m3g;

public abstract class IndexBuffer extends Object3D {
    /** Triangle list indices (counter-clockwise order preserved per strip parity). */
    char[] triangles;
    int triangleIndexCount;

    IndexBuffer() {
    }

    public int getIndexCount() {
        return triangleIndexCount;
    }

    public void getIndices(int[] out) {
        for (int i = 0; i < triangleIndexCount; i++) {
            out[i] = triangles[i];
        }
    }
}
