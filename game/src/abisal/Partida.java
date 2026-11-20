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
    final Depredadores depredadores;
    /** red flash when hurt */
    float golpeT;
    String causaMuerte;
    /** test options (pause menu) */
    int zonaViaje = 1;
    boolean o2Infinito;
    final Inventario inv = new Inventario();
    private final Mesh capsula;
    private final Mesh superficie;
    private final Mesh oceano;
    /** head above the water: sky and an opaque sea instead of the underwater world */
    boolean fuera;

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
    static final int JUEGO = 0, PDA = 1, PAUSA = 2, MUERTO = 3, FABRICADOR = 4;
    int selFab;
    boolean linterna;
    private final Light foco = new Light();
    private final Transform tf = new Transform();
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

    // stats: per-frame time split, averaged over the 10 s log window
    long sumLogica, sum3d, sum2d, sumPresentar;
    int nTiempos;
    long ms3d, ms2d;

    void registrarTiempos(long logica, long presentar) {
        sumLogica += logica;
        sum3d += ms3d;
        sum2d += ms2d;
        sumPresentar += presentar;
        nTiempos++;
    }

    // the interaction prompt scans every prop: refresh it a few times a second only
    private String accionCache;
    private int accionTic;

    // stats
    int fpsCuenta;
    long fpsT0 = System.currentTimeMillis();
    int fps;
    long msFrame;
    float logT;

    Partida() {
        this(false);
    }

    float o2Max() {
        return inv.tiene[Inventario.H_TANQUE] ? 75 : O2_MAX;
    }

    Partida(boolean continuar) {
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
        oceano = crearOceano(fog);
        peces = new Peces(fog);
        depredadores = new Depredadores(fog);
        foco.setMode(Light.SPOT);
        foco.setColor(0xfff2d0);
        foco.setIntensity(1.6f);
        foco.setSpotAngle(28);
        foco.setSpotExponent(6);
        foco.setAttenuation(1, 0.02f, 0.0025f);
        Log.d("capsula, superficie y peces listos; " + Log.memoria());
        boolean cargada = continuar && Guardado.cargar(this);
        mundo.cargarTodo(px, pz);
        for (int i = 0; i < NP; i++) {
            sx[i] = (Terreno.azar(i, 1, 300) - 0.5f) * 24;
            sy[i] = (Terreno.azar(i, 2, 300) - 0.5f) * 14;
            sz[i] = (Terreno.azar(i, 3, 300) - 0.5f) * 24;
        }
        if (cargada) {
            avisar("Partida cargada. Dia " + (1 + (int) (tiempo / 1200)) + ".");
        } else {
            avisar("Dia 1. La estacion Halcyon ha desaparecido. Sobrevive.");
        }
        Log.d("partida lista en " + (System.currentTimeMillis() - t0) + " ms");
    }

    /** the underside of the sea surface: a big translucent sheet that follows the camera */
    private static Mesh crearSuperficie(Fog fog) {
        // seen from below: a bright translucent sheet broken into ripples of light
        MB b = new MB();
        b.scale = 1f / 64f;
        b.jitter = 18;
        int n = 8;
        float s = 160;
        float c = 2 * s / n;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float x0 = -s + i * c;
                float z0 = -s + j * c;
                b.color = ((i + j) & 1) == 0 ? 0xb8f2fa : 0x8fdcea;
                b.quad(x0, 0, z0, x0, 0, z0 + c, x0 + c, 0, z0 + c, x0 + c, 0, z0);
            }
        }
        return b.build(fog, 170, true, 0);
    }

    /** seen from above: the opaque open sea, hiding everything below it */
    private static Mesh crearOceano(Fog fog) {
        MB b = new MB();
        b.scale = 1f / 32f;
        b.jitter = 10;
        int n = 12;
        float s = 300;
        float c = 2 * s / n;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float x0 = -s + i * c;
                float z0 = -s + j * c;
                b.color = ((i * 7 + j * 3) % 3) == 0 ? 0x1f6c8c : 0x24789a;
                b.quad(x0, 0, z0, x0, 0, z0 + c, x0 + c, 0, z0 + c, x0 + c, 0, z0);
            }
        }
        return b.build(fog);
    }

    void avisar(String s) {
        aviso = s;
        avisoT = 4;
        Log.d("aviso: " + s);
    }

    // ------------------------------------------------------------------ update

    void actualizar(Entrada in, float dt) {
        long t0 = System.currentTimeMillis();
        if (in.pulso(Entrada.DEVLOG)) {
            verDevlog = !verDevlog;
            Log.d("panel devlog " + (verDevlog ? "abierto" : "cerrado"));
        }
        if (in.pulso(Entrada.LINTERNA) && estado == JUEGO) {
            if (inv.tiene[Inventario.H_LINTERNA]) {
                linterna = !linterna;
                Log.d("linterna " + (linterna ? "encendida" : "apagada"));
            } else {
                avisar("No tienes linterna. Fabricala en la capsula.");
            }
        }
        if (estado == FABRICADOR) {
            actualizarFabricador(in);
            return;
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
        // look: analog stick, or the d-pad in "cruceta" mode (for a broken stick)
        float lx = in.ax;
        float ly = in.ay;
        if (Opciones.cruceta) {
            lx = (in.tiene(Entrada.DER) ? 0.8f : 0) - (in.tiene(Entrada.IZQ) ? 0.8f : 0);
            ly = (in.tiene(Entrada.ABAJO) ? 0.8f : 0) - (in.tiene(Entrada.ARRIBA) ? 0.8f : 0);
        }
        yaw -= lx * 110 * dt;
        pitch -= ly * 80 * dt;
        if (pitch > 80) {
            pitch = 80;
        }
        if (pitch < -80) {
            pitch = -80;
        }
        float cy = j2me.FMath.cos(yaw * j2me.FMath.RAD);
        float syw = j2me.FMath.sin(yaw * j2me.FMath.RAD);
        float cp = j2me.FMath.cos(pitch * j2me.FMath.RAD);
        float sp = j2me.FMath.sin(pitch * j2me.FMath.RAD);
        fx = -syw * cp;
        fy = sp;
        fz = -cy * cp;
        // swim: X / up = forward along the view, circle / down = back, left/right = strafe,
        // R = up, L = down
        float ax = 0, ay = 0, az = 0;
        float aletas = inv.tiene[Inventario.H_ALETAS] ? 1.3f : 1f;
        boolean cr = Opciones.cruceta;
        float adelante = aletas * (in.tiene(Entrada.X) || (!cr && in.tiene(Entrada.ARRIBA)) ? 1 : 0)
                - (in.tiene(Entrada.CIRCULO) || (!cr && in.tiene(Entrada.ABAJO)) ? 0.6f : 0);
        ax += fx * adelante * VEL;
        ay += fy * adelante * VEL;
        az += fz * adelante * VEL;
        float lado = cr ? 0 : (in.tiene(Entrada.DER) ? 1 : 0) - (in.tiene(Entrada.IZQ) ? 1 : 0);
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
        // the surface: near it you float up with the head out of the water (+0.3 m);
        // holding L dives again
        if (py > -0.6f && !in.tiene(Entrada.L) && ay >= 0) {
            py += (0.3f - py) * Math.min(1, dt * 4);
            if (vy < 0) {
                vy = 0;
            }
        }
        if (py > 0.3f) {
            py = 0.3f;
            if (vy > 0) {
                vy = 0;
            }
        }
        boolean ahoraFuera = py > 0.02f;
        if (ahoraFuera != fuera) {
            fuera = ahoraFuera;
            Sonido.tocar(Sonido.BURBUJAS);
            Log.d(fuera ? "superficie: cabeza fuera del agua" : "superficie: bajo el agua");
        }
        float cdx = px;
        float cdz = pz;
        float cd = j2me.FMath.sqrt(cdx * cdx + cdz * cdz);
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
        depredadores.actualizar(dt, this);
        if (golpeT > 0) {
            golpeT -= dt;
        }
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
                    + ", objetos " + mundo.propsDibujados + ", peces " + peces.dibujados
                    + ", O2 " + (int) o2 + ", " + Log.memoria());
            if (nTiempos > 0) {
                Log.d("tiempos por frame (ms): logica " + sumLogica / nTiempos + ", 3D " + sum3d / nTiempos
                        + ", 2D " + sum2d / nTiempos + ", esperar GPU+vsync " + sumPresentar / nTiempos
                        + ", trozos construidos " + mundo.construidos);
            }
            sumLogica = sum3d = sum2d = sumPresentar = 0;
            nTiempos = 0;
        }
        msFrame = System.currentTimeMillis() - t0;
    }

    private void supervivencia(float dt) {
        boolean enSuperficie = py > -1.0f;
        if (enSuperficie) {
            if (o2 < o2Max() && o2 + O2_RECARGA * dt >= o2Max()) {
                Log.d("oxigeno recargado en superficie");
            }
            o2 = Math.min(o2Max(), o2 + O2_RECARGA * dt);
        } else {
            o2 -= o2Infinito ? 0 : dt;
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
            Log.d("el jugador murio (muerte " + muertes + ") en " + (int) px + "," + (int) py + "," + (int) pz
                    + (causaMuerte != null ? ": " + causaMuerte : ""));
        }
    }

    /** damage from a creature */
    void danar(float cant, String causa) {
        if (estado != JUEGO) {
            return;
        }
        salud -= cant;
        golpeT = 0.45f;
        causaMuerte = causa;
        avisar(causa + " (-" + (int) cant + ")");
        Sonido.tocar(Sonido.GOLPE);
    }

    private void interactuar() {
        // knife attack on a creature in front
        if (inv.tiene[Inventario.H_CUCHILLO] && depredadores.golpear(px, py, pz, fx, fy, fz)) {
            avisar("Cuchillazo!");
            Sonido.tocar(Sonido.GOLPE);
            return;
        }
        // the capsule hatch: air, save and the fabricator
        if (px * px + pz * pz < 5.5f * 5.5f && py > -4) {
            o2 = o2Max();
            boolean ok = Guardado.guardar(this);
            avisar(ok ? "Capsula: oxigeno recargado, partida guardada" : "Capsula: no se pudo guardar");
            Sonido.tocar(Sonido.RECOGER);
            estado = FABRICADOR;
            selFab = 0;
            Log.d("fabricador abierto");
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
            if (Mundo.CUCHILLO[tipo] && !inv.tiene[Inventario.H_CUCHILLO]) {
                avisar("Necesitas un cuchillo para cortar esto");
                return;
            }
            if (inv.meter(item)) {
                mundo.recoger(sel[0], sel[1]);
                String extra = "";
                if (tipo == Mundo.P_ALGA && inv.meter(Inventario.BULBO)) {
                    extra = ", +1 Bulbo de alga";
                }
                avisar("+1 " + Inventario.NOMBRE[item] + extra);
                Sonido.tocar(Sonido.RECOGER);
            } else {
                avisar("Inventario lleno");
            }
        }
    }

    private void reaparecer() {
        causaMuerte = null;
        px = 0;
        py = -2.5f;
        pz = 9;
        vx = vy = vz = 0;
        o2 = o2Max();
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
        if (in.pulso(Entrada.X) && inv.cant[selPda] > 0) {
            if (selPda == Inventario.PEZ) {
                inv.cant[selPda]--;
                comida = Math.min(100, comida + 14);
                agua = Math.min(100, agua + 4);
                avisar("Comiste un burbujin crudo (+14 comida, +4 agua)");
            } else if (selPda == Inventario.PEZ_COCIDO) {
                inv.cant[selPda]--;
                comida = Math.min(100, comida + 30);
                agua = Math.min(100, agua + 2);
                avisar("Comiste un burbujin cocido (+30 comida)");
            } else if (selPda == Inventario.AGUA) {
                inv.cant[selPda]--;
                agua = Math.min(100, agua + 30);
                avisar("Bebiste agua filtrada (+30 agua)");
            }
        }
    }

    /** test travel: jump to a point inside the given biome, a few metres above the floor */
    void viajar(int zona) {
        float r0 = zona == 0 ? 40 : Terreno.LIMITE[zona - 1];
        float r1 = Terreno.LIMITE[zona];
        for (int intento = 0; intento < 48; intento++) {
            float ang = (yaw + intento * 37) * j2me.FMath.RAD;
            float r = r0 + (r1 - r0) * (0.3f + 0.4f * ((intento * 7) % 10) / 10f);
            float x = -j2me.FMath.sin(ang) * r;
            float z = -j2me.FMath.cos(ang) * r;
            if (Terreno.bioma(x, z) == zona) {
                px = x;
                pz = z;
                py = Math.min(-4, Terreno.altura(x, z) + 4);
                vx = vy = vz = 0;
                long t0 = System.currentTimeMillis();
                mundo.cargarTodo(px, pz);
                Log.d("prueba: viaje a " + Mundo.nombreBioma(zona) + " (" + (int) px + "," + (int) py + "," + (int) pz
                        + ") en " + (System.currentTimeMillis() - t0) + " ms");
                avisar("Viaje de prueba: " + Mundo.nombreBioma(zona) + ", " + (int) -py + " m");
                return;
            }
        }
        avisar("No se encontro un punto en " + Mundo.nombreBioma(zona));
    }

    private void actualizarFabricador(Entrada in) {
        int n = Inventario.RECETAS.length;
        if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
            estado = JUEGO;
            return;
        }
        if (in.pulso(Entrada.ABAJO)) {
            selFab = (selFab + 1) % n;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            selFab = (selFab + n - 1) % n;
        }
        if (in.pulso(Entrada.X)) {
            Inventario.Receta r = Inventario.RECETAS[selFab];
            if (inv.fabricar(r)) {
                avisar("Fabricado: " + r.nombre);
                Sonido.tocar(Sonido.RECOGER);
                if (r.herramienta == Inventario.H_TANQUE) {
                    o2 = o2Max();
                }
                Log.d("fabricado: " + r.nombre);
            } else if (r.herramienta >= 0 && inv.tiene[r.herramienta]) {
                avisar("Ya tienes " + r.nombre);
            } else {
                avisar("Faltan materiales: " + Inventario.ingredientes(r));
            }
        }
    }

    private void actualizarPausa(Entrada in) {
        if (in.pulso(Entrada.START) || in.pulso(Entrada.CIRCULO)) {
            estado = JUEGO;
            return;
        }
        if (in.pulso(Entrada.ABAJO)) {
            selPausa = (selPausa + 1) % 6;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            selPausa = (selPausa + 5) % 6;
        }
        if (selPausa == 2 && (in.pulso(Entrada.DER) || in.pulso(Entrada.IZQ))) {
            zonaViaje = (zonaViaje + (in.pulso(Entrada.DER) ? 1 : 4)) % 5;
        }
        if (in.pulso(Entrada.X)) {
            if (selPausa == 0) {
                estado = JUEGO;
            } else if (selPausa == 1) {
                Opciones.cruceta = !Opciones.cruceta;
                Opciones.guardar();
            } else if (selPausa == 2) {
                viajar(zonaViaje);
                estado = JUEGO;
            } else if (selPausa == 3) {
                o2Infinito = !o2Infinito;
                Log.d("prueba: oxigeno infinito " + (o2Infinito ? "SI" : "NO"));
            } else if (selPausa == 4) {
                tiempo += CICLO / 8; // +3 hours
                Log.d("prueba: hora adelantada a " + reloj());
            } else {
                salirAlTitulo = true;
                Log.d("volviendo al titulo");
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    /** water colour and visibility from the depth of the camera */
    private int colorAgua;

    // ------------------------------------------------------------------ day and night

    /** a full day lasts 20 minutes; a new game starts in the morning */
    static final float CICLO = 1200;
    static final float INICIO = 0.06f * CICLO;

    /** fraction of the day: 0 = sunrise (06:00), 0.25 = noon, 0.5 = sunset, 0.75 = midnight */
    float horaDia() {
        float f = ((tiempo + INICIO) % CICLO) / CICLO;
        return f < 0 ? f + 1 : f;
    }

    /** daylight 0.08 (moonlit night) .. 1 (day), smooth through dusk and dawn */
    float luzDia() {
        float s = j2me.FMath.sin(horaDia() * 2 * j2me.FMath.PI);
        float l = (s + 0.12f) / 0.42f;
        l = l < 0 ? 0 : (l > 1 ? 1 : l);
        l = l * l * (3 - 2 * l);
        return 0.08f + 0.92f * l;
    }

    String reloj() {
        float h = (6 + horaDia() * 24) % 24;
        int hh = (int) h;
        int mm = (int) ((h - hh) * 60);
        return (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
    }

    private int nocheAvisada = -1;

    private void avisoNoche(float l) {
        int noche = l < 0.3f ? 1 : 0;
        if (noche != nocheAvisada) {
            if (nocheAvisada >= 0) {
                avisar(noche == 1 ? "Anochece. La linterna sera util." : "Amanece en Talassa.");
            }
            nocheAvisada = noche;
        }
    }

    private int cieloArriba, cieloHorizonte;

    /** sky colours for the current daylight: day, sunset glow, night */
    private void colorCielo(float l) {
        if (l >= 0.6f) {
            cieloArriba = 0x3d8fd0;
            cieloHorizonte = 0xc4e4f0;
        } else if (l >= 0.25f) {
            float k = (l - 0.25f) / 0.35f;
            cieloArriba = Terreno.mezcla(0x34427e, 0x3d8fd0, k);
            cieloHorizonte = Terreno.mezcla(0xf0a466, 0xc4e4f0, k);
        } else {
            float k = l / 0.25f;
            cieloArriba = Terreno.mezcla(0x040818, 0x34427e, k);
            cieloHorizonte = Terreno.mezcla(0x15233f, 0xf0a466, k);
        }
    }

    private void ajustarAgua() {
        float l = luzDia();
        avisoNoche(l);
        if (fuera) {
            // open air: haze in the colour of the horizon, long view, light of the hour
            colorCielo(l);
            colorAgua = cieloHorizonte;
            lejos = 280;
            fog.setColor(colorAgua);
            fog.setLinear(40, lejos);
            sol.setIntensity(0.9f * l);
            amb.setIntensity(0.2f + 0.35f * l);
            return;
        }
        float prof = -py;
        float tt = Math.min(1, prof / 260f);
        // deep water is dark navy, not black: the relief still reads and glowing flora
        // stands out (the flashlight lights it up properly)
        colorAgua = Terreno.mezcla(0x2a8fb0, 0x08304a, j2me.FMath.sqrt(tt));
        lejos = 72 - 34 * tt;
        // night: the shallows go dark navy and visibility drops (the deep is dark anyway)
        float noche = (1 - l) * (1 - 0.6f * tt);
        colorAgua = Terreno.mezcla(colorAgua, 0x030e1a, noche * 0.85f);
        lejos *= 1 - 0.3f * noche;
        fog.setColor(colorAgua);
        fog.setLinear(lejos * 0.15f, lejos);
        float luz = 1 - 0.75f * tt;
        sol.setIntensity((0.12f + 0.38f * luz) * l);
        amb.setIntensity((0.42f + 0.3f * luz) * (1 - 0.6f * noche));
    }

    void dibujar(Graphics g) {
        ajustarAgua();
        int horizonte = Juego.H / 2 + (int) (j2me.FMath.tan(pitch * j2me.FMath.RAD) * FOCAL);
        if (fuera) {
            // sky: zenith colour fading to the horizon colour (the sea covers the rest)
            for (int y = 0; y < Juego.H; y += 8) {
                float f = (horizonte - y) / 220f;
                g.setColor(Terreno.mezcla(cieloHorizonte, cieloArriba, f));
                g.fillRect(0, y, Juego.W, 8);
            }
            float l = luzDia();
            if (l < 0.35f) {
                dibujarEstrellas(g, horizonte, (0.35f - l) / 0.35f);
            }
        } else {
            // background: the fog colour, a little lighter towards the surface when looking up
            int arriba = Terreno.mezcla(colorAgua, 0x9fe6f0, py > -40 ? 0.35f * (1 + py / 40) : 0);
            for (int y = 0; y < Juego.H; y += 8) {
                float f = (horizonte - y) / 160f;
                g.setColor(Terreno.mezcla(colorAgua, arriba, f));
                g.fillRect(0, y, Juego.W, 8);
            }
        }
        long t3d = System.currentTimeMillis();
        Log.fase(Log.F_3D);
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
        if (linterna) {
            // the spot shines along -Z of its transform, like the camera
            g3d.addLight(foco, tc);
        }
        float hx = fx;
        float hz = fz;
        float hl = j2me.FMath.sqrt(hx * hx + hz * hz);
        if (hl > 0.01f) {
            hx /= hl;
            hz /= hl;
        }
        if (fuera) {
            // only the sea and what floats on it: nothing below the surface is visible
            mundo.trisDibujados = 0;
            mundo.propsDibujados = 0;
            peces.dibujados = 0;
            t.setIdentity();
            t.postTranslate(((int) (px / 50)) * 50f, 0, ((int) (pz / 50)) * 50f);
            g3d.render(oceano, t);
        } else {
            mundo.dibujar(g3d, px, py, pz, hx, hz, lejos);
        }
        // the capsule floats at the surface, bobbing gently
        if (px * px + pz * pz < (lejos + 10) * (lejos + 10)) {
            t.setIdentity();
            t.postTranslate(0, j2me.FMath.sin(tiempo * 0.8f) * 0.12f + 0.3f, 0);
            t.postRotate(j2me.FMath.sin(tiempo * 0.5f) * 2, 1, 0, 0);
            g3d.render(capsula, t);
        }
        if (!fuera) {
            peces.dibujar(g3d, px, pz, hx, hz, lejos);
            depredadores.dibujar(g3d, px, pz, hx, hz, lejos);
        }
        if (!fuera && py > -lejos) {
            t.setIdentity();
            t.postTranslate(px, 0, pz);
            g3d.render(superficie, t);
        }
        g3d.releaseTarget();
        long t2d = System.currentTimeMillis();
        ms3d = t2d - t3d;
        Log.fase(Log.F_2D);
        if (!fuera) {
            dibujarNieve(g);
        }
        dibujarHud(g);
        if (estado == FABRICADOR) {
            dibujarFabricador(g);
        } else if (estado == PDA) {
            dibujarPda(g);
        } else if (estado == PAUSA) {
            dibujarPausa(g);
        } else if (estado == MUERTO) {
            dibujarMuerte(g);
        }
        if (verDevlog) {
            dibujarDevlog(g);
        }
        ms2d = System.currentTimeMillis() - t2d;
        fpsCuenta++;
        long ahora = System.currentTimeMillis();
        if (ahora - fpsT0 >= 1000) {
            fps = (int) (fpsCuenta * 1000 / (ahora - fpsT0));
            fpsCuenta = 0;
            fpsT0 = ahora;
        }
    }

    /** stars above the horizon at night; they keep their place in the sky as you turn */
    private void dibujarEstrellas(Graphics g, int horizonte, float intensidad) {
        g.setColor(Terreno.mezcla(cieloArriba, 0xf4f8ff, intensidad));
        for (int i = 0; i < 60; i++) {
            float az = Terreno.azar(i, 7, 500) * 360;
            float el = 4 + Terreno.azar(i, 8, 500) * 70;
            float d = Peces.angulo(az - yaw);
            if (d < -45 || d > 45) {
                continue;
            }
            int sx = (int) (Juego.W / 2 + j2me.FMath.tan(d * j2me.FMath.RAD) * FOCAL);
            int sy = horizonte - (int) (j2me.FMath.tan(el * j2me.FMath.RAD) * FOCAL * 0.6f);
            if (sy < 0 || sy > horizonte) {
                continue;
            }
            int tam = Terreno.azar(i, 9, 500) > 0.8f ? 2 : 1;
            g.fillRect(sx, sy, tam, tam);
        }
    }

    /** floating particles that make the water feel thick (projected by hand, very cheap) */
    private void dibujarNieve(Graphics g) {
        float cyw = j2me.FMath.cos(yaw * j2me.FMath.RAD);
        float syw = j2me.FMath.sin(yaw * j2me.FMath.RAD);
        float cp = j2me.FMath.cos(pitch * j2me.FMath.RAD);
        float sp = j2me.FMath.sin(pitch * j2me.FMath.RAD);
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
        barra(g, 8, y0, "O2 " + (int) o2 + "s", o2, o2Max(), parpadeo ? 0xff5040 : 0x4fd8ff);
        barra(g, 8, y0 + 12, "Salud", salud, 100, 0xff5a5a);
        barra(g, 8, y0 + 24, "Comida", comida, 100, 0xffa040);
        barra(g, 8, y0 + 36, "Agua", agua, 100, 0x5aa0ff);
        // depth and heading, top centre
        int prof = (int) -py;
        String rumbo = rumbo();
        g.setColor(0x0b1d26);
        g.fillRect(Juego.W / 2 - 80, 4, 160, 14);
        g.setColor(0xe8f4f8);
        g.drawString(rumbo + "  " + prof + " m  " + reloj(), Juego.W / 2, 6, Graphics.TOP | Graphics.HCENTER);
        // inventory count, bottom right
        g.drawString("Mochila " + inv.total() + "/" + Inventario.CAPACIDAD, Juego.W - 8, Juego.H - 14,
                Graphics.TOP | Graphics.RIGHT);
        // interaction prompt
        if (--accionTic <= 0) {
            accionTic = 6;
            accionCache = accionDisponible();
        }
        String accion = accionCache;
        if (accion != null && estado == JUEGO) {
            g.setColor(0x0b1d26);
            int w = accion.length() * 6 + 16;
            g.fillRect(Juego.W / 2 - w / 2, Juego.H / 2 + 20, w, 14);
            g.setColor(0xffffff);
            g.drawString(accion, Juego.W / 2, Juego.H / 2 + 22, Graphics.TOP | Graphics.HCENTER);
        }
        if (golpeT > 0) {
            int w = (int) (golpeT * 30);
            g.setColor(0xc01810);
            g.fillRect(0, 0, Juego.W, w);
            g.fillRect(0, Juego.H - w, Juego.W, w);
            g.fillRect(0, 0, w, Juego.H);
            g.fillRect(Juego.W - w, 0, w, Juego.H);
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
            return "[] Capsula: oxigeno, guardar y fabricar";
        }
        if (inv.tiene[Inventario.H_CUCHILLO] && depredadores.alAlcance(px, py, pz, fx, fy, fz)) {
            return "[] Atacar con el cuchillo";
        }
        if (peces.alcanzable(px, py, pz) >= 0) {
            return "[] Atrapar burbujin";
        }
        if (mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
            int tipo = mundo.chunks[sel[0]].tipo[sel[1]];
            int item = Mundo.DA[tipo];
            if (Mundo.CUCHILLO[tipo] && !inv.tiene[Inventario.H_CUCHILLO]) {
                return "Necesitas un cuchillo";
            }
            return "[] " + (Mundo.CUCHILLO[tipo] ? "Cortar " : "Recoger ") + Inventario.NOMBRE[item];
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
            int y = 68 + i * 13;
            if (i == selPda) {
                g.setColor(0x1d4a58);
                g.fillRect(90, y - 2, 300, 12);
            }
            g.setColor(inv.cant[i] > 0 ? 0xffffff : 0x6a8a94);
            g.drawString(Inventario.NOMBRE[i], 100, y, Graphics.TOP | Graphics.LEFT);
            g.drawString("x" + inv.cant[i], 380, y, Graphics.TOP | Graphics.RIGHT);
        }
        g.setColor(0x9fc4cc);
        StringBuffer hs = new StringBuffer("Herramientas: ");
        int nh = 0;
        for (int i = 0; i < Inventario.NH; i++) {
            if (inv.tiene[i]) {
                if (nh++ > 0) {
                    hs.append(", ");
                }
                hs.append(Inventario.HERRAMIENTA[i]);
            }
        }
        if (nh == 0) {
            hs.append("ninguna");
        }
        g.drawString(hs.toString(), 96, 194, Graphics.TOP | Graphics.LEFT);
        String ayuda = selPda == Inventario.PEZ || selPda == Inventario.PEZ_COCIDO ? "X comer   "
                : (selPda == Inventario.AGUA ? "X beber   " : "");
        g.drawString(ayuda + "/\\ o O cerrar", 96, 208, Graphics.TOP | Graphics.LEFT);
        g.drawString("Mochila " + inv.total() + "/" + Inventario.CAPACIDAD, 384, 208, Graphics.TOP | Graphics.RIGHT);
    }

    private void dibujarFabricador(Graphics g) {
        panel(g, 40, 30, 400, 212);
        g.setColor(0x7fe8f4);
        g.drawString("FABRICADOR DE LA CAPSULA", 56, 40, Graphics.TOP | Graphics.LEFT);
        Inventario.Receta[] rs = Inventario.RECETAS;
        for (int i = 0; i < rs.length; i++) {
            Inventario.Receta r = rs[i];
            int y = 62 + i * 22;
            boolean hecho = r.herramienta >= 0 && inv.tiene[r.herramienta];
            boolean puede = inv.puede(r);
            if (i == selFab) {
                g.setColor(0x1d4a58);
                g.fillRect(50, y - 3, 380, 21);
            }
            g.setColor(hecho ? 0x6a8a94 : (puede ? 0xffffff : 0xb08080));
            g.drawString(r.nombre + (hecho ? "  (hecho)" : ""), 58, y, Graphics.TOP | Graphics.LEFT);
            g.setColor(0x9fc4cc);
            g.drawString(Inventario.ingredientes(r), 58, y + 9, Graphics.TOP | Graphics.LEFT);
        }
        Inventario.Receta r = rs[selFab];
        g.setColor(0x9fffa0);
        g.drawString(r.efecto, 58, 202, Graphics.TOP | Graphics.LEFT);
        g.setColor(0x9fc4cc);
        g.drawString("X fabricar   O salir", 424, 222, Graphics.TOP | Graphics.RIGHT);
        g.drawString("Mochila " + inv.total() + "/" + Inventario.CAPACIDAD, 58, 222, Graphics.TOP | Graphics.LEFT);
    }

    private void dibujarPausa(Graphics g) {
        panel(g, 100, 46, 280, 190);
        g.setColor(0x7fe8f4);
        g.drawString("PAUSA", Juego.W / 2, 60, Graphics.TOP | Graphics.HCENTER);
        String[] op = { "Continuar", "Mirar con: " + (Opciones.cruceta ? "CRUCETA" : "STICK"),
                "Prueba - viajar a: < " + Mundo.nombreBioma(zonaViaje) + " >",
                "Prueba - oxigeno infinito: " + (o2Infinito ? "SI" : "NO"),
                "Prueba - adelantar 3 h (" + reloj() + ")", "Salir al titulo" };
        for (int i = 0; i < 6; i++) {
            g.setColor(i == selPausa ? 0xffffff : 0x6a8a94);
            g.drawString((i == selPausa ? "> " : "  ") + op[i], Juego.W / 2, 80 + i * 18,
                    Graphics.TOP | Graphics.HCENTER);
        }
        g.setColor(0x9fc4cc);
        g.drawString(selPausa == 2 ? "<- -> elegir zona, X viajar" : (Opciones.cruceta
                ? "Cruceta: girar y mirar. X/O avanzar" : "Stick: mirar. Cruceta: nadar"), Juego.W / 2, 194,
                Graphics.TOP | Graphics.HCENTER);
        g.drawString("R/L subir y bajar", Juego.W / 2, 210, Graphics.TOP | Graphics.HCENTER);
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
        if (causaMuerte != null) {
            g.setColor(0xffb0a0);
            g.drawString(causaMuerte, Juego.W / 2, 130, Graphics.TOP | Graphics.HCENTER);
        }
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
