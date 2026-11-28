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
    /** chunks are built out to RADIO but only dropped beyond RADIO+1: going back and forth
     *  across a chunk border (the pod sits right on a corner) no longer rebuilds whole rows */
    static final int MAX = (2 * RADIO + 3) * (2 * RADIO + 3);
    /** with more than this many kept, the margin ring is let go */
    static final int GUARDAR = (2 * RADIO + 2) * (2 * RADIO + 2);

    // prop kinds
    public static final int P_COBRE = 0, P_CUARZO = 1, P_ABANICO = 2, P_TUBOS = 3, P_ALGA = 4, P_ROCA = 5,
            P_LANZA = 6, P_FLOR = 7, P_PENA = 8, P_PILAR = 9, P_ARCO = 10, P_CUEVA = 11, P_CRESTA = 12,
            P_VAINAS = 13, P_TUNEL = 14, P_PASTO = 15, P_ESPONJA = 16, P_TUBOG = 17, P_RESTO = 18,
            P_FUMAROLA = 19, P_LIRIO = 20, P_COSTILLAS = 21, P_CORALCENIZA = 22, P_BASALTO = 23,
            P_CRISTALT = 24, P_ANEMONA = 25, P_HIERBA = 26, P_ESPINO = 27, P_OPALO = 28,
            P_HONGO = 29, P_VEJIGA = 30, P_CAJA = 31;
    static final int N_TIPOS = 32;

    /** rock formations and wreckage collide through their own shapes */
    static boolean esFormacion(int t) {
        return t == P_ARCO || t == P_CUEVA || t == P_CRESTA || t == P_TUNEL || t == P_RESTO || t == P_COSTILLAS;
    }
    /** collectable: what a prop gives (index into Inventario), -1 = scenery */
    public static final int[] DA = { Inventario.COBRE, Inventario.CUARZO, Inventario.CORAL, Inventario.CORAL,
            -1, Inventario.TITANIO, -1, -1, -1, -1, -1, -1, -1, Inventario.FIBRA, -1, -1, -1, -1, -1,
            Inventario.URANIO, -1, -1,
            Inventario.AZUFRE, -1,
            Inventario.CRISTAL_T, -1, -1, -1, Inventario.OPALO, Inventario.HONGO, -1, -1 };
    /** props that can only be harvested with the knife */
    public static final boolean[] CUCHILLO = { false, false, true, true, true, false, false, false, false, false,
            false, false, false, true, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false };

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
    /** 0.68..1 from the game's adaptive quality: below 0.85 small far props are skipped */
    float lod = 1f;
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
        /** air bladders: the time (reloj) when each one is full again */
        float[] listo;
    }

    /**
     * Two wreckage sites are guaranteed in every ring zone, at pseudo-random spots inside that
     * zone: the story can be found anywhere, each zone has its own pages, and every site stays
     * put so it can be revisited. Wild wreckage still spawns wherever the prop roll says so.
     */
    private static final int[] ZONA_RESTO = { Terreno.BAJIOS, Terreno.BOSQUE, Terreno.MESETA, Terreno.CANONES,
            Terreno.ABISMO, Terreno.LLANURA };
    private final float[] restoX = new float[ZONA_RESTO.length * 2];
    private final float[] restoZ = new float[ZONA_RESTO.length * 2];
    private int nResto;
    private boolean restosListos;

    private void colocarRestosFijos() {
        int n = 0;
        for (int z = 0; z < ZONA_RESTO.length; z++) {
            int anillo = ZONA_RESTO[z] == Terreno.LLANURA ? 5 : ZONA_RESTO[z];
            float r0 = anillo == 0 ? 30 : Terreno.LIMITE[anillo - 1];
            float r1 = Terreno.LIMITE[anillo];
            for (int k = 0; k < 2; k++) {
                float base = Terreno.azar(z, k, 71) * 360;
                for (int intento = 0; intento < 24; intento++) {
                    float rr = r0 + (r1 - r0) * (0.25f + 0.5f * Terreno.azar(z * 7 + k, intento, 72));
                    float ang = (base + intento * 13) * j2me.FMath.RAD;
                    float x = j2me.FMath.cos(ang) * rr;
                    float zz = j2me.FMath.sin(ang) * rr;
                    if (Terreno.bioma(x, zz) == ZONA_RESTO[z]) {
                        restoX[n] = x;
                        restoZ[n] = zz;
                        n++;
                        break;
                    }
                }
            }
        }
        nResto = n;
    }

    Mundo(Fog fog) {
        this.fog = fog;
        long t0 = System.currentTimeMillis();
        // the world's scenery is made in Blender (assets/modelos: rocas, flora, vejiga, caja,
        // cueva, tunel, resto_estacion); the code-built models are only made if a file is missing
        Mesh[] r = ModeloM3G.partes("/modelos/rocas.m3g", fog, 19, 255, true, Texturas.roca(), 1f / 2.5f);
        Mesh[] f = ModeloM3G.partes("/modelos/flora.m3g", fog, 33, 255, true);
        Mesh[] v = ModeloM3G.partes("/modelos/vejiga.m3g", fog, 2, 220, true);
        Mesh[] cj = ModeloM3G.partes("/modelos/caja.m3g", fog, 1, 255, true, Texturas.metal(), 2f);
        if (r != null) {
            modelos[P_ROCA] = new Mesh[] { r[0], r[1] };
            modelos[P_COBRE] = new Mesh[] { r[2], r[3] };
            modelos[P_CUARZO] = new Mesh[] { r[4] };
            modelos[P_CRESTA] = new Mesh[] { r[5], r[6], r[7] };
            modelos[P_ARCO] = new Mesh[] { r[8], r[9], r[10] };
            modelos[P_PILAR] = new Mesh[] { r[11], r[12] };
            modelos[P_BASALTO] = new Mesh[] { r[13], r[14] };
            modelos[P_FUMAROLA] = new Mesh[] { r[15], r[16] };
            modelos[P_OPALO] = new Mesh[] { r[17] };
            modelos[P_CRISTALT] = new Mesh[] { r[18] };
        } else {
            modelos[P_COBRE] = new Mesh[] { Modelos.rocaVeta(fog, 0x8d8790, 0xd27a3c, false, 3),
                    Modelos.rocaVeta(fog, 0x958d86, 0xd27a3c, false, 5) };
            modelos[P_CUARZO] = new Mesh[] { Modelos.rocaVeta(fog, 0x7f8794, 0xf2d6e8, true, 8) };
            modelos[P_ROCA] = new Mesh[] { roca(fog, 21), roca(fog, 33) };
            modelos[P_PILAR] = new Mesh[] { Modelos.pilarCanon(fog, 5), Modelos.pilarCanon(fog, 12) };
            modelos[P_ARCO] = new Mesh[] { Modelos.arcoPiedra(fog, 1), Modelos.arcoPiedra(fog, 4),
                    Modelos.arcoPiedra(fog, 7) };
            modelos[P_CRESTA] = new Mesh[] { Modelos.crestaArrecife(fog, 1), Modelos.crestaArrecife(fog, 9),
                    Modelos.crestaArrecife(fog, 15) };
            modelos[P_FUMAROLA] = new Mesh[] { Modelos.fumarola(fog, 2), Modelos.fumarola(fog, 9) };
            modelos[P_BASALTO] = new Mesh[] { Modelos.basalto(fog, 2), Modelos.basalto(fog, 7) };
            modelos[P_CRISTALT] = new Mesh[] { Modelos.cristalTermico(fog) };
            modelos[P_OPALO] = new Mesh[] { Modelos.opalo(fog) };
        }
        // big boulders: the same rock, scaled up at placement; scenery, not a resource
        modelos[P_PENA] = modelos[P_ROCA];
        if (f != null) {
            modelos[P_ABANICO] = new Mesh[] { f[0], f[1] };
            modelos[P_TUBOS] = new Mesh[] { f[2], f[3] };
            modelos[P_ALGA] = new Mesh[] { f[4], f[5], f[6] };
            modelos[P_VAINAS] = new Mesh[] { f[7], f[8] };
            modelos[P_LANZA] = new Mesh[] { f[9], f[10] };
            modelos[P_FLOR] = new Mesh[] { f[11], f[12] };
            modelos[P_PASTO] = new Mesh[] { f[13], f[14], f[15] };
            modelos[P_ESPONJA] = new Mesh[] { f[16], f[17] };
            modelos[P_TUBOG] = new Mesh[] { f[18], f[19] };
            modelos[P_LIRIO] = new Mesh[] { f[20], f[21] };
            modelos[P_COSTILLAS] = new Mesh[] { f[22], f[23] };
            modelos[P_CORALCENIZA] = new Mesh[] { f[24], f[25] };
            modelos[P_ANEMONA] = new Mesh[] { f[26], f[27] };
            modelos[P_HIERBA] = new Mesh[] { f[28], f[29] };
            modelos[P_HONGO] = new Mesh[] { f[30] };
            modelos[P_ESPINO] = new Mesh[] { f[31], f[32] };
        } else {
            modelos[P_ABANICO] = new Mesh[] { Modelos.coralAbanico(fog, 1), Modelos.coralAbanico(fog, 6) };
            modelos[P_TUBOS] = new Mesh[] { Modelos.coralTubos(fog, 2), Modelos.coralTubos(fog, 9) };
            modelos[P_ALGA] = new Mesh[] { Modelos.algaCinta(fog, 4), Modelos.algaCinta(fog, 11),
                    Modelos.algaCinta(fog, 17) };
            modelos[P_VAINAS] = new Mesh[] { Modelos.algaCinta(fog, 5, true), Modelos.algaCinta(fog, 13, true) };
            modelos[P_LANZA] = new Mesh[] { Modelos.plantaLanza(fog, 3), Modelos.plantaLanza(fog, 8) };
            modelos[P_FLOR] = new Mesh[] { Modelos.florAbisal(fog, 2), Modelos.florAbisal(fog, 7) };
            modelos[P_PASTO] = new Mesh[] { Modelos.pastoMarino(fog, 1), Modelos.pastoMarino(fog, 5),
                    Modelos.pastoMarino(fog, 9) };
            modelos[P_ESPONJA] = new Mesh[] { Modelos.esponja(fog, 1), Modelos.esponja(fog, 4) };
            modelos[P_TUBOG] = new Mesh[] { Modelos.tuboGigante(fog, 2), Modelos.tuboGigante(fog, 7) };
            modelos[P_LIRIO] = new Mesh[] { Modelos.lirioDuna(fog, 1), Modelos.lirioDuna(fog, 6) };
            modelos[P_COSTILLAS] = new Mesh[] { Modelos.costillas(fog, 1), Modelos.costillas(fog, 4) };
            modelos[P_CORALCENIZA] = new Mesh[] { Modelos.coralCeniza(fog, 3), Modelos.coralCeniza(fog, 8) };
            modelos[P_ANEMONA] = new Mesh[] { Modelos.anemona(fog, 1), Modelos.anemona(fog, 2) };
            modelos[P_HIERBA] = new Mesh[] { Modelos.hierbaAlta(fog, 1), Modelos.hierbaAlta(fog, 4) };
            modelos[P_HONGO] = new Mesh[] { Modelos.hongo(fog) };
            modelos[P_ESPINO] = new Mesh[] { Modelos.espinoUrticante(fog, 1), Modelos.espinoUrticante(fog, 5) };
        }
        // caves, tunnels and the station wreckage: thick walls (they were paper-thin sheets)
        Mesh[] m3 = ModeloM3G.partes("/modelos/cueva.m3g", fog, 3, 255, true, Texturas.roca(), 1f / 2.5f);
        modelos[P_CUEVA] = m3 != null ? m3 : new Mesh[] { Modelos.cueva(fog, 1), Modelos.cueva(fog, 6), Modelos.cueva(fog, 11) };
        m3 = ModeloM3G.partes("/modelos/tunel.m3g", fog, 2, 255, true, Texturas.roca(), 1f / 2.5f);
        modelos[P_TUNEL] = m3 != null ? m3 : new Mesh[] { Modelos.tunel(fog, 1), Modelos.tunel(fog, 8) };
        m3 = ModeloM3G.partes("/modelos/resto_estacion.m3g", fog, 2, 255, true, Texturas.metal(), 0.5f);
        modelos[P_RESTO] = m3 != null ? m3 : new Mesh[] { Modelos.restoEstacion(fog, 1), Modelos.restoEstacion(fog, 3) };
        modelos[P_VEJIGA] = v != null ? v : new Mesh[] { Modelos.vejigaAire(fog, 1), Modelos.vejigaAire(fog, 6) };
        modelos[P_CAJA] = cj != null ? cj : new Mesh[] { Modelos.cajaSuministros(fog) };
        pluma = Modelos.pluma(fog);
        burbujas = Modelos.burbujasFumarola(fog);
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
            int dist = Math.max(Math.abs(c.cx - ccx), Math.abs(c.cz - ccz));
            if (dist > RADIO + 1 || (dist > RADIO && nChunks > GUARDAR)) {
                if (c.terreno != null) {
                    c.terreno.getVertexBuffer().liberar(); // its render cache goes with it
                }
                chunks[i] = chunks[--nChunks];
                chunks[nChunks] = null;
                cercaSucia = true;
            } else {
                i++;
            }
        }
        // a chunk under construction advances a few milliseconds of work per frame, row by
        // row, whatever phase it is in (a whole chunk costs ~90 ms on a real PSP)
        if (pend != null) {
            if (Math.abs(pend.cx - ccx) > RADIO + 1 || Math.abs(pend.cz - ccz) > RADIO + 1) {
                pend = null; // walked away before it was finished
            } else {
                avanzar();
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
                        Chunk c = new Chunk();
                        c.cx = ccx + dx;
                        c.cz = ccz + dz;
                        c.t0 = System.currentTimeMillis();
                        float x0 = c.cx * LADO, z0 = c.cz * LADO;
                        c.y0 = Terreno.altura(x0 + LADO / 2, z0 + LADO / 2);
                        if (pendHBuf == null) {
                            pendHBuf = new float[(Terreno.CHUNK + 1) * (Terreno.CHUNK + 1)];
                        }
                        pendH = pendHBuf;
                        pend = c;
                        pendFase = 0;
                        pendFila = 0;
                        avanzar();
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
        int antes = presupuesto;
        presupuesto = 1000000;
        while (actualizar(px, pz)) {
        }
        presupuesto = antes;
        Log.d("mundo inicial: " + nChunks + " trozos en " + (System.currentTimeMillis() - t0) + " ms; "
                + Log.memoria());
    }

    /** chunk being built across several frames */
    private Chunk pend;
    private int pendFase;
    private int pendFila;
    private float[] pendH;
    private MB pendMB;
    /** one terrain builder reused by every chunk: no big arrays to grow and discard */
    private MB mbTerreno;
    private float[] pendHBuf;
    /** milliseconds of chunk work per frame (unlimited on the loading screen) */
    int presupuesto = 4;

    /**
     * Advance the chunk under construction until the frame's budget is spent. Phases:
     * 0 height rows, 1 mesh rows, 2 upload the mesh, 3 prop rows, then it is done.
     */
    private void avanzar() {
        int fa = Log.fase(Log.F_TROZO);
        // if the previous slice overran, give the next frames a rest: a slow step must not be
        // followed by more work that keeps the frame long
        if (esperaTrozo > 0) {
            esperaTrozo--;
            Log.fase(fa);
            return;
        }
        long inicio = System.currentTimeMillis();
        long fin = inicio + presupuesto;
        int n = Terreno.CHUNK;
        int guardia = 0; // safety: never spin forever if the clock or a phase misbehaves
        do {
            long tp = System.currentTimeMillis();
            int fasePaso = pendFase;
            pasoTrozo();
            long dur = System.currentTimeMillis() - tp;
            if (dur > pasoMax[fasePaso]) {
                pasoMax[fasePaso] = dur;
            }
            guardia++;
        } while (pend != null && guardia < 8 && System.currentTimeMillis() < fin);
        if (System.currentTimeMillis() - inicio > 25) {
            esperaTrozo = 2; // this slice was already a hitch: skip the next couple of frames
        }
        Log.fase(fa);
    }

    /** frames to wait before touching chunks again (set after a slice that ran long) */
    private int esperaTrozo;

    /** longest single step of chunk building per phase (ms), for the devlog; reset by the reader */
    final long[] pasoMax = new long[4];

    private void pasoTrozo() {
        int n = Terreno.CHUNK;
        {
            Chunk c = pend;
            float x0 = c.cx * LADO, z0 = c.cz * LADO;
            if (pendFase == 0) {
                Terreno.filaAlturas(pendH, x0, z0, n, c.y0, pendFila++);
                if (pendFila > n) {
                    pendFase = 1;
                    pendFila = 0;
                    if (mbTerreno == null) {
                        mbTerreno = Terreno.nuevoMB();
                    }
                    mbTerreno.reiniciar();
                    pendMB = mbTerreno;
                }
            } else if (pendFase == 1) {
                Terreno.filaMalla(pendMB, pendH, x0, z0, n, pendFila++);
                if (pendFila >= n) {
                    pendFase = 2;
                    pendMB.empezarPasos();
                }
            } else if (pendFase == 2) {
                // converting the mesh for the GPU is split into slices across frames
                Mesh m = pendMB.paso(fog, 255, false, 0);
                if (m == null) {
                    return;
                }
                c.terreno = m;
                c.tris = n * n * 2;
                pendMB = null;
                pendH = null;
                prepararObjetos(c);
                pendFase = 3;
                pendFila = 0;
            } else {
                filaObjetos(c, pendFila++);
                if (pendFila >= n) {
                    terminar(c);
                    chunks[nChunks++] = c;
                    pend = null;
                    cercaSucia = true;
                }
            }
        }
    }

    /** props: arrays for one candidate per 4 m cell (at most 56, so the denser flora fits) */
    private void prepararObjetos(Chunk c) {
        int cap = 44;
        c.tipo = new int[cap];
        c.x = new float[cap];
        c.y = new float[cap];
        c.z = new float[cap];
        c.rot = new float[cap];
        c.esc = new float[cap];
        c.var = new int[cap];
        c.quitado = new boolean[cap];
        c.listo = new float[cap];
    }

    /** minimum distance between two props of a chunk, so nothing grows in heaps */
    static final float SEPARACION = 5.5f;

    private boolean libre(Chunk c, float px, float pz) {
        for (int q = 0; q < c.n; q++) {
            float dx = c.x[q] - px, dz = c.z[q] - pz;
            if (dx * dx + dz * dz < SEPARACION * SEPARACION) {
                return false;
            }
        }
        return true;
    }

    /** the props of one row of cells */
    private void filaObjetos(Chunk c, int j) {
        int cap = c.tipo.length;
        int cx = c.cx, cz = c.cz;
        int gx0 = cx * Terreno.CHUNK;
        int gz0 = cz * Terreno.CHUNK;
        // the harvested-state keys share one prefix per chunk: only one String per prop
        String prefijo = recogidos.isEmpty() ? null : (cx + "," + cz + ",");
            for (int i = 0; i < Terreno.CHUNK && c.n < cap; i++) {
                int gx = gx0 + i;
                int gz = gz0 + j;
                float a = Terreno.azar(gx, gz, 101);
                float px = (gx + 0.2f + 0.6f * Terreno.azar(gx, gz, 102)) * Terreno.CELDA;
                float pz = (gz + 0.2f + 0.6f * Terreno.azar(gx, gz, 103)) * Terreno.CELDA;
                int bio = Terreno.bioma(px, pz);
                int tipo = elegir(bio, a, Terreno.azar(gx, gz, 104), veta(px, pz),
                        Terreno.fbm(px * 0.02f, pz * 0.02f, 61));
                if (tipo < 0 || !libre(c, px, pz)) {
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
                c.y[k] = Terreno.alturaMalla(px, pz) - 0.15f; // on the drawn ground
                c.rot[k] = Terreno.azar(gx, gz, 105) * 360;
                c.esc[k] = 0.8f + Terreno.azar(gx, gz, 106) * 0.5f;
                if (tipo == P_PENA) {
                    c.esc[k] *= 2.4f;
                } else if (tipo == P_ROCA) {
                    // titanium outcrop: a small rock
                    c.esc[k] *= 0.28f;
                } else if (tipo == P_COBRE) {
                    c.esc[k] *= 0.5f;
                } else if (tipo == P_CUARZO) {
                    // quartz: the smallest of the ore rocks
                    c.esc[k] *= 0.35f;
                }
                if (DA[tipo] >= 0 && !CUCHILLO[tipo]) {
                    c.recursos++;
                }
                c.var[k] = (int) (Terreno.azar(gx, gz, 107) * modelos[tipo].length) % modelos[tipo].length;
                // only build the key string when something was ever harvested
                c.quitado[k] = prefijo != null && recogidos.get(prefijo + k) != null;
            }
    }

    /**
     * The starter kit, and only that: one quartz and one titanium ~15 m from the pod (just
     * outside the clear spawn ring) so the knife, which everything else needs, is always
     * reachable. Every other outcrop in the world is the normal random vein placement.
     */
    static final float[] INICIO_X = { 14, -13 };
    static final float[] INICIO_Z = { 7, -9 };
    static final int[] INICIO_T = { P_CUARZO, P_ROCA };

    /** last step: rock formations and bookkeeping */
    private Chunk terminar(Chunk c) {
        if (!restosListos) {
            restosListos = true;
            colocarRestosFijos();
        }
        int faseAntes = Log.fase(Log.F_TROZO);
        long t0 = c.t0;
        int cx = c.cx, cz = c.cz;
        float x0 = cx * LADO;
        float z0 = cz * LADO;
        // one key prefix per chunk instead of building a String for every prop
        String prefijo = recogidos.isEmpty() ? null : (cx + "," + cz + ",");
        for (int q = 0; q < INICIO_X.length; q++) {
            float qx = INICIO_X[q], qz = INICIO_Z[q];
            if (qx >= x0 && qx < x0 + LADO && qz >= z0 && qz < z0 + LADO && c.n < c.tipo.length) {
                int tipo = INICIO_T[q];
                int k = c.n++;
                c.tipo[k] = tipo;
                c.x[k] = qx;
                c.z[k] = qz;
                c.y[k] = Terreno.alturaMalla(qx, qz) - 0.15f;
                c.rot[k] = q * 70;
                c.esc[k] = 1; // a touch bigger than the wild ones so they are easy to spot
                c.var[k] = 0;
                c.recursos++;
                c.quitado[k] = prefijo != null && recogidos.get(prefijo + k) != null;
            }
        }
        // the two guaranteed wreckage sites of each zone, if their chunk is being built
        for (int q = 0; q < nResto; q++) {
            float qx = restoX[q], qz = restoZ[q];
            if (qx >= x0 && qx < x0 + LADO && qz >= z0 && qz < z0 + LADO && c.n < c.tipo.length) {
                int k = c.n++;
                c.tipo[k] = P_RESTO;
                c.x[k] = qx;
                c.z[k] = qz;
                c.y[k] = Terreno.alturaMalla(qx, qz) - 0.15f;
                c.rot[k] = q * 47;
                c.esc[k] = 1;
                c.var[k] = 0;
                c.recursos++;
                c.quitado[k] = prefijo != null && recogidos.get(prefijo + k) != null;
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
        // supply crates from the station: extremely rare now
        if (a > 0.7f && a < 0.7001f && bio != Terreno.VACIO) {
            return P_CAJA;
        }
        // air bladders: rare plants holding a pocket of breathable air (kelp, plateau, deep)
        if (a > 0.6f && a < 0.6045f && (bio == Terreno.BOSQUE || bio == Terreno.MESETA || bio == Terreno.CANONES
                || bio == Terreno.ABISMO)) {
            return P_VEJIGA;
        }
        switch (bio) {
        case Terreno.BAJIOS: {
            // open sand with moss patches; corals grow in reef patches
            float coral = arrecife > 0.6f ? 0.08f : 0.01f;
            if (a < coral) {
                return b < 0.36f ? P_TUBOS : (b < 0.7f ? P_ABANICO : (b < 0.82f ? P_ANEMONA : (b < 0.94f ? P_ESPONJA : P_PENA)));
            }
            if (arrecife > 0.55f && a < coral + 0.008f) {
                return P_ESPINO;
            }
            // seagrass in the open sand, away from the reefs
            float pasto = arrecife < 0.45f ? 0.04f : 0f;
            if (a < coral + pasto) {
                return P_PASTO;
            }
            coral += pasto;
            float rec = v > 0.62f ? 0.008f : 0.002f;
            if (a < coral + rec) {
                return b < 0.34f ? P_ROCA : (b < 0.64f ? P_COBRE : P_CUARZO);
            }
            return -1;
        }
        case Terreno.BOSQUE: {
            // the kelp grows in groves, with clearings between them
            float alga = arrecife > 0.5f ? 0.14f : 0.04f;
            // clearings between the groves: patches of tall grass, and stinging thorns
            if (arrecife < 0.3f && a < 0.05f) {
                return P_HIERBA;
            }
            if (arrecife < 0.4f && a < 0.058f) {
                return P_ESPINO;
            }
            if (a < alga) {
                // only a few kelps carry harvestable seed pods
                return b < 0.08f ? P_VAINAS : (b < 0.86f ? P_ALGA : (b < 0.93f ? P_ESPONJA : P_PENA));
            }
            float rec = v > 0.62f ? 0.014f : 0.003f;
            if (a < alga + rec) {
                return b < 0.33f ? P_ROCA : (b < 0.63f ? P_COBRE : P_CUARZO);
            }
            return -1;
        }
        case Terreno.MESETA: {
            float lanza = arrecife > 0.58f ? 0.08f : 0.03f;
            if (a < lanza) {
                return b < 0.5f ? P_LANZA : (b < 0.68f ? P_TUBOG : (b < 0.8f ? P_ESPONJA : (b < 0.9f ? P_ANEMONA : P_PENA)));
            }
            float rec = v > 0.6f ? 0.016f : 0.003f;
            if (a < lanza + rec) {
                return b < 0.4f ? P_CUARZO : (b < 0.7f ? P_COBRE : P_ROCA);
            }
            return -1;
        }
        case Terreno.CANONES:
        case Terreno.ABISMO: {
            if (a < 0.13f) {
                if (bio == Terreno.CANONES && b > 0.88f) {
                    return P_PILAR;
                }
                return b < 0.55f ? P_FLOR : (b < 0.72f ? P_TUBOG : (b < 0.85f ? P_ESPONJA : P_PENA));
            }
            float rec = v > 0.65f ? 0.014f : 0f;
            if (a < 0.13f + rec) {
                return b < 0.5f ? P_CUARZO : P_COBRE;
            }
            return -1;
        }
        case Terreno.LLANURA: {
            // the outer shelf is barren: pale rock, ore veins and wreckage the crater spat out
            float rec = v > 0.62f ? 0.03f : 0.003f;
            if (a < rec) {
                return b < 0.45f ? P_ROCA : (b < 0.75f ? P_CUARZO : P_OPALO);
            }
            float forma = arrecife > 0.7f ? 0.012f : 0.004f;
            if (a < rec + forma) {
                return b < 0.5f ? P_PENA : (b < 0.8f ? P_RESTO : P_CRESTA);
            }
            return -1;
        }
        case Terreno.PRADERA: {
            // wide seagrass meadows on the dunes, a few sponges and the odd outcrop
            // tall grass grows in dense patches where the noise is high: lurkers hide in it
            float hierba = arrecife > 0.62f ? 0.12f : 0.01f;
            if (a < hierba) {
                return P_HIERBA;
            }
            a -= hierba;
            if (a < 0.006f) {
                return P_ESPINO;
            }
            float pasto = arrecife > 0.45f ? 0.07f : 0.02f;
            if (a < pasto) {
                return b < 0.7f ? P_PASTO : (b < 0.86f ? P_LIRIO : (b < 0.94f ? P_ANEMONA : P_ESPONJA));
            }
            float rec = v > 0.6f ? 0.02f : 0.0015f;
            if (a < pasto + rec) {
                return b < 0.35f ? P_ROCA : (b < 0.62f ? P_COBRE : P_CUARZO);
            }
            return -1;
        }
        case Terreno.TERMAL: {
            // hot vents in clusters along the rifts, little else grows
            float vent = arrecife > 0.55f ? 0.035f : 0.01f;
            if (a < vent) {
                return b < 0.4f ? P_FUMAROLA : (b < 0.75f ? P_CORALCENIZA : (b < 0.92f ? P_BASALTO : P_PENA));
            }
            float rec = v > 0.6f ? 0.03f : 0.002f;
            if (a < vent + rec) {
                return b < 0.45f ? P_CRISTALT : (b < 0.75f ? P_ROCA : P_CUARZO);
            }
            return -1;
        }
        default:
            return -1;
        }
    }

    static final float VEJIGA_T = 90;

    /**
     * An air bladder within reach of (x, y, z): returns its chunk and index in sel, or
     * false. Full or not, so the caller can say how long it needs to refill.
     */
    boolean vejigaCerca(float x, float y, float z, int[] sel) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_VEJIGA) {
                    continue;
                }
                float dx = x - c.x[k], dz = z - c.z[k], dy = y - (c.y[k] + 1.0f * c.esc[k]);
                if (dx * dx + dy * dy + dz * dz < 2.6f * 2.6f) {
                    sel[0] = i;
                    sel[1] = k;
                    return true;
                }
            }
        }
        return false;
    }

    /** seconds until the bladder is full again (0 = full) */
    float vejigaFalta(int ci, int k) {
        return Math.max(0, chunks[ci].listo[k] - reloj);
    }

    void vaciarVejiga(int ci, int k) {
        chunks[ci].listo[k] = reloj + VEJIGA_T;
    }

    /** true if (x, y, z) brushes against a stinging thorn */
    boolean cercaEspino(float x, float y, float z) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_ESPINO) {
                    continue;
                }
                float dx = x - c.x[k], dz = z - c.z[k], dy = y - c.y[k];
                float r = 1.1f * c.esc[k];
                if (dx * dx + dz * dz < r * r && dy < 1.8f * c.esc[k] && dy > -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /** true if an anemone is close enough to lean on: its polyps slowly close the wounds */
    boolean cercaAnemona(float x, float y, float z) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_ANEMONA) {
                    continue;
                }
                float dx = x - c.x[k], dz = z - c.z[k], dy = y - c.y[k];
                float r = 1.4f * c.esc[k];
                if (dx * dx + dz * dz < r * r && dy < 2.0f * c.esc[k] && dy > -1.2f) {
                    return true;
                }
            }
        }
        return false;
    }

    /** true if (x, y, z) is in the scalding plume just above a hot vent */
    boolean cercaFumarola(float x, float y, float z) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_FUMAROLA) {
                    continue;
                }
                float dx = x - c.x[k], dz = z - c.z[k];
                float dy = y - c.y[k];
                float top = 4.5f * c.esc[k];
                if (dy > top - 1 && dy < top + 6 && dx * dx + dz * dz < 2.2f * 2.2f) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String nombreBioma(int b) {
        switch (b) {
        case Terreno.BAJIOS:
            return "Bajíos";
        case Terreno.BOSQUE:
            return "Bosque de algas";
        case Terreno.MESETA:
            return "Meseta roja";
        case Terreno.CANONES:
            return "Cañones";
        case Terreno.ABISMO:
            return "Fosa abisal";
        case Terreno.PRADERA:
            return "Praderas de arena";
        case Terreno.TERMAL:
            return "Grietas termales";
        case Terreno.LLANURA:
            return "Llanura exterior";
        default:
            return "Vacío";
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
                // big props are judged by their size, not their centre: an arch or a tunnel
                // half in view must not vanish
                float tam = TAM[tipo] * c.esc[k];
                float lim = lejos + 2 + tam;
                float d2 = ox * ox + oz * oz;
                if (d2 > lim * lim) {
                    continue;
                }
                if (d2 > 9 + tam * tam && (ox * fx + oz * fz) < -3 - tam) {
                    continue;
                }
                t.setIdentity();
                t.postTranslate(c.x[k], c.y[k], c.z[k]);
                if (MECE[tipo] > 0 && d2 < 40 * 40) {
                    // plants sway with the current (each with its own phase)
                    float fase = reloj * 1.1f + c.x[k] * 0.37f + c.z[k] * 0.23f;
                    t.postRotate(j2me.FMath.sin(fase) * MECE[tipo], 1, 0, 0);
                    t.postRotate(j2me.FMath.sin(fase * 0.7f + 1) * MECE[tipo] * 0.6f, 0, 0, 1);
                }
                t.postRotate(c.rot[k], 0, 1, 0);
                float e = c.esc[k];
                if (tipo == P_VEJIGA) {
                    // the sacs breathe slowly; empty ones hang limp until they refill
                    float lleno = reloj >= c.listo[k] ? 1 : 0.45f + 0.55f * (1 - (c.listo[k] - reloj) / VEJIGA_T);
                    float r = 1 + 0.06f * j2me.FMath.sin(reloj * 1.7f + k);
                    t.postScale(e * (0.7f + 0.3f * lleno) * r, e * lleno, e * (0.7f + 0.3f * lleno) * r);
                    if (lleno >= 1 && d2 < 22 * 22 && ((int) (reloj * 2) + k) % 3 == 0) {
                        g3d.render(modelos[tipo][c.var[k]], t);
                        t.setIdentity();
                        t.postTranslate(c.x[k], c.y[k] + 1.6f * e + (reloj * 1.2f + k) % 3f, c.z[k]);
                        g3d.render(burbujas, t);
                        propsDibujados++;
                        continue;
                    }
                } else {
                    t.postScale(e, e, e);
                }
                g3d.render(modelos[tipo][c.var[k]], t);
                propsDibujados++;
                if (tipo == P_FUMAROLA && d2 < 34 * 34) {
                    // smoke column from the throat, slowly turning and breathing
                    t.postTranslate(0, 4.4f, 0);
                    t.postRotate(reloj * 25 + k * 50, 0, 1, 0);
                    float sp = 1 + 0.08f * j2me.FMath.sin(reloj * 1.3f + k);
                    t.postScale(sp, 1, sp);
                    g3d.render(pluma, t);
                    // two bubble streams half a cycle apart rise through it forever
                    if (d2 < 24 * 24) {
                        for (int q = 0; q < 2; q++) {
                            float sube = ((reloj * 1.4f + q * 3 + k) % 6f);
                            t.setIdentity();
                            t.postTranslate(c.x[k], c.y[k] + 4.4f * e + sube, c.z[k]);
                            t.postRotate(q * 90 + k * 30, 0, 1, 0);
                            g3d.render(burbujas, t);
                        }
                    }
                }
            }
        }
    }

    /** seconds, for animated scenery (the vent plumes) */
    float reloj;
    private final Mesh pluma, burbujas;

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
                if (c.tipo[k] == P_VAINAS) {
                    // tall kelp: the pods hang 6-10 m up the stalk, so reach the nearest point of it
                    float e = c.esc[k];
                    float rel = Math.max(0.6f, Math.min(10.5f * e, py - c.y[k]));
                    oy = c.y[k] + rel - py;
                }
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

    /** rough radius of each prop kind (m, scale 1), for view culling */
    static final float[] TAM = new float[N_TIPOS];

    /** how much each prop kind sways with the current (degrees; 0 = rigid) */
    static final float[] MECE = new float[N_TIPOS];
    static {
        for (int i = 0; i < N_TIPOS; i++) {
            TAM[i] = 2;
        }
        TAM[P_ARCO] = 8;
        TAM[P_CUEVA] = 6;
        TAM[P_CRESTA] = 7;
        TAM[P_TUNEL] = 16;
        TAM[P_COSTILLAS] = 12;
        TAM[P_RESTO] = 5;
        TAM[P_PILAR] = 6;
        TAM[P_PENA] = 3;
        TAM[P_BASALTO] = 3;
        TAM[P_TUBOG] = 3;
        TAM[P_FUMAROLA] = 2;
        // dune lilies can be picked by hand: they give their fruit
        DA[P_LIRIO] = Inventario.FRUTO;
        MECE[P_ALGA] = 3;
        MECE[P_VAINAS] = 3;
        MECE[P_PASTO] = 7;
        MECE[P_HIERBA] = 6;
        MECE[P_LIRIO] = 5;
        MECE[P_LANZA] = 2;
        MECE[P_ANEMONA] = 6;
        MECE[P_ABANICO] = 2;
        MECE[P_ESPINO] = 4;
    }

    /** collision radius of each prop kind at scale 1 (0 = you swim through it) */
    static final float[] RADIO_COL = { 1.0f, 1.0f, 0.5f, 0.7f, 0f, 1.5f, 0.4f, 0f, 1.5f, 3.2f, 0f, 0f, 0f, 0f, 0f, 0f, 0.6f, 1.6f, 0f, 0.9f, 0f, 0f, 0.5f, 1.7f, 0.7f, 0f, 0f, 0f, 0.4f, 0f, 0.5f, 0.6f };
    /** height of each prop kind at scale 1 */
    static final float[] ALTO_COL = { 1.2f, 1.4f, 2.0f, 1.8f, 0f, 1.5f, 1.5f, 0f, 1.8f, 20f, 5.5f, 3.8f, 2.4f, 0f, 3.4f, 0f, 1.1f, 4f, 2.5f, 4.5f, 2.5f, 5.5f, 2.0f, 5.0f, 1.0f, 0f, 0f, 0f, 0.5f, 0f, 1.0f, 0.8f };
    /**
     * Formations collide through a few circles in their local frame (x, z, radius): the
     * arch only at its two legs (swim under it), the ridge along its line of rocks.
     */
    static final float[] COL_ARCO = { -4.0f, 0, 1.9f, 4.0f, 0, 1.9f };
    static final float[] COL_RESTO = { -1.8f, 0, 1.2f, 1.6f, 0.5f, 1.2f };
    /** fossil ribcage: where each rib meets the sand (both sides) and the skull */
    static final float[] COL_COSTILLAS = { -5.0f, 3.4f, 0.5f, -5.0f, -3.4f, 0.5f, -2.3f, 3.6f, 0.5f, -2.3f, -3.6f, 0.5f,
            0.4f, 3.7f, 0.5f, 0.4f, -3.7f, 0.5f, 3.0f, 3.5f, 0.5f, 3.0f, -3.5f, 0.5f, 5.7f, 3.3f, 0.5f, 5.7f, -3.3f, 0.5f,
            8.8f, 0, 1.6f };
    static final float[] COL_CRESTA = { -4, 0, 1.6f, -2, 0, 2.0f, 0, 0, 2.2f, 2, 0, 2.0f, 4, 0, 1.6f };

    /** push the point (p[0], p[1], p[2]) out of any solid prop it is inside */
    /**
     * Rock formations of the shallows: about one per chunk (arches, small caves, reef
     * ridges), placed on the chunk as a whole rather than per cell.
     */
    private void colocarFormaciones(Chunk c, float x0, float z0) {
        int cx = c.cx, cz = c.cz;
        // at most one formation per chunk, kept away from the chunk edges so two of them
        // never stand side by side
        for (int intento = 0; intento < 1 && c.n < c.tipo.length; intento++) {
            float a = Terreno.azar(cx * 7 + intento, cz, 201);
            if (a > 0.6f) {
                continue;
            }
            float px = x0 + 14 + Terreno.azar(cx, cz * 3 + intento, 202) * (LADO - 28);
            float pz = z0 + 14 + Terreno.azar(cx * 5 + intento, cz, 203) * (LADO - 28);
            int bioF = Terreno.bioma(px, pz);
            if ((bioF != Terreno.BAJIOS && bioF != Terreno.BOSQUE && bioF != Terreno.PRADERA
                    && bioF != Terreno.MESETA && bioF != Terreno.CANONES) || px * px + pz * pz < 25 * 25) {
                continue;
            }
            float b = Terreno.azar(cx, cz, 204 + intento);
            int tipo;
            if (bioF == Terreno.MESETA) {
                // the red plateau: wreckage of the station that drifted down here, and caves
                // and tunnels in its rock
                if (b > 0.55f) {
                    continue;
                }
                tipo = b < 0.25f ? P_RESTO : (b < 0.42f ? P_CUEVA : P_TUNEL);
            } else if (bioF == Terreno.CANONES) {
                // the canyons: caves and tunnels in the walls (spitters live in them)
                if (b > 0.5f) {
                    continue;
                }
                tipo = b < 0.28f ? P_CUEVA : P_TUNEL;
            } else if (bioF == Terreno.PRADERA) {
                // the meadows: the odd fossil ribcage, rock arches, some wreckage (kept rare:
                // a meadow full of bones stopped feeling like a meadow)
                if (b > 0.4f) {
                    continue;
                }
                tipo = b < 0.1f ? P_COSTILLAS : (b < 0.3f ? P_ARCO : P_RESTO);
            } else if (bioF == Terreno.BOSQUE) {
                // the kelp forest only gets wreckage of the station, and not often
                if (b > 0.35f) {
                    continue;
                }
                tipo = P_RESTO;
            } else {
                tipo = b < 0.15f ? P_ARCO : (b < 0.38f ? P_CUEVA : (b < 0.66f ? P_TUNEL : (b < 0.84f ? P_CRESTA : P_RESTO)));
            }
            float rot = Terreno.azar(cx, cz, 205 + intento) * 360;
            float esc = 0.85f + Terreno.azar(cx, cz, 206 + intento) * 0.35f;
            float y = Terreno.alturaMalla(px, pz) - 0.2f;
            if (tipo == P_TUNEL) {
                // a tunnel only where the floor along its arc is nearly flat, and resting on
                // its lowest point (never floating over a hill)
                float co = j2me.FMath.cos(rot * j2me.FMath.RAD), si = j2me.FMath.sin(rot * j2me.FMath.RAD);
                float hmin = y, hmax = y;
                for (int q = -2; q <= 2; q++) {
                    float th = q * 32 * j2me.FMath.RAD;
                    float lx = Modelos.TUNEL_R * j2me.FMath.sin(th) * esc;
                    float lz = (Modelos.TUNEL_R - Modelos.TUNEL_R * j2me.FMath.cos(th)) * esc;
                    float h = Terreno.altura(px + lx * co + lz * si, pz - lx * si + lz * co);
                    hmin = Math.min(hmin, h);
                    hmax = Math.max(hmax, h);
                }
                if (hmax - hmin > 2.0f) {
                    tipo = P_CUEVA; // too uneven for a tunnel: a cave instead
                } else {
                    // sit it on the average floor: on the highest point one mouth touched the
                    // sand and the other end hung in the air
                    y = (hmin + hmax) * 0.5f + 0.4f;
                }
            }
            if (tipo == P_CUEVA) {
                // sample the floor all around the dome's base: only place it where that ring
                // is nearly flat, so it never floats over a hollow or sinks under a mound
                float rr = 3.2f * esc;
                float sum = 0, hminC = 1e9f, hmaxC = -1e9f;
                for (int q = 0; q < 8; q++) {
                    float ang = q * 0.7854f;
                    float h = Terreno.altura(px + j2me.FMath.cos(ang) * rr, pz + j2me.FMath.sin(ang) * rr);
                    hminC = Math.min(hminC, h);
                    hmaxC = Math.max(hmaxC, h);
                    sum += h;
                }
                if (hmaxC - hminC > 1.6f) {
                    continue; // too uneven: no cave here
                }
                y = sum * 0.125f - 0.3f;
            }
            int k = c.n++;
            c.tipo[k] = tipo;
            c.x[k] = px;
            c.z[k] = pz;
            c.y[k] = y;
            c.rot[k] = rot;
            c.esc[k] = esc;
            c.var[k] = (int) (Terreno.azar(cx, cz, 207 + intento) * 3) % modelos[tipo].length;
            c.quitado[k] = false;
            // cave mushrooms grow in half of the caves and tunnels
            if ((tipo == P_CUEVA || tipo == P_TUNEL) && Terreno.azar(cx, cz, 209) < 0.5f && c.n < c.tipo.length) {
                int o = c.n++;
                c.tipo[o] = P_HONGO;
                c.x[o] = px + 1.2f;
                c.z[o] = pz + 0.8f;
                c.y[o] = Terreno.alturaMalla(px + 1.2f, pz + 0.8f) - 0.05f;
                c.rot[o] = rot;
                c.esc[o] = 1.3f;
                c.var[o] = 0;
                c.quitado[o] = !recogidos.isEmpty() && recogidos.get(cx + "," + cz + "," + o) != null;
            }
            // a rare cave opal inside some caves and tunnels
            if ((tipo == P_CUEVA || tipo == P_TUNEL) && Terreno.azar(cx, cz, 208) < 0.35f && c.n < c.tipo.length) {
                int o = c.n++;
                c.tipo[o] = P_OPALO;
                c.x[o] = px;
                c.z[o] = pz;
                c.y[o] = Terreno.alturaMalla(px, pz) - 0.1f;
                c.rot[o] = rot;
                c.esc[o] = 1;
                c.var[o] = 0;
                c.quitado[o] = !recogidos.isEmpty() && recogidos.get(cx + "," + cz + "," + o) != null;
            }
        }
    }

    /** key of a piece of station wreckage within 'max' metres, or null */
    /** an unopened supply crate within reach: chunk and index in sel */
    boolean cajaCerca(float x, float y, float z, int[] sel) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_CAJA || c.quitado[k]) {
                    continue;
                }
                float dx = x - c.x[k], dy = y - (c.y[k] + 0.5f), dz = z - c.z[k];
                if (dx * dx + dy * dy + dz * dz < 2.8f * 2.8f) {
                    sel[0] = i;
                    sel[1] = k;
                    return true;
                }
            }
        }
        return false;
    }

    String restoCercano(float px, float py, float pz, float max) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_RESTO) {
                    continue;
                }
                float dx = c.x[k] - px, dy = c.y[k] + 1 - py, dz = c.z[k] - pz;
                if (dx * dx + dy * dy + dz * dz < max * max) {
                    return "resto:" + c.cx + "," + c.cz + "," + k;
                }
            }
        }
        return null;
    }

    /** nearest cave within 'max' metres; fills out[0..2] with its centre and returns true */
    /** a tall-grass clump between dmin and dmax metres of (px, pz), for a lurker to hide in */
    boolean hierbaCercana(float px, float pz, float dmin, float dmax, float[] out) {
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_HIERBA) {
                    continue;
                }
                float dx = c.x[k] - px, dz = c.z[k] - pz;
                float d2 = dx * dx + dz * dz;
                if (d2 > dmin * dmin && d2 < dmax * dmax) {
                    out[0] = c.x[k];
                    out[1] = c.y[k];
                    out[2] = c.z[k];
                    return true;
                }
            }
        }
        return false;
    }

    /** the nearest cave or tunnel within max metres: out = x, y, z of its heart */
    boolean refugioCercano(float px, float pz, float max, float[] out) {
        float mejor = max * max;
        boolean hay = false;
        for (int i = 0; i < nChunks; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n; k++) {
                if (c.tipo[k] != P_CUEVA && c.tipo[k] != P_TUNEL) {
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
    private static void circulo(float[] p, float cx, float cz, float r, float tope) {
        float dx = p[0] - cx;
        float dz = p[2] - cz;
        float d2 = dx * dx + dz * dz;
        if (d2 < r * r && d2 > 1e-6f) {
            float d = j2me.FMath.sqrt(d2);
            // coming from above (less overlap upwards than sideways): rest on top of it
            if (tope - p[1] < r - d) {
                p[1] = tope;
                return;
            }
            p[0] = cx + dx / d * r;
            p[2] = cz + dz / d * r;
        }
    }

    private static void formacion(float[] p, Chunk c, int k, float rj) {
        int tipo = c.tipo[k];
        float e = c.esc[k];
        float tope = c.y[k] + ALTO_COL[tipo] * e + rj * 0.5f;
        if (p[1] > tope) {
            return;
        }
        float rot = c.rot[k] * j2me.FMath.RAD;
        float co = j2me.FMath.cos(rot), si = j2me.FMath.sin(rot);
        // something as big as the Remora does not fit into caves or tunnels: solid for it
        if (rj > 1.2f && tipo == P_CUEVA) {
            circulo(p, c.x[k], c.z[k], 4.1f * e + rj, tope);
            return;
        }
        if (rj > 1.2f && tipo == P_TUNEL) {
            // too big for the tunnel: solid along its arc
            for (int q = -2; q <= 2; q++) {
                float th = q * 30 * j2me.FMath.RAD;
                float lx = Modelos.TUNEL_R * j2me.FMath.sin(th) * e;
                float lz = (Modelos.TUNEL_R - Modelos.TUNEL_R * j2me.FMath.cos(th)) * e;
                circulo(p, c.x[k] + lx * co + lz * si, c.z[k] - lx * si + lz * co, 3.3f * e + rj, tope);
            }
            return;
        }
        if (tipo == P_TUNEL) {
            // an arch bent along a 130-degree arc: measure the diver against the arc's centre;
            // the ends are open. Cross-section: half circle, taller than wide inside.
            float dx = p[0] - c.x[k], dz = p[2] - c.z[k];
            float lx = (dx * co - dz * si) / e;
            float lz = (dx * si + dz * co) / e;
            float R = Modelos.TUNEL_R;
            float ax = lx, az = lz - R;
            float dC = j2me.FMath.sqrt(ax * ax + az * az);
            if (dC < 1e-3f) {
                return;
            }
            float ang = j2me.FMath.atan2(ax, -az) * 57.29578f;
            if (ang < -Modelos.TUNEL_ANG - 3 || ang > Modelos.TUNEL_ANG + 3) {
                return;
            }
            float lat = dC - R;
            float ly = (p[1] - c.y[k]) / e + 0.6f;
            if (ly < 0) {
                return;
            }
            final float VERT = 1.45f;
            float lys = ly / VERT;
            float rr = j2me.FMath.sqrt(lat * lat + lys * lys);
            float rIn = 2.0f - rj / e, rOut = 3.1f + rj / e;
            if (rr < rIn || rr > rOut || rr < 1e-3f) {
                return;
            }
            float nr = rr < (rIn + rOut) / 2 ? rIn : rOut;
            float nlat = lat / rr * nr;
            float nly = lys / rr * nr * VERT;
            float f = (R + nlat) / dC;
            float nlx = ax * f, nlz = az * f + R;
            p[0] = c.x[k] + (nlx * co + nlz * si) * e;
            p[2] = c.z[k] + (-nlx * si + nlz * co) * e;
            p[1] = c.y[k] + (nly - 0.6f) * e;
            return;
        }
        if (tipo == P_CUEVA) {
            // a rock dome (3.6 m across, 3 m tall, centre 0.4 m below the base) with its mouth
            // open towards local +Z: the shell pushes out from inside and outside, roof too
            float dx = p[0] - c.x[k], dz = p[2] - c.z[k];
            float dy = p[1] - (c.y[k] - 0.4f * e);
            if (dy < 0) {
                return;
            }
            float rx = 3.6f * e, ry = 3.0f * e;
            float ux = dx / rx, uy = dy / ry, uz = dz / rx;
            float q = j2me.FMath.sqrt(ux * ux + uy * uy + uz * uz);
            float qIn = 0.9f - rj / rx, qOut = 1.15f + rj / rx;
            if (q < qIn || q > qOut || q < 1e-3f) {
                return;
            }
            // the mouth: horizontal direction within +-35 degrees of local +Z, lower part
            float dh = j2me.FMath.sqrt(dx * dx + dz * dz);
            if (dh > 1e-3f && (dx * si + dz * co) / dh > 0.45f && uy / q < 0.78f) {
                return;
            }
            float nq = q < (qIn + qOut) / 2 ? qIn : qOut;
            float f = nq / q;
            p[0] = c.x[k] + dx * f;
            p[2] = c.z[k] + dz * f;
            p[1] = c.y[k] - 0.4f * e + dy * f;
            return;
        }
        float[] col = tipo == P_ARCO ? COL_ARCO
                : (tipo == P_RESTO ? COL_RESTO : (tipo == P_COSTILLAS ? COL_COSTILLAS : COL_CRESTA));
        for (int i = 0; i < col.length; i += 3) {
            float lx = col[i] * e, lz = col[i + 1] * e;
            circulo(p, c.x[k] + lx * co + lz * si, c.z[k] - lx * si + lz * co, col[i + 2] * e + rj, tope);
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
                if (esFormacion(tipo)) {
                    formacion(p, c, k, radioJugador);
                    continue;
                }
                if (RADIO_COL[tipo] == 0) {
                    continue;
                }
                float e = c.esc[k];
                float tope = c.y[k] + ALTO_COL[tipo] * e + radioJugador * 0.5f;
                if (p[1] > tope) {
                    continue;
                }
                circulo(p, c.x[k], c.z[k], RADIO_COL[tipo] * e + radioJugador, tope);
            }
        }
    }

    // ---------------------------------------------------------- nearby solids for creatures
    // A short list of the rock formations (caves, tunnels, arches, wrecks) and big solid
    // props around the diver, rebuilt when the diver has moved a few metres or chunks
    // changed. Creatures and projectiles test against it only: cheap enough for every fish.
    private static final int MAX_CERCA = 40;
    private final Chunk[] cercaC = new Chunk[MAX_CERCA];
    private final int[] cercaK = new int[MAX_CERCA];
    private final float[] cercaR = new float[MAX_CERCA];
    private int nCerca;
    private float cercaX = 1e9f, cercaZ = 1e9f;
    boolean cercaSucia = true;
    private final float[] qTmp = new float[3];

    void prepararCerca(float px, float pz) {
        float mx = px - cercaX, mz = pz - cercaZ;
        if (!cercaSucia && mx * mx + mz * mz < 8 * 8) {
            return;
        }
        cercaSucia = false;
        cercaX = px;
        cercaZ = pz;
        nCerca = 0;
        for (int i = 0; i < nChunks && nCerca < MAX_CERCA; i++) {
            Chunk c = chunks[i];
            for (int k = 0; k < c.n && nCerca < MAX_CERCA; k++) {
                int tipo = c.tipo[k];
                boolean forma = esFormacion(tipo);
                if (!forma && RADIO_COL[tipo] * c.esc[k] < 0.9f) {
                    continue;
                }
                float r = (forma ? TAM[tipo] + 2 : RADIO_COL[tipo] + 0.5f) * c.esc[k];
                float dx = c.x[k] - px, dz = c.z[k] - pz;
                if (dx * dx + dz * dz > (60 + r) * (60 + r)) {
                    continue;
                }
                cercaC[nCerca] = c;
                cercaK[nCerca] = k;
                cercaR[nCerca] = r;
                nCerca++;
            }
        }
    }

    /** push a creature at p (radius r) out of nearby rock; true if it was inside something */
    boolean empujarCerca(float[] p, float r) {
        float x0 = p[0], y0 = p[1], z0 = p[2];
        for (int i = 0; i < nCerca; i++) {
            Chunk c = cercaC[i];
            int k = cercaK[i];
            if (c.quitado[k]) {
                continue;
            }
            float dx = p[0] - c.x[k], dz = p[2] - c.z[k];
            float br = cercaR[i] + r;
            if (dx * dx + dz * dz > br * br) {
                continue;
            }
            int tipo = c.tipo[k];
            if (esFormacion(tipo)) {
                formacion(p, c, k, r);
            } else {
                float tope = c.y[k] + ALTO_COL[tipo] * c.esc[k] + r * 0.5f;
                if (p[1] <= tope) {
                    circulo(p, c.x[k], c.z[k], RADIO_COL[tipo] * c.esc[k] + r, tope);
                }
            }
        }
        float mx = p[0] - x0, my = p[1] - y0, mz = p[2] - z0;
        return mx * mx + my * my + mz * mz > 1e-6f;
    }

    /** true if the point is inside rock */
    boolean enRoca(float x, float y, float z) {
        qTmp[0] = x;
        qTmp[1] = y;
        qTmp[2] = z;
        return empujarCerca(qTmp, 0.15f);
    }

    /** true if a straight line between two points does not cross rock walls (a sample every 0.15 m) */
    boolean lineaLibre(float ax, float ay, float az, float bx, float by, float bz) {
        float lx = bx - ax, ly = by - ay, lz = bz - az;
        float dist = j2me.FMath.sqrt(lx * lx + ly * ly + lz * lz);
        int n = Math.min(200, 2 + (int) (dist / 0.15f));
        for (int i = 1; i < n; i++) {
            float f = (float) i / n;
            if (enRoca(ax + (bx - ax) * f, ay + (by - ay) * f, az + (bz - az) * f)) {
                return false;
            }
        }
        return true;
    }

    int recoger(int ci, int k) {
        Chunk c = chunks[ci];
        c.quitado[k] = true;
        recogidos.put(c.cx + "," + c.cz + "," + k, Boolean.TRUE);
        return c.tipo[k];
    }
}
