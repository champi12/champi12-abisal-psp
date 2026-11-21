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
            menu = -1;
            Sonido.tocar(Sonido.CLICK);
            break;
        case ESCOTILLA:
            salir(p);
            break;
        default:
            break;
        }
    }

    /** locker list: every item in the backpack or in the locker */
    private final int[] lista = new int[Inventario.N];
    private int nLista;
    /** the action menu over the selected item (-1 closed) */
    private int menu = -1, nMenu;
    private final String[] menuOp = new String[5];
    private final int[] menuAcc = new int[5];
    private static final int GUARDAR1 = 0, GUARDAR_TODO = 1, SACAR1 = 2, SACAR_TODO = 3, CANCELAR = 4;

    private void listar(Partida p) {
        nLista = 0;
        for (int i = 0; i < Inventario.N; i++) {
            if (p.inv.cant[i] > 0 || taquilla[i] > 0) {
                lista[nLista++] = i;
            }
        }
        if (selTaq >= nLista) {
            selTaq = Math.max(0, nLista - 1);
        }
    }

    private void opcion(String s, int a) {
        menuOp[nMenu] = s;
        menuAcc[nMenu++] = a;
    }

    /** locker: up/down choose, X opens a small menu (store, take, all), O back */
    private void actualizarTaquilla(Partida p, Entrada in) {
        listar(p);
        if (menu >= 0) {
            if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
                menu = -1;
                return;
            }
            if (in.pulso(Entrada.ABAJO)) {
                menu = (menu + 1) % nMenu;
            }
            if (in.pulso(Entrada.ARRIBA)) {
                menu = (menu + nMenu - 1) % nMenu;
            }
            if (in.pulso(Entrada.X)) {
                mover(p, lista[selTaq], menuAcc[menu]);
                menu = -1;
            }
            return;
        }
        if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
            enTaquilla = false;
            return;
        }
        if (nLista > 0 && in.pulso(Entrada.ABAJO)) {
            selTaq = (selTaq + 1) % nLista;
        }
        if (nLista > 0 && in.pulso(Entrada.ARRIBA)) {
            selTaq = (selTaq + nLista - 1) % nLista;
        }
        if (nLista > 0 && in.pulso(Entrada.X)) {
            int it = lista[selTaq];
            nMenu = 0;
            if (p.inv.cant[it] > 0) {
                opcion("Guardar 1", GUARDAR1);
                if (p.inv.cant[it] > 1) {
                    opcion("Guardar todo (" + p.inv.cant[it] + ")", GUARDAR_TODO);
                }
            }
            if (taquilla[it] > 0) {
                opcion("Sacar 1", SACAR1);
                if (taquilla[it] > 1) {
                    opcion("Sacar todo (" + taquilla[it] + ")", SACAR_TODO);
                }
            }
            opcion("Cancelar", CANCELAR);
            menu = 0;
            Sonido.tocar(Sonido.CLICK);
        }
    }

    private void mover(Partida p, int it, int acc) {
        int n = 0;
        if (acc == GUARDAR1 || acc == GUARDAR_TODO) {
            int max = acc == GUARDAR1 ? 1 : p.inv.cant[it];
            while (n < max && p.inv.cant[it] > 0 && totalTaquilla() < CAP_TAQUILLA) {
                p.inv.cant[it]--;
                taquilla[it]++;
                n++;
            }
            if (n < max) {
                p.avisar("Taquilla llena (" + CAP_TAQUILLA + ")");
            }
        } else if (acc == SACAR1 || acc == SACAR_TODO) {
            int max = acc == SACAR1 ? 1 : taquilla[it];
            while (n < max && taquilla[it] > 0 && p.inv.meter(it)) {
                taquilla[it]--;
                n++;
            }
            if (n < max) {
                p.avisar("Mochila llena");
            }
        }
        if (n > 0) {
            Sonido.tocar(Sonido.RECOGER);
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
        int vis = 11;
        int ini = Math.max(0, Math.min(nLista - vis, selTaq - vis / 2));
        int ySel = 58;
        for (int k = 0; k < vis && ini + k < nLista; k++) {
            int i = lista[ini + k];
            int y = 58 + k * 14;
            if (ini + k == selTaq) {
                g.setColor(0x1d4a58);
                g.fillRect(70, y - 2, 340, 13);
                ySel = y;
            }
            g.setColor(0xffffff);
            g.drawString(Inventario.NOMBRE[i], 80, y, Graphics.TOP | Graphics.LEFT);
            g.drawString("x" + p.inv.cant[i], 310, y, Graphics.TOP | Graphics.RIGHT);
            g.drawString("x" + taquilla[i], 390, y, Graphics.TOP | Graphics.RIGHT);
        }
        if (nLista == 0) {
            g.setColor(0x6a8a94);
            g.drawString("Nada que guardar", 80, 58, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0x9fc4cc);
        g.drawString("X opciones   O cerrar", 76, 222, Graphics.TOP | Graphics.LEFT);
        g.drawString(totalTaquilla() + "/" + CAP_TAQUILLA, 404, 222, Graphics.TOP | Graphics.RIGHT);
        if (menu >= 0) {
            int mx = 250, my = Math.min(ySel + 12, 214 - nMenu * 13), mw = 150;
            g.setColor(0x06141c);
            g.fillRect(mx, my, mw, nMenu * 13 + 6);
            g.setColor(0x2fb3c0);
            g.drawRect(mx, my, mw - 1, nMenu * 13 + 5);
            for (int k = 0; k < nMenu; k++) {
                int y = my + 4 + k * 13;
                if (k == menu) {
                    g.setColor(0x1d6a78);
                    g.fillRect(mx + 2, y - 2, mw - 4, 12);
                }
                g.setColor(k == menu ? 0xffffff : 0xb8d4dc);
                g.drawString(menuOp[k], mx + 8, y, Graphics.TOP | Graphics.LEFT);
            }
        }
    }
}
