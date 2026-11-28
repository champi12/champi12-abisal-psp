package abisal;

import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * Side quest: six data echoes, the emergency recorders the Halcyon crew dropped when the
 * station broke up. Each lies in a different zone, glows faintly and pings on the radar
 * when you are within 120 m. [] next to one plays its recording and gives what its
 * owner left inside. Found echoes are saved.
 */
final class Ecos {
    static final int N = 6;
    /** zone of each echo and its distance from the centre (fixed for every game) */
    private static final int[] ZONA = { Terreno.BAJIOS, Terreno.BOSQUE, Terreno.PRADERA, Terreno.MESETA,
            Terreno.TERMAL, Terreno.CANONES };
    final float[] x = new float[N], y = new float[N], z = new float[N];
    final boolean[] hallado = new boolean[N];
    private final Mesh malla, brillo;
    private final Transform t = new Transform();

    static final String[] TITULO = { "Eco 1: Inés Valdivia, buceadora", "Eco 2: Tomas Ferro, botánico",
            "Eco 3: Lucía Arrieta, cartógrafa", "Eco 4: Bruno Salcedo, ingeniero", "Eco 5: Marta Quiroga, geóloga",
            "Eco 6: Dr. Elías Mora, director" };
    static final String[][] TEXTO = {
            { "Eco 1: Inés Valdivia, buceadora", "Si alguien oye esto: no naden de noche sin luz.",
                    "Los aguijoneros cazan en grupo y no los siguen lejos:", "aléjense de su zona y los dejaran en paz.",
                    "Dejo bengalas en la caja. Úsenlas." },
            { "Eco 2: Tomas Ferro, botánico", "Las vejigas del bosque guardan aire de verdad.",
                    "Las medi: se rellenan en minuto y medio.", "Con ellas se puede bajar más lejos de lo que creiamos.",
                    "Dejo fibra y bulbos para quien los necesite." },
            { "Eco 3: Lucía Arrieta, cartógrafa", "El suelo de las praderas se mueve. No es la corriente:",
                    "algo vive enterrado en la arena abierta.", "Naden por encima de la hierba o pegados a las rocas.",
                    "Les dejo mis baterías de repuesto." },
            { "Eco 4: Bruno Salcedo, ingeniero", "La Rémora aguanta más de lo que parece. Si el casco",
                    "cruje, suban: el soldador lo repara, pero no en marcha.", "El módulo de profundidad necesita cristal térmico",
                    "de las grietas. Aquí tienen circuitos." },
            { "Eco 5: Marta Quiroga, geóloga", "Las grietas estan vivas. Cada fumarola late al mismo",
                    "ritmo que lo que medimos bajo la fosa.", "No es geología. Es un corazón.",
                    "Dejo cristal térmico y un kit." },
            { "Eco 6: Dr. Elías Mora, director", "Fue culpa mía. Perforamos demasiado cerca de la Madre",
                    "y respondió. La estación no se hundió: la arrastró.", "Si escuchas esto, termina lo que empezamos bien:",
                    "llévale los fragmentos al santuario. Y vete." } };
    /** what each echo leaves (item, amount, ...) */
    static final int[][] PREMIO = { { Inventario.BENGALA, 3, Inventario.BOTIQUIN, 1 },
            { Inventario.FIBRA, 4, Inventario.BULBO, 3 }, { Inventario.BATERIA, 2, Inventario.CABLE, 2 },
            { Inventario.CIRCUITO, 2, Inventario.LAMINA, 2 }, { Inventario.CRISTAL_T, 3, Inventario.KIT, 1 },
            { Inventario.OPALO, 2, Inventario.BOYA, 1 } };

    Ecos(Fog fog) {
        Mesh e3 = ModeloM3G.malla("/modelos/eco.m3g", fog, 255, true);
        malla = e3 != null ? e3 : Modelos.ecoDatos(fog);
        brillo = Modelos.luzBaliza(0xc080ff);
        // one point per zone, at a fixed heading from the zone's centre
        float[] pt = new float[2];
        for (int i = 0; i < N; i++) {
            Terreno.buscarPunto(ZONA[i], 40 + i * 67, 0.35f, -5000, pt);
            x[i] = pt[0];
            z[i] = pt[1];
            y[i] = Terreno.altura(pt[0], pt[1]) + 0.2f;
            Log.d("eco de datos " + (i + 1) + " en " + (int) x[i] + "," + (int) y[i] + "," + (int) z[i]);
        }
    }

    int hallados() {
        int n = 0;
        for (int i = 0; i < N; i++) {
            if (hallado[i]) {
                n++;
            }
        }
        return n;
    }

    /** the unfound echo within reach of the diver, or -1 */
    int cerca(float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
            if (!hallado[i]) {
                float dx = x[i] - px, dy = y[i] + 0.5f - py, dz = z[i] - pz;
                if (dx * dx + dy * dy + dz * dz < 4.5f * 4.5f) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** [] on an echo: its recording and its reward; returns the text to show */
    String[] abrir(Partida p, int i) {
        hallado[i] = true;
        int[] r = PREMIO[i];
        for (int k = 0; k < r.length; k += 2) {
            for (int q = 0; q < r[k + 1]; q++) {
                if (!p.inv.meter(r[k])) {
                    break;
                }
            }
        }
        Log.d("eco de datos " + (i + 1) + " encontrado (" + hallados() + "/" + N + ")");
        return TEXTO[i];
    }

    void dibujar(Graphics3D g3d, float px, float pz, float lejos, float tiempo) {
        for (int i = 0; i < N; i++) {
            if (hallado[i]) {
                continue;
            }
            float dx = x[i] - px, dz = z[i] - pz;
            float d2 = dx * dx + dz * dz;
            if (d2 > 140 * 140) {
                continue;
            }
            if (d2 < (lejos + 3) * (lejos + 3)) {
                t.setIdentity();
                t.postTranslate(x[i], y[i] + 0.1f * j2me.FMath.sin(tiempo * 1.3f + i), z[i]);
                t.postRotate(tiempo * 30 + i * 60, 0, 1, 0);
                g3d.render(malla, t);
            }
            // a violet pulse, fog-free: found from afar when you know where to look
            if (((int) (tiempo * 1.2f) + i) % 3 == 0) {
                t.setIdentity();
                t.postTranslate(x[i], y[i] + 0.9f, z[i]);
                float k = 0.5f + j2me.FMath.sqrt(d2) / 90;
                t.postScale(k, k, k);
                g3d.render(brillo, t);
            }
        }
    }

    /** radar: unfound echoes within range ping in violet */
    void radar(Graphics g, Partida p, int cx, int cy, int r, float rango) {
        float sy = j2me.FMath.sin(p.yaw * j2me.FMath.RAD), cyw = j2me.FMath.cos(p.yaw * j2me.FMath.RAD);
        for (int i = 0; i < N; i++) {
            if (hallado[i]) {
                continue;
            }
            float ox = x[i] - p.px, oz = z[i] - p.pz;
            float ad = -(ox * sy + oz * cyw), de = ox * cyw - oz * sy;
            if (ad * ad + de * de > rango * rango) {
                continue;
            }
            int bx = cx + (int) (de / rango * (r - 2)), by = cy - (int) (ad / rango * (r - 2));
            g.setColor(0xc080ff);
            g.fillRect(bx - 1, by - 1, 3, 3);
        }
    }
}
