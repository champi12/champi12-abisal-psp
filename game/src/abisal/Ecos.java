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

    static final String[] TITULO = { "Eco 1: Ines Valdivia, buceadora", "Eco 2: Tomas Ferro, botanico",
            "Eco 3: Lucia Arrieta, cartografa", "Eco 4: Bruno Salcedo, ingeniero", "Eco 5: Marta Quiroga, geologa",
            "Eco 6: Dr. Elias Mora, director" };
    static final String[][] TEXTO = {
            { "Eco 1: Ines Valdivia, buceadora", "Si alguien oye esto: no nadeis de noche sin luz.",
                    "Los aguijoneros cazan en grupo y no os siguen lejos:", "alejaos de su zona y os dejaran en paz.",
                    "Dejo bengalas en la caja. Usadlas." },
            { "Eco 2: Tomas Ferro, botanico", "Las vejigas del bosque guardan aire de verdad.",
                    "Las medi: se rellenan en minuto y medio.", "Con ellas se puede bajar mas lejos de lo que creiamos.",
                    "Dejo fibra y bulbos para quien los necesite." },
            { "Eco 3: Lucia Arrieta, cartografa", "El suelo de las praderas se mueve. No es la corriente:",
                    "algo vive enterrado en la arena abierta.", "Nadad por encima de la hierba o pegados a las rocas.",
                    "Os dejo mis baterias de repuesto." },
            { "Eco 4: Bruno Salcedo, ingeniero", "La Remora aguanta mas de lo que parece. Si el casco",
                    "cruje, subid: el soldador lo repara, pero no en marcha.", "El modulo de profundidad necesita cristal termico",
                    "de las grietas. Aqui teneis circuitos." },
            { "Eco 5: Marta Quiroga, geologa", "Las grietas estan vivas. Cada fumarola late al mismo",
                    "ritmo que lo que medimos bajo la fosa.", "No es geologia. Es un corazon.",
                    "Dejo cristal termico y un kit." },
            { "Eco 6: Dr. Elias Mora, director", "Fue culpa mia. Perforamos demasiado cerca de la Madre",
                    "y respondio. La estacion no se hundio: la arrastro.", "Si escuchas esto, termina lo que empezamos bien:",
                    "llevale los fragmentos al santuario. Y vete." } };
    /** what each echo leaves (item, amount, ...) */
    static final int[][] PREMIO = { { Inventario.BENGALA, 3, Inventario.BOTIQUIN, 1 },
            { Inventario.FIBRA, 4, Inventario.BULBO, 3 }, { Inventario.BATERIA, 2, Inventario.CABLE, 2 },
            { Inventario.CIRCUITO, 2, Inventario.LAMINA, 2 }, { Inventario.CRISTAL_T, 3, Inventario.KIT, 1 },
            { Inventario.OPALO, 2, Inventario.BOYA, 1 } };

    Ecos(Fog fog) {
        malla = Modelos.ecoDatos(fog);
        brillo = Modelos.luzBaliza(0xc080ff);
        // one point per zone, at a fixed angle and depth into the zone's ring
        for (int i = 0; i < N; i++) {
            int zona = ZONA[i];
            int anillo = zona == Terreno.PRADERA ? Terreno.BOSQUE : (zona == Terreno.TERMAL ? Terreno.CANONES : zona);
            float r0 = anillo == 0 ? 60 : Terreno.LIMITE[anillo - 1], r1 = Terreno.LIMITE[anillo];
            float ang = (40 + i * 67) * j2me.FMath.RAD;
            for (int intento = 0; intento < 60; intento++) {
                float r = r0 + (r1 - r0) * (0.35f + (intento % 5) * 0.08f);
                float a = ang + intento * 0.23f;
                float px = j2me.FMath.cos(a) * r, pz = j2me.FMath.sin(a) * r;
                if (Terreno.bioma(px, pz) == zona || intento == 59) {
                    x[i] = px;
                    z[i] = pz;
                    y[i] = Terreno.altura(px, pz) + 0.2f;
                    Log.d("eco de datos " + (i + 1) + " en " + (int) px + "," + (int) y[i] + "," + (int) pz);
                    break;
                }
            }
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
