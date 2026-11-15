package abisal;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.m3g.Background;
import javax.microedition.m3g.Camera;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Light;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * One game in progress: the diver, survival meters, the world, the fish, the HUD and the
 * devlog overlay.
 */
public final class Partida {
    static final float FOV = 65;
    static final float FOCAL = (Juego.H / 2f) / (float) Math.tan(Math.toRadians(FOV / 2));

    // survival tuning
    static final float O2_MAX = 45;          // seconds of air in the basic tank
    static final float O2_RECARGA = 15;      // per second at the surface
    static final float HAMBRE = 0.06f;       // food points per second
    static final float SED = 0.08f;          // water points per second
    static final float VEL = 4.2f;           // swim speed, m/s
    static final float VEL_LATERAL = 2.6f;
    static final float VEL_VERTICAL = 3.0f;

    private final Graphics3D g3d = Graphics3D.getInstance();
    private final Camera cam = new Camera();
    private final Light sol = new Light();
    private final Light amb = new Light();
    private final Background fondo = new Background();
    final Fog fog = new Fog();
    private final Transform tc = new Transform();
    private final Transform tl = new Transform();
    private final Transform t = new Transform();

    final Mundo mundo;
    final Peces peces;
    final Inventario inv = new Inventario();
    private final Mesh capsula;
    private final Mesh superficie;

    // diver
    float px = 0, py = -2.5f, pz = 10;
    float vx, vy, vz;
    float yaw = 150, pitch = -4;
    float o2 = O2_MAX, salud = 100, comida = 75, agua = 85;
    float tiempo;
    int muertes;

    // view direction (updated each frame)
    float fx, fy, fz;
    float lejos = 70;

    // ui state
    static final int JUEGO = 0, PDA = 1, PAUSA = 2, MUERTO = 3;
    int estado = JUEGO;
    int selPda;
    int selPausa;
    boolean verDevlog;
    String aviso;
    float avisoT;
    int biomaActual = -1;
    String rotulo;
    float rotuloT;
    final int[] sel = new int[2];
    private final float[] col = new float[3];
    boolean salirAlTitulo;

    // particles (marine snow) relative to the camera
    static final int NP = 48;
    final float[] sx = new float[NP], sy = new float[NP], sz = new float[NP];

    // stats
    int fpsCuenta;
    long fpsT0 = System.currentTimeMillis();
    int fps;
    long msFrame;
    float logT;

    Partida() {
        long t0 = System.currentTimeMillis();
        Log.d("nueva partida: preparando escena; " + Log.memoria());
        cam.setPerspective(FOV, (float) Juego.W / Juego.H, 0.25f, 120);
        sol.setMode(Light.DIRECTIONAL);
        sol.setColor(0xfff4e0);
        amb.setMode(Light.AMBIENT);
        amb.setColor(0xa6c2cc);
        fondo.setColorClearEnable(false);
        fog.setMode(Fog.LINEAR);
        ajustarAgua();
        mundo = new Mundo(fog);
        capsula = Modelos.capsula(fog);
        superficie = crearSuperficie(fog);
        peces = new Peces(fog);
        Log.d("capsula, superficie y peces listos; " + Log.memoria());
        mundo.cargarTodo(px, pz);
        for (int i = 0; i < NP; i++) {
            sx[i] = (Terreno.azar(i, 1, 300) - 0.5f) * 24;
            sy[i] = (Terreno.azar(i, 2, 300) - 0.5f) * 14;
            sz[i] = (Terreno.azar(i, 3, 300) - 0.5f) * 24;
        }
        avisar("Dia 1. La estacion Halcyon ha desaparecido. Sobrevive.");
        Log.d("partida lista en " + (System.currentTimeMillis() - t0) + " ms");
    }

    /** the underside of the sea surface: a big translucent sheet that follows the camera */
    private static Mesh crearSuperficie(Fog fog) {
        MB b = new MB();
        b.scale = 1f / 64f;
        b.color = 0x9fe6f0;
        float s = 160;
        b.quad(-s, 0, -s, -s, 0, s, s, 0, s, s, 0, -s);
        return b.build(fog, 150, true, 0);
    }

    void avisar(String s) {
        aviso = s;
        avisoT = 4;
        Log.d("aviso: " + s);
    }

    // ------------------------------------------------------------------ update

    void actualizar(Entrada in, float dt) {
        long t0 = System.currentTimeMillis();
        if (in.pulso(Entrada.SELECT)) {
            verDevlog = !verDevlog;
            Log.d("panel devlog " + (verDevlog ? "abierto" : "cerrado"));
        }
        if (estado == PDA) {
            actualizarPda(in);
            return;
        }
        if (estado == PAUSA) {
            actualizarPausa(in);
            return;
        }
        if (estado == MUERTO) {
            if (in.pulso(Entrada.X)) {
                reaparecer();
            }
            return;
        }
        if (in.pulso(Entrada.START)) {
            estado = PAUSA;
            selPausa = 0;
            return;
        }
        if (in.pulso(Entrada.TRIANGULO)) {
            estado = PDA;
            selPda = 0;
            return;
        }
        tiempo += dt;
        // look
        float lx = in.ax;
        float ly = in.ay;
        yaw -= lx * 110 * dt;
        pitch -= ly * 80 * dt;
        if (pitch > 80) {
            pitch = 80;
        }
        if (pitch < -80) {
            pitch = -80;
        }
        float cy = (float) Math.cos(Math.toRadians(yaw));
        float syw = (float) Math.sin(Math.toRadians(yaw));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));
        fx = -syw * cp;
        fy = sp;
        fz = -cy * cp;
        // swim: X / up = forward along the view, circle / down = back, left/right = strafe,
        // R = up, L = down
        float ax = 0, ay = 0, az = 0;
        float adelante = (in.tiene(Entrada.X) || in.tiene(Entrada.ARRIBA) ? 1 : 0)
                - (in.tiene(Entrada.CIRCULO) || in.tiene(Entrada.ABAJO) ? 0.6f : 0);
        ax += fx * adelante * VEL;
        ay += fy * adelante * VEL;
        az += fz * adelante * VEL;
        float lado = (in.tiene(Entrada.DER) ? 1 : 0) - (in.tiene(Entrada.IZQ) ? 1 : 0);
        ax += cy * lado * VEL_LATERAL;
        az += -syw * lado * VEL_LATERAL;
        ay += ((in.tiene(Entrada.R) ? 1 : 0) - (in.tiene(Entrada.L) ? 1 : 0)) * VEL_VERTICAL;
        // water: velocity eases towards the target (inertia), then drifts to a stop
        float k = Math.min(1, dt * 2.5f);
        vx += (ax - vx) * k;
        vy += (ay - vy) * k;
        vz += (az - vz) * k;
        px += vx * dt;
        py += vy * dt;
        pz += vz * dt;
        // collisions: sea floor, the surface and the capsule hull
        float suelo = Terreno.altura(px, pz) + 0.9f;
        if (py < suelo) {
            py = suelo;
            if (vy < 0) {
                vy = 0;
            }
        }
        if (py > -0.35f) {
            py = -0.35f;
            if (vy > 0) {
                vy = 0;
            }
        }
        float cdx = px;
        float cdz = pz;
        float cd = (float) Math.sqrt(cdx * cdx + cdz * cdz);
        if (cd < 3.0f && py > -2.6f && cd > 0.01f) {
            px = cdx / cd * 3.0f;
            pz = cdz / cd * 3.0f;
        }
        col[0] = px;
        col[1] = py;
        col[2] = pz;
        mundo.empujar(col, 0.45f);
        px = col[0];
        pz = col[2];
        supervivencia(dt);
        // world streaming and life
        mundo.actualizar(px, pz);
        peces.actualizar(dt, px, py, pz);
        // biome banner
        int bio = Terreno.bioma(px, pz);
        if (bio != biomaActual) {
            biomaActual = bio;
            rotulo = Mundo.nombreBioma(bio);
            rotuloT = 3;
            Log.d("entrando en bioma: " + rotulo + " (prof " + (int) -py + " m)");
        }
        // interaction
        if (in.pulso(Entrada.CUADRADO)) {
            interactuar();
        }
        if (avisoT > 0) {
            avisoT -= dt;
        }
        if (rotuloT > 0) {
            rotuloT -= dt;
        }
        // periodic devlog line
        logT += dt;
        if (logT >= 10) {
            logT = 0;
            Log.d("estado: fps " + fps + ", pos " + (int) px + "," + (int) py + "," + (int) pz + ", "
                    + Mundo.nombreBioma(bio) + ", trozos " + mundo.nChunks + ", tris " + mundo.trisDibujados
                    + ", O2 " + (int) o2 + ", " + Log.memoria());
        }
        msFrame = System.currentTimeMillis() - t0;
    }

    private void supervivencia(float dt) {
        boolean enSuperficie = py > -1.0f;
        if (enSuperficie) {
            if (o2 < O2_MAX && o2 + O2_RECARGA * dt >= O2_MAX) {
                Log.d("oxigeno recargado en superficie");
            }
            o2 = Math.min(O2_MAX, o2 + O2_RECARGA * dt);
        } else {
            o2 -= dt;
            if (o2 < 10 && o2 + dt >= 10) {
                avisar("Oxigeno bajo. Sube a la superficie.");
            }
        }
        comida -= HAMBRE * dt;
        agua -= SED * dt;
        if (o2 <= 0) {
            o2 = 0;
            salud -= 12 * dt;
        }
        if (comida <= 0) {
            comida = 0;
            salud -= 0.5f * dt;
        }
        if (agua <= 0) {
            agua = 0;
            salud -= 0.7f * dt;
        }
        if (o2 > 0 && comida > 20 && agua > 20 && salud < 100) {
            salud = Math.min(100, salud + 0.5f * dt);
        }
        if (salud <= 0) {
            salud = 0;
            estado = MUERTO;
            muertes++;
            Log.d("el jugador murio (muerte " + muertes + ") en " + (int) px + "," + (int) py + "," + (int) pz);
        }
    }

    private void interactuar() {
        // the capsule hatch
        if (px * px + pz * pz < 5.5f * 5.5f && py > -4) {
            o2 = O2_MAX;
            avisar("Capsula: oxigeno recargado. (Fabricador: proxima version)");
            Sonido.tocar(Sonido.RECOGER);
            return;
        }
        int f = peces.alcanzable(px, py, pz);
        if (f >= 0) {
            if (inv.meter(Inventario.PEZ)) {
                peces.vivo[f] = false;
                avisar("Atrapaste un burbujin");
                Sonido.tocar(Sonido.RECOGER);
            } else {
                avisar("Inventario lleno");
            }
            return;
        }
        if (mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
            int tipo = mundo.chunks[sel[0]].tipo[sel[1]];
            int item = Mundo.DA[tipo];
            if (inv.meter(item)) {
                mundo.recoger(sel[0], sel[1]);
                avisar("+1 " + Inventario.NOMBRE[item]);
                Sonido.tocar(Sonido.RECOGER);
            } else {
                avisar("Inventario lleno");
            }
        }
    }

    private void reaparecer() {
        px = 0;
        py = -2.5f;
        pz = 9;
        vx = vy = vz = 0;
        o2 = O2_MAX;
        salud = 60;
        comida = Math.max(comida, 30);
        agua = Math.max(agua, 30);
        // lose half of what was carried
        for (int i = 0; i < Inventario.N; i++) {
            inv.cant[i] /= 2;
        }
        estado = JUEGO;
        avisar("Despiertas junto a la capsula. Perdiste parte del inventario.");
        Log.d("reaparicion en la capsula");
    }

    private void actualizarPda(Entrada in) {
        if (in.pulso(Entrada.TRIANGULO) || in.pulso(Entrada.CIRCULO)) {
            estado = JUEGO;
            return;
        }
        if (in.pulso(Entrada.ABAJO)) {
            selPda = (selPda + 1) % Inventario.N;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            selPda = (selPda + Inventario.N - 1) % Inventario.N;
        }
        if (in.pulso(Entrada.X) && selPda == Inventario.PEZ && inv.cant[Inventario.PEZ] > 0) {
            inv.cant[Inventario.PEZ]--;
            comida = Math.min(100, comida + 14);
            agua = Math.min(100, agua + 4);
            avisar("Comiste un burbujin crudo (+14 comida, +4 agua)");
        }
    }

    private void actualizarPausa(Entrada in) {
        if (in.pulso(Entrada.START) || in.pulso(Entrada.CIRCULO)) {
            estado = JUEGO;
            return;
        }
        if (in.pulso(Entrada.ABAJO) || in.pulso(Entrada.ARRIBA)) {
            selPausa = 1 - selPausa;
        }
        if (in.pulso(Entrada.X)) {
            if (selPausa == 0) {
                estado = JUEGO;
            } else {
                salirAlTitulo = true;
                Log.d("volviendo al titulo");
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    /** water colour and visibility from the depth of the camera */
    private int colorAgua;

    private void ajustarAgua() {
        float prof = -py;
        float tt = Math.min(1, prof / 260f);
        colorAgua = Terreno.mezcla(0x2a8fb0, 0x041420, (float) Math.sqrt(tt));
        lejos = 72 - 44 * tt;
        fog.setColor(colorAgua);
        fog.setLinear(lejos * 0.15f, lejos);
        float luz = 1 - 0.85f * tt;
        sol.setIntensity(0.5f * luz);
        amb.setIntensity(0.25f + 0.47f * luz);
    }

    void dibujar(Graphics g) {
        ajustarAgua();
        // background: the fog colour, a little lighter towards the surface when looking up
        int arriba = Terreno.mezcla(colorAgua, 0x9fe6f0, py > -40 ? 0.35f * (1 + py / 40) : 0);
        int horizonte = Juego.H / 2 + (int) (Math.tan(Math.toRadians(pitch)) * FOCAL);
        for (int y = 0; y < Juego.H; y += 8) {
            float f = (horizonte - y) / 160f;
            g.setColor(Terreno.mezcla(colorAgua, arriba, f));
            g.fillRect(0, y, Juego.W, 8);
        }
        g3d.bindTarget(g, true, 0);
        g3d.clear(fondo);
        cam.setPerspective(FOV, (float) Juego.W / Juego.H, 0.25f, lejos + 10);
        tc.setIdentity();
        tc.postTranslate(px, py, pz);
        tc.postRotate(yaw, 0, 1, 0);
        tc.postRotate(pitch, 1, 0, 0);
        g3d.setCamera(cam, tc);
        g3d.resetLights();
        tl.setIdentity();
        tl.postRotate(-65, 1, 0, 0);
        tl.postRotate(25, 0, 1, 0);
        g3d.addLight(sol, tl);
        g3d.addLight(amb, null);
        float hx = fx;
        float hz = fz;
        float hl = (float) Math.sqrt(hx * hx + hz * hz);
        if (hl > 0.01f) {
            hx /= hl;
            hz /= hl;
        }
        mundo.dibujar(g3d, px, py, pz, hx, hz, lejos);
        // the capsule floats at the surface, bobbing gently
        if (px * px + pz * pz < (lejos + 10) * (lejos + 10)) {
            t.setIdentity();
            t.postTranslate(0, (float) Math.sin(tiempo * 0.8f) * 0.12f + 0.3f, 0);
            t.postRotate((float) Math.sin(tiempo * 0.5f) * 2, 1, 0, 0);
            g3d.render(capsula, t);
        }
        peces.dibujar(g3d, px, pz, hx, hz, Math.min(lejos, 40));
        if (py > -lejos) {
            t.setIdentity();
            t.postTranslate(px, 0, pz);
            g3d.render(superficie, t);
        }
        g3d.releaseTarget();
        dibujarNieve(g);
        dibujarHud(g);
        if (estado == PDA) {
            dibujarPda(g);
        } else if (estado == PAUSA) {
            dibujarPausa(g);
        } else if (estado == MUERTO) {
            dibujarMuerte(g);
        }
        if (verDevlog) {
            dibujarDevlog(g);
        }
        fpsCuenta++;
        long ahora = System.currentTimeMillis();
        if (ahora - fpsT0 >= 1000) {
            fps = (int) (fpsCuenta * 1000 / (ahora - fpsT0));
            fpsCuenta = 0;
            fpsT0 = ahora;
        }
    }

    /** floating particles that make the water feel thick (projected by hand, very cheap) */
    private void dibujarNieve(Graphics g) {
        float cyw = (float) Math.cos(Math.toRadians(yaw));
        float syw = (float) Math.sin(Math.toRadians(yaw));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));
        int c = Terreno.mezcla(colorAgua, 0xffffff, 0.55f);
        g.setColor(c);
        for (int i = 0; i < NP; i++) {
            // particles live in a box that wraps around the camera
            float ox = envolver(sx[i] - px % 24, 24);
            float oy = envolver(sy[i] - py % 14 + (tiempo * 0.15f) % 14, 14);
            float oz = envolver(sz[i] - pz % 24, 24);
            float x1 = ox * cyw - oz * syw;
            float z1 = ox * syw + oz * cyw;
            float y2 = oy * cp + z1 * sp;
            float z2 = -oy * sp + z1 * cp;
            if (z2 > -0.5f) {
                continue;
            }
            int sxp = (int) (Juego.W / 2 + x1 / -z2 * FOCAL);
            int syp = (int) (Juego.H / 2 - y2 / -z2 * FOCAL);
            int tam = z2 > -5 ? 2 : 1;
            g.fillRect(sxp, syp, tam, tam);
        }
    }

    private static float envolver(float v, float s) {
        v = v % s;
        if (v > s / 2) {
            v -= s;
        }
        if (v < -s / 2) {
            v += s;
        }
        return v;
    }

    private void barra(Graphics g, int x, int y, String et, float v, float max, int color) {
        g.setColor(0x0b1d26);
        g.fillRect(x, y, 92, 9);
        g.setColor(color);
        int w = (int) (88 * Math.max(0, v) / max);
        g.fillRect(x + 2, y + 2, w, 5);
        g.setColor(0xe8f4f8);
        g.drawString(et, x + 96, y - 1, Graphics.TOP | Graphics.LEFT);
    }

    private void dibujarHud(Graphics g) {
        // survival meters, bottom left
        int y0 = Juego.H - 52;
        boolean parpadeo = o2 < 10 && ((int) (tiempo * 4) & 1) == 0;
        barra(g, 8, y0, "O2 " + (int) o2 + "s", o2, O2_MAX, parpadeo ? 0xff5040 : 0x4fd8ff);
        barra(g, 8, y0 + 12, "Salud", salud, 100, 0xff5a5a);
        barra(g, 8, y0 + 24, "Comida", comida, 100, 0xffa040);
        barra(g, 8, y0 + 36, "Agua", agua, 100, 0x5aa0ff);
        // depth and heading, top centre
        int prof = (int) -py;
        String rumbo = rumbo();
        g.setColor(0x0b1d26);
        g.fillRect(Juego.W / 2 - 56, 4, 112, 14);
        g.setColor(0xe8f4f8);
        g.drawString(rumbo + "   " + prof + " m", Juego.W / 2, 6, Graphics.TOP | Graphics.HCENTER);
        // inventory count, bottom right
        g.drawString("Mochila " + inv.total() + "/" + Inventario.CAPACIDAD, Juego.W - 8, Juego.H - 14,
                Graphics.TOP | Graphics.RIGHT);
        // interaction prompt
        String accion = accionDisponible();
        if (accion != null && estado == JUEGO) {
            g.setColor(0x0b1d26);
            int w = accion.length() * 6 + 16;
            g.fillRect(Juego.W / 2 - w / 2, Juego.H / 2 + 20, w, 14);
            g.setColor(0xffffff);
            g.drawString(accion, Juego.W / 2, Juego.H / 2 + 22, Graphics.TOP | Graphics.HCENTER);
        }
        // crosshair
        g.setColor(0xe8f4f8);
        g.fillRect(Juego.W / 2 - 1, Juego.H / 2 - 1, 3, 3);
        if (avisoT > 0 && aviso != null) {
            g.setColor(0xffffff);
            g.drawString(aviso, Juego.W / 2, 26, Graphics.TOP | Graphics.HCENTER);
        }
        if (rotuloT > 0 && rotulo != null) {
            g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
            g.setColor(0xe8f4f8);
            g.drawString(rotulo, Juego.W / 2, 60, Graphics.TOP | Graphics.HCENTER);
            g.setFont(Font.getDefaultFont());
        }
    }

    private String rumbo() {
        float a = yaw % 360;
        if (a < 0) {
            a += 360;
        }
        // yaw 0 looks towards -Z = north; yaw grows turning left (west)
        String[] n = { "N", "NO", "O", "SO", "S", "SE", "E", "NE" };
        return n[(int) ((a + 22.5f) / 45) % 8];
    }

    private String accionDisponible() {
        if (px * px + pz * pz < 5.5f * 5.5f && py > -4) {
            return "[] Capsula: recargar oxigeno";
        }
        if (peces.alcanzable(px, py, pz) >= 0) {
            return "[] Atrapar burbujin";
        }
        if (mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
            int item = Mundo.DA[mundo.chunks[sel[0]].tipo[sel[1]]];
            return "[] Recoger " + Inventario.NOMBRE[item];
        }
        return null;
    }

    private void panel(Graphics g, int x, int y, int w, int h) {
        g.setColor(0x06141c);
        g.fillRect(x, y, w, h);
        g.setColor(0x2fb3c0);
        g.drawRect(x, y, w - 1, h - 1);
    }

    private void dibujarPda(Graphics g) {
        panel(g, 80, 40, 320, 190);
        g.setColor(0x7fe8f4);
        g.drawString("PDA - INVENTARIO", 96, 50, Graphics.TOP | Graphics.LEFT);
        for (int i = 0; i < Inventario.N; i++) {
            int y = 74 + i * 18;
            if (i == selPda) {
                g.setColor(0x1d4a58);
                g.fillRect(90, y - 3, 300, 16);
            }
            g.setColor(inv.cant[i] > 0 ? 0xffffff : 0x6a8a94);
            g.drawString(Inventario.NOMBRE[i], 100, y, Graphics.TOP | Graphics.LEFT);
            g.drawString("x" + inv.cant[i], 380, y, Graphics.TOP | Graphics.RIGHT);
        }
        g.setColor(0x9fc4cc);
        String ayuda = selPda == Inventario.PEZ ? "X comer   " : "";
        g.drawString(ayuda + "/\\ o O cerrar", 96, 208, Graphics.TOP | Graphics.LEFT);
        g.drawString("Mochila " + inv.total() + "/" + Inventario.CAPACIDAD, 384, 208, Graphics.TOP | Graphics.RIGHT);
    }

    private void dibujarPausa(Graphics g) {
        panel(g, 150, 80, 180, 100);
        g.setColor(0x7fe8f4);
        g.drawString("PAUSA", Juego.W / 2, 92, Graphics.TOP | Graphics.HCENTER);
        String[] op = { "Continuar", "Salir al titulo" };
        for (int i = 0; i < 2; i++) {
            g.setColor(i == selPausa ? 0xffffff : 0x6a8a94);
            g.drawString((i == selPausa ? "> " : "  ") + op[i], Juego.W / 2, 118 + i * 18,
                    Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void dibujarMuerte(Graphics g) {
        g.setColor(0x200606);
        g.fillRect(0, 100, Juego.W, 70);
        g.setColor(0xff8a7a);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("HAS PERDIDO EL CONOCIMIENTO", Juego.W / 2, 110, Graphics.TOP | Graphics.HCENTER);
        g.setFont(Font.getDefaultFont());
        g.setColor(0xffffff);
        g.drawString("Pulsa X para despertar en la capsula", Juego.W / 2, 145, Graphics.TOP | Graphics.HCENTER);
    }

    private void dibujarDevlog(Graphics g) {
        panel(g, Juego.W - 186, 22, 180, 112);
        g.setColor(0x9fffa0);
        int x = Juego.W - 180;
        int y = 28;
        g.drawString("DEVLOG  " + fps + " fps  " + msFrame + " ms", x, y, Graphics.TOP | Graphics.LEFT);
        g.drawString("trozos " + mundo.nChunks + " vis " + mundo.chunksDibujados, x, y + 12,
                Graphics.TOP | Graphics.LEFT);
        g.drawString("tris terreno " + mundo.trisDibujados, x, y + 24, Graphics.TOP | Graphics.LEFT);
        g.drawString("objetos " + mundo.propsDibujados + " peces " + peces.dibujados, x, y + 36,
                Graphics.TOP | Graphics.LEFT);
        g.drawString("pos " + (int) px + " " + (int) py + " " + (int) pz, x, y + 48, Graphics.TOP | Graphics.LEFT);
        g.drawString("vista " + (int) lejos + " m", x, y + 60, Graphics.TOP | Graphics.LEFT);
        Runtime r = Runtime.getRuntime();
        g.drawString("heap " + (r.totalMemory() - r.freeMemory()) / 1024 + "/" + r.totalMemory() / 1024 + " KB", x,
                y + 72, Graphics.TOP | Graphics.LEFT);
        int sis = j2me.Pad.memLibre();
        g.drawString("sistema libre " + sis + " KB", x, y + 84, Graphics.TOP | Graphics.LEFT);
    }
}
