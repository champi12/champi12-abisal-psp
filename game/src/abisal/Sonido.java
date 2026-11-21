package abisal;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/** Sound effects (generated at build time by tools/assets/gen_sounds.py). */
public final class Sonido {
    public static final int RECOGER = 0, BURBUJAS = 1, GOLPE = 2, RUGIDO = 3, DESCARGA = 4, TAJO = 5,
            MORDIDA = 6, ALARMA = 7, FABRICAR = 8, PDA = 9, CLICK = 10, MOTOR = 11, SONAR = 12, LATIDO = 13, CHILLIDO = 14;
    private static final String[] RUTA = { "/snd/recoger.wav", "/snd/burbujas.wav", "/snd/golpe.wav",
            "/snd/rugido.wav", "/snd/descarga.wav", "/snd/tajo.wav", "/snd/mordida.wav", "/snd/alarma.wav",
            "/snd/fabricar.wav", "/snd/pda.wav", "/snd/click.wav", "/snd/motor.wav", "/snd/sonar.wav",
            "/snd/latido.wav", "/snd/chillido.wav" };
    private static final Player[] p = new Player[RUTA.length];

    private Sonido() {
    }

    static void cargar() {
        for (int i = 0; i < RUTA.length; i++) {
            try {
                p[i] = Manager.createPlayer(RUTA[i]);
                p[i].prefetch();
                Log.d("sonido cargado: " + RUTA[i]);
            } catch (Throwable e) {
                Log.d("sonido FALLO: " + RUTA[i] + " (" + e + ")");
            }
        }
    }

    private static final long[] ultima = new long[RUTA.length];

    static void tocar(int i) {
        // never retrigger the same sound within 250 ms: stop/start touches the mixer lock
        long ahora = System.currentTimeMillis();
        if (ahora - ultima[i] < 250) {
            return;
        }
        ultima[i] = ahora;
        int antes = Log.fase(Log.F_SONIDO);
        try {
            if (p[i] != null) {
                p[i].stop();
                p[i].setMediaTime(0);
                p[i].start();
            }
        } catch (Throwable e) {
            Log.d("sonido error al tocar " + i + ": " + e);
        }
        Log.fase(antes);
    }
}
