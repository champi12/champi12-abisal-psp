package abisal;

import java.util.Hashtable;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * The world around the player: terrain chunks streamed in and out as the player swims,
 * plus the props (rocks, corals, kelp) placed deterministically on each chunk. At most
 * one chunk is built per frame so streaming never causes a long hitch.
 */
public final class Mundo {
    /** chunk side in metres */
    public static final float LADO = Terreno.CHUNK * Terreno.CELDA;
    /** chunks kept around the player (radius, in chunks) */
    static final int RADIO = 2;
    static final int MAX = (2 * RADIO + 1) * (2 * RADIO + 1);

    // prop kinds
    public static final int P_COBRE = 0, P_CUARZO = 1, P_ABANICO = 2, P_TUBOS = 3, P_ALGA = 4, P_ROCA = 5,
            P_LANZA = 6, P_FLOR = 7, P_PENA = 8, P_PILAR = 9, P_ARCO = 10, P_CUEVA = 11, P_CRESTA = 12;
    static final int N_TIPOS = 13;
    /** collectable: what a prop gives (index into Inventario), -1 = scenery */
    public static final int[] DA = { Inventario.COBRE, Inventario.CUARZO, Inventario.CORAL, Inventario.CORAL,
            Inventario.FIBRA, Inventario.TITANIO, -1, -1, -1, -1, -1, -1, -1 };
    /** props that can only be harvested with the knife */
    public static final boolean[] CUCHILLO = { false, false, true, true, true, false, false, false, false, false,
            false, false, false };

    final Fog fog;
    final Mesh[][] modelos = new Mesh[N_TIPOS][];
    final Chunk[] chunks = new Chunk[MAX];
    int nChunks;
    /** props taken by the player: key "cx,cz,i" */
    final Hashtable recogidos = new Hashtable();
    // stats for the devlog overlay
    int trisDibujados;
    int chunksDibujados;
    int propsDibujados;
    int construidos;

    private final Transform t = new Transform();

    static final class Chunk {
        int cx;
        int cz;
        Mesh terreno;
        int tris;
        // props: kind, position, rotation, scale, variant
        int n;
        int[] tipo;
        float[] x;
        float[] y;
        float[] z;
        float[] rot;
        float[] esc;
        int[] var;
        int recursos;
        long t0;
        /** the terrain mesh is stored relative to this height */
        float y0;
        boolean[] quitado;
    }

    Mundo(Fog fog) {
        this.fog = fog;
        long t0 = System.currentTimeMillis();
        modelos[P_COBRE] = new Mesh[] { Modelos.rocaVeta(fog, 0x8d8790, 0xd27a3c, false, 3),
                Modelos.rocaVeta(fog, 0x958d86, 0xd27a3c, false, 5) };
        modelos[P_CUARZO] = new Mesh[] { Modelos.rocaVeta(fog, 0x7f8794, 0xf2d6e8, true, 8) };
        modelos[P_ABANICO] = new Mesh[] { Modelos.coralAbanico(fog, 1), Modelos.coralAbanico(fog, 6) };
        modelos[P_TUBOS] = new Mesh[] { Modelos.coralTubos(fog, 2), Modelos.coralTubos(fog, 9) };
        modelos[P_ALGA] = new Mesh[] { Modelos.algaCinta(fog, 4), Modelos.algaCinta(fog, 11),
                Modelos.algaCinta(fog, 17) };
        modelos[P_ROCA] = new Mesh[] { roca(fog, 21), roca(fog, 33) };
        modelos[P_LANZA] = new Mesh[] { Modelos.plantaLanza(fog, 3), Modelos.plantaLanza(fog, 8) };
        modelos[P_FLOR] = new Mesh[] { Modelos.florAbisal(fog, 2), Modelos.florAbisal(fog, 7) };
        // big boulders: the same rock, scaled up at placement; scenery, not a resource
        modelos[P_PENA] = modelos[P_ROCA];
        modelos[P_PILAR] = new Mesh[] { Modelos.pilarCanon(fog, 5), Modelos.pilarCanon(fog, 12) };
        modelos[P_ARCO] = new Mesh[] { Modelos.arcoPiedra(fog, 1), Modelos.arcoPiedra(fog, 4) };
        modelos[P_CUEVA] = new Mesh[] { Modelos.cueva(fog, 1), Modelos.cueva(fog, 6) };
        modelos[P_CRESTA] = new Mesh[] { Modelos.crestaArrecife(fog, 1), Modelos.crestaArrecife(fog, 9) };
        Log.d("modelos del mundo creados en " + (System.currentTimeMillis() - t0) + " ms; " + Log.memoria());
    }

    private static Mesh roca(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        b.color = 0x7d7a80;
        b.move(0, 0.4f, 0);
        b.roca(1.6f, 1.1f, 1.4f, 8, 5, semilla, 0.25f);
        return b.build(fog);
    }

    static int clave(int cx, int cz) {
        return (cx & 0xffff) << 16 | (cz & 0xffff);
    }

    private Chunk buscar(int cx, int cz) {
        for (int i = 0; i < nChunks; i++) {
            if (chunks[i].cx == cx && chunks[i].cz == cz) {
                return chunks[i];
            }
        }
        return null;
    }

    /**
     * Keep the chunks around (px, pz) loaded: drop far ones, build the nearest missing one.
     * Returns true if a chunk was built this frame.
     */
    boolean actualizar(float px, float pz) {
        int ccx = j2me.FMath.floor(px / LADO);
        int ccz = j2me.FMath.floor(pz / LADO);
        for (int i = 0; i < nChunks;) {
            Chunk c = chunks[i];
            if (Math.abs(c.cx - ccx) > RADIO || Math.abs(c.cz - ccz) > RADIO) {
                chunks[i] = chunks[--nChunks];
                chunks[nChunks] = null;
            } else {
                i++;
            }
        }
        // a chunk under construction advances one phase per frame (each phase is about a
        // third of the ~40 ms a whole chunk costs on a real PSP, so no frame spikes)
        if (pend != null) {
            if (Math.abs(pend.cx - ccx) > RADIO || Math.abs(pend.cz - ccz) > RADIO) {
                pend = null; // walked away before it was finished
            } else {
                int fa = Log.fase(Log.F_TROZO);
                if (pendFase == 1) {
                    pend.terreno = Terreno.mallaDe(pendH, pend.cx * LADO, pend.cz * LADO, Terreno.CHUNK, fog);
                    pend.tris = Terreno.CHUNK * Terreno.CHUNK * 2;
                    pendH = null;
                    pendFase = 2;
                } else {
                    chunks[nChunks++] = construir(pend);
                    pend = null;
                }
                Log.fase(fa);
                return true;
            }
        }
        // nearest missing chunk first (rings outward)
        for (int r = 0; r <= RADIO; r++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    if (buscar(ccx + dx, ccz + dz) == null && nChunks < MAX) {
                        int fa = Log.fase(Log.F_TROZO);
                        Chunk c = new Chunk();
                        c.cx = ccx + dx;
                        c.cz = ccz + dz;
                        c.t0 = System.currentTimeMillis();
                        float x0 = c.cx * LADO, z0 = c.cz * LADO;
                        c.y0 = Terreno.altura(x0 + LADO / 2, z0 + LADO / 2);
                        pendH = Terreno.alturas(x0, z0, Terreno.CHUNK, c.y0);
                        pend = c;
                        pendFase = 1;
                        Log.fase(fa);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** build every chunk around the start position at once (loading screen) */
    void cargarTodo(float px, float pz) {
        long t0 = System.currentTimeMillis();
        while (actualizar(px, pz)) {
        }
        Log.d("mundo inicial: " + nChunks + " trozos en " + (System.currentTimeMillis() - t0) + " ms; "
                + Log.memoria());
    }

    /** chunk being built across several frames */
    private Chunk pend;
    private int pendFase;
    private float[] pendH;

    /** last phase: the props of a chunk whose terrain is ready */
    private Chunk construir(Chunk c) {
        int faseAntes = Log.fase(Log.F_TROZO);
        long t0 = c.t0;
        int cx = c.cx, cz = c.cz;
        float x0 = cx * LADO;
        float z0 = cz * LADO;
        // props: one candidate per 4 m cell
        int cap = 40;
        c.tipo = new int[cap];
        c.x = new float[cap];
        c.y = new float[cap];
        c.z = new float[cap];
        c.rot = new float[cap];
        c.esc = new float[cap];
        c.var = new int[cap];
        c.quitado = new boolean[cap];
        int gx0 = cx * Terreno.CHUNK;
        int gz0 = cz * Terreno.CHUNK;
        for (int j = 0; j < Terreno.CHUNK && c.n < cap; j++) {
            for (int i = 0; i < Terreno.CHUNK && c.n < cap; i++) {
                int gx = gx0 + i;
                int gz = gz0 + j;
                float a = Terreno.azar(gx, gz, 101);
                float px = (gx + 0.2f + 0.6f * Terreno.azar(gx, gz, 102)) * Terreno.CELDA;
                float pz = (gz + 0.2f + 0.6f * Terreno.azar(gx, gz, 103)) * Terreno.CELDA;
                int bio = Terreno.bioma(px, pz);
                int tipo = elegir(bio, a, Terreno.azar(gx, gz, 104), veta(px, pz),
                        Terreno.fbm(px * 0.02f, pz * 0.02f, 61));
                if (tipo < 0) {
                    continue;
                }
                // keep the spawn area clear around the pod
                if (px * px + pz * pz < 12 * 12) {
                    continue;
                }
                int k = c.n++;
                c.tipo[k] = tipo;
                c.x[k] = px;
                c.z[k] = pz;
                c.y[k] = Terreno.altura(px, pz) - 0.15f;
                c.rot[k] = Terreno.azar(gx, gz, 105) * 360;
                c.esc[k] = 0.8f + Terreno.azar(gx, gz, 106) * 0.5f;
                if (tipo == P_PENA) {
                    c.esc[k] *= 2.4f;
                } else if (tipo == P_ROCA) {
                    // titanium outcrop: about a metre across, not a boulder
                    c.esc[k] *= 0.45f;
                }
                if (DA[tipo] >= 0 && !CUCHILLO[tipo]) {
                    c.recursos++;
                }
                c.var[k] = (int) (Terreno.azar(gx, gz, 107) * modelos[tipo].length) % modelos[tipo].length;
                c.quitado[k] = recogidos.get(cx + "," + cz + "," + k) != null;
            }
        }
        colocarFormaciones(c, x0, z0);
        construidos++;
        long ms = System.currentTimeMillis() - t0;
        if (construidos <= 60 || ms > 60) {
            Log.d("trozo " + cx + "," + cz + " (" + nombreBioma(Terreno.bioma(x0 + LADO / 2, z0 + LADO / 2))
                    + ") listo (3 fases, 1 por frame) en " + ms + " ms, " + c.n + " objetos, " + c.recursos + " recursos");
        }
        Log.fase(faseAntes);
        return c;
    }

    /**
     * Mineral veins: a low-frequency noise field. Resources (ore outcrops) cluster where it
     * is high and are rare elsewhere; vegetation does not depend on it. A rich patch is
     * guaranteed 20-50 m from the escape pod so the first tools can always be built.
     */
    static float veta(float x, float z) {
        float v = Terreno.fbm(x * 0.025f, z * 0.025f, 41);
        float r = j2me.FMath.sqrt(x * x + z * z);
        if (r > 15 && r < 55) {
            v += 0.22f * (1 - Math.abs(r - 35) / 20);
        }
        return v;
    }

    /**
     * Which prop (if any) is placed on a cell. a = density roll, b = kind roll, v = vein
     * field. Vegetation is common; ore only appears in veins (v > threshold), with a tiny
     * chance elsewhere, so resources have to be searched for.
     */
    private static int elegir(int bio, float a, float b, float v, float arrecife) {
        switch (bio) {
        case Terreno.BAJIOS: {
            // open sand with moss patches; corals grow in reef patches
            float coral = arrecife > 0.6f ? 0.05f : 0.003f;
            if (a < coral) {
                return b < 0.5f ? P_TUBOS : (b < 0.95f ? P_ABANICO : P_PENA);
            }
            float rec = v > 0.62f ? 0.014f : 0.0015f;
            if (a < coral + rec) {
                return b < 0.5f ? P_ROCA : (b < 0.82f ? P_COBRE : P_CUARZO);
            }
            return -1;
        }
        case Terreno.BOSQUE: {
            // the kelp grows in groves, with clearings between them
            float alga = arrecife > 0.5f ? 0.1f : 0.02f;
            if (a < alga) {
                return b < 0.9f ? P_ALGA : P_PENA;
            }
            float rec = v > 0.62f ? 0.025f : 0.002f;
            if (a < alga + rec) {
                return b < 0.45f ? P_ROCA : (b < 0.8f ? P_COBRE : P_CUARZO);
            }
            return -1;
        }
        case Terreno.MESETA: {
            float lanza = arrecife > 0.55f ? 0.06f : 0.01f;
            if (a < lanza) {
                return b < 0.8f ? P_LANZA : P_PENA;
            }
            float rec = v > 0.6f ? 0.03f : 0.002f;
            if (a < lanza + rec) {
                return b < 0.4f ? P_CUARZO : (b < 0.7f ? P_COBRE : P_ROCA);
            }
            return -1;
        }
        case Terreno.CANONES:
        case Terreno.ABISMO: {
            if (a < 0.05f) {
                if (bio == Terreno.CANONES && b > 0.85f) {
                    return P_PILAR;
                }
                return b < 0.75f ? P_FLOR : P_PENA;
            }
            float rec = v > 0.65f ? 0.025f : 0f;
            if (a < 0.05f + rec) {
                return b < 0.5f ? P_CUARZO : P_COBRE;
            }
            return -1;
        }
        default:
            return -1;
        }
    }

    public static String nombreBioma(int b) {
        switch (b) {
        case Terreno.BAJIOS:
            return "Bajios";
        case Terreno.BOSQUE:
            return "Bosque de algas";
        case Terreno.MESETA:
            return "Meseta roja";
        case Terreno.CANONES:
            return "Canones";
        case Terreno.ABISMO:
            return "Fosa abisal";
        default:
            return "Vacio";
        }
    }

    /**
     * Draw the chunks and props in view. (fx, fz) is the horizontal view direction; things
     * behind the camera or beyond the fog are skipped.
     */
    void dibujar(Graphics3D g3d, float px, float py, float pz, float fx, float fz, float lejos) {
        trisDibujados = 0;
        chunksDibujados = 0;
        propsDibujados = 0;
        float medio = LADO * 0.71f;
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            float cxm = (c.cx + 0.5f) * LADO;
            float czm = (c.cz + 0.5f) * LADO;
            float dx = cxm - px;
            float dz = czm - pz;
            float d = j2me.FMath.sqrt(dx * dx + dz * dz);
            if (d - medio > lejos) {
                continue;
            }
            if (d > medio && (dx * fx + dz * fz) < -medio) {
                continue; // behind the camera
            }
            t.setIdentity();
            t.postTranslate(c.cx * LADO, c.y0, c.cz * LADO);
            g3d.render(c.terreno, t);
            trisDibujados += c.tris;
            chunksDibujados++;
            for (int k = 0; k < c.n; k++) {
                if (c.quitado[k]) {
                    continue;
                }
                int tipo = c.tipo[k];
                float ox = c.x[k] - px;
                float oz = c.z[k] - pz;
                // drawn up to the fog limit: far props emerge from the murky water instead
                // of popping in
                float lim = lejos + 2;
                float d2 = ox * ox + oz * oz;
                if (d2 > lim * lim) {
                    continue;
                }
                if (d2 > 9 && (ox * fx + oz * fz) < -3) {
                    continue;
                }
                t.setIdentity();
                t.postTranslate(c.x[k], c.y[k], c.z[k]);
                t.postRotate(c.rot[k], 0, 1, 0);
                float e = c.esc[k];
                t.postScale(e, e, e);
                g3d.render(modelos[tipo][c.var[k]], t);
                propsDibujados++;
            }
        }
    }

    /** nearest collectable prop within reach in front of the player; result in sel[] */
    boolean alcanzable(float px, float py, float pz, float fx, float fy, float fz, int[] sel) {
        float mejor = 3.2f;
        boolean hay = false;
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.quitado[k] || DA[c.tipo[k]] < 0) {
                    continue;
                }
                float ox = c.x[k] - px;
                float oy = c.y[k] + 0.6f - py;
                float oz = c.z[k] - pz;
                float d = j2me.FMath.sqrt(ox * ox + oy * oy + oz * oz);
                if (d < mejor && (d < 1.4f || (ox * fx + oy * fy + oz * fz) > d * 0.5f)) {
                    mejor = d;
                    sel[0] = i;
                    sel[1] = k;
                    hay = true;
                }
            }
        }
        return hay;
    }

    /** collision radius of each prop kind at scale 1 (0 = you swim through it) */
    static final float[] RADIO_COL = { 1.0f, 1.0f, 0.5f, 0.7f, 0f, 1.5f, 0.4f, 0f, 1.5f, 3.0f, 0f, 0f, 0f };
    /** height of each prop kind at scale 1 */
    static final float[] ALTO_COL = { 1.2f, 1.4f, 2.0f, 1.8f, 0f, 1.5f, 1.5f, 0f, 1.5f, 20f, 5.5f, 3.0f, 2.2f };
    /**
     * Formations collide through a few circles in their local frame (x, z, radius): the
     * arch only at its two legs (swim under it), the ridge along its line of rocks.
     */
    static final float[] COL_ARCO = { -4.0f, 0, 1.5f, 4.0f, 0, 1.5f };
    static final float[] COL_CRESTA = { -4, 0, 1.2f, -2, 0, 1.6f, 0, 0, 1.8f, 2, 0, 1.6f, 4, 0, 1.2f };

    /** push the point (p[0], p[1], p[2]) out of any solid prop it is inside */
    /**
     * Rock formations of the shallows: about one per chunk (arches, small caves, reef
     * ridges), placed on the chunk as a whole rather than per cell.
     */
    private void colocarFormaciones(Chunk c, float x0, float z0) {
        int cx = c.cx, cz = c.cz;
        for (int intento = 0; intento < 2 && c.n < c.tipo.length; intento++) {
            float a = Terreno.azar(cx * 7 + intento, cz, 201);
            if (a > 0.55f) {
                continue;
            }
            float px = x0 + 8 + Terreno.azar(cx, cz * 3 + intento, 202) * (LADO - 16);
            float pz = z0 + 8 + Terreno.azar(cx * 5 + intento, cz, 203) * (LADO - 16);
            if (Terreno.bioma(px, pz) != Terreno.BAJIOS || px * px + pz * pz < 25 * 25) {
                continue;
            }
            float b = Terreno.azar(cx, cz, 204 + intento);
            int tipo = b < 0.3f ? P_ARCO : (b < 0.6f ? P_CUEVA : P_CRESTA);
            int k = c.n++;
            c.tipo[k] = tipo;
            c.x[k] = px;
            c.z[k] = pz;
            c.y[k] = Terreno.altura(px, pz) - 0.2f;
            c.rot[k] = Terreno.azar(cx, cz, 205 + intento) * 360;
            c.esc[k] = 0.85f + Terreno.azar(cx, cz, 206 + intento) * 0.35f;
            c.var[k] = Terreno.azar(cx, cz, 207 + intento) < 0.5f ? 0 : 1;
            c.quitado[k] = false;
        }
    }

    /** nearest cave within 'max' metres; fills out[0..2] with its centre and returns true */
    boolean cuevaCercana(float px, float pz, float max, float[] out) {
        float mejor = max * max;
        boolean hay = false;
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_CUEVA) {
                    continue;
                }
                float dx = c.x[k] - px, dz = c.z[k] - pz;
                float d2 = dx * dx + dz * dz;
                if (d2 < mejor) {
                    mejor = d2;
                    out[0] = c.x[k];
                    out[1] = c.y[k];
                    out[2] = c.z[k];
                    hay = true;
                }
            }
        }
        return hay;
    }

    /** push a point out of one circle */
    private static void circulo(float[] p, float cx, float cz, float r) {
        float dx = p[0] - cx;
        float dz = p[2] - cz;
        float d2 = dx * dx + dz * dz;
        if (d2 < r * r && d2 > 1e-6f) {
            float d = j2me.FMath.sqrt(d2);
            p[0] = cx + dx / d * r;
            p[2] = cz + dz / d * r;
        }
    }

    private static void formacion(float[] p, Chunk c, int k, float rj) {
        int tipo = c.tipo[k];
        float e = c.esc[k];
        if (p[1] > c.y[k] + ALTO_COL[tipo] * e + rj * 0.5f) {
            return;
        }
        float rot = c.rot[k] * j2me.FMath.RAD;
        float co = j2me.FMath.cos(rot), si = j2me.FMath.sin(rot);
        if (tipo == P_CUEVA) {
            // a wall ring with the mouth open towards local +Z: inside and outside are free
            float dx = p[0] - c.x[k], dz = p[2] - c.z[k];
            float d = j2me.FMath.sqrt(dx * dx + dz * dz);
            float rIn = 3.0f * e, rOut = 3.7f * e;
            if (d < rIn - rj || d > rOut + rj || d < 1e-4f) {
                return;
            }
            // local direction of the point: the mouth spans +-35 degrees around +Z
            float lz = (dx * si + dz * co) / d;
            if (lz > 0.82f) {
                return;
            }
            float r = d < (rIn + rOut) / 2 ? rIn - rj : rOut + rj;
            p[0] = c.x[k] + dx / d * r;
            p[2] = c.z[k] + dz / d * r;
            return;
        }
        float[] col = tipo == P_ARCO ? COL_ARCO : COL_CRESTA;
        for (int i = 0; i < col.length; i += 3) {
            float lx = col[i] * e, lz = col[i + 1] * e;
            circulo(p, c.x[k] + lx * co + lz * si, c.z[k] - lx * si + lz * co, col[i + 2] * e + rj);
        }
    }

    void empujar(float[] p, float radioJugador) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                int tipo = c.tipo[k];
                if (c.quitado[k]) {
                    continue;
                }
                if (tipo >= P_ARCO) {
                    formacion(p, c, k, radioJugador);
                    continue;
                }
                if (RADIO_COL[tipo] == 0) {
                    continue;
                }
                float e = c.esc[k];
                if (p[1] > c.y[k] + ALTO_COL[tipo] * e + radioJugador * 0.5f) {
                    continue;
                }
                float dx = p[0] - c.x[k];
                float dz = p[2] - c.z[k];
                float r = RADIO_COL[tipo] * e + radioJugador;
                float d2 = dx * dx + dz * dz;
                if (d2 < r * r && d2 > 1e-6f) {
                    float d = j2me.FMath.sqrt(d2);
                    p[0] = c.x[k] + dx / d * r;
                    p[2] = c.z[k] + dz / d * r;
                }
            }
        }
    }

    int recoger(int ci, int k) {
        Chunk c = chunks[ci];
        c.quitado[k] = true;
        recogidos.put(c.cx + "," + c.cz + "," + k, Boolean.TRUE);
        return c.tipo[k];
    }
}
