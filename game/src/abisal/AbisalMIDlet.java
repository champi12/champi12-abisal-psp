package abisal;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class AbisalMIDlet extends MIDlet {
    private Juego juego;

    private Thread hilo;

    protected void startApp() {
        if (juego == null) {
            juego = new Juego(this);
            Display.getDisplay(this).setCurrent(juego);
        }
        if (hilo == null || !hilo.isAlive()) {
            juego.corriendo = true;
            hilo = new Thread(juego);
            hilo.start();
        }
        juego.pausado = false;
    }

    protected void pauseApp() {
        if (juego != null) {
            juego.pausado = true;
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (juego != null) {
            juego.corriendo = false;
        }
    }

    void salir() {
        destroyApp(true);
        notifyDestroyed();
    }
}
