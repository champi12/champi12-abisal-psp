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
 * Model viewer: shows one model at a time under water, slowly turning. Used to review
 * every graphic before it goes into the game (left/right changes model).
 */
public final class Galeria {
    private final Graphics3D g3d = Graphics3D.getInstance();
    private final Camera cam = new Camera();
    private final Light sol = new Light();
    private final Light amb = new Light();
    private final Background fondo = new Background();
    private final Fog fog = new Fog();
    private final Transform t = new Transform();
    private final Transform tc = new Transform();
    private final Transform tl = new Transform();
    private int indice;
    private Mesh modelo;
    private Mesh suelo;
    private float giro = 35;
    private float dist = 9;
    private float alto = 1.5f;

    private final boolean etiqueta;

    Galeria(String inicial, boolean etiqueta) {
        this.etiqueta = etiqueta;
        cam.setPerspective(60, (float) Juego.W / Juego.H, 0.3f, 200);
        sol.setMode(Light.DIRECTIONAL);
        sol.setColor(0xfff4e0);
        sol.setIntensity(0.5f);
        amb.setMode(Light.AMBIENT);
        amb.setColor(0xa6c2cc);
        amb.setIntensity(0.72f);
        fondo.setColorClearEnable(false);
        fog.setMode(Fog.LINEAR);
        fog.setLinear(6, 60);
        fog.setColor(0x1e6f8c);
        if (inicial != null) {
            for (int i = 0; i < Modelos.NOMBRES.length; i++) {
                if (Modelos.NOMBRES[i].equals(inicial)) {
                    indice = i;
                }
            }
        }
        cargar();
    }

    private void cargar() {
        long t0 = System.currentTimeMillis();
        modelo = Modelos.crear(Modelos.NOMBRES[indice], fog);
        Log.d("galeria: modelo " + Modelos.NOMBRES[indice] + " creado en " + (System.currentTimeMillis() - t0)
                + " ms");
        dist = Modelos.DIST[indice];
        // keep big models (leviathans, pillars) out of the fog
        fog.setLinear(Math.max(6, dist * 0.6f), Math.max(60, dist * 2.2f));
        cam.setPerspective(60, (float) Juego.W / Juego.H, Math.max(0.3f, dist / 60), Math.max(200, dist * 4));
        alto = 0;
        if (suelo == null) {
            suelo = Modelos.suelo(fog);
        }
    }

    private Depredadores dep;
    private float reloj;

    void actualizar(Entrada in, float dt) {
        if (!etiqueta) {
            giro += 12 * dt;
            return;
        }
        if (in.pulso(Entrada.DER)) {
            indice = (indice + 1) % Modelos.NOMBRES.length;
            cargar();
        }
        if (in.pulso(Entrada.IZQ)) {
            indice = (indice + Modelos.NOMBRES.length - 1) % Modelos.NOMBRES.length;
            cargar();
        }
        giro += 20 * dt + in.ax * 90 * dt;
        alto += -in.ay * 4 * dt;
        if (in.tiene(Entrada.L)) {
            dist += 6 * dt;
        }
        if (in.tiene(Entrada.R)) {
            dist = Math.max(2, dist - 6 * dt);
        }
    }

    void dibujar(Graphics g) {
        // vertical water gradient: bright near the surface, deep blue below
        for (int y = 0; y < Juego.H; y += 8) {
            float f = y / (float) Juego.H;
            int r = (int) (40 - 30 * f);
            int gg = (int) (140 - 80 * f);
            int b = (int) (170 - 60 * f);
            g.setColor(r, gg, b);
            g.fillRect(0, y, Juego.W, 8);
        }
        g3d.bindTarget(g, true, 0);
        g3d.clear(fondo);
        tc.setIdentity();
        tc.postRotate(giro, 0, 1, 0);
        float base = Modelos.NOMBRES[indice].startsWith("fondo") ? Terreno.altura(0, 0) : 0;
        tc.postTranslate(0, base + Modelos.ALTO[indice] + alto, 0);
        tc.postRotate(-14, 1, 0, 0);
        tc.postTranslate(0, 0, dist);
        g3d.setCamera(cam, tc);
        g3d.resetLights();
        tl.setIdentity();
        tl.postRotate(-60, 1, 0, 0);
        tl.postRotate(20, 0, 1, 0);
        g3d.addLight(sol, tl);
        g3d.addLight(amb, null);
        t.setIdentity();
        if (Modelos.SUELO[indice]) {
            g3d.render(suelo, t);
        }
        String nom = Modelos.NOMBRES[indice];
        int anim = nom.equals("acorazado") ? 2 : (nom.equals("dientesierra") ? 0 : (nom.equals("anguila_brasa") ? 1 : -1));
        if (anim >= 0) {
            // jointed creatures: animated from their parts
            if (dep == null) {
                dep = new Depredadores(fog);
            }
            reloj += 1 / 30f;
            dep.dibujarMuestra(g3d, t, anim, reloj);
        } else if (modelo != null) {
            if (base != 0) {
                t.postTranslate(-60, 0, -60); // terrain meshes are local to their corner
            }
            g3d.render(modelo, t);
        }
        g3d.releaseTarget();
        if (!etiqueta) {
            return;
        }
        g.setColor(0xffffff);
        g.drawString("O volver   GALERIA  " + (indice + 1) + "/" + Modelos.NOMBRES.length + "  " + Modelos.NOMBRES[indice],
                8, 6, Graphics.TOP | Graphics.LEFT);
    }
}
