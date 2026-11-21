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
    public static final String VERSION = "0.16";

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
            Sonido.cargar();
            Opciones.cargar();
            String gal = midlet.getAppProperty("Abisal-Galeria");
            if (gal != null) {
                galeria = new Galeria(gal.trim(), true);
                estado = GALERIA;
            } else {
                fondoTitulo = new Galeria("capsula", false);
            }
            prepararTitulo();
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

    boolean continuar;

    private void prepararTitulo() {
        hayGuardado = Guardado.existe();
        opciones = hayGuardado ? new String[] { "Continuar", "Nueva partida", "Galeria de modelos" }
                : new String[] { "Nueva partida", "Galeria de modelos" };
        selTitulo = 0;
        Log.d("titulo: " + (hayGuardado ? "hay partida guardada" : "sin partida guardada"));
    }

    private String nombreEstado() {
        String[] n = { "titulo", "cargando", "jugando", "galeria" };
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
            fondoTitulo.actualizar(in, dt);
            fondoTitulo.dibujar(g);
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
                if (op.startsWith("Galeria")) {
                    galeria = new Galeria(null, true);
                    estado = GALERIA;
                } else {
                    continuar = op.equals("Continuar");
                    estado = CARGANDO;
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
            partida = new Partida(continuar);
            estado = JUGANDO;
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
                fondoTitulo = new Galeria("capsula", false);
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
        g.drawString("Supervivencia en el oceano de Talassa", W / 2, 54, Graphics.TOP | Graphics.HCENTER);
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
