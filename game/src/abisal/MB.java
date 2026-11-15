package abisal;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.CompositingMode;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Material;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.PolygonMode;
import javax.microedition.m3g.TriangleStripArray;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;

/**
 * Mesh builder for the procedural low-poly models. Every triangle gets its own three
 * vertices with the face normal (flat shading) and a per-vertex colour, so a model is just
 * a list of coloured faces. Positions are stored as shorts scaled by {@link #scale}.
 */
public final class MB {
    float[] pos = new float[3 * 256];
    float[] nrm = new float[3 * 256];
    int[] col = new int[256];
    boolean[] luz = new boolean[86];
    int n;
    /** faces added while true glow: they are drawn unlit (bioluminescence, lamps) */
    boolean brillo;
    /** metres per short unit: 1/256 m gives +-128 m of range with 4 mm precision */
    float scale = 1f / 256f;
    /** current colour (0xRRGGBB), used by the shape helpers */
    int color = 0xffffff;
    /** optional colour variation applied per face (0 = none), deterministic */
    int jitter;
    private int seed = 12345;
    /** current transform applied to added points: rotation (3x3) + translation */
    private final float[] m = new float[12];
    private final float[] tmp = new float[9];

    public MB() {
        identity();
    }

    public void identity() {
        for (int i = 0; i < 12; i++) {
            m[i] = 0;
        }
        m[0] = m[4] = m[8] = 1;
    }

    /** translate the local frame */
    public void move(float x, float y, float z) {
        m[9] += m[0] * x + m[3] * y + m[6] * z;
        m[10] += m[1] * x + m[4] * y + m[7] * z;
        m[11] += m[2] * x + m[5] * y + m[8] * z;
    }

    /** rotate the local frame about its X, Y or Z axis (degrees) */
    public void rot(int axis, float deg) {
        float a = deg * 0.017453292f;
        float c = (float) Math.cos(a);
        float s = (float) Math.sin(a);
        int i0;
        int i1;
        if (axis == 0) {
            i0 = 3;
            i1 = 6;
        } else if (axis == 1) {
            i0 = 6;
            i1 = 0;
        } else {
            i0 = 0;
            i1 = 3;
        }
        for (int k = 0; k < 3; k++) {
            float u = m[i0 + k];
            float v = m[i1 + k];
            m[i0 + k] = u * c + v * s;
            m[i1 + k] = -u * s + v * c;
        }
    }

    private void grow() {
        int cap = col.length * 2;
        float[] p = new float[cap * 3];
        float[] q = new float[cap * 3];
        int[] c = new int[cap];
        boolean[] l = new boolean[cap / 3 + 1];
        System.arraycopy(luz, 0, l, 0, n / 3);
        luz = l;
        System.arraycopy(pos, 0, p, 0, n * 3);
        System.arraycopy(nrm, 0, q, 0, n * 3);
        System.arraycopy(col, 0, c, 0, n);
        pos = p;
        nrm = q;
        col = c;
    }

    private int rnd() {
        seed = seed * 1103515245 + 12345;
        return (seed >>> 16) & 0x7fff;
    }

    private int faceColor(int c) {
        if (jitter == 0) {
            return c;
        }
        int d = (rnd() % (2 * jitter + 1)) - jitter;
        int r = clamp(((c >> 16) & 255) + d);
        int g = clamp(((c >> 8) & 255) + d);
        int b = clamp((c & 255) + d);
        return (r << 16) | (g << 8) | b;
    }

    static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /** triangle in local coordinates, counter-clockwise seen from the front */
    public void tri(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
        triC(ax, ay, az, bx, by, bz, cx, cy, cz, faceColor(color));
    }

    public void triC(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
            int c) {
        if (n + 3 > col.length) {
            grow();
        }
        float[] w = tmp;
        xf(ax, ay, az, w, 0);
        xf(bx, by, bz, w, 3);
        xf(cx, cy, cz, w, 6);
        float ux = w[3] - w[0];
        float uy = w[4] - w[1];
        float uz = w[5] - w[2];
        float vx = w[6] - w[0];
        float vy = w[7] - w[1];
        float vz = w[8] - w[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) {
            return;
        }
        nx /= l;
        ny /= l;
        nz /= l;
        luz[n / 3] = brillo;
        for (int k = 0; k < 3; k++) {
            int o = n * 3;
            pos[o] = w[k * 3];
            pos[o + 1] = w[k * 3 + 1];
            pos[o + 2] = w[k * 3 + 2];
            nrm[o] = nx;
            nrm[o + 1] = ny;
            nrm[o + 2] = nz;
            col[n] = c;
            n++;
        }
    }

    private void xf(float x, float y, float z, float[] out, int o) {
        out[o] = m[0] * x + m[3] * y + m[6] * z + m[9];
        out[o + 1] = m[1] * x + m[4] * y + m[7] * z + m[10];
        out[o + 2] = m[2] * x + m[5] * y + m[8] * z + m[11];
    }

    public void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
            float dx, float dy, float dz) {
        int c = faceColor(color);
        triC(ax, ay, az, bx, by, bz, cx, cy, cz, c);
        triC(ax, ay, az, cx, cy, cz, dx, dy, dz, c);
    }

    /**
     * Surface of revolution around the local Y axis. prof holds (radius, y) pairs from
     * bottom to top; seg is the number of sides. cols (optional) gives one colour per band.
     */
    public void lathe(float[] prof, int seg, int[] cols) {
        int bands = prof.length / 2 - 1;
        for (int b = 0; b < bands; b++) {
            float r0 = prof[b * 2];
            float y0 = prof[b * 2 + 1];
            float r1 = prof[b * 2 + 2];
            float y1 = prof[b * 2 + 3];
            int save = color;
            if (cols != null) {
                color = cols[b < cols.length ? b : cols.length - 1];
            }
            for (int s = 0; s < seg; s++) {
                float a0 = (float) (s * 2 * Math.PI / seg);
                float a1 = (float) ((s + 1) * 2 * Math.PI / seg);
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                if (r0 < 1e-4f) {
                    tri(0, y0, 0, r1 * c1, y1, -r1 * s1, r1 * c0, y1, -r1 * s0);
                } else if (r1 < 1e-4f) {
                    tri(r0 * c0, y0, -r0 * s0, r0 * c1, y0, -r0 * s1, 0, y1, 0);
                } else {
                    quad(r0 * c0, y0, -r0 * s0, r0 * c1, y0, -r0 * s1, r1 * c1, y1, -r1 * s1, r1 * c0, y1,
                            -r1 * s0);
                }
            }
            color = save;
        }
    }

    /** axis-aligned box centred at (x, y, z) in the local frame */
    public void box(float x, float y, float z, float sx, float sy, float sz) {
        float x0 = x - sx / 2;
        float x1 = x + sx / 2;
        float y0 = y - sy / 2;
        float y1 = y + sy / 2;
        float z0 = z - sz / 2;
        float z1 = z + sz / 2;
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1); // front
        quad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0); // back
        quad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1); // right
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0); // left
        quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0); // top
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1); // bottom
    }

    /** low-poly ellipsoid (UV sphere) centred at the local origin */
    public void ellipsoid(float rx, float ry, float rz, int seg, int rings) {
        float[] prof = new float[(rings + 1) * 2];
        for (int i = 0; i <= rings; i++) {
            float a = (float) (-Math.PI / 2 + Math.PI * i / rings);
            prof[i * 2] = (float) Math.cos(a);
            prof[i * 2 + 1] = (float) Math.sin(a) * ry;
        }
        // lathe is around Y with unit radius; scale X/Z through the frame
        float[] save = new float[12];
        System.arraycopy(m, 0, save, 0, 12);
        for (int k = 0; k < 3; k++) {
            m[k] *= rx;
            m[6 + k] *= rz;
        }
        lathe(prof, seg, null);
        System.arraycopy(save, 0, m, 0, 12);
    }

    /**
     * Irregular rock: a sphere whose radius is perturbed per vertex (deterministic in
     * seed). Colours come from the current colour with jitter.
     */
    public void roca(float rx, float ry, float rz, int seg, int rings, int semilla, float rugosidad) {
        float[] px = new float[(rings + 1) * seg];
        float[] py = new float[(rings + 1) * seg];
        float[] pz = new float[(rings + 1) * seg];
        for (int j = 0; j <= rings; j++) {
            float a = (float) (-Math.PI / 2 + Math.PI * j / rings);
            for (int i = 0; i < seg; i++) {
                float o = (float) (2 * Math.PI * i / seg);
                int key = (j == 0 || j == rings) ? j * 977 : j * 977 + i * 131;
                float k = 1 + (Terreno.azar(key, semilla, 77) - 0.5f) * 2 * rugosidad;
                int q = j * seg + i;
                px[q] = (float) (Math.cos(a) * Math.cos(o)) * rx * k;
                py[q] = (float) Math.sin(a) * ry * k;
                pz[q] = (float) (-Math.cos(a) * Math.sin(o)) * rz * k;
            }
        }
        for (int j = 0; j < rings; j++) {
            for (int i = 0; i < seg; i++) {
                int i1 = (i + 1) % seg;
                int a = j * seg + i;
                int b2 = j * seg + i1;
                int c = (j + 1) * seg + i1;
                int d = (j + 1) * seg + i;
                if (j > 0) {
                    tri(px[a], py[a], pz[a], px[b2], py[b2], pz[b2], px[c], py[c], pz[c]);
                }
                if (j < rings - 1) {
                    tri(px[a], py[a], pz[a], px[c], py[c], pz[c], px[d], py[d], pz[d]);
                }
            }
        }
    }

    /** pointed hexagonal crystal along the local +Y axis */
    public void cristal(float r, float h) {
        lathe(new float[] { 0f, -0.1f * h, r, 0f, r, h * 0.75f, 0f, h }, 6, null);
    }

    // ------------------------------------------------------------------ output

    /** Build a Mesh. glow > 0 makes the faces emissive (bioluminescence), alpha < 255 blends. */
    public Mesh build(Fog fog, int alpha, boolean twoSided, int emissive) {
        VertexArray pa = new VertexArray(n, 3, 2);
        short[] ps = new short[n * 3];
        float inv = 1f / scale;
        for (int i = 0; i < n * 3; i++) {
            float v = pos[i] * inv;
            ps[i] = (short) (v < -32767 ? -32767 : (v > 32767 ? 32767 : Math.round(v)));
        }
        pa.set(0, n, ps);
        VertexArray na = new VertexArray(n, 3, 1);
        byte[] nb = new byte[n * 3];
        for (int i = 0; i < n * 3; i++) {
            nb[i] = (byte) Math.round(nrm[i] * 127);
        }
        na.set(0, n, nb);
        VertexArray ca = new VertexArray(n, 4, 1);
        byte[] cb = new byte[n * 4];
        for (int i = 0; i < n; i++) {
            cb[i * 4] = (byte) (col[i] >> 16);
            cb[i * 4 + 1] = (byte) (col[i] >> 8);
            cb[i * 4 + 2] = (byte) col[i];
            cb[i * 4 + 3] = (byte) alpha;
        }
        ca.set(0, n, cb);
        VertexBuffer vb = new VertexBuffer();
        vb.setPositions(pa, scale, null);
        vb.setNormals(na);
        vb.setColors(ca);
        int tris = n / 3;
        int nl = 0;
        for (int i = 0; i < tris; i++) {
            if (luz[i]) {
                nl++;
            }
        }
        Appearance normal = appearance(fog, alpha, twoSided, emissive);
        if (nl == 0) {
            return new Mesh(vb, tiras(0, tris, false), normal);
        }
        Appearance ap2 = new Appearance();
        PolygonMode pm = new PolygonMode();
        pm.setShading(PolygonMode.SHADE_FLAT);
        pm.setCulling(twoSided ? PolygonMode.CULL_NONE : PolygonMode.CULL_BACK);
        ap2.setPolygonMode(pm);
        if (fog != null) {
            ap2.setFog(fog);
        }
        if (nl == tris) {
            return new Mesh(vb, tiras(0, tris, true), ap2);
        }
        return new Mesh(vb, new TriangleStripArray[] { tiras(tris - nl, tris, false), tiras(nl, tris, true) },
                new Appearance[] { normal, ap2 });
    }

    /** index buffer with the triangles whose glow flag equals g (count of them given) */
    private TriangleStripArray tiras(int count, int tris, boolean g) {
        if (count == 0) {
            count = tris;
        }
        int[] idx = new int[count * 3];
        int[] lens = new int[count];
        int k = 0;
        for (int i = 0; i < tris && k < count; i++) {
            if (luz[i] == g || count == tris) {
                idx[k * 3] = i * 3;
                idx[k * 3 + 1] = i * 3 + 1;
                idx[k * 3 + 2] = i * 3 + 2;
                lens[k] = 3;
                k++;
            }
        }
        return new TriangleStripArray(idx, lens);
    }

    public Mesh build(Fog fog) {
        return build(fog, 255, false, 0);
    }

    static Appearance appearance(Fog fog, int alpha, boolean twoSided, int emissive) {
        Appearance ap = new Appearance();
        Material mat = new Material();
        mat.setVertexColorTrackingEnable(true);
        mat.setColor(Material.EMISSIVE, emissive);
        mat.setColor(Material.SPECULAR, 0);
        ap.setMaterial(mat);
        PolygonMode pm = new PolygonMode();
        pm.setShading(PolygonMode.SHADE_FLAT);
        pm.setCulling(twoSided ? PolygonMode.CULL_NONE : PolygonMode.CULL_BACK);
        pm.setTwoSidedLightingEnable(twoSided);
        ap.setPolygonMode(pm);
        if (alpha < 255) {
            CompositingMode cm = new CompositingMode();
            cm.setBlending(CompositingMode.ALPHA);
            cm.setDepthWriteEnable(false);
            ap.setCompositingMode(cm);
            ap.setLayer(2);
        }
        if (fog != null) {
            ap.setFog(fog);
        }
        return ap;
    }
}
