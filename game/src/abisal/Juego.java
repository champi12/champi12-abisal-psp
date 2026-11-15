package abisal;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/** Main canvas: owns the game loop and switches between title, loading, game and gallery. */
public final class Juego extends GameCanvas implements Runnable {
    public static final int W = 480;
    public static final int H = 272;
    static final int FPS = 30;
    public static final String VERSION = "0.2";

    static final int TITULO = 0, CARGANDO = 1, JUGANDO = 2, GALERIA = 3;

    final AbisalMIDlet midlet;
    volatile boolean corriendo = true;
    volatile boolean pausado;
    int estado = TITULO;
    int selTitulo;
    Galeria galeria;
    Galeria fondoTitulo;
    Partida partida;
    final Entrada in = new Entrada();
    long frame;

    Juego(AbisalMIDlet m) {
        super(false);
        midlet = m;
        setFullScreenMode(true);
    }

    public void run() {
        Log.d("ABISAL " + VERSION + " - hilo del juego iniciado; " + Log.memoria());
        int fw = j2me.Pad.firmware();
        if (fw != 0) {
            Log.d("firmware PSP: " + Integer.toHexString(fw));
        }
        Graphics g = getGraphics();
        try {
            Sonido.cargar();
            String gal = midlet.getAppProperty("Abisal-Galeria");
            if (gal != null) {
                galeria = new Galeria(gal.trim(), true);
                estado = GALERIA;
            } else {
                fondoTitulo = new Galeria("capsula", false);
            }
            Log.d("pantalla de titulo lista; " + Log.memoria());
        } catch (Throwable e) {
            Log.d("ERROR al iniciar: " + e);
            e.printStackTrace();
        }
        long next = System.currentTimeMillis();
        while (corriendo) {
            if (!pausado) {
                try {
                    paso(g);
                } catch (Throwable e) {
                    Log.d("ERROR en el bucle (" + nombreEstado() + "): " + e);
                    e.printStackTrace();
                    estado = TITULO;
                    partida = null;
                }
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
        Log.d("saliendo del juego");
        midlet.salir();
    }

    private String nombreEstado() {
        String[] n = { "titulo", "cargando", "jugando", "galeria" };
        return n[estado];
    }

    private void paso(Graphics g) {
        in.leerAnalogico();
        float dt = 1f / FPS;
        switch (estado) {
        case TITULO:
            fondoTitulo.actualizar(in, dt);
            fondoTitulo.dibujar(g);
            dibujarTitulo(g);
            if (in.pulso(Entrada.ABAJO) || in.pulso(Entrada.ARRIBA)) {
                selTitulo = 1 - selTitulo;
            }
            if (in.pulso(Entrada.X)) {
                if (selTitulo == 0) {
                    estado = CARGANDO;
                    Log.d("menu: nueva partida");
                } else {
                    galeria = new Galeria(null, true);
                    estado = GALERIA;
                    Log.d("menu: galeria de modelos");
                }
            }
            break;
        case CARGANDO:
            // one frame with the loading text, then build the world
            g.setColor(0x041420);
            g.fillRect(0, 0, W, H);
            g.setColor(0x7fe8f4);
            g.drawString("Cargando el oceano...", W / 2, H / 2 - 5, Graphics.TOP | Graphics.HCENTER);
            in.finFrame();
            flushGraphics();
            fondoTitulo = null;
            System.gc();
            Log.d("cargando partida; " + Log.memoria());
            partida = new Partida();
            estado = JUGANDO;
            return;
        case JUGANDO:
            partida.actualizar(in, dt);
            partida.dibujar(g);
            if (partida.salirAlTitulo) {
                partida = null;
                System.gc();
                fondoTitulo = new Galeria("capsula", false);
                estado = TITULO;
                Log.d("de vuelta en el titulo; " + Log.memoria());
            }
            break;
        default:
            galeria.actualizar(in, dt);
            galeria.dibujar(g);
            if (in.pulso(Entrada.CIRCULO) && fondoTitulo != null) {
                galeria = null;
                estado = TITULO;
            }
            break;
        }
        in.finFrame();
        flushGraphics();
    }

    private void dibujarTitulo(Graphics g) {
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.setColor(0x06141c);
        g.drawString("A B I S A L", W / 2 + 2, 30, Graphics.TOP | Graphics.HCENTER);
        g.setColor(0xe8fbff);
        g.drawString("A B I S A L", W / 2, 28, Graphics.TOP | Graphics.HCENTER);
        g.setFont(Font.getDefaultFont());
        g.setColor(0x9fe6f0);
        g.drawString("Supervivencia en el oceano de Talassa", W / 2, 54, Graphics.TOP | Graphics.HCENTER);
        String[] op = { "Nueva partida", "Galeria de modelos" };
        for (int i = 0; i < 2; i++) {
            g.setColor(0x06141c);
            g.fillRect(W / 2 - 80, 196 + i * 20, 160, 16);
            g.setColor(i == selTitulo ? 0xffffff : 0x6a8a94);
            g.drawString((i == selTitulo ? "> " : "") + op[i], W / 2, 199 + i * 20, Graphics.TOP | Graphics.HCENTER);
        }
        g.setColor(0x9fc4cc);
        g.drawString("v" + VERSION, W - 6, H - 12, Graphics.TOP | Graphics.RIGHT);
    }

    protected void keyPressed(int k) {
        in.presionar(k);
    }

    protected void keyReleased(int k) {
        in.soltar(k);
    }
}
