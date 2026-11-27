package abisal;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Group;
import javax.microedition.m3g.Loader;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Node;
import javax.microedition.m3g.Object3D;
import javax.microedition.m3g.Transform;
import javax.microedition.m3g.VertexArray;
import javax.microedition.m3g.VertexBuffer;

/**
 * A model made in Blender and exported with the M3G add-on (assets/modelos/*.py build
 * them, tools/blender/exportar_modelos.sh exports them). Each piece is a Mesh found by its
 * user ID (the "#N" at the end of the Blender object name). Blender is Z-up: the exporter
 * puts that conversion in each node's transform, which immediate-mode rendering ignores,
 * so it is kept here (base[]) and applied when a piece is drawn.
 */
final class ModeloM3G {
    final Mesh[] pieza;
    final Transform[] base;

    private ModeloM3G(int n) {
        pieza = new Mesh[n];
        base = new Transform[n];
    }

    /** loads the model and finds pieces 1..n by ID; null if anything is missing */
    static ModeloM3G cargar(String ruta, Fog fog, int n) {
        try {
            Object3D[] raiz = Loader.load(ruta);
            ModeloM3G m = new ModeloM3G(n);
            for (int i = 0; i < raiz.length; i++) {
                m.buscar(raiz[i], fog);
            }
            for (int i = 0; i < n; i++) {
                if (m.pieza[i] == null) {
                    Log.d("modelo " + ruta + ": falta la pieza #" + (i + 1));
                    return null;
                }
            }
            Log.d("modelo " + ruta + " cargado (" + n + " piezas)");
            return m;
        } catch (Throwable e) {
            Log.d("modelo " + ruta + " no se pudo cargar: " + e);
            return null;
        }
    }

    private void buscar(Object3D o, Fog fog) {
        if (o instanceof Mesh) {
            Mesh me = (Mesh) o;
            int id = me.getUserID();
            if (id >= 1 && id <= pieza.length) {
                pieza[id - 1] = me;
                Transform t = new Transform();
                me.getCompositeTransform(t);
                base[id - 1] = t;
                for (int k = 0; k < me.getSubmeshCount(); k++) {
                    Appearance ap = me.getAppearance(k);
                    if (ap != null) {
                        ap.setFog(fog);
                        // the add-on writes an almost white emissive colour on every material
                        // when Blender's emission is off: keep emission only where the colour
                        // matches it (the glowing parts), otherwise everything washes out white
                        javax.microedition.m3g.Material mat = ap.getMaterial();
                        if (mat != null) {
                            int em = mat.getColor(javax.microedition.m3g.Material.EMISSIVE) & 0xffffff;
                            int di = mat.getColor(javax.microedition.m3g.Material.DIFFUSE) & 0xffffff;
                            // (allowing for a unit of rounding per channel between the two)
                            if (!casiIgual(em, di)) {
                                mat.setColor(javax.microedition.m3g.Material.EMISSIVE, 0);
                            }
                            // the add-on sets the ambient colour to a fifth of the diffuse one;
                            // the game's own models use the full colour for both, so the water's
                            // ambient light shows them the same way
                            mat.setColor(javax.microedition.m3g.Material.AMBIENT, di);
                        }
                    }
                }
            }
        }
        if (o instanceof Group) {
            Group g = (Group) o;
            for (int i = 0; i < g.getChildCount(); i++) {
                buscar(g.getChild(i), fog);
            }
        }
    }

    /**
     * A model made in Blender as drop-in parts for the game's own animation code: pieces
     * 1..n with their Blender transform baked into the vertices (so they are in game axes,
     * Y up, facing +Z, exactly like the code-built parts they replace). alpha < 255 makes
     * them translucent, dosCaras draws both faces. null if the file or a piece is missing.
     */
    static Mesh[] partes(String ruta, Fog fog, int n, int alpha, boolean dosCaras) {
        ModeloM3G m = cargar(ruta, fog, n);
        if (m == null) {
            return null;
        }
        try {
            for (int i = 0; i < n; i++) {
                hornear(m.pieza[i], m.base[i]);
                ajustar(m.pieza[i], alpha, dosCaras);
            }
        } catch (Throwable e) {
            Log.d("modelo " + ruta + ": no se pudo preparar: " + e);
            return null;
        }
        return m.pieza;
    }

    /** one rigid mesh (a model with a single piece #1) */
    static Mesh malla(String ruta, Fog fog, int alpha, boolean dosCaras) {
        Mesh[] p = partes(ruta, fog, 1, alpha, dosCaras);
        return p == null ? null : p[0];
    }

    private static final float[] TMP4 = new float[4];

    /** apply the transform t to the mesh's positions and normals (requantised to shorts) */
    private static void hornear(Mesh me, Transform t) {
        VertexBuffer vb = me.getVertexBuffer();
        float[] sb = new float[4];
        VertexArray pa = vb.getPositions(sb);
        int nv = pa.getVertexCount();
        float[] f = new float[nv * 3];
        leer(pa, f);
        float[] v = TMP4;
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, z0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE, z1 = -Float.MAX_VALUE;
        for (int i = 0; i < nv; i++) {
            v[0] = f[i * 3] * sb[0] + sb[1];
            v[1] = f[i * 3 + 1] * sb[0] + sb[2];
            v[2] = f[i * 3 + 2] * sb[0] + sb[3];
            v[3] = 1;
            t.transform(v);
            f[i * 3] = v[0];
            f[i * 3 + 1] = v[1];
            f[i * 3 + 2] = v[2];
            x0 = Math.min(x0, v[0]);
            y0 = Math.min(y0, v[1]);
            z0 = Math.min(z0, v[2]);
            x1 = Math.max(x1, v[0]);
            y1 = Math.max(y1, v[1]);
            z1 = Math.max(z1, v[2]);
        }
        float bx = (x0 + x1) / 2, by = (y0 + y1) / 2, bz = (z0 + z1) / 2;
        float r = Math.max(x1 - x0, Math.max(y1 - y0, z1 - z0));
        float esc = Math.max(r, 1e-4f) / 65000f;
        short[] q = new short[nv * 3];
        for (int i = 0; i < nv; i++) {
            q[i * 3] = (short) Math.round((f[i * 3] - bx) / esc);
            q[i * 3 + 1] = (short) Math.round((f[i * 3 + 1] - by) / esc);
            q[i * 3 + 2] = (short) Math.round((f[i * 3 + 2] - bz) / esc);
        }
        VertexArray np = new VertexArray(nv, 3, 2);
        np.set(0, nv, q);
        vb.setPositions(np, esc, new float[] { bx, by, bz });
        VertexArray na = vb.getNormals();
        if (na != null) {
            float[] g = new float[nv * 3];
            leer(na, g);
            short[] qn = new short[nv * 3];
            for (int i = 0; i < nv; i++) {
                v[0] = g[i * 3];
                v[1] = g[i * 3 + 1];
                v[2] = g[i * 3 + 2];
                v[3] = 0; // a direction: no translation
                t.transform(v);
                float l = j2me.FMath.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
                if (l < 1e-6f) {
                    l = 1;
                }
                qn[i * 3] = (short) (v[0] / l * 32000);
                qn[i * 3 + 1] = (short) (v[1] / l * 32000);
                qn[i * 3 + 2] = (short) (v[2] / l * 32000);
            }
            VertexArray nn = new VertexArray(nv, 3, 2);
            nn.set(0, nv, qn);
            vb.setNormals(nn);
        }
    }

    private static void leer(VertexArray a, float[] out) {
        int nv = a.getVertexCount(), nc = a.getComponentCount();
        if (a.getComponentType() == 1) {
            byte[] b = new byte[nv * nc];
            a.get(0, nv, b);
            for (int i = 0; i < nv; i++) {
                for (int k = 0; k < 3; k++) {
                    out[i * 3 + k] = k < nc ? b[i * nc + k] : 0;
                }
            }
        } else {
            short[] s = new short[nv * nc];
            a.get(0, nv, s);
            for (int i = 0; i < nv; i++) {
                for (int k = 0; k < 3; k++) {
                    out[i * 3 + k] = k < nc ? s[i * nc + k] : 0;
                }
            }
        }
    }

    /** translucency and two-sided faces, like the code-built parts (MB.appearance) */
    private static void ajustar(Mesh me, int alpha, boolean dosCaras) {
        for (int k = 0; k < me.getSubmeshCount(); k++) {
            Appearance ap = me.getAppearance(k);
            if (ap == null) {
                continue;
            }
            if (dosCaras) {
                javax.microedition.m3g.PolygonMode pm = ap.getPolygonMode();
                if (pm == null) {
                    pm = new javax.microedition.m3g.PolygonMode();
                    ap.setPolygonMode(pm);
                }
                pm.setCulling(javax.microedition.m3g.PolygonMode.CULL_NONE);
                pm.setTwoSidedLightingEnable(true);
            }
            if (alpha < 255) {
                javax.microedition.m3g.CompositingMode cm = new javax.microedition.m3g.CompositingMode();
                cm.setBlending(javax.microedition.m3g.CompositingMode.ALPHA);
                cm.setDepthWriteEnable(false);
                ap.setCompositingMode(cm);
                ap.setLayer(2);
                javax.microedition.m3g.Material mat = ap.getMaterial();
                if (mat != null) {
                    int d = mat.getColor(javax.microedition.m3g.Material.DIFFUSE);
                    mat.setColor(javax.microedition.m3g.Material.DIFFUSE, (d & 0xffffff) | (alpha << 24));
                }
            }
        }
    }

    /** the glowing parts (emissive materials) shine through the fog, like a beacon */
    void brilloSinNiebla() {
        for (int i = 0; i < pieza.length; i++) {
            Mesh me = pieza[i];
            for (int k = 0; k < me.getSubmeshCount(); k++) {
                Appearance ap = me.getAppearance(k);
                if (ap != null && ap.getMaterial() != null
                        && (ap.getMaterial().getColor(javax.microedition.m3g.Material.EMISSIVE) & 0xffffff) != 0) {
                    ap.setFog(null);
                }
            }
        }
    }

    private static boolean casiIgual(int a, int b) {
        for (int sh = 0; sh < 24; sh += 8) {
            int d = ((a >> sh) & 255) - ((b >> sh) & 255);
            if (d > 3 || d < -3) {
                return false;
            }
        }
        return true;
    }
}
