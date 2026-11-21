package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Mesh;

/** Procedural low-poly models. Every model is original and built only from code. */
public final class Modelos {
    private Modelos() {
    }

    public static final String[] NOMBRES = { "capsula", "fondo_bajios", "roca_cobre", "roca_cuarzo",
            "coral_abanico", "coral_tubos", "alga_cinta", "pez_burbujin",
            // batch 2 (pending approval: gallery only)
            "planta_lanza", "pilar_canon", "flor_abisal", "dientesierra", "acorazado", "coloso_ceniza",
            "remora",
            // alien fish (pending approval)
            "burbujin_v2", "lazofin", "farolillo",
            // shallows rock formations
            "arco_piedra", "cueva", "cresta_arrecife" };
    /** gallery framing: camera distance, height of the point looked at, show sand floor */
    public static final float[] DIST = { 9, 22, 4.5f, 4f, 4.5f, 4.5f, 13, 1.6f, 5, 34, 4, 8, 4, 95, 10, 1.6f, 3.2f,
            1.3f, 14, 11, 13 };
    public static final float[] ALTO = { 0.5f, 3, 0.6f, 0.6f, 1.0f, 0.8f, 5f, 0f, 1.2f, 10, 0.8f, 0, 0.5f, 0, 0, 0, 0,
            0, 2.5f, 1.2f, 1 };
    public static final boolean[] SUELO = { false, false, true, true, true, true, true, false, true, true, true,
            false, true, false, false, false, false, false, true, true, true };

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
        if ("planta_lanza".equals(nombre)) {
            return plantaLanza(fog, 3);
        }
        if ("pilar_canon".equals(nombre)) {
            return pilarCanon(fog, 5);
        }
        if ("flor_abisal".equals(nombre)) {
            return florAbisal(fog, 2);
        }
        if ("dientesierra".equals(nombre)) {
            return dientesierra(fog);
        }
        if ("acorazado".equals(nombre)) {
            return acorazado(fog);
        }
        if ("coloso_ceniza".equals(nombre)) {
            return colosoCeniza(fog);
        }
        if ("remora".equals(nombre)) {
            return remora(fog);
        }
        if ("burbujin_v2".equals(nombre)) {
            return burbujinV2(fog);
        }
        if ("lazofin".equals(nombre)) {
            return lazofin(fog);
        }
        if ("farolillo".equals(nombre)) {
            return farolillo(fog);
        }
        if ("arco_piedra".equals(nombre)) {
            return arcoPiedra(fog, 1);
        }
        if ("cueva".equals(nombre)) {
            return cueva(fog, 1);
        }
        if ("cresta_arrecife".equals(nombre)) {
            return crestaArrecife(fog, 1);
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
        b.box(0, 0.25f, 0, 0.2f, 0.5f, 0.18f);
        // the original flat fan, but every branch is a solid prism (front, back and edges),
        // thicker at the base, so it has body when seen from any side
        int ramas = 13;
        float y0 = 0.45f;
        float[] tx = new float[ramas];
        float[] ty = new float[ramas];
        for (int i = 0; i < ramas; i++) {
            float a = -70 + 140f * i / (ramas - 1) + (Terreno.azar(i, semilla, 3) - 0.5f) * 10;
            float len = 1.3f + Terreno.azar(i, semilla, 4) * 0.6f;
            float rad = a * 0.017453292f;
            float dx = (float) Math.sin(rad);
            float dy = (float) Math.cos(rad);
            int pasos = 4;
            for (int k = 0; k < pasos; k++) {
                float t0 = (float) k / pasos;
                float t1 = (float) (k + 1) / pasos;
                float w0 = 0.13f * (1 - t0) + 0.05f;
                float w1 = 0.13f * (1 - t1) + 0.05f;
                float h0 = 0.09f * (1 - t0) + 0.035f; // half thickness
                float h1 = 0.09f * (1 - t1) + 0.035f;
                b.color = Terreno.mezcla(base, punta, t1);
                float ax = dx * len * t0;
                float ay = y0 + dy * len * t0;
                float bx = dx * len * t1;
                float by = y0 + dy * len * t1;
                // corners: l = left edge, r = right edge, f = front (+z), k = back (-z)
                float alx = ax - dy * w0, aly = ay + dx * w0, arx = ax + dy * w0, ary = ay - dx * w0;
                float blx = bx - dy * w1, bly = by + dx * w1, brx = bx + dy * w1, bry = by - dx * w1;
                b.quad(alx, aly, h0, arx, ary, h0, brx, bry, h1, blx, bly, h1);
                b.quad(arx, ary, -h0, alx, aly, -h0, blx, bly, -h1, brx, bry, -h1);
                b.color = Terreno.mezcla(base, punta, t1 * 0.8f);
                b.quad(arx, ary, h0, arx, ary, -h0, brx, bry, -h1, brx, bry, h1);
                b.quad(alx, aly, -h0, alx, aly, h0, blx, bly, h1, blx, bly, -h1);
            }
            float m = 0.6f;
            tx[i] = dx * len * m;
            ty[i] = y0 + dy * len * m;
        }
        // webs between neighbouring branches
        b.color = Terreno.mezcla(base, punta, 0.5f);
        for (int i = 1; i < ramas; i++) {
            float g = 0.04f;
            b.quad(tx[i - 1], ty[i - 1] - g, 0, tx[i], ty[i] - g, 0, tx[i], ty[i] + g, 0, tx[i - 1], ty[i - 1] + g, 0);
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
            b.lathe(new float[] { r * 1.2f, -0.1f, r, h * 0.5f, r * 1.15f, h }, 7, null);
            // rim (from the outer to the inner radius, facing up) and the inner wall, whose
            // profile runs downwards so its faces point into the tube
            b.color = 0xc85432;
            b.lathe(new float[] { r * 1.15f, h, r * 0.82f, h }, 7, null);
            b.color = 0x5a1c24;
            b.lathe(new float[] { r * 0.82f, h, r * 0.75f, h * 0.35f }, 7, null);
            // floor inside the tube
            b.color = 0x3a1018;
            b.lathe(new float[] { r * 0.75f, h * 0.35f, 0f, h * 0.35f }, 7, null);
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
    // ================================================================ batch 2

    /** Spear plant (red plateau): a clump of stiff red blades with glowing yellow tips. ~2 m */
    static Mesh plantaLanza(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        int n = 9;
        for (int i = 0; i < n; i++) {
            float a = i * (360f / n) + Terreno.azar(i, semilla, 1) * 30;
            float incl = 8 + Terreno.azar(i, semilla, 2) * 26;
            float h = 1.2f + Terreno.azar(i, semilla, 3) * 1.1f;
            float w = 0.2f;
            b.identity();
            b.rot(1, a);
            b.move(0, 0, 0.12f);
            b.rot(0, incl);
            // blade: a flattened diamond, two-sided by drawing both faces
            b.brillo = false;
            b.color = 0xa8302c;
            b.tri(-w, 0, 0, w, 0, 0, 0, h * 0.8f, 0.03f);
            b.tri(w, 0, 0, -w, 0, 0, 0, h * 0.8f, -0.03f);
            b.color = 0xd04a36;
            b.tri(-w, 0, 0, 0, h * 0.8f, 0.03f, 0, h * 0.8f, -0.03f);
            b.tri(w, 0, 0, 0, h * 0.8f, -0.03f, 0, h * 0.8f, 0.03f);
            // glowing tip
            b.brillo = true;
            b.color = 0xffd25a;
            b.tri(-w * 0.5f, h * 0.8f, 0, w * 0.5f, h * 0.8f, 0, 0, h, 0);
            b.tri(w * 0.5f, h * 0.8f, 0, -w * 0.5f, h * 0.8f, 0, 0, h, 0);
        }
        b.brillo = false;
        b.identity();
        b.color = 0x5a2a26;
        b.lathe(new float[] { 0.32f, 0f, 0.25f, 0.18f, 0f, 0.22f }, 6, null);
        return b.build(fog, 255, true, 0);
    }

    /** Canyon pillar: a tall cracked rock column with veins of glowing cyan mineral. ~20 m */
    static Mesh pilarCanon(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 9;
        float alto = 20;
        int tramos = 6;
        float[] xs = new float[tramos + 1];
        float[] ys = new float[tramos + 1];
        float[] zs = new float[tramos + 1];
        float[] rs = new float[tramos + 1];
        int[] cols = new int[tramos];
        for (int k = 0; k <= tramos; k++) {
            ys[k] = alto * k / tramos;
            xs[k] = (Terreno.azar(k, semilla, 2) - 0.5f) * 0.8f;
            zs[k] = (Terreno.azar(k, semilla, 3) - 0.5f) * 0.8f;
            rs[k] = 3.4f - k * 0.3f + Terreno.azar(k, semilla, 1) * 0.6f;
            if (k < tramos) {
                cols[k] = Terreno.mezcla(0x3e3c4e, 0x55536a, (k & 1) * 0.6f);
            }
        }
        ys[0] = -1.5f; // sink the base into the floor
        b.tubo(xs, ys, zs, rs, 7, cols, true);
        b.identity();
        b.color = 0x34323f;
        b.move(0, alto, 0);
        b.roca(1.9f, 1.2f, 1.7f, 7, 4, semilla, 0.25f);
        // glowing veins: thin strips running up the sides
        b.brillo = true;
        b.color = 0x45e6ff;
        for (int i = 0; i < 4; i++) {
            b.identity();
            b.rot(1, i * 90 + 20);
            float y = 2 + i * 3.5f;
            float r = 3.4f - y * 0.06f;
            b.quad(-0.35f, y, r + 0.12f, 0.35f, y, r + 0.12f, 0.75f, y + 6, r - 0.05f, 0.05f, y + 6, r - 0.05f);
        }
        return b.build(fog);
    }

    /** Abyss bloom: a glowing violet cup flower on a pale stalk, pulses with light. ~1.5 m */
    static Mesh florAbisal(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 6;
        b.color = 0x8d8fa8;
        b.lathe(new float[] { 0.09f, 0f, 0.06f, 0.6f, 0.05f, 1.1f }, 5, null);
        // leaves at the base
        b.color = 0x3a3f66;
        for (int i = 0; i < 3; i++) {
            b.identity();
            b.rot(1, i * 120);
            b.tri(0, 0.05f, 0, 0.14f, 0.08f, 0.55f, -0.14f, 0.08f, 0.55f);
        }
        // the cup: six glowing petals
        b.brillo = true;
        for (int i = 0; i < 6; i++) {
            b.identity();
            b.move(0, 1.1f, 0);
            b.rot(1, i * 60);
            b.color = (i & 1) == 0 ? 0xb46cff : 0x9a52f0;
            b.tri(-0.12f, 0, 0.05f, 0.12f, 0, 0.05f, 0.3f, 0.4f, 0.34f);
            b.tri(-0.12f, 0, 0.05f, 0.3f, 0.4f, 0.34f, -0.08f, 0.46f, 0.38f);
        }
        b.identity();
        b.color = 0xfff0a8;
        b.move(0, 1.18f, 0);
        b.ellipsoid(0.1f, 0.1f, 0.1f, 5, 3);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Sawtooth: an ambush eel of the red plateau, slate grey with a row of orange saw
     * spikes along its back, a wide hinged jaw and glowing amber eyes. ~4.5 m, faces +Z.
     */
    static Mesh dientesierra(Fog fog) {
        MB b = new MB();
        b.jitter = 7;
        int seg = 9;
        float largo = 4.5f;
        float[] cxs = new float[seg + 1];
        float[] cys = new float[seg + 1];
        float[] czs = new float[seg + 1];
        float[] rs = new float[seg + 1];
        int[] bandas = new int[seg];
        for (int k = 0; k <= seg; k++) {
            float t = (float) k / seg;
            // from the head (t = 0, +Z) to the tail, with a gentle S curve
            czs[k] = largo * (0.5f - t);
            cxs[k] = (float) Math.sin(t * 5) * 0.25f;
            cys[k] = 0.15f;
            rs[k] = radioAnguila(t);
            if (k < seg) {
                bandas[k] = (k & 1) == 0 ? 0x4a5560 : 0x56616c;
            }
        }
        b.identity();
        b.tubo(cxs, cys, czs, rs, 7, bandas, true);
        // dorsal saw spikes along the back
        b.color = 0xe0702c;
        for (int k = 0; k < seg; k++) {
            float t0 = (float) k / seg;
            float zm = (czs[k] + czs[k + 1]) / 2;
            float xm = (cxs[k] + cxs[k + 1]) / 2;
            float rm = (rs[k] + rs[k + 1]) / 2;
            float y0 = 0.15f + rm * 0.85f;
            b.tri(xm, y0, zm + 0.18f, xm, y0 + 0.35f * (1 - t0 * 0.6f), zm - 0.05f, xm, y0, zm - 0.2f);
        }
        // tail fin
        b.identity();
        b.color = 0xc85a26;
        float zt = -largo / 2;
        float xt = cxs[seg];
        b.tri(xt, 0.15f, zt + 0.05f, xt, 0.55f, zt - 0.7f, xt, -0.15f, zt - 0.6f);
        // head: wide jaw (upper and lower wedge)
        float zh = largo / 2;
        b.color = 0x3c454f;
        b.quad(-0.34f, 0.02f, zh, 0.34f, 0.02f, zh, 0.16f, 0.12f, zh + 0.75f, -0.16f, 0.12f, zh + 0.75f);
        b.quad(-0.34f, 0.3f, zh, -0.16f, 0.12f, zh + 0.75f, 0.16f, 0.12f, zh + 0.75f, 0.34f, 0.3f, zh);
        b.tri(-0.34f, 0.02f, zh, -0.16f, 0.12f, zh + 0.75f, -0.34f, 0.3f, zh);
        b.tri(0.34f, 0.02f, zh, 0.34f, 0.3f, zh, 0.16f, 0.12f, zh + 0.75f);
        b.color = 0x8a3a30;
        b.quad(-0.3f, -0.05f, zh, -0.14f, -0.25f, zh + 0.6f, 0.14f, -0.25f, zh + 0.6f, 0.3f, -0.05f, zh);
        b.color = 0x2c333b;
        b.quad(-0.3f, -0.05f, zh, 0.3f, -0.05f, zh, 0.14f, -0.25f, zh + 0.6f, -0.14f, -0.25f, zh + 0.6f);
        // teeth
        b.color = 0xf2ead8;
        for (int i = 0; i < 4; i++) {
            float x = -0.18f + i * 0.12f;
            b.tri(x - 0.04f, 0.04f, zh + 0.5f, x + 0.04f, 0.04f, zh + 0.5f, x, -0.14f, zh + 0.5f);
            b.tri(x + 0.04f, 0.04f, zh + 0.5f, x - 0.04f, 0.04f, zh + 0.5f, x, -0.14f, zh + 0.5f);
        }
        // glowing amber eyes
        b.brillo = true;
        b.color = 0xffb02e;
        b.box(0.3f, 0.2f, zh + 0.15f, 0.08f, 0.08f, 0.12f);
        b.box(-0.3f, 0.2f, zh + 0.15f, 0.08f, 0.08f, 0.12f);
        return b.build(fog, 255, true, 0);
    }

    private static float radioAnguila(float t) {
        // thick behind the head, thinning to the tail
        return 0.36f * (1 - t * 0.8f) + 0.04f;
    }

    /**
     * Ironback crab: a squat crab with a hexagonal plated shell (rust with teal edges),
     * six legs and two heavy claws. Guards ore in the plateau. ~1.6 m wide, faces +Z.
     */
    static Mesh acorazado(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        // shell: flattened hexagonal dome
        b.color = 0x9a4e2e;
        b.move(0, 0.55f, 0);
        b.lathe(new float[] { 0.75f, -0.15f, 0.8f, 0.05f, 0.62f, 0.3f, 0.3f, 0.45f, 0f, 0.5f }, 6,
                new int[] { 0x5a3a30, 0x2f8f8a, 0x9a4e2e, 0xb05a32 });
        // belly
        b.color = 0x6c4a3a;
        b.lathe(new float[] { 0f, -0.35f, 0.6f, -0.18f, 0.75f, -0.15f }, 6, null);
        // six legs: two segments each
        for (int i = 0; i < 6; i++) {
            float lado = i < 3 ? 1 : -1;
            float a = (i % 3) * 32 - 32;
            b.identity();
            b.move(0, 0.45f, 0);
            b.rot(1, lado > 0 ? 90 + a : -90 - a);
            b.color = 0x7a4630;
            b.move(0, 0, 0.6f);
            b.rot(0, -25);
            b.box(0, 0, 0.3f, 0.1f, 0.1f, 0.6f);
            b.move(0, 0, 0.58f);
            b.rot(0, 75);
            b.color = 0x2f8f8a;
            b.box(0, 0, 0.28f, 0.08f, 0.08f, 0.56f);
        }
        // claws
        for (int s = -1; s <= 1; s += 2) {
            b.identity();
            b.move(s * 0.45f, 0.5f, 0.5f);
            b.rot(1, s * -20);
            b.color = 0x8a4a2e;
            b.box(0, 0, 0.25f, 0.16f, 0.16f, 0.5f);
            b.move(0, 0, 0.62f);
            b.color = 0xb05a32;
            b.box(0, 0.06f, 0.12f, 0.3f, 0.2f, 0.34f);
            b.color = 0x2f8f8a;
            b.box(0, -0.1f, 0.18f, 0.22f, 0.08f, 0.3f);
        }
        // eye stalks
        b.identity();
        b.brillo = true;
        b.color = 0x8fff9a;
        b.box(0.16f, 1.05f, 0.62f, 0.07f, 0.07f, 0.07f);
        b.box(-0.16f, 1.05f, 0.62f, 0.07f, 0.07f, 0.07f);
        return b.build(fog);
    }

    /**
     * Ash Colossus: a gigantic ray-like leviathan of the abyss. Ash-grey wings with darker
     * ridges, a long whip tail and three glowing lure tendrils hanging below its mouth.
     * About 60 m from wingtip to wingtip, faces +Z.
     */
    static Mesh colosoCeniza(Fog fog) {
        MB b = new MB();
        b.scale = 1f / 64f; // +-512 m range: it is huge
        b.jitter = 6;
        // wings: a fan of triangles, upper and lower surfaces
        int n = 8;
        float env = 30;
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i < n; i++) {
                float t0 = (float) i / n;
                float t1 = (float) (i + 1) / n;
                float x0 = s * env * t0;
                float x1 = s * env * t1;
                float z0f = 14 * (1 - t0 * t0) - 4 * t0;
                float z1f = 14 * (1 - t1 * t1) - 4 * t1;
                float z0b = -10 * (1 - t0) + 2 * t0;
                float z1b = -10 * (1 - t1) + 2 * t1;
                float y0 = (float) Math.sin(t0 * 2.6f) * 3.5f;
                float y1 = (float) Math.sin(t1 * 2.6f) * 3.5f;
                float g0 = 2.4f * (1 - t0);
                float g1 = 2.4f * (1 - t1);
                b.color = (i & 1) == 0 ? 0x6e6a70 : 0x5c585f;
                if (s > 0) {
                    b.quad(x0, y0 + g0, z0b, x1, y1 + g1, z1b, x1, y1 + g1, z1f, x0, y0 + g0, z0f);
                } else {
                    b.quad(x0, y0 + g0, z0b, x0, y0 + g0, z0f, x1, y1 + g1, z1f, x1, y1 + g1, z1b);
                }
                b.color = 0x8c8890;
                if (s > 0) {
                    b.quad(x0, y0 - g0 * 0.5f, z0b, x0, y0 - g0 * 0.5f, z0f, x1, y1 - g1 * 0.5f, z1f, x1,
                            y1 - g1 * 0.5f, z1b);
                } else {
                    b.quad(x0, y0 - g0 * 0.5f, z0b, x1, y1 - g1 * 0.5f, z1b, x1, y1 - g1 * 0.5f, z1f, x0,
                            y0 - g0 * 0.5f, z0f);
                }
                // front and back edges
                b.color = 0x4a464e;
                if (s > 0) {
                    b.quad(x0, y0 - g0 * 0.5f, z0f, x0, y0 + g0, z0f, x1, y1 + g1, z1f, x1, y1 - g1 * 0.5f, z1f);
                    b.quad(x0, y0 + g0, z0b, x0, y0 - g0 * 0.5f, z0b, x1, y1 - g1 * 0.5f, z1b, x1, y1 + g1, z1b);
                } else {
                    b.quad(x0, y0 - g0 * 0.5f, z0f, x1, y1 - g1 * 0.5f, z1f, x1, y1 + g1, z1f, x0, y0 + g0, z0f);
                    b.quad(x0, y0 + g0, z0b, x1, y1 + g1, z1b, x1, y1 - g1 * 0.5f, z1b, x0, y0 - g0 * 0.5f, z0b);
                }
            }
        }
        // head bulge with a dark mouth slit
        b.identity();
        b.color = 0x625e66;
        b.move(0, 0.5f, 13);
        b.ellipsoid(5, 2.4f, 4, 8, 4);
        b.identity();
        b.color = 0x1a1418;
        b.quad(-3.5f, -0.6f, 16.6f, 3.5f, -0.6f, 16.6f, 3.2f, 0.2f, 16.9f, -3.2f, 0.2f, 16.9f);
        // tail
        b.color = 0x4a464e;
        b.tri(-1.2f, 0, -9, 1.2f, 0, -9, 0, 0.3f, -40);
        b.tri(1.2f, 0, -9, -1.2f, 0, -9, 0, -0.3f, -40);
        // glowing lures hanging under the mouth
        b.brillo = true;
        for (int i = -1; i <= 1; i++) {
            float x = i * 2.2f;
            b.color = 0x7affd6;
            b.quad(x - 0.1f, -1.5f, 15, x + 0.1f, -1.5f, 15, x + 0.1f, -9 + Math.abs(i) * 2, 16, x - 0.1f,
                    -9 + Math.abs(i) * 2, 16);
            b.identity();
            b.move(x, -9.4f + Math.abs(i) * 2, 16);
            b.color = 0xd8fff2;
            b.ellipsoid(0.6f, 0.6f, 0.6f, 5, 3);
            b.identity();
        }
        // eyes
        b.color = 0xff5a3a;
        b.box(3.8f, 1.6f, 15, 0.8f, 0.5f, 0.8f);
        b.box(-3.8f, 1.6f, 15, 0.8f, 0.5f, 0.8f);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Remora: a one-person submersible. A long orange torpedo hull with a dark bubble
     * canopy on top, two side thruster pods and a ring-guarded rear propeller. ~5 m long,
     * faces +Z.
     */
    static Mesh remora(Fog fog) {
        MB b = new MB();
        b.jitter = 5;
        // hull along Z
        b.rot(0, 90);
        b.lathe(new float[] { 0f, -2.5f, 0.45f, -2.3f, 0.75f, -1.6f, 0.85f, -0.4f, 0.85f, 0.8f, 0.7f, 1.8f, 0.4f, 2.4f,
                0f, 2.55f }, 8, new int[] { 0x3a4048, 0xe8692a, 0xf07a34, 0xf07a34, 0xe8ecef, 0xe8ecef,
                        0x3a4048 });
        // canopy
        b.identity();
        b.color = 0x1c3a4c;
        b.move(0, 0.6f, 0.8f);
        b.ellipsoid(0.55f, 0.45f, 0.95f, 8, 4);
        // canopy frame
        b.identity();
        b.color = 0xe8ecef;
        b.box(0, 1.02f, 0.8f, 0.08f, 0.06f, 1.6f);
        // side thruster pods
        for (int s = -1; s <= 1; s += 2) {
            b.identity();
            b.color = 0x8a959c;
            b.box(s * 0.95f, -0.1f, -0.2f, 0.5f, 0.12f, 0.6f);
            b.move(s * 1.35f, -0.1f, -0.3f);
            b.rot(0, 90);
            b.color = 0xe8ecef;
            b.lathe(new float[] { 0.1f, -0.9f, 0.28f, -0.7f, 0.3f, 0.6f, 0.2f, 0.9f, 0f, 1.0f }, 6,
                    new int[] { 0x3a4048, 0xe8ecef, 0xe8ecef, 0xe8692a });
        }
        // rear ring guard and propeller
        b.identity();
        b.color = 0x8a959c;
        b.move(0, 0, -2.45f);
        b.rot(0, 90);
        b.lathe(new float[] { 0.75f, -0.15f, 0.75f, 0.15f, 0.65f, 0.15f, 0.65f, -0.15f, 0.75f, -0.15f }, 10, null);
        b.identity();
        b.color = 0x2a2e33;
        for (int i = 0; i < 3; i++) {
            b.identity();
            b.move(0, 0, -2.45f);
            b.rot(2, i * 120);
            b.quad(0, 0, 0.03f, 0.6f, -0.1f, 0.03f, 0.6f, 0.1f, -0.03f, 0, 0.06f, -0.03f);
            b.quad(0, 0, 0.03f, 0, 0.06f, -0.03f, 0.6f, 0.1f, -0.03f, 0.6f, -0.1f, 0.03f);
        }
        // headlights
        b.identity();
        b.brillo = true;
        b.color = 0xfff6d8;
        b.box(0.35f, -0.25f, 2.25f, 0.18f, 0.12f, 0.1f);
        b.box(-0.35f, -0.25f, 2.25f, 0.18f, 0.12f, 0.1f);
        return b.build(fog);
    }
    // ================================================================ alien fish

    /**
     * Bubblefin (alien redesign): a plump body with three-fold symmetry - three tail fins
     * spread 120 degrees apart around the tail, three stubby side fins - four small eyes in
     * a row across the face, a short feeding tube and a glowing gas sac on top. Teal fading
     * to violet with orange fins. Edible; schools in the shallows. ~0.55 m, faces +Z.
     */
    static Mesh burbujinV2(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        b.rot(0, 90);
        b.lathe(new float[] { 0f, -0.3f, 0.11f, -0.25f, 0.2f, -0.1f, 0.21f, 0.04f, 0.17f, 0.17f, 0.09f, 0.25f, 0f,
                0.27f }, 9, new int[] { 0x6a4fc0, 0x5a62c8, 0x2fb8b0, 0x2fb8b0, 0x38cfc0, 0x2a9c96 });
        // three tail fins, 120 degrees apart (alien tri-radial symmetry)
        for (int i = 0; i < 3; i++) {
            b.identity();
            b.move(0, 0, -0.28f);
            b.rot(2, i * 120 + 90);
            b.color = i == 0 ? 0xff8a3a : 0xf2702e;
            b.tri(0, 0, 0, 0, 0.2f, -0.22f, 0, 0.05f, -0.3f);
        }
        // three side fins around the middle
        for (int i = 0; i < 3; i++) {
            b.identity();
            b.rot(2, i * 120 + 30);
            b.color = 0xff9a4a;
            b.tri(0, 0.18f, 0.06f, 0, 0.32f, -0.06f, 0, 0.18f, -0.08f);
        }
        // feeding tube
        b.identity();
        b.color = 0x2a7c78;
        b.move(0, -0.05f, 0.25f);
        b.rot(0, 90);
        b.lathe(new float[] { 0.04f, 0f, 0.03f, 0.12f, 0.045f, 0.14f }, 5, null);
        // four eyes in a row across the face
        b.identity();
        b.color = 0x0e1216;
        for (int i = 0; i < 4; i++) {
            float x = -0.13f + i * 0.087f;
            b.box(x, 0.07f, 0.2f - Math.abs(x) * 0.5f, 0.045f, 0.05f, 0.04f);
        }
        // glowing gas sac
        b.brillo = true;
        b.color = 0xb8f8ff;
        b.move(0, 0.21f, -0.02f);
        b.ellipsoid(0.08f, 0.07f, 0.12f, 6, 3);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Ribbonfin: a flat disc-shaped fish that swims on its edge, golden with a magenta rim,
     * three eyes on its front edge and two long trailing ribbons. Schools in the kelp
     * forest. ~0.7 m disc, ribbons ~1.6 m, faces +Z.
     */
    static Mesh lazofin(Fog fog) {
        MB b = new MB();
        b.jitter = 7;
        int seg = 10;
        float r = 0.35f;
        float g = 0.17f;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * 2 * Math.PI / seg);
            float a1 = (float) ((i + 1) * 2 * Math.PI / seg);
            float y0 = (float) Math.sin(a0) * r;
            float z0 = (float) Math.cos(a0) * r;
            float y1 = (float) Math.sin(a1) * r;
            float z1 = (float) Math.cos(a1) * r;
            // two faces of the disc (bulging slightly in the middle) and the rim
            b.color = 0xffd248;
            b.tri(g, 0, 0, g * 0.3f, y0, z0, g * 0.3f, y1, z1);
            b.color = 0xf2c23c;
            b.tri(-g, 0, 0, -g * 0.3f, y1, z1, -g * 0.3f, y0, z0);
            b.color = 0xd0386e;
            b.quad(g * 0.3f, y0, z0, -g * 0.3f, y0, z0, -g * 0.3f, y1, z1, g * 0.3f, y1, z1);
        }
        // two trailing ribbons
        for (int s = -1; s <= 1; s += 2) {
            float y = s * 0.12f;
            for (int k = 0; k < 4; k++) {
                float z0 = -0.3f - k * 0.4f;
                float z1 = z0 - 0.4f;
                float w0 = 0.05f * (1 - k * 0.2f);
                float w1 = 0.05f * (1 - (k + 1) * 0.2f);
                float o0 = (float) Math.sin(k * 1.3f) * 0.08f * s;
                float o1 = (float) Math.sin((k + 1) * 1.3f) * 0.08f * s;
                b.color = (k & 1) == 0 ? 0xd0386e : 0xe85a8a;
                b.quad(0, y + o0 - w0, z0, 0, y + o0 + w0, z0, 0, y + o1 + w1, z1, 0, y + o1 - w1, z1);
            }
        }
        // three eyes on the front edge
        b.color = 0x101418;
        for (int i = -1; i <= 1; i++) {
            b.box(0, i * 0.09f + 0.04f, 0.34f - Math.abs(i) * 0.02f, 0.1f, 0.05f, 0.05f);
        }
        // glowing spots on the disc
        b.brillo = true;
        b.color = 0xfff2a0;
        for (int i = 0; i < 3; i++) {
            float a = (float) (i * 2.1 + 0.5);
            b.box(0, (float) Math.sin(a) * 0.18f, (float) Math.cos(a) * 0.18f, 0.14f, 0.05f, 0.05f);
        }
        // a little self-light so the gold reads even when the disc faces away from the sun
        return b.build(fog, 255, true, 0x504010);
    }

    /**
     * Lanternling: a small deep-water fish, almost black, with a line of glowing cyan
     * organs along each flank, two pale glowing eyes and a forked tail. Lives in the
     * canyons and the abyss. ~0.4 m, faces +Z.
     */
    static Mesh farolillo(Fog fog) {
        MB b = new MB();
        b.jitter = 4;
        b.rot(0, 90);
        b.lathe(new float[] { 0f, -0.2f, 0.06f, -0.17f, 0.1f, -0.05f, 0.11f, 0.06f, 0.08f, 0.15f, 0f, 0.2f }, 7,
                new int[] { 0x15161f, 0x1c1d29, 0x22243a, 0x1c1d29, 0x15161f });
        b.identity();
        // forked tail
        b.color = 0x262842;
        b.tri(0, 0, -0.18f, 0, 0.12f, -0.34f, 0, 0.02f, -0.26f);
        b.tri(0, 0, -0.18f, 0, -0.02f, -0.26f, 0, -0.12f, -0.34f);
        // dorsal spine fin
        b.tri(0, 0.09f, 0.05f, 0, 0.18f, -0.06f, 0, 0.08f, -0.1f);
        // glowing organs along both flanks and the eyes
        b.brillo = true;
        b.color = 0x3ff0ff;
        for (int i = 0; i < 5; i++) {
            float z = 0.1f - i * 0.06f;
            b.box(0.1f - i * 0.006f, -0.02f, z, 0.03f, 0.025f, 0.025f);
            b.box(-0.1f + i * 0.006f, -0.02f, z, 0.03f, 0.025f, 0.025f);
        }
        b.color = 0xe8fff4;
        b.box(0.065f, 0.04f, 0.13f, 0.03f, 0.04f, 0.04f);
        b.box(-0.065f, 0.04f, 0.13f, 0.03f, 0.04f, 0.04f);
        return b.build(fog, 255, true, 0);
    }

    // ================================================================ rock formations

    private static MB rocaMB() {
        MB b = new MB();
        b.jitter = 9;
        b.uvEscala = 1f / 2.5f;
        b.uvCaja = true;
        b.textura = Texturas.roca();
        return b;
    }

    /**
     * Stone arch: a rough rock band bent over in a half circle, legs buried in the sand, with
     * loose boulders at its feet. About 8 m wide and 5 m tall: you (and fish) swim through.
     * The arch spans the local X axis.
     */
    static Mesh arcoPiedra(Fog fog, int semilla) {
        MB b = rocaMB();
        int n = 12;
        float[] xs = new float[n + 3];
        float[] ys = new float[n + 3];
        float[] zs = new float[n + 3];
        float[] rs = new float[n + 3];
        int[] cols = new int[n + 2];
        // a leg going down into the sand, the arc, the other leg
        xs[0] = -4.2f;
        ys[0] = -1.5f;
        rs[0] = 1.5f;
        for (int k = 0; k <= n; k++) {
            float t = (float) k / n;
            float a = (float) (Math.PI * (1 - t));
            xs[k + 1] = (float) Math.cos(a) * 4.0f;
            ys[k + 1] = (float) Math.sin(a) * 4.6f;
            float grosor = 1.35f - 0.45f * (float) Math.sin(t * Math.PI);
            rs[k + 1] = grosor * (0.9f + Terreno.azar(k, semilla, 3) * 0.25f);
            zs[k + 1] = (Terreno.azar(k, semilla, 4) - 0.5f) * 0.3f;
        }
        xs[n + 2] = 4.2f;
        ys[n + 2] = -1.5f;
        rs[n + 2] = 1.5f;
        for (int k = 0; k < cols.length; k++) {
            cols[k] = Terreno.mezcla(0x8a8478, 0x6e6a64, Terreno.azar(k, semilla, 5));
        }
        b.tubo(xs, ys, zs, rs, 7, cols, true);
        // boulders around the feet
        b.color = 0x7e786e;
        for (int i = 0; i < 4; i++) {
            b.identity();
            float lado = i < 2 ? -1 : 1;
            b.move(lado * (4.2f + Terreno.azar(i, semilla, 6) * 1.5f), 0.1f, (Terreno.azar(i, semilla, 7) - 0.5f) * 3);
            b.roca(0.9f + Terreno.azar(i, semilla, 8) * 0.6f, 0.6f, 0.8f, 6, 4, semilla * 5 + i, 0.25f);
        }
        return b.build(fog);
    }

    /**
     * Small cave: a rock dome about 7 m across and 3 m high, open on one side (local +Z),
     * with glowing nodules on the inner walls. Fish hide inside. Two-sided so the inside is
     * drawn as well.
     */
    static Mesh cueva(Fog fog, int semilla) {
        MB b = rocaMB();
        int seg = 12;
        int anillos = 5;
        float rx = 3.6f, ry = 3.0f;
        float[][] vx = new float[anillos + 1][seg];
        float[][] vy = new float[anillos + 1][seg];
        float[][] vz = new float[anillos + 1][seg];
        for (int j = 0; j <= anillos; j++) {
            float e = (float) (Math.PI / 2 * j / anillos);
            for (int i = 0; i < seg; i++) {
                float a = (float) (2 * Math.PI * i / seg);
                float k = j == anillos ? 1 : 0.85f + Terreno.azar(i + j * 31, semilla, 11) * 0.3f;
                vx[j][i] = (float) (Math.cos(e) * Math.sin(a)) * rx * k;
                vz[j][i] = (float) (Math.cos(e) * Math.cos(a)) * rx * k;
                vy[j][i] = (float) Math.sin(e) * ry * k - 0.4f;
            }
        }
        for (int j = 0; j < anillos; j++) {
            for (int i = 0; i < seg; i++) {
                // the mouth: two sectors facing +Z, open up to about two thirds of the height
                boolean boca = (i == 0 || i == seg - 1) && j < 3;
                if (boca) {
                    continue;
                }
                int i1 = (i + 1) % seg;
                b.color = Terreno.mezcla(0x857f74, 0x625d56, Terreno.azar(i + j * 17, semilla, 12));
                b.quad(vx[j][i], vy[j][i], vz[j][i], vx[j][i1], vy[j][i1], vz[j][i1], vx[j + 1][i1], vy[j + 1][i1],
                        vz[j + 1][i1], vx[j + 1][i], vy[j + 1][i], vz[j + 1][i]);
            }
        }
        // lip of rock framing the mouth
        b.color = 0x6e685f;
        b.identity();
        b.move(0, 1.9f, rx * 0.85f);
        b.roca(1.4f, 0.55f, 0.7f, 6, 3, semilla * 3, 0.3f);
        // glowing nodules inside
        b.identity();
        b.brillo = true;
        b.textura = null;
        b.color = 0x7affc8;
        for (int i = 0; i < 4; i++) {
            float a = (float) (Math.PI * (0.6f + i * 0.25f));
            b.identity();
            b.move((float) Math.sin(a) * 2.6f, 0.4f + i * 0.35f, (float) Math.cos(a) * 2.6f);
            b.ellipsoid(0.16f, 0.12f, 0.16f, 5, 2);
        }
        b.brillo = false;
        b.textura = Texturas.roca();
        return b.build(fog, 255, true, 0);
    }

    /**
     * Reef ridge: a low line of rocks about 10 m long along the local X axis, half buried in
     * the sand. It breaks up the open floor; corals tend to grow around it.
     */
    static Mesh crestaArrecife(Fog fog, int semilla) {
        MB b = rocaMB();
        for (int i = 0; i < 6; i++) {
            float x = -5 + i * 2 + (Terreno.azar(i, semilla, 21) - 0.5f) * 0.8f;
            float s = 1.0f + Terreno.azar(i, semilla, 22) * 0.9f;
            if (i == 0 || i == 5) {
                s *= 0.7f;
            }
            b.identity();
            b.move(x, s * 0.25f, (Terreno.azar(i, semilla, 23) - 0.5f) * 1.2f);
            b.color = Terreno.mezcla(0x8a8276, 0x6b675f, Terreno.azar(i, semilla, 24));
            b.roca(s * 1.2f, s * 0.9f, s, 7, 4, semilla * 13 + i, 0.25f);
        }
        return b.build(fog);
    }
}
