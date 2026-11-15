package abisal;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/** Main canvas: owns the game loop and dispatches to the current screen. */
public final class Juego extends GameCanvas implements Runnable {
    public static final int W = 480;
    public static final int H = 272;
    static final int FPS = 30;

    final AbisalMIDlet midlet;
    volatile boolean corriendo = true;
    volatile boolean pausado;
    Galeria galeria;
    final Entrada in = new Entrada();
    long frame;

    Juego(AbisalMIDlet m) {
        super(false);
        midlet = m;
        setFullScreenMode(true);
    }

    public void run() {
        Graphics g = getGraphics();
        String gal = midlet.getAppProperty("Abisal-Galeria");
        galeria = new Galeria(gal != null ? gal.trim() : null);
        long next = System.currentTimeMillis();
        while (corriendo) {
            if (!pausado) {
                in.leerAnalogico();
                galeria.actualizar(in, 1f / FPS);
                galeria.dibujar(g);
                in.finFrame();
                flushGraphics();
                frame++;
            }
            next += 1000 / FPS;
            long wait = next - System.currentTimeMillis();
            if (wait < -200) {
                next = System.currentTimeMillis();
            }
            try {
                Thread.sleep(wait > 1 ? wait : 1);
            } catch (InterruptedException e) {
            }
        }
        midlet.salir();
    }

    protected void keyPressed(int k) {
        in.presionar(k);
    }

    protected void keyReleased(int k) {
        in.soltar(k);
    }
}
