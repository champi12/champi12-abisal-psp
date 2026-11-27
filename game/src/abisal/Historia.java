package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * The story missions. The crew logs point the way; the capsule radio (once repaired)
 * marks each place on the radar and the HUD. After a mission the signal points back to
 * the capsule until the radio is heard.
 *
 * Act I (the beacon)
 *   0 build the knife and the flashlight
 *   1 repair the capsule radio (repair kit)
 *   2 Container 7 on the red plateau: the spare beacon module
 *   3 research module "Tetis" in the thermal rifts: the power core
 *   4 a resonant crystal in the abyss (the Remora needs the depth module)
 *   5 build the rescue beacon and launch it from the surface
 * Act II (the Heartbeat)
 *   6 the rescue ship cannot land while the Heartbeat shakes the planet: place the
 *     first seismic sensor
 *   7 place the second sensor
 *   8-10 the source is a sealed Sanctuary: find the three glyph fragments the first
 *     inhabitants left in the meadows, the plateau and the thermal rifts
 *  11 open the Sanctuary at the edge of the void: the resonance key
 *  12 take the key to the Mother of the Heartbeat, under the abyss
 *  13 the true ending (free play goes on)
 */
public final class Historia {
    static final int LUGARES = 10;
    static final int SENSOR_A = 3, SENSOR_B = 4, SANTUARIO = 5, MADRE = 6, FRAG = 7;
    static final String[] NOMBRE_LUGAR = { "Contenedor 7", "Módulo Tetis", "Cristal resonante", "Punto sísmico A",
            "Punto sísmico B", "Santuario", "Madre del Latido", "Piedra de glifo (praderas)",
            "Piedra de glifo (meseta)", "Piedra de glifo (grietas)" };
    static final String[] MISION = { "Fabrica el cuchillo y la linterna",
            "Repara la radio del módulo (kit de reparación)", "Busca el Contenedor 7 en la meseta roja",
            "Recupera el núcleo del módulo Tetis (grietas termales)",
            "Extrae un cristal resonante en la fosa (módulo de profundidad)",
            "Fabrica la baliza y lánzala desde la superficie",
            "Coloca un sensor sísmico en el punto A (cañones)", "Coloca el segundo sensor en el punto B (fosa)",
            "Primer fragmento de glifo: en las praderas de arena", "Segundo fragmento de glifo: en la meseta roja",
            "Tercer fragmento de glifo: en las grietas termales",
            "Abre el Santuario, al borde del vacío (3 fragmentos)",
            "Lleva la llave de resonancia a la Madre del Latido", "Talassa duerme tranquila. Sigue explorando" };

    /** radio messages: index = the stage they announce */
    static final String[][] RADIO = {
            {},
            { "NÚCLEO - sin señal", "La radio esta muda: el maremoto fundio el módulo.",
                    "Fabrica un kit de reparación y usalo aquí." },
            { "NÚCLEO - Sistema de la Halcyon", "Emergencia. Baliza principal destruida.",
                    "El repuesto viajaba en el Contenedor 7: su transpondedor",
                    "emite desde la meseta roja. Señal marcada en el radar." },
            { "NÚCLEO - Sistema de la Halcyon", "Módulo de baliza recibido. Falta energía.",
                    "El módulo de investigacion Tetis cayo en las grietas",
                    "termales con su núcleo intacto. Cuidado con el calor." },
            { "NÚCLEO - Sistema de la Halcyon", "Núcleo aceptado. Para sintonizar la baliza hace",
                    "falta un cristal resonante de la fosa abisal (600 m).",
                    "Mejora la Rémora: módulo de profundidad (cristal térmico)." },
            { "NÚCLEO - Sistema de la Halcyon", "Componentes completos. Ensambla la baliza en el",
                    "sintetizador y lánzala desde la superficie (PDA, X).", "Rescate estimado: 4 días." },
            { "NÚCLEO - Nave de rescate Albatros", "Tenemos tu baliza, pero no podemos posarnos: cada",
                    "41 minutos un pulso sísmico sacude toda la superficie.",
                    "Fabrica dos sensores sísmicos (pestana MISIÓN) y",
                    "colocalos en los puntos marcados para hallar la fuente." },
            { "NÚCLEO - Nave de rescate Albatros", "Primer sensor en linea. Falta el segundo, en la fosa." },
            { "NÚCLEO - Nave de rescate Albatros", "Triangulado. El pulso nace bajo una estructura que",
                    "no aparece en ningún mapa, al borde del vacío. Esta sellada.",
                    "Los sensores captan tres ecos identicos dispersos por el",
                    "cráter: piedras con glifos. Puede que sean la llave." },
            { "NÚCLEO - Nave de rescate Albatros", "Un fragmento. Faltan dos." },
            { "NÚCLEO - Nave de rescate Albatros", "Dos fragmentos. Queda el de las grietas termales." },
            { "NÚCLEO - Nave de rescate Albatros", "Los tres fragmentos vibran a la vez. Esa estructura",
                    "acaba de responder: sus glifos se han encendido.",
                    "Llevalos al Santuario. Y ten cuidado: algo enorme",
                    "patrulla el borde del vacío." },
            { "NÚCLEO - Nave de rescate Albatros", "Hemos leido la llave. Los antiguos no hicieron un",
                    "templo: hicieron una cuna. El Latido es el corazón de",
                    "una criatura que duerme bajo la fosa: la Madre.",
                    "Los colosos son sus crias. La llave puede calmarla." },
            { "NÚCLEO - Nave de rescate Albatros", "El pulso se ha detenido. Bajamos al amanecer.",
                    "Gracias, superviviente. Talassa respira." } };

    static final String[] FINAL = { "LA BALIZA SUBE HACIA EL CIELO DE TALASSA",
            "Una columna de luz atraviesa las nubes. La radio responde:", "la nave Albatros viene en camino...",
            "pero bajo tus pies el Latido cambia de ritmo,", "como si algo, muy abajo, también hubiera escuchado.", "",
            "FIN DEL PRIMER ACTO - escucha la radio de la cápsula", "(X para continuar)" };

    static final String[] INSCRIPCION = { "EL SANTUARIO",
            "Los glifos se encienden al tocar el altar. Una voz sin",
            "palabras llena el agua: imagenes de criaturas enormes que",
            "duermen, de pueblos que cantaban para calmarlas.",
            "Del núcleo se desprende una llave que vibra en tus manos.", "", "+1 Llave de resonancia",
            "(X para continuar)" };

    static final String[] FINAL2 = { "LA MADRE DEL LATIDO",
            "La llave canta. Los tentaculos de la Madre se detienen,",
            "su luz se vuelve lenta y tranquila... y el pulso cesa.",
            "Por primera vez desde el maremoto, el mar esta en calma.", "",
            "Al amanecer, la Albatros se posa sobre las olas.",
            "FIN - gracias por jugar ABISAL", "(X para seguir explorando)" };

    int etapa;
    boolean radioReparada;
    /** last radio message heard (so the HUD can say "new message") */
    int radioOida;
    /** the text panel to show in the FINAL state */
    String[] pantalla = FINAL;

    final float[] lx = new float[LUGARES], ly = new float[LUGARES], lz = new float[LUGARES];
    private Mesh glifo;
    /** each place gives its item (or takes its sensor) once */
    final boolean[] tomado = new boolean[LUGARES];
    private final Mesh[] mallas = new Mesh[LUGARES];
    private final Mesh[] madreP;
    private final Transform t = new Transform(), tp = new Transform();
    /** radius and height of each place for collisions, and how close to use it */
    static final float[] RADIO_LUGAR = { 3.0f, 3.2f, 1.8f, 0.6f, 0.6f, 2.6f, 30f, 1.2f, 1.2f, 1.2f };
    static final float[] ALTO_LUGAR = { 3.4f, 3.4f, 4f, 2f, 2f, 2f, 26f, 3f, 3f, 3f };
    static final float[] ALCANCE = { 6.5f, 6.5f, 5f, 5f, 5f, 6f, 45f, 5f, 5f, 5f };

    Historia(Fog fog) {
        // fixed, deterministic places of the crater (x = cos a * r, z = sin a * r)
        colocar(0, 140, 730, Terreno.MESETA, -200);
        colocar(1, 78, 990, Terreno.TERMAL, -290);
        colocar(2, 300, 1380, Terreno.ABISMO, -2000);
        colocar(SENSOR_A, 200, 1050, Terreno.CANONES, -300);
        colocar(SENSOR_B, 330, 1310, Terreno.ABISMO, -890);
        colocar(SANTUARIO, 250, 1470, Terreno.ABISMO, -890);
        colocar(MADRE, 160, 1400, Terreno.ABISMO, -890);
        colocar(FRAG, 240, 420, Terreno.PRADERA, -200);
        colocar(FRAG + 1, 20, 760, Terreno.MESETA, -250);
        colocar(FRAG + 2, 100, 1100, Terreno.TERMAL, -295);
        glifo = Modelos.piedraGlifo(fog);
        mallas[0] = Modelos.contenedor(fog);
        mallas[1] = Modelos.moduloTetis(fog);
        mallas[2] = Modelos.cristalResonante(fog);
        mallas[SENSOR_A] = mallas[SENSOR_B] = Modelos.sensor(fog);
        mallas[SANTUARIO] = Modelos.santuario(fog);
        madreP = Modelos.madre(fog);
    }

    /** search outwards from (angle, radius) for a spot in the biome not deeper than minY */
    private void colocar(int i, float grados, float r, int bioma, float minY) {
        for (int k = 0; k < 60; k++) {
            float a = (grados + (k % 5) * 3) * j2me.FMath.RAD;
            float rr = r - (k / 5) * 12;
            float x = j2me.FMath.cos(a) * rr, z = j2me.FMath.sin(a) * rr;
            float h = Terreno.altura(x, z);
            if (Terreno.bioma(x, z) == bioma && h > minY || k == 59) {
                lx[i] = x;
                lz[i] = z;
                ly[i] = h - 0.3f;
                Log.d("lugar " + NOMBRE_LUGAR[i] + " en " + (int) x + "," + (int) h + "," + (int) z);
                return;
            }
        }
    }

    /** stages whose place is only known after hearing the radio */
    private static boolean necesitaRadio(int e) {
        return e >= 2 && e <= 12 && e != 7 && e != 9 && e != 10;
    }

    /** place the current stage points to (-1 none, LUGARES = the capsule) */
    int objetivo() {
        if (necesitaRadio(etapa) && radioOida < etapa) {
            return LUGARES;
        }
        switch (etapa) {
        case 1:
            return LUGARES;
        case 2:
            return 0;
        case 3:
            return 1;
        case 4:
            return 2;
        case 6:
            return SENSOR_A;
        case 7:
            return SENSOR_B;
        case 8:
        case 9:
        case 10:
            return FRAG + etapa - 8;
        case 11:
            return SANTUARIO;
        case 12:
            return MADRE;
        default:
            return -1;
        }
    }

    float objX() {
        int o = objetivo();
        return o == LUGARES ? 0 : lx[o];
    }

    float objY() {
        int o = objetivo();
        return o == LUGARES ? 0 : ly[o] + (o == MADRE ? 25 : 2);
    }

    float objZ() {
        int o = objetivo();
        return o == LUGARES ? 0 : lz[o];
    }

    /** a radio message the player has not heard yet */
    boolean radioNueva() {
        return radioReparada && radioOida < etapa && etapa < RADIO.length && RADIO[etapa].length > 0;
    }

    void actualizar(Partida p) {
        if (etapa == 0 && p.inv.tiene[Inventario.H_CUCHILLO] && p.inv.tiene[Inventario.H_LINTERNA]) {
            avanzar(p, 1);
        }
    }

    /** the mission line for the HUD and the PDA */
    String mision() {
        if (necesitaRadio(etapa) && radioOida < etapa) {
            return "Vuelve a la cápsula y escucha la radio";
        }
        return MISION[Math.min(etapa, MISION.length - 1)];
    }

    void avanzar(Partida p, int nueva) {
        etapa = nueva;
        if (!necesitaRadio(etapa)) {
            radioOida = Math.max(radioOida, etapa);
        }
        p.avisar(necesitaRadio(etapa) && radioOida < etapa ? "Misión cumplida. Vuelve a la cápsula: radio"
                : "Nueva misión: " + MISION[Math.min(etapa, MISION.length - 1)]);
        Sonido.tocar(Sonido.SONAR);
        Log.d("historia: etapa " + etapa + " - " + MISION[Math.min(etapa, MISION.length - 1)]);
    }

    /** the place within reach of the diver, or -1 */
    int cercano(float px, float py, float pz) {
        for (int i = 0; i < LUGARES; i++) {
            float dx = px - lx[i], dy = py - ly[i], dz = pz - lz[i];
            float r = ALCANCE[i];
            if (dx * dx + dz * dz < r * r && dy > -2 && dy < ALTO_LUGAR[i] + 6) {
                return i;
            }
        }
        return -1;
    }

    /** prompt for the place in reach, or null */
    String accion(float px, float py, float pz) {
        int i = cercano(px, py, pz);
        if (i < 0) {
            return null;
        }
        switch (i) {
        case SENSOR_A:
        case SENSOR_B:
            if (tomado[i]) {
                return "Sensor sísmico funcionando";
            }
            return etapa == (i == SENSOR_A ? 6 : 7) ? "[] Colocar el sensor sísmico" : null;
        case SANTUARIO:
            return etapa == 11 && !tomado[i] ? "[] Colocar los fragmentos en el altar" : "Santuario de los antiguos";
        case MADRE:
            return etapa == 12 ? "[] Usar la llave de resonancia" : "Madre del Latido";
        case FRAG:
        case FRAG + 1:
        case FRAG + 2:
            if (tomado[i]) {
                return "Piedra de glifo (vacia)";
            }
            return etapa == 8 + i - FRAG ? "[] Desprender el fragmento de glifo" : "Piedra de glifo";
        default:
            if (tomado[i]) {
                return NOMBRE_LUGAR[i] + " (ya revisado)";
            }
            return i == 0 ? "[] Abrir el Contenedor 7" : (i == 1 ? "[] Buscar en el módulo Tetis" : "[] Extraer el cristal");
        }
    }

    /** square near a place. true if something happened */
    boolean interactuar(Partida p) {
        int i = cercano(p.px, p.py, p.pz);
        if (i < 0) {
            return false;
        }
        if (i == SENSOR_A || i == SENSOR_B) {
            int quiere = i == SENSOR_A ? 6 : 7;
            if (tomado[i] || etapa != quiere) {
                return false;
            }
            if (p.inv.cant[Inventario.SENSOR] <= 0) {
                p.avisar("Necesitas un sensor sísmico (sintetizador, pestana MISIÓN)");
                return true;
            }
            p.inv.cant[Inventario.SENSOR]--;
            tomado[i] = true;
            Sonido.tocar(Sonido.FABRICAR);
            Log.d("historia: sensor colocado en " + NOMBRE_LUGAR[i]);
            avanzar(p, quiere + 1);
            return true;
        }
        if (i >= FRAG) {
            if (tomado[i] || etapa != 8 + i - FRAG) {
                return false;
            }
            tomado[i] = true;
            p.inv.cant[Inventario.FRAGMENTO]++;
            Sonido.tocar(Sonido.LATIDO);
            p.avisar("+1 Fragmento de glifo (" + p.inv.cant[Inventario.FRAGMENTO] + "/3)");
            avanzar(p, etapa + 1);
            return true;
        }
        if (i == SANTUARIO) {
            if (etapa != 11 || tomado[i]) {
                return false;
            }
            if (p.inv.cant[Inventario.FRAGMENTO] < 3) {
                p.avisar("El altar tiene tres huecos: faltan fragmentos de glifo");
                return true;
            }
            p.inv.cant[Inventario.FRAGMENTO] -= 3;
            tomado[i] = true;
            p.inv.cant[Inventario.LLAVE]++;
            Sonido.tocar(Sonido.LATIDO);
            pantalla = INSCRIPCION;
            avanzar(p, 12);
            p.estado = Partida.FINAL;
            return true;
        }
        if (i == MADRE) {
            if (etapa != 12) {
                return false;
            }
            if (p.inv.cant[Inventario.LLAVE] <= 0) {
                p.avisar("Necesitas la llave de resonancia del Santuario");
                return true;
            }
            p.inv.cant[Inventario.LLAVE]--;
            pantalla = FINAL2;
            avanzar(p, 13);
            p.estado = Partida.FINAL;
            Log.d("historia: FINAL VERDADERO");
            return true;
        }
        if (tomado[i]) {
            p.avisar(NOMBRE_LUGAR[i] + ": no queda nada útil");
            return true;
        }
        if (i == 2 && !p.inv.tiene[Inventario.H_CUCHILLO]) {
            p.avisar("Necesitas el cuchillo para desprender el cristal");
            return true;
        }
        int item = i == 0 ? Inventario.MODULO_BALIZA : (i == 1 ? Inventario.NUCLEO : Inventario.CRISTAL_R);
        p.inv.cant[item]++; // story items always fit
        tomado[i] = true;
        p.avisar("+1 " + Inventario.NOMBRE[item]);
        Sonido.tocar(Sonido.FABRICAR);
        Log.d("historia: " + Inventario.NOMBRE[item] + " recogido");
        if (etapa == i + 2) {
            avanzar(p, etapa + 1);
        }
        return true;
    }

    /** true when the Heartbeat can be felt (near the Mother, before the true ending) */
    boolean latidoCerca(float px, float pz) {
        float dx = px - lx[MADRE], dz = pz - lz[MADRE];
        return etapa < 13 && dx * dx + dz * dz < 380 * 380;
    }

    /** 0 when far from the Heartbeat, 1 right above the Mother: scales its sound and shake */
    float cercaniaLatido(float px, float pz) {
        float dx = px - lx[MADRE], dz = pz - lz[MADRE];
        float d = j2me.FMath.sqrt(dx * dx + dz * dz);
        return Math.max(0, Math.min(1, 1 - d / 600));
    }

    /** the mission places are solid */
    void empujar(float[] q, float rj) {
        for (int i = 0; i < LUGARES; i++) {
            if ((i == SENSOR_A || i == SENSOR_B) && !tomado[i]) {
                continue;
            }
            if (i == MADRE) {
                // her dome: an ellipsoid 35 m wide and 33 m tall from just below the floor
                float ex = (q[0] - lx[i]) / (35 + rj), ey = (q[1] - (ly[i] - 4)) / (33 + rj),
                        ez = (q[2] - lz[i]) / (35 + rj);
                float d2 = ex * ex + ey * ey + ez * ez;
                if (d2 < 1 && d2 > 1e-6f) {
                    float k = 1 / j2me.FMath.sqrt(d2);
                    q[0] = lx[i] + ex * k * (35 + rj);
                    q[1] = ly[i] - 4 + ey * k * (33 + rj);
                    q[2] = lz[i] + ez * k * (35 + rj);
                }
                continue;
            }
            float r = RADIO_LUGAR[i] + rj;
            float dx = q[0] - lx[i], dz = q[2] - lz[i];
            float top = ly[i] + ALTO_LUGAR[i] + rj * 0.5f;
            if (q[1] > top) {
                continue;
            }
            float d2 = dx * dx + dz * dz;
            if (d2 < r * r && d2 > 1e-4f) {
                float d = j2me.FMath.sqrt(d2);
                if (top - q[1] < r - d) {
                    q[1] = top;
                } else {
                    q[0] = lx[i] + dx / d * r;
                    q[2] = lz[i] + dz / d * r;
                }
            }
        }
    }

    void dibujar(Graphics3D g3d, float px, float pz, float lejos, float tiempo) {
        for (int i = 0; i < LUGARES; i++) {
            float dx = lx[i] - px, dz = lz[i] - pz;
            float margen = i == MADRE ? 80 : (i == SANTUARIO ? 20 : 8);
            if (dx * dx + dz * dz > (lejos + margen) * (lejos + margen)) {
                continue;
            }
            if ((i == SENSOR_A || i == SENSOR_B) && !tomado[i]) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(lx[i], ly[i], lz[i]);
            t.postRotate(i * 70 + 20, 0, 1, 0);
            if (i == MADRE) {
                dibujarMadre(g3d, tiempo);
                continue;
            }
            if (i >= FRAG) {
                // the glyph stone: the fragment floats above it until taken
                g3d.render(glifo, t);
                continue;
            }
            if (i == 2) {
                // the resonant crystal pulses with the Heartbeat
                float k = 1 + 0.05f * j2me.FMath.sin(tiempo * 3);
                t.postScale(k, k, k);
            }
            g3d.render(mallas[i], t);
        }
    }

    /** the Mother: her body swells with each heartbeat, her tendrils sway slowly */
    private void dibujarMadre(Graphics3D g3d, float tiempo) {
        float latido = etapa < 13 ? pulso(tiempo) : 0;
        float k = 1 + latido * 0.03f;
        tp.set(t);
        tp.postScale(k, 1 + latido * 0.05f, k);
        g3d.render(madreP[6], tp);
        float calma = etapa < 13 ? 1 : 0.3f;
        for (int i = 0; i < 6; i++) {
            float a = i * 1.047f + 0.3f;
            float bx = j2me.FMath.cos(a) * 20, bz = j2me.FMath.sin(a) * 20;
            tp.set(t);
            tp.postTranslate(bx, 16, bz);
            tp.postRotate(j2me.FMath.sin(tiempo * 0.4f + i) * 9 * calma, j2me.FMath.sin(a), 0, -j2me.FMath.cos(a));
            tp.postTranslate(-bx, -16, -bz);
            g3d.render(madreP[i], tp);
        }
    }

    /** heartbeat curve: a double thump every 3.2 s (0..1) */
    static float pulso(float tiempo) {
        float f = (tiempo % 3.2f);
        float a = f < 0.25f ? f / 0.25f : (f < 0.5f ? (0.5f - f) / 0.25f : 0);
        float g = f > 0.55f && f < 0.95f ? (f < 0.75f ? (f - 0.55f) / 0.2f : (0.95f - f) / 0.2f) * 0.6f : 0;
        return Math.max(a, g);
    }
}
