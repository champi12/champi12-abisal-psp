package abisal;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

/** Sound effects (generated at build time by tools/assets/gen_sounds.py). */
public final class Sonido {
    public static final int RECOGER = 0, BURBUJAS = 1, GOLPE = 2, RUGIDO = 3, DESCARGA = 4, TAJO = 5,
            MORDIDA = 6, ALARMA = 7, FABRICAR = 8, PDA = 9, CLICK = 10, MOTOR = 11, SONAR = 12, LATIDO = 13, CHILLIDO = 14, ESCOTILLA = 15,
            ATRAPAR = 16, CORTAR = 17, MINERAL = 18, COMER = 19, VENENO = 20, ESCUPIR = 21, SALPICON = 22,
            GOLPE_PEZ = 23, GOLPE_ROCA = 24, CASCO_ROTO = 25, EXPLOSION = 26, HINCHAR = 27, CANTO = 28, CRISTAL = 29,
            HUESO = 30, SILBIDO = 31, BRAZADA = 32, ZONA = 33, ESCANER = 34;
    private static final String[] RUTA = { "/snd/recoger.wav", "/snd/burbujas.wav", "/snd/golpe.wav",
            "/snd/rugido.wav", "/snd/descarga.wav", "/snd/tajo.wav", "/snd/mordida.wav", "/snd/alarma.wav",
            "/snd/fabricar.wav", "/snd/pda.wav", "/snd/click.wav", "/snd/motor.wav", "/snd/sonar.wav",
            "/snd/latido.wav", "/snd/chillido.wav",
            "/snd/escotilla.wav", "/snd/atrapar.wav", "/snd/cortar.wav", "/snd/mineral.wav", "/snd/comer.wav",
            "/snd/veneno.wav", "/snd/escupir.wav", "/snd/salpicon.wav", "/snd/golpe_pez.wav", "/snd/golpe_roca.wav",
            "/snd/casco_roto.wav", "/snd/explosion.wav", "/snd/hinchar.wav", "/snd/canto.wav", "/snd/cristal.wav",
            "/snd/hueso.wav", "/snd/silbido.wav", "/snd/brazada.wav", "/snd/zona.wav", "/snd/escaner.wav" };
    private static final Player[] p = new Player[RUTA.length];
    private static Player musica;
    private static Player remora;
    private static Player remoraGo;

    private Sonido() {
    }

    static void cargar() {
        for (int i = 0; i < RUTA.length; i++) {
            String r = RUTA[i];
            Log.d("Sonido.cargar: " + (i + 1) + "/" + RUTA.length + " " + r);
            try {
                p[i] = Manager.createPlayer(r);
                Log.d("Sonido.cargar: createPlayer OK " + r);
                p[i].prefetch();
                Log.d("sonido cargado: " + r);
            } catch (Throwable e) {
                Log.d("sonido FALLO: " + r + " (" + e + ")");
                e.printStackTrace();
            }
        }
        Log.d("Sonido.cargar: fin");
        // ambient music: a long generated loop, played on repeat at low volume
        try {
            musica = Manager.createPlayer("/snd/musica.wav");
            musica.prefetch();
            musica.setLoopCount(-1);
            VolumeControl vc = (VolumeControl) musica.getControl("VolumeControl");
            if (vc != null) {
                vc.setLevel(45);
            }
            Log.d("musica cargada");
        } catch (Throwable e) {
            Log.d("musica FALLO: " + e);
        }
        // the Remora's cabin ambience: looped while you are inside it, very low
        try {
            remora = Manager.createPlayer("/snd/remora_ambient.wav");
            remora.prefetch();
            remora.setLoopCount(-1);
            VolumeControl vc2 = (VolumeControl) remora.getControl("VolumeControl");
            if (vc2 != null) {
                vc2.setLevel(22);
            }
            Log.d("remora ambiente cargado");
        } catch (Throwable e) {
            Log.d("remora ambiente FALLO: " + e);
        }
        // the propeller loop while it actually moves
        try {
            remoraGo = Manager.createPlayer("/snd/remora_go.wav");
            remoraGo.prefetch();
            remoraGo.setLoopCount(-1);
            VolumeControl vc3 = (VolumeControl) remoraGo.getControl("VolumeControl");
            if (vc3 != null) {
                vc3.setLevel(38);
            }
            Log.d("remora marcha cargada");
        } catch (Throwable e) {
            Log.d("remora marcha FALLO: " + e);
        }
    }

    static void remoraEmpezar() {
        try {
            if (remora != null) {
                remora.start();
            }
        } catch (Throwable e) {
            Log.d("remora ambiente error: " + e);
        }
    }

    static void remoraParar() {
        try {
            if (remora != null) {
                remora.stop();
            }
        } catch (Throwable e) {
            Log.d("remora ambiente error: " + e);
        }
    }

    static void remoraGoEmpezar() {
        try {
            if (remoraGo != null) {
                remoraGo.start();
            }
        } catch (Throwable e) {
            Log.d("remora marcha error: " + e);
        }
    }

    static void remoraGoParar() {
        try {
            if (remoraGo != null) {
                remoraGo.stop();
            }
        } catch (Throwable e) {
            Log.d("remora marcha error: " + e);
        }
    }

    /** silence every voice (pause menu, leaving the title): loops and one-shots alike */
    static void pararTodo() {
        for (int i = 0; i < p.length; i++) {
            try {
                if (p[i] != null) {
                    p[i].stop();
                }
            } catch (Throwable e) {
            }
        }
        remoraParar();
        remoraGoParar();
        try {
            if (musica != null) {
                musica.stop();
            }
        } catch (Throwable e) {
            Log.d("musica error: " + e);
        }
    }

    static void musicaEmpezar() {
        try {
            if (musica != null) {
                musica.start();
            }
        } catch (Throwable e) {
            Log.d("musica error: " + e);
        }
    }

    private static final long[] ultima = new long[RUTA.length];

    static void tocar(int i) {
        tocar(i, 100);
    }

    /** play sound i at the given volume (0-100), so far sounds can be fainter */
    static void tocar(int i, int volumen) {
        // never retrigger the same sound within 250 ms: stop/start touches the mixer lock
        long ahora = System.currentTimeMillis();
        if (ahora - ultima[i] < 250) {
            return;
        }
        ultima[i] = ahora;
        int antes = Log.fase(Log.F_SONIDO);
        try {
            if (p[i] != null) {
                VolumeControl vc = (VolumeControl) p[i].getControl("VolumeControl");
                if (vc != null) {
                    vc.setLevel(volumen < 0 ? 0 : (volumen > 100 ? 100 : volumen));
                }
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
