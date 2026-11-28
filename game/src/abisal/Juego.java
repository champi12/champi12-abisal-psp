package abisal;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/** Main canvas: owns the game loop and switches between title, loading, game and gallery. */
public final class Juego extends GameCanvas implements Runnable {
    private int reanudacionesVistas = j2me.Pad.reanudaciones();
    public static final int W = 480;
    public static final int H = 272;
    static final int FPS = 30;
    public static final String VERSION = "0.38";

    static final int TITULO = 0, CARGANDO = 1, JUGANDO = 2, GALERIA = 3;

    final AbisalMIDlet midlet;
    volatile boolean corriendo = true;
    volatile boolean pausado;
    int estado = TITULO;
    int selTitulo;
    boolean hayGuardado;
    String[] opciones;
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
        // every screen of the game repaints the whole frame
        j2me.Pad.conservarPantalla(false);
        // draw each frame's meshes as one batch (one camera/light setup per frame)
        javax.microedition.m3g.Graphics3D.getInstance().setModoLote(true);
        try {
            Log.d("Juego.run: cargando opciones...");
            Opciones.cargar();
            Log.d("Juego.run: opciones OK, comprobando galeria...");
            String gal = midlet.getAppProperty("Abisal-Galeria");
            if (gal != null) {
                galeria = new Galeria(gal.trim(), true);
                estado = GALERIA;
                Log.d("Juego.run: galeria directa OK");
            } else {
                // pantalla de título: la cápsula flotando en el mar
                fondoTitulo = new Galeria("cápsula", false);
                Log.d("Juego.run: fondo de titulo creado (capsula en el mar)");
            }
            Log.d("Juego.run: preparando titulo...");
            prepararTitulo();
            Log.d("pantalla de titulo lista; " + Log.memoria());
            Log.d("Juego.run: cargando sonidos...");
            Sonido.cargar();
            Log.d("Juego.run: sonidos OK");
        } catch (Throwable e) {
            Log.d("ERROR al iniciar: " + e);
            e.printStackTrace();
        }
        Log.d("Juego.run: entrando en el bucle principal");
        long next = System.currentTimeMillis();
        while (corriendo) {
            if (!pausado) {
                try {
                    paso(g);
                    if (fallosMemoria > 0 && frame % 300 == 0) {
                        fallosMemoria--;
                    }
                } catch (OutOfMemoryError e) {
                    soltarDestino3D();
                    // out of memory in one frame: free what can be freed and keep playing;
                    // only if it keeps happening, save and go back to the title
                    fallosMemoria++;
                    // the render caches are the big blocks: drop them all, they rebuild as drawn
                    javax.microedition.m3g.VertexBuffer.liberarTodo();
                    System.gc();
                    Log.d("ERROR memoria en " + nombreEstado() + " (" + fallosMemoria + "), caches liberadas; "
                            + Log.memoria());
                    if (fallosMemoria >= 4) {
                        volverAlTitulo();
                    }
                } catch (Throwable e) {
                    soltarDestino3D();
                    Log.d("ERROR en el bucle (" + nombreEstado() + "): " + e);
                    e.printStackTrace();
                    volverAlTitulo();
                }
                frame++;
            }
            // the display paces the game at exactly 30 fps (two refreshes per picture); this
            // only keeps the PC build near it. A slow frame is never "caught up" with a burst
            // of fast ones: that was the speed-up you could feel
            next += 30;
            long wait = next - System.currentTimeMillis();
            if (wait < 0) {
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

    boolean continuar;

    private int fallosMemoria;

    /** an error in the middle of a 3D frame leaves the target bound: release it */
    private static void soltarDestino3D() {
        try {
            javax.microedition.m3g.Graphics3D.getInstance().releaseTarget();
        } catch (Throwable e) {
            // nothing was bound
        }
    }

    /** after an error: back to a working title screen (its scene rebuilt if it was freed) */
    private void volverAlTitulo() {
        try {
            if (partida != null && estado == JUGANDO) {
                Guardado.guardar(partida);
            }
        } catch (Throwable e2) {
            Log.d("no se pudo guardar tras el error: " + e2);
        }
        partida = null;
        fallosMemoria = 0;
        fondoTitulo = null;  // PARCHE: forzar reconstruccion
        javax.microedition.m3g.VertexBuffer.liberarTodo();
        System.gc();
        try {
            fondoTitulo = new Galeria("cápsula", false);
            prepararTitulo();
        } catch (Throwable e3) {
            Log.d("ERROR al volver al titulo: " + e3);
            try { prepararTitulo(); } catch (Throwable e4) { }
        }
        estado = TITULO;
    }

    private void prepararTitulo() {
        hayGuardado = Guardado.existe();
        opciones = hayGuardado ? new String[] { "Continuar", "Nueva partida" }
                : new String[] { "Nueva partida" };
        selTitulo = 0;
        Log.d("titulo: " + (hayGuardado ? "hay partida guardada" : "sin partida guardada"));
    }

    private String nombreEstado() {
        String[] n = { "título", "cargando", "jugando", "galería" };
        return n[estado];
    }

    private void paso(Graphics g) {
        in.leerAnalogico();
        float dt = 1f / FPS;
        if (estado != JUGANDO) {
            Log.fase(estado == CARGANDO ? Log.F_CARGAR : Log.F_TITULO);
        }
        switch (estado) {
        case TITULO:
            if (fondoTitulo != null) { // la galería del título puede estar desactivada
                fondoTitulo.actualizar(in, dt);
                fondoTitulo.dibujar(g);
            }
            dibujarTitulo(g);
            if (in.pulso(Entrada.ABAJO)) {
                selTitulo = (selTitulo + 1) % opciones.length;
            }
            if (in.pulso(Entrada.ARRIBA)) {
                selTitulo = (selTitulo + opciones.length - 1) % opciones.length;
            }
            if (in.pulso(Entrada.X)) {
                String op = opciones[selTitulo];
                Log.d("menu: " + op);
                continuar = op.equals("Continuar");
                estado = CARGANDO;
            }
            break;
        case CARGANDO:
            // one frame with the loading text, then build the world
            g.setColor(0x041420);
            g.fillRect(0, 0, W, H);
            g.setColor(0x7fe8f4);
            g.drawString("Cargando el océano...", W / 2, H / 2 - 5, Graphics.TOP | Graphics.HCENTER);
            in.finFrame();
            flushGraphics();
            fondoTitulo = null;
            javax.microedition.m3g.VertexBuffer.liberarTodo(); // nothing of an earlier game stays
            System.gc();
            Log.d("cargando partida; " + Log.memoria());
            Sonido.pararTodo(); // the title's clicks must not leak into the game
            try {
                partida = new Partida(continuar);
                estado = JUGANDO;
            } catch (Throwable e) {
                Log.d("ERROR al cargar la partida: " + e);
                e.printStackTrace();
                partida = null;
                continuar = false;
                try {
                    partida = new Partida(false);
                    estado = JUGANDO;
                } catch (Throwable e2) {
                    Log.d("ERROR tambien en partida nueva: " + e2);
                    volverAlTitulo();
                }
            }
            Sonido.musicaEmpezar();
            return;
        case JUGANDO:
            long t0 = System.currentTimeMillis();
            Log.fase(Log.F_LOGICA);
            // woke up from sleep: stop in the pause menu (nothing runs on while you look)
            int r = j2me.Pad.reanudaciones();
            if (r != reanudacionesVistas) {
                reanudacionesVistas = r;
                partida.alReanudar();
            }
            partida.actualizar(in, dt);
            long t1 = System.currentTimeMillis();
            partida.dibujar(g);
            in.finFrame();
            long t2 = System.currentTimeMillis();
            Log.fase(Log.F_PRESENTAR);
            flushGraphics();
            partida.registrarTiempos(t1 - t0, System.currentTimeMillis() - t2);
            if (partida.salirAlTitulo) {
                partida = null;
                System.gc();
                fondoTitulo = new Galeria("cápsula", false);
                prepararTitulo();
                estado = TITULO;
                Log.d("de vuelta en el titulo; " + Log.memoria());
            }
            return;
        default:
            galeria.actualizar(in, dt);
            galeria.dibujar(g);
            if (in.pulso(Entrada.CIRCULO) && fondoTitulo != null) {
                galeria = null;
                prepararTitulo();
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
        g.drawString("Supervivencia en el océano de Talassa", W / 2, 54, Graphics.TOP | Graphics.HCENTER);
        String[] op = opciones;
        int y0 = H - 20 - op.length * 20;
        for (int i = 0; i < op.length; i++) {
            g.setColor(0x06141c);
            g.fillRect(W / 2 - 80, y0 + i * 20, 160, 16);
            g.setColor(i == selTitulo ? 0xffffff : 0x6a8a94);
            g.drawString((i == selTitulo ? "> " : "") + op[i], W / 2, y0 + 3 + i * 20, Graphics.TOP | Graphics.HCENTER);
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
