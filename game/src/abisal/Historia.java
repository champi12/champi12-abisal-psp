package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * The story missions. The crew logs point the way; the capsule radio (once repaired)
 * marks each place on the radar and the HUD:
 *   0 build the knife and the flashlight
 *   1 repair the capsule radio (repair kit)
 *   2 Container 7 on the red plateau: the spare beacon module
 *   3 research module "Tetis" in the thermal rifts: the power core
 *   4 a resonant crystal in the abyss (the Remora needs the depth module)
 *   5 build the rescue beacon and launch it from the surface
 *   6 the end (free play goes on)
 */
public final class Historia {
    static final int LUGARES = 3;
    static final String[] NOMBRE_LUGAR = { "Contenedor 7", "Modulo Tetis", "Cristal resonante" };
    static final String[] MISION = { "Fabrica el cuchillo y la linterna",
            "Repara la radio de la capsula (kit de reparacion)", "Busca el Contenedor 7 en la meseta roja",
            "Recupera el nucleo del modulo Tetis (grietas termales)",
            "Extrae un cristal resonante en la fosa (modulo de profundidad)",
            "Fabrica la baliza y lanzala desde la superficie", "Rescate en camino. Sigue explorando" };

    /** radio messages: index = the stage they announce */
    static final String[][] RADIO = {
            {},
            { "RADIO - sin senal", "La radio esta muda: el maremoto fundio el modulo.",
                    "Fabrica un kit de reparacion y usalo aqui." },
            { "RADIO - Sistema automatico de la Halcyon", "Emergencia. Baliza principal destruida.",
                    "El repuesto viajaba en el Contenedor 7: su transpondedor",
                    "emite desde la meseta roja. Senal marcada en el radar." },
            { "RADIO - Sistema automatico de la Halcyon", "Modulo de baliza recibido. Falta energia.",
                    "El modulo de investigacion Tetis cayo en las grietas",
                    "termales con su nucleo intacto. Cuidado con el calor." },
            { "RADIO - Sistema automatico de la Halcyon", "Nucleo aceptado. Para sintonizar la baliza hace",
                    "falta un cristal resonante de la fosa abisal (600 m).",
                    "Mejora la Remora: modulo de profundidad (cristal termico)." },
            { "RADIO - Sistema automatico de la Halcyon", "Componentes completos. Ensambla la baliza en el",
                    "fabricador y lanzala desde la superficie (PDA, X).", "Rescate estimado: 4 dias." },
            { "RADIO - Nave de rescate Albatros", "Recibimos tu baliza, superviviente. Aguanta.",
                    "...los sensores registran un pulso enorme bajo la fosa.",
                    "Algo respondio a la senal. Vamos tan rapido como podemos." } };

    static final String[] FINAL = { "LA BALIZA SUBE HACIA EL CIELO DE TALASSA",
            "Una columna de luz atraviesa las nubes. Minutos despues,", "la radio responde: la nave Albatros viene en camino.",
            "Bajo tus pies, el Latido cambia de ritmo...", "como si algo, muy abajo, tambien hubiera escuchado.", "",
            "FIN - gracias por jugar ABISAL", "(X para seguir explorando)" };

    int etapa;
    boolean radioReparada;
    /** last radio message heard (so the HUD can say "new message") */
    int radioOida;

    final float[] lx = new float[LUGARES], ly = new float[LUGARES], lz = new float[LUGARES];
    /** each place gives its item once */
    final boolean[] tomado = new boolean[LUGARES];
    private final Mesh[] mallas = new Mesh[LUGARES];
    private final Transform t = new Transform();

    Historia(Fog fog) {
        // fixed, deterministic places of the crater (x = cos a * r, z = sin a * r)
        colocar(0, 140, 730, Terreno.MESETA, -200);
        colocar(1, 78, 990, Terreno.TERMAL, -290);
        colocar(2, 300, 1380, Terreno.ABISMO, -2000);
        mallas[0] = Modelos.contenedor(fog);
        mallas[1] = Modelos.moduloTetis(fog);
        mallas[2] = Modelos.cristalResonante(fog);
    }

    /** search outwards from (angle, radius) for a spot in the biome not deeper than minY */
    private void colocar(int i, float grados, float r, int bioma, float minY) {
        for (int k = 0; k < 40; k++) {
            float a = (grados + (k % 5) * 3) * j2me.FMath.RAD;
            float rr = r - (k / 5) * 12;
            float x = j2me.FMath.cos(a) * rr, z = j2me.FMath.sin(a) * rr;
            float h = Terreno.altura(x, z);
            if (Terreno.bioma(x, z) == bioma && h > minY || k == 39) {
                lx[i] = x;
                lz[i] = z;
                ly[i] = h - 0.3f;
                Log.d("lugar " + NOMBRE_LUGAR[i] + " en " + (int) x + "," + (int) h + "," + (int) z);
                return;
            }
        }
    }

    /** place the current stage points to (-1 none, LUGARES = the capsule) */
    int objetivo() {
        switch (etapa) {
        case 1:
            return LUGARES;
        case 2:
            return 0;
        case 3:
            return 1;
        case 4:
            return 2;
        case 5:
            return -1;
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
        return o == LUGARES ? 0 : ly[o] + 2;
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

    void avanzar(Partida p, int nueva) {
        etapa = nueva;
        p.avisar("Nueva mision: " + MISION[etapa]);
        Sonido.tocar(Sonido.SONAR);
        Log.d("historia: etapa " + etapa + " - " + MISION[etapa]);
    }

    /** the place within reach of the diver, or -1 */
    int cercano(float px, float py, float pz) {
        for (int i = 0; i < LUGARES; i++) {
            float dx = px - lx[i], dy = py - ly[i], dz = pz - lz[i];
            float r = i == 2 ? 5 : 6.5f;
            if (dx * dx + dz * dz < r * r && dy > -2 && dy < 6) {
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
        if (tomado[i]) {
            return NOMBRE_LUGAR[i] + " (ya revisado)";
        }
        return i == 0 ? "[] Abrir el Contenedor 7" : (i == 1 ? "[] Buscar en el modulo Tetis" : "[] Extraer el cristal");
    }

    /** square near a place: take its item. true if something happened */
    boolean interactuar(Partida p) {
        int i = cercano(p.px, p.py, p.pz);
        if (i < 0) {
            return false;
        }
        if (tomado[i]) {
            p.avisar(NOMBRE_LUGAR[i] + ": no queda nada util");
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

    /** the mission places are solid */
    void empujar(float[] q, float rj) {
        for (int i = 0; i < LUGARES; i++) {
            float r = (i == 0 ? 3.0f : (i == 1 ? 3.2f : 1.8f)) + rj;
            float dx = q[0] - lx[i], dz = q[2] - lz[i];
            float top = ly[i] + (i == 2 ? 4 : 3.4f) + rj * 0.5f;
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
            if (dx * dx + dz * dz > (lejos + 8) * (lejos + 8)) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(lx[i], ly[i], lz[i]);
            t.postRotate(i * 70 + 20, 0, 1, 0);
            if (i == 2) {
                // the resonant crystal pulses with the Heartbeat
                float k = 1 + 0.05f * j2me.FMath.sin(tiempo * 3);
                t.postScale(k, k, k);
            }
            g3d.render(mallas[i], t);
        }
    }
}
