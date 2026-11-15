package abisal;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class AbisalMIDlet extends MIDlet {
    private Juego juego;

    protected void startApp() {
        if (juego == null) {
            juego = new Juego(this);
            Display.getDisplay(this).setCurrent(juego);
            new Thread(juego).start();
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
