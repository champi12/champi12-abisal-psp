package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Mesh;

/**
 * The sea floor: a deterministic height field over the 512 x 512 m world (centre = the
 * escape pod), plus the colour of each biome. Chunks are built as flat-shaded meshes.
 */
public final class Terreno {
    public static final float CELDA = 4f;     // metres per grid cell
    public static final int CHUNK = 12;       // cells per chunk side (48 m)

    public static final int BAJIOS = 0, BOSQUE = 1, MESETA = 2;

    private Terreno() {
    }

    // ------------------------------------------------------------------ noise

    private static int hash(int x, int z, int s) {
        int h = x * 374761393 + z * 668265263 + s * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    /** value noise in [0, 1] */
    static float ruido(float x, float z, int s) {
        int ix = j2me.FMath.floor(x);
        int iz = j2me.FMath.floor(z);
        float fx = x - ix;
        float fz = z - iz;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = (hash(ix, iz, s) & 0xffff) / 65535f;
        float b = (hash(ix + 1, iz, s) & 0xffff) / 65535f;
        float c = (hash(ix, iz + 1, s) & 0xffff) / 65535f;
        float d = (hash(ix + 1, iz + 1, s) & 0xffff) / 65535f;
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    static float fbm(float x, float z, int s) {
        return ruido(x, z, s) * 0.55f + ruido(x * 2.1f, z * 2.1f, s + 1) * 0.3f
                + ruido(x * 4.3f, z * 4.3f, s + 2) * 0.15f;
    }

    /** 0..1 random value for a grid point (used to place props) */
    public static float azar(int x, int z, int s) {
        return (hash(x, z, s) & 0xffff) / 65535f;
    }

    // ------------------------------------------------------------------ shape

    /*
     * World layout (a crater about 3 km across, centre = the escape pod):
     *   0 - 270 m      Bajios        shallows, sand and coral       -5 .. -30 m
     *   270 - 570 m    Bosque        ribbon kelp forest             -30 .. -95 m
     *                  (a sector of it: Praderas de arena, dunes and seagrass)
     *   570 - 900 m    Meseta roja   red plateau, rock ridges       -95 .. -210 m
     *   900 - 1270 m   Canones       dark canyons and pillars       -210 .. -460 m
     *                  (a sector of it: Grietas termales, basalt and hot vents)
     *   1270 - 1500 m  Fosa          the abyss, bioluminescent      -460 .. -900 m
     *   > 1500 m       Vacio         open ocean: the floor falls away, nothing safe
     * Biome borders wobble with the angle so the rings do not look like rings.
     */
    public static final float BORDE = 1500f;
    public static final float[] LIMITE = { 270, 570, 900, 1270, 1500 };
    public static final float[] PROF = { -5, -30, -95, -210, -460, -900 };

    /** distance from the centre with the border wobble applied */
    static float radio(float x, float z) {
        float r = j2me.FMath.sqrt(x * x + z * z);
        float w = (fbm(x * 0.004f, z * 0.004f, 9) - 0.5f) * 160 + (fbm(x * 0.02f, z * 0.02f, 10) - 0.5f) * 40;
        return r + w;
    }

    public static int bioma(float x, float z) {
        float r = radio(x, z);
        for (int i = 0; i < LIMITE.length; i++) {
            if (r < LIMITE[i]) {
                if (i == BOSQUE && pesoSector(x, z, PRADERA_A0, PRADERA_A1) > 0.5f) {
                    return PRADERA;
                }
                if (i == CANONES && pesoSector(x, z, TERMAL_A0, TERMAL_A1) > 0.5f) {
                    return TERMAL;
                }
                return i;
            }
        }
        return VACIO;
    }

    public static final int CANONES = 3, ABISMO = 4, VACIO = 5, PRADERA = 6, TERMAL = 7;
    /** angular sectors (degrees) of the two regional biomes inside their rings */
    static final float PRADERA_A0 = 195, PRADERA_A1 = 285, TERMAL_A0 = 35, TERMAL_A1 = 120;

    /**
     * 0..1: how far inside an angular sector a point is, with a soft 12 degree edge and a
     * wobble so the sector border is not a straight line.
     */
    static float pesoSector(float x, float z, float a0, float a1) {
        float a = j2me.FMath.atan2(z, x) * 57.29578f;
        if (a < 0) {
            a += 360;
        }
        a += (fbm(x * 0.006f, z * 0.006f, 14) - 0.5f) * 30;
        float k = Math.min(a - a0, a1 - a) / 12f;
        return k <= 0 ? 0 : (k >= 1 ? 1 : k);
    }

    private static float suave(float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    /** Height of the sea floor (negative, metres below the surface). */
    public static float altura(float x, float z) {
        float r = radio(x, z);
        float base;
        int bio = 0;
        while (bio < LIMITE.length && r >= LIMITE[bio]) {
            bio++;
        }
        if (bio >= LIMITE.length) {
            // the void: the floor drops out of sight
            base = PROF[5] - suave((r - BORDE) / 60) * 2000;
        } else {
            float r0 = bio == 0 ? 0 : LIMITE[bio - 1];
            float t = (r - r0) / (LIMITE[bio] - r0);
            // each biome is a gentle slope with a steeper drop into the next one
            float k = t < 0.8f ? t / 0.8f * 0.35f : 0.35f + suave((t - 0.8f) / 0.2f) * 0.65f;
            base = PROF[bio] + (PROF[bio + 1] - PROF[bio]) * k;
        }
        float n = fbm(x * 0.03f, z * 0.03f, 3) - 0.5f;
        float h;
        if (bio == 0) {
            float ondas = j2me.FMath.sin(x * 0.45f + fbm(x * 0.05f, z * 0.05f, 5) * 6) * 0.35f;
            // relief inside the shallows: sand basins and mounds, and rocky crests that rise
            // in steep steps (their slopes get the rock colour); calmer near the pod
            float calma = Math.min(1, Math.max(0, (r - 25) / 50));
            float lomas = (fbm(x * 0.012f, z * 0.012f, 81) - 0.5f) * 13;
            float cresta = fbm(x * 0.03f, z * 0.03f, 82);
            float crestas = cresta > 0.62f ? (cresta - 0.62f) * 28 : 0;
            h = base + n * 7 + ondas + (lomas + crestas) * calma;
        } else if (bio == 1) {
            // kelp forest: rolling hills with rocky outcrops poking out of them
            float colinas = (fbm(x * 0.014f, z * 0.014f, 91) - 0.5f) * 22;
            float aflora = fbm(x * 0.04f, z * 0.04f, 92);
            h = base + n * 12 + colinas + (aflora > 0.64f ? (aflora - 0.64f) * 45 : 0);
            float w = pesoSector(x, z, PRADERA_A0, PRADERA_A1);
            if (w > 0) {
                // sand meadows: long dunes and soft ripples, no rock
                float dir = fbm(x * 0.004f, z * 0.004f, 97) * 2;
                float duna = j2me.FMath.sin((x * 0.7f + z * 0.7f) * 0.06f + dir * 3) * 5
                        + j2me.FMath.sin(x * 0.35f + fbm(x * 0.05f, z * 0.05f, 98) * 5) * 0.4f;
                float hp = base + n * 6 + duna + (fbm(x * 0.01f, z * 0.01f, 99) - 0.5f) * 14;
                h += (hp - h) * w;
            }
        } else if (bio == 2) {
            // red plateau: flat tops broken by rock ridges
            float c = fbm(x * 0.03f, z * 0.03f, 11);
            // terraces: the height is quantised into 6 m steps with steep risers
            float t = (fbm(x * 0.01f, z * 0.01f, 93) - 0.5f) * 40;
            float paso = 6;
            float q = t / paso;
            float fl = j2me.FMath.floor(q);
            float fr = q - fl;
            float terraza = (fl + suave((fr - 0.75f) * 4)) * paso;
            h = base + n * 6 + terraza + (c > 0.56f ? (c - 0.56f) * 120 : 0);
        } else if (bio == 3) {
            // canyons: deep cuts and tall pillars
            float c = fbm(x * 0.015f, z * 0.015f, 12);
            float v = Math.abs(c - 0.5f);
            h = base + n * 25 - (v < 0.06f ? (0.06f - v) * 1500 : 0) + (c > 0.72f ? (c - 0.72f) * 500 : 0);
            float w = pesoSector(x, z, TERMAL_A0, TERMAL_A1);
            if (w > 0) {
                // thermal rifts: flat basalt shelves split by deep glowing cracks
                float g = fbm(x * 0.02f, z * 0.02f, 15);
                float gv = Math.abs(g - 0.5f);
                float escalon = j2me.FMath.floor((fbm(x * 0.008f, z * 0.008f, 16) - 0.5f) * 6) * 9;
                float ht = base + n * 5 + escalon - (gv < 0.035f ? (0.035f - gv) * 1400 : 0);
                h += (ht - h) * w;
            }
        } else {
            // abyss: broken slopes, ridges and rock spires
            float cr = fbm(x * 0.025f, z * 0.025f, 95);
            float aguja = fbm(x * 0.08f, z * 0.08f, 96);
            h = base + n * 30 + (cr > 0.6f ? (cr - 0.6f) * 90 : 0) + (aguja > 0.78f ? (aguja - 0.78f) * 160 : 0);
        }
        return Math.min(h, -3.5f);
    }

    /** floor colour for a face at height h and slope (0 flat .. 1 vertical) */
    public static int color(float x, float z, float h, float pend) {
        int bio = bioma(x, z);
        float n = fbm(x * 0.08f, z * 0.08f, 21);
        boolean roca = pend > 0.35f;
        switch (bio) {
        case BAJIOS: {
            if (roca) {
                return mezcla(0x9a8f86, 0x7d746e, n);
            }
            // sand with patches of green moss/algae growing on the floor (colour only)
            int arena = mezcla(0xd8c08a, 0xb9a06e, n);
            float musgo = fbm(x * 0.045f, z * 0.045f, 63);
            if (musgo > 0.56f) {
                float k = Math.min(1, (musgo - 0.56f) * 7);
                return mezcla(arena, mezcla(0x7c9a48, 0x5d7d3a, n), k);
            }
            return arena;
        }
        case BOSQUE: {
            if (roca) {
                return mezcla(0x6d6a72, 0x56525e, n);
            }
            int fondo = mezcla(0x8f8a5e, 0x6f7a4c, n);
            float musgo = fbm(x * 0.05f, z * 0.05f, 64);
            return musgo > 0.5f ? mezcla(fondo, 0x4f6e32, Math.min(1, (musgo - 0.5f) * 5)) : fondo;
        }
        case MESETA:
            return roca ? mezcla(0x5a4a55, 0x42384a, n) : mezcla(0x9a4e3c, 0x74403a, n);
        case CANONES:
            return roca ? mezcla(0x4a4860, 0x3a3850, n) : mezcla(0x5a6a78, 0x485866, n);
        case PRADERA: {
            // pale sand with seagrass meadows
            int arena = mezcla(0xe0cc98, 0xc8b07c, n);
            float pasto = fbm(x * 0.03f, z * 0.03f, 65);
            return pasto > 0.48f ? mezcla(arena, mezcla(0x6f9a40, 0x557c34, n), Math.min(1, (pasto - 0.48f) * 4)) : arena;
        }
        case TERMAL: {
            // black basalt; the walls of the cracks glow orange-red with the heat below
            if (roca) {
                return mezcla(0x8a2a10, 0xd8561a, Math.min(1, (pend - 0.35f) * 2.5f) * (0.6f + 0.4f * n));
            }
            return mezcla(0x2a2426, 0x3c3032, n);
        }
        default:
            return roca ? mezcla(0x3a3852, 0x2e2c44, n) : mezcla(0x40486a, 0x343a58, n);
        }
    }

    static int mezcla(int a, int b, float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /**
     * Flat-shaded mesh of the floor between (x0, z0) and (x0 + cells * CELDA, ...), in
     * coordinates relative to (x0, 0, z0).
     */
    public static Mesh malla(float x0, float z0, int cells, Fog fog) {
        return malla(x0, z0, cells, 0, fog);
    }

    /**
     * Terrain mesh with heights stored relative to y0 (place it at (x0, y0, z0)): vertex
     * positions are 16-bit, so absolute depths below ~128 m would be clamped.
     */
    public static Mesh malla(float x0, float z0, int cells, float y0, Fog fog) {
        return mallaDe(alturas(x0, z0, cells, y0), x0, z0, cells, fog);
    }

    /** the height grid of a chunk, relative to y0 */
    public static float[] alturas(float x0, float z0, int cells, float y0) {
        float[] h = new float[(cells + 1) * (cells + 1)];
        for (int j = 0; j <= cells; j++) {
            filaAlturas(h, x0, z0, cells, y0, j);
        }
        return h;
    }

    /** one row of the height grid (the work is spread over frames) */
    public static void filaAlturas(float[] h, float x0, float z0, int cells, float y0, int j) {
        int n = cells + 1;
        for (int i = 0; i < n; i++) {
            h[j * n + i] = altura(x0 + i * CELDA, z0 + j * CELDA) - y0;
        }
    }

    public static MB nuevoMB() {
        MB b = new MB();
        b.scale = 1f / 64f; // +-512 m around y0 (abyss cliffs), 1.6 cm precision
        b.jitter = 5;
        // textured: one 64x64 detail tile every 4 m, tinted by the face colour
        b.uvEscala = 1f / 4f;
        b.textura = Texturas.suelo();
        b.suave = true;
        return b;
    }

    /** colour of a grid vertex: slope from the neighbouring heights, so colours blend */
    private static int colorVertice(float[] h, float x0, float z0, int cells, int i, int j) {
        int n = cells + 1;
        int i0 = i > 0 ? i - 1 : i, i1 = i < cells ? i + 1 : i;
        int j0 = j > 0 ? j - 1 : j, j1 = j < cells ? j + 1 : j;
        float gx = (h[j * n + i1] - h[j * n + i0]) / ((i1 - i0) * CELDA);
        float gz = (h[j1 * n + i] - h[j0 * n + i]) / ((j1 - j0) * CELDA);
        float pend = Math.min(1, (Math.abs(gx) + Math.abs(gz)) * 0.6f);
        return color(x0 + i * CELDA, z0 + j * CELDA, h[j * n + i], pend);
    }

    /** the triangles of one row of cells */
    // vertex colours of the two rows being meshed: each row is computed once and reused
    private static int[] filaA = new int[64], filaB = new int[64];
    private static float[] filaH;
    private static int filaJ = -2;

    public static void filaMalla(MB b, float[] h, float x0, float z0, int cells, int j) {
        int n = cells + 1;
        if (h == filaH && j == filaJ + 1) {
            int[] t = filaA;
            filaA = filaB;
            filaB = t;
        } else {
            for (int i = 0; i < n; i++) {
                filaA[i] = colorVertice(h, x0, z0, cells, i, j);
            }
        }
        for (int i = 0; i < n; i++) {
            filaB[i] = colorVertice(h, x0, z0, cells, i, j + 1);
        }
        filaH = h;
        filaJ = j;
        for (int i = 0; i < cells; i++) {
            float ax = i * CELDA;
            float az = j * CELDA;
            float h00 = h[j * n + i];
            float h10 = h[j * n + i + 1];
            float h01 = h[(j + 1) * n + i];
            float h11 = h[(j + 1) * n + i + 1];
            float cx = x0 + ax + CELDA / 2;
            float cz = z0 + az + CELDA / 2;
            int c00 = filaA[i];
            int c10 = filaA[i + 1];
            int c01 = filaB[i];
            int c11 = filaB[i + 1];
            // alternate the diagonal for a less regular look; colours per vertex blend softly
            if (((i + j) & 1) == 0) {
                b.triCol(ax, h00, az, c00, ax, h01, az + CELDA, c01, ax + CELDA, h11, az + CELDA, c11);
                b.triCol(ax, h00, az, c00, ax + CELDA, h11, az + CELDA, c11, ax + CELDA, h10, az, c10);
            } else {
                b.triCol(ax, h00, az, c00, ax, h01, az + CELDA, c01, ax + CELDA, h10, az, c10);
                b.triCol(ax + CELDA, h10, az, c10, ax, h01, az + CELDA, c01, ax + CELDA, h11, az + CELDA, c11);
            }
        }
    }

    public static Mesh mallaDe(float[] h, float x0, float z0, int cells, Fog fog) {
        MB b = nuevoMB();
        for (int j = 0; j < cells; j++) {
            filaMalla(b, h, x0, z0, cells, j);
        }
        return b.build(fog);
    }
}
