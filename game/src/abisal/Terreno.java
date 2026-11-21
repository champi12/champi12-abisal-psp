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
     * World layout (a crater about 2 km across, centre = the escape pod):
     *   0 - 180 m      Bajios        shallows, sand and coral       -5 .. -30 m
     *   180 - 380 m    Bosque        ribbon kelp forest             -30 .. -95 m
     *   380 - 600 m    Meseta roja   red plateau, rock ridges       -95 .. -210 m
     *   600 - 850 m    Canones       dark canyons and pillars       -210 .. -460 m
     *   850 - 1000 m   Fosa          the abyss, bioluminescent      -460 .. -900 m
     *   > 1000 m       Vacio         open ocean: the floor falls away, nothing safe
     * Biome borders wobble with the angle so the rings do not look like rings.
     */
    public static final float BORDE = 1000f;
    public static final float[] LIMITE = { 180, 380, 600, 850, 1000 };
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
                return i;
            }
        }
        return VACIO;
    }

    public static final int CANONES = 3, ABISMO = 4, VACIO = 5;

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
            float lomas = (fbm(x * 0.012f, z * 0.012f, 81) - 0.5f) * 10;
            float cresta = fbm(x * 0.03f, z * 0.03f, 82);
            float crestas = cresta > 0.64f ? (cresta - 0.64f) * 22 : 0;
            h = base + n * 7 + ondas + (lomas + crestas) * calma;
        } else if (bio == 1) {
            h = base + n * 14;
        } else if (bio == 2) {
            // red plateau: flat tops broken by rock ridges
            float c = fbm(x * 0.03f, z * 0.03f, 11);
            h = base + n * 10 + (c > 0.56f ? (c - 0.56f) * 120 : 0);
        } else if (bio == 3) {
            // canyons: deep cuts and tall pillars
            float c = fbm(x * 0.015f, z * 0.015f, 12);
            float v = Math.abs(c - 0.5f);
            h = base + n * 25 - (v < 0.06f ? (0.06f - v) * 1500 : 0) + (c > 0.72f ? (c - 0.72f) * 500 : 0);
        } else {
            h = base + n * 30;
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

    /** the height grid of a chunk, relative to y0 (first half of the work) */
    public static float[] alturas(float x0, float z0, int cells, float y0) {
        int n = cells + 1;
        float[] h = new float[n * n];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                h[j * n + i] = altura(x0 + i * CELDA, z0 + j * CELDA) - y0;
            }
        }
        return h;
    }

    /** the mesh for a height grid (second half: colours and triangles) */
    public static Mesh mallaDe(float[] h, float x0, float z0, int cells, Fog fog) {
        MB b = new MB();
        b.scale = 1f / 64f; // +-512 m around y0 (abyss cliffs), 1.6 cm precision
        b.jitter = 5;
        // textured: one 64x64 detail tile every 4 m, tinted by the face colour
        b.uvEscala = 1f / 4f;
        b.textura = Texturas.suelo();
        int n = cells + 1;
        for (int j = 0; j < cells; j++) {
            for (int i = 0; i < cells; i++) {
                float ax = i * CELDA;
                float az = j * CELDA;
                float h00 = h[j * n + i];
                float h10 = h[j * n + i + 1];
                float h01 = h[(j + 1) * n + i];
                float h11 = h[(j + 1) * n + i + 1];
                float cx = x0 + ax + CELDA / 2;
                float cz = z0 + az + CELDA / 2;
                float pend = Math.min(1, (Math.abs(h00 - h11) + Math.abs(h10 - h01)) / (2 * CELDA));
                float hm = (h00 + h10 + h01 + h11) / 4;
                b.color = color(cx, cz, hm, pend);
                // alternate the diagonal for a less regular look
                if (((i + j) & 1) == 0) {
                    b.tri(ax, h00, az, ax, h01, az + CELDA, ax + CELDA, h11, az + CELDA);
                    b.tri(ax, h00, az, ax + CELDA, h11, az + CELDA, ax + CELDA, h10, az);
                } else {
                    b.tri(ax, h00, az, ax, h01, az + CELDA, ax + CELDA, h10, az);
                    b.tri(ax + CELDA, h10, az, ax, h01, az + CELDA, ax + CELDA, h11, az + CELDA);
                }
            }
        }
        return b.build(fog);
    }
}
