package javax.microedition.m3g;

public class VertexArray extends Object3D {
    final int vertexCount;
    final int componentCount;
    final int componentSize;
    final byte[] bytes;
    final short[] shorts;
    int version;

    public VertexArray(int numVertices, int numComponents, int componentSize) {
        if (numVertices < 1 || numVertices > 65535 || numComponents < 2 || numComponents > 4
                || componentSize < 1 || componentSize > 2) {
            throw new IllegalArgumentException();
        }
        vertexCount = numVertices;
        componentCount = numComponents;
        this.componentSize = componentSize;
        if (componentSize == 1) {
            bytes = new byte[numVertices * numComponents];
            shorts = null;
        } else {
            shorts = new short[numVertices * numComponents];
            bytes = null;
        }
    }

    public void set(int first, int n, short[] values) {
        if (shorts == null) {
            throw new IllegalStateException();
        }
        System.arraycopy(values, 0, shorts, first * componentCount, n * componentCount);
        version++;
    }

    public void set(int first, int n, byte[] values) {
        if (bytes == null) {
            throw new IllegalStateException();
        }
        System.arraycopy(values, 0, bytes, first * componentCount, n * componentCount);
        version++;
    }

    public void get(int first, int n, short[] values) {
        if (shorts == null) {
            throw new IllegalStateException();
        }
        System.arraycopy(shorts, first * componentCount, values, 0, n * componentCount);
    }

    public void get(int first, int n, byte[] values) {
        if (bytes == null) {
            throw new IllegalStateException();
        }
        System.arraycopy(bytes, first * componentCount, values, 0, n * componentCount);
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public int getComponentType() {
        return componentSize;
    }

    final float value(int v, int c) {
        int i = v * componentCount + c;
        return bytes != null ? bytes[i] : shorts[i];
    }

    final int raw(int i) {
        return bytes != null ? bytes[i] : shorts[i];
    }

    Object3D duplicateImpl() {
        VertexArray a = new VertexArray(vertexCount, componentCount, componentSize);
        a.copyBase(this);
        if (bytes != null) {
            System.arraycopy(bytes, 0, a.bytes, 0, bytes.length);
        } else {
            System.arraycopy(shorts, 0, a.shorts, 0, shorts.length);
        }
        return a;
    }
}
