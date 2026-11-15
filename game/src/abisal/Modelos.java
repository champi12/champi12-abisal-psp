package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Mesh;

/** Procedural low-poly models. Every model is original and built only from code. */
public final class Modelos {
    private Modelos() {
    }

    public static final String[] NOMBRES = { "capsula", "fondo_bajios", "roca_cobre", "roca_cuarzo",
            "coral_abanico", "coral_tubos", "alga_cinta", "pez_burbujin" };
    /** gallery framing: camera distance, height of the point looked at, show sand floor */
    public static final float[] DIST = { 9, 22, 4.5f, 4f, 4.5f, 4.5f, 13, 1.6f };
    public static final float[] ALTO = { 0.5f, 3, 0.6f, 0.6f, 1.0f, 0.8f, 5f, 0f };
    public static final boolean[] SUELO = { false, false, true, true, true, true, true, false };

    public static Mesh crear(String nombre, Fog fog) {
        if ("capsula".equals(nombre)) {
            return capsula(fog);
        }
        if ("fondo_bajios".equals(nombre)) {
            return Terreno.malla(-60, -60, 30, fog);
        }
        if ("roca_cobre".equals(nombre)) {
            return rocaVeta(fog, 0x8d8790, 0xd27a3c, false, 3);
        }
        if ("roca_cuarzo".equals(nombre)) {
            return rocaVeta(fog, 0x7f8794, 0xf2d6e8, true, 8);
        }
        if ("coral_abanico".equals(nombre)) {
            return coralAbanico(fog, 1);
        }
        if ("coral_tubos".equals(nombre)) {
            return coralTubos(fog, 2);
        }
        if ("alga_cinta".equals(nombre)) {
            return algaCinta(fog, 4);
        }
        if ("pez_burbujin".equals(nombre)) {
            return pezBurbujin(fog);
        }
        return null;
    }

    /** small sand disc used by the gallery under seabed props */
    public static Mesh suelo(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.color = 0xcdb684;
        int seg = 12;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * 2 * Math.PI / seg);
            float a1 = (float) ((i + 1) * 2 * Math.PI / seg);
            b.tri(0, 0, 0, (float) Math.cos(a0) * 5, 0, -(float) Math.sin(a0) * 5, (float) Math.cos(a1) * 5, 0,
                    -(float) Math.sin(a1) * 5);
        }
        return b.build(fog);
    }

    /**
     * Mineral outcrop: a rough rock half buried in the sand with crystals of ore poking
     * out of it. Breaking it with the hand yields the ore.
     */
    static Mesh rocaVeta(Fog fog, int piedra, int mineral, boolean puntas, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        b.color = piedra;
        b.move(0, 0.35f, 0);
        b.roca(1.0f, 0.8f, 0.9f, 9, 6, semilla, 0.22f);
        b.jitter = 14;
        b.color = mineral;
        for (int i = 0; i < 6; i++) {
            float a = i * 61 + semilla * 13;
            float e = 25 + (i * 37 % 40);
            b.identity();
            b.move(0, 0.35f, 0);
            b.rot(1, a);
            b.rot(0, -e);
            if (puntas) {
                b.move(0, 0.6f, 0);
                b.cristal(0.13f + (i % 3) * 0.04f, 0.6f + (i % 2) * 0.4f);
            } else {
                b.move(0, 0.78f, 0);
                b.roca(0.3f, 0.16f, 0.26f, 5, 3, semilla * 7 + i, 0.3f);
            }
        }
        return b.build(fog);
    }

    /** Fan coral: a lattice of branches spreading in a vertical plane, violet to pink. */
    static Mesh coralAbanico(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        int base = 0x6a3fb0;
        int punta = 0xe46fb4;
        // trunk
        b.color = base;
        b.box(0, 0.25f, 0, 0.16f, 0.5f, 0.12f);
        // branches: thin double-sided quads fanning out from the trunk top
        int ramas = 13;
        for (int i = 0; i < ramas; i++) {
            float a = -70 + 140f * i / (ramas - 1) + (Terreno.azar(i, semilla, 3) - 0.5f) * 10;
            float len = 1.3f + Terreno.azar(i, semilla, 4) * 0.6f;
            float rad = a * 0.017453292f;
            float dx = (float) Math.sin(rad);
            float dy = (float) Math.cos(rad);
            float x0 = 0;
            float y0 = 0.45f;
            int pasos = 4;
            for (int k = 0; k < pasos; k++) {
                float t0 = (float) k / pasos;
                float t1 = (float) (k + 1) / pasos;
                float w0 = 0.13f * (1 - t0) + 0.05f;
                float w1 = 0.13f * (1 - t1) + 0.05f;
                b.color = Terreno.mezcla(base, punta, t1);
                float ax = x0 + dx * len * t0;
                float ay = y0 + dy * len * t0;
                float bx = x0 + dx * len * t1;
                float by = y0 + dy * len * t1;
                b.quad(ax - dy * w0, ay + dx * w0, 0, ax + dy * w0, ay - dx * w0, 0, bx + dy * w1, by - dx * w1, 0,
                        bx - dy * w1, by + dx * w1, 0);
            }
            // cross webs between neighbouring branches
            if (i > 0) {
                float pa = (-70 + 140f * (i - 1) / (ramas - 1)) * 0.017453292f;
                for (int k = 1; k <= 3; k++) {
                    float m = len * (0.25f + 0.22f * k);
                    float g = 0.035f;
                    b.color = Terreno.mezcla(base, punta, 0.25f * k);
                    b.quad((float) Math.sin(pa) * m, y0 + (float) Math.cos(pa) * m - g, 0, dx * m,
                            y0 + dy * m - g, 0, dx * m, y0 + dy * m + g, 0, (float) Math.sin(pa) * m,
                            y0 + (float) Math.cos(pa) * m + g, 0);
                }
            }
        }
        return b.build(fog, 255, true, 0);
    }

    /** Tube coral: a cluster of open tubes, coral orange outside with glowing cyan rims. */
    static Mesh coralTubos(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 8;
        for (int i = 0; i < 7; i++) {
            float a = i * 51.4f + Terreno.azar(i, semilla, 1) * 30;
            float d = i == 0 ? 0 : 0.35f + Terreno.azar(i, semilla, 2) * 0.25f;
            float h = 0.6f + Terreno.azar(i, semilla, 5) * 1.1f;
            float r = 0.12f + Terreno.azar(i, semilla, 6) * 0.08f;
            b.identity();
            b.rot(1, a);
            b.move(0, 0, d);
            b.rot(0, d * 25);
            b.brillo = false;
            b.color = 0xe0663c;
            b.lathe(new float[] { r * 1.2f, 0f, r, h * 0.5f, r * 1.15f, h }, 7, null);
            // inside of the tube (visible from above)
            b.color = 0x5a1c24;
            b.lathe(new float[] { r * 1.05f, h, 0.01f, h - 0.15f }, 7, null);
            // glowing rim
            b.brillo = true;
            b.color = 0x5ff2ff;
            b.lathe(new float[] { r * 1.15f, h, r * 1.3f, h + 0.05f, r * 1.05f, h + 0.06f }, 7, null);
        }
        return b.build(fog);
    }

    /**
     * Ribbon kelp: a tall stalk with long gold-green ribbons hanging off it and glowing
     * gas bulbs that keep it afloat. About 10 m tall.
     */
    static Mesh algaCinta(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 9;
        float alto = 9 + Terreno.azar(1, semilla, 1) * 3;
        int tramos = 8;
        float px = 0;
        float pz = 0;
        for (int k = 0; k < tramos; k++) {
            float t0 = (float) k / tramos;
            float t1 = (float) (k + 1) / tramos;
            float nx = (float) Math.sin(t1 * 3 + semilla) * 0.5f;
            float nz = (float) Math.cos(t1 * 2.3f + semilla) * 0.4f;
            b.color = Terreno.mezcla(0x4f6a22, 0x8a9a35, t1);
            float w = 0.16f;
            // stalk: two crossed strips
            b.quad(px - w, alto * t0, pz, px + w, alto * t0, pz, nx + w, alto * t1, nz, nx - w, alto * t1, nz);
            b.quad(px, alto * t0, pz - w, px, alto * t0, pz + w, nx, alto * t1, nz + w, nx, alto * t1, nz - w);
            // a ribbon leaf on alternating sides
            if (k > 0) {
                float lado = (k & 1) == 0 ? 1 : -1;
                float ang = k * 73 + semilla * 40;
                float lx = (float) Math.cos(ang * 0.0174f) * lado;
                float lz = (float) Math.sin(ang * 0.0174f) * lado;
                float y = alto * t0;
                b.color = Terreno.mezcla(0x7d8f2a, 0xc4b246, t1);
                b.quad(px, y, pz, px + lx * 2.6f, y + 0.4f, pz + lz * 2.6f, px + lx * 3.4f, y + 1.6f,
                        pz + lz * 3.4f, px, y + 1.1f, pz);
                b.quad(px, y + 0.3f, pz, px - lz * 2.0f, y + 0.9f, pz + lx * 2.0f, px - lz * 2.6f, y + 2.0f,
                        pz + lx * 2.6f, px, y + 1.4f, pz);
                // gas bulb at the base of the leaf
                b.brillo = true;
                b.color = 0xffd75a;
                b.identity();
                b.move(px + lx * 0.25f, y + 0.25f, pz + lz * 0.25f);
                b.ellipsoid(0.2f, 0.25f, 0.2f, 5, 3);
                b.identity();
                b.brillo = false;
            }
            px = nx;
            pz = nz;
        }
        return b.build(fog, 255, true, 0);
    }

    /**
     * Bubblefin: a small round fish, teal with an orange belly and fins and a glowing
     * air sac on its back. Edible; schools in the shallows. Faces +Z, about 0.6 m long.
     */
    static Mesh pezBurbujin(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        // body
        b.color = 0x2bb3a6;
        b.rot(0, 90);
        b.lathe(new float[] { 0f, -0.32f, 0.12f, -0.26f, 0.19f, -0.1f, 0.2f, 0.05f, 0.15f, 0.2f, 0.07f, 0.28f, 0f,
                0.31f }, 8, new int[] { 0x2bb3a6, 0x2bb3a6, 0x34c7b8, 0x34c7b8, 0x2bb3a6, 0x1f8f86 });
        b.identity();
        // belly stripe
        b.color = 0xf08a3a;
        b.quad(-0.12f, -0.17f, -0.15f, 0.12f, -0.17f, -0.15f, 0.12f, -0.17f, 0.15f, -0.12f, -0.17f, 0.15f);
        // tail fin (two-sided by building both faces)
        b.color = 0xf27a2e;
        b.tri(0, 0, -0.28f, 0, 0.17f, -0.5f, 0, -0.17f, -0.5f);
        b.tri(0, 0, -0.28f, 0, -0.17f, -0.5f, 0, 0.17f, -0.5f);
        // side fins
        b.tri(0.18f, -0.03f, 0.05f, 0.33f, -0.1f, -0.08f, 0.18f, -0.06f, -0.05f);
        b.tri(-0.18f, -0.03f, 0.05f, -0.18f, -0.06f, -0.05f, -0.33f, -0.1f, -0.08f);
        // eyes
        b.color = 0x101418;
        b.box(0.15f, 0.06f, 0.17f, 0.06f, 0.07f, 0.06f);
        b.box(-0.15f, 0.06f, 0.17f, 0.06f, 0.07f, 0.06f);
        // glowing air sac
        b.brillo = true;
        b.color = 0xa8f6ff;
        b.move(0, 0.2f, -0.02f);
        b.ellipsoid(0.09f, 0.08f, 0.12f, 6, 3);
        return b.build(fog);
    }

    /**
     * Emergency pod of the Halcyon station: a squat octagonal float with a white hull, a
     * teal equator band with portholes, three outrigger pontoons, a top hatch and a mast
     * carrying the (broken) red distress light. Origin at the waterline, about 5 m wide.
     */
    static Mesh capsula(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        int blanco = 0xe8ecef;
        int gris = 0x8a959c;
        int turquesa = 0x1fa3a8;
        int oscuro = 0x1d2a33;
        int coral = 0xe0533d;
        // hull: octagonal lathe, bottom keel to top dome
        float[] casco = { 0.0f, -2.1f, 1.1f, -2.0f, 1.9f, -1.5f, 2.2f, -0.7f, 2.25f, -0.1f, 2.25f, 0.45f, 2.05f,
                1.0f, 1.55f, 1.55f, 0.85f, 1.85f, 0.0f, 1.95f };
        int[] bandas = { gris, gris, blanco, turquesa, turquesa, blanco, blanco, blanco, blanco };
        b.lathe(casco, 8, bandas);
        // portholes: dark recessed panels around the teal band
        b.color = oscuro;
        for (int i = 0; i < 8; i += 2) {
            b.identity();
            b.rot(1, i * 45 + 22.5f);
            b.move(0, 0.18f, 2.27f);
            b.box(0, 0, 0, 0.75f, 0.42f, 0.06f);
        }
        // hatch on top
        b.identity();
        b.color = gris;
        b.move(0, 1.95f, 0);
        b.lathe(new float[] { 0.75f, 0f, 0.75f, 0.18f, 0.55f, 0.3f, 0f, 0.32f }, 8, null);
        b.color = coral;
        b.box(0.0f, 0.36f, 0.0f, 0.5f, 0.08f, 0.12f);
        // mast with the distress light
        b.identity();
        b.color = gris;
        b.move(1.0f, 1.6f, -0.6f);
        b.box(0, 0.9f, 0, 0.12f, 1.8f, 0.12f);
        b.color = coral;
        b.move(0, 1.9f, 0);
        b.ellipsoid(0.2f, 0.2f, 0.2f, 6, 3);
        // three outrigger pontoons on struts
        for (int i = 0; i < 3; i++) {
            b.identity();
            b.rot(1, i * 120 + 30);
            b.color = gris;
            b.move(0, -0.35f, 2.6f);
            b.box(0, 0, -0.25f, 0.22f, 0.18f, 0.9f);
            b.move(0, 0, 0.55f);
            b.rot(0, 90);
            b.color = coral;
            b.lathe(new float[] { 0f, -0.95f, 0.32f, -0.8f, 0.42f, -0.4f, 0.42f, 0.4f, 0.32f, 0.8f, 0f, 0.95f }, 6,
                    new int[] { coral, coral, blanco, coral, coral });
        }
        return b.build(fog);
    }
}
