package javax.microedition.m3g;

import java.util.Vector;

/**
 * Besides the M3G API this keeps an interleaved, pre-scaled copy of the vertex data in the
 * format the native renderer consumes: per vertex 9 words {u, v, ABGR, nx, ny, nz, x, y, z}
 * (floats stored as raw bits). It is rebuilt whenever a referenced array changes.
 */
public class VertexBuffer extends Object3D {
    static final int STRIDE = 9;
    static final int HAS_NORMALS = 1;
    static final int HAS_COLORS = 2;
    static final int HAS_TEX = 4;

    VertexArray positions;
    float posScale = 1;
    final float[] posBias = new float[3];
    VertexArray normals;
    VertexArray colors;
    final VertexArray[] texCoords = new VertexArray[2];
    final float[] texScale = { 1, 1 };
    final float[][] texBias = { new float[3], new float[3] };
    int defaultColor = 0xffffffff;
    int version;

    int[] cache;
    int cacheFlags;
    private int cacheKey = -1;

    // ---- render caches under a memory budget (not in the M3G API) ----
    // Each cache is 36 bytes per vertex, several times the source arrays. Kept for every
    // model ever drawn they would slowly fill a 12 MB heap, so past a budget the ones
    // drawn longest ago are dropped (and rebuilt if they are drawn again).
    /** milliseconds, updated by Graphics3D.bindTarget */
    static long ahora;
    long ultimoUso;
    private static VertexBuffer[] conCache = new VertexBuffer[256];
    private static int nConCache;
    private static int bytesCache;
    static int presupuesto = 3072 * 1024;

    /** KB held by vertex caches, for the devlog */
    public static int cacheKB() {
        return bytesCache >> 10;
    }

    /** sets the budget for vertex caches (KB) */
    public static void presupuestoKB(int kb) {
        presupuesto = kb << 10;
    }

    private void soltar() {
        if (cache != null) {
            bytesCache -= cache.length * 4;
            cache = null;
            cacheKey = -1;
        }
    }

    /** drop least recently drawn caches until `extra` more bytes fit in the budget */
    private static void podar(int extra, VertexBuffer actual) {
        while (bytesCache + extra > presupuesto && nConCache > 0) {
            int peor = -1;
            for (int i = 0; i < nConCache; i++) {
                VertexBuffer v = conCache[i];
                if (v != actual && v.ultimoUso < ahora && (peor < 0 || v.ultimoUso < conCache[peor].ultimoUso)) {
                    peor = i;
                }
            }
            if (peor < 0) {
                return; // everything left is in use this frame
            }
            conCache[peor].soltar();
            conCache[peor] = conCache[--nConCache];
            conCache[nConCache] = null;
        }
    }

    private void registrar() {
        for (int i = 0; i < nConCache; i++) {
            if (conCache[i] == this) {
                return;
            }
        }
        if (nConCache == conCache.length) {
            VertexBuffer[] n = new VertexBuffer[conCache.length * 2];
            System.arraycopy(conCache, 0, n, 0, nConCache);
            conCache = n;
        }
        conCache[nConCache++] = this;
    }

    public VertexBuffer() {
    }

    public int getVertexCount() {
        return positions != null ? positions.vertexCount : 0;
    }

    public void setPositions(VertexArray a, float scale, float[] bias) {
        if (a != null && a.componentCount != 3) {
            throw new IllegalArgumentException();
        }
        positions = a;
        posScale = scale;
        if (bias != null) {
            posBias[0] = bias[0];
            posBias[1] = bias[1];
            posBias[2] = bias[2];
        } else {
            posBias[0] = posBias[1] = posBias[2] = 0;
        }
        version++;
    }

    public VertexArray getPositions(float[] scaleBias) {
        if (scaleBias != null) {
            scaleBias[0] = posScale;
            scaleBias[1] = posBias[0];
            scaleBias[2] = posBias[1];
            scaleBias[3] = posBias[2];
        }
        return positions;
    }

    public void setTexCoords(int unit, VertexArray a, float scale, float[] bias) {
        if (unit < 0 || unit > 1) {
            throw new IndexOutOfBoundsException();
        }
        texCoords[unit] = a;
        texScale[unit] = scale;
        float[] b = texBias[unit];
        if (bias != null) {
            for (int i = 0; i < bias.length && i < 3; i++) {
                b[i] = bias[i];
            }
        } else {
            b[0] = b[1] = b[2] = 0;
        }
        version++;
    }

    public VertexArray getTexCoords(int unit, float[] scaleBias) {
        if (scaleBias != null && texCoords[unit] != null) {
            scaleBias[0] = texScale[unit];
            for (int i = 0; i < texCoords[unit].componentCount && i + 1 < scaleBias.length; i++) {
                scaleBias[i + 1] = texBias[unit][i];
            }
        }
        return texCoords[unit];
    }

    public void setNormals(VertexArray a) {
        if (a != null && a.componentCount != 3) {
            throw new IllegalArgumentException();
        }
        normals = a;
        version++;
    }

    public VertexArray getNormals() {
        return normals;
    }

    public void setColors(VertexArray a) {
        if (a != null && (a.componentSize != 1)) {
            throw new IllegalArgumentException();
        }
        colors = a;
        version++;
    }

    public VertexArray getColors() {
        return colors;
    }

    public void setDefaultColor(int argb) {
        defaultColor = argb;
        version++;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    private int key() {
        int k = version * 31;
        if (positions != null) {
            k += positions.version * 7 + System.identityHashCode(positions);
        }
        if (normals != null) {
            k = k * 31 + normals.version + System.identityHashCode(normals);
        }
        if (colors != null) {
            k = k * 31 + colors.version + System.identityHashCode(colors);
        }
        if (texCoords[0] != null) {
            k = k * 31 + texCoords[0].version + System.identityHashCode(texCoords[0]);
        }
        return k;
    }

    /**
     * Not in the M3G API: builds the interleaved vertex data now. Called right after a
     * static model is created, while the heap is still clean, so a big model never needs
     * a large contiguous block for the first time in the middle of a long game.
     */
    public void preparar() {
        vertices();
    }

    /** Returns the interleaved vertex data, rebuilding it if needed. */
    int[] vertices() {
        if (positions == null) {
            return null;
        }
        int k = key();
        if (cache != null && k == cacheKey) {
            return cache;
        }
        int n = positions.vertexCount;
        soltar();
        podar((n * STRIDE + 6) * 4, this);
        // 6 extra words after the vertices: the bounding box (min xyz, max xyz) in object
        // space, so the PSP renderer can cull/accept a whole mesh with 8 corner tests
        int[] d = new int[n * STRIDE + 6];
        float bx0 = Float.MAX_VALUE, by0 = Float.MAX_VALUE, bz0 = Float.MAX_VALUE;
        float bx1 = -Float.MAX_VALUE, by1 = -Float.MAX_VALUE, bz1 = -Float.MAX_VALUE;
        int flags = 0;
        VertexArray tc = texCoords[0];
        float ts = texScale[0];
        float tbu = texBias[0][0];
        float tbv = texBias[0][1];
        if (tc != null) {
            flags |= HAS_TEX;
        }
        if (normals != null) {
            flags |= HAS_NORMALS;
        }
        if (colors != null) {
            flags |= HAS_COLORS;
        }
        int dc = defaultColor;
        int defNative = (dc & 0xff00ff00) | ((dc >> 16) & 0xff) | ((dc & 0xff) << 16);
        for (int i = 0; i < n; i++) {
            int o = i * STRIDE;
            if (tc != null && i < tc.vertexCount) {
                d[o] = Float.floatToIntBits(tc.value(i, 0) * ts + tbu);
                d[o + 1] = Float.floatToIntBits(tc.value(i, 1) * ts + tbv);
            }
            if (colors != null && i < colors.vertexCount) {
                int cc = colors.componentCount;
                int base = i * cc;
                byte[] cb = colors.bytes;
                int r = cb[base] & 0xff;
                int g = cb[base + 1] & 0xff;
                int b = cb[base + 2] & 0xff;
                int a = cc == 4 ? (cb[base + 3] & 0xff) : 255;
                d[o + 2] = (a << 24) | (b << 16) | (g << 8) | r;
            } else {
                d[o + 2] = defNative;
            }
            if (normals != null && i < normals.vertexCount) {
                float nx = normals.value(i, 0);
                float ny = normals.value(i, 1);
                float nz = normals.value(i, 2);
                float len = j2me.FMath.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }
                d[o + 3] = Float.floatToIntBits(nx);
                d[o + 4] = Float.floatToIntBits(ny);
                d[o + 5] = Float.floatToIntBits(nz);
            }
            float qx = positions.value(i, 0) * posScale + posBias[0];
            float qy = positions.value(i, 1) * posScale + posBias[1];
            float qz = positions.value(i, 2) * posScale + posBias[2];
            d[o + 6] = Float.floatToIntBits(qx);
            d[o + 7] = Float.floatToIntBits(qy);
            d[o + 8] = Float.floatToIntBits(qz);
            bx0 = Math.min(bx0, qx);
            by0 = Math.min(by0, qy);
            bz0 = Math.min(bz0, qz);
            bx1 = Math.max(bx1, qx);
            by1 = Math.max(by1, qy);
            bz1 = Math.max(bz1, qz);
        }
        int b = n * STRIDE;
        d[b] = Float.floatToIntBits(bx0);
        d[b + 1] = Float.floatToIntBits(by0);
        d[b + 2] = Float.floatToIntBits(bz0);
        d[b + 3] = Float.floatToIntBits(bx1);
        d[b + 4] = Float.floatToIntBits(by1);
        d[b + 5] = Float.floatToIntBits(bz1);
        j2me.Gfx.pixelsChanged(d);
        cache = d;
        cacheFlags = flags;
        cacheKey = k;
        bytesCache += d.length * 4;
        registrar();
        return d;
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        if (positions != null) {
            v.addElement(positions);
        }
        if (normals != null) {
            v.addElement(normals);
        }
        if (colors != null) {
            v.addElement(colors);
        }
        for (int i = 0; i < 2; i++) {
            if (texCoords[i] != null) {
                v.addElement(texCoords[i]);
            }
        }
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.COLOR) {
            defaultColor = Material.toARGB(v, defaultColor);
            version++;
        } else if (p == AnimationTrack.ALPHA) {
            defaultColor = (defaultColor & 0xffffff) | (Material.clamp8(v[0]) << 24);
            version++;
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    Object3D duplicateImpl() {
        VertexBuffer b = new VertexBuffer();
        b.copyBase(this);
        b.positions = positions;
        b.posScale = posScale;
        System.arraycopy(posBias, 0, b.posBias, 0, 3);
        b.normals = normals;
        b.colors = colors;
        for (int i = 0; i < 2; i++) {
            b.texCoords[i] = texCoords[i];
            b.texScale[i] = texScale[i];
            System.arraycopy(texBias[i], 0, b.texBias[i], 0, 3);
        }
        b.defaultColor = defaultColor;
        return b;
    }
}
