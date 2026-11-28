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
            return cargar1(ruta, fog, n);
        } catch (OutOfMemoryError e) {
            // the heap is fragmented by the loads before: collect and try once more
            System.gc();
            Log.d("modelo " + ruta + ": sin memoria, se reintenta tras recoger basura");
            try {
                return cargar1(ruta, fog, n);
            } catch (OutOfMemoryError e2) {
                Log.d("modelo " + ruta + " no se pudo cargar: " + e2);
                return null;
            }
        }
    }

    private static ModeloM3G cargar1(String ruta, Fog fog, int n) {
        // the loader's buffers for the previous file are garbage by now: with ~60 files
        // loaded back to back the heap peaked at 9 of 11 MB, too close on a real PSP
        Runtime rt = Runtime.getRuntime();
        if (rt.totalMemory() - rt.freeMemory() > 4 * 1024 * 1024) {
            System.gc();
        }
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
            Log.d("modelo " + ruta + " cargado (" + n + " piezas), heap " + (rt.totalMemory() - rt.freeMemory()) / 1024 + " KB");
            return m;
        } catch (OutOfMemoryError e) {
            throw e;
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
                            // no specular highlight, like the game's own models: with it the
                            // metal parts burned to white under the sun
                            mat.setColor(javax.microedition.m3g.Material.SPECULAR, 0);
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
        return partes(ruta, fog, n, alpha, dosCaras, null, 0);
    }

    /**
     * Like partes(), with a texture (rock, metal...) box-projected over the merged model
     * like the code-built props do: each face takes the plane it faces most; uvEscala is
     * texture repeats per metre.
     */
    static Mesh[] partes(String ruta, Fog fog, int n, int alpha, boolean dosCaras,
            javax.microedition.m3g.Texture2D tex, float uvEscala) {
        texActual = tex;
        uvActual = uvEscala;
        ModeloM3G m = cargar(ruta, fog, n);
        if (m == null) {
            return null;
        }
        try {
            for (int i = 0; i < n; i++) {
                hornear(m.pieza[i], m.base[i]);
                // one material per colour would be one draw call per colour: merge them
                // into vertex colours (two submeshes at most: lit and glowing)
                Mesh f = fusionar(m.pieza[i], fog, alpha, dosCaras);
                if (f != null) {
                    m.pieza[i] = f;
                } else {
                    ajustar(m.pieza[i], alpha, dosCaras);
                }
            }
        } catch (Throwable e) {
            Log.d("modelo " + ruta + ": no se pudo preparar: " + e);
            if (e instanceof OutOfMemoryError && !reintentando) {
                // fragmented heap: collect and build it once more
                reintentando = true;
                System.gc();
                Mesh[] r = partes(ruta, fog, n, alpha, dosCaras, tex, uvEscala);
                reintentando = false;
                return r;
            }
            return null;
        }
        return m.pieza;
    }

    private static boolean reintentando;

    /** first-person pieces (the hands): baked like partes(), lit but never fogged */
    static Mesh[] partesPrimerPlano(String ruta, Fog fog, int n) {
        Mesh[] p = partes(ruta, fog, n, 255, true);
        if (p != null) {
            for (int i = 0; i < n; i++) {
                for (int k = 0; k < p[i].getSubmeshCount(); k++) {
                    Appearance ap = p[i].getAppearance(k);
                    if (ap != null) {
                        ap.setFog(null);
                    }
                }
            }
        }
        return p;
    }

    /** one rigid mesh (a model with a single piece #1) */
    static Mesh malla(String ruta, Fog fog, int alpha, boolean dosCaras) {
        Mesh[] p = partes(ruta, fog, 1, alpha, dosCaras);
        return p == null ? null : p[0];
    }

    private static final float[] TMP4 = new float[4];
    /** the texture of the model being merged (set by partes) */
    private static javax.microedition.m3g.Texture2D texActual;
    private static float uvActual;

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

    /**
     * The mesh rebuilt like the game's own models: every triangle gets its own three
     * vertices coloured with its material's diffuse colour, lit triangles in one submesh and
     * glowing (emissive) ones in another, drawn unlit. Null if it has nothing to merge.
     */
    private static Mesh fusionar(Mesh me, Fog fog, int alpha, boolean dosCaras) {
        int ns = me.getSubmeshCount();
        VertexBuffer vb = me.getVertexBuffer();
        float[] sb = new float[4];
        VertexArray pa = vb.getPositions(sb);
        VertexArray na = vb.getNormals();
        int nv = pa.getVertexCount();
        float[] pos = new float[nv * 3];
        leer(pa, pos);
        float[] nor = new float[nv * 3];
        if (na != null) {
            leer(na, nor);
        }
        // block-style creatures carry their own UVs into the shared creature atlas
        float[] tsb = new float[4];
        VertexArray ta0 = texActual == null ? vb.getTexCoords(0, tsb) : null;
        float[] uvf = null;
        if (ta0 != null) {
            uvf = new float[nv * 3];
            leer(ta0, uvf);
        }
        // triangles per submesh, and which are glowing
        int[][] idx = new int[ns][];
        int[] color = new int[ns];
        boolean[] brillo = new boolean[ns];
        int nLuz = 0, nBri = 0;
        for (int k = 0; k < ns; k++) {
            javax.microedition.m3g.IndexBuffer ib = me.getIndexBuffer(k);
            idx[k] = new int[ib.getIndexCount()];
            ib.getIndices(idx[k]);
            Appearance ap = me.getAppearance(k);
            javax.microedition.m3g.Material mat = ap != null ? ap.getMaterial() : null;
            int di = mat != null ? mat.getColor(javax.microedition.m3g.Material.DIFFUSE) & 0xffffff : 0xffffff;
            int em = mat != null ? mat.getColor(javax.microedition.m3g.Material.EMISSIVE) & 0xffffff : 0;
            color[k] = di;
            brillo[k] = em != 0;
            if (brillo[k]) {
                nBri += idx[k].length / 3;
            } else {
                nLuz += idx[k].length / 3;
            }
        }
        int nt = nLuz + nBri;
        if (nt == 0) {
            return null;
        }
        // vertices are shared inside each submesh (same original vertex, same colour), so the
        // merged model has about as many vertices as the original - not three per triangle
        // (the PSP's clipping works per vertex: fewer vertices, less work)
        int[] mapa = new int[nv];
        int cap = nt * 3;
        short[] qp = new short[cap * 3];
        short[] qn = new short[cap * 3];
        byte[] cc = new byte[cap * 4];
        javax.microedition.m3g.Texture2D tex = texActual;
        short[] uv = tex != null || uvf != null ? new short[cap * 2] : null;
        if (uvf != null) {
            tex = Texturas.criaturas();
        }
        int[] idxLuz = new int[nLuz * 3], idxBri = new int[nBri * 3];
        int iLuz = 0, iBri = 0;
        int n = 0;
        float esc = sb[0];
        float[] w = new float[9];
        for (int pasada = 0; pasada < 2; pasada++) { // lit submeshes first, then the glowing ones
            for (int k = 0; k < ns; k++) {
                if (brillo[k] != (pasada == 1)) {
                    continue;
                }
                for (int q = 0; q < nv; q++) {
                    mapa[q] = -1;
                }
                int c = color[k];
                int[] ix = idx[k];
                for (int t = 0; t + 2 < ix.length; t += 3) {
                    // a textured model needs per-face UVs: its vertices are not shared
                    boolean propio = uv != null && uvf == null && !brillo[k];
                    if (propio) {
                        for (int q = 0; q < 3; q++) {
                            int s0 = ix[t + q];
                            w[q * 3] = pos[s0 * 3] * esc + sb[1];
                            w[q * 3 + 1] = pos[s0 * 3 + 1] * esc + sb[2];
                            w[q * 3 + 2] = pos[s0 * 3 + 2] * esc + sb[3];
                        }
                    }
                    for (int q = 0; q < 3; q++) {
                        int s0 = ix[t + q];
                        int d = propio ? -1 : mapa[s0];
                        if (d < 0) {
                            d = n++;
                            if (!propio) {
                                mapa[s0] = d;
                            }
                            qp[d * 3] = (short) pos[s0 * 3];
                            qp[d * 3 + 1] = (short) pos[s0 * 3 + 1];
                            qp[d * 3 + 2] = (short) pos[s0 * 3 + 2];
                            qn[d * 3] = (short) nor[s0 * 3];
                            qn[d * 3 + 1] = (short) nor[s0 * 3 + 1];
                            qn[d * 3 + 2] = (short) nor[s0 * 3 + 2];
                            cc[d * 4] = (byte) (c >> 16);
                            cc[d * 4 + 1] = (byte) (c >> 8);
                            cc[d * 4 + 2] = (byte) c;
                            cc[d * 4 + 3] = (byte) alpha;
                            if (propio) {
                                caja(w, q, uv, d);
                            } else if (uvf != null) {
                                // the file's UV, already in texture space (the exporter flips v: flipping
                                // it again read the unpainted bottom of the atlas, a magenta box)
                                float u = uvf[s0 * 3] * tsb[0] + tsb[1];
                                float vv = uvf[s0 * 3 + 1] * tsb[0] + tsb[2];
                                uv[d * 2] = clamp16(u * 8192);
                                uv[d * 2 + 1] = clamp16(vv * 8192);
                            }
                        }
                        if (brillo[k]) {
                            idxBri[iBri++] = d;
                        } else {
                            idxLuz[iLuz++] = d;
                        }
                    }
                }
            }
        }
        // trim the arrays to the vertices really used
        qp = recorta(qp, n * 3);
        qn = recorta(qn, n * 3);
        byte[] cc2 = new byte[n * 4];
        System.arraycopy(cc, 0, cc2, 0, n * 4);
        cc = cc2;
        if (uv != null) {
            uv = recorta(uv, n * 2);
        }
        VertexArray np = new VertexArray(n, 3, 2);
        np.set(0, n, qp);
        VertexArray nn = new VertexArray(n, 3, 2);
        nn.set(0, n, qn);
        VertexArray ca = new VertexArray(n, 4, 1);
        ca.set(0, n, cc);
        VertexBuffer nvb = new VertexBuffer();
        nvb.setPositions(np, esc, new float[] { sb[1], sb[2], sb[3] });
        nvb.setNormals(nn);
        nvb.setColors(ca);
        if (uv != null) {
            VertexArray ta = new VertexArray(n, 2, 2);
            ta.set(0, n, uv);
            nvb.setTexCoords(0, ta, uvf != null ? 1f / 8192f : 1f / 64f, null);
        }
        int partes = (nLuz > 0 ? 1 : 0) + (nBri > 0 ? 1 : 0);
        javax.microedition.m3g.IndexBuffer[] ibs = new javax.microedition.m3g.IndexBuffer[partes];
        Appearance[] aps = new Appearance[partes];
        int j = 0;
        if (nLuz > 0) {
            ibs[j] = lista(idxLuz);
            aps[j] = apariencia(fog, alpha, dosCaras, true);
            if (tex != null) {
                aps[j].setTexture(0, tex);
            }
            j++;
        }
        if (nBri > 0) {
            ibs[j] = lista(idxBri);
            aps[j] = apariencia(fog, alpha, dosCaras, false);
            if (uvf != null) {
                aps[j].setTexture(0, tex); // the glowing faces keep their pixel art too
            }
        }
        return new Mesh(nvb, ibs, aps);
    }

    /** box projection UV for corner q of the triangle w (9 floats, metres) */
    private static void caja(float[] w, int q, short[] uv, int d) {
        float ax = w[3] - w[0], ay = w[4] - w[1], az = w[5] - w[2];
        float bx = w[6] - w[0], by = w[7] - w[1], bz = w[8] - w[2];
        float nx = Math.abs(ay * bz - az * by), ny = Math.abs(az * bx - ax * bz), nz = Math.abs(ax * by - ay * bx);
        float px = w[q * 3], py = w[q * 3 + 1], pz = w[q * 3 + 2];
        float u, vv;
        if (ny >= nx && ny >= nz) {
            u = px;
            vv = pz;
        } else if (nx >= nz) {
            u = pz;
            vv = py;
        } else {
            u = px;
            vv = py;
        }
        uv[d * 2] = clamp16(u * uvActual * 64);
        uv[d * 2 + 1] = clamp16(vv * uvActual * 64);
    }

    private static short[] recorta(short[] a, int n) {
        short[] b = new short[n];
        System.arraycopy(a, 0, b, 0, n);
        return b;
    }

    /** a triangle list as a strip array (each strip one triangle) */
    private static javax.microedition.m3g.TriangleStripArray lista(int[] idx) {
        int[] l = new int[idx.length / 3];
        for (int i = 0; i < l.length; i++) {
            l[i] = 3;
        }
        return new javax.microedition.m3g.TriangleStripArray(idx, l);
    }

    private static short clamp16(float v) {
        return (short) (v < -32767 ? -32767 : (v > 32767 ? 32767 : Math.round(v)));
    }

    private static javax.microedition.m3g.TriangleStripArray tiras(int primero, int tris) {
        int[] l = new int[tris];
        for (int i = 0; i < tris; i++) {
            l[i] = 3;
        }
        return new javax.microedition.m3g.TriangleStripArray(primero, l);
    }

    /** like MB.appearance: lit with vertex colours, or unlit (the glowing parts) */
    private static Appearance apariencia(Fog fog, int alpha, boolean dosCaras, boolean iluminada) {
        Appearance ap = new Appearance();
        if (iluminada) {
            javax.microedition.m3g.Material mat = new javax.microedition.m3g.Material();
            mat.setVertexColorTrackingEnable(true);
            mat.setColor(javax.microedition.m3g.Material.EMISSIVE, 0);
            mat.setColor(javax.microedition.m3g.Material.SPECULAR, 0);
            ap.setMaterial(mat);
        }
        javax.microedition.m3g.PolygonMode pm = new javax.microedition.m3g.PolygonMode();
        pm.setShading(javax.microedition.m3g.PolygonMode.SHADE_FLAT);
        pm.setCulling(dosCaras ? javax.microedition.m3g.PolygonMode.CULL_NONE : javax.microedition.m3g.PolygonMode.CULL_BACK);
        pm.setTwoSidedLightingEnable(dosCaras);
        ap.setPolygonMode(pm);
        if (alpha < 255) {
            javax.microedition.m3g.CompositingMode cm = new javax.microedition.m3g.CompositingMode();
            cm.setBlending(javax.microedition.m3g.CompositingMode.ALPHA);
            cm.setDepthWriteEnable(false);
            ap.setCompositingMode(cm);
            ap.setLayer(2);
        }
        if (fog != null) {
            ap.setFog(fog);
        }
        return ap;
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
    /**
     * Merges each piece's colours into vertex colours (one or two draw calls per piece) with
     * the pixel-art texture box-projected in the piece's own space. Unlike partes() the pieces
     * are not baked: they keep their base transform, so the story places still animate them.
     */
    void texturizar(Fog fog, javax.microedition.m3g.Texture2D tex, float uvEscala) {
        texActual = tex;
        uvActual = uvEscala;
        try {
            for (int i = 0; i < pieza.length; i++) {
                Mesh f = fusionar(pieza[i], fog, 255, true);
                if (f != null) {
                    pieza[i] = f;
                }
            }
        } catch (Throwable e) {
            Log.d("modelo: no se pudo texturizar: " + e);
        }
        texActual = null;
    }

    void brilloSinNiebla() {
        for (int i = 0; i < pieza.length; i++) {
            Mesh me = pieza[i];
            for (int k = 0; k < me.getSubmeshCount(); k++) {
                Appearance ap = me.getAppearance(k);
                // (a merged piece's glowing part is the submesh without a material)
                if (ap != null && (ap.getMaterial() == null
                        || (ap.getMaterial().getColor(javax.microedition.m3g.Material.EMISSIVE) & 0xffffff) != 0)) {
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
