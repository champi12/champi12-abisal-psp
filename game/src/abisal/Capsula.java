package abisal;

import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Light;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * Inside the escape pod: the diver sits in the middle of the cabin and looks around.
 * Five stations on the walls, chosen by looking at them: the fabricator, the radio
 * (story messages, after repairing it), the medical dispenser (a first-aid kit every
 * 10 minutes), the storage locker and the ladder to the hatch. Entering refills the
 * oxygen and saves the game.
 */
public final class Capsula {
    static final int FABRICADOR = 0, RADIO = 1, BOTIQUIN = 2, TAQUILLA = 3, ESCOTILLA = 4;
    static final float[] RUMBO = { 0, 72, 144, 216, 288 };
    static final String[] NOMBRE = { "[] Fabricador", "[] Radio", "[] Dispensador medico", "[] Taquilla",
            "[] Subir por la escotilla (salir)" };
    static final float RECARGA_BOTIQUIN = 600;

    private final Mesh interior;
    private final Camera cam = new Camera();
    private final Background fondo = new Background();
    private final Light luz = new Light(), amb = new Light();
    private final Transform tc = new Transform(), t = new Transform();

    float yaw, pitch;
    /** the station looked at, or -1 */
    int mirando = -1;
    /** storage locker contents (same item indices as the backpack) */
    final int[] taquilla = new int[Inventario.N];
    static final int CAP_TAQUILLA = 60;
    boolean enTaquilla;
    int selTaq;
    /** a radio message on screen (index into Historia.RADIO), -1 none */
    int mensaje = -1;
    /** seconds until the dispenser has another kit */
    float botiquinT;

    Capsula(Fog fog) {
        interior = Modelos.interiorCapsula(null);
        cam.setPerspective(70, (float) Juego.W / Juego.H, 0.1f, 20);
        fondo.setColor(0x20262a);
        luz.setMode(Light.OMNI);
        luz.setColor(0xfff0d8);
        luz.setIntensity(1.1f);
        amb.setMode(Light.AMBIENT);
        amb.setColor(0xb8c4cc);
        amb.setIntensity(0.55f);
    }

    void entrar() {
        yaw = 0;
        pitch = -8;
        enTaquilla = false;
        mensaje = -1;
        Log.d("capsula: interior");
    }

    int totalTaquilla() {
        int n = 0;
        for (int i = 0; i < Inventario.N; i++) {
            n += taquilla[i];
        }
        return n;
    }

    /** frames until the game is saved (0 = done); the cabin shows "Guardando..." meanwhile */
    int guardarEn;
    private boolean guardadoOk = true;

    void actualizar(Partida p, Entrada in, float dt) {
        if (guardarEn > 0 && --guardarEn == 0) {
            guardadoOk = Guardado.guardar(p);
        }
        if (botiquinT > 0) {
            botiquinT -= dt;
        }
        if (mensaje >= 0) {
            if (in.pulso(Entrada.X) || in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.CUADRADO)) {
                mensaje = -1;
            }
            return;
        }
        if (enTaquilla) {
            actualizarTaquilla(p, in);
            return;
        }
        if (in.pulso(Entrada.CIRCULO)) {
            salir(p);
            return;
        }
        if (in.pulso(Entrada.TRIANGULO)) {
            p.abrirPda();
            return;
        }
        float lx = in.ax, ly = in.ay;
        if (Opciones.cruceta) {
            lx = (in.tiene(Entrada.DER) ? 0.8f : 0) - (in.tiene(Entrada.IZQ) ? 0.8f : 0);
            ly = (in.tiene(Entrada.ABAJO) ? 0.8f : 0) - (in.tiene(Entrada.ARRIBA) ? 0.8f : 0);
        } else {
            // with the stick, the d-pad jumps from station to station
            if (in.pulso(Entrada.DER)) {
                yaw = RUMBO[(estacionMasCercana() + 4) % 5];
            } else if (in.pulso(Entrada.IZQ)) {
                yaw = RUMBO[(estacionMasCercana() + 1) % 5];
            }
        }
        yaw -= lx * 100 * dt;
        pitch = Math.max(-50, Math.min(60, pitch - ly * 70 * dt));
        yaw = (yaw % 360 + 360) % 360;
        int e = estacionMasCercana();
        mirando = Math.abs(Peces.angulo(yaw - RUMBO[e])) < 28 && pitch < 40 ? e : -1;
        if (pitch > 45) {
            mirando = ESCOTILLA; // looking up at the hatch
        }
        if (in.pulso(Entrada.CUADRADO) || in.pulso(Entrada.X)) {
            usar(p, mirando);
        }
    }

    private int estacionMasCercana() {
        int mejor = 0;
        float md = 999;
        for (int i = 0; i < 5; i++) {
            float d = Math.abs(Peces.angulo(yaw - RUMBO[i]));
            if (d < md) {
                md = d;
                mejor = i;
            }
        }
        return mejor;
    }

    private void salir(Partida p) {
        p.estado = Partida.JUEGO;
        p.enCapsula = false;
        Sonido.tocar(Sonido.ESCOTILLA);
        Log.d("capsula: sales al agua");
    }

    private void usar(Partida p, int e) {
        Historia h = p.historia;
        switch (e) {
        case FABRICADOR:
            p.estado = Partida.FABRICADOR;
            p.selFab = 0;
            Sonido.tocar(Sonido.PDA);
            break;
        case RADIO:
            if (!h.radioReparada) {
                if (p.inv.cant[Inventario.KIT] > 0) {
                    p.inv.cant[Inventario.KIT]--;
                    h.radioReparada = true;
                    Sonido.tocar(Sonido.FABRICAR);
                    p.avisar("Radio reparada");
                    Log.d("historia: radio reparada");
                    if (h.etapa <= 1) {
                        h.etapa = 2;
                    }
                    mensaje = h.etapa;
                    h.radioOida = h.etapa;
                    p.avisar("Nueva mision: " + Historia.MISION[h.etapa]);
                } else {
                    mensaje = 1; // "no signal: build a repair kit"
                }
            } else {
                mensaje = Math.min(h.etapa, Historia.RADIO.length - 1);
                if (Historia.RADIO[mensaje].length == 0) {
                    mensaje = 1;
                }
                if (h.radioOida < h.etapa) {
                    h.radioOida = h.etapa;
                    p.avisar("Nueva mision: " + Historia.MISION[h.etapa]);
                }
                Sonido.tocar(Sonido.SONAR);
            }
            break;
        case BOTIQUIN:
            if (botiquinT > 0) {
                p.avisar("Dispensador recargando: " + (int) (botiquinT / 60 + 1) + " min");
            } else if (p.inv.meter(Inventario.BOTIQUIN)) {
                botiquinT = RECARGA_BOTIQUIN;
                p.avisar("+1 Botiquin (usalo desde la PDA)");
                Sonido.tocar(Sonido.RECOGER);
            } else {
                p.avisar("Mochila llena");
            }
            break;
        case TAQUILLA:
            enTaquilla = true;
            selTaq = 0;
            Sonido.tocar(Sonido.CLICK);
            break;
        case ESCOTILLA:
            salir(p);
            break;
        default:
            break;
        }
    }

    /** locker: up/down choose, [] store one, X take one, O back */
    private void actualizarTaquilla(Partida p, Entrada in) {
        int n = Inventario.N;
        if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
            enTaquilla = false;
            return;
        }
        if (in.pulso(Entrada.ABAJO)) {
            selTaq = (selTaq + 1) % n;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            selTaq = (selTaq + n - 1) % n;
        }
        if (in.pulso(Entrada.CUADRADO) || in.pulso(Entrada.DER)) {
            if (p.inv.cant[selTaq] > 0 && totalTaquilla() < CAP_TAQUILLA) {
                p.inv.cant[selTaq]--;
                taquilla[selTaq]++;
            }
        }
        if (in.pulso(Entrada.X) || in.pulso(Entrada.IZQ)) {
            if (taquilla[selTaq] > 0 && p.inv.meter(selTaq)) {
                taquilla[selTaq]--;
            }
        }
    }

    void dibujar(Partida p, Graphics g, Graphics3D g3d) {
        g3d.bindTarget(g, true, 0);
        g3d.clear(fondo);
        tc.setIdentity();
        tc.postTranslate(0, 1.15f, 0);
        tc.postRotate(yaw, 0, 1, 0);
        tc.postRotate(pitch, 1, 0, 0);
        g3d.setCamera(cam, tc);
        g3d.resetLights();
        t.setIdentity();
        t.postTranslate(0, 2.1f, 0);
        g3d.addLight(luz, t);
        g3d.addLight(amb, null);
        t.setIdentity();
        g3d.render(interior, t);
        g3d.releaseTarget();
        // HUD
        g.setColor(0xe8f4f8);
        g.fillRect(Juego.W / 2 - 1, Juego.H / 2 - 1, 3, 3);
        if (mensaje >= 0) {
            String[] r = Historia.RADIO[mensaje];
            p.panelTexto(g, r);
            return;
        }
        if (enTaquilla) {
            dibujarTaquilla(p, g);
            return;
        }
        if (mirando >= 0 && p.estado == Partida.INTERIOR) {
            String s = NOMBRE[mirando];
            if (mirando == RADIO && p.historia.radioNueva()) {
                s = "[] Radio - MENSAJE NUEVO";
            } else if (mirando == BOTIQUIN && botiquinT > 0) {
                s = "Dispensador medico (recargando)";
            }
            p.etiqueta(g, s);
        }
        g.setColor(0x9fc4cc);
        g.drawString(guardarEn > 0 ? "CAPSULA  Guardando..."
                : (guardadoOk ? "CAPSULA  O2 recargado, partida guardada" : "CAPSULA  No se pudo guardar"), 8, 6,
                Graphics.TOP | Graphics.LEFT);
        g.drawString("O salir   /\\ PDA   " + (Opciones.cruceta ? "cruceta mirar" : "<- -> estaciones"), Juego.W - 8,
                Juego.H - 14, Graphics.TOP | Graphics.RIGHT);
        g.setColor(0x7fe8f4);
        g.drawString("Mision: " + p.historia.mision(), 8, 18, Graphics.TOP | Graphics.LEFT);
    }

    private void dibujarTaquilla(Partida p, Graphics g) {
        p.panelCaja(g, 60, 30, 360, 212);
        g.setColor(0x7fe8f4);
        g.drawString("TAQUILLA            mochila  taquilla", 76, 40, Graphics.TOP | Graphics.LEFT);
        int n = Inventario.N, vis = 11;
        int ini = Math.max(0, Math.min(n - vis, selTaq - vis / 2));
        for (int k = 0; k < vis && ini + k < n; k++) {
            int i = ini + k;
            int y = 58 + k * 14;
            if (i == selTaq) {
                g.setColor(0x1d4a58);
                g.fillRect(70, y - 2, 340, 13);
            }
            boolean hay = p.inv.cant[i] > 0 || taquilla[i] > 0;
            g.setColor(hay ? 0xffffff : 0x6a8a94);
            g.drawString(Inventario.NOMBRE[i], 80, y, Graphics.TOP | Graphics.LEFT);
            g.drawString("x" + p.inv.cant[i], 310, y, Graphics.TOP | Graphics.RIGHT);
            g.drawString("x" + taquilla[i], 390, y, Graphics.TOP | Graphics.RIGHT);
        }
        g.setColor(0x9fc4cc);
        g.drawString("[] guardar   X sacar   O cerrar", 76, 222, Graphics.TOP | Graphics.LEFT);
        g.drawString(totalTaquilla() + "/" + CAP_TAQUILLA, 404, 222, Graphics.TOP | Graphics.RIGHT);
    }
}
