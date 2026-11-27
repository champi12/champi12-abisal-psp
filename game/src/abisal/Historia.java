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
                    "falta un cristal resonante de la fosa abisal (400 m).",
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
    private final ModeloM3G mContenedor, mTetis, mCristal, mSensor, mGlifo, mSantuario, mMadre;
    private final Transform tq = new Transform();
    private final boolean[] vistoLog = new boolean[LUGARES];
    private final Transform t = new Transform(), tp = new Transform();
    /** radius and height of each place for collisions, and how close to use it */
    static final float[] RADIO_LUGAR = { 3.0f, 3.2f, 1.8f, 0.6f, 0.6f, 2.6f, 30f, 1.2f, 1.2f, 1.2f };
    static final float[] ALTO_LUGAR = { 3.4f, 3.4f, 4f, 2f, 2f, 2f, 26f, 3f, 3f, 3f };
    static final float[] ALCANCE = { 6.5f, 6.5f, 5f, 5f, 5f, 6f, 45f, 5f, 5f, 5f };

    Historia(Fog fog) {
        // fixed, deterministic places of the crater (x = cos a * r, z = sin a * r)
        // each place sits at a fraction of its zone's ring (0 = inner edge, 1 = outer edge), so
        // they stay in the right zone however big the crater is
        colocar(0, 140, 0.35f, Terreno.MESETA, -400);
        colocar(1, 78, 0.35f, Terreno.TERMAL, -600);
        colocar(2, 300, 0.6f, Terreno.ABISMO, -2000);
        colocar(SENSOR_A, 200, 0.5f, Terreno.CANONES, -600);
        colocar(SENSOR_B, 330, 0.45f, Terreno.ABISMO, -2000);
        colocar(SANTUARIO, 250, 0.92f, Terreno.ABISMO, -2000);
        colocar(MADRE, 160, 0.55f, Terreno.ABISMO, -2000);
        colocar(FRAG, 240, 0.5f, Terreno.PRADERA, -400);
        colocar(FRAG + 1, 20, 0.5f, Terreno.MESETA, -400);
        colocar(FRAG + 2, 100, 0.6f, Terreno.TERMAL, -600);
        // the story models are made in Blender (assets/modelos); the old code-built ones
        // are only made if a file is missing
        mContenedor = ModeloM3G.cargar("/modelos/contenedor.m3g", fog, 3);
        mTetis = ModeloM3G.cargar("/modelos/tetis.m3g", fog, 4);
        mCristal = ModeloM3G.cargar("/modelos/cristal.m3g", fog, 3);
        mSensor = ModeloM3G.cargar("/modelos/sensor.m3g", fog, 3);
        mGlifo = ModeloM3G.cargar("/modelos/glifo.m3g", fog, 2);
        mSantuario = ModeloM3G.cargar("/modelos/santuario.m3g", fog, 3);
        mMadre = ModeloM3G.cargar("/modelos/madre.m3g", fog, 3);
        glifo = mGlifo == null ? Modelos.piedraGlifo(fog) : null;
        mallas[0] = mContenedor == null ? Modelos.contenedor(fog) : null;
        mallas[1] = mTetis == null ? Modelos.moduloTetis(fog) : null;
        mallas[2] = mCristal == null ? Modelos.cristalResonante(fog) : null;
        mallas[SENSOR_A] = mallas[SENSOR_B] = mSensor == null ? Modelos.sensor(fog) : null;
        mallas[SANTUARIO] = mSantuario == null ? Modelos.santuario(fog) : null;
        madreP = mMadre == null ? Modelos.madre(fog) : null;
    }

    /** the ring (index into Terreno.LIMITE) a zone lives in */
    private static int anillo(int bioma) {
        switch (bioma) {
        case Terreno.PRADERA:
            return Terreno.BOSQUE;
        case Terreno.TERMAL:
            return Terreno.CANONES;
        case Terreno.LLANURA:
            return 5;
        default:
            return bioma;
        }
    }

    /**
     * Put place i in its zone: start at the given angle and fraction of the zone's ring, then
     * search nearby (wider and wider) for a spot that really is that zone and not deeper than
     * minY. (It used to search around fixed radii of the old, smaller crater: after the crater
     * grew, no spot matched and the places fell back into the kelp forest.)
     */
    private void colocar(int i, float grados, float rel, int bioma, float minY) {
        int an = anillo(bioma);
        float r0 = an == 0 ? 0 : Terreno.LIMITE[an - 1], r1 = Terreno.LIMITE[an];
        float rObj = r0 + (r1 - r0) * rel;
        float fbx = 0, fbz = 0;
        boolean hayFb = false;
        for (int j = 0; j < 24; j++) {
            // radius: 0, +20, -20, +40, -40 ... metres
            float rr = rObj + ((j & 1) == 0 ? 1 : -1) * ((j + 1) / 2) * 20;
            if (rr < r0 + 10 || rr > r1 - 10) {
                continue;
            }
            for (int k = 0; k < 15; k++) {
                float a = (grados + ((k & 1) == 0 ? 1 : -1) * ((k + 1) / 2) * 3) * j2me.FMath.RAD;
                float x = j2me.FMath.cos(a) * rr, z = j2me.FMath.sin(a) * rr;
                if (Terreno.bioma(x, z) != bioma) {
                    continue;
                }
                float h = Terreno.altura(x, z);
                if (!hayFb) {
                    hayFb = true;
                    fbx = x;
                    fbz = z;
                }
                if (h > minY) {
                    poner(i, x, z);
                    return;
                }
            }
        }
        if (!hayFb) {
            float a = grados * j2me.FMath.RAD;
            fbx = j2me.FMath.cos(a) * rObj;
            fbz = j2me.FMath.sin(a) * rObj;
        }
        poner(i, fbx, fbz);
    }

    private void poner(int i, float x, float z) {
        float h = Terreno.altura(x, z);
        lx[i] = x;
        lz[i] = z;
        ly[i] = h - 0.3f;
        Log.d("lugar " + NOMBRE_LUGAR[i] + " en " + (int) x + "," + (int) h + "," + (int) z + " ("
                + Mundo.nombreBioma(Terreno.bioma(x, z)) + ")");
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
            if (!vistoLog[i]) {
                vistoLog[i] = true;
                Log.d("historia: dibujando " + NOMBRE_LUGAR[i] + " a " + (int) j2me.FMath.sqrt(dx * dx + dz * dz) + " m");
            }
            if (dibujarM3G(g3d, i, tiempo)) {
                continue;
            }
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

    /** one piece of a Blender model at the place transform t (no extra motion) */
    private void pieza(Graphics3D g3d, ModeloM3G m, int k) {
        tq.set(t);
        tq.postMultiply(m.base[k]);
        g3d.render(m.pieza[k], tq);
    }

    /** a piece turned by a degrees about its own vertical axis (Blender Z), raised by sube */
    private void piezaGiro(Graphics3D g3d, ModeloM3G m, int k, float a, float sube) {
        tq.set(t);
        tq.postMultiply(m.base[k]);
        if (sube != 0) {
            tq.postTranslate(0, 0, sube);
        }
        tq.postRotate(a, 0, 0, 1);
        g3d.render(m.pieza[k], tq);
    }

    private void piezaEscala(Graphics3D g3d, ModeloM3G m, int k, float e) {
        tq.set(t);
        tq.postMultiply(m.base[k]);
        tq.postScale(e, e, e);
        g3d.render(m.pieza[k], tq);
    }

    /** the Blender versions of the story places, with their animations; false if missing */
    private boolean dibujarM3G(Graphics3D g3d, int i, float tiempo) {
        float latido = etapa < 13 ? pulso(tiempo) : 0;
        if (i == 0 && mContenedor != null) {
            t.postRotate(6, 1, 0, 0); // lying a little on its side
            pieza(g3d, mContenedor, 0);
            // the open door swings slowly in the current, the transponder blinks until found
            piezaGiro(g3d, mContenedor, 1, j2me.FMath.sin(tiempo * 0.6f) * 7 + j2me.FMath.sin(tiempo * 1.7f) * 2, 0);
            if (!tomado[0] && (tiempo % 1.6f) < 0.5f) {
                pieza(g3d, mContenedor, 2);
            }
            return true;
        }
        if (i == 1 && mTetis != null) {
            t.postRotate(8, 1, 0, 0);
            pieza(g3d, mTetis, 0);
            // the broken radar dish turns in fits and starts
            float giro = tiempo * 18 + j2me.FMath.sin(tiempo * 1.3f) * 30;
            piezaGiro(g3d, mTetis, 1, giro, 0);
            // the panels flicker now and then (failing power)
            int f = (int) (tiempo * 12);
            if (f % 23 != 0 && f % 37 != 1) {
                pieza(g3d, mTetis, 2);
            }
            if ((tiempo % 1.4f) < 0.2f) {
                pieza(g3d, mTetis, 3);
            }
            return true;
        }
        if (i == 2 && mCristal != null) {
            pieza(g3d, mCristal, 0);
            // the crystals swell with every heartbeat; the shards circle and bob
            piezaEscala(g3d, mCristal, 1, 1 + 0.06f * latido + 0.015f * j2me.FMath.sin(tiempo * 2));
            if (!tomado[2]) {
                piezaGiro(g3d, mCristal, 2, tiempo * 22, j2me.FMath.sin(tiempo * 1.2f) * 0.25f);
            }
            return true;
        }
        if ((i == SENSOR_A || i == SENSOR_B) && mSensor != null) {
            pieza(g3d, mSensor, 0);
            piezaGiro(g3d, mSensor, 1, tiempo * 45, 0);
            if (((tiempo + i) % 2f) < 0.3f) {
                pieza(g3d, mSensor, 2);
            }
            return true;
        }
        if (i >= FRAG && mGlifo != null) {
            pieza(g3d, mGlifo, 0);
            if (!tomado[i]) {
                piezaGiro(g3d, mGlifo, 1, tiempo * 50, j2me.FMath.sin(tiempo * 1.5f) * 0.18f);
            }
            return true;
        }
        if (i == SANTUARIO && mSantuario != null) {
            pieza(g3d, mSantuario, 0);
            // the core beats with the Heartbeat; its glyph rings turn around it
            piezaEscala(g3d, mSantuario, 1, 1 + 0.12f * latido + 0.04f * j2me.FMath.sin(tiempo * 1.1f));
            piezaGiro(g3d, mSantuario, 2, tiempo * 14, j2me.FMath.sin(tiempo * 0.7f) * 0.3f);
            return true;
        }
        if (i == MADRE && mMadre != null) {
            float k = 1 + latido * 0.03f;
            tq.set(t);
            tq.postScale(k, 1 + latido * 0.05f, k);
            tq.postMultiply(mMadre.base[0]);
            g3d.render(mMadre.pieza[0], tq);
            // six tendrils rise from the dome and sway; calmer once she sleeps
            float calma = etapa < 13 ? 1 : 0.3f;
            for (int c = 0; c < 6; c++) {
                float a = c * 1.047f + 0.3f;
                tq.set(t);
                tq.postTranslate(j2me.FMath.cos(a) * 21, 15, j2me.FMath.sin(a) * 21);
                tq.postRotate(-a * 57.29578f, 0, 1, 0);
                tq.postRotate((j2me.FMath.sin(tiempo * 0.4f + c) * 9 - 4) * calma, 0, 0, 1);
                tq.postRotate(j2me.FMath.sin(tiempo * 0.3f + c * 2) * 5 * calma, 1, 0, 0);
                tq.postMultiply(mMadre.base[1]);
                g3d.render(mMadre.pieza[1], tq);
            }
            piezaEscala(g3d, mMadre, 2, 1 + latido * 0.12f);
            return true;
        }
        return false;
    }

    /** (old model) the Mother: her body swells with each heartbeat, her tendrils sway slowly */
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
