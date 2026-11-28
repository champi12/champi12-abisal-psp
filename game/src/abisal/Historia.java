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
    static final int LUGARES = 14;
    static final int SENSOR_A = 3, SENSOR_B = 4, SANTUARIO = 5, MADRE = 6, FRAG = 7, ORGANO = 10, REACTOR = 11,
            PETREL = 12, PECIO = 13;
    static final String[] NOMBRE_LUGAR = { "Contenedor 7", "Módulo Tetis", "Cristal resonante", "Punto sísmico A",
            "Punto sísmico B", "Santuario", "Madre del Latido", "Piedra de glifo (cañones)",
            "Piedra de glifo (grietas)", "Piedra de glifo (arrecife)", "Órgano de los colosos",
            "Reactor de la Halcyon", "Boya de la Petrel", "Pecio Albatros" };
    /**
     * Four acts, each stage in a different zone of the crater:
     * I  Sobrevivir: shallows, kelp forest, sand meadows, glass reef, the surface;
     * II El Latido: red plateau, outer shelf, the Albatros, canyons, thermal rifts, glass reef;
     * III La cuna: the ossuary, the Batel, the Sanctuary and the Mother in the trench;
     * IV El corazón de Talassa: the Halcyon's reactor in the rifts and the choice at the Petrel.
     */
    static final String[] MISION = { "Fabrica el cuchillo y la linterna",
            "Repara la radio del módulo (kit de reparación)", "Busca el Contenedor 7 en el bosque de algas",
            "Recupera el núcleo del módulo Tetis (praderas de arena)",
            "Extrae un cristal resonante del arrecife de cristal (Rémora)",
            "Fabrica la baliza y lánzala desde la superficie",
            "Coloca un sensor sísmico en la meseta roja", "Coloca el segundo sensor en la llanura exterior",
            "Entra en el puente del pecio Albatros (llanura exterior)",
            "Fragmento de glifo de los cañones (radiación: traje ligero)",
            "Fragmento de glifo de las grietas termales (radiación alta)",
            "Fragmento de glifo del arrecife de cristal",
            "Busca el órgano de los colosos en el osario (Rémora a 450 m)",
            "Fabrica el Batel (marfil del osario, uranio de las grietas)",
            "Abre el Santuario, al borde del vacío en la fosa",
            "Lleva la llave de resonancia a la Madre del Latido",
            "Apaga el reactor de la Halcyon (grietas termales, traje pesado)",
            "Ve a la boya de la Petrel (llanura exterior, en la superficie)", "Talassa respira. Sigue explorando" };

    /** radio messages: index = the stage they announce */
    static final String[][] RADIO = {
            {},
            { "NÚCLEO - sin señal", "La radio está muda: el maremoto fundió el módulo.",
                    "Fabrica un kit de reparación y úsalo aquí." },
            { "NÚCLEO - Sistema de la Halcyon", "Emergencia. Baliza principal destruida.",
                    "El repuesto viajaba en el Contenedor 7: su transpondedor",
                    "emite desde el bosque de algas. Señal en el radar." },
            { "NÚCLEO - Sistema de la Halcyon", "Módulo de baliza recibido. Falta energía.",
                    "El módulo de investigación Tetis cayó en las praderas",
                    "de arena con su núcleo intacto. Algo vive bajo la arena:", "nada sobre la hierba." },
            { "NÚCLEO - Sistema de la Halcyon", "Núcleo aceptado. Para sintonizar la baliza hace falta",
                    "un cristal resonante del arrecife de cristal (130 m).", "Demasiado hondo a pulmón: fabrica la Rémora.",
                    "De día caza allí algo que no se ve. Ve de noche." },
            { "NÚCLEO - Sistema de la Halcyon", "Componentes completos. Ensambla la baliza en el",
                    "fabricador y lánzala desde la superficie (PDA, X)." },
            { "NÚCLEO - Nave de rescate Petrel", "Tenemos tu baliza. Somos la segunda nave: la Albatros",
                    "vino antes que nosotros y no volvió. No bajaremos",
                    "mientras ese pulso sacuda el mar cada 41 minutos.",
                    "Coloca sensores sísmicos en la meseta roja y en la", "llanura exterior (cerámica: azufre de la meseta)." },
            { "NÚCLEO - Nave de rescate Petrel", "Primer sensor en línea. El segundo, en la llanura",
                    "exterior: allí cayó la Albatros." },
            { "NÚCLEO - Nave de rescate Petrel", "Triangulado... y los dos sensores apuntan a la",
                    "Albatros. Su caja negra sigue emitiendo desde el", "puente del pecio. Entra y léela." },
            { "NÚCLEO - Nave de rescate Petrel", "La caja negra marca tres piedras talladas: en los",
                    "cañones, en las grietas termales y en el arrecife.",
                    "Las grietas queman de radiación: fabrica el traje", "ligero (visor de cristal plomado del arrecife)." },
            { "NÚCLEO - Nave de rescate Petrel", "Un fragmento. El siguiente, en las grietas termales." },
            { "NÚCLEO - Nave de rescate Petrel", "Dos fragmentos. Queda el del arrecife de cristal." },
            { "NÚCLEO - Nave de rescate Petrel", "Los tres fragmentos vibran juntos, pero no basta:",
                    "les falta una voz. Al norte, en el osario, los antiguos",
                    "convirtieron el cráneo de un coloso en un instrumento.",
                    "La Rémora solo llega con el módulo de profundidad.", "De noche salen los roehuesos." },
            { "NÚCLEO - Nave de rescate Petrel", "Ese diapasón es la voz que faltaba. El Santuario está",
                    "bajo la fosa, a más de 800 m: ni la Rémora ni tu traje",
                    "lo aguantan. Fabrica el Batel: marfil del osario y", "uranio de las grietas termales." },
            { "NÚCLEO - Nave de rescate Petrel", "El Batel está listo. El Santuario está al borde del",
                    "vacío, en la fosa. Algo enorme patrulla ese borde:", "no te separes del fondo." },
            { "NÚCLEO - Nave de rescate Petrel", "Hemos leído la llave. Los antiguos no hicieron un",
                    "templo: hicieron una cuna. El Latido es el corazón de",
                    "una criatura que duerme bajo la fosa: la Madre.", "Los colosos eran sus voces. La llave puede calmarla." },
            { "NÚCLEO - Nave de rescate Petrel", "El pulso cesó... pero la Madre se apaga. El Latido no",
                    "era rabia: era dolor. Algo sigue cantando desafinado",
                    "bajo las grietas termales: el reactor de la Halcyon.",
                    "Okafor lo sabía. Apágalo (traje pesado o el Batel)." },
            { "NÚCLEO - Nave de rescate Petrel", "Lo has hecho. Todo el cráter brilla. Hemos dejado una",
                    "boya en la superficie de la llanura exterior.",
                    "Embarcamos al amanecer. Si vienes, tus datos irán a la",
                    "flota. Si te quedas... nadie sabrá que esto existe." },
            {} };

    static final String[] FINAL = { "LA BALIZA SUBE HACIA EL CIELO DE TALASSA",
            "Una columna de luz atraviesa las nubes. La radio responde:", "una segunda nave, la Petrel, viene en camino...",
            "pero bajo tus pies el Latido cambia de ritmo,", "como si algo, muy abajo, también hubiera escuchado.", "",
            "FIN DEL ACTO I - escucha la radio de la cápsula", "(X para continuar)" };

    static final String[] ORGANO_TXT = { "EL ÓRGANO DE LOS COLOSOS",
            "Soplas por la boca del cráneo y los tubos de hueso cantan.",
            "Ves a los colosos llevar el canto de la Madre de un borde",
            "al otro del cráter. Cuando murieron, el canto se rompió.",
            "Sobre el altar, un diapasón de hueso vibra con tus glifos.", "", "+1 Diapasón de hueso",
            "(X para continuar)" };

    static final String[] INSCRIPCION = { "EL SANTUARIO",
            "Los glifos se encienden al tocar el altar y el diapasón",
            "les da voz. Imágenes de criaturas enormes que duermen,",
            "de pueblos que cantaban para calmarlas.",
            "Del núcleo se desprende una llave que vibra en tus manos.", "", "+1 Llave de resonancia",
            "(X para continuar)" };

    static final String[] FINAL2 = { "LA MADRE DEL LATIDO",
            "La llave canta. Los tentáculos de la Madre se detienen,",
            "su luz se vuelve lenta... y el pulso cesa.",
            "Pero su brillo es débil, y bajo las grietas algo",
            "sigue sonando, metálico y desafinado.", "",
            "FIN DEL ACTO III - escucha la radio de la cápsula", "(X para continuar)" };

    static final String[] REACTOR_TXT = { "EL REACTOR DE LA HALCYON",
            "Bajas las barras de control una a una. El zumbido que",
            "llenaba las grietas se apaga... y por primera vez oyes",
            "el Latido limpio: lento, hondo, sano. La Madre contesta",
            "y todo el cráter se enciende de luz.", "",
            "Escucha la radio de la cápsula", "(X para continuar)" };

    static final String[] ELECCION = { "LA PETREL",
            "La cápsula de embarque está abierta. Si subes, vuelves a",
            "casa, y tus datos sobre la Madre llegarán a la flota,",
            "que vendrá a estudiarla... o a desmontarla.",
            "Si te quedas, el cráter guardará su secreto, y tú con él.", "",
            "X subir a la Petrel", "O quedarte en Talassa" };

    static final String[] FINAL_IRSE = { "DE VUELTA A CASA",
            "La cápsula sube entre burbujas. Por la ventanilla ves",
            "el cráter encenderse una última vez, como un adiós.",
            "Tus notas viajan contigo. Alguien, algún día, volverá.", "",
            "FIN - gracias por jugar ABISAL", "", "(X para seguir explorando)" };

    static final String[] FINAL_QUEDARSE = { "GUARDIÁN DE TALASSA",
            "Ves la Petrel alejarse hasta ser una estrella más.",
            "Abajo, la Madre late tranquila. Los colosos jóvenes",
            "vuelven a cantar, y tú aprendes su canción.", "",
            "FIN - gracias por jugar ABISAL", "", "(X para seguir explorando)" };

    /** the choice at the Petrel is on screen (X leave, O stay) */
    boolean eligiendo;
    /** 0 not yet, 1 left with the Petrel, 2 stayed */
    int finalElegido;

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
    private final ModeloM3G mContenedor, mTetis, mCristal, mSensor, mGlifo, mSantuario, mMadre, mOrgano, mReactor,
            mPetrel;
    private final Transform tq = new Transform();
    private final boolean[] vistoLog = new boolean[LUGARES];
    private final Transform t = new Transform(), tp = new Transform();
    /** radius and height of each place for collisions, and how close to use it */
    static final float[] RADIO_LUGAR = { 3.0f, 3.2f, 1.8f, 0.6f, 0.6f, 2.6f, 30f, 1.2f, 1.2f, 1.2f, 5.5f, 3.2f, 0f, 0f };
    static final float[] ALTO_LUGAR = { 3.4f, 3.4f, 4f, 2f, 2f, 2f, 26f, 3f, 3f, 3f, 6.5f, 7.5f, 0f, 0f };
    static final float[] ALCANCE = { 6.5f, 6.5f, 5f, 5f, 5f, 6f, 45f, 5f, 5f, 5f, 9f, 7f, 7f, 0f };

    Historia(Fog fog) {
        // fixed, deterministic places: each in its own zone, at a heading and fraction of the
        // way from the zone's centre (rel 1 = 520 m)
        colocar(0, 40, 0.3f, Terreno.BOSQUE, -400);
        colocar(1, 200, 0.35f, Terreno.PRADERA, -400);
        colocar(2, 120, 0.3f, Terreno.CRISTAL, -400);
        colocar(SENSOR_A, 60, 0.35f, Terreno.MESETA, -400);
        colocar(SENSOR_B, 250, 0.3f, Terreno.LLANURA, -600);
        // the Sanctuary sits on the trench's outer rim, right at the edge of the void
        colocar(SANTUARIO, 125, 0.95f, Terreno.ABISMO, -2000);
        colocar(MADRE, 0, 0, Terreno.ABISMO, -2000);
        colocar(FRAG, 250, 0.45f, Terreno.CANONES, -600);
        colocar(FRAG + 1, 0, 0.5f, Terreno.TERMAL, -600);
        colocar(FRAG + 2, 300, 0.4f, Terreno.CRISTAL, -400);
        colocar(ORGANO, 270, 0.35f, Terreno.OSARIO, -440);
        colocar(REACTOR, 160, 0.3f, Terreno.TERMAL, -600);
        // the Petrel's buoy floats on the surface over the outer shelf
        colocar(PETREL, 100, 0.25f, Terreno.LLANURA, -1000);
        ly[PETREL] = 0;
        // the story models are made in Blender (assets/modelos); the old code-built ones
        // are only made if a file is missing
        mContenedor = ModeloM3G.cargar("/modelos/contenedor.m3g", fog, 3);
        mTetis = ModeloM3G.cargar("/modelos/tetis.m3g", fog, 4);
        mCristal = ModeloM3G.cargar("/modelos/cristal.m3g", fog, 3);
        mSensor = ModeloM3G.cargar("/modelos/sensor.m3g", fog, 3);
        mGlifo = ModeloM3G.cargar("/modelos/glifo.m3g", fog, 2);
        mSantuario = ModeloM3G.cargar("/modelos/santuario.m3g", fog, 3);
        mMadre = ModeloM3G.cargar("/modelos/madre.m3g", fog, 3);
        mOrgano = ModeloM3G.cargar("/modelos/organo.m3g", fog, 2);
        mReactor = ModeloM3G.cargar("/modelos/reactor.m3g", fog, 2);
        mPetrel = ModeloM3G.cargar("/modelos/boya_petrel.m3g", fog, 2);
        // pixel-art detail like the rest of the scenery: plating on the machines, stone on the
        // alien places
        ModeloM3G[] metal = { mContenedor, mTetis, mSensor, mReactor, mPetrel };
        for (int i = 0; i < metal.length; i++) {
            if (metal[i] != null) {
                metal[i].texturizar(fog, Texturas.panel(), 2f);
            }
        }
        ModeloM3G[] piedra = { mCristal, mGlifo, mSantuario, mOrgano, mMadre };
        for (int i = 0; i < piedra.length; i++) {
            if (piedra[i] != null) {
                piedra[i].texturizar(fog, Texturas.piedra(), 1.25f);
            }
        }
        if (mOrgano != null) {
            mOrgano.brilloSinNiebla();
        }
        if (mReactor != null) {
            mReactor.brilloSinNiebla();
        }
        // in the dark of the trench the sensor lights and the Mother's veins must be seen
        if (mSensor != null) {
            mSensor.brilloSinNiebla();
        }
        if (mMadre != null) {
            mMadre.brilloSinNiebla();
        }
        glifo = mGlifo == null ? Modelos.piedraGlifo(fog) : null;
        mallas[0] = mContenedor == null ? Modelos.contenedor(fog) : null;
        mallas[1] = mTetis == null ? Modelos.moduloTetis(fog) : null;
        mallas[2] = mCristal == null ? Modelos.cristalResonante(fog) : null;
        mallas[SENSOR_A] = mallas[SENSOR_B] = mSensor == null ? Modelos.sensor(fog) : null;
        mallas[SANTUARIO] = mSantuario == null ? Modelos.santuario(fog) : null;
        madreP = mMadre == null ? Modelos.madre(fog) : null;
    }

    private final float[] pto = new float[2];

    /**
     * Put place i in its zone: from the zone's centre towards the given heading, at rel x
     * 520 m, then the nearest spot that really is that zone and not deeper than minY.
     */
    private void colocar(int i, float grados, float rel, int bioma, float minY) {
        Terreno.buscarPunto(bioma, grados, rel, minY, pto);
        poner(i, pto[0], pto[1]);
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
        return e >= 2 && e <= 17 && e != 7 && e != 10 && e != 11 && e != 15;
    }

    /** the Albatros wreck (set by the game once it has placed it) */
    void lugarPecio(float x, float y, float z) {
        lx[PECIO] = x;
        ly[PECIO] = y;
        lz[PECIO] = z;
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
            return PECIO;
        case 9:
        case 10:
        case 11:
            return FRAG + etapa - 9;
        case 12:
            return ORGANO;
        case 14:
            return SANTUARIO;
        case 15:
            return MADRE;
        case 16:
            return REACTOR;
        case 17:
            return PETREL;
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
        return o == LUGARES ? 0 : ly[o] + (o == MADRE ? 25 : (o == PETREL ? 0 : 2));
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
        Inventario.etapa = etapa;
        if (etapa == 0 && p.inv.tiene[Inventario.H_CUCHILLO] && p.inv.tiene[Inventario.H_LINTERNA]) {
            avanzar(p, 1);
        }
        if (etapa == 13 && p.inv.tiene[Inventario.H_BATEL]) {
            avanzar(p, 14);
        }
    }

    /** the diver reached the Albatros bridge */
    void puentePecio(Partida p) {
        if (etapa == 8) {
            tomado[PECIO] = true;
            avanzar(p, 9);
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
            if (i == PECIO) {
                continue; // the wreck's rooms are handled by the game
            }
            float dx = px - lx[i], dy = py - ly[i], dz = pz - lz[i];
            float r = ALCANCE[i];
            if (i == PETREL) {
                // the buoy floats on the surface: board it from the surface or just below
                if (dx * dx + dz * dz < r * r && py > -6) {
                    return i;
                }
                continue;
            }
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
            return etapa == 14 && !tomado[i] ? "[] Colocar los fragmentos y el diapasón" : "Santuario de los antiguos";
        case MADRE:
            return etapa == 15 ? "[] Usar la llave de resonancia" : "Madre del Latido";
        case FRAG:
        case FRAG + 1:
        case FRAG + 2:
            if (tomado[i]) {
                return "Piedra de glifo (vacía)";
            }
            return etapa == 9 + i - FRAG ? "[] Desprender el fragmento de glifo" : "Piedra de glifo";
        case ORGANO:
            return etapa == 12 && !tomado[i] ? "[] Soplar por el órgano de los colosos" : "Órgano de los colosos";
        case REACTOR:
            return etapa == 16 && !tomado[i] ? "[] Bajar las barras de control" : "Reactor de la Halcyon";
        case PETREL:
            return etapa == 17 ? "[] Subir a la cápsula de la Petrel" : "Boya de la Petrel";
        default:
            if (tomado[i]) {
                return NOMBRE_LUGAR[i] + " (ya revisado)";
            }
            return i == 0 ? "[] Abrir el Contenedor 7" : (i == 1 ? "[] Buscar en el módulo Tetis" : "[] Extraer el cristal");
        }
    }

    /** show a text panel and switch the game to it */
    private void panel(Partida p, String[] txt) {
        pantalla = txt;
        p.estado = Partida.FINAL;
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
                p.avisar("Necesitas un sensor sísmico (fabricador, pestaña MISIÓN)");
                return true;
            }
            p.inv.cant[Inventario.SENSOR]--;
            tomado[i] = true;
            Sonido.tocar(Sonido.FABRICAR);
            Log.d("historia: sensor colocado en " + NOMBRE_LUGAR[i]);
            avanzar(p, quiere + 1);
            return true;
        }
        if (i >= FRAG && i < FRAG + 3) {
            if (tomado[i] || etapa != 9 + i - FRAG) {
                return false;
            }
            tomado[i] = true;
            p.inv.cant[Inventario.FRAGMENTO]++;
            Sonido.tocar(Sonido.LATIDO);
            p.avisar("+1 Fragmento de glifo (" + p.inv.cant[Inventario.FRAGMENTO] + "/3)");
            avanzar(p, etapa + 1);
            return true;
        }
        if (i == ORGANO) {
            if (etapa != 12 || tomado[i]) {
                return false;
            }
            if (p.inv.cant[Inventario.FRAGMENTO] < 3) {
                p.avisar("El órgano calla: le faltan los tres fragmentos de glifo");
                return true;
            }
            tomado[i] = true;
            p.inv.cant[Inventario.DIAPASON]++;
            Sonido.tocar(Sonido.LATIDO);
            Sonido.tocar(Sonido.CANTO);
            avanzar(p, 13);
            panel(p, ORGANO_TXT);
            Log.d("historia: organo de los colosos, diapason");
            return true;
        }
        if (i == SANTUARIO) {
            if (etapa != 14 || tomado[i]) {
                return false;
            }
            if (p.inv.cant[Inventario.FRAGMENTO] < 3 || p.inv.cant[Inventario.DIAPASON] <= 0) {
                p.avisar("El altar tiene tres huecos y una horquilla: faltan piezas");
                return true;
            }
            p.inv.cant[Inventario.FRAGMENTO] -= 3;
            p.inv.cant[Inventario.DIAPASON]--;
            tomado[i] = true;
            p.inv.cant[Inventario.LLAVE]++;
            Sonido.tocar(Sonido.LATIDO);
            Sonido.tocar(Sonido.CANTO);
            avanzar(p, 15);
            panel(p, INSCRIPCION);
            return true;
        }
        if (i == MADRE) {
            if (etapa != 15) {
                return false;
            }
            if (p.inv.cant[Inventario.LLAVE] <= 0) {
                p.avisar("Necesitas la llave de resonancia del Santuario");
                return true;
            }
            p.inv.cant[Inventario.LLAVE]--;
            Sonido.tocar(Sonido.CANTO);
            avanzar(p, 16);
            panel(p, FINAL2);
            Log.d("historia: la Madre calmada (fin del acto III)");
            return true;
        }
        if (i == REACTOR) {
            if (etapa != 16 || tomado[i]) {
                return false;
            }
            if (!p.inv.tiene[Inventario.H_TRAJE_RAD2]) {
                p.avisar("La radiación del reactor te quemaría: necesitas el traje pesado");
                return true;
            }
            tomado[i] = true;
            Sonido.tocar(Sonido.ESCOTILLA);
            Sonido.tocar(Sonido.CANTO);
            avanzar(p, 17);
            panel(p, REACTOR_TXT);
            Log.d("historia: reactor apagado");
            return true;
        }
        if (i == PETREL) {
            if (etapa != 17) {
                return false;
            }
            eligiendo = true;
            panel(p, ELECCION);
            Sonido.tocar(Sonido.ESCOTILLA);
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

    /** the choice at the Petrel: leave (true) or stay */
    void elegir(Partida p, boolean irse) {
        eligiendo = false;
        finalElegido = irse ? 1 : 2;
        avanzar(p, 18);
        pantalla = irse ? FINAL_IRSE : FINAL_QUEDARSE;
        Sonido.tocar(irse ? Sonido.ESCOTILLA : Sonido.CANTO);
        Log.d("historia: final elegido - " + (irse ? "irse con la Petrel" : "quedarse en Talassa"));
    }

    /** true when the Heartbeat can be felt (near the Mother, before the true ending) */
    boolean latidoCerca(float px, float pz) {
        float dx = px - lx[MADRE], dz = pz - lz[MADRE];
        return etapa < 16 && dx * dx + dz * dz < 380 * 380;
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
            if (((i == SENSOR_A || i == SENSOR_B) && !tomado[i]) || i == PETREL || i == PECIO) {
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
            if (((i == SENSOR_A || i == SENSOR_B) && !tomado[i]) || i == PECIO || (i == PETREL && etapa < 17)) {
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
            if (i >= ORGANO) {
                continue; // no code-built stand-in for the newer places
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
        float latido = etapa < 16 ? pulso(tiempo) : 0;
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
        if (i == ORGANO && mOrgano != null) {
            t.postRotate(180, 0, 1, 0);
            pieza(g3d, mOrgano, 0);
            if (!tomado[ORGANO]) {
                // the bone tuning fork floats and turns over the altar, humming
                piezaGiro(g3d, mOrgano, 1, tiempo * 40, j2me.FMath.sin(tiempo * 1.3f) * 0.2f);
            }
            return true;
        }
        if (i == REACTOR && mReactor != null) {
            pieza(g3d, mReactor, 0);
            // the core flickers out of tune until it is shut down
            int f = (int) (tiempo * 9);
            if (!tomado[REACTOR] && f % 7 != 0) {
                piezaEscala(g3d, mReactor, 1, 1 + 0.03f * j2me.FMath.sin(tiempo * 23));
            }
            return true;
        }
        if (i == PETREL && mPetrel != null) {
            // bobbing on the swell
            t.postTranslate(0, j2me.FMath.sin(tiempo * 1.1f) * 0.25f, 0);
            t.postRotate(j2me.FMath.sin(tiempo * 0.8f) * 4, 0, 0, 1);
            pieza(g3d, mPetrel, 0);
            if ((tiempo % 1.2f) < 0.3f) {
                pieza(g3d, mPetrel, 1);
            }
            return true;
        }
        if (i >= FRAG && i < FRAG + 3 && mGlifo != null) {
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
            float calma = etapa < 16 ? 1 : 0.3f;
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
        float latido = etapa < 16 ? pulso(tiempo) : 0;
        float k = 1 + latido * 0.03f;
        tp.set(t);
        tp.postScale(k, 1 + latido * 0.05f, k);
        g3d.render(madreP[6], tp);
        float calma = etapa < 16 ? 1 : 0.3f;
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
