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
    public static final int P_COBRE = 0, P_CUARZO = 1, P_ABANICO = 2, P_TUBOS = 3, P_ALGA = 4, P_ROCA = 5;
    static final int N_TIPOS = 6;
    /** collectable: what a prop gives (index into Inventario), -1 = scenery */
    public static final int[] DA = { Inventario.COBRE, Inventario.CUARZO, Inventario.CORAL, Inventario.CORAL,
            Inventario.FIBRA, Inventario.TITANIO };
    /** props that can only be harvested with the knife */
    public static final boolean[] CUCHILLO = { false, false, true, true, true, false };

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
        int ccx = (int) Math.floor(px / LADO);
        int ccz = (int) Math.floor(pz / LADO);
        for (int i = 0; i < nChunks;) {
            Chunk c = chunks[i];
            if (Math.abs(c.cx - ccx) > RADIO || Math.abs(c.cz - ccz) > RADIO) {
                chunks[i] = chunks[--nChunks];
                chunks[nChunks] = null;
            } else {
                i++;
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
                        chunks[nChunks++] = construir(ccx + dx, ccz + dz);
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

    private Chunk construir(int cx, int cz) {
        long t0 = System.currentTimeMillis();
        Chunk c = new Chunk();
        c.cx = cx;
        c.cz = cz;
        float x0 = cx * LADO;
        float z0 = cz * LADO;
        c.terreno = Terreno.malla(x0, z0, Terreno.CHUNK, fog);
        c.tris = Terreno.CHUNK * Terreno.CHUNK * 2;
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
                int tipo = elegir(bio, a, Terreno.azar(gx, gz, 104));
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
                c.var[k] = (int) (Terreno.azar(gx, gz, 107) * modelos[tipo].length) % modelos[tipo].length;
                c.quitado[k] = recogidos.get(cx + "," + cz + "," + k) != null;
            }
        }
        construidos++;
        long ms = System.currentTimeMillis() - t0;
        if (construidos <= 60 || ms > 60) {
            Log.d("trozo " + cx + "," + cz + " (" + nombreBioma(Terreno.bioma(x0 + LADO / 2, z0 + LADO / 2))
                    + ") listo en " + ms + " ms, " + c.n + " objetos");
        }
        return c;
    }

    /** which prop (if any) grows on a cell of this biome: a = density roll, b = kind roll */
    private static int elegir(int bio, float a, float b) {
        switch (bio) {
        case Terreno.BAJIOS:
            if (a > 0.12f) {
                return -1;
            }
            if (b < 0.28f) {
                return P_TUBOS;
            }
            if (b < 0.56f) {
                return P_ABANICO;
            }
            if (b < 0.76f) {
                return P_COBRE;
            }
            if (b < 0.86f) {
                return P_CUARZO;
            }
            return P_ROCA;
        case Terreno.BOSQUE:
            if (a > 0.16f) {
                return -1;
            }
            if (b < 0.62f) {
                return P_ALGA;
            }
            if (b < 0.8f) {
                return P_COBRE;
            }
            if (b < 0.9f) {
                return P_CUARZO;
            }
            return P_ROCA;
        case Terreno.MESETA:
            if (a > 0.06f) {
                return -1;
            }
            return b < 0.6f ? P_ROCA : (b < 0.85f ? P_CUARZO : P_COBRE);
        default:
            if (a > 0.03f) {
                return -1;
            }
            return P_ROCA;
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
            float d = (float) Math.sqrt(dx * dx + dz * dz);
            if (d - medio > lejos) {
                continue;
            }
            if (d > medio && (dx * fx + dz * fz) < -medio) {
                continue; // behind the camera
            }
            t.setIdentity();
            t.postTranslate(c.cx * LADO, 0, c.cz * LADO);
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
                float d = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
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
    static final float[] RADIO_COL = { 1.0f, 1.0f, 0.5f, 0.7f, 0f, 1.5f };
    /** height of each prop kind at scale 1 */
    static final float[] ALTO_COL = { 1.2f, 1.4f, 2.0f, 1.8f, 0f, 1.5f };

    /** push the point (p[0], p[1], p[2]) out of any solid prop it is inside */
    void empujar(float[] p, float radioJugador) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                int tipo = c.tipo[k];
                if (c.quitado[k] || RADIO_COL[tipo] == 0) {
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
                    float d = (float) Math.sqrt(d2);
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
