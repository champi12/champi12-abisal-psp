package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * Small fish around the player. They wander in loose schools, flee when the player gets
 * close and are respawned out of sight when they fall too far behind, so the sea always
 * feels alive without simulating thousands of fish.
 */
public final class Peces {
    static final int N = 26;
    final float[] x = new float[N];
    final float[] y = new float[N];
    final float[] z = new float[N];
    final float[] dir = new float[N];   // heading, degrees
    final float[] vel = new float[N];
    final float[] fase = new float[N];
    final boolean[] vivo = new boolean[N];
    final float[] suelo = new float[N];
    /** hits left before it dies, and seconds of panic after being hit */
    final int[] vida = new int[N];
    final float[] miedo = new float[N];
    /** a knock from the Remora: the fish's own drift velocity (decays like water drag), how
     *  stunned it is (1 = tumbling, 0 = fine), its tumble angle and a cooldown between knocks */
    final float[] kx = new float[N], ky = new float[N], kz = new float[N];
    final float[] aturdido = new float[N], tumbo = new float[N], tumboDir = new float[N], golpeT = new float[N];
    /** smooth steering: wanted heading, time until a new one, current speed, turn rate
     *  (for the bank), wanted height and vertical speed */
    final float[] objetivo = new float[N], giroT = new float[N], velAct = new float[N], giro = new float[N];
    final float[] yObj = new float[N], vyPez = new float[N];
    /** cave fish: they stay within a few metres of their cave (hogar) */
    final boolean[] conHogar = new boolean[N];
    final float[] hx = new float[N], hz = new float[N];
    Mundo mundo;
    private final float[] cueva = new float[3];
    private int tic;
    /** 0 = bubblefin (shallows, kelp), 1 = lanternling (canyons, abyss), 2 = ribbonfin (kelp) */
    final int[] especie = new int[N];
    private final Mesh[] mallas = new Mesh[NE];
    /** set by the game each frame: some species only come out at night */
    static boolean noche;
    /** what catching each species by hand gives (-1: it cannot be caught) */
    static final int[] CAPTURA = { Inventario.PEZ, Inventario.FAROLILLO, Inventario.LAZOFIN, -1, -1, -1, -1, -1,
            Inventario.CARACOL, Inventario.CAMARONES, -1, Inventario.CANDIL, -1, -1, -1, Inventario.CABALLITO_CRUDO,
            -1, -1,
            // the zones' own fish: 18 espejo, 19 raspa, 20 prisma, 21 aguja, 22 lapida, 23 candela,
            // 24 teja, 25 ceniza, 26 ojo, 27 murcielago
            Inventario.PEZ, -1, Inventario.PEZ, -1, Inventario.PEZ, -1, Inventario.PEZ, Inventario.PEZ, Inventario.PEZ,
            -1 };
    /** number of species */
    static final int NE = 28;
    /**
     * The one zone each species lives in: every zone has its own fish, none is shared. 11
     * (lantern fish) and 14 (spark ray) only come out at night.
     */
    static final int[] ZONA_PEZ = { Terreno.BAJIOS, Terreno.ABISMO, Terreno.BOSQUE, Terreno.BAJIOS, Terreno.BOSQUE,
            Terreno.BOSQUE, Terreno.PRADERA, Terreno.PRADERA, Terreno.PRADERA, Terreno.TERMAL, Terreno.BAJIOS,
            Terreno.BAJIOS, Terreno.CANONES, Terreno.MESETA, Terreno.BOSQUE, Terreno.BAJIOS, Terreno.LLANURA,
            Terreno.LLANURA, Terreno.CRISTAL, Terreno.OSARIO, Terreno.CRISTAL, Terreno.CRISTAL, Terreno.OSARIO,
            Terreno.OSARIO, Terreno.MESETA, Terreno.TERMAL, Terreno.ABISMO, Terreno.CANONES };
    /**
     * Food fish also turn up in a few other zones of a similar depth (never a shallows fish
     * down in the deep): species, then the extra zones it lives in.
     */
    static final int[][] OTRAS_ZONAS = { { 0, Terreno.BOSQUE, Terreno.PRADERA }, { 15, Terreno.PRADERA },
            { 2, Terreno.PRADERA }, { 11, Terreno.BOSQUE }, { 1, Terreno.CANONES, Terreno.OSARIO },
            { 18, Terreno.MESETA }, { 22, Terreno.LLANURA }, { 24, Terreno.CANONES }, { 26, Terreno.OSARIO } };

    /** true if species e lives in zone b (its own zone or one of its extra ones) */
    static boolean viveEn(int e, int b) {
        if (ZONA_PEZ[e] == b) {
            return true;
        }
        for (int k = 0; k < OTRAS_ZONAS.length; k++) {
            int[] o = OTRAS_ZONAS[k];
            if (o[0] == e) {
                for (int q = 1; q < o.length; q++) {
                    if (o[q] == b) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    /** puffer urchins: how inflated each one is (0-1) */
    final float[] hinchado = new float[N];
    private final Transform t = new Transform();
    private int semilla = 99;
    int dibujados;

    /** animated parts of rigged species (null = a single rigid mesh in mallas) */
    private final Mesh[][] partes = new Mesh[NE][];
    private final Transform tp = new Transform();
    static final float B = 99;

    Peces(Fog fog) {
        // rigged: the model is split into parts ("bones") animated separately
        // the bubblefin is a Blender model (assets/modelos/burbujin.py); the old procedural
        // one stays as a fallback if the file cannot be loaded
        // (baked like the other species: block style, its pixel art from the creature atlas)
        burbujin = pp("burbujin", fog, 3, 255);
        // every species is a Blender model (assets/modelos); the code-built one is only made
        // if its file is missing. The parts are cut along the same boxes as before, so the
        // animation code below is unchanged.
        partes[0] = Modelos.constructor(0, fog).buildPartes(fog, 255, true, 0,
                new float[][] { { -B, B, -B, B, -B, -0.2f } }); // tail, body (the bubblefin has its own model)
        partes[1] = pp("farolillo", fog, 2, 255);
        if (partes[1] == null) {
            partes[1] = Modelos.constructor(1, fog).buildPartes(fog, 255, true, 0,
                    new float[][] { { -B, B, -B, B, -B, -0.14f } });
        }
        partes[2] = pp("lazofin", fog, 3, 255);
        if (partes[2] == null) {
            partes[2] = Modelos.constructor(2, fog).buildPartes(fog, 255, true, 0x504010,
                    new float[][] { { -B, B, -B, B, -B, -1.1f }, { -B, B, -B, B, -B, -0.3f } }); // far, near ribbon
        }
        partes[3] = pp("planeador", fog, 3, 255);
        if (partes[3] == null) {
            partes[3] = Modelos.constructor(3, fog).buildPartes(fog, 255, true, 0,
                    new float[][] { { 0.2f, B, -B, B, -B, B }, { -B, -0.2f, -B, B, -B, B } }); // right, left wing
        }
        partes[4] = pp("medusa", fog, 2, 175);
        if (partes[4] == null) {
            partes[4] = Modelos.constructor(4, fog).buildPartes(fog, 175, true, 0,
                    new float[][] { { -B, B, -B, -0.05f, -B, B } }); // tentacles, bell
        }
        partes[7] = pp("manta", fog, 3, 255);
        if (partes[7] == null) {
            partes[7] = Modelos.constructor(7, fog).buildPartes(fog, 255, true, 0,
                    new float[][] { { 0.45f, B, -B, B, -B, B }, { -B, -0.45f, -B, B, -B, B } });
        }
        partes[10] = pp("tortuga", fog, 5, 255);
        if (partes[10] == null) {
            partes[10] = Modelos.constructor(10, fog).buildPartes(fog, 255, true, 0,
                    new float[][] { { 0.42f, B, -B, B, -0.2f, B }, { -B, -0.42f, -B, B, -0.2f, B },
                            { 0.34f, B, -0.1f, 0.03f, -B, -0.45f }, { -B, -0.34f, -0.1f, 0.03f, -B, -0.45f } });
        }
        mallas[5] = pm("cardumen", fog, 255);
        if (mallas[5] == null) {
            mallas[5] = Modelos.cardumen(fog);
        }
        partes[6] = pp("pastor", fog, 6, 255);
        if (partes[6] == null) {
            partes[6] = Modelos.constructor(6, fog).buildPartes(fog, 255, false, 0,
                    new float[][] { { 0.95f, B, -B, B, 0, B }, { -B, -0.95f, -B, B, 0, B }, { 0.95f, B, -B, B, -B, 0 },
                            { -B, -0.95f, -B, B, -B, 0 }, { -B, B, -B, B, 1.0f, B } }); // 4 flippers, head, body
        }
        mallas[8] = pm("caracol", fog, 255);
        if (mallas[8] == null) {
            mallas[8] = Modelos.caracolDuna(fog);
        }
        mallas[9] = pm("camarones", fog, 255);
        if (mallas[9] == null) {
            mallas[9] = Modelos.camaronesIgneos(fog);
        }
        partes[11] = pp("pez_candil", fog, 2, 255);
        if (partes[11] == null) {
            partes[11] = Modelos.constructor(11, fog).buildPartes(fog, 255, true, 0,
                    new float[][] { { -B, B, -B, B, -B, -0.18f } }); // tail, body
        }
        mallas[12] = pm("flotador", fog, 220);
        if (mallas[12] == null) {
            mallas[12] = Modelos.flotador(fog);
        }
        mallas[13] = pm("erizo", fog, 255);
        if (mallas[13] == null) {
            mallas[13] = Modelos.erizoGlobo(fog);
        }
        mallas[14] = pm("raya_chispa", fog, 255);
        if (mallas[14] == null) {
            mallas[14] = Modelos.rayaChispa(fog);
        }
        mallas[15] = pm("caballito", fog, 255);
        if (mallas[15] == null) {
            mallas[15] = Modelos.caballito(fog);
        }
        mallas[16] = pm("farol_llanura", fog, 255);
        if (mallas[16] == null) {
            mallas[16] = Modelos.farolLlanura(fog);
        }
        mallas[17] = pm("raya_llanura", fog, 255);
        if (mallas[17] == null) {
            mallas[17] = Modelos.rayaLlanura(fog);
        }
        // the fish of the new zones and the ones that make every zone's fauna its own: all
        // tail + body, animated like the lantern fish (fallback: a lanternling)
        partes[18] = pp("pez_espejo", fog, 2, 255);
        partes[19] = pp("raspa", fog, 2, 255);
        Mesh[] pz8 = pp("peces_zonas", fog, 16, 255);
        for (int k = 0; k < 8; k++) {
            partes[20 + k] = pz8 != null ? new Mesh[] { pz8[k * 2], pz8[k * 2 + 1] } : null;
        }
        for (int k = 18; k < NE; k++) {
            if (partes[k] == null) {
                partes[k] = partes[11];
            }
        }
    }

    private static Mesh[] pp(String nombre, Fog fog, int piezas, int alpha) {
        return ModeloM3G.partes("/modelos/" + nombre + ".m3g", fog, piezas, alpha, true);
    }

    private static Mesh pm(String nombre, Fog fog, int alpha) {
        return ModeloM3G.malla("/modelos/" + nombre + ".m3g", fog, alpha, true);
    }

    /**
     * Draw a rigged creature from transform t. f is its animation phase, v its speed.
     * Returns false if the species is not rigged.
     */
    private boolean dibujarPartes(Graphics3D g3d, int e, float f, float v) {
        Mesh[] m = partes[e];
        if (m == null) {
            return false;
        }
        for (int k = 0; k < m.length; k++) {
            if (m[k] == null) {
                return false; // a part came out empty: draw nothing rather than crash
            }
        }
        if (e == 0 && burbujin != null && burbujin[0] != null && burbujin[1] != null && burbujin[2] != null) {
            dibujarBurbujin(g3d, f, v);
            return true;
        }
        switch (e) {
        case 0:
        case 1:
        case 11:
        case 18:
        case 19:
        case 20:
        case 21:
        case 22:
        case 23:
        case 24:
        case 25:
        case 26:
        case 27: {
            // tail beat, stronger when swimming fast; the body counter-swings a little,
            // with a subtle roll and a breathing pulse so it feels alive
            float zc = e == 0 ? -0.2f : (e == 1 ? -0.14f : -0.18f);
            float a = j2me.FMath.sin(f * 2.4f) * (14 + v * 10);
            t.postRotate(j2me.FMath.sin(f * 2.4f + 1.2f) * 3, 0, 0, 1);
            t.postScale(1, 1 + 0.04f * j2me.FMath.sin(f * 3.2f), 1);
            t.postRotate(-a * 0.2f, 0, 1, 0);
            g3d.render(m[1], t);
            tp.set(t);
            tp.postTranslate(0, 0, zc);
            tp.postRotate(a, 0, 1, 0);
            tp.postTranslate(0, 0, -zc);
            g3d.render(m[0], tp);
            return true;
        }
        case 2: {
            // the disc rolls and bobs; the two ribbons wave one after the other
            t.postRotate(j2me.FMath.sin(f * 0.9f) * 12, 0, 0, 1);
            t.postTranslate(0, j2me.FMath.sin(f * 1.4f) * 0.05f, 0);
            g3d.render(m[2], t);
            tp.set(t);
            tp.postTranslate(0, 0, -0.3f);
            tp.postRotate(j2me.FMath.sin(f * 2.0f) * 22, 1, 0, 0);
            tp.postTranslate(0, 0, 0.3f);
            g3d.render(m[1], tp);
            tp.postTranslate(0, 0, -1.1f);
            tp.postRotate(j2me.FMath.sin(f * 2.0f - 1.2f) * 32, 1, 0, 0);
            tp.postTranslate(0, 0, 1.1f);
            g3d.render(m[0], tp);
            return true;
        }
        case 3:
        case 7: {
            // wings flap from the body edge; the body bobs against the stroke
            float xc = e == 3 ? 0.18f : 0.4f;
            float aleteo = j2me.FMath.sin(f * (e == 3 ? 2.2f : 0.9f)) * (e == 3 ? 28 : 22);
            t.postTranslate(0, -aleteo * (e == 3 ? 0.0015f : 0.004f), 0);
            g3d.render(m[2], t);
            tp.set(t);
            tp.postTranslate(xc, 0, 0);
            tp.postRotate(aleteo, 0, 0, 1);
            tp.postTranslate(-xc, 0, 0);
            g3d.render(m[0], tp);
            tp.set(t);
            tp.postTranslate(-xc, 0, 0);
            tp.postRotate(-aleteo, 0, 0, 1);
            tp.postTranslate(xc, 0, 0);
            g3d.render(m[1], tp);
            return true;
        }
        case 4: {
            // the bell pulses; the tentacles trail and sway behind each pulse
            float pulso = j2me.FMath.sin(f * 2.2f);
            tp.set(t);
            t.postScale(1 + 0.16f * pulso, 1 - 0.18f * pulso, 1 + 0.16f * pulso);
            g3d.render(m[1], t);
            tp.postRotate(j2me.FMath.sin(f * 2.2f - 1.5f) * 16, 1, 0, 0);
            tp.postRotate(j2me.FMath.sin(f * 1.3f) * 12, 0, 0, 1);
            tp.postScale(1, 1 + 0.12f * j2me.FMath.sin(f * 2.2f - 1), 1);
            g3d.render(m[0], tp);
            return true;
        }
        case 6: {
            // sand shepherd: a slow, heavy sway; flippers paddle in diagonal pairs; the head
            // bobs down to graze now and then
            t.postRotate(j2me.FMath.sin(f * 0.8f) * 4, 0, 0, 1);
            g3d.render(m[5], t);
            for (int k = 0; k < 4; k++) {
                float lado = (k & 1) == 0 ? 1 : -1;
                float zc = k < 2 ? 0.55f : -0.55f;
                float fase = f * 1.2f + ((k == 0 || k == 3) ? 0 : j2me.FMath.PI);
                tp.set(t);
                tp.postTranslate(lado, -0.1f, zc);
                tp.postRotate(j2me.FMath.sin(fase) * 20 * lado, 0, 0, 1);
                tp.postRotate(j2me.FMath.cos(fase) * 12 * lado, 0, 1, 0);
                tp.postTranslate(-lado, 0.1f, -zc);
                g3d.render(m[k], tp);
            }
            float pasta = Math.max(0, j2me.FMath.sin(f * 0.35f)) * 18;
            tp.set(t);
            tp.postTranslate(0, 0, 1.0f);
            tp.postRotate(pasta + j2me.FMath.sin(f * 0.9f) * 5, 1, 0, 0);
            tp.postTranslate(0, 0, -1.0f);
            g3d.render(m[4], tp);
            return true;
        }
        case 10: {
            // turtle: big sweeping front flippers, small rear paddles out of phase
            float brazada = j2me.FMath.sin(f * 1.3f);
            t.postRotate(brazada * 3, 0, 0, 1);
            g3d.render(m[4], t);
            for (int k = 0; k < 4; k++) {
                float lado = (k & 1) == 0 ? 1 : -1;
                boolean frente = k < 2;
                float xc = (frente ? 0.4f : 0.3f) * lado;
                float zc = frente ? 0.2f : -0.5f;
                float a = frente ? brazada * 30 : j2me.FMath.sin(f * 1.3f + 1.6f) * 15;
                tp.set(t);
                tp.postTranslate(xc, 0, zc);
                tp.postRotate(a * lado, 0, 0, 1);
                tp.postRotate((frente ? j2me.FMath.cos(f * 1.3f) * 18 : 0) * lado, 0, 1, 0);
                tp.postTranslate(-xc, 0, -zc);
                g3d.render(m[k], tp);
            }
            return true;
        }
        default:
            return false;
        }
    }

    /** body radius of each species, for collisions with the diver */
    static final float[] RADIO = { 0.25f, 0.2f, 0.3f, 0.4f, 0.35f, 0.6f, 1.0f, 1.2f, 0.25f, 0.5f, 0.6f, 0.2f, 0.6f, 0.35f, 0.5f, 0.22f, 0.45f,
            1.0f, 0.25f, 0.3f, 0.22f, 0.3f, 0.3f, 0.25f, 0.28f, 0.25f, 0.22f, 0.3f };

    /**
     * Contact with the diver (nave false) or the Remora (nave true); (vx, vz) is their velocity.
     * The diver never shoves a fish: it only startles and slips away by itself, with a gentle
     * drift so it does not stay inside the diver. The Remora knocks it like a real body in the
     * water: it is thrown off with the hull's momentum, tumbles a moment, then flees.
     */
    void empujar(float[] q, float rj, float vx, float vz, boolean nave) {
        for (int i = 0; i < N; i++) {
            if (!vivo[i]) {
                continue;
            }
            float r = RADIO[especie[i]] + rj;
            float dy = y[i] - q[1];
            if (dy > r || dy < -r) {
                continue;
            }
            float dx = x[i] - q[0], dz = z[i] - q[2];
            float d2 = dx * dx + dz * dz;
            if (d2 >= r * r) {
                continue;
            }
            float d = j2me.FMath.sqrt(d2);
            if (d < 1e-3f) {
                dx = 1;
                dz = 0;
                d = 1;
            }
            float nx = dx / d, nz = dz / d;
            if (!nave) {
                // a startled flick away, never a shove
                miedo[i] = Math.max(miedo[i], 2.5f);
                float k = (r - d) * 1.5f;
                kx[i] += nx * k;
                kz[i] += nz * k;
                continue;
            }
            if (golpeT[i] <= 0) {
                float sp = j2me.FMath.sqrt(vx * vx + vz * vz);
                float imp = Math.min(9, 1.5f + sp * 0.8f);
                // thrown away from the hull and carried along with it
                kx[i] = nx * imp + vx * 0.55f;
                kz[i] = nz * imp + vz * 0.55f;
                ky[i] = (dy >= 0 ? 1 : -1) * imp * 0.25f;
                aturdido[i] = Math.min(1, 0.35f + sp * 0.12f);
                tumboDir[i] = ((i & 1) == 0 ? 1 : -1) * (0.6f + azar() * 0.8f);
                miedo[i] = 5;
                golpeT[i] = 0.5f;
            }
            // never left inside the hull (a soft correction, the drift does the rest)
            float k = (r - d) * 0.5f;
            x[i] += nx * k;
            z[i] += nz * k;
        }
    }

    /** the Blender bubblefin: pieces 1 body, 2 side fins, 3 tail */
    private Mesh[] burbujin;
    private final Transform tq = new Transform();

    private void pieza(Graphics3D g3d, Transform t0, int i) {
        g3d.render(burbujin[i], t0);
    }

    private void dibujarBurbujin(Graphics3D g3d, float f, float v) {
        t.postScale(0.72f, 0.72f, 0.72f); // the model is ~0.85 m long; the fish is ~0.6 m
        float rapido = Math.min(1, v / 2.5f); // 0 hovering .. 1 fleeing
        // the whole fish bobs gently and rolls a little with each stroke
        t.postTranslate(0, j2me.FMath.sin(f * 0.45f) * 0.025f, 0);
        float a = j2me.FMath.sin(f * 1.3f) * (7 + rapido * 13);
        t.postRotate(j2me.FMath.sin(f * 1.3f + 1.2f) * (1 + rapido * 1.5f), 0, 0, 1);
        // the head counter-swings against the tail: an S along the body
        t.postRotate(-a * 0.15f, 0, 1, 0);
        tp.set(t);
        tp.postScale(1, 1 + 0.02f * j2me.FMath.sin(f * 0.8f), 1); // breathing
        pieza(g3d, tp, 0);
        // side fins: busy paddling while it hovers, swept back and still when it speeds off
        float aleteo = j2me.FMath.sin(f * 1.1f) * (9 - rapido * 6);
        tp.set(t);
        tp.postTranslate(0, 0, 0.05f);
        tp.postRotate(aleteo, 0, 0, 1);
        tp.postRotate(rapido * 10, 1, 0, 0);
        tp.postTranslate(0, 0, -0.05f);
        pieza(g3d, tp, 1);
        // the tail pivots where it joins the body (z = -0.23 in model units), with a lag
        // behind the body so the beat travels down it
        float ac = j2me.FMath.sin(f * 1.3f - 0.9f) * (11 + rapido * 17);
        tp.set(t);
        tp.postTranslate(0, 0, -0.23f);
        tp.postRotate(ac, 0, 1, 0);
        tp.postTranslate(0, 0, 0.23f);
        pieza(g3d, tp, 2);
    }

    /** gallery: one animated creature of a species at transform base */
    void dibujarMuestra(Graphics3D g3d, Transform base, int e, float tiempo) {
        t.set(base);
        if (!dibujarPartes(g3d, e, tiempo * 3, 1)) {
            g3d.render(mallas[e], t);
        }
    }

    private float azar() {
        semilla = semilla * 1103515245 + 12345;
        return ((semilla >>> 8) & 0xffff) / 65535f;
    }

    /** a species of the zone at a point (-1: none) */
    private static int especieDe(float x, float z) {
        int b = Terreno.bioma(x, z);
        for (int e = 0; e < NE; e++) {
            if (ZONA_PEZ[e] == b && e != 11 && e != 14) {
                return e;
            }
        }
        return -1;
    }

    private boolean habitat(int i, float x, float z) {
        int e = especie[i];
        if ((e == 11 || e == 14) && !noche) {
            return false;
        }
        return viveEn(e, Terreno.bioma(x, z));
    }

    private void nacer(int i, float px, float py, float pz) {
        kx[i] = ky[i] = kz[i] = 0;
        aturdido[i] = tumbo[i] = golpeT[i] = 0;
        float a = azar() * 6.2832f;
        float d = 18 + azar() * 20;
        float nx = px + j2me.FMath.cos(a) * d;
        float nz = pz + j2me.FMath.sin(a) * d;
        // a few bubblefins live in the small caves of the shallows
        conHogar[i] = false;
        if (mundo != null && azar() < 0.1f && mundo.cuevaCercana(px, pz, 30, cueva)
                && Terreno.bioma(cueva[0], cueva[2]) == Terreno.BAJIOS) {
            especie[i] = 0;
            conHogar[i] = true;
            hx[i] = cueva[0];
            hz[i] = cueva[2];
            x[i] = cueva[0] + (azar() - 0.5f) * 3;
            z[i] = cueva[2] + (azar() - 0.5f) * 3;
            float s0 = Terreno.altura(x[i], z[i]);
            suelo[i] = s0 + 0.6f;
            y[i] = s0 + 1 + azar();
            dir[i] = azar() * 360;
            vel[i] = 0.8f + azar() * 0.5f;
            fase[i] = azar() * 6;
            vivo[i] = true;
            vida[i] = 1;
            miedo[i] = 0;
            return;
        }
        int e = especieDe(nx, nz);
        if (e < 0) {
            vivo[i] = false;
            return;
        }
        int bio = Terreno.bioma(nx, nz);
        float r = azar();
        // mix of each zone's own species
        boolean hayPastor = false, hayTortuga = false;
        for (int q = 0; q < N; q++) {
            hayPastor |= vivo[q] && especie[q] == 6;
            hayTortuga |= vivo[q] && especie[q] == 10;
        }
        switch (bio) {
        case Terreno.BAJIOS:
            e = noche && r < 0.4f ? 11 : (r < 0.25f ? 3 : (r < 0.35f && !hayTortuga ? 10 : (r < 0.55f ? 15 : 0)));
            break;
        case Terreno.BOSQUE:
            e = noche && r < 0.15f ? 14 : (r < 0.45f ? 2 : (r < 0.62f ? 4 : 5));
            break;
        case Terreno.PRADERA:
            e = r < 0.35f ? 7 : (r < 0.7f || hayPastor ? 8 : 6);
            break;
        case Terreno.MESETA:
            e = r < 0.6f ? 24 : 13;
            break;
        case Terreno.CANONES:
            e = r < 0.4f ? 12 : 27;
            break;
        case Terreno.TERMAL:
            e = r < 0.6f ? 9 : 25;
            break;
        case Terreno.ABISMO:
            e = r < 0.5f ? 1 : 26;
            break;
        case Terreno.LLANURA:
            e = r < 0.55f ? 16 : 17;
            break;
        case Terreno.CRISTAL:
            e = r < 0.4f ? 18 : (r < 0.75f ? 20 : 21);
            break;
        case Terreno.OSARIO:
            e = r < 0.4f ? 22 : (r < 0.75f ? 19 : 23);
            break;
        default:
            break;
        }
        // now and then a food fish from a neighbouring zone of the same depth
        if (azar() < 0.18f) {
            int k = (int) (azar() * OTRAS_ZONAS.length) % OTRAS_ZONAS.length;
            int otro = OTRAS_ZONAS[k][0];
            if (viveEn(otro, bio) && ZONA_PEZ[otro] != bio && (otro != 11 || noche)) {
                e = otro;
            }
        }
        hinchado[i] = 0;
        especie[i] = e;
        float s = Terreno.altura(nx, nz);
        suelo[i] = s + 1.0f;
        x[i] = nx;
        z[i] = nz;
        y[i] = Math.min(-1.5f, Math.max(s + 1.5f, py + (azar() - 0.5f) * 8));
        dir[i] = azar() * 360;
        vel[i] = (e == 0 ? 0.6f : 1.2f) + azar() * (e == 0 ? 0.3f : 1f); // bubblefins amble
        fase[i] = azar() * 6;
        vivo[i] = true;
        vida[i] = e == 6 || e == 7 || e == 10 ? 3 : (e == 4 ? 2 : 1);
        if (e == 7) {
            y[i] = Math.max(y[i], s + 3); // mantas cruise a few metres above the dunes
        }
        miedo[i] = 0;
    }

    /** test builds only (DESFILE=1): every species lined up in front of the camera */
    static boolean desfile;
    /** first species shown in the parade */
    static int desfileDesde;

    void actualizar(float dt, float px, float py, float pz) {
        tic++;
        if (desfile) {
            for (int i = 0; i < N; i++) {
                int e = desfileDesde + i % (NE - desfileDesde);
                vivo[i] = i < NE - desfileDesde;
                especie[i] = e;
                int k = e - desfileDesde;
                float sep = desfileDesde > 0 ? 1.0f : 1.7f;
                x[i] = px + ((k % 5) - 2f) * sep;
                y[i] = py + 0.6f - (k / 5) * (desfileDesde > 0 ? 0.8f : 1.5f);
                z[i] = pz - (desfileDesde > 0 ? 3.2f : 5.5f);
                dir[i] = 0;
                velAct[i] = 0.5f;
                giro[i] = 0;
                vyPez[i] = 0;
                fase[i] += dt * 2;
            }
            return;
        }
        for (int i = 0; i < N; i++) {
            // the expensive terrain queries run every 8 frames per fish, staggered
            boolean consultar = ((tic + i) & 7) == 0;
            if (!vivo[i]) {
                if (azar() < 0.045f) {
                    nacer(i, px, py, pz);
                    objetivo[i] = dir[i];
                    velAct[i] = vel[i] * 0.5f;
                    yObj[i] = y[i];
                    vyPez[i] = 0;
                    giro[i] = 0;
                    giroT[i] = 0;
                }
                continue;
            }
            float dx = x[i] - px;
            float dy = y[i] - py;
            float dz = z[i] - pz;
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > 50 * 50) {
                vivo[i] = false; // reborn out of sight next frames
                continue;
            }
            int e = especie[i];
            float v = vel[i];
            float giroMax = 70; // degrees per second when cruising
            if (e == 4) {
                v = 0.3f; // jellies drift
                giroMax = 15;
            } else if (e == 6) {
                v = 0.7f; // the shepherd grazes slowly
                giroMax = 30;
            } else if (e == 7) {
                v = 1.1f; // the manta glides in wide arcs
                giroMax = 30;
            } else if (e == 8) {
                v = 0.22f; // snails crawl
                giroMax = 40;
            } else if (e == 10) {
                v = 0.6f; // the turtle cruises slowly and does not fear the diver
                giroMax = 25;
            } else if (e == 12) {
                v = 0.35f; // floaters just drift
                giroMax = 10;
            } else if (e == 14) {
                v = 1.0f; // the spark ray glides and does not flee
                giroMax = 30;
            } else if (e == 13) {
                // the puffer urchin does not flee: it swells into a ball of spines
                boolean cerca = d2 < 4 * 4 || miedo[i] > 0;
                hinchado[i] += ((cerca ? 1 : 0) - hinchado[i]) * Math.min(1, dt * (cerca ? 4 : 0.5f));
                v = 0.5f * (1 - hinchado[i]);
                giroMax = 40;
            }
            if (miedo[i] > 0) {
                miedo[i] -= dt;
            }
            if (golpeT[i] > 0) {
                golpeT[i] -= dt;
            }
            // the knock's drift, slowed by the water; a stunned fish barely swims or steers
            if (kx[i] != 0 || ky[i] != 0 || kz[i] != 0) {
                x[i] += kx[i] * dt;
                y[i] += ky[i] * dt;
                z[i] += kz[i] * dt;
                float arr = 1 / (1 + dt * 2.8f);
                kx[i] *= arr;
                ky[i] *= arr;
                kz[i] *= arr;
                if (kx[i] * kx[i] + ky[i] * ky[i] + kz[i] * kz[i] < 0.0004f) {
                    kx[i] = ky[i] = kz[i] = 0;
                }
            }
            if (aturdido[i] > 0) {
                tumbo[i] += tumboDir[i] * aturdido[i] * 540 * dt;
                aturdido[i] -= dt * 0.9f;
                v *= 1 - Math.max(0, aturdido[i]);
                giroMax *= 1 - 0.8f * Math.max(0, aturdido[i]);
            } else if (tumbo[i] != 0) {
                // it rights itself
                tumbo[i] = angulo(tumbo[i]);
                tumbo[i] -= tumbo[i] * Math.min(1, dt * 4);
                if (Math.abs(tumbo[i]) < 0.5f) {
                    tumbo[i] = 0;
                }
            }
            boolean huye = e != 8 && e != 13 && e != 14
                    && (miedo[i] > 0 || (d2 < 4.5f * 4.5f && e != 4 && e != 6 && e != 7 && e != 10 && e != 12));
            giroT[i] -= dt;
            if (huye) {
                // flee: head straight away from the diver, turning hard and fast
                objetivo[i] = j2me.FMath.atan2(dz, dx) * 57.29578f;
                giroMax = 260;
                v *= 2.6f;
            } else if (conHogar[i] && (x[i] - hx[i]) * (x[i] - hx[i]) + (z[i] - hz[i]) * (z[i] - hz[i]) > 9) {
                // too far from its cave: head home
                objetivo[i] = j2me.FMath.atan2(hz[i] - z[i], hx[i] - x[i]) * 57.29578f;
            } else if (giroT[i] <= 0) {
                // wander: pick a new heading now and then, not a new jitter every frame
                objetivo[i] = dir[i] + (azar() - 0.5f) * 120;
                giroT[i] = 2 + azar() * 4;
                // and a new cruising height near the current one
                yObj[i] = y[i] + (azar() - 0.5f) * 2.5f;
            }
            // turn towards the wanted heading at a limited rate (eased near the end)
            float delta = angulo(objetivo[i] - dir[i]);
            float paso = Math.max(-giroMax * dt, Math.min(giroMax * dt, delta * Math.min(1, dt * 2.5f)));
            dir[i] += paso;
            giro[i] += (paso / Math.max(dt, 0.001f) - giro[i]) * Math.min(1, dt * 4);
            // speed eases too: a fish accelerates with a few tail beats
            velAct[i] += (v - velAct[i]) * Math.min(1, dt * (huye ? 4 : 1.5f));
            float vv = velAct[i];
            float r = dir[i] * j2me.FMath.RAD;
            float nx = x[i] + j2me.FMath.cos(r) * vv * dt;
            float nz = z[i] + j2me.FMath.sin(r) * vv * dt;
            if (consultar && !conHogar[i] && !habitat(i, nx, nz)) {
                // leaving its biome: turn around smoothly instead of snapping
                objetivo[i] = dir[i] + 180;
                giroT[i] = 3;
            }
            x[i] = nx;
            z[i] = nz;
            fase[i] += dt * (2 + vv * 2.5f);
            if (consultar) {
                suelo[i] = Terreno.altura(x[i], z[i]) + 1.0f;
            }
            // vertical: ease towards the wanted height, kept between the floor and the surface
            boolean fondo = e == 3 || e == 6 || e == 8;
            float minY = suelo[i] + (e == 8 ? -0.95f : (fondo ? 0.2f : (e == 7 ? 2.0f : 0.6f)));
            float alto = fondo ? minY + (e == 6 ? 0.8f : (e == 8 ? 0 : 0.4f)) : yObj[i];
            alto = Math.max(minY, Math.min(-1.2f, alto));
            float vyObj = (alto - y[i]) * 0.8f + j2me.FMath.sin(fase[i] * 0.5f) * (e == 4 ? 0.25f : 0.08f);
            vyObj = Math.max(-1.2f, Math.min(1.2f, vyObj));
            vyPez[i] += (vyObj - vyPez[i]) * Math.min(1, dt * 2);
            y[i] += vyPez[i] * dt;
            if (y[i] < minY - 0.4f) {
                y[i] += (minY - 0.4f - y[i]) * Math.min(1, dt * 6); // never sink into the floor
            }
            if (e == 8) {
                y[i] = minY; // the snail stays on the sand
            }
            if (y[i] > -1) {
                y[i] = -1;
            }
            // caves, tunnels and wrecks are solid for fish too: they slide along the walls
            if (mundo != null) {
                q[0] = x[i];
                q[1] = y[i];
                q[2] = z[i];
                if (mundo.empujarCerca(q, RADIO[e] * 0.5f)) {
                    x[i] = q[0];
                    y[i] = q[1];
                    z[i] = q[2];
                    if (!huye && giroT[i] > 0.5f) {
                        giroT[i] = 0.5f; // pick another way soon
                    }
                }
            }
        }
    }

    private final float[] q = new float[3];

    static float angulo(float a) {
        while (a > 180) {
            a -= 360;
        }
        while (a < -180) {
            a += 360;
        }
        return a;
    }

    void dibujar(Graphics3D g3d, float px, float pz, float fx, float fz, float lejos) {
        dibujados = 0;
        for (int i = 0; i < N; i++) {
            if (!vivo[i]) {
                continue;
            }
            float ox = x[i] - px;
            float oz = z[i] - pz;
            if (ox * ox + oz * oz > lejos * lejos || (ox * fx + oz * fz) < -2) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(x[i], y[i], z[i]);
            // the model faces +Z; heading 0 = +X
            t.postRotate(90 - dir[i], 0, 1, 0);
            // lean into turns and nose up/down when climbing or diving
            float banco = Math.max(-25, Math.min(25, -giro[i] * 0.25f));
            float cabeceo = Math.max(-25, Math.min(25, -vyPez[i] * 30 / Math.max(0.4f, velAct[i])));
            t.postRotate(cabeceo, 1, 0, 0);
            t.postRotate(banco + tumbo[i], 0, 0, 1);
            float f = fase[i];
            // far away a rigged fish is a few pixels: one draw instead of 2-5, and it is still
            // there (no popping, same count on screen)
            if (ox * ox + oz * oz > 32 * 32 && partes[especie[i]] != null) {
                g3d.render(partes[especie[i]][0], t);
                dibujados++;
                continue;
            }
            if (dibujarPartes(g3d, especie[i], f, velAct[i])) {
                dibujados++;
                continue;
            }
            switch (especie[i]) {
            case 3: // glider: the wings beat as a slow roll, with a gentle pitch
                t.postRotate(j2me.FMath.sin(f * 2) * 14, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 1.3f) * 5, 1, 0, 0);
                break;
            case 4: { // jelly: the bell pulses
                float pulso = j2me.FMath.sin(f * 2.2f);
                t.postScale(1 + 0.12f * pulso, 1 - 0.14f * pulso, 1 + 0.12f * pulso);
                break;
            }
            case 5: // school: drifts and sways gently, together
                t.postRotate(j2me.FMath.sin(f * 1.2f) * 8, 0, 1, 0);
                t.postRotate(j2me.FMath.sin(f * 0.9f) * 4, 0, 0, 1);
                break;
            case 7: // manta: slow, deep wing beats
                t.postRotate(j2me.FMath.sin(f * 0.9f) * 10, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 0.9f + 1.2f) * 6, 1, 0, 0);
                t.postScale(1, 1 + 0.25f * j2me.FMath.sin(f * 1.8f), 1);
                break;
            case 10: // turtle: slow flipper strokes rock the body
                t.postRotate(j2me.FMath.sin(f * 1.2f) * 6, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 2.4f) * 3, 1, 0, 0);
                break;
            case 8: // snail: a slow stretch of the foot
                t.postScale(1, 1, 1 + 0.08f * j2me.FMath.sin(f * 2));
                break;
            case 9: // shrimp swarm: jittery darting
                t.postRotate(j2me.FMath.sin(f * 2.5f) * 18, 0, 1, 0);
                t.postRotate(j2me.FMath.sin(f * 1.7f) * 8, 1, 0, 0);
                break;
            case 14: // spark ray: slow wing beats
                t.postRotate(j2me.FMath.sin(f * 1.1f) * 8, 0, 0, 1);
                t.postScale(1, 1 + 0.3f * j2me.FMath.sin(f * 2.2f), 1);
                break;
            case 13: { // puffer urchin: it swells up, and wobbles while inflated
                float k = 1 + hinchado[i] * 0.9f;
                t.postRotate(j2me.FMath.sin(f * 1.3f) * 8 * hinchado[i], 0, 0, 1);
                t.postScale(k, k * (1 + 0.04f * j2me.FMath.sin(f * 3)), k);
                break;
            }
            case 6: // shepherd: a slow, heavy sway
                t.postRotate(j2me.FMath.sin(f * 0.8f) * 5, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 0.6f) * 6, 0, 1, 0);
                break;
            case 15: // seahorse: bobs in place, holding itself upright
                t.postRotate(j2me.FMath.sin(f * 1.6f) * 4, 1, 0, 0);
                t.postRotate(j2me.FMath.sin(f * 1.1f) * 3, 0, 0, 1);
                break;
            case 16: // lanternfish: the lure sways, the body barely moves
                t.postRotate(j2me.FMath.sin(f * 1.2f) * 4, 0, 1, 0);
                t.postRotate(j2me.FMath.sin(f * 0.9f) * 3, 0, 0, 1);
                break;
            case 17: // ray: a slow wing beat and a lazy bank
                t.postRotate(j2me.FMath.sin(f * 1.4f) * 6, 0, 0, 1);
                t.postScale(1 + 0.05f * j2me.FMath.sin(f * 1.4f), 1, 1 + 0.03f * j2me.FMath.sin(f * 1.4f));
                break;
            default: { // fish: a body wave from the tail beat, a little roll and a breathing pulse
                float r = j2me.FMath.sin(f * 2.4f);
                t.postRotate(r * (4 + velAct[i] * 2.5f), 0, 1, 0);
                t.postRotate(j2me.FMath.sin(f * 2.4f + 1.2f) * 3.5f, 0, 0, 1);
                float br = 1 + 0.05f * j2me.FMath.sin(f * 3.2f);
                t.postScale(1, br, 1);
            }
            }
            g3d.render(mallas[especie[i]], t);
            dibujados++;
        }
    }

    /**
     * Knife hit on the fish in front of the diver. Returns its index (or -1); the caller
     * reads especie[] and vivo[] to know what happened. A survivor panics and flees.
     */
    int golpear(float px, float py, float pz, float fx, float fy, float fz) {
        int mejor = alAlcance(px, py, pz, fx, fy, fz);
        if (mejor >= 0) {
            vida[mejor]--;
            if (vida[mejor] <= 0) {
                vivo[mejor] = false;
            } else {
                miedo[mejor] = 6;
            }
        }
        return mejor;
    }

    /** the fish in front of the diver within knife reach, or -1 */
    int alAlcance(float px, float py, float pz, float fx, float fy, float fz) {
        int mejor = -1;
        float md = 3.0f;
        for (int i = 0; i < N; i++) {
            if (!vivo[i]) {
                continue;
            }
            float ox = x[i] - px, oy = y[i] - py, oz = z[i] - pz;
            float alcance = especie[i] == 6 ? 3.0f : 2.4f;
            float d = j2me.FMath.sqrt(ox * ox + oy * oy + oz * oz);
            if (d < alcance && d < md && (d < 0.9f || ox * fx + oy * fy + oz * fz > d * 0.55f)) {
                md = d;
                mejor = i;
            }
        }
        return mejor;
    }

    /** true if a spark ray is touching (x, y, z) */
    boolean tocaRaya(float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
            if (!vivo[i] || especie[i] != 14) {
                continue;
            }
            float ox = x[i] - px, oy = y[i] - py, oz = z[i] - pz;
            if (ox * ox + oy * oy + oz * oz < 1.4f * 1.4f) {
                return true;
            }
        }
        return false;
    }

    /** true if an inflated puffer urchin is touching (x, y, z) */
    boolean pinchaErizo(float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
            if (!vivo[i] || especie[i] != 13 || hinchado[i] < 0.6f) {
                continue;
            }
            float ox = x[i] - px, oy = y[i] - py, oz = z[i] - pz;
            if (ox * ox + oy * oy + oz * oz < 1.3f * 1.3f) {
                return true;
            }
        }
        return false;
    }

    /** every fish within r metres panics (Remora pulse) */
    void asustar(float px, float py, float pz, float r) {
        for (int i = 0; i < N; i++) {
            float ox = x[i] - px, oy = y[i] - py, oz = z[i] - pz;
            if (vivo[i] && ox * ox + oy * oy + oz * oz < r * r) {
                miedo[i] = 5;
            }
        }
    }

    /** index of a catchable fish (bubblefin) within reach of the hand, or -1 */
    int alcanzable(float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
            if (!vivo[i] || CAPTURA[especie[i]] < 0) {
                continue;
            }
            float dx = x[i] - px;
            float dy = y[i] - py;
            float dz = z[i] - pz;
            if (dx * dx + dy * dy + dz * dz < 1.8f * 1.8f) {
                return i;
            }
        }
        return -1;
    }
}
