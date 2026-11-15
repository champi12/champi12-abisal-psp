package abisal;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/** Sound effects (generated at build time by tools/assets/gen_sounds.py). */
public final class Sonido {
    public static final int RECOGER = 0, BURBUJAS = 1;
    private static final String[] RUTA = { "/snd/recoger.wav", "/snd/burbujas.wav" };
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

    static void tocar(int i) {
        try {
            if (p[i] != null) {
                p[i].stop();
                p[i].setMediaTime(0);
                p[i].start();
            }
        } catch (Throwable e) {
            Log.d("sonido error al tocar " + i + ": " + e);
        }
    }
}
