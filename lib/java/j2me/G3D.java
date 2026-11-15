package j2me;

/** Native 3D backend used by javax.microedition.m3g.Graphics3D. */
public final class G3D {
    private G3D() {
    }

    public static native void begin(int vx, int vy, int vw, int vh, int cx, int cy, int cw, int ch);

    public static native void viewport(int x, int y, int w, int h);

    public static native void end();

    public static native void clear(int argb, boolean color, boolean depth);

    /** Row-major 4x4 projection matrix. */
    public static native void setProjection(float[] m);

    /** n lights, 16 floats each: mode, r, g, b, pos xyz, dir xyz, att c/l/q, spot angle, spot exp. */
    public static native void setLights(int n, float[] data);

    /**
     * Draw an indexed triangle list. verts holds 9 words per vertex
     * {u, v, ABGR, nx, ny, nz, x, y, z}; mv is the row-major modelview matrix; state is the
     * packed appearance (see Graphics3D.drawSubmesh).
     */
    public static native void draw(int[] verts, int vcount, char[] indices, int icount, float[] mv, int[] state,
            int[] tex, int tw, int th);

    /** zlib inflate. */
    public static native byte[] inflate(byte[] data, int off, int len, int outLen);
}
