package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Mesh;

/** Procedural low-poly models. Every model is original and built only from code. */
public final class Modelos {
    private Modelos() {
    }

    public static final String[] NOMBRES = { "capsula" };

    public static Mesh crear(String nombre, Fog fog) {
        if ("capsula".equals(nombre)) {
            return capsula(fog);
        }
        return null;
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
