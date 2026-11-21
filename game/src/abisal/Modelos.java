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
            "arco_piedra", "cueva", "cresta_arrecife", "tunel", "planeador", "medusa",
            // new biomes
            "fumarola", "manta_cristal", "caracol_duna", "camarones_igneos", "anguila_brasa", "lirio_duna",
            "costillas", "coral_ceniza", "basalto" };
    /** gallery framing: camera distance, height of the point looked at, show sand floor */
    public static final float[] DIST = { 9, 22, 4.5f, 4f, 4.5f, 4.5f, 13, 1.6f, 5, 34, 4, 8, 4, 95, 10, 1.6f, 3.2f,
            1.3f, 14, 11, 13, 22, 2.6f, 2.6f, 9, 5, 1.4f, 3, 7, 5, 26, 4.5f, 9 };
    public static final float[] ALTO = { 0.5f, 3, 0.6f, 0.6f, 1.0f, 0.8f, 5f, 0f, 1.2f, 10, 0.8f, 0, 0.5f, 0, 0, 0, 0,
            0, 2.5f, 1.2f, 1, 1.5f, 0, 0, 2.5f, 0, 0.2f, 0, 0, 1.2f, 2.5f, 1, 2 };
    public static final boolean[] SUELO = { false, false, true, true, true, true, true, false, true, true, true,
            false, true, false, false, false, false, false, true, true, true, true, false, false, true, false, true,
            false, false, true, true, true, true };

    public static Mesh crear(String nombre, Fog fog) {
        if ("fumarola".equals(nombre)) {
            return fumarola(fog, 2);
        }
        if ("manta_cristal".equals(nombre)) {
            return mantaCristal(fog);
        }
        if ("caracol_duna".equals(nombre)) {
            return caracolDuna(fog);
        }
        if ("camarones_igneos".equals(nombre)) {
            return camaronesIgneos(fog);
        }
        if ("anguila_brasa".equals(nombre)) {
            return anguilaBrasa(fog);
        }
        if ("lirio_duna".equals(nombre)) {
            return lirioDuna(fog, 1);
        }
        if ("costillas".equals(nombre)) {
            return costillas(fog, 1);
        }
        if ("coral_ceniza".equals(nombre)) {
            return coralCeniza(fog, 3);
        }
        if ("basalto".equals(nombre)) {
            return basalto(fog, 2);
        }
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
        if ("tunel".equals(nombre)) {
            return tunel(fog, 1);
        }
        if ("planeador".equals(nombre)) {
            return planeador(fog);
        }
        if ("medusa".equals(nombre)) {
            return medusa(fog);
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
            b.tri(0, 0, 0, MB.fc(a0) * 5, 0, -MB.fs(a0) * 5, MB.fc(a1) * 5, 0,
                    -MB.fs(a1) * 5);
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
            float dx = MB.fs(rad);
            float dy = MB.fc(rad);
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
        return algaCinta(fog, semilla, false);
    }

    /**
     * Ribbon kelp; with vainas = true it also carries clusters of glowing orange seed pods
     * near the top. Only those (a few per grove) can be harvested for fibre and bulbs.
     */
    static Mesh algaCinta(Fog fog, int semilla, boolean vainas) {
        MB b = new MB();
        b.jitter = 9;
        float alto = 9 + Terreno.azar(1, semilla, 1) * 3;
        int tramos = 8;
        float px = 0;
        float pz = 0;
        for (int k = 0; k < tramos; k++) {
            float t0 = (float) k / tramos;
            float t1 = (float) (k + 1) / tramos;
            float nx = MB.fs(t1 * 3 + semilla) * 0.5f;
            float nz = MB.fc(t1 * 2.3f + semilla) * 0.4f;
            b.color = Terreno.mezcla(0x4f6a22, 0x8a9a35, t1);
            float w = 0.16f;
            // stalk: two crossed strips
            b.quad(px - w, alto * t0, pz, px + w, alto * t0, pz, nx + w, alto * t1, nz, nx - w, alto * t1, nz);
            b.quad(px, alto * t0, pz - w, px, alto * t0, pz + w, nx, alto * t1, nz + w, nx, alto * t1, nz - w);
            // a ribbon leaf on alternating sides
            if (k > 0) {
                float lado = (k & 1) == 0 ? 1 : -1;
                float ang = k * 73 + semilla * 40;
                float lx = MB.fc(ang * 0.0174f) * lado;
                float lz = MB.fs(ang * 0.0174f) * lado;
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
        if (vainas) {
            b.brillo = true;
            for (int i = 0; i < 3; i++) {
                float a = i * 2.1f + semilla;
                for (int k = 0; k < 3; k++) {
                    b.identity();
                    b.color = k == 1 ? 0xffc060 : 0xff8a30;
                    b.move(px + MB.fc(a) * 0.45f + k * 0.12f, alto * (0.62f + i * 0.12f) - k * 0.2f,
                            pz + MB.fs(a) * 0.45f);
                    b.ellipsoid(0.17f, 0.24f, 0.17f, 5, 3);
                }
            }
            b.brillo = false;
            b.identity();
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
            cxs[k] = MB.fs(t * 5) * 0.25f;
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
                float y0 = MB.fs(t0 * 2.6f) * 3.5f;
                float y1 = MB.fs(t1 * 2.6f) * 3.5f;
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
            float y0 = MB.fs(a0) * r;
            float z0 = MB.fc(a0) * r;
            float y1 = MB.fs(a1) * r;
            float z1 = MB.fc(a1) * r;
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
                float o0 = MB.fs(k * 1.3f) * 0.08f * s;
                float o1 = MB.fs((k + 1) * 1.3f) * 0.08f * s;
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
            b.box(0, MB.fs(a) * 0.18f, MB.fc(a) * 0.18f, 0.14f, 0.05f, 0.05f);
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
            xs[k + 1] = MB.fc(a) * 4.0f;
            ys[k + 1] = MB.fs(a) * 4.6f;
            float grosor = 1.35f - 0.45f * MB.fs(t * Math.PI);
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
                vy[j][i] = MB.fs(e) * ry * k - 0.4f;
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
            b.move(MB.fs(a) * 2.6f, 0.4f + i * 0.35f, MB.fc(a) * 2.6f);
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

    /**
     * Rock tunnel: an arched passage about 18 m long (local X axis), gently curved, open at
     * both ends so you swim in one side and out the other. Two-sided: the inside is drawn.
     * Boulders sit on its back.
     */
    static Mesh tunel(Fog fog, int semilla) {
        MB b = rocaMB();
        int anillos = 13;
        int perfil = 8;
        float largo = 18;
        float[][] vx = new float[anillos][perfil + 1];
        float[][] vy = new float[anillos][perfil + 1];
        float[][] vz = new float[anillos][perfil + 1];
        for (int k = 0; k < anillos; k++) {
            float t = (float) k / (anillos - 1);
            float x = -largo / 2 + largo * t;
            float zc = MB.fs(t * Math.PI) * 1.6f;
            float r = 2.5f + (Terreno.azar(k, semilla, 31) - 0.5f) * 0.5f;
            if (k == 0 || k == anillos - 1) {
                r += 0.4f; // the mouths flare out a little
            }
            for (int j = 0; j <= perfil; j++) {
                // the arch goes from the floor on one side, over the top, to the other side
                float a = (float) (Math.PI * j / perfil);
                float rr = r * (0.9f + Terreno.azar(k * 17 + j, semilla, 32) * 0.2f);
                vx[k][j] = x;
                vy[k][j] = MB.fs(a) * rr * 1.05f - 0.6f;
                vz[k][j] = zc + MB.fc(a) * rr;
            }
        }
        for (int k = 0; k < anillos - 1; k++) {
            for (int j = 0; j < perfil; j++) {
                b.color = Terreno.mezcla(0x857f74, 0x625d56, Terreno.azar(k * 31 + j, semilla, 33));
                b.quad(vx[k][j], vy[k][j], vz[k][j], vx[k + 1][j], vy[k + 1][j], vz[k + 1][j], vx[k + 1][j + 1],
                        vy[k + 1][j + 1], vz[k + 1][j + 1], vx[k][j + 1], vy[k][j + 1], vz[k][j + 1]);
            }
        }
        b.color = 0x6e685f;
        for (int i = 0; i < 4; i++) {
            float t = 0.15f + i * 0.23f;
            b.identity();
            b.move(-largo / 2 + largo * t, 2.3f, MB.fs(t * Math.PI) * 1.6f);
            b.roca(1.3f, 0.8f, 1.1f, 6, 3, semilla * 7 + i, 0.3f);
        }
        return b.build(fog, 255, true, 0);
    }

    /**
     * Sand glider: a small ray, sandy yellow with blue spots on its wings, that skims just
     * above the floor of the shallows. About 1.2 m across, faces +Z.
     */
    static Mesh planeador(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        b.color = 0xd8b86a;
        b.ellipsoid(0.22f, 0.07f, 0.32f, 8, 3);
        // wings
        b.color = 0xc9a65a;
        b.tri(0.18f, 0, 0.2f, 0.62f, 0.02f, -0.05f, 0.18f, 0, -0.25f);
        b.tri(-0.18f, 0, 0.2f, -0.18f, 0, -0.25f, -0.62f, 0.02f, -0.05f);
        // tail
        b.color = 0x8a6e3a;
        b.tri(0.03f, 0, -0.28f, 0, 0.02f, -0.85f, -0.03f, 0, -0.28f);
        // eyes and glowing blue spots
        b.color = 0x101418;
        b.box(0.08f, 0.06f, 0.24f, 0.04f, 0.04f, 0.04f);
        b.box(-0.08f, 0.06f, 0.24f, 0.04f, 0.04f, 0.04f);
        b.brillo = true;
        b.color = 0x5ac8ff;
        b.box(0.38f, 0.025f, -0.02f, 0.07f, 0.01f, 0.07f);
        b.box(-0.38f, 0.025f, -0.02f, 0.07f, 0.01f, 0.07f);
        b.box(0.26f, 0.03f, -0.12f, 0.05f, 0.01f, 0.05f);
        b.box(-0.26f, 0.03f, -0.12f, 0.05f, 0.01f, 0.05f);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Glow jelly: a translucent pink-violet bell with a bright core and trailing tentacles.
     * It drifts and pulses slowly. About 0.9 m with tentacles.
     */
    static Mesh medusa(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.brillo = true;
        b.color = 0xd070e8;
        b.lathe(new float[] { 0.42f, 0f, 0.4f, 0.12f, 0.3f, 0.26f, 0.15f, 0.34f, 0f, 0.36f }, 8,
                new int[] { 0xb050d8, 0xc864e6, 0xe08af2, 0xf0aaff });
        b.color = 0xffe0ff;
        b.move(0, 0.12f, 0);
        b.ellipsoid(0.12f, 0.1f, 0.12f, 6, 3);
        b.identity();
        b.color = 0xe090f0;
        for (int i = 0; i < 6; i++) {
            float a = i * 1.047f;
            float x = MB.fc(a) * 0.28f;
            float z = MB.fs(a) * 0.28f;
            float l = 0.6f + (i % 3) * 0.2f;
            b.quad(x - 0.02f, 0, z, x + 0.02f, 0, z, x * 0.7f + 0.01f, -l, z * 0.7f, x * 0.7f - 0.01f, -l, z * 0.7f);
        }
        return b.build(fog, 175, true, 0);
    }

    // ================================================================ first-person hands

    /**
     * A diving glove closed around a handle that runs along local Z, centred at x = cx with
     * radius r: the four fingers are rings that wrap the handle and the thumb lies on top,
     * so the tool is held, not resting on the hand.
     */
    private static void guante(MB b, float cx, float r) {
        float d = 2 * r + 0.026f;
        b.color = 0x1f2e38;
        for (int i = 0; i < 4; i++) {
            b.box(cx, -0.004f, -0.045f + i * 0.027f, d, d - 0.004f, 0.023f); // fingers around the grip
        }
        b.color = 0x263845;
        b.box(cx - 0.006f, r + 0.014f, -0.025f, 0.03f, 0.022f, 0.065f); // thumb over the grip
        b.color = 0x2a3c4a;
        b.box(cx + 0.012f, -0.008f, 0.1f, d + 0.02f, d + 0.012f, 0.11f); // back of the hand / wrist
        b.color = 0x34505f;
        b.box(cx + 0.012f, -0.008f, 0.22f, d + 0.04f, d + 0.03f, 0.13f); // cuff of the suit
        b.color = 0xe0702c;
        b.box(cx + 0.012f, d / 2 + 0.01f, 0.2f, d + 0.03f, 0.01f, 0.04f); // orange suit stripe
        // forearm in the suit sleeve, all the way to the elbow (off screen): the arm is one
        // piece from the hand to the elbow, so animating it around the elbow never
        // detaches the hand from the body
        b.color = 0x2c4250;
        b.box(cx + 0.012f, -0.012f, 0.56f, d + 0.05f, d + 0.04f, 0.58f);
        b.color = 0x24384a;
        b.box(cx + 0.012f, -0.03f, 0.56f, d + 0.056f, 0.02f, 0.5f); // seam under the sleeve
        b.color = 0xe0702c;
        b.box(cx + 0.012f, d / 2 + 0.012f, 0.5f, 0.016f, 0.012f, 0.5f); // stripe along the arm
    }

    /** where the elbow is in the hand mesh (the arm pivots around it) */
    static final float CODO_Z = 0.84f;

    /** right hand holding the knife: dark grip, titanium blade (metal texture) */
    static Mesh manoCuchillo(Fog fog) {
        MB b = new MB();
        b.primerPlano = true;
        b.jitter = 4;
        guante(b, 0.02f, 0.02f);
        b.color = 0x202428;
        b.box(0.02f, 0, -0.01f, 0.035f, 0.04f, 0.12f); // grip (mostly hidden by the fingers)
        b.color = 0x8a959c;
        b.box(0.02f, 0, -0.08f, 0.07f, 0.05f, 0.015f); // guard
        b.uvEscala = 8;
        b.uvCaja = true;
        b.textura = Texturas.metal();
        b.color = 0xd8e0e4;
        // blade: a long flat wedge with a point
        b.color = 0xf2f8fa;
        // a big, bright blade: two faces with a little thickness, and a point
        float t = 0.008f;
        b.quad(0.02f + t, 0.035f, -0.09f, 0.02f + t, 0.035f, -0.44f, 0.02f + t, -0.06f, -0.38f, 0.02f + t, -0.06f,
                -0.09f);
        b.quad(0.02f - t, -0.06f, -0.09f, 0.02f - t, -0.06f, -0.38f, 0.02f - t, 0.035f, -0.44f, 0.02f - t, 0.035f,
                -0.09f);
        b.color = 0xb8c4ca;
        b.quad(0.02f - t, 0.035f, -0.09f, 0.02f - t, 0.035f, -0.44f, 0.02f + t, 0.035f, -0.44f, 0.02f + t, 0.035f,
                -0.09f);
        // the cutting edge catches the light
        b.brillo = true;
        b.color = 0xffffff;
        b.quad(0.02f - t, -0.06f, -0.09f, 0.02f + t, -0.06f, -0.09f, 0.02f + t, -0.06f, -0.38f, 0.02f - t, -0.06f,
                -0.38f);
        b.brillo = false;
        return b.build(fog, 255, true, 0);
    }

    /** left hand holding the flashlight: grey body, glowing lens */
    static Mesh manoLinterna(Fog fog) {
        MB b = new MB();
        b.primerPlano = true;
        b.jitter = 4;
        guante(b, 0f, 0.036f);
        b.identity();
        b.move(0.0f, 0.0f, 0.02f);
        b.rot(0, -90);
        b.uvEscala = 8;
        b.uvCaja = true;
        b.textura = Texturas.metal();
        b.color = 0x6e7880;
        // closed body: back cap, grip, wider head (the lens is at the far end, +Y = forward)
        b.lathe(new float[] { 0f, -0.06f, 0.032f, -0.06f, 0.034f, 0.1f, 0.05f, 0.16f, 0.05f, 0.2f }, 8, null);
        b.textura = null;
        b.uvEscala = 0;
        b.brillo = true;
        b.color = 0xfff2c0;
        b.lathe(new float[] { 0.046f, 0.2f, 0f, 0.205f }, 8, null);
        return b.build(fog, 255, false, 0);
    }

    /** the visible beam of the flashlight: a long faint cone of light along local -Z */
    /**
     * The flashlight's pool of light: a soft radial glow (bright centre fading to black at
     * the rim, smooth-shaded) drawn additively in front of the camera. On the PSP the
     * per-vertex spot light alone barely shows on large faces; this makes the lamp read as
     * light cutting through the dark water.
     */
    static Mesh charcoLuz(Fog fog, int centro, int medio, float radio, float alto) {
        MB b = new MB();
        b.primerPlano = true;
        b.brillo = true;
        b.aditivo = true;
        b.suave = true;
        int seg = 16;
        float r1 = radio * 0.42f;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * i / seg), a1 = (float) (2 * Math.PI * (i + 1) / seg);
            float c0 = MB.fc(a0), s0 = MB.fs(a0);
            float c1 = MB.fc(a1), s1 = MB.fs(a1);
            b.triCol(0, 0, 0, centro, c0 * r1, s0 * r1 * alto, 0, medio, c1 * r1, s1 * r1 * alto, 0, medio);
            b.triCol(c0 * r1, s0 * r1 * alto, 0, medio, c0 * radio, s0 * radio * alto, 0, 0,
                    c1 * radio, s1 * radio * alto, 0, 0);
            b.triCol(c0 * r1, s0 * r1 * alto, 0, medio, c1 * radio, s1 * radio * alto, 0, 0,
                    c1 * r1, s1 * r1 * alto, 0, medio);
        }
        return b.build(fog, 254, true, 0);
    }

    static Mesh hazLinterna(Fog fog) {
        return charcoLuz(fog, 0x6a5e44, 0x2e281c, 3.2f, 1f);
    }

    /** the Remora's headlights: a wider, whiter pool */
    static Mesh hazRemora(Fog fog) {
        return charcoLuz(fog, 0x5a6478, 0x262c36, 5.5f, 0.7f);
    }

    // ================================================================ more life

    /**
     * School of silverlings: eight small silver fish in a loose formation, built as ONE mesh
     * so a whole school costs a single draw call. Faces +Z.
     */
    static Mesh cardumen(Fog fog) {
        MB b = new MB();
        b.jitter = 10;
        for (int i = 0; i < 8; i++) {
            b.identity();
            b.move((Terreno.azar(i, 3, 61) - 0.5f) * 1.6f, (Terreno.azar(i, 3, 62) - 0.5f) * 0.9f,
                    (Terreno.azar(i, 3, 63) - 0.5f) * 1.8f);
            b.color = i % 3 == 0 ? 0xd8e4ec : 0xb8c8d4;
            b.rot(0, 90);
            b.lathe(new float[] { 0f, -0.14f, 0.05f, -0.06f, 0.055f, 0.05f, 0f, 0.13f }, 5, null);
            b.identity();
            b.move((Terreno.azar(i, 3, 61) - 0.5f) * 1.6f, (Terreno.azar(i, 3, 62) - 0.5f) * 0.9f,
                    (Terreno.azar(i, 3, 63) - 0.5f) * 1.8f);
            b.color = 0x7a98b0;
            b.tri(0, 0, -0.12f, 0, 0.06f, -0.22f, 0, -0.06f, -0.22f);
            b.brillo = true;
            b.color = 0x9fe8ff;
            b.box(0, 0.03f, 0.02f, 0.012f, 0.012f, 0.12f); // shimmering stripe
            b.brillo = false;
        }
        return b.build(fog, 255, true, 0);
    }

    /**
     * Sand shepherd: a big, gentle grazer about 2.5 m long - a broad plated shell like a
     * flattened dome, four paddle fins and a long snout that sifts the sand. Faces +Z.
     */
    static Mesh pastorArena(Fog fog) {
        MB b = new MB();
        b.jitter = 7;
        b.lathe(new float[] { 0f, -0.35f, 0.9f, -0.25f, 1.1f, 0.05f, 0.8f, 0.4f, 0f, 0.55f }, 9,
                new int[] { 0x8a7a5a, 0x6f7f5a, 0x5a6e52, 0x748a5e });
        b.color = 0x9a8a64;
        b.box(0, -0.05f, 1.25f, 0.35f, 0.3f, 0.5f); // head
        b.box(0, -0.15f, 1.65f, 0.14f, 0.12f, 0.5f); // snout
        b.color = 0x5e6a4a;
        for (int i = 0; i < 4; i++) {
            float lado = i < 2 ? 1 : -1;
            float z = (i % 2 == 0) ? 0.55f : -0.55f;
            b.identity();
            b.move(lado * 1.0f, -0.1f, z);
            b.rot(2, lado * 15);
            b.box(lado * 0.4f, 0, 0, 0.8f, 0.06f, 0.35f);
        }
        b.identity();
        b.brillo = true;
        b.color = 0xa8ffb0;
        b.box(0.14f, 0.05f, 1.45f, 0.05f, 0.05f, 0.05f);
        b.box(-0.14f, 0.05f, 1.45f, 0.05f, 0.05f, 0.05f);
        return b.build(fog);
    }

    /** a clump of seagrass blades for the sand clearings: one mesh, a few dozen triangles */
    static Mesh pastoMarino(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 14;
        for (int i = 0; i < 11; i++) {
            float x = (Terreno.azar(i, semilla, 71) - 0.5f) * 1.6f;
            float z = (Terreno.azar(i, semilla, 72) - 0.5f) * 1.6f;
            float h = 0.5f + Terreno.azar(i, semilla, 73) * 0.7f;
            float inc = (Terreno.azar(i, semilla, 74) - 0.5f) * 0.5f;
            b.color = Terreno.mezcla(0x6e9a3a, 0x9ab84a, Terreno.azar(i, semilla, 75));
            b.tri(x - 0.05f, 0, z, x + 0.05f, 0, z, x + inc, h, z + inc * 0.5f);
            b.tri(x, 0, z - 0.05f, x, 0, z + 0.05f, x + inc * 0.5f, h * 0.9f, z + inc);
        }
        return b.build(fog, 255, true, 0);
    }

    /** barrel sponge: a thick porous vase, ochre outside, dark inside */
    static Mesh esponja(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 12;
        b.lathe(new float[] { 0.35f, 0f, 0.5f, 0.35f, 0.55f, 0.8f, 0.48f, 1.05f }, 8,
                new int[] { 0xb07a3a, 0xc48a44, 0xd09a50 });
        b.color = 0x4a2a1a;
        b.lathe(new float[] { 0.48f, 1.05f, 0.38f, 1.05f, 0.3f, 0.4f, 0f, 0.4f }, 8, null);
        return b.build(fog);
    }

    /** giant tube coral of the red plateau: a cluster of 3-4 m tubes with glowing mouths */
    static Mesh tuboGigante(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        for (int i = 0; i < 5; i++) {
            float a = i * 72 + Terreno.azar(i, semilla, 81) * 30;
            float d = i == 0 ? 0 : 0.8f + Terreno.azar(i, semilla, 82) * 0.6f;
            float h = 2.2f + Terreno.azar(i, semilla, 83) * 2.0f;
            float r = 0.32f + Terreno.azar(i, semilla, 84) * 0.15f;
            b.identity();
            b.rot(1, a);
            b.move(0, 0, d);
            b.rot(0, d * 12);
            b.brillo = false;
            b.color = 0x9a2e2a;
            b.lathe(new float[] { r * 1.3f, -0.2f, r, h * 0.6f, r * 1.2f, h }, 7,
                    new int[] { 0x7a2422, 0xa83a30 });
            b.color = 0xc04a36;
            b.lathe(new float[] { r * 1.2f, h, r * 0.8f, h }, 7, null);
            b.color = 0x3a1012;
            b.lathe(new float[] { r * 0.8f, h, r * 0.7f, h * 0.5f }, 7, null);
            b.brillo = true;
            b.color = 0xff9a4a;
            b.lathe(new float[] { r * 0.75f, h * 0.62f, 0f, h * 0.6f }, 7, null);
        }
        b.brillo = false;
        return b.build(fog);
    }

    /**
     * Wreckage of the Halcyon station: a bent hull plate, a cargo container lying on its
     * side and a broken beam, in brushed metal with orange markings. Along local X.
     */
    static Mesh restoEstacion(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 8;
        b.uvEscala = 0.5f;
        b.uvCaja = true;
        b.textura = Texturas.metal();
        // container
        b.identity();
        b.move(-1.8f, 0.6f, 0);
        b.rot(2, 8);
        b.color = 0x9aa4aa;
        b.box(0, 0, 0, 2.6f, 1.3f, 1.3f);
        b.color = 0xe0702c;
        b.box(0, 0.66f, 0, 2.6f, 0.03f, 0.3f);
        // bent hull plate (two panels at an angle)
        b.identity();
        b.move(1.6f, 0.2f, 0.5f);
        b.rot(1, 25);
        b.color = 0xc8ced2;
        b.quad(-1.2f, 0, -0.8f, 1.2f, 0, -0.8f, 1.2f, 1.6f, -0.3f, -1.2f, 1.6f, -0.3f);
        b.quad(-1.2f, 1.6f, -0.3f, 1.2f, 1.6f, -0.3f, 1.0f, 2.4f, 0.6f, -1.0f, 2.4f, 0.6f);
        b.color = 0x1fa3a8; // the station's teal stripe
        b.quad(-1.2f, 0.6f, -0.62f, 1.2f, 0.6f, -0.62f, 1.2f, 0.9f, -0.52f, -1.2f, 0.9f, -0.52f);
        // broken beam
        b.identity();
        b.move(0.2f, 0.25f, -1.2f);
        b.rot(1, -35);
        b.rot(2, 12);
        b.color = 0x6e767c;
        b.box(0, 0, 0, 3.4f, 0.25f, 0.25f);
        return b.build(fog, 255, true, 0);
    }

    // ================================================================ new biomes

    /**
     * Hydrothermal vent (Grietas termales): a tapered chimney of black mineral crust about
     * 4.5 m tall with a glowing orange throat and a faint additive heat plume above it.
     */
    static Mesh fumarola(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 8;
        b.lathe(new float[] { 1.3f, -0.3f, 1.0f, 1.2f, 0.75f, 2.6f, 0.55f, 3.8f, 0.45f, 4.5f, 0.3f, 4.3f }, 9,
                new int[] { 0x2c2628, 0x362e2e, 0x40302a, 0x4a3226, 0x6a2a14 });
        // mineral knobs on the flanks
        for (int i = 0; i < 4; i++) {
            float a = i * 1.7f + semilla;
            b.identity();
            b.color = 0x3a3030;
            b.move(MB.fc(a) * 0.9f, 0.8f + i * 0.7f, MB.fs(a) * 0.9f);
            b.roca(0.4f, 0.35f, 0.4f, 6, 3, semilla * 5 + i, 0.35f);
        }
        // the glowing throat
        b.identity();
        b.brillo = true;
        b.color = 0xff7a1e;
        b.move(0, 4.35f, 0);
        b.ellipsoid(0.32f, 0.12f, 0.32f, 8, 2);
        b.identity();
        b.color = 0xffc060;
        b.move(0, 4.42f, 0);
        b.ellipsoid(0.18f, 0.06f, 0.18f, 6, 2);
        b.brillo = false;
        b.identity();
        return b.build(fog, 255, false, 0);
    }

    /** the smoke above a vent: translucent puffs that grow as they rise (one mesh) */
    static Mesh pluma(Fog fog) {
        MB b = new MB();
        b.brillo = true;
        float[] alto = { 0.4f, 1.5f, 2.9f, 4.6f, 6.6f };
        float[] r = { 0.35f, 0.6f, 0.9f, 1.25f, 1.6f };
        int[] col = { 0x5a4438, 0x4a3e3a, 0x403a3a, 0x383636, 0x323232 };
        for (int i = 0; i < alto.length; i++) {
            b.identity();
            b.color = col[i];
            b.move((Terreno.azar(i, 4, 91) - 0.5f) * 0.5f * i, alto[i], (Terreno.azar(i, 4, 92) - 0.5f) * 0.5f * i);
            b.ellipsoid(r[i], r[i] * 0.8f, r[i], 7, 3);
        }
        b.identity();
        return b.build(fog, 80, true, 0);
    }

    /** a short stream of bubbles (rises in a loop above vents) */
    static Mesh burbujasFumarola(Fog fog) {
        MB b = new MB();
        b.brillo = true;
        b.color = 0xd8f4ff;
        for (int i = 0; i < 8; i++) {
            b.identity();
            b.move((Terreno.azar(i, 2, 81) - 0.5f) * 0.7f, i * 0.75f, (Terreno.azar(i, 2, 82) - 0.5f) * 0.7f);
            float r = 0.06f + Terreno.azar(i, 2, 83) * 0.07f;
            b.ellipsoid(r, r, r, 4, 2);
        }
        b.identity();
        return b.build(fog, 170, false, 0);
    }

    /**
     * Crystal manta (Praderas de arena): a gentle ray about 3 m across, pale blue and
     * translucent-looking, with glowing crystal ridges along its wings. Faces +Z.
     */
    static Mesh mantaCristal(Fog fog) {
        MB b = new MB();
        b.jitter = 5;
        b.color = 0x8fb8cc;
        b.ellipsoid(0.45f, 0.14f, 0.75f, 8, 3);
        // wings: two layers so they read from above and below
        b.color = 0xa8cde0;
        b.tri(0.35f, 0.02f, 0.55f, 1.55f, 0.08f, -0.1f, 0.4f, 0, -0.55f);
        b.tri(-0.35f, 0.02f, 0.55f, -0.4f, 0, -0.55f, -1.55f, 0.08f, -0.1f);
        b.color = 0x7aa2b8;
        b.tri(0.35f, -0.02f, 0.55f, 0.4f, -0.02f, -0.55f, 1.55f, 0.04f, -0.1f);
        b.tri(-0.35f, -0.02f, 0.55f, -1.55f, 0.04f, -0.1f, -0.4f, -0.02f, -0.55f);
        // head lobes
        b.color = 0x6f96aa;
        b.tri(0.18f, 0, 0.7f, 0.32f, -0.05f, 1.05f, 0.05f, 0, 0.75f);
        b.tri(-0.18f, 0, 0.7f, -0.05f, 0, 0.75f, -0.32f, -0.05f, 1.05f);
        // long whip tail
        b.color = 0x5a7a8c;
        b.tri(0.04f, 0, -0.7f, 0, 0.03f, -2.2f, -0.04f, 0, -0.7f);
        // glowing crystal ridges
        b.brillo = true;
        b.color = 0xbff4ff;
        for (int i = 0; i < 4; i++) {
            float x = 0.5f + i * 0.25f;
            b.box(x, 0.08f, 0.1f - i * 0.08f, 0.06f, 0.03f, 0.18f);
            b.box(-x, 0.08f, 0.1f - i * 0.08f, 0.06f, 0.03f, 0.18f);
        }
        b.color = 0x101820;
        b.brillo = false;
        b.box(0.2f, 0.1f, 0.55f, 0.05f, 0.05f, 0.05f);
        b.box(-0.2f, 0.1f, 0.55f, 0.05f, 0.05f, 0.05f);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Dune snail (Praderas de arena): a slow grazer half a metre long with a spiral shell
     * whose ridges glow soft green. Faces +Z.
     */
    static Mesh caracolDuna(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        // foot
        b.color = 0xb8a080;
        b.move(0, 0.08f, 0.05f);
        b.ellipsoid(0.16f, 0.08f, 0.32f, 7, 2);
        // shell: stacked ellipsoids spiralling up
        for (int i = 0; i < 4; i++) {
            float k = 1 - i * 0.22f;
            b.identity();
            b.color = (i & 1) == 0 ? 0xd8c8a0 : 0xc0a878;
            b.move(0.03f * i, 0.2f + i * 0.09f, -0.05f - i * 0.03f);
            b.ellipsoid(0.2f * k, 0.13f * k, 0.22f * k, 7, 3);
        }
        // glowing ridge spots and eye stalks
        b.identity();
        b.brillo = true;
        b.color = 0x9cff9a;
        b.box(0.17f, 0.22f, -0.05f, 0.04f, 0.04f, 0.04f);
        b.box(-0.14f, 0.3f, -0.08f, 0.04f, 0.04f, 0.04f);
        b.box(0.05f, 0.44f, -0.12f, 0.035f, 0.035f, 0.035f);
        b.color = 0xffffff;
        b.box(0.06f, 0.24f, 0.36f, 0.025f, 0.025f, 0.025f);
        b.box(-0.06f, 0.24f, 0.36f, 0.025f, 0.025f, 0.025f);
        b.brillo = false;
        b.color = 0x9a8466;
        b.box(0.06f, 0.17f, 0.33f, 0.012f, 0.06f, 0.012f);
        b.box(-0.06f, 0.17f, 0.33f, 0.012f, 0.06f, 0.012f);
        return b.build(fog, 255, false, 0);
    }

    /**
     * Ember shrimp school (Grietas termales): a dozen small red shrimp with glowing tails,
     * one mesh for the whole group. Faces +Z.
     */
    static Mesh camaronesIgneos(Fog fog) {
        MB b = new MB();
        b.jitter = 10;
        for (int i = 0; i < 10; i++) {
            float ox = (Terreno.azar(i, 5, 71) - 0.5f) * 1.6f;
            float oy = (Terreno.azar(i, 5, 72) - 0.5f) * 0.8f;
            float oz = (Terreno.azar(i, 5, 73) - 0.5f) * 1.6f;
            b.identity();
            b.move(ox, oy, oz);
            b.color = i % 3 == 0 ? 0xd8482a : 0xb8361e;
            b.ellipsoid(0.04f, 0.04f, 0.1f, 5, 2);
            b.brillo = true;
            b.color = 0xffa040;
            b.tri(0, 0.02f, -0.08f, 0.04f, 0, -0.16f, -0.04f, 0, -0.16f);
            b.brillo = false;
        }
        b.identity();
        return b.build(fog, 255, true, 0);
    }

    /**
     * Ember eel (Grietas termales): a black serpent about 4 m long with glowing orange
     * bands and fins, a predator that hunts in the rifts. Faces +Z.
     */
    static Mesh anguilaBrasa(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        int seg = 10;
        float largo = 4.2f;
        float[] cxs = new float[seg + 1];
        float[] cys = new float[seg + 1];
        float[] czs = new float[seg + 1];
        float[] rs = new float[seg + 1];
        int[] bandas = new int[seg];
        for (int k = 0; k <= seg; k++) {
            float t = (float) k / seg;
            czs[k] = largo * (0.5f - t);
            cxs[k] = MB.fs(t * 6) * 0.3f;
            cys[k] = 0.1f;
            rs[k] = 0.26f * (1 - t * 0.85f) + 0.03f;
            if (k < seg) {
                bandas[k] = (k % 3) == 1 ? 0x8a2a10 : 0x1c181a;
            }
        }
        b.tubo(cxs, cys, czs, rs, 6, bandas, true);
        // glowing dorsal and tail fins
        b.brillo = true;
        b.color = 0xff6a1a;
        for (int k = 1; k < seg; k += 2) {
            float zm = (czs[k] + czs[k + 1]) / 2;
            float xm = (cxs[k] + cxs[k + 1]) / 2;
            float y0 = 0.1f + rs[k] * 0.9f;
            b.tri(xm, y0, zm + 0.15f, xm, y0 + 0.22f, zm - 0.05f, xm, y0, zm - 0.18f);
        }
        b.tri(cxs[seg], 0.1f, -largo / 2, cxs[seg], 0.4f, -largo / 2 - 0.5f, cxs[seg], -0.2f, -largo / 2 - 0.45f);
        // eyes
        b.color = 0xffe070;
        b.box(0.16f, 0.2f, largo / 2 - 0.05f, 0.05f, 0.05f, 0.06f);
        b.box(-0.16f, 0.2f, largo / 2 - 0.05f, 0.05f, 0.05f, 0.06f);
        b.brillo = false;
        // jaw
        b.color = 0x2a2224;
        float zh = largo / 2;
        b.quad(-0.24f, 0.0f, zh, 0.24f, 0.0f, zh, 0.1f, 0.08f, zh + 0.5f, -0.1f, 0.08f, zh + 0.5f);
        b.quad(-0.24f, 0.24f, zh, -0.1f, 0.08f, zh + 0.5f, 0.1f, 0.08f, zh + 0.5f, 0.24f, 0.24f, zh);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Dune lily (Praderas de arena): three tall slender stalks, each ending in a pale cup
     * with a glowing violet heart. About 2.5 m tall.
     */
    static Mesh lirioDuna(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 8;
        for (int i = 0; i < 3; i++) {
            float a = i * 2.1f + semilla;
            float bx = MB.fc(a) * 0.35f, bz = MB.fs(a) * 0.35f;
            float alto = 1.8f + Terreno.azar(i, semilla, 41) * 0.9f;
            float tx = bx * 1.8f, tz = bz * 1.8f;
            b.identity();
            b.color = 0x5e8a4a;
            b.quad(bx - 0.04f, 0, bz, bx + 0.04f, 0, bz, tx + 0.03f, alto, tz, tx - 0.03f, alto, tz);
            b.quad(bx, 0, bz - 0.04f, bx, 0, bz + 0.04f, tx, alto, tz + 0.03f, tx, alto, tz - 0.03f);
            b.color = 0xe8e0f0;
            b.move(tx, alto, tz);
            b.lathe(new float[] { 0.02f, 0f, 0.22f, 0.18f, 0.3f, 0.34f }, 6, null);
            b.brillo = true;
            b.color = 0xc080ff;
            b.ellipsoid(0.09f, 0.12f, 0.09f, 5, 2);
            b.brillo = false;
        }
        // leaves at the base
        b.identity();
        b.color = 0x4f7a3c;
        for (int i = 0; i < 4; i++) {
            float a = i * 1.57f + semilla;
            float lx = MB.fc(a), lz = MB.fs(a);
            b.tri(0, 0.05f, 0, lx * 0.9f - lz * 0.15f, 0.3f, lz * 0.9f + lx * 0.15f, lx * 0.9f + lz * 0.15f, 0.3f,
                    lz * 0.9f - lx * 0.15f);
        }
        return b.build(fog, 255, true, 0);
    }

    /**
     * Fossil ribcage (Praderas de arena): the bleached ribs of some ancient giant, half
     * buried in the sand along local X. You can swim under the arches. About 16 m long.
     */
    static Mesh costillas(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 6;
        // spine, lying along X
        float[] sx = new float[7], sy = new float[7], sz = new float[7], sr = new float[7];
        for (int k = 0; k < 7; k++) {
            float t = k / 6f;
            sx[k] = -8 + 16 * t;
            sy[k] = 5.2f - (float) Math.abs(t - 0.45f) * 3.5f;
            sz[k] = MB.fs(t * 3 + semilla) * 0.4f;
            sr[k] = 0.45f - Math.abs(t - 0.4f) * 0.35f;
        }
        b.tubo(sx, sy, sz, sr, 6, new int[] { 0xd8d0bc, 0xcfc6b0, 0xd8d0bc, 0xc4bca6, 0xd8d0bc, 0xcfc6b0 }, true);
        // ribs: curved bones arching down to the sand on both sides
        b.color = 0xd2cab4;
        for (int k = 1; k < 6; k++) {
            float x = sx[k];
            float top = sy[k];
            float alcance = 3.6f + MB.fs(k * 1.3f) * 0.5f;
            for (int lado = -1; lado <= 1; lado += 2) {
                float[] rx = new float[5], ry = new float[5], rz = new float[5], rr = new float[5];
                for (int j = 0; j < 5; j++) {
                    float u = j / 4f;
                    float ang = u * 1.75f;
                    rx[j] = x + u * 0.6f;
                    ry[j] = top * MB.fc(ang * 0.9f) - u * 0.8f;
                    rz[j] = sz[k] + lado * alcance * MB.fs(ang);
                    rr[j] = 0.22f - u * 0.1f;
                }
                b.tubo(rx, ry, rz, rr, 5, null, false);
            }
        }
        // the skull, half sunk at one end
        b.identity();
        b.color = 0xcfc6b0;
        b.move(8.8f, 1.6f, sz[6]);
        b.roca(1.6f, 1.2f, 1.3f, 7, 4, semilla * 11, 0.15f);
        b.color = 0x1a1410;
        b.box(9.6f, 1.9f, sz[6] + 0.7f, 0.3f, 0.25f, 0.2f);
        b.box(9.6f, 1.9f, sz[6] - 0.7f, 0.3f, 0.25f, 0.2f);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Ash coral (Grietas termales): a cluster of black branching tubes whose tips glow
     * like embers. About 2 m tall.
     */
    static Mesh coralCeniza(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        for (int i = 0; i < 6; i++) {
            float a = i * 1.05f + semilla;
            float d = 0.2f + Terreno.azar(i, semilla, 51) * 0.5f;
            float h = 0.9f + Terreno.azar(i, semilla, 52) * 1.2f;
            float x = MB.fc(a) * d, z = MB.fs(a) * d;
            b.identity();
            b.move(x, 0, z);
            b.lathe(new float[] { 0.16f, 0f, 0.12f, h * 0.6f, 0.1f, h }, 6, new int[] { 0x2a2426, 0x3a2c2a });
            b.brillo = true;
            b.color = i % 2 == 0 ? 0xff6a20 : 0xffa040;
            b.move(0, h, 0);
            b.ellipsoid(0.1f, 0.06f, 0.1f, 6, 2);
            b.brillo = false;
        }
        b.identity();
        return b.build(fog, 255, false, 0);
    }

    /**
     * Basalt columns (Grietas termales): a cluster of hexagonal stone columns of different
     * heights, dark grey with a reddish sheen. About 3 m across, up to 5 m tall.
     */
    static Mesh basalto(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 8;
        for (int i = 0; i < 7; i++) {
            float x, z;
            if (i == 0) {
                x = 0;
                z = 0;
            } else {
                float a = (i - 1) * 1.0472f;
                x = MB.fc(a) * 0.95f;
                z = MB.fs(a) * 0.95f;
            }
            float h = 1.5f + Terreno.azar(i, semilla, 61) * 3.5f;
            b.identity();
            b.move(x, 0, z);
            b.lathe(new float[] { 0.5f, -0.5f, 0.5f, h, 0f, h }, 6,
                    new int[] { Terreno.mezcla(0x3a3436, 0x4a3a36, Terreno.azar(i, semilla, 62)), 0x5a4a44 });
        }
        b.identity();
        return b.build(fog);
    }

    // ================================================================ jointed creatures
    // The PSP has no hardware skinning: creatures that need to bend are built from rigid
    // parts ("bones") drawn in a chain, each with its own transform.

    /** ironback crab body: shell, belly and eye stalks (legs and claws are separate parts) */
    static Mesh acorazadoCuerpo(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.color = 0x9a4e2e;
        b.move(0, 0.55f, 0);
        b.lathe(new float[] { 0.75f, -0.15f, 0.8f, 0.05f, 0.62f, 0.3f, 0.3f, 0.45f, 0f, 0.5f }, 6,
                new int[] { 0x5a3a30, 0x2f8f8a, 0x9a4e2e, 0xb05a32 });
        b.color = 0x6c4a3a;
        b.lathe(new float[] { 0f, -0.35f, 0.6f, -0.18f, 0.75f, -0.15f }, 6, null);
        b.identity();
        b.brillo = true;
        b.color = 0x8fff9a;
        b.box(0.16f, 1.05f, 0.62f, 0.07f, 0.07f, 0.07f);
        b.box(-0.16f, 1.05f, 0.62f, 0.07f, 0.07f, 0.07f);
        return b.build(fog);
    }

    /** one crab leg, hip at the origin, reaching out along +Z and down to the floor */
    static Mesh acorazadoPata(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        b.color = 0x7a4630;
        b.rot(0, -25);
        b.box(0, 0, 0.3f, 0.1f, 0.1f, 0.6f);
        b.move(0, 0, 0.58f);
        b.rot(0, 75);
        b.color = 0x2f8f8a;
        b.box(0, 0, 0.28f, 0.08f, 0.08f, 0.56f);
        return b.build(fog);
    }

    /** a crab claw arm, shoulder at the origin, along +Z */
    static Mesh acorazadoPinza(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        b.color = 0x8a4a2e;
        b.box(0, 0, 0.25f, 0.16f, 0.16f, 0.5f);
        b.move(0, 0, 0.62f);
        b.color = 0xb05a32;
        b.box(0, 0.06f, 0.12f, 0.3f, 0.2f, 0.34f);
        b.color = 0x2f8f8a;
        b.box(0, -0.1f, 0.18f, 0.22f, 0.08f, 0.3f);
        return b.build(fog);
    }

    /**
     * A serpent body cut into four segments for chain animation. Segment k spans the body
     * from its front joint (z = 0) back to z = -largo/4; the head (jaws, eyes) sits in
     * front of segment 0 and the tail fin behind segment 3.
     * tipo 0 = sawtooth (grey, orange saw spikes), 1 = ember eel (black, glowing bands).
     */
    static Mesh[] serpiente(Fog fog, int tipo) {
        Mesh[] m = new Mesh[4];
        float largo = tipo == 0 ? 4.5f : 4.2f;
        float paso = largo / 4;
        for (int k = 0; k < 4; k++) {
            MB b = new MB();
            b.jitter = 6;
            int n = 3;
            float[] cx = new float[n + 1], cy = new float[n + 1], cz = new float[n + 1], r = new float[n + 1];
            int[] bandas = new int[n];
            for (int j = 0; j <= n; j++) {
                float t = (k + (float) j / n) / 4f;
                cx[j] = 0;
                cy[j] = tipo == 0 ? 0.15f : 0.1f;
                // overlap a little into the next segment so no gap opens when bending
                cz[j] = -paso * j / n + (j == 0 ? 0.08f : 0) - (j == n ? 0.08f : 0);
                r[j] = tipo == 0 ? 0.36f * (1 - t * 0.8f) + 0.04f : 0.26f * (1 - t * 0.85f) + 0.03f;
                if (j < n) {
                    int q = k * n + j;
                    bandas[j] = tipo == 0 ? ((q & 1) == 0 ? 0x4a5560 : 0x56616c) : ((q % 3) == 1 ? 0x8a2a10 : 0x1c181a);
                }
            }
            b.tubo(cx, cy, cz, r, tipo == 0 ? 7 : 6, bandas, true);
            // dorsal spikes / fins along the back of this segment
            b.identity();
            b.brillo = tipo == 1;
            b.color = tipo == 0 ? 0xe0702c : 0xff6a1a;
            for (int j = 0; j < n; j++) {
                float t = (k + (j + 0.5f) / n) / 4f;
                float rm = tipo == 0 ? 0.36f * (1 - t * 0.8f) + 0.04f : 0.26f * (1 - t * 0.85f) + 0.03f;
                float zm = -paso * (j + 0.5f) / n;
                float y0 = cy[0] + rm * 0.85f;
                float h = (tipo == 0 ? 0.35f : 0.22f) * (1 - t * 0.6f);
                b.tri(0, y0, zm + 0.15f, 0, y0 + h, zm - 0.05f, 0, y0, zm - 0.18f);
            }
            b.brillo = false;
            if (k == 3) {
                // tail fin
                b.color = tipo == 0 ? 0xc85a26 : 0xff6a1a;
                b.brillo = tipo == 1;
                b.tri(0, cy[0], -paso + 0.05f, 0, cy[0] + 0.45f, -paso - 0.65f, 0, cy[0] - 0.3f, -paso - 0.55f);
                b.brillo = false;
            }
            if (k == 0) {
                float y = cy[0];
                if (tipo == 0) {
                    // wide jaw: upper and lower wedges, teeth, amber eyes
                    b.color = 0x3c454f;
                    b.quad(-0.34f, y - 0.13f, 0, 0.34f, y - 0.13f, 0, 0.16f, y - 0.03f, 0.75f, -0.16f, y - 0.03f, 0.75f);
                    b.quad(-0.34f, y + 0.15f, 0, -0.16f, y - 0.03f, 0.75f, 0.16f, y - 0.03f, 0.75f, 0.34f, y + 0.15f, 0);
                    b.tri(-0.34f, y - 0.13f, 0, -0.16f, y - 0.03f, 0.75f, -0.34f, y + 0.15f, 0);
                    b.tri(0.34f, y - 0.13f, 0, 0.34f, y + 0.15f, 0, 0.16f, y - 0.03f, 0.75f);
                    b.color = 0x8a3a30;
                    b.quad(-0.3f, y - 0.2f, 0, -0.14f, y - 0.4f, 0.6f, 0.14f, y - 0.4f, 0.6f, 0.3f, y - 0.2f, 0);
                    b.color = 0xf2ead8;
                    for (int i = 0; i < 4; i++) {
                        float x = -0.18f + i * 0.12f;
                        b.tri(x - 0.04f, y - 0.11f, 0.5f, x + 0.04f, y - 0.11f, 0.5f, x, y - 0.29f, 0.5f);
                        b.tri(x + 0.04f, y - 0.11f, 0.5f, x - 0.04f, y - 0.11f, 0.5f, x, y - 0.29f, 0.5f);
                    }
                    b.brillo = true;
                    b.color = 0xffb02e;
                    b.box(0.3f, y + 0.05f, 0.15f, 0.08f, 0.08f, 0.12f);
                    b.box(-0.3f, y + 0.05f, 0.15f, 0.08f, 0.08f, 0.12f);
                    b.brillo = false;
                } else {
                    b.color = 0x2a2224;
                    b.quad(-0.24f, y - 0.1f, 0, 0.24f, y - 0.1f, 0, 0.1f, y - 0.02f, 0.5f, -0.1f, y - 0.02f, 0.5f);
                    b.quad(-0.24f, y + 0.14f, 0, -0.1f, y - 0.02f, 0.5f, 0.1f, y - 0.02f, 0.5f, 0.24f, y + 0.14f, 0);
                    b.brillo = true;
                    b.color = 0xffe070;
                    b.box(0.16f, y + 0.1f, 0.05f, 0.05f, 0.05f, 0.06f);
                    b.box(-0.16f, y + 0.1f, 0.05f, 0.05f, 0.05f, 0.06f);
                    b.brillo = false;
                }
            }
            m[k] = b.build(fog, 255, true, 0);
        }
        return m;
    }

    // ================================================================ story and capsule

    /** Container 7: a dented cargo container lying on the plateau, doors ajar. ~6 m long. */
    static Mesh contenedor(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        b.uvEscala = 0.5f;
        b.uvCaja = true;
        b.textura = Texturas.metal();
        b.color = 0xb06a34;
        b.rot(2, 6); // lying a little tilted
        b.box(0, 1.4f, 0, 6.2f, 2.8f, 2.8f);
        b.color = 0x8a5228;
        for (int i = 0; i < 7; i++) {
            b.box(-2.7f + i * 0.9f, 1.4f, 0, 0.12f, 2.9f, 2.9f); // ribs
        }
        // doors at +X, one open
        b.color = 0x9a5c2c;
        b.box(3.15f, 1.4f, 0.7f, 0.1f, 2.6f, 1.3f);
        b.identity();
        b.move(3.2f, 0, -0.65f);
        b.rot(1, -55);
        b.box(0.65f, 1.4f, 0, 1.3f, 2.6f, 0.1f);
        b.identity();
        b.rot(2, 6);
        // white "7" on the side and a hazard stripe
        b.textura = null;
        b.uvEscala = 0;
        b.color = 0xeae6dc;
        b.box(0.3f, 2.3f, 1.42f, 1.1f, 0.22f, 0.04f);
        b.box(0.7f, 1.6f, 1.42f, 0.22f, 1.3f, 0.04f);
        b.color = 0xe8c020;
        b.box(-2.0f, 0.5f, 1.42f, 1.6f, 0.25f, 0.04f);
        // transponder light
        b.brillo = true;
        b.color = 0xffd040;
        b.box(-2.6f, 2.95f, 0, 0.25f, 0.2f, 0.25f);
        return b.build(fog);
    }

    /** Research module "Tetis": a broken horizontal cylinder with glowing panels. ~8 m. */
    static Mesh moduloTetis(Fog fog) {
        MB b = new MB();
        b.jitter = 6;
        b.uvEscala = 0.5f;
        b.uvCaja = true;
        b.textura = Texturas.metal();
        b.move(0, 2.0f, 0);
        b.rot(2, 90);
        b.rot(0, 8);
        b.lathe(new float[] { 0f, -4f, 1.7f, -3.8f, 2.0f, -3.4f, 2.0f, 2.8f, 1.8f, 3.6f }, 10,
                new int[] { 0x8a949c, 0xa8b0b6, 0xc0c6ca, 0x9aa2a8 });
        b.identity();
        b.textura = null;
        b.uvEscala = 0;
        // broken ring and debris at the torn end
        b.color = 0x5a6268;
        b.move(4.0f, 1.6f, 0.6f);
        b.box(0, 0, 0, 0.6f, 1.6f, 0.3f);
        b.identity();
        b.color = 0x3a4248;
        b.box(-1, 4.1f, 0, 0.15f, 1.6f, 0.15f); // antenna
        b.brillo = true;
        b.color = 0x5ff0e0;
        b.box(-1.5f, 2.0f, 2.02f, 1.4f, 0.5f, 0.05f);
        b.box(1.2f, 2.3f, 2.02f, 0.8f, 0.35f, 0.05f);
        b.color = 0xff5a3a;
        b.box(-1, 4.95f, 0, 0.2f, 0.2f, 0.2f);
        return b.build(fog);
    }

    /** Resonant crystal: a cluster of tall violet crystals glowing from within. ~4 m. */
    static Mesh cristalResonante(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.color = 0x3a3450;
        b.roca(1.8f, 0.7f, 1.6f, 7, 3, 77, 0.3f);
        b.brillo = true;
        for (int i = 0; i < 6; i++) {
            float a = i * 1.05f;
            float d = i == 0 ? 0 : 0.8f;
            float h = i == 0 ? 3.6f : 1.6f + (i % 3) * 0.5f;
            b.identity();
            b.move(MB.fc(a) * d, 0.3f, MB.fs(a) * d);
            b.rot(2, i == 0 ? 0 : (MB.fc(a) * 25));
            b.rot(0, i == 0 ? 0 : (MB.fs(a) * -25));
            b.color = i == 0 ? 0xc89cff : (i % 2 == 0 ? 0xa070f0 : 0x8a5ae0);
            b.lathe(new float[] { 0.32f, 0f, 0.36f, h * 0.7f, 0f, h }, 6, null);
        }
        b.identity();
        return b.build(fog);
    }

    /** thermal crystal (resource of the thermal rifts): orange crystals on black rock */
    static Mesh cristalTermico(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.color = 0x2c2626;
        b.roca(0.7f, 0.35f, 0.6f, 6, 3, 31, 0.3f);
        b.brillo = true;
        for (int i = 0; i < 4; i++) {
            float a = i * 1.6f;
            b.identity();
            b.move(MB.fc(a) * 0.25f, 0.2f, MB.fs(a) * 0.25f);
            b.rot(2, MB.fc(a) * 30);
            b.color = i % 2 == 0 ? 0xff9a30 : 0xffc060;
            b.lathe(new float[] { 0.1f, 0f, 0.11f, 0.4f + i * 0.08f, 0f, 0.6f + i * 0.1f }, 5, null);
        }
        b.identity();
        return b.build(fog);
    }

    /** sea anemone: a squat column with a crown of swaying tentacles. ~0.8 m */
    static Mesh anemona(Fog fog, int semilla) {
        MB b = new MB();
        b.jitter = 10;
        int col = semilla % 2 == 0 ? 0xe0604a : 0x9a5ae0;
        b.lathe(new float[] { 0.22f, 0f, 0.2f, 0.3f, 0.26f, 0.38f, 0f, 0.4f }, 7,
                new int[] { 0x6a4a3a, Terreno.mezcla(col, 0x402020, 0.4f), col });
        for (int i = 0; i < 10; i++) {
            float a = i * 0.628f + semilla;
            float c = MB.fc(a), sn = MB.fs(a);
            b.identity();
            b.color = i % 2 == 0 ? col : Terreno.mezcla(col, 0xffffff, 0.3f);
            b.quad(c * 0.2f - sn * 0.03f, 0.38f, sn * 0.2f + c * 0.03f, c * 0.2f + sn * 0.03f, 0.38f, sn * 0.2f - c * 0.03f,
                    c * 0.42f, 0.75f, sn * 0.42f, c * 0.4f, 0.78f, sn * 0.4f);
        }
        b.brillo = true;
        b.color = Terreno.mezcla(col, 0xffffff, 0.6f);
        b.identity();
        b.move(0, 0.4f, 0);
        b.ellipsoid(0.08f, 0.04f, 0.08f, 5, 2);
        return b.build(fog, 255, true, 0);
    }

    /** coral turtle: a slow, gentle sea turtle with a reef growing on its shell. ~1.4 m */
    static Mesh tortuga(Fog fog) {
        MB b = new MB();
        b.jitter = 7;
        b.color = 0x6a7a4a;
        b.move(0, 0.05f, 0);
        b.ellipsoid(0.55f, 0.24f, 0.7f, 8, 3);
        // plates and little corals on the shell
        b.color = 0x8a6a3a;
        b.identity();
        b.move(0, 0.22f, 0);
        b.ellipsoid(0.42f, 0.1f, 0.55f, 7, 2);
        b.brillo = true;
        b.color = 0xff8a6a;
        b.box(0.15f, 0.34f, 0.1f, 0.08f, 0.12f, 0.08f);
        b.color = 0xf0d060;
        b.box(-0.18f, 0.33f, -0.15f, 0.07f, 0.1f, 0.07f);
        b.brillo = false;
        // head
        b.color = 0x7a8a5a;
        b.identity();
        b.move(0, 0.06f, 0.82f);
        b.ellipsoid(0.16f, 0.13f, 0.2f, 6, 3);
        b.color = 0x101010;
        b.box(0.1f, 0.12f, 0.92f, 0.03f, 0.03f, 0.03f);
        b.box(-0.1f, 0.12f, 0.92f, 0.03f, 0.03f, 0.03f);
        // flippers
        b.color = 0x5a6a3e;
        b.tri(0.4f, 0, 0.35f, 0.95f, -0.02f, 0.1f, 0.42f, 0, 0.05f);
        b.tri(-0.4f, 0, 0.35f, -0.42f, 0, 0.05f, -0.95f, -0.02f, 0.1f);
        b.tri(0.35f, 0, -0.45f, 0.6f, -0.02f, -0.7f, 0.2f, 0, -0.55f);
        b.tri(-0.35f, 0, -0.45f, -0.2f, 0, -0.55f, -0.6f, -0.02f, -0.7f);
        return b.build(fog, 255, true, 0);
    }

    /**
     * Inside the escape pod (seen from its centre, eye at y = 1.1): a round cabin about
     * 3.6 m across. Stations at fixed headings (degrees of yaw, 0 = -Z): fabricator 0,
     * radio 72, medical dispenser 144, storage locker 216, ladder to the hatch 288.
     */
    static Mesh interiorCapsula(Fog fog) {
        MB b = new MB();
        b.jitter = 4;
        // walls: a cylinder seen from inside, with a ring of panels
        int seg = 14;
        float r = 1.8f;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (2 * Math.PI * i / seg), a1 = (float) (2 * Math.PI * (i + 1) / seg);
            float x0 = MB.fc(a0) * r, z0 = MB.fs(a0) * r, x1 = MB.fc(a1) * r, z1 = MB.fs(a1) * r;
            b.color = i % 2 == 0 ? 0xc8ccc4 : 0xb8bcb4;
            b.quad(x0, 0, z0, x0, 2.4f, z0, x1, 2.4f, z1, x1, 0, z1);
            b.color = 0x5a6268;
            b.quad(x0 * 0.99f, 0.95f, z0 * 0.99f, x0 * 0.99f, 1.0f, z0 * 0.99f, x1 * 0.99f, 1.0f, z1 * 0.99f, x1 * 0.99f,
                    0.95f, z1 * 0.99f);
            // floor and ceiling
            b.color = 0x3a4046;
            b.tri(0, 0, 0, x1, 0, z1, x0, 0, z0);
            b.color = 0x9aa0a4;
            b.tri(0, 2.4f, 0, x0, 2.4f, z0, x1, 2.4f, z1);
        }
        b.color = 0x2a3036;
        b.box(0, 0.01f, 0, 1.4f, 0.02f, 1.4f); // floor grating
        // stations, each built at heading h: position (-sin h, -cos h) * radius
        estacion(b, 0, 1.55f);
        b.color = 0x6a7278; // fabricator: a cabinet with a glowing screen and an arm
        b.box(0, 1.1f, 0, 0.9f, 1.2f, 0.35f);
        b.color = 0x3a4248;
        b.box(0, 0.75f, -0.18f, 0.7f, 0.08f, 0.25f);
        b.brillo = true;
        b.color = 0x5ff0e0;
        b.box(0, 1.35f, 0.18f, 0.6f, 0.35f, 0.02f);
        b.brillo = false;
        estacion(b, 72, 1.6f);
        b.color = 0x4a5258; // radio: a console with dials
        b.box(0, 1.05f, 0, 0.7f, 0.5f, 0.3f);
        b.color = 0x22282c;
        b.box(-0.2f, 1.05f, 0.16f, 0.12f, 0.12f, 0.02f);
        b.box(0.2f, 1.05f, 0.16f, 0.12f, 0.12f, 0.02f);
        b.brillo = true;
        b.color = 0xffb040;
        b.box(0, 1.2f, 0.16f, 0.3f, 0.08f, 0.02f);
        b.brillo = false;
        estacion(b, 144, 1.6f);
        b.color = 0xe8eae6; // medical dispenser: white box, red cross
        b.box(0, 1.25f, 0, 0.5f, 0.55f, 0.25f);
        b.color = 0xd02a2a;
        b.box(0, 1.25f, 0.13f, 0.28f, 0.08f, 0.02f);
        b.box(0, 1.25f, 0.13f, 0.08f, 0.28f, 0.02f);
        estacion(b, 216, 1.55f);
        b.color = 0x7a8a96; // storage locker
        b.box(0, 1.0f, 0, 0.75f, 1.9f, 0.4f);
        b.color = 0x5a6a76;
        b.box(0, 1.0f, 0.21f, 0.02f, 1.8f, 0.02f);
        b.color = 0x2a3036;
        b.box(0.08f, 1.05f, 0.22f, 0.04f, 0.18f, 0.03f);
        estacion(b, 288, 1.5f);
        b.color = 0x8a9298; // ladder up to the hatch
        b.box(-0.22f, 1.2f, 0, 0.05f, 2.4f, 0.05f);
        b.box(0.22f, 1.2f, 0, 0.05f, 2.4f, 0.05f);
        for (int i = 0; i < 6; i++) {
            b.box(0, 0.3f + i * 0.38f, 0, 0.44f, 0.04f, 0.04f);
        }
        b.identity();
        b.color = 0x6a7278; // the hatch in the ceiling
        b.move(0, 2.39f, 0);
        b.lathe(new float[] { 0.5f, 0f, 0f, 0.01f }, 10, null);
        b.identity();
        b.brillo = true;
        b.color = 0xfff0d0; // ceiling lamps
        for (int i = 0; i < 3; i++) {
            float a = i * 2.094f + 0.5f;
            b.box(MB.fc(a) * 1.2f, 2.36f, MB.fs(a) * 1.2f, 0.3f, 0.03f, 0.12f);
        }
        b.color = 0x9ad8ff; // porthole
        estacion(b, 252, 1.78f);
        b.brillo = true;
        b.box(0, 1.6f, 0.02f, 0.4f, 0.4f, 0.02f);
        b.brillo = false;
        b.identity();
        return b.build(fog, 255, true, 0);
    }

    /** move the builder to a station on the wall, facing the centre */
    private static void estacion(MB b, float grados, float r) {
        b.identity();
        float a = grados * j2me.FMath.RAD;
        b.move(-MB.fs(a) * r, 0, -MB.fc(a) * r);
        b.rot(1, grados);
    }
}
