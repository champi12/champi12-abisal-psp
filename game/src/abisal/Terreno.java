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
    public static final int CANONES = 3, ABISMO = 4, LLANURA = 8, VACIO = 9, PRADERA = 6, TERMAL = 7,
            CRISTAL = 10, OSARIO = 11;

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
     * World layout: every zone appears ONCE, as its own region around the shallows (an
     * additively weighted Voronoi map with wobbly borders), so each zone lies in one
     * direction and the story has to send you all over the crater:
     *   centre        Bajios             shallows, sand and coral       -5 .. -27 m
     *   NE  ~900 m    Bosque de algas    kelp forest                    ~ -55 m
     *   SW  ~900 m    Praderas de arena  dunes and seagrass             ~ -45 m
     *   N   ~950 m    Arrecife de cristal glass spires                  ~ -130 m
     *   S   ~1300 m   Meseta roja        red terraces                   ~ -150 m
     *   W   ~1700 m   Canones            dark canyons and pillars       ~ -300 m
     *   E   ~1700 m   Grietas termales   basalt and hot vents           ~ -320 m
     *   NNE ~1900 m   Osario             bones of the old colossi       ~ -380 m
     *   SSW ~2150 m   Fosa abisal        the trench (to -850 m)
     *   ENE ~2250 m   Llanura exterior   the outer shelf                ~ -240 m
     *   > BORDE       Vacio              the floor falls away
     * Neighbouring zones blend over a band ~90 m each side of the border (slopes and cliffs).
     */
    public static final float BORDE = 2700f;
    /** zone seeds: x, z (m) and size bonus (m), indexed by zone id (NaN = not a region) */
    static final float[] ZX = new float[12], ZZ = new float[12], ZR = new float[12];
    static final int[] ZONAS = { BAJIOS, BOSQUE, MESETA, CANONES, ABISMO, PRADERA, TERMAL, LLANURA, CRISTAL, OSARIO };
    private static void zona(int id, float grados, float dist, float bonus) {
        float a = grados * 0.017453292f;
        ZX[id] = (float) Math.cos(a) * dist;
        ZZ[id] = (float) Math.sin(a) * dist;
        ZR[id] = bonus;
    }
    static {
        zona(BAJIOS, 0, 0, 300);
        zona(BOSQUE, 30, 900, 150);
        zona(PRADERA, 150, 900, 150);          // praderas
        zona(CRISTAL, 270, 950, 150);         // arrecife de cristal
        zona(MESETA, 90, 1300, 200);
        zona(CANONES, 210, 1700, 200);
        zona(TERMAL, 330, 1700, 160);         // grietas termales
        zona(OSARIO, 285, 1900, 180);        // osario
        zona(ABISMO, 125, 2150, 260);
        zona(LLANURA, 40, 2250, 200);          // llanura exterior
    }
    static final float BANDA = 90;

    /** results of the last zonas() call: nearest zone, second nearest, and their scores */
    private static int z1, z2;
    private static float s1, s2, ultX = Float.NaN, ultZ = Float.NaN;

    /** finds the two zones closest to (x, z) (borders wobble with a warp of the point) */
    private static void zonas(float x, float z) {
        if (x == ultX && z == ultZ) {
            return;
        }
        ultX = x;
        ultZ = z;
        float wx = x + (ruido(x * 0.003f, z * 0.003f, 9) - 0.5f) * 260 + (ruido(x * 0.013f, z * 0.013f, 10) - 0.5f) * 50;
        float wz = z + (ruido(x * 0.003f, z * 0.003f, 19) - 0.5f) * 260 + (ruido(x * 0.013f, z * 0.013f, 20) - 0.5f) * 50;
        float a = Float.MAX_VALUE, b = Float.MAX_VALUE;
        int ia = 0, ib = 0;
        for (int k = 0; k < ZONAS.length; k++) {
            int id = ZONAS[k];
            float dx = wx - ZX[id], dz = wz - ZZ[id];
            float sc = j2me.FMath.sqrt(dx * dx + dz * dz) - ZR[id];
            if (sc < a) {
                b = a;
                ib = ia;
                a = sc;
                ia = id;
            } else if (sc < b) {
                b = sc;
                ib = id;
            }
        }
        z1 = ia;
        z2 = ib;
        s1 = a;
        s2 = b;
    }

    /** distance from the centre with the border wobble applied (for the void's edge) */
    static float radio(float x, float z) {
        float r = j2me.FMath.sqrt(x * x + z * z);
        float w = (fbm(x * 0.004f, z * 0.004f, 9) - 0.5f) * 160 + (fbm(x * 0.02f, z * 0.02f, 10) - 0.5f) * 40;
        return r + w;
    }

    public static int bioma(float x, float z) {
        if (radio(x, z) >= BORDE) {
            return VACIO;
        }
        zonas(x, z);
        return z1;
    }

    /** the centre (seed) of a zone */
    public static float centroX(int zona) {
        return ZX[zona];
    }

    public static float centroZ(int zona) {
        return ZZ[zona];
    }

    /**
     * A point of the given zone: from its centre, towards the given heading (degrees, x =
     * cos, z = sin) at rel * 520 m, then searched around it in a widening spiral for a spot
     * that is that zone and not deeper than minY. out = {x, z}; false if none was found (out
     * then holds the first point of the zone seen, or the centre).
     */
    public static boolean buscarPunto(int zona, float grados, float rel, float minY, float[] out) {
        float cx = ZX[zona], cz = ZZ[zona];
        float a = grados * 0.017453292f;
        float ox = cx + (float) Math.cos(a) * rel * 520, oz = cz + (float) Math.sin(a) * rel * 520;
        boolean visto = false;
        out[0] = cx;
        out[1] = cz;
        for (int anillo = 0; anillo < 16; anillo++) {
            int n = anillo == 0 ? 1 : 8 + anillo * 2;
            for (int k = 0; k < n; k++) {
                float b = k * 6.2831853f / n + anillo * 0.7f;
                float x = ox + (float) Math.cos(b) * anillo * 30, z = oz + (float) Math.sin(b) * anillo * 30;
                if (bioma(x, z) != zona) {
                    continue;
                }
                if (!visto) {
                    visto = true;
                    out[0] = x;
                    out[1] = z;
                }
                if (altura(x, z) > minY) {
                    out[0] = x;
                    out[1] = z;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 0..1 radiation at a point: the thermal field is the hot spot, the deep trench and the
     * canyons carry a lighter dose, and every shallower zone is clean. Deeper water adds a
     * bit more, so the bottom of the trench is the worst place to be.
     */
    public static float radiacion(float x, float z, float y) {
        int bio = bioma(x, z);
        float r = 0;
        if (bio == TERMAL) {
            r = 0.55f + fbm(x * 0.02f, z * 0.02f, 91) * 0.45f;
        } else if (bio == ABISMO) {
            r = 0.3f + fbm(x * 0.015f, z * 0.015f, 92) * 0.35f;
        } else if (bio == CANONES || bio == OSARIO) {
            r = 0.1f + fbm(x * 0.02f, z * 0.02f, 93) * 0.2f;
        }
        if (r > 0 && y < -120) {
            r += Math.min(0.25f, (-y - 120) / 1200f);
        }
        return r > 1 ? 1 : r;
    }


    private static float suave(float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    /** Height of the sea floor (negative, metres below the surface). */
    public static float altura(float x, float z) {
        zonas(x, z);
        int a = z1, b = z2;
        float e = s2 - s1;
        float h = relieve(a, x, z);
        if (e < 2 * BANDA) {
            // near a border: blend into the neighbour (0.5 each right on the border)
            float w = 0.5f * (1 - suave(e / (2 * BANDA)));
            h += (relieve(b, x, z) - h) * w;
        }
        float r = radio(x, z);
        if (r > BORDE - 40) {
            // the void: the floor drops out of sight
            h -= suave((r - BORDE + 40) / 100) * 2000;
        }
        return Math.min(h, -3.5f);
    }

    /** the floor of zone bio at (x, z), as if that zone went on forever */
    private static float relieve(int bio, float x, float z) {
        float n = fbm(x * 0.03f, z * 0.03f, 3) - 0.5f;
        float dx = x - ZX[bio], dz = z - ZZ[bio];
        float dc = j2me.FMath.sqrt(dx * dx + dz * dz);
        switch (bio) {
        case BAJIOS: {
            float base = -5 - 22 * Math.min(1, dc / 470);
            float ondas = j2me.FMath.sin(x * 0.45f + fbm(x * 0.05f, z * 0.05f, 5) * 6) * 0.35f;
            // relief inside the shallows: sand basins and mounds, and rocky crests that rise
            // in steep steps (their slopes get the rock colour); calmer near the pod
            float calma = Math.min(1, Math.max(0, (dc - 25) / 50));
            float lomas = (fbm(x * 0.012f, z * 0.012f, 81) - 0.5f) * 13;
            float cresta = fbm(x * 0.03f, z * 0.03f, 82);
            float crestas = cresta > 0.62f ? (cresta - 0.62f) * 28 : 0;
            float ondulacion = (fbm(x * 0.07f, z * 0.07f, 84) - 0.5f) * 1.2f;
            return base + n * 8 + ondas + ondulacion + (lomas + crestas) * calma;
        }
        case BOSQUE: {
            // kelp forest: rolling hills with rocky outcrops poking out of them
            float colinas = (fbm(x * 0.014f, z * 0.014f, 91) - 0.5f) * 22;
            float aflora = fbm(x * 0.04f, z * 0.04f, 92);
            return -55 + n * 12 + colinas + (aflora > 0.64f ? (aflora - 0.64f) * 45 : 0);
        }
        case PRADERA: {
            // sand meadows: long dunes and soft ripples, no rock
            float dir = fbm(x * 0.004f, z * 0.004f, 97) * 2;
            float duna = j2me.FMath.sin((x * 0.7f + z * 0.7f) * 0.06f + dir * 3) * 5
                    + j2me.FMath.sin(x * 0.35f + fbm(x * 0.05f, z * 0.05f, 98) * 5) * 0.4f;
            return -45 + n * 6 + duna + (fbm(x * 0.01f, z * 0.01f, 99) - 0.5f) * 14;
        }
        case MESETA: {
            // red plateau: flat tops broken by rock ridges
            float c = fbm(x * 0.03f, z * 0.03f, 11);
            // terraces: the height is quantised into 6 m steps with steep risers
            float t = (fbm(x * 0.01f, z * 0.01f, 93) - 0.5f) * 40;
            float paso = 6;
            float q = t / paso;
            float fl = j2me.FMath.floor(q);
            float fr = q - fl;
            float terraza = (fl + suave((fr - 0.75f) * 4)) * paso;
            return -150 + n * 6 + terraza + (c > 0.56f ? (c - 0.56f) * 120 : 0);
        }
        case CANONES: {
            // canyons: deep cuts and tall pillars
            float c = fbm(x * 0.015f, z * 0.015f, 12);
            float v = Math.abs(c - 0.5f);
            return -300 + n * 25 - (v < 0.06f ? (0.06f - v) * 1500 : 0) + (c > 0.72f ? (c - 0.72f) * 500 : 0);
        }
        case TERMAL: {
            // thermal rifts: flat basalt shelves split by deep glowing cracks
            float g = fbm(x * 0.02f, z * 0.02f, 15);
            float gv = Math.abs(g - 0.5f);
            float escalon = j2me.FMath.floor((fbm(x * 0.008f, z * 0.008f, 16) - 0.5f) * 6) * 9;
            return -320 + n * 5 + escalon - (gv < 0.035f ? (0.035f - gv) * 1400 : 0);
        }
        case LLANURA: {
            // exterior plain: pale cold sand with low ridges and long shallow dunes
            float colinas = (fbm(x * 0.01f, z * 0.01f, 71) - 0.5f) * 26;
            float dunas = j2me.FMath.sin(x * 0.05f + fbm(x * 0.02f, z * 0.02f, 72) * 5) * 2.2f;
            float grietas = fbm(x * 0.006f, z * 0.006f, 73);
            return -240 + n * 9 + colinas + dunas + (grietas > 0.72f ? (grietas - 0.72f) * 120 : 0);
        }
        case CRISTAL: {
            // glass reef: a basin of dark silt with low glassy ridges and round hollows
            float cuenca = (fbm(x * 0.008f, z * 0.008f, 101) - 0.5f) * 30;
            float loma = fbm(x * 0.025f, z * 0.025f, 102);
            float hoyo = fbm(x * 0.05f, z * 0.05f, 103);
            return -130 + n * 6 + cuenca + (loma > 0.6f ? (loma - 0.6f) * 70 : 0)
                    - (hoyo > 0.7f ? (hoyo - 0.7f) * 40 : 0);
        }
        case OSARIO: {
            // the ossuary: a wide grey bowl with low mounds of silt over the old bones
            float cuenco = -40 * (1 - Math.min(1, dc / 700));
            float monticulo = fbm(x * 0.02f, z * 0.02f, 111);
            return -360 + cuenco + n * 10 + (monticulo > 0.58f ? (monticulo - 0.58f) * 45 : 0);
        }
        default: {
            // abyss: a trench deepest at its centre, broken slopes, ridges and rock spires
            float base = -480 - 370 * (1 - suave(dc / 650));
            float cr = fbm(x * 0.025f, z * 0.025f, 95);
            float aguja = fbm(x * 0.08f, z * 0.08f, 96);
            return base + n * 30 + (cr > 0.6f ? (cr - 0.6f) * 90 : 0) + (aguja > 0.78f ? (aguja - 0.78f) * 160 : 0);
        }
        }
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
        case LLANURA: {
            // the outer shelf: cold pale sand, dark basalt ridges, no algae at all
            if (roca) {
                return mezcla(0x555c63, 0x3f464d, n);
            }
            return mezcla(0xa9b1ac, 0x8d9792, n);
        }
        case CRISTAL: {
            // dark blue silica silt; the slopes are glassy and catch a cold light
            if (roca) {
                return mezcla(0x5a88b8, 0x7ab8e0, n);
            }
            float brillo = fbm(x * 0.04f, z * 0.04f, 66);
            return brillo > 0.6f ? mezcla(0x34426a, 0x4a76a8, Math.min(1, (brillo - 0.6f) * 4)) : mezcla(0x2c3656, 0x36405e, n);
        }
        case OSARIO: {
            // pale grey bone silt with darker ash
            if (roca) {
                return mezcla(0x8a8476, 0x6e695e, n);
            }
            return mezcla(0xa49e8c, 0x8a8576, n);
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
    /**
     * Height of the drawn terrain mesh at (x, z): the same triangles as filaMalla (4 m cells,
     * alternating diagonal), not the smooth height function. Props placed with this sit on
     * the visible ground even on sharp peaks.
     */
    public static float alturaMalla(float x, float z) {
        int gx = j2me.FMath.floor(x / CELDA), gz = j2me.FMath.floor(z / CELDA);
        float fx = x / CELDA - gx, fz = z / CELDA - gz;
        float x0 = gx * CELDA, z0 = gz * CELDA;
        float h00 = altura(x0, z0), h10 = altura(x0 + CELDA, z0);
        float h01 = altura(x0, z0 + CELDA), h11 = altura(x0 + CELDA, z0 + CELDA);
        if (((gx + gz) & 1) == 0) {
            // diagonal from (0,0) to (1,1)
            if (fz >= fx) {
                return h00 + (h11 - h01) * fx + (h01 - h00) * fz;
            }
            return h00 + (h10 - h00) * fx + (h11 - h10) * fz;
        }
        // diagonal from (1,0) to (0,1)
        if (fx + fz <= 1) {
            return h00 + (h10 - h00) * fx + (h01 - h00) * fz;
        }
        return h11 + (h01 - h11) * (1 - fx) + (h10 - h11) * (1 - fz);
    }

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
