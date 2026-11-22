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
    /** story missions and their places; the escape pod's cabin */
    final Historia historia;
    final Capsula interior;
    /** inside the pod (the fabricator and the PDA return to the cabin) */
    boolean enCapsula;
    // first-minutes tutorial: one hint at a time until the player has done it
    private int tutorial;
    private boolean tutPda, tutSuperficie;
    private float tutX, tutZ;
    static final String[] TUTORIAL = { "Mira con el stick (o la cruceta) y nada con X hacia donde miras",
            "Busca rocas con vetas: cobre (naranja) y cuarzo (cristales). [] para recoger",
            "El O2 esta abajo a la izquierda: sube a la superficie para respirar",
            "Entra en la cápsula con [] en la escotilla y fabrica el cuchillo",
            "Abre la PDA con /\\: inventario, comida, agua y la misión" };

    /** seconds the current hint has been on screen: none stays longer than 20 s */
    private float tutT;
    private int tutVisto = -1;

    private void actualizarTutorial() {
        if (historia.etapa >= 2 || tutorial >= TUTORIAL.length) {
            tutorial = TUTORIAL.length;
            return;
        }
        if (tutorial != tutVisto) {
            tutVisto = tutorial;
            tutT = 0;
        }
        tutT += 1f / Juego.FPS;
        if (tutT > 20) {
            tutorial++;
            return;
        }
        switch (tutorial) {
        case 0:
            if ((px - tutX) * (px - tutX) + (pz - tutZ) * (pz - tutZ) > 12 * 12) {
                tutorial++;
            }
            break;
        case 1:
            if (inv.cant[Inventario.COBRE] > 0 || inv.cant[Inventario.CUARZO] > 0) {
                tutorial++;
            }
            break;
        case 2:
            if (fuera) {
                tutSuperficie = true;
            }
            if (tutSuperficie) {
                tutorial++;
            }
            break;
        case 3:
            if (inv.tiene[Inventario.H_CUCHILLO]) {
                tutorial++;
            }
            break;
        default:
            if (tutPda) {
                tutorial++;
            }
            break;
        }
    }

    /** zone hostiles with special attacks */
    final Hostiles hostiles;
    /** status effects (seconds left) */
    float veneno, aturdido, ceguera, vision, mareo, boya;

    /** time of the last blow (causaMuerte only counts if it was recent) */
    private float causaT = -99;

    /** what actually killed the diver: a recent blow, or what was draining the health */
    private String causaReal() {
        if (causaMuerte != null && tiempo - causaT < 5) {
            return causaMuerte;
        }
        if (o2 <= 0) {
            return "Te quedaste sin oxígeno";
        }
        if (veneno > 0) {
            return "El veneno acabo contigo";
        }
        if (agua <= 0 && comida <= 0) {
            return "Hambre y sed";
        }
        if (agua <= 0) {
            return "Deshidratación: necesitabas agua";
        }
        if (comida <= 0) {
            return "Inanición: necesitabas comer";
        }
        return causaMuerte != null && tiempo - causaT < 20 ? causaMuerte : "Tus heridas";
    }

    // ------------------------------------------------------------------ hand equipment
    // One thing in the left hand at a time, chosen in the PDA; L uses it (the flashlight
    // too: equip it, then L switches it on and off). When something runs out the hand is
    // left empty.
    static final int EQ_NADA = 0, EQ_LINTERNA = 1, EQ_BOYA = 2, EQ_BENGALA = 3, EQ_SOLDADOR = 4, EQ_IMPULSOR = 5,
            EQ_ESCANER = 6;
    static final String[] EQ_NOMBRE = { "nada", "Linterna", "Boya de emergencia", "Bengalas", "Soldador",
            "Impulsor de mano", "Escáner de mano" };
    int equipo;
    /** the hand thruster overheated: it stays off until it has cooled down a bit */
    boolean impBloq;
    private float impBurbuja;
    /** hand scanner: what it is pointed at (Bestiario code, -1 none) and the progress */
    int escObjetivo = -1;
    float escProgreso;
    private int escTic;
    final Bestiario bestiario = new Bestiario();
    final Mapa mapa = new Mapa();
    /** seconds since the last automatic save */
    private float autoT;
    Ecos ecos;
    private String[] ecoTexto;
    /** right hand: 0 knife, 1 discharge rod */
    static final int ARMA_CUCHILLO = 0, ARMA_VARA = 1, ARMA_ARPON = 2;
    static final String[] ARMA_NOMBRE = { "Cuchillo", "Vara de descarga", "Arpón" };
    int arma;
    private float varaT;
    /** the rod's discharge burst in the water: where, and for how long it shows */
    private float descargaX, descargaY, descargaZ, descargaT;

    boolean armaEnMano() {
        return arma == ARMA_VARA ? inv.tiene[Inventario.H_VARA]
                : (arma == ARMA_ARPON ? inv.tiene[Inventario.H_ARPON] : inv.tiene[Inventario.H_CUCHILLO]);
    }

    /** the knife is the one that cuts coral and kelp */
    boolean cuchilloEnMano() {
        return inv.tiene[Inventario.H_CUCHILLO] && (arma == ARMA_CUCHILLO || !armaEnMano());
    }

    /** hand thruster charge (0-100): it drains while pushing and refills on its own */
    float impCarga = 100;
    boolean impulsando;
    /** flares thrown: position, velocity, seconds of light left */
    static final int NBG = 3;
    final float[] bgX = new float[NBG], bgY = new float[NBG], bgZ = new float[NBG], bgVX = new float[NBG],
            bgVY = new float[NBG], bgVZ = new float[NBG], bgT = new float[NBG];
    static final float BENGALA_T = 90;
    private final Light luzBengala = new Light();
    private float soldarSon, chispaT;

    boolean equipable(int e) {
        switch (e) {
        case EQ_BOYA:
            return inv.cant[Inventario.BOYA] > 0;
        case EQ_BENGALA:
            return inv.cant[Inventario.BENGALA] > 0;
        case EQ_SOLDADOR:
            return inv.tiene[Inventario.H_SOLDADOR];
        case EQ_IMPULSOR:
            return inv.tiene[Inventario.H_IMPULSOR];
        case EQ_LINTERNA:
            return inv.tiene[Inventario.H_LINTERNA];
        case EQ_ESCANER:
            return inv.tiene[Inventario.H_ESCANER];
        default:
            return true;
        }
    }

    void equipar(int e) {
        equipo = e;
        linterna = false;
        if (e != EQ_NADA) {
            avisar("En la mano: " + EQ_NOMBRE[e] + " (L para usar)");
        } else {
            avisar("Mano libre");
        }
        Sonido.tocar(Sonido.CLICK);
    }

    /** L with something in the left hand (pressed or held, depending on the item) */
    private void usarEquipo(Entrada in, float dt) {
        impulsando = false;
        if (estado != JUEGO) {
            return;
        }
        if (enRemora) {
            if (in.pulso(Entrada.L)) {
                faros = !faros;
                Sonido.tocar(Sonido.CLICK);
                Log.d("faros de la remora " + (faros ? "encendidos" : "apagados"));
            }
            return;
        }
        if (equipo != EQ_NADA && !equipable(equipo) && boya <= 0) {
            equipo = EQ_NADA; // used up: the hand stays empty
            linterna = false;
        }
        boolean pulso = in.pulso(Entrada.L), mantener = in.tiene(Entrada.L);
        if (equipo != EQ_ESCANER || !mantener) {
            escProgreso = 0;
            escObjetivo = -1;
        }
        switch (equipo) {
        case EQ_NADA:
            if (pulso) {
                avisar("Mano izquierda vacia: equipa algo desde la PDA");
            }
            break;
        case EQ_LINTERNA:
            if (pulso) {
                linterna = !linterna;
                accionIzq(ACC_CLIC, 0.2f);
                Sonido.tocar(Sonido.CLICK);
                Log.d("linterna " + (linterna ? "encendida" : "apagada"));
            }
            break;
        case EQ_BOYA:
            if (pulso) {
                inflarBoya();
            }
            break;
        case EQ_BENGALA:
            if (pulso) {
                lanzarBengala();
            }
            break;
        case EQ_SOLDADOR:
            if (mantener) {
                soldar(pulso, dt);
            }
            break;
        case EQ_IMPULSOR:
            if (mantener && !impBloq && !fuera) {
                impulsando = true;
                impCarga = Math.max(0, impCarga - 9 * dt);
                if (impCarga <= 0) {
                    impBloq = true;
                    impulsando = false;
                    avisar("Impulsor sobrecalentado: se apaga hasta que se enfrie");
                    Sonido.tocar(Sonido.CLICK);
                }
            } else if (pulso && impBloq) {
                avisar("Impulsor enfriándose...");
            }
            break;
        case EQ_ESCANER:
            if (mantener) {
                escanear(dt);
            }
            break;
        default:
            break;
        }
    }

    /** hand scanner: hold L on a creature or plant for a moment to record it */
    private void escanear(float dt) {
        if (--escTic <= 0) {
            escTic = 4;
            int o = bestiario.apuntado(this);
            if (o != escObjetivo) {
                escObjetivo = o;
                escProgreso = 0;
                if (o >= 0) {
                    Log.d("escaner apunta a: " + Bestiario.NOMBRE[o]);
                }
            }
        }
        if (escObjetivo < 0) {
            return;
        }
        if (bestiario.conocido[escObjetivo]) {
            return;
        }
        escProgreso += dt;
        if (escProgreso >= 1.6f) {
            bestiario.conocido[escObjetivo] = true;
            avisar("Escaneado: " + Bestiario.NOMBRE[escObjetivo] + " (bestiario en la PDA, L)");
            Sonido.tocar(Sonido.SONAR);
            Log.d("escaner: " + Bestiario.NOMBRE[escObjetivo] + " (" + bestiario.total() + "/" + Bestiario.N + ")");
        }
    }

    private void lanzarBengala() {
        if (fuera) {
            avisar("La bengala se enciende bajo el agua");
            return;
        }
        if (accIzqT > 0 || bengalaPend > 0) {
            return;
        }
        // the arm swings back and throws: the flare leaves the hand mid-swing
        accionIzq(ACC_LANZAR, 0.55f);
        bengalaPend = 0.2f;
    }

    private float bengalaPend;

    private void soltarBengala() {
        int b = 0;
        for (int i = 0; i < NBG; i++) {
            if (bgT[i] <= 0) {
                b = i;
                break;
            }
            if (bgT[i] < bgT[b]) {
                b = i;
            }
        }
        inv.cant[Inventario.BENGALA]--;
        bgX[b] = px + fx * 0.8f;
        bgY[b] = py + fy * 0.8f - 0.2f;
        bgZ[b] = pz + fz * 0.8f;
        bgVX[b] = fx * 9;
        bgVY[b] = fy * 9 + 1;
        bgVZ[b] = fz * 9;
        bgT[b] = BENGALA_T;
        Sonido.tocar(Sonido.TAJO);
        avisar("Bengala lanzada (" + inv.cant[Inventario.BENGALA] + " quedan)");
        if (inv.cant[Inventario.BENGALA] <= 0) {
            equipo = EQ_NADA;
        }
    }

    private void actualizarBengalas(float dt) {
        if (bengalaPend > 0) {
            bengalaPend -= dt;
            if (bengalaPend <= 0 && inv.cant[Inventario.BENGALA] > 0) {
                soltarBengala();
            }
        }
        for (int i = 0; i < NBG; i++) {
            if (bgT[i] <= 0) {
                continue;
            }
            bgT[i] -= dt;
            // thrown, slowed by the water, then it sinks slowly and rests on the floor
            float k = Math.min(1, dt * 1.6f);
            bgVX[i] -= bgVX[i] * k;
            bgVZ[i] -= bgVZ[i] * k;
            bgVY[i] += (-0.5f - bgVY[i]) * k;
            bgX[i] += bgVX[i] * dt;
            bgY[i] += bgVY[i] * dt;
            bgZ[i] += bgVZ[i] * dt;
            float suelo = Terreno.altura(bgX[i], bgZ[i]) + 0.1f;
            if (bgY[i] < suelo) {
                bgY[i] = suelo;
                bgVX[i] = bgVY[i] = bgVZ[i] = 0;
            }
            if (bgY[i] > -0.3f) {
                bgY[i] = -0.3f;
            }
            col[0] = bgX[i];
            col[1] = bgY[i];
            col[2] = bgZ[i];
            if (mundo.empujarCerca(col, 0.1f)) {
                bgX[i] = col[0];
                bgY[i] = col[1];
                bgZ[i] = col[2];
                bgVX[i] = bgVZ[i] = 0;
            }
        }
    }

    /** welding the Remora's hull: close to it, R held */
    private void soldar(boolean pulso, float dt) {
        float dx = rx - px, dy = ry - py, dz = rz - pz;
        if (!inv.tiene[Inventario.H_REMORA] || dx * dx + dy * dy + dz * dz > 5.5f * 5.5f) {
            if (pulso) {
                avisar("Acércate a la Rémora para soldar el casco");
            }
            return;
        }
        if (rCasco >= 100) {
            if (pulso) {
                avisar("El casco de la Rémora esta intacto");
            }
            return;
        }
        rCasco = Math.min(100, rCasco + 7 * dt);
        chispaT = 0.15f;
        soldarSon -= dt;
        if (soldarSon <= 0) {
            soldarSon = 0.35f;
            Sonido.tocar(Sonido.CORTAR);
        }
        if (rCasco >= 100) {
            rAveriada = false;
            avisar("Casco reparado: Rémora al 100%");
            Sonido.tocar(Sonido.FABRICAR);
        }
    }

    private void inflarBoya() {
        if (fuera) {
            avisar("La boya solo sirve bajo el agua");
            return;
        }
        if (boya > 0) {
            return;
        }
        if (inv.cant[Inventario.BOYA] <= 0) {
            equipo = EQ_NADA;
            return;
        }
        inv.cant[Inventario.BOYA]--;
        boya = 7;
        // the burst as it inflates: a cloud of bubbles all around
        for (int q = 0; q < 14; q++) {
            float a = q * 0.449f;
            float rr = 0.8f + Terreno.azar(q, burSemilla, 15) * 1.2f;
            emitirBurbuja(px + j2me.FMath.cos(a) * rr, py - 0.5f + Terreno.azar(q, burSemilla, 16),
                    pz + j2me.FMath.sin(a) * rr, 4 + Terreno.azar(q, burSemilla, 17) * 3);
        }
        burSemilla++;
        Sonido.tocar(Sonido.BURBUJAS);
        avisar("Boya inflada: subiendo!");
        Log.d("boya inflada a " + (int) -py + " m");
    }

    void envenenar(float s) {
        if (veneno <= 0) {
            Sonido.tocar(Sonido.VENENO);
        }
        veneno = Math.max(veneno, s);
    }

    void aturdir(float s) {
        aturdido = Math.max(aturdido, s);
    }

    void cegar(float s) {
        ceguera = Math.max(ceguera, s);
    }

    void robarOxigeno(float s) {
        if (!enRemora) {
            o2 = Math.max(0, o2 - s);
            avisar("Te ha robado aire! (-" + (int) s + " s de O2)");
        }
    }

    /** the diver's own base (habitat module) */
    boolean hayBase;
    float baseX, baseY, baseZ;
    private Mesh baseMalla, boyaMalla, balizaCian, balizaRoja, granjaMalla, recargaMalla, balizaVerde, postBaliza;
    private float latidoT;
    // the Remora minisub: parked somewhere, or driven by the player
    /** the Remora: [0] propeller (spins), [1] the rest */
    private final Mesh[] remoraP;
    private float helice, heliceVel;
    // a few small bubbles from the propeller while it spins
    static final int NBUR = 28;
    private final float[] burX = new float[NBUR], burY = new float[NBUR], burZ = new float[NBUR],
            burT = new float[NBUR];
    private float burEmitir, burSubida[] = new float[NBUR];
    private int burSemilla;

    /** a bubble at (x, y, z) rising at sube m/s (reuses the oldest slot if all are busy) */
    private void emitirBurbuja(float x, float y, float z, float sube) {
        int mejor = 0;
        for (int i = 0; i < NBUR; i++) {
            if (burT[i] <= 0) {
                mejor = i;
                break;
            }
            if (burT[i] < burT[mejor]) {
                mejor = i;
            }
        }
        burT[mejor] = 2;
        burX[mejor] = x;
        burY[mejor] = y;
        burZ[mejor] = z;
        burSubida[mejor] = sube;
    }

    private void dibujarBurbujas() {
        for (int i = 0; i < NBUR; i++) {
            if (burT[i] > 0) {
                burT[i] -= dtDibujo;
                burY[i] += burSubida[i] * dtDibujo;
                burX[i] += j2me.FMath.sin(tiempo * 3 + i) * 0.2f * dtDibujo;
                t.setIdentity();
                t.postTranslate(burX[i], burY[i], burZ[i]);
                float k = 0.6f + (2 - burT[i]) * 0.3f;
                t.postScale(k, k, k);
                g3d.render(burbuja, t);
            }
        }
    }
    private Mesh burbuja;
    private final Mesh manoCuchillo, manoLinterna, manoBoya, manoVacia, haz, hazRemora;
    private final Transform tp2 = new Transform();
    private Mesh manoDerVacia, manoArpon, manoArponVacio, saeta;
    private Mesh manoBengala, manoSoldador, manoImpulsor, chispa, bengalaLuz, manoVara, chispaAzul, manoEscaner;
    /** knife swing animation, seconds left */
    float tajo;
    static final float TAJO = 0.45f;
    /** a swing whose effect has not landed yet; a short kick of the view on impact */
    boolean golpePendiente;
    float golpeCam;
    boolean enRemora;
    /** the Remora's headlights: their own switch (L while aboard), on by default */
    boolean faros = true;
    /** Remora defence pulse: seconds until it can fire again, and the screen flash */
    float pulsoCarga, destello;
    /** vehicle steering: turn rate with inertia, and the roll it produces */
    float giroVel, alabeo;
    float rx = 6, ry = -2.5f, rz = 7, rRumbo = 150;
    /** the Remora's hull (0-100): it takes the damage while you are aboard */
    float rCasco = 100;
    /** broken down: it cannot be boarded until repaired with a repair kit */
    boolean rAveriada;
    /** seconds the "exit" hint stays on screen after boarding */
    float bordoT;

    /** headlights actually lit: they flicker when the hull is badly damaged */
    boolean farosActivos() {
        if (!faros) {
            return false;
        }
        if (rCasco < 30) {
            int k = (int) (tiempo * 11);
            return (k % 7) != 0 && (k % 11) != 3;
        }
        return true;
    }
    static final float REMORA_VEL = 9.5f;

    /** the Remora's crush depth: 300 m, 900 m with the depth module */
    float remoraMax() {
        return inv.tiene[Inventario.H_PROFUNDIDAD] ? 900 : 300;
    }
    /** red flash when hurt */
    float golpeT;
    String causaMuerte;
    /** test options (pause menu) */
    int zonaViaje = 1;
    static final int SENAL = 99;
    static final int[] ORDEN_VIAJE = { Terreno.BAJIOS, Terreno.BOSQUE, Terreno.PRADERA, Terreno.MESETA,
            Terreno.CANONES, Terreno.TERMAL, Terreno.ABISMO, SENAL };
    boolean o2Infinito;
    final Inventario inv = new Inventario();
    private final Mesh capsula;
    private final Mesh superficie;
    private final Mesh oceano;
    private final Mesh olasAbajo;
    static final float VEL_OLA = 1.6f;

    /** height of the swell at a point: two crossing wave trains (same as the mesh) */
    static float ola(float x, float z, float t) {
        float u = x - t * VEL_OLA;
        return j2me.FMath.sin(u * (6.2831855f / 24)) * 0.45f
                + j2me.FMath.sin((u + z) * (6.2831855f / 16)) * 0.22f;
    }

    /**
     * The open sea as a wavy mesh that tiles every 48 m (the wave periods divide it) and
     * spans 144 m; it is scrolled with the swell. abajo: the translucent underside.
     */
    private static Mesh crearOlas(Fog fog, boolean abajo) {
        MB b = new MB();
        b.scale = 1f / 32f;
        b.jitter = abajo ? 14 : 8;
        b.suave = true;
        int n = abajo ? 16 : 30;
        float c = abajo ? 6 : 10;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                float x0 = i * c, z0 = j * c;
                float h00 = ola(x0, z0, 0), h10 = ola(x0 + c, z0, 0);
                float h01 = ola(x0, z0 + c, 0), h11 = ola(x0 + c, z0 + c, 0);
                int base = abajo ? 0xa8eef6 : 0x1f6c8c;
                int cima = abajo ? 0xe0fcff : 0x4a9cb8;
                int c00 = Terreno.mezcla(base, cima, (h00 + 0.67f) / 1.34f);
                int c10 = Terreno.mezcla(base, cima, (h10 + 0.67f) / 1.34f);
                int c01 = Terreno.mezcla(base, cima, (h01 + 0.67f) / 1.34f);
                int c11 = Terreno.mezcla(base, cima, (h11 + 0.67f) / 1.34f);
                b.triCol(x0, h00, z0, c00, x0, h01, z0 + c, c01, x0 + c, h11, z0 + c, c11);
                b.triCol(x0, h00, z0, c00, x0 + c, h11, z0 + c, c11, x0 + c, h10, z0, c10);
            }
        }
        return abajo ? b.build(fog, 120, true, 0) : b.build(fog);
    }
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
    static final int JUEGO = 0, PDA = 1, PAUSA = 2, MUERTO = 3, FABRICADOR = 4, LEYENDO = 5, INTERIOR = 6,
            FINAL = 7, BESTIARIO = 8, MAPA = 9, ECO = 10;

    /** the crew's logs, found in the station wreckage (original story) */
    static final String[][] REGISTROS = {
            { "REGISTRO 1 - Dra. Inés Vargas, biologa",
                    "Los sismos ya no son aleatorios. Los sensores del fondo",
                    "registran un pulso cada 41 minutos, siempre igual.",
                    "Algo late bajo la fosa. Lo llamo el Latido." },
            { "REGISTRO 2 - Teo Ruiz, ingeniero",
                    "La baliza principal se fundio con la última sacudida.",
                    "El módulo de repuesto iba en el contenedor 7, que cayo",
                    "hacia la meseta roja. Sin el no hay rescate." },
            { "REGISTRO 3 - Comandante Okafor",
                    "Ordeno evacuar a las cápsulas. La Halcyon no aguantara",
                    "otro pulso. Que nadie baje a la fosa: los sonares",
                    "muestran movimiento donde no deberia haber nada." },
            { "REGISTRO 4 - Dra. Inés Vargas",
                    "Los cristales de la fosa vibran a la misma frecuencia",
                    "que el pulso. No es geología. Es un organismo...",
                    "y es más grande que la estación entera." },
            { "REGISTRO 5 - Teo Ruiz",
                    "Una sombra con alas de ceniza cruzo los focos a 600 m.",
                    "Tenía luces colgando bajo la boca, como cebos.",
                    "Los sonares la perdieron en dos segundos." },
            { "REGISTRO 6 - sin firma",
                    "Si sigues vivo: repara la baliza. Hara falta un núcleo",
                    "de energía de la estación y cristal de la fosa.",
                    "Y date prisa. El Latido nos escucha." },
            { "REGISTRO 7 - Mara Lund, piloto", "Volaba la ruta de la meseta cuando vi luces violetas",
                    "en una piedra tallada. Nadie talla piedras en Talassa.",
                    "Nadie... que nosotros conozcamos." },
            { "REGISTRO 8 - Teo Ruiz", "Los gigantes de arrecife no huyen de nosotros. Tienen",
                    "parasitos en el lomo y un coral que no crece en otro",
                    "sitio. Muestras recogidas: excelentes para tanques." },
            { "REGISTRO 9 - Dra. Inés Vargas", "Los chillones de los cañones cazan a ciegas: su",
                    "chillido aturde a la presa. Si oyes uno, aparta la",
                    "vista y prepara el cuchillo." },
            { "REGISTRO 10 - Comandante Okafor", "Prohibido bajar al borde del vacío. Dos sondas",
                    "perdidas. La última transmitio la imagen de algo",
                    "con placas que brillaban como hielo azul." },
            { "REGISTRO 11 - Dra. Inés Vargas", "Las piedras de glifo y el Latido comparten firma.",
                    "Creo que son partes de un mismo instrumento. Si se",
                    "reunen... quiza puedan hablar con lo que duerme abajo." },
            { "REGISTRO 12 - sin firma", "Si alguien escucha esto: no tengan miedo de la Madre.",
                    "No es un monstruo. Esta dormida... y nos escucha." } };
    int leyendo;
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
    static final int NP = 28;
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
        long trabajo = logica + ms3d + ms2d;
        if (trabajo > peorFrame) {
            peorFrame = trabajo;
        }
        if (logica > peorLogica) {
            peorLogica = logica;
        }
        if (trabajo > 33) {
            lentos++;
        }
    }

    long peorFrame, peorLogica;
    int lentos;
    private int gcCuenta0, gcMs0, gcKb0;

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
        return inv.tiene[Inventario.H_TANQUE2] ? 120 : (inv.tiene[Inventario.H_TANQUE] ? 75 : O2_MAX);
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
        oceano = crearOlas(fog, false);
        olasAbajo = crearOlas(fog, true);
        peces = new Peces(fog);
        peces.mundo = null; // set once the world exists (below)
        remoraP = Modelos.remoraPartes(fog);
        burbuja = Modelos.burbuja(fog);
        baseMalla = Modelos.habitat(fog);
        granjaMalla = Modelos.moduloGranja(fog);
        recargaMalla = Modelos.moduloRecarga(fog);
        boyaMalla = Modelos.boyaInflada(fog);
        ecos = new Ecos(fog);
        balizaCian = Modelos.luzBaliza(0x5ff0ff);
        balizaVerde = Modelos.luzBaliza(COLOR_SENAL);
        postBaliza = Modelos.postBaliza(fog);
        balizaRoja = Modelos.luzBaliza(0xff4030);
        manoCuchillo = Modelos.manoCuchillo(fog);
        manoLinterna = Modelos.manoLinterna(fog);
        manoBoya = Modelos.manoBoya(fog, true);
        manoDerVacia = Modelos.manoDerVacia(fog);
        manoArpon = Modelos.manoArpon(fog, true);
        manoArponVacio = Modelos.manoArpon(fog, false);
        saeta = Modelos.saetaArpon(fog);
        manoBengala = Modelos.manoBengala(fog);
        manoSoldador = Modelos.manoSoldador(fog);
        manoImpulsor = Modelos.manoImpulsor(fog);
        manoEscaner = Modelos.manoEscaner(fog);
        chispa = Modelos.luzBaliza(0xffe8a0);
        chispaAzul = Modelos.luzBaliza(0x80e0ff);
        manoVara = Modelos.manoVara(fog);
        bengalaLuz = Modelos.luzBaliza(0xff5030);
        luzBengala.setMode(Light.OMNI);
        luzBengala.setColor(0xff7050);
        luzBengala.setIntensity(3.0f);
        luzBengala.setAttenuation(1, 0.08f, 0.02f);
        manoVacia = Modelos.manoBoya(fog, false);
        haz = Modelos.hazLinterna(fog);
        hazRemora = Modelos.hazRemora(fog);
        depredadores = new Depredadores(fog);
        historia = new Historia(fog);
        hostiles = new Hostiles(fog);
        interior = new Capsula(fog);
        peces.mundo = mundo;
        foco.setMode(Light.SPOT);
        foco.setColor(0xfff2d0);
        foco.setIntensity(1.6f);
        foco.setSpotAngle(28);
        foco.setSpotExponent(6);
        foco.setAttenuation(1, 0.02f, 0.0025f);
        Log.d("capsula, superficie y peces listos; vivos " + Log.vivo() + " KB; " + Log.memoria());
        boolean cargada = continuar && Guardado.cargar(this);
        if (equipo == EQ_NADA && inv.tiene[Inventario.H_LINTERNA]) {
            equipo = EQ_LINTERNA; // games from before 0.22: the lamp in the hand, as before
        }
        tutX = px;
        tutZ = pz;
        if (cargada && (inv.tiene[Inventario.H_CUCHILLO] || historia.etapa >= 2)) {
            tutorial = TUTORIAL.length; // a game already under way: no hints
        }
        mundo.cargarTodo(px, pz);
        for (int i = 0; i < NP; i++) {
            sx[i] = (Terreno.azar(i, 1, 300) - 0.5f) * 24;
            sy[i] = (Terreno.azar(i, 2, 300) - 0.5f) * 14;
            sz[i] = (Terreno.azar(i, 3, 300) - 0.5f) * 24;
        }
        if (cargada) {
            avisar("Partida cargada. Día " + (1 + (int) (tiempo / 1200)) + ".");
        } else {
            avisar("Día 1. La estación Halcyon ha desaparecido. Sobrevive.");
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

    /** place the habitat module on the floor here (one base; it can be moved) */
    private void colocarBase() {
        if (fuera || enRemora) {
            avisar("Coloca el hábitat bajo el agua, a pie");
            return;
        }
        float suelo = Terreno.altura(px, pz);
        if (-suelo > 260) {
            avisar("Demasiado profundo para el hábitat (máximo 260 m)");
            return;
        }
        if (px * px + pz * pz < 20 * 20) {
            avisar("Demasiado cerca de la cápsula");
            return;
        }
        inv.cant[Inventario.HABITAT]--;
        if (hayBase) {
            avisar("Base trasladada aquí");
        } else {
            avisar("Base construida. [] en la escotilla para entrar");
        }
        hayBase = true;
        baseX = px + fx * 4;
        baseZ = pz + fz * 4;
        baseY = Terreno.altura(baseX, baseZ) - 0.2f;
        Sonido.tocar(Sonido.FABRICAR);
        Log.d("base colocada en " + (int) baseX + "," + (int) baseY + "," + (int) baseZ);
        estado = JUEGO;
    }

    /** base modules: harvest waiting in the algae farm, and its growth timer */
    int cosecha;
    float granjaT;
    static final float CRECER = 150;
    static final int COSECHA_MAX = 6;
    /** module offsets from the habitat centre (x): farm east, recharge room west */
    static final float MOD_DX = 6.6f;

    /** entering the base: harvest the farm, the recharge room heals and fixes the Remora */
    private void alEntrarBase() {
        if (inv.tiene[Inventario.H_GRANJA] && cosecha > 0) {
            int dados = 0;
            while (cosecha > 0) {
                if (!inv.meter((dados & 1) == 0 ? Inventario.BULBO : Inventario.AGUA)) {
                    break;
                }
                cosecha--;
                dados++;
            }
            avisar(dados > 0 ? "Granja: +" + dados + " (bulbos y agua)" : "Granja lista, pero la mochila esta llena");
        }
        if (inv.tiene[Inventario.H_RECARGA]) {
            salud = 100;
            float dx = rx - baseX, dz = rz - baseZ;
            if (inv.tiene[Inventario.H_REMORA] && dx * dx + dz * dz < 40 * 40 && rCasco < 100) {
                rCasco = 100;
                rAveriada = false;
                avisar("Sala de recarga: Rémora reparada y salud al máximo");
            } else {
                avisar("Sala de recarga: salud al máximo");
            }
        }
    }

    boolean cercaBase() {
        if (!hayBase) {
            return false;
        }
        float dx = px - baseX, dz = pz - baseZ, dy = py - baseY;
        return dx * dx + dz * dz < 5.5f * 5.5f && dy > -1 && dy < 7;
    }

    /** the console woke up from sleep: pause the game (the cabin keeps its own state) */
    void alReanudar() {
        Log.d("consola reanudada tras suspension");
        if (estado == JUEGO) {
            estado = PAUSA;
            selPausa = 0;
        }
        avisar("Consola reanudada");
    }

    private void comer(float c, float a, float sal, String msg) {
        inv.cant[selPda]--;
        comida = Math.min(100, comida + c);
        agua = Math.max(0, Math.min(100, agua + a));
        salud = Math.max(1, Math.min(100, salud + sal));
        avisar(msg);
        Sonido.tocar(Sonido.COMER);
    }

    void abrirPda() {
        tutPda = true;
        estado = PDA;
        Sonido.tocar(Sonido.PDA);
        menuPda = -1; // the cursor stays where you left it
        listarPda();
    }

    void avisar(String s) {
        // a warning repeated every frame (hull, oxygen...) only refreshes the one on screen:
        // no new log line, no Memory Stick write
        if (avisoT > 0 && s.equals(aviso)) {
            avisoT = 4;
            return;
        }
        aviso = s;
        avisoT = 4;
        Log.d("aviso: " + s);
    }

    // ------------------------------------------------------------------ update

    /** the last update's time step, for animations done while drawing */
    private float dtDibujo = 1 / 30f;

    void actualizar(Entrada in, float dt) {
        dtDibujo = dt;
        if (enCapsula) {
            // inside the pod or the base time goes on: the day turns, hunger and thirst grow
            tiempo += dt;
            o2 = o2Max();
            comida = Math.max(0, comida - HAMBRE * dt);
            agua = Math.max(0, agua - SED * dt);
            if (comida > 20 && agua > 20) {
                salud = Math.min(100, salud + 0.5f * dt);
            } else if (comida <= 0 || agua <= 0) {
                salud = Math.max(1, salud - 0.6f * dt);
            }
        }
        long t0 = System.currentTimeMillis();
        if (in.pulso(Entrada.DEVLOG)) {
            verDevlog = !verDevlog;
            Log.d("panel devlog " + (verDevlog ? "abierto" : "cerrado"));
        }
        if (estado == FABRICADOR) {
            actualizarFabricador(in);
            return;
        }
        if (estado == INTERIOR) {
            o2 = o2Max();
            interior.actualizar(this, in, dt);
            return;
        }
        if (estado == FINAL) {
            if (in.pulso(Entrada.X)) {
                estado = JUEGO;
            }
            return;
        }
        if (estado == LEYENDO) {
            int n = registrosLeidos();
            if (in.pulso(Entrada.X) || in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
                estado = enCapsula ? INTERIOR : JUEGO;
            } else if (in.pulso(Entrada.DER) && n > 0) {
                leyendo = (leyendo + 1) % n;
            } else if (in.pulso(Entrada.IZQ) && n > 0) {
                leyendo = (leyendo + n - 1) % n;
            }
            return;
        }
        if (estado == PDA) {
            actualizarPda(in);
            return;
        }
        if (estado == BESTIARIO) {
            if (!bestiario.actualizar(in)) {
                estado = PDA;
            }
            return;
        }
        if (estado == MAPA) {
            if (!mapa.actualizar(in)) {
                estado = PDA;
            }
            return;
        }
        if (estado == ECO) {
            if (in.pulso(Entrada.X) || in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.CUADRADO)) {
                estado = JUEGO;
            }
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
            abrirPda();
            return;
        }
        historia.actualizar(this);
        actualizarTutorial();
        if (interior.botiquinT > 0) {
            interior.botiquinT -= dt;
        }
        tiempo += dt;
        // look: analog stick, or the d-pad in "cruceta" mode (for a broken stick)
        float lx = in.ax;
        float ly = in.ay;
        if (Opciones.cruceta) {
            lx = (in.tiene(Entrada.DER) ? 0.8f : 0) - (in.tiene(Entrada.IZQ) ? 0.8f : 0);
            ly = (in.tiene(Entrada.ABAJO) ? 0.8f : 0) - (in.tiene(Entrada.ARRIBA) ? 0.8f : 0);
        }
        if (enRemora) {
            // a vehicle: the turn rate builds up and dies down, and the hull rolls into turns
            giroVel += (-lx * 55 - giroVel) * Math.min(1, dt * 2.2f);
            yaw += giroVel * dt;
            pitch -= ly * 40 * dt;
            alabeo += (giroVel * 0.25f - alabeo) * Math.min(1, dt * 3);
        } else {
            giroVel = 0;
            alabeo = 0;
            yaw -= lx * 110 * dt;
            pitch -= ly * 80 * dt;
        }
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
        float aletas = enRemora ? REMORA_VEL / VEL * (inv.tiene[Inventario.H_TURBO] ? 1.3f : 1)
                : (inv.tiene[Inventario.H_ALETAS] ? 1.3f : 1f);
        boolean cr = Opciones.cruceta;
        // you swim where you look: X only (up/down by looking up/down, like the original)
        float adelante = aletas * (in.tiene(Entrada.X) ? 1 : 0) * (aturdido > 0 ? 0.3f : 1);
        ax += fx * adelante * VEL;
        ay += fy * adelante * VEL;
        az += fz * adelante * VEL;
        if (impulsando) {
            // the hand thruster pulls you along the view, much faster than the fins
            ax = fx * VEL * 2.4f;
            ay = fy * VEL * 2.4f;
            az = fz * VEL * 2.4f;
        }
        float lado = cr || enRemora ? 0 : (in.tiene(Entrada.DER) ? 1 : 0) - (in.tiene(Entrada.IZQ) ? 1 : 0);
        ax += cy * lado * VEL_LATERAL;
        az += -syw * lado * VEL_LATERAL;

        // water: velocity eases towards the target (inertia), then drifts to a stop
        // the sub is heavier: slower to get going and to stop
        if (boya > 0 && !enRemora) {
            ay = 9; // the inflated buoy hauls the diver up
        }
        float k = Math.min(1, dt * (enRemora ? 1.0f : 2.5f));
        vx += (ax - vx) * k;
        vy += (ay - vy) * k;
        vz += (az - vz) * k;
        px += vx * dt;
        py += vy * dt;
        pz += vz * dt;
        // collisions: sea floor, the surface and the capsule hull
        float suelo = Terreno.alturaMalla(px, pz) + (enRemora ? 1.5f : 0.9f);
        if (enRemora) {
            // the sub is ~5 m long: keep its nose and tail out of the slopes too
            float hx = -j2me.FMath.sin(yaw * j2me.FMath.RAD) * 2.4f;
            float hz = -j2me.FMath.cos(yaw * j2me.FMath.RAD) * 2.4f;
            suelo = Math.max(suelo, Math.max(Terreno.altura(px + hx, pz + hz), Terreno.altura(px - hx, pz - hz)) + 0.9f);
        }
        if (py < suelo) {
            py = suelo;
            if (vy < 0) {
                vy = 0;
            }
        }
        // The surface has two clear states and nothing in between, so the camera is never
        // a few centimetres from the water plane (on real hardware the surface triangles
        // then pass right through the eye):
        //  - under water the camera stays at or below -0.7 m; rising past it (not diving)
        //    jumps to the surface;
        //  - at the surface the head is at +0.5 m; L, or swimming steeply downwards, dives
        //    straight back to -0.8 m.
        if (fuera) {
            boolean bucear = ay < -1.5f;
            if (bucear) {
                fuera = false;
                py = -0.8f;
                vy = -1;
                transicion("bajo el agua");
            } else {
                // ride the swell
                py = 0.5f + ola(px, pz, tiempo);
                vy = 0;
            }
        } else if (py > -0.7f) {
            boolean subir = ay >= -0.3f;
            if (subir) {
                fuera = true;
                py = 0.5f;
                vy = 0;
                transicion("cabeza fuera del agua");
            } else {
                py = -0.7f;
                if (vy > 0) {
                    vy = 0;
                }
            }
        }
        float cdx = px;
        float cdz = pz;
        float cd = j2me.FMath.sqrt(cdx * cdx + cdz * cdz);
        float rCap = enRemora ? 4.4f : 3.0f;
        if (cd < rCap && py > -2.6f - (enRemora ? 1 : 0) && cd > 0.01f) {
            px = cdx / cd * rCap;
            pz = cdz / cd * rCap;
        }
        // the parked Remora is solid for the diver: a capsule along its 5 m hull, so the
        // camera never gets inside the nose or the tail
        if (!enRemora && inv.tiene[Inventario.H_REMORA]) {
            float hr = rRumbo * j2me.FMath.RAD;
            float nx = -j2me.FMath.sin(hr), nz = -j2me.FMath.cos(hr); // its nose direction
            float ox = px - rx, oy = py - ry, oz = pz - rz;
            float tq = Math.max(-2.3f, Math.min(2.3f, ox * nx + oz * nz));
            float qx = rx + nx * tq, qz = rz + nz * tq;
            float dx = px - qx, dy = oy, dz = pz - qz;
            float d2 = dx * dx + dy * dy + dz * dz;
            float rr = 1.0f + 0.75f;
            if (d2 < rr * rr && d2 > 1e-4f) {
                float d = j2me.FMath.sqrt(d2);
                px = qx + dx / d * rr;
                py = ry + dy / d * rr;
                pz = qz + dz / d * rr;
                if (dy > 0 && vy < 0) {
                    vy = 0;
                }
            }
            // the two side thruster pods stick out 1.6 m: short capsules of their own
            for (int ld = -1; ld <= 1; ld += 2) {
                float cx = rx - nz * 1.35f * ld - nx * 0.3f, cz = rz + nx * 1.35f * ld - nz * 0.3f;
                float qx2 = px - cx, qz2 = pz - cz;
                float tp = Math.max(-0.9f, Math.min(0.9f, qx2 * nx + qz2 * nz));
                float sx = cx + nx * tp, sz = cz + nz * tp;
                float ex = px - sx, ey = py - (ry - 0.1f), ez = pz - sz;
                float e2 = ex * ex + ey * ey + ez * ez;
                float re = 0.35f + 0.75f;
                if (e2 < re * re && e2 > 1e-4f) {
                    float d = j2me.FMath.sqrt(e2);
                    px = sx + ex / d * re;
                    py = ry - 0.1f + ey / d * re;
                    pz = sz + ez / d * re;
                }
            }
        }
        col[0] = px;
        col[1] = py;
        col[2] = pz;
        // the Remora is ~5 m long: it does not fit into caves or tunnels
        mundo.empujar(col, enRemora ? 1.8f : 0.7f);
        historia.empujar(col, enRemora ? 1.8f : 0.7f);
        depredadores.empujarErrante(col, enRemora ? 1.8f : 0.7f);
        if (hayBase) {
            // the habitat: a cylinder 6 m across and 4.8 m tall
            float r = 3.1f + (enRemora ? 1.8f : 0.6f);
            float dx = col[0] - baseX, dz = col[2] - baseZ;
            float top = baseY + 4.8f;
            float d2 = dx * dx + dz * dz;
            if (col[1] < top && d2 < r * r && d2 > 1e-4f) {
                float d = j2me.FMath.sqrt(d2);
                if (top - col[1] < r - d) {
                    col[1] = top;
                } else {
                    col[0] = baseX + dx / d * r;
                    col[2] = baseZ + dz / d * r;
                }
            }
            // the modules: smaller cylinders on each side
            for (int m = 0; m < 2; m++) {
                if (!inv.tiene[m == 0 ? Inventario.H_GRANJA : Inventario.H_RECARGA]) {
                    continue;
                }
                float mx = baseX + (m == 0 ? MOD_DX : -MOD_DX);
                float rm = 2.2f + (enRemora ? 1.8f : 0.6f);
                float ex = col[0] - mx, ez = col[2] - baseZ;
                float tm = baseY + 3.2f;
                float e2 = ex * ex + ez * ez;
                if (col[1] < tm && e2 < rm * rm && e2 > 1e-4f) {
                    float d = j2me.FMath.sqrt(e2);
                    if (tm - col[1] < rm - d) {
                        col[1] = tm;
                    } else {
                        col[0] = mx + ex / d * rm;
                        col[2] = baseZ + ez / d * rm;
                    }
                }
            }
        }
        depredadores.empujarCriaturas(col, enRemora ? 1.8f : 0.5f);
        hostiles.empujar(col, enRemora ? 1.8f : 0.5f);
        peces.empujar(col, enRemora ? 1.8f : 0.5f);
        px = col[0];
        pz = col[2];
        if (col[1] > py) {
            // resting on top of a rock or wreck: stop sinking into it instead of sliding off
            py = col[1];
            if (vy < 0) {
                vy = 0;
            }
        } else if (col[1] < py) {
            // a cave or tunnel roof from inside
            py = col[1];
            if (vy > 0) {
                vy = 0;
            }
        }
        supervivencia(dt);
        if (-py > maxProf) {
            maxProf = -py;
        }
        if (inv.tiene[Inventario.H_REMORA] && rx == 6 && rz == 7 && !enRemora && ry == -2.5f) {
            ry = Math.max(-4, Terreno.altura(rx, rz) + 2);
        }
        // world streaming and life
        mundo.actualizar(px, pz);
        // autosave every 5 minutes, never in danger (low air or health)
        autoT += dt;
        if (autoT > 300 && estado == JUEGO && o2 > 15 && salud > 30) {
            autoT = 0;
            if (Guardado.guardar(this)) {
                avisar("Partida guardada automáticamente");
            }
        }
        mundo.prepararCerca(px, pz);
        mapa.marcar(px, pz);
        mapa.preparar();
        peces.actualizar(dt, px, py, pz);
        depredadores.actualizar(dt, this);
        Peces.noche = luzDia() < 0.3f;
        hostiles.actualizar(dt, this);
        if (veneno > 0) {
            veneno -= dt;
            salud -= 2 * dt;

        }
        if (aturdido > 0) {
            aturdido -= dt;
        }
        if (ceguera > 0) {
            ceguera -= dt;
        }
        if (vision > 0) {
            vision -= dt;
        }
        if (mareo > 0) {
            mareo -= dt;
        }
        if (boya > 0) {
            boya -= dt;
            if (fuera) {
                boya = 0;
            }
        }
        if (bordoT > 0) {
            bordoT -= dt;
        }
        // the algae farm grows on its own, also while you are away or in the cabin
        if (hayBase && inv.tiene[Inventario.H_GRANJA] && cosecha < COSECHA_MAX) {
            granjaT += dt;
            if (granjaT >= CRECER) {
                granjaT = 0;
                cosecha++;
            }
        }
        if (golpeT > 0) {
            golpeT -= dt;
        }
        if (tajo > 0) {
            tajo -= dt;
            // the strike lands at 55% of the swing (end of the downward chop)
            if (golpePendiente && (TAJO - tajo) / TAJO >= 0.55f) {
                golpePendiente = false;
                ejecutarGolpe();
            }
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
        // hot vents: their plume scalds a diver (the Remora's hull shrugs it off)
        mundo.reloj = tiempo;
        if (calorT > 0) {
            calorT -= dt;
        }
        if (!enRemora && calorT <= 0 && (bio == Terreno.TERMAL) && mundo.cercaFumarola(px, py, pz)) {
            calorT = 0.8f;
            danar(6, "Te quema el agua hirviendo de una fumarola");
        }
        // the reef wanderer's parasites nip whoever gets right next to them
        if (!enRemora && calorT <= 0 && depredadores.parasitoCerca(px, py, pz)) {
            calorT = 3;
            danar(4, "Te pican los parasitos del errante");
        }
        // a spark ray: a shock that leaves you dizzy
        if (!enRemora && calorT <= 0 && peces.tocaRaya(px, py, pz)) {
            calorT = 2;
            aturdir(2);
            danar(4, "Te dio una descarga una raya chispa");
        }
        // an inflated puffer urchin pricks whoever touches it
        if (!enRemora && calorT <= 0 && peces.pinchaErizo(px, py, pz)) {
            calorT = 1.2f;
            danar(5, "Te pinchó un erizo globo");
        }
        // stinging thorns (checked a few times a second: it scans the props)
        if (!enRemora && calorT <= 0 && ((int) (tiempo * 10) & 1) == 0 && mundo.cercaEspino(px, py, pz)) {
            calorT = 0.9f;
            danar(5, "Te pico un espino urticante");
        }
        // the Heartbeat: near the Mother you hear it and the water trembles
        if (historia.latidoCerca(px, pz) && !fuera) {
            latidoT -= dt;
            if (latidoT <= 0) {
                latidoT = 3.2f;
                Sonido.tocar(Sonido.LATIDO);
            }
        }
        // the Remora's weapon: R fires an electric defence pulse (8 s to recharge)
        if (pulsoCarga > 0) {
            pulsoCarga -= dt;
        }
        if (destello > 0) {
            destello -= dt;
        }
        if (alarmaT > 0) {
            alarmaT -= dt;
        }
        // the Remora's sonar pings every few seconds while you drive it
        if (enRemora && !fuera) {
            sonarT -= dt;
            if (sonarT <= 0) {
                sonarT = 6;
                Sonido.tocar(Sonido.SONAR);
            }
        }
        usarEquipo(in, dt);
        if (!impulsando && impCarga < 100) {
            impCarga = Math.min(100, impCarga + (impBloq ? 7 : 5) * dt);
            if (impBloq && impCarga >= 40) {
                impBloq = false;
                avisar("Impulsor listo");
            }
        }
        actualizarBengalas(dt);
        if (chispaT > 0) {
            chispaT -= dt;
        }
        if (varaT > 0) {
            varaT -= dt;
        }
        if (descargaT > 0) {
            descargaT -= dt;
        }
        // keep the weapon slot on something you actually have
        if (!armaEnMano()) {
            arma = inv.tiene[Inventario.H_CUCHILLO] ? ARMA_CUCHILLO
                    : (inv.tiene[Inventario.H_VARA] ? ARMA_VARA : (inv.tiene[Inventario.H_ARPON] ? ARMA_ARPON : arma));
        }
        actualizarArpon(dt);
        if (enRemora && in.pulso(Entrada.R)) {
            if (pulsoCarga > 0) {
                avisar("Pulso de defensa recargando...");
            } else {
                pulsoCarga = 8;
                destello = 0.35f;
                int n = depredadores.pulso(px, py, pz, 12) + hostiles.pulso(px, py, pz, 12);
                peces.asustar(px, py, pz, 14);
                Sonido.tocar(Sonido.DESCARGA);
                avisar(n > 0 ? "Pulso de defensa! (" + n + " alcanzados)" : "Pulso de defensa");
            }
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
            // one devlog call for the whole block: a single write instead of three
            int gcN = j2me.Pad.gcInfo(0), gcMs = j2me.Pad.gcInfo(1), gcMax = j2me.Pad.gcInfo(2),
                    gcKb = j2me.Pad.gcInfo(3);
            StringBuffer sb = new StringBuffer(400);
            sb.append("estado: fps ").append(fps).append(", pos ").append((int) px).append(',').append((int) py)
                    .append(',').append((int) pz).append(", ").append(Mundo.nombreBioma(bio)).append(", trozos ")
                    .append(mundo.nChunks).append(", tris ").append(mundo.trisDibujados).append(", objetos ")
                    .append(mundo.propsDibujados).append(", peces ").append(peces.dibujados).append(", O2 ")
                    .append((int) o2).append(", ").append(Log.memoria());
            if (nTiempos > 0) {
                sb.append("\n    tiempos por frame (ms): logica ").append(sumLogica / nTiempos).append(", 3D ")
                        .append(sum3d / nTiempos).append(", 2D ").append(sum2d / nTiempos)
                        .append(", esperar GPU+vsync ").append(sumPresentar / nTiempos)
                        .append(", trozos construidos ").append(mundo.construidos);
            }
            sb.append("\n    picos: peor frame ").append(peorFrame).append(" ms (logica ").append(peorLogica)
                    .append("), frames lentos ").append(lentos).append("; gc ").append(gcN - gcCuenta0)
                    .append(" veces, ").append(gcMs - gcMs0).append(" ms, pausa max ").append(gcMax)
                    .append(" ms, asignado ").append(gcKb - gcKb0).append(" KB");
            sb.append("\n    trozos, pasó más largo por fase (ms): alturas ").append(mundo.pasoMax[0])
                    .append(", malla ").append(mundo.pasoMax[1]).append(", subir ").append(mundo.pasoMax[2])
                    .append(", objetos ").append(mundo.pasoMax[3]).append(" | subir:");
            for (int q = 0; q < 6; q++) {
                sb.append(' ').append(MB.tPaso[q]);
                MB.tPaso[q] = 0;
            }
            for (int q = 0; q < 4; q++) {
                mundo.pasoMax[q] = 0;
            }
            Log.d(sb.toString());
            gcCuenta0 = gcN;
            gcMs0 = gcMs;
            gcKb0 = gcKb;
            peorFrame = peorLogica = 0;
            lentos = 0;
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
            if (enRemora) {
                // the sub has its own air supply
                o2 = Math.min(o2Max(), o2 + O2_RECARGA * dt);
                if (-py > remoraMax()) {
                    danarCasco(4 * dt);
                    if (((int) (tiempo * 2) & 3) == 0) {
                        avisar("Casco de la Rémora crujiendo: sube a menos profundidad");
                        alarma();
                    }
                }
            } else {
                o2 -= o2Infinito ? 0 : dt;
            }
            if (o2 < 10 && o2 + dt >= 10) {
                avisar("Oxígeno bajo. Sube a la superficie.");
            }
            if (o2 < 10 && !enRemora) {
                alarma();
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
            causaMuerte = causaReal();
            estado = MUERTO;
            muertes++;
            Log.d("el jugador murio (muerte " + muertes + ") en " + (int) px + "," + (int) py + "," + (int) pz
                    + (causaMuerte != null ? ": " + causaMuerte : ""));
        }
    }

    /** the first frame after crossing the surface is logged too (freeze diagnosis) */
    private boolean registrarFrame;

    private void transicion(String que) {
        // log first: if anything after this hangs, the devlog says how far it got
        Log.d("superficie: " + que + " (y " + py + ")");
        registrarFrame = true;
        Sonido.tocar(Sonido.BURBUJAS);
    }

    boolean cercaRemora() {
        float dx = rx - px, dy = ry - py, dz = rz - pz;
        return dx * dx + dy * dy + dz * dz < 4.5f * 4.5f;
    }

    /** progress checklist shown in the PDA */
    String[] objetivos() {
        return new String[] { "MISIÓN: " + historia.mision(),
                (inv.tiene[Inventario.H_CUCHILLO] ? "OK " : "-  ") + "Fabricar un cuchillo",
                (ecos.hallados() == Ecos.N ? "OK " : "-  ") + "Ecos de datos de la tripulación " + ecos.hallados() + "/"
                        + Ecos.N,
                (inv.tiene[Inventario.H_LINTERNA] ? "OK " : "-  ") + "Fabricar una linterna",
                (inv.tiene[Inventario.H_REMORA] ? "OK " : "-  ") + "Construir la Rémora",
                (maxProf >= 460 ? "OK " : "-  ") + "Descender a la fosa abisal (460 m)" };
    }

    float maxProf;

    int registrosLeidos() {
        int n = 0;
        while (n < REGISTROS.length && mundo.recogidos.get("R" + n) != null) {
            n++;
        }
        return n;
    }

    private void dibujarRegistro(Graphics g) {
        String[] r = REGISTROS[leyendo];
        panel(g, 30, 70, 420, 120);
        g.setColor(0x7fe8f4);
        g.drawString(r[0], 44, 80, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xffffff);
        for (int i = 1; i < r.length; i++) {
            g.drawString(r[i], 44, 98 + (i - 1) * 16, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0x9fc4cc);
        g.drawString("X / O cerrar   <- -> otros registros", 436, 174, Graphics.TOP | Graphics.RIGHT);
    }

    /** damage from a creature */
    private float alarmaT, sonarT, calorT;

    /** the warning beeps, at most once every 2.5 s */
    private void alarma() {
        if (alarmaT <= 0) {
            alarmaT = 2.5f;
            Sonido.tocar(Sonido.ALARMA);
        }
    }

    void danar(float cant, String causa) {
        if (estado != JUEGO) {
            return;
        }
        if (enRemora) {
            // aboard, the hull takes the blow, not the diver
            danarCasco(cant * 0.8f);
            golpeT = 0.3f;
            avisar(causa + " (casco -" + (int) (cant * 0.8f) + "%)");
            Sonido.tocar(Sonido.GOLPE);
            return;
        }
        salud -= cant;
        golpeT = 0.45f;
        causaMuerte = causa;
        causaT = tiempo;
        avisar(causa + " (-" + (int) cant + ")");
        Sonido.tocar(Sonido.MORDIDA);
    }

    private void danarCasco(float d) {
        float antes = rCasco;
        rCasco = Math.max(0, rCasco - d);
        if (antes >= 30 && rCasco < 30) {
            avisar("Casco de la Rémora crítico!");
            alarma();
        }
        if (rCasco <= 0 && enRemora) {
            // it breaks down: the diver is thrown out
            rAveriada = true;
            enRemora = false;
            rx = px;
            ry = py;
            rz = pz;
            rRumbo = yaw;
            px += j2me.FMath.cos(yaw * j2me.FMath.RAD) * 2.5f;
            pz -= j2me.FMath.sin(yaw * j2me.FMath.RAD) * 2.5f;
            heliceVel = 0;
            avisar("La Rémora se ha averiado! Repárala con un kit de reparación");
            Sonido.tocar(Sonido.ALARMA);
            Log.d("remora averiada");
        }
    }

    private void interactuar() {
        // the Remora: get out, or get in when parked nearby (no knife swing for this)
        if (enRemora) {
            enRemora = false;
            rx = px;
            ry = py;
            rz = pz;
            rRumbo = yaw;
            px += j2me.FMath.cos(yaw * j2me.FMath.RAD) * 2.5f;
            pz -= j2me.FMath.sin(yaw * j2me.FMath.RAD) * 2.5f;
            avisar("Sales de la Rémora");
            Log.d("jugador sale de la remora");
            heliceVel = 400; // still spinning fast when you step out
            return;
        }
        if (inv.tiene[Inventario.H_REMORA] && cercaRemora() && rAveriada) {
            if (inv.cant[Inventario.KIT] > 0) {
                inv.cant[Inventario.KIT]--;
                rAveriada = false;
                rCasco = 100;
                avisar("Rémora reparada (casco 100%)");
                Sonido.tocar(Sonido.FABRICAR);
            } else {
                avisar("Rémora averiada: necesitas un kit de reparación");
            }
            return;
        }
        if (inv.tiene[Inventario.H_REMORA] && cercaRemora()) {
            enRemora = true;
            px = rx;
            py = ry;
            pz = rz;
            yaw = rRumbo;
            vx = vy = vz = 0;
            avisar("A bordo de la Rémora. [] para salir");
            bordoT = 5;
            Log.d("jugador entra en la remora");
            Sonido.tocar(Sonido.MOTOR);
            Sonido.tocar(Sonido.RECOGER);
            return;
        }
        // an air bladder: a few breaths of air, then it needs time to refill
        if (mundo.vejigaCerca(px, py, pz, sel)) {
            float falta = mundo.vejigaFalta(sel[0], sel[1]);
            if (falta > 0) {
                avisar("La vejiga se esta rellenando (" + (int) (falta + 1) + " s)");
            } else {
                o2 = Math.min(o2Max(), o2 + 20);
                mundo.vaciarVejiga(sel[0], sel[1]);
                Sonido.tocar(Sonido.BURBUJAS);
                for (int q = 0; q < 8; q++) {
                    emitirBurbuja(px + fx * 0.8f + (Terreno.azar(q, burSemilla, 21) - 0.5f) * 0.8f, py - 0.3f,
                            pz + fz * 0.8f + (Terreno.azar(q, burSemilla, 22) - 0.5f) * 0.8f, 2 + q * 0.3f);
                }
                burSemilla++;
                avisar("Respiras de la vejiga: +20 s de aire");
            }
            return;
        }
        // a data echo: its recording
        int eco = ecos.cerca(px, py, pz);
        if (eco >= 0) {
            ecoTexto = ecos.abrir(this, eco);
            estado = ECO;
            Sonido.tocar(Sonido.SONAR);
            avisar("Eco de datos " + ecos.hallados() + "/" + Ecos.N + ": su contenido esta en tu mochila");
            return;
        }
        // a supply crate: open it
        if (mundo.cajaCerca(px, py, pz, sel)) {
            abrirCaja(sel[0], sel[1]);
            return;
        }
        // one of your signal beacons: pick it up again
        int sb = balizaCerca();
        if (sb >= 0) {
            if (inv.meter(Inventario.SENAL)) {
                sbPuesta[sb] = false;
                avisar("Recogiste la baliza de señal");
                Sonido.tocar(Sonido.RECOGER);
            } else {
                avisar("Mochila llena: tira algo desde la PDA");
            }
            return;
        }
        // station wreckage: read the next crew log hidden in it (once per piece)
        String resto = mundo.restoCercano(px, py, pz, 4.5f);
        if (resto != null && mundo.recogidos.get(resto) == null) {
            mundo.recogidos.put(resto, Boolean.TRUE);
            int n = registrosLeidos();
            if (n < REGISTROS.length) {
                mundo.recogidos.put("R" + n, Boolean.TRUE);
                leyendo = n;
                estado = LEYENDO;
                Sonido.tocar(Sonido.RECOGER);
                Log.d("registro encontrado: " + (n + 1));
            } else {
                avisar("Solo chatarra. Ya tienes todos los registros.");
            }
            return;
        }
        // mission places (Container 7, module Tetis, the resonant crystal)
        if (historia.interactuar(this)) {
            return;
        }
        // the capsule hatch (or the base's): air, save and the fabricator
        if ((px * px + pz * pz < 5.5f * 5.5f && py > -4) || cercaBase()) {
            if (cercaBase()) {
                alEntrarBase();
            }
            o2 = o2Max();
            // the save is written a few frames later, while the still cabin is on screen
            interior.guardarEn = 3;
            Sonido.tocar(Sonido.ESCOTILLA);
            estado = INTERIOR;
            enCapsula = true;
            linterna = false;
            tajo = 0;
            golpePendiente = false;
            interior.entrar();
            return;
        }
        // catching a small fish with the hand: no knife swing. With a full backpack the
        // hand cannot catch it, so the swing goes ahead (the knife still works)
        int f = peces.alcanzable(px, py, pz);
        if (f >= 0 && inv.total() < inv.capacidad()) {
            int item = Peces.CAPTURA[peces.especie[f]];
            inv.meter(item);
            peces.vivo[f] = false;
            accionIzq(ACC_AGARRAR, 0.45f);
            avisar("Atrapaste: " + Inventario.NOMBRE[item]);
            Sonido.tocar(Sonido.ATRAPAR);
            return;
        }
        // the harpoon fires instead of swinging (unless there is something to pick up)
        if (arma == ARMA_ARPON && armaEnMano() && !mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
            dispararArpon();
            return;
        }
        // from here on the hand swings: the effect happens at the moment of impact
        if (tajo <= 0) {
            tajo = TAJO;
            tajoLado = -tajoLado; // forehand, backhand, forehand...
            golpePendiente = true;
            if (armaEnMano()) {
                Sonido.tocar(Sonido.TAJO);
            }
        }
    }

    /** the knife (or the hand) reaches its target: cut, hit or harvest what is in front */
    private void ejecutarGolpe() {
        // coral from the reef wanderer's back: only found there (creatures in front are
        // attacked first: the lurkers and parasites living up there)
        boolean vivoDelante = depredadores.alAlcance(px, py, pz, fx, fy, fz)
                || hostiles.alAlcance(px, py, pz, fx, fy, fz) || peces.alAlcance(px, py, pz, fx, fy, fz) >= 0;
        if (!vivoDelante && depredadores.sobreErrante(px, py, pz)) {
            if (!cuchilloEnMano()) {
                avisar(inv.tiene[Inventario.H_CUCHILLO] ? "Equipa el cuchillo (PDA, L) para cortar su coral"
                        : "Necesitas el cuchillo para cortar su coral");
            } else if (depredadores.eCoral <= 0) {
                avisar("Ya no queda coral sano en su lomo");
            } else if (inv.meter(Inventario.CORAL_E)) {
                depredadores.eCoral--;
                avisar("+1 Coral del errante");
                Sonido.tocar(Sonido.CORTAR);
                golpeCam = 0.15f;
                reboteT = 0.15f;
            } else {
                avisar("Mochila llena: tira algo desde la PDA");
            }
            return;
        }
        // the discharge rod: a shock just in front, stuns and hurts everything it touches
        if (arma == ARMA_VARA && inv.tiene[Inventario.H_VARA]) {
            boolean criatura = depredadores.alAlcance(px, py, pz, fx, fy, fz)
                    || hostiles.alAlcance(px, py, pz, fx, fy, fz) || peces.alAlcance(px, py, pz, fx, fy, fz) >= 0;
            // with nothing alive in front, the rod still picks up loose things (rocks, ore)
            if (criatura || !mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
                if (varaT > 0) {
                    if (criatura) {
                        avisar("Vara recargando...");
                    }
                    return;
                }
                varaT = 1.4f;
                float cx = px + fx * 1.6f, cy = py + fy * 1.6f, cz = pz + fz * 1.6f;
                int n = depredadores.pulso(cx, cy, cz, 2.6f) + hostiles.pulso(cx, cy, cz, 2.6f);
                descargaX = cx;
                descargaY = cy;
                descargaZ = cz;
                descargaT = 0.3f;
                destello = 0.1f;
                Sonido.tocar(Sonido.DESCARGA);
                golpeCam = 0.15f;
                reboteT = 0.15f;
                boolean pez = atacarFauna(); // the fish in front takes the shock too
                peces.asustar(cx, cy, cz, 6);
                if (n > 0) {
                    avisar("Descarga! " + (n == 1 ? "La criatura queda aturdida" : n + " criaturas aturdidas"));
                } else if (!pez && criatura) {
                    avisar("Descarga!");
                }
                return;
            }
        }
        // knife attack on a creature in front
        if (arma == ARMA_CUCHILLO && inv.tiene[Inventario.H_CUCHILLO]) {
            String m = depredadores.golpear(this, px, py, pz, fx, fy, fz);
            if (m == null) {
                m = hostiles.golpear(this, px, py, pz, fx, fy, fz);
            }
            if (m != null) {
                avisar(m);
                Sonido.tocar(Sonido.GOLPE);
                golpeCam = 0.15f; // a small kick of the view, only when the blade connects
                reboteT = 0.15f;
                return;
            }
            if (atacarFauna()) {
                golpeCam = 0.15f;
                reboteT = 0.15f;
                return;
            }
        }
        if (mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
            int tipo = mundo.chunks[sel[0]].tipo[sel[1]];
            int item = Mundo.DA[tipo];
            if (Mundo.CUCHILLO[tipo] && !cuchilloEnMano()) {
                avisar(inv.tiene[Inventario.H_CUCHILLO] ? "Equipa el cuchillo (PDA, L) para cortar esto"
                        : "Necesitas un cuchillo para cortar esto");
                return;
            }
            if (inv.meter(item)) {
                mundo.recoger(sel[0], sel[1]);
                String extra = "";
                if (tipo == Mundo.P_VAINAS && inv.meter(Inventario.BULBO)) {
                    extra = ", +1 Bulbo de alga";
                }
                avisar("+1 " + Inventario.NOMBRE[item] + extra);
                boolean roca = tipo == Mundo.P_COBRE || tipo == Mundo.P_CUARZO || tipo == Mundo.P_ROCA
                        || tipo == Mundo.P_CRISTALT || tipo == Mundo.P_OPALO;
                Sonido.tocar(roca ? Sonido.MINERAL : Sonido.CORTAR);
                golpeCam = 0.1f;
            } else {
                avisar("Mochila llena: tira algo desde la PDA");
            }
        }
    }

    private static final String[] NOMBRE_PEZ = { "burbujín", "farolillo", "lazofín", "planeador", "medusa",
            "cardumen", "pastor de arena", "manta de cristal", "caracol de duna", "enjambre de camarones", "tortuga coralina", "pez candil", "flotador",
            "erizo globo", "raya chispa" };

    /** knife on the peaceful fauna: most flee, the ones with stingers or horns hit back */
    private boolean atacarFauna() {
        int f = peces.golpear(px, py, pz, fx, fy, fz);
        if (f < 0) {
            return false;
        }
        int e = peces.especie[f];
        boolean muerto = !peces.vivo[f];
        Sonido.tocar(Sonido.GOLPE);
        if (e == 4) {
            // jelly: its stinging tentacles burn the attacker on every hit
            danar(7, "Te quemó una medusa");
            avisar(muerto ? "Cortaste la medusa, pero te quemó" : "La medusa te pica al defenderse!");
        } else if (e == 6) {
            // the shepherd is big and horned: it rams back while it still can
            if (!muerto) {
                danar(10, "Te embistió un pastor de arena");
                avisar("El pastor de arena te embiste con sus cuernos!");
            } else {
                avisar("Abatiste al pastor de arena");
            }
        } else if (e == 12) {
            o2 = Math.min(o2Max(), o2 + 15);
            avisar("Reventaste un flotador: +15 s de aire");
            Sonido.tocar(Sonido.BURBUJAS);
        } else if (muerto && e == 0 && inv.meter(Inventario.PEZ)) {
            avisar("+1 " + Inventario.NOMBRE[Inventario.PEZ]);
        } else {
            avisar(muerto ? "Abatiste un " + NOMBRE_PEZ[e] : "El " + NOMBRE_PEZ[e] + " huye herido");
        }
        Log.d("cuchillazo a fauna: " + NOMBRE_PEZ[e] + (muerto ? " muere" : " herido"));
        return true;
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
        avisar("Despiertas junto a la cápsula. Perdiste parte del inventario.");
        Log.d("reaparicion en la capsula");
    }

    // ------------------------------------------------------------------ PDA
    /** PDA list: the items you carry (0..N-1) then the tools you own (1000 + tool) */
    private final int[] listaPda = new int[Inventario.N + Inventario.NH];
    private int nPda, posPda;
    /** the small action menu over the selected entry (-1 = closed) */
    private int menuPda = -1;
    private final String[] menuOp = new String[5];
    private final int[] menuAcc = new int[5];
    private int nMenu;
    static final int A_USAR = 0, A_EQUIPAR = 1, A_TIRAR = 2, A_CANCELAR = 3, A_GUARDAR = 4, A_LINTERNA = 5;

    private void listarPda() {
        nPda = 0;
        for (int i = 0; i < Inventario.N; i++) {
            if (inv.cant[i] > 0) {
                listaPda[nPda++] = i;
            }
        }
        for (int h = 0; h < Inventario.NH; h++) {
            if (inv.tiene[h]) {
                listaPda[nPda++] = 1000 + h;
            }
        }
        if (posPda >= nPda) {
            posPda = Math.max(0, nPda - 1);
        }
        selPda = nPda > 0 ? listaPda[posPda] : -1;
    }

    /** what "use" means for an item, or null */
    private static String usoDe(int i) {
        if (i == Inventario.AGUA) {
            return "Beber";
        }
        if (i == Inventario.PEZ || i == Inventario.PEZ_COCIDO || i == Inventario.SOPA
                || (i >= Inventario.LAZOFIN && i <= Inventario.HONGO)) {
            return "Comer";
        }
        if (i == Inventario.BOTIQUIN) {
            return "Curar";
        }
        if (i == Inventario.BALIZA) {
            return "Lanzar (en la superficie)";
        }
        if (i == Inventario.HABITAT || i == Inventario.SENAL) {
            return "Colocar aquí";
        }
        return null;
    }

    /** the hand-equipment slot an entry goes to (EQ_*), or -1 */
    private static int equipoDe(int c) {
        if (c == Inventario.BOYA) {
            return EQ_BOYA;
        }
        if (c == Inventario.BENGALA) {
            return EQ_BENGALA;
        }
        if (c == 1000 + Inventario.H_SOLDADOR) {
            return EQ_SOLDADOR;
        }
        if (c == 1000 + Inventario.H_LINTERNA) {
            return EQ_LINTERNA;
        }
        if (c == 1000 + Inventario.H_ESCANER) {
            return EQ_ESCANER;
        }
        if (c == 1000 + Inventario.H_IMPULSOR) {
            return EQ_IMPULSOR;
        }
        return -1;
    }

    /** the weapon an entry is (ARMA_*), or -1 */
    private static int armaDe(int c) {
        return c == 1000 + Inventario.H_CUCHILLO ? ARMA_CUCHILLO
                : (c == 1000 + Inventario.H_VARA ? ARMA_VARA : (c == 1000 + Inventario.H_ARPON ? ARMA_ARPON : -1));
    }

    private void opcion(String s, int acc) {
        menuOp[nMenu] = s;
        menuAcc[nMenu++] = acc;
    }

    private void abrirMenuPda() {
        int c = selPda;
        nMenu = 0;
        if (c < 0) {
            return;
        }
        int eq = equipoDe(c), ar = armaDe(c);
        if (c < 1000 && usoDe(c) != null) {
            opcion(usoDe(c), A_USAR);
        }
        if (eq >= 0) {
            opcion(equipo == eq ? "Guardar (mano izq.)" : "Equipar en la mano izq.", equipo == eq ? A_GUARDAR : A_EQUIPAR);
        }
        if (ar >= 0 && !(arma == ar && armaEnMano())) {
            opcion("Equipar en la mano der.", A_EQUIPAR);
        }
        if (c < 1000 && !Inventario.deHistoria(c)) {
            opcion("Tirar uno", A_TIRAR);
        }
        if (nMenu == 0) {
            avisar(c >= 1000 ? (ar >= 0 ? "Ya la tienes en la mano derecha" : "Herramienta pasiva: siempre funciona")
                    : "Es para la baliza: no se puede tirar");
            return;
        }
        opcion("Cancelar", A_CANCELAR);
        menuPda = 0;
        Sonido.tocar(Sonido.CLICK);
    }

    private void ejecutarMenuPda(int acc) {
        int c = selPda;
        menuPda = -1;
        switch (acc) {
        case A_USAR:
            usarObjeto();
            break;
        case A_EQUIPAR:
            if (armaDe(c) >= 0) {
                arma = armaDe(c);
                avisar("Mano derecha: " + ARMA_NOMBRE[arma]);
                Sonido.tocar(Sonido.CLICK);
            } else {
                equipar(equipoDe(c));
            }
            break;
        case A_GUARDAR:
            equipar(EQ_NADA);
            break;
        case A_TIRAR:
            inv.cant[c]--;
            avisar("Tiraste 1 " + Inventario.NOMBRE[c]);
            break;
        default:
            break;
        }
    }

    private void actualizarPda(Entrada in) {
        listarPda();
        if (menuPda >= 0) {
            // the action menu: up/down, X choose, O close
            if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
                menuPda = -1;
                return;
            }
            if (in.pulso(Entrada.ABAJO)) {
                menuPda = (menuPda + 1) % nMenu;
            }
            if (in.pulso(Entrada.ARRIBA)) {
                menuPda = (menuPda + nMenu - 1) % nMenu;
            }
            if (in.pulso(Entrada.X) || in.pulso(Entrada.CUADRADO)) {
                ejecutarMenuPda(menuAcc[menuPda]);
            }
            return;
        }
        if (in.pulso(Entrada.TRIANGULO) || in.pulso(Entrada.CIRCULO)) {
            estado = enCapsula ? INTERIOR : JUEGO;
            return;
        }
        if (nPda > 0 && in.pulso(Entrada.ABAJO)) {
            posPda = (posPda + 1) % nPda;
        }
        if (nPda > 0 && in.pulso(Entrada.ARRIBA)) {
            posPda = (posPda + nPda - 1) % nPda;
        }
        selPda = nPda > 0 ? listaPda[posPda] : -1;
        if (in.pulso(Entrada.R)) {
            if (registrosLeidos() > 0) {
                leyendo = 0;
                estado = LEYENDO;
            } else {
                avisar("Aún no has encontrado registros (busca restos de la estación)");
            }
            return;
        }
        if (in.pulso(Entrada.L)) {
            estado = BESTIARIO;
            Sonido.tocar(Sonido.PDA);
            return;
        }
        if (in.pulso(Entrada.CUADRADO)) {
            estado = MAPA;
            Sonido.tocar(Sonido.PDA);
            return;
        }
        if (in.pulso(Entrada.X)) {
            abrirMenuPda();
        }
    }

    /** "use" on the selected item (eat, drink, heal, launch, place) */
    private void usarObjeto() {
        if (selPda < 0 || selPda >= 1000 || inv.cant[selPda] <= 0) {
            return;
        }
        if (selPda == Inventario.PEZ) {
            inv.cant[selPda]--;
            comida = Math.min(100, comida + 14);
            agua = Math.min(100, agua + 4);
            avisar("Comiste un burbujín crudo (+14 comida, +4 agua)");
            Sonido.tocar(Sonido.COMER);
        } else if (selPda == Inventario.LAZOFIN) {
            comer(8, 12, 0, "Lazofín crudo: jugoso (+8 comida, +12 agua)");
        } else if (selPda == Inventario.FAROLILLO) {
            comer(10, 2, 0, "Farolillo crudo: tus ojos brillan... visión nocturna");
            vision = 90;
        } else if (selPda == Inventario.CARACOL) {
            comer(18, -6, 0, "Caracol de duna: muy salado (+18 comida, -6 agua)");
        } else if (selPda == Inventario.CAMARONES) {
            comer(16, 0, -8, "Camarones igneos crudos: pican! (+16 comida, -8 salud)");
        } else if (selPda == Inventario.CAMARONES_ASADOS) {
            comer(28, 0, 0, "Camarones asados (+28 comida)");
        } else if (selPda == Inventario.CANDIL) {
            comer(12, 2, 8, "Pez candil: reconfortante (+12 comida, +8 salud)");
        } else if (selPda == Inventario.FRUTO) {
            comer(6, 12, 0, "Fruto de lirio (+6 comida, +12 agua)");
        } else if (selPda == Inventario.HONGO) {
            comer(4, 0, 25, "Hongo de cueva: cura... pero todo da vueltas");
            mareo = 20;
        } else if (selPda == Inventario.PEZ_COCIDO) {
            inv.cant[selPda]--;
            comida = Math.min(100, comida + 30);
            agua = Math.min(100, agua + 2);
            avisar("Comiste un burbujín cocido (+30 comida)");
            Sonido.tocar(Sonido.COMER);
        } else if (selPda == Inventario.HABITAT) {
            colocarBase();
        } else if (selPda == Inventario.SENAL) {
            colocarBaliza();
        } else if (selPda == Inventario.SOPA) {
            comer(25, 25, 5, "Sopa de algas: caliente y reconfortante (+25 comida, +25 agua)");
        } else if (selPda == Inventario.BOTIQUIN) {
            inv.cant[selPda]--;
            salud = Math.min(100, salud + 50);
            avisar("Usaste un botiquín (+50 salud)");
            Sonido.tocar(Sonido.RECOGER);
        } else if (selPda == Inventario.BALIZA) {
            if (fuera) {
                inv.cant[selPda]--;
                historia.pantalla = Historia.FINAL;
                historia.avanzar(this, 6);
                estado = FINAL;
                Sonido.tocar(Sonido.DESCARGA);
                Log.d("historia: baliza lanzada - FINAL");
                return;
            }
            avisar("Lanza la baliza desde la superficie");
        } else if (selPda == Inventario.AGUA) {
            inv.cant[selPda]--;
            agua = Math.min(100, agua + 30);
            avisar("Bebiste agua filtrada (+30 agua)");
            Sonido.tocar(Sonido.COMER);
        }
    }

    /** test travel: jump to a point inside the given biome, a few metres above the floor */
    void viajar(int zona) {
        if (zona == SENAL) {
            // test: next to the current mission signal
            if (historia.objetivo() < 0) {
                avisar("No hay ninguna señal activa");
                return;
            }
            float lejosObj = historia.objetivo() == Historia.MADRE ? 42 : 12;
            px = historia.objX() + lejosObj;
            pz = historia.objZ() + lejosObj;
            py = Math.min(-4, Math.max(Terreno.altura(px, pz) + 4, historia.objY() - 10));
            vx = vy = vz = 0;
            mundo.cargarTodo(px, pz);
            avisar("Viaje de prueba: junto a la señal");
            Log.d("prueba: viaje a la senal (" + (int) px + "," + (int) py + "," + (int) pz + ")");
            return;
        }
        // the regional biomes live inside a ring: search that ring
        int anillo = zona == Terreno.PRADERA ? Terreno.BOSQUE : (zona == Terreno.TERMAL ? Terreno.CANONES : zona);
        float r0 = anillo == 0 ? 40 : Terreno.LIMITE[anillo - 1];
        float r1 = Terreno.LIMITE[anillo];
        for (int intento = 0; intento < 48; intento++) {
            float ang = (yaw + intento * 37 + (intento > 20 ? 11 : 0)) * j2me.FMath.RAD;
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
        avisar("No se encontró un punto en " + Mundo.nombreBioma(zona));
    }

    /** fabricator tab and the recipes shown in it */
    int tabFab;
    private final int[] listaFab = new int[Inventario.RECETAS.length];

    private int recetasDeTab() {
        int n = 0;
        for (int i = 0; i < Inventario.RECETAS.length; i++) {
            if (Inventario.RECETAS[i].cat == tabFab) {
                listaFab[n++] = i;
            }
        }
        return n;
    }

    private void actualizarFabricador(Entrada in) {
        if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO)) {
            estado = enCapsula ? INTERIOR : JUEGO;
            return;
        }
        int nt = Inventario.CATEGORIAS.length;
        if (in.pulso(Entrada.R) || in.pulso(Entrada.DER)) {
            tabFab = (tabFab + 1) % nt;
            selFab = 0;
            Sonido.tocar(Sonido.CLICK);
        }
        if (in.pulso(Entrada.L) || in.pulso(Entrada.IZQ)) {
            tabFab = (tabFab + nt - 1) % nt;
            selFab = 0;
            Sonido.tocar(Sonido.CLICK);
        }
        int n = recetasDeTab();
        if (n == 0) {
            return;
        }
        if (in.pulso(Entrada.ABAJO)) {
            selFab = (selFab + 1) % n;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            selFab = (selFab + n - 1) % n;
        }
        selFab = Math.min(selFab, n - 1);
        if (in.pulso(Entrada.X)) {
            Inventario.Receta r = Inventario.RECETAS[listaFab[selFab]];
            if (inv.fabricar(r)) {
                avisar("Fabricado: " + r.nombre);
                if (r.herramienta == Inventario.H_TANQUE || r.herramienta == Inventario.H_TANQUE2) {
                    o2 = o2Max();
                }
                Log.d("fabricado: " + r.nombre);
                Sonido.tocar(Sonido.FABRICAR);
                // a new hand tool goes straight into the empty left hand
                int eq = equipoDe(r.herramienta >= 0 ? 1000 + r.herramienta : r.item);
                if (eq >= 0 && equipo == EQ_NADA) {
                    equipo = eq;
                    avisar("Fabricado: " + r.nombre + " (en la mano: L para usar)");
                }
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
            selPausa = (selPausa + 1) % 8;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            selPausa = (selPausa + 7) % 8;
        }
        if (selPausa == 2 && (in.pulso(Entrada.DER) || in.pulso(Entrada.IZQ))) {
            // order: shallows, kelp, meadows, plateau, canyons, rifts, abyss
            int pos = 0;
            for (int q = 0; q < ORDEN_VIAJE.length; q++) {
                if (ORDEN_VIAJE[q] == zonaViaje) {
                    pos = q;
                }
            }
            pos = (pos + (in.pulso(Entrada.DER) ? 1 : ORDEN_VIAJE.length - 1)) % ORDEN_VIAJE.length;
            zonaViaje = ORDEN_VIAJE[pos];
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
            } else if (selPausa == 5) {
                int[] dar = { Inventario.TITANIO, Inventario.COBRE, Inventario.CUARZO, Inventario.FIBRA,
                        Inventario.CORAL, Inventario.BULBO };
                for (int i = 0; i < dar.length; i++) {
                    for (int k = 0; k < 4; k++) {
                        inv.meter(dar[i]);
                    }
                }
                inv.tiene[Inventario.H_CUCHILLO] = true;
                inv.tiene[Inventario.H_LINTERNA] = true;
                inv.tiene[Inventario.H_TANQUE] = true;
                inv.tiene[Inventario.H_ALETAS] = true;
                inv.tiene[Inventario.H_RADAR] = true;
                inv.cant[Inventario.KIT]++;
                inv.cant[Inventario.CRISTAL_T] += 3;
                inv.cant[Inventario.LAMINA] += 2;
                inv.cant[Inventario.CIRCUITO] += 1;
                inv.cant[Inventario.BATERIA] += 1;
                inv.cant[Inventario.VIDRIO] += 1;
                inv.cant[Inventario.HABITAT] += 1;
                // 0.21: the hand equipment, the new weapon and the base modules
                inv.tiene[Inventario.H_SOLDADOR] = true;
                inv.tiene[Inventario.H_IMPULSOR] = true;
                inv.tiene[Inventario.H_VARA] = true;
                inv.tiene[Inventario.H_MOCHILA] = true; // 48 slots: the test kit fits
                inv.tiene[Inventario.H_ESCANER] = true;
                inv.tiene[Inventario.H_ARPON] = true;
                inv.cant[Inventario.SENAL] += 2;
                inv.tiene[Inventario.H_GRANJA] = true;
                inv.tiene[Inventario.H_RECARGA] = true;
                inv.cant[Inventario.BOYA] += 2;
                inv.cant[Inventario.BENGALA] += 3;
                rCasco = 45; // a damaged hull, to try the welder
                // and the Remora, parked a few metres ahead
                inv.tiene[Inventario.H_REMORA] = true;
                rx = px - j2me.FMath.sin(yaw * j2me.FMath.RAD) * 3;
                rz = pz - j2me.FMath.cos(yaw * j2me.FMath.RAD) * 3;
                ry = Math.max(Terreno.altura(rx, rz) + 1.5f, Math.min(-3, py));
                rRumbo = yaw;
                avisar("Prueba: materiales, herramientas y la Rémora");
                Log.d("prueba: materiales anadidos");
            } else if (selPausa == 6) {
                // test: complete the current mission and hear the radio at once
                Historia h = historia;
                h.radioReparada = true;
                inv.tiene[Inventario.H_PROFUNDIDAD] = true;
                inv.tiene[Inventario.H_REMORA] = true;
                int e = h.etapa;
                if (e >= 2 && e <= 4) {
                    h.tomado[e - 2] = true;
                } else if (e == 6 || e == 7) {
                    h.tomado[e == 6 ? Historia.SENSOR_A : Historia.SENSOR_B] = true;
                } else if (e >= 8 && e <= 10) {
                    h.tomado[Historia.FRAG + e - 8] = true;
                    inv.cant[Inventario.FRAGMENTO]++;
                } else if (e == 11) {
                    h.tomado[Historia.SANTUARIO] = true;
                    inv.cant[Inventario.FRAGMENTO] = 0;
                    inv.cant[Inventario.LLAVE]++;
                } else if (e == 12 && inv.cant[Inventario.LLAVE] == 0) {
                    inv.cant[Inventario.LLAVE]++;
                }
                if (e < 12) {
                    h.etapa++;
                }
                h.radioOida = h.etapa;
                avisar("Prueba: misión " + h.etapa + " - " + h.mision());
                Log.d("prueba: historia en etapa " + h.etapa);
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
                avisar(noche == 1 ? "Anochece. La linterna sera útil." : "Amanece en Talassa.");
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

    /** 0 = bright daylight shallows .. 1 = night or the deep: how much a lamp shows */
    float oscuridad() {
        float tt = Math.min(1, -py / 260f);
        return Math.max((1 - luzDia()) * (1 - 0.6f * tt), tt);
    }

    private void ajustarAgua() {
        float l = luzDia();
        avisoNoche(l);
        if (fuera) {
            // open air: haze in the colour of the horizon, long view, light of the hour
            colorCielo(l);
            colorAgua = cieloHorizonte;
            lejos = 110;
            fog.setColor(colorAgua);
            fog.setLinear(30, lejos);
            sol.setIntensity(0.9f * l);
            amb.setIntensity(0.2f + 0.35f * l);
            return;
        }
        float prof = -py;
        float tt = Math.min(1, prof / 260f);
        // deep water is dark navy, not black: the relief still reads and glowing flora
        // stands out (the flashlight lights it up properly)
        colorAgua = Terreno.mezcla(0x2a8fb0, 0x010a10, j2me.FMath.sqrt(tt));
        lejos = 62 - 26 * tt;
        // night: the shallows go dark navy and visibility drops (the deep is dark anyway)
        float noche = (1 - l) * (1 - 0.6f * tt);
        colorAgua = Terreno.mezcla(colorAgua, 0x000306, noche * 0.97f);
        lejos *= 1 - 0.5f * noche;
        float luz = 1 - 0.88f * tt;
        sol.setIntensity((0.02f + 0.48f * luz) * l);
        amb.setIntensity((0.12f + 0.6f * luz) * (1 - 0.88f * noche));
        // a lamp cuts through the murk: in the dark the visible range grows and the haze
        // starts further away (the spot itself lights up what is in front)
        if (vision > 0) {
            // night vision: the dark water reads clearly for a while
            float kv = Math.min(1, vision / 5);
            amb.setIntensity(amb.getIntensity() + 0.45f * kv);
            colorAgua = Terreno.mezcla(colorAgua, 0x1a4a3a, 0.35f * kv);
        }
        float oscuro = Math.max(noche, tt);
        if ((enRemora ? farosActivos() : linterna) && oscuro > 0.15f) {
            // the lamp lights what it touches, it does not light up the water: the haze
            // stays dark (no milky veil), but it starts much further away so everything
            // near is crisp, and surfaces get enough light to read clearly
            float k = Math.min(1, (oscuro - 0.15f) * 2);
            float extra = enRemora ? 1.2f : (inv.tiene[Inventario.H_LINTERNA2] ? 1.15f : 1.05f);
            lejos = Math.min(62, lejos * (1 + (extra - 1) * k));
            fog.setColor(colorAgua);
            fog.setLinear(lejos * (0.15f + (enRemora ? 0.45f : 0.3f) * k), lejos);
            if (enRemora) {
                amb.setIntensity(amb.getIntensity() + 0.48f * k);
            }
        } else {
            fog.setColor(colorAgua);
            fog.setLinear(lejos * 0.15f, lejos);
        }
    }

    void dibujar(Graphics g) {
        if (estado == INTERIOR
                || (enCapsula && (estado == FABRICADOR || estado == PDA || estado == LEYENDO || estado == BESTIARIO
                        || estado == MAPA))) {
            // inside the pod: its own small scene instead of the sea
            long t3d = System.currentTimeMillis();
            interior.dibujar(this, g, g3d);
            long t2d = System.currentTimeMillis();
            ms3d = t2d - t3d;
            if (estado == FABRICADOR) {
                dibujarFabricador(g);
            } else if (estado == PDA) {
                dibujarPda(g);
            } else if (estado == LEYENDO) {
                dibujarRegistro(g);
            } else if (estado == BESTIARIO) {
                bestiario.dibujar(g, this);
            } else if (estado == MAPA) {
                mapa.dibujar(g, this);
            }
            if (avisoT > 0 && aviso != null) {
                g.setColor(0x0b1d26);
                g.fillRect(0, Juego.H - 30, Juego.W, 13);
                g.setColor(0xffffff);
                g.drawString(aviso, Juego.W / 2, Juego.H - 28, Graphics.TOP | Graphics.HCENTER);
            }
            if (verDevlog) {
                dibujarDevlog(g);
            }
            ms2d = System.currentTimeMillis() - t2d;
            contarFrame();
            return;
        }
        dibujarMar(g);
        contarFrame();
    }

    private void contarFrame() {
        fpsCuenta++;
        long ahora = System.currentTimeMillis();
        if (ahora - fpsT0 >= 1000) {
            fps = (int) (fpsCuenta * 1000 / (ahora - fpsT0));
            fpsCuenta = 0;
            fpsT0 = ahora;
        }
    }

    private void dibujarMar(Graphics g) {
        ajustarAgua();
        int horizonte = Juego.H / 2 + (int) (j2me.FMath.tan(pitch * j2me.FMath.RAD) * FOCAL);
        if (fuera) {
            // sky: zenith colour fading to the horizon colour (the sea covers the rest)
            for (int y = 0; y < Juego.H; y += 24) {
                float f = (horizonte - y) / 220f;
                g.setColor(Terreno.mezcla(cieloHorizonte, cieloArriba, f));
                g.fillRect(0, y, Juego.W, 24);
            }
            float l = luzDia();
            if (l < 0.35f) {
                dibujarEstrellas(g, horizonte, (0.35f - l) / 0.35f);
            }
        } else {
            // background: the fog colour, a little lighter towards the surface when looking up
            int arriba = Terreno.mezcla(colorAgua, 0x9fe6f0, py > -40 ? 0.35f * (1 + py / 40) : 0);
            for (int y = 0; y < Juego.H; y += 24) {
                float f = (horizonte - y) / 160f;
                g.setColor(Terreno.mezcla(colorAgua, arriba, f));
                g.fillRect(0, y, Juego.W, 24);
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
        if (golpeCam > 0) {
            golpeCam -= dtDibujo;
            tc.postRotate(-golpeCam * 12, 1, 0, 0);
        }
        if (mareo > 0) {
            float km = Math.min(1, mareo / 4);
            tc.postRotate(j2me.FMath.sin(tiempo * 1.7f) * 7 * km, 0, 0, 1);
            tc.postRotate(j2me.FMath.sin(tiempo * 1.1f) * 4 * km, 1, 0, 0);
        }
        if (aturdido > 0) {
            tc.postRotate(j2me.FMath.sin(tiempo * 35) * 1.5f, 0, 0, 1);
        }
        if (latidoT > 2.8f && historia.latidoCerca(px, pz)) {
            tc.postRotate((latidoT - 2.8f) * 4 * j2me.FMath.sin(tiempo * 60), 0, 0, 1);
        }
        if (alabeo != 0) {
            tc.postRotate(alabeo, 0, 0, 1);
        }
        g3d.setCamera(cam, tc);
        g3d.resetLights();
        tl.setIdentity();
        tl.postRotate(-65, 1, 0, 0);
        tl.postRotate(25, 0, 1, 0);
        g3d.addLight(sol, tl);
        g3d.addLight(amb, null);
        if (enRemora ? farosActivos() : linterna) {
            // the spot shines along -Z of its transform, like the camera; the Remora's
            // headlights are stronger and wider than the hand flashlight
            foco.setIntensity(enRemora ? 4.2f : (inv.tiene[Inventario.H_LINTERNA2] ? 7.5f : 6.0f));
            foco.setSpotAngle(enRemora ? 50 : 32);
            foco.setSpotExponent(enRemora ? 2 : 4);
            foco.setAttenuation(1, enRemora ? 0.006f : 0.004f, enRemora ? 0.0006f : 0.0003f);
            g3d.addLight(foco, tc);
        }
        // the nearest burning flare lights up its surroundings
        int bc = -1;
        float bd = 30 * 30;
        for (int i = 0; i < NBG; i++) {
            if (bgT[i] > 0) {
                float dx = bgX[i] - px, dy = bgY[i] - py, dz = bgZ[i] - pz;
                float d2 = dx * dx + dy * dy + dz * dz;
                if (d2 < bd) {
                    bd = d2;
                    bc = i;
                }
            }
        }
        if (bc >= 0 && !fuera) {
            tl.setIdentity();
            tl.postTranslate(bgX[bc], bgY[bc] + 0.6f, bgZ[bc]);
            float vida = Math.min(1, bgT[bc] / 8); // it dims in its last seconds
            luzBengala.setIntensity((2.6f + j2me.FMath.sin(tiempo * 23) * 0.4f) * vida);
            g3d.addLight(luzBengala, tl);
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
            // the wave mesh tiles every 48 m; it scrolls with the swell
            float desp = (tiempo * VEL_OLA) % 48;
            t.postTranslate(((int) (px / 48)) * 48f + desp - 144, 0, ((int) (pz / 48)) * 48f - 144);
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
        if (!enRemora) {
            dibujarManos(dtDibujo);
        } else if (farosActivos() && !fuera && oscuridad() > 0.1f) {
            // headlights: a wide pool of light ahead of the cockpit
            t.set(tc);
            t.postTranslate(0, -0.6f, -7);
            g3d.render(hazRemora, t);
        }
        dibujarArpon();
        dibujarBalizas(lejos);
        ecos.dibujar(g3d, px, pz, lejos, tiempo);
        if (descargaT > 0) {
            // the rod's shock: a crackling blue burst that grows and fades
            t.setIdentity();
            t.postTranslate(descargaX, descargaY, descargaZ);
            t.postRotate(tiempo * 900, 0, 1, 0);
            float k = 2 + (0.3f - descargaT) * 8 + Terreno.azar((int) (tiempo * 40), 4, 80) * 1.5f;
            t.postScale(k, k * 0.7f, k);
            g3d.render(chispaAzul, t);
        }
        for (int i = 0; i < NBG; i++) {
            if (bgT[i] > 0 && !fuera) {
                float dx = bgX[i] - px, dz = bgZ[i] - pz;
                if (dx * dx + dz * dz < 120 * 120) {
                    t.setIdentity();
                    t.postTranslate(bgX[i], bgY[i] + 0.15f, bgZ[i]);
                    float k = (0.75f + j2me.FMath.sin(tiempo * 19 + i) * 0.15f) * Math.min(1, bgT[i] / 8);
                    t.postScale(k, k, k);
                    g3d.render(bengalaLuz, t);
                    if (((int) (tiempo * 10) + i) % 6 == 0) {
                        emitirBurbuja(bgX[i], bgY[i] + 0.2f, bgZ[i], 1.5f);
                    }
                }
            }
        }
        if (inv.tiene[Inventario.H_REMORA] && !enRemora && !fuera) {
            // the parked Remora's beacon: a steady cyan glow and a blinking red light,
            // drawn without fog so they can be found in the dark from far away
            float dx = rx - px, dz = rz - pz;
            float d2 = dx * dx + dz * dz;
            if (d2 < 160 * 160 && d2 > 12 * 12 && oscuridad() > 0.2f) {
                t.setIdentity();
                t.postTranslate(rx, ry + 0.9f, rz);
                float k = 1 + j2me.FMath.sqrt(d2) / 40; // stays visible as it gets far
                t.postScale(k, k, k);
                g3d.render(balizaCian, t);
                if (((int) (tiempo * 1.5f) & 1) == 0 && !rAveriada) {
                    t.postTranslate(0, 0.5f, 0);
                    g3d.render(balizaRoja, t);
                }
            }
        }
        if (hayBase) {
            float dx = baseX - px, dz = baseZ - pz;
            if (dx * dx + dz * dz < (lejos + 6) * (lejos + 6) && !fuera) {
                t.setIdentity();
                t.postTranslate(baseX, baseY, baseZ);
                g3d.render(baseMalla, t);
                if (inv.tiene[Inventario.H_GRANJA]) {
                    t.postTranslate(MOD_DX, 0, 0);
                    g3d.render(granjaMalla, t);
                    t.postTranslate(-MOD_DX, 0, 0);
                }
                if (inv.tiene[Inventario.H_RECARGA]) {
                    t.postTranslate(-MOD_DX, 0, 0);
                    g3d.render(recargaMalla, t);
                }
            }
        }
        if (inv.tiene[Inventario.H_REMORA] && !enRemora) {
            float dx = rx - px, dz = rz - pz;
            if (dx * dx + dz * dz < lejos * lejos && (!fuera || ry > -1)) {
                t.setIdentity();
                t.postTranslate(rx, ry + j2me.FMath.sin(tiempo * 1.2f) * 0.08f, rz);
                t.postRotate(rRumbo + 180, 0, 1, 0);
                g3d.render(remoraP[1], t);
                // the propeller idles slowly when parked, spins up for a while after you leave
                heliceVel += (40 - heliceVel) * Math.min(1, dtDibujo * 0.3f);
                helice = (helice + heliceVel * dtDibujo * 6) % 360;
                t.postTranslate(0, 0, -2.45f);
                t.postRotate(helice, 0, 0, 1);
                t.postTranslate(0, 0, 2.45f);
                g3d.render(remoraP[0], t);
                if (chispaT > 0) {
                    // sparks where the welder touches the hull
                    tp2.setIdentity();
                    tp2.postTranslate(px + fx * 1.8f, py + fy * 1.8f - 0.3f, pz + fz * 1.8f);
                    float k = 0.12f + Terreno.azar((int) (tiempo * 30), 2, 78) * 0.2f;
                    tp2.postScale(k, k, k);
                    g3d.render(chispa, tp2);
                }
                // bubbles: more while it spins fast, a trickle when idle
                burEmitir += heliceVel * dtDibujo * 0.012f;
                float hr = rRumbo * j2me.FMath.RAD;
                while (burEmitir >= 1) {
                    burEmitir -= 1;
                    float lado = (Terreno.azar(burSemilla++, 1, 9) - 0.5f) * 0.8f;
                    emitirBurbuja(rx + j2me.FMath.sin(hr) * 2.6f + j2me.FMath.cos(hr) * lado,
                            ry + (Terreno.azar(burSemilla, 2, 10) - 0.5f) * 0.6f,
                            rz + j2me.FMath.cos(hr) * 2.6f - j2me.FMath.sin(hr) * lado, 1.1f);
                }
            }
        }
        if (!fuera) {
            if (boya > 0) {
                // the inflated buoy above the head, and a stream of bubbles from it
                t.set(tc);
                t.postTranslate(0, 1.3f, -0.6f);
                t.postRotate(tiempo * 40, 0, 1, 0);
                g3d.render(boyaMalla, t);
                // the air rushing out of the buoy's valve: a dense stream of bubbles in
                // front of the face, rising faster than the diver so they stream past
                float fr = yaw * j2me.FMath.RAD;
                float fx = -j2me.FMath.sin(fr), fz = -j2me.FMath.cos(fr);
                for (int q = 0; q < 3; q++) {
                    float a = 1.0f + Terreno.azar(burSemilla++, 3, 11) * 1.4f;
                    float l = (Terreno.azar(burSemilla, 4, 12) - 0.5f) * 1.6f;
                    emitirBurbuja(px + fx * a - fz * l, py - 1.2f + Terreno.azar(burSemilla, 5, 13) * 1.2f,
                            pz + fz * a + fx * l, 11 + Terreno.azar(burSemilla, 6, 14) * 3);
                }
            }
            dibujarBurbujas();
            peces.dibujar(g3d, px, pz, hx, hz, lejos);
            depredadores.dibujar(g3d, px, pz, hx, hz, lejos);
            hostiles.dibujar(g3d, px, pz, hx, hz, lejos);
        }
        if (!fuera && py > -lejos) {
            t.setIdentity();
            t.postTranslate(px, 0, pz);
            g3d.render(superficie, t);
            // from below, the swell moves across the surface too
            if (py > -25) {
                float desp = (tiempo * VEL_OLA) % 48;
                t.setIdentity();
                t.postTranslate(((int) (px / 48)) * 48f + desp - 48, 0, ((int) (pz / 48)) * 48f - 48);
                g3d.render(olasAbajo, t);
            }
        }
        g3d.releaseTarget();
        if (registrarFrame) {
            registrarFrame = false;
            Log.d("superficie: primer frame dibujado tras la transicion");
        }
        long t2d = System.currentTimeMillis();
        ms3d = t2d - t3d;
        Log.fase(Log.F_2D);
        if (!fuera) {
            dibujarNieve(g);
        }
        dibujarHud(g);
        if (estado == FABRICADOR) {
            dibujarFabricador(g);
        } else if (estado == LEYENDO) {
            dibujarRegistro(g);
        } else if (estado == BESTIARIO) {
            bestiario.dibujar(g, this);
        } else if (estado == MAPA) {
            mapa.dibujar(g, this);
        } else if (estado == ECO && ecoTexto != null) {
            panelTexto(g, ecoTexto);
        } else if (estado == PDA) {
            dibujarPda(g);
        } else if (estado == PAUSA) {
            dibujarPausa(g);
        } else if (estado == MUERTO) {
            dibujarMuerte(g);
        } else if (estado == FINAL) {
            panelTexto(g, historia.pantalla);
        }
        if (verDevlog) {
            dibujarDevlog(g);
        }
        ms2d = System.currentTimeMillis() - t2d;
    }

    /** the diver's hands in front of the camera: knife (right) and flashlight (left) */
    /** camera turn speed, smoothed, so the arms lag a little behind fast turns */
    private float giroSuave, yawAntes;

    /**
     * The diver's arms, first-person "view model" style: each arm is one mesh from the hand
     * to the elbow and is animated by rotating it around the elbow, which sits off screen
     * below and behind the view. The hand therefore moves on an arc and the arm always
     * stays attached to the body. The knife strike has the three classic phases:
     * anticipation (raise and pull back), a fast strike down and across with a short
     * lunge, and an eased recovery. Both arms bob while swimming and sway with turns.
     */
    // ------------------------------------------------------------------ supply crates
    /** what the crates hold: common things everywhere, rarer ones deeper down */
    static final int[] CAJA_COMUN = { Inventario.TITANIO, Inventario.COBRE, Inventario.CUARZO, Inventario.FIBRA,
            Inventario.BULBO, Inventario.AGUA, Inventario.PEZ_COCIDO, Inventario.BENGALA };
    static final int[] CAJA_RARA = { Inventario.LAMINA, Inventario.CABLE, Inventario.VIDRIO, Inventario.BATERIA,
            Inventario.CIRCUITO, Inventario.BOTIQUIN, Inventario.CUERDA, Inventario.BOYA };
    static final int[] CAJA_PROFUNDA = { Inventario.CRISTAL_T, Inventario.OPALO, Inventario.CIRCUITO, Inventario.KIT };

    private void abrirCaja(int ci, int k) {
        Mundo.Chunk c = mundo.chunks[ci];
        int semilla = c.cx * 31 + c.cz * 17 + k;
        float hondo = Math.min(1, -py / 300);
        StringBuffer sb = new StringBuffer("Caja de suministros:");
        int dados = 0;
        for (int q = 0; q < 3; q++) {
            float r = Terreno.azar(semilla, q, 971);
            int[] lista = r < 0.2f * hondo ? CAJA_PROFUNDA : (r < 0.55f ? CAJA_RARA : CAJA_COMUN);
            int it = lista[(int) (Terreno.azar(semilla, q, 972) * lista.length) % lista.length];
            if (!inv.meter(it)) {
                break;
            }
            sb.append(q == 0 ? " " : ", ").append(Inventario.CORTO[it]);
            dados++;
        }
        if (dados == 0) {
            avisar("Mochila llena: tira algo desde la PDA");
            return;
        }
        mundo.recoger(ci, k);
        accionIzq(ACC_AGARRAR, 0.45f);
        Sonido.tocar(Sonido.ESCOTILLA);
        avisar(sb.toString());
        Log.d("caja abierta: " + sb);
    }

    // ------------------------------------------------------------------ signal beacons
    static final int NSENAL = 6;
    final float[] sbX = new float[NSENAL], sbY = new float[NSENAL], sbZ = new float[NSENAL];
    final boolean[] sbPuesta = new boolean[NSENAL];
    static final int COLOR_SENAL = 0x7aff7a;

    private void colocarBaliza() {
        for (int i = 0; i < NSENAL; i++) {
            if (!sbPuesta[i]) {
                sbPuesta[i] = true;
                sbX[i] = px + fx * 1.5f;
                sbZ[i] = pz + fz * 1.5f;
                sbY[i] = fuera ? Terreno.altura(sbX[i], sbZ[i]) : Math.max(Terreno.altura(sbX[i], sbZ[i]), py - 1.6f);
                inv.cant[Inventario.SENAL]--;
                estado = JUEGO;
                avisar("Baliza de señal " + (i + 1) + " colocada");
                Sonido.tocar(Sonido.FABRICAR);
                return;
            }
        }
        avisar("Ya tienes " + NSENAL + " balizas puestas: recoge alguna");
    }

    private int balizaCerca() {
        for (int i = 0; i < NSENAL; i++) {
            if (sbPuesta[i]) {
                float dx = sbX[i] - px, dy = sbY[i] + 0.8f - py, dz = sbZ[i] - pz;
                if (dx * dx + dy * dy + dz * dz < 2.5f * 2.5f) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void dibujarBalizas(float lejos) {
        for (int i = 0; i < NSENAL; i++) {
            if (!sbPuesta[i]) {
                continue;
            }
            float dx = sbX[i] - px, dz = sbZ[i] - pz;
            float d2 = dx * dx + dz * dz;
            if (d2 < (lejos + 4) * (lejos + 4)) {
                t.setIdentity();
                t.postTranslate(sbX[i], sbY[i], sbZ[i]);
                g3d.render(postBaliza, t);
            }
            // its light, fog-free, can be seen from far away
            if (d2 < 200 * 200 && !fuera) {
                t.setIdentity();
                t.postTranslate(sbX[i], sbY[i] + 1.3f, sbZ[i]);
                float k = (0.6f + j2me.FMath.sqrt(d2) / 60) * (((int) (tiempo * 2) + i) % 4 == 0 ? 1.3f : 1);
                t.postScale(k, k, k);
                g3d.render(balizaVerde, t);
            }
        }
    }

    // ------------------------------------------------------------------ harpoon
    /** the bolt in flight: position, velocity, seconds left (0 = none); reload time */
    private float arX, arY, arZ, arVX, arVY, arVZ, arT, arpT;
    private boolean arClavado;
    static final float ARPON_RECARGA = 1.3f;

    private void dispararArpon() {
        if (arpT > 0) {
            avisar("Recargando el arpón...");
            return;
        }
        arpT = ARPON_RECARGA;
        arX = px + fx * 0.7f;
        arY = py + fy * 0.7f - 0.15f;
        arZ = pz + fz * 0.7f;
        arVX = fx * 26;
        arVY = fy * 26;
        arVZ = fz * 26;
        arT = 1.2f;
        arClavado = false;
        reboteT = 0.2f;
        Sonido.tocar(Sonido.TAJO);
    }

    private void actualizarArpon(float dt) {
        if (arpT > 0) {
            arpT -= dt;
        }
        if (arT <= 0) {
            return;
        }
        arT -= dt;
        if (arClavado) {
            return; // stuck in the rock or the sand for a moment
        }
        float sx = arVX * dt, sy = arVY * dt, sz = arVZ * dt;
        arX += sx;
        arY += sy;
        arZ += sz;
        arVY -= 1.5f * dt; // a slight drop
        float l = j2me.FMath.sqrt(arVX * arVX + arVY * arVY + arVZ * arVZ);
        float dx = arVX / l, dy = arVY / l, dz = arVZ / l;
        // what it hits: checked from just behind the tip, with no answer back at the diver
        float bx = arX - dx * 0.4f, by = arY - dy * 0.4f, bz = arZ - dz * 0.4f;
        depredadores.aDistancia = true;
        hostiles.aDistancia = true;
        String m = depredadores.golpear(this, bx, by, bz, dx, dy, dz);
        if (m == null) {
            m = hostiles.golpear(this, bx, by, bz, dx, dy, dz);
        }
        depredadores.aDistancia = false;
        hostiles.aDistancia = false;
        if (m == null) {
            int f = peces.golpear(bx, by, bz, dx, dy, dz);
            if (f >= 0) {
                m = !peces.vivo[f] ? "El arpón abatió: " + NOMBRE_PEZ[peces.especie[f]]
                        : "Arpón! El " + NOMBRE_PEZ[peces.especie[f]] + " huye herido";
                if (!peces.vivo[f] && peces.especie[f] == 0) {
                    inv.meter(Inventario.PEZ); // a bubblefin: it comes back on the line
                }
            }
        }
        if (m != null) {
            avisar(m);
            Sonido.tocar(Sonido.GOLPE);
            arT = 0;
            return;
        }
        if (arY < Terreno.altura(arX, arZ) || mundo.enRoca(arX, arY, arZ)) {
            arClavado = true;
            arT = Math.min(arT, 2);
            Sonido.tocar(Sonido.MINERAL);
        }
    }

    private void dibujarArpon() {
        if (arT <= 0) {
            return;
        }
        t.setIdentity();
        t.postTranslate(arX, arY, arZ);
        float h = j2me.FMath.sqrt(arVX * arVX + arVZ * arVZ);
        t.postRotate(j2me.FMath.atan2(arVX, arVZ) * 57.29578f, 0, 1, 0);
        t.postRotate(-j2me.FMath.atan2(arVY, h) * 57.29578f, 1, 0, 0);
        g3d.render(saeta, t);
    }

    // ------------------------------------------------------------------ arm animation
    /** left-hand gestures: throw, switch click, reach to grab */
    static final int ACC_LANZAR = 1, ACC_CLIC = 2, ACC_AGARRAR = 3;
    private int accIzqTipo;
    private float accIzqT, accIzqDur;
    /** swimming stroke phase and how much of it shows (0 idle - 1 swimming) */
    private float brazFase, nadoAmt;
    /** hands lag behind the view when looking up or down */
    private float pitchSuave;
    /** equipment swap: hands dip out of view and come back with the new thing */
    private float bajadaT;
    private int eqVisto = -1, armaVista = -1;
    private boolean lintVisto;
    /** forehand (1) or backhand (-1) knife swing, and the jolt when the blade connects */
    private float tajoLado = 1, reboteT;
    static final float BAJADA = 0.4f;

    private void accionIzq(int tipo, float dur) {
        accIzqTipo = tipo;
        accIzqT = dur;
        accIzqDur = dur;
    }

    /** one arm: the shared rest pose plus offsets (m), then rotations (degrees) */
    private void brazo(Mesh m, boolean der, float dx, float dy, float dz, float rx, float ry, float rz, float sway,
            float bajada) {
        t.set(tc);
        t.postTranslate((der ? 0.36f : -0.34f) + sway + dx, -0.42f + dy - bajada * 0.35f, (der ? -0.10f : -0.08f) + dz);
        t.postRotate(ry, 0, 1, 0);
        t.postRotate(rx - bajada * 30, 1, 0, 0);
        t.postRotate(rz, 0, 0, 1);
        if (der && m == manoArpon || m == manoArponVacio) {
            // the speargun: held level, aimed at the crosshair
            t.postRotate(5, 0, 1, 0);
            t.postRotate(14, 1, 0, 0);
            t.postScale(0.7f, 0.7f, 0.7f);
            t.postTranslate(-0.04f, 0.05f, -Modelos.CODO_Z);
        } else if (der) {
            // forearm pointing forward, up and a little inwards, blade flat towards the eye
            t.postRotate(12, 0, 1, 0);
            t.postRotate(26, 1, 0, 0);
            t.postRotate(-70, 0, 0, 1);
            t.postScale(0.7f, 0.7f, 0.7f);
            t.postTranslate(-0.02f, 0, -Modelos.CODO_Z);
        } else {
            t.postRotate(-10, 0, 1, 0);
            t.postRotate(24, 1, 0, 0);
            t.postScale(0.7f, 0.7f, 0.7f);
            t.postTranslate(0, 0, -Modelos.CODO_Z);
        }
        g3d.render(m, t);
    }

    private void dibujarManos(float dt) {
        // turning and looking: the hands trail the view a little
        float giro = Peces.angulo(yaw - yawAntes) / Math.max(dt, 0.001f);
        yawAntes = yaw;
        giroSuave += (giro - giroSuave) * Math.min(1, dt * 8);
        float sway = Math.max(-0.05f, Math.min(0.05f, giroSuave * 0.0006f));
        pitchSuave += (pitch - pitchSuave) * Math.min(1, dt * 7);
        float lagY = Math.max(-0.04f, Math.min(0.04f, (pitch - pitchSuave) * 0.003f));
        // swimming: a breaststroke whose pace follows the speed; idle: a slow float
        float vel = j2me.FMath.sqrt(vx * vx + vy * vy + vz * vz);
        float objetivo = enRemora ? 0 : Math.min(1, vel / 3.5f);
        nadoAmt += (objetivo - nadoAmt) * Math.min(1, dt * 3);
        brazFase += dt * (1.6f + vel * 0.55f);
        float c = j2me.FMath.sin(brazFase), c2 = j2me.FMath.cos(brazFase);
        float flota = j2me.FMath.sin(tiempo * 1.7f) * 0.008f * (1 - nadoAmt);
        // equipment changed: dip and come back
        boolean lint = equipo == EQ_LINTERNA;
        if ((eqVisto >= 0 && eqVisto != equipo) || (armaVista >= 0 && armaVista != arma)) {
            bajadaT = BAJADA;
        }
        eqVisto = equipo;
        armaVista = arma;
        lintVisto = lint;
        if (bajadaT > 0) {
            bajadaT -= dt;
        }
        float bajada = bajadaT > 0 ? j2me.FMath.sin(j2me.FMath.PI * (1 - bajadaT / BAJADA)) : 0;
        if (reboteT > 0) {
            reboteT -= dt;
        }
        if (accIzqT > 0) {
            accIzqT -= dt;
        }

        // ---- right hand: the weapon (or the bare glove)
        {
            float rx = 0, ry = 0, rz = 0, dz = 0, dy = 0, dx = 0;
            float nado = nadoAmt * (tajo > 0 ? 0.2f : 1);
            if (tajo > 0) {
                float p = (TAJO - tajo) / TAJO;
                if (arma == ARMA_VARA && armaEnMano()) {
                    // the rod: a straight thrust
                    if (p < 0.3f) {
                        float k = suaviza(p / 0.3f);
                        dz = 0.06f * k;
                        rx = 8 * k;
                    } else if (p < 0.55f) {
                        float k = (p - 0.3f) / 0.25f;
                        k = k * k;
                        dz = 0.06f - 0.26f * k;
                        rx = 8 - 16 * k;
                    } else {
                        float k = suaviza((p - 0.55f) / 0.45f);
                        dz = -0.2f * (1 - k);
                        rx = -8 * (1 - k);
                    }
                } else {
                    // the knife: wind up, a fast chop across, follow through; alternating
                    // forehand and backhand
                    float l = tajoLado;
                    if (p < 0.3f) {
                        float k = suaviza(p / 0.3f);
                        rx = 22 * k;
                        ry = 10 * k * l;
                        rz = -12 * k * l;
                        dx = 0.03f * k * l;
                    } else if (p < 0.55f) {
                        float k = (p - 0.3f) / 0.25f;
                        k = k * k;
                        rx = 22 - 62 * k;
                        ry = (10 - 30 * k) * l;
                        rz = (-12 + 30 * k) * l;
                        dx = (0.03f - 0.08f * k) * l;
                        dz = -0.13f * k;
                    } else {
                        float k = suaviza((p - 0.55f) / 0.45f);
                        rx = -40 * (1 - k);
                        ry = -20 * (1 - k) * l;
                        rz = 18 * (1 - k) * l;
                        dx = -0.05f * (1 - k) * l;
                        dz = -0.13f * (1 - k);
                    }
                }
            }
            if (reboteT > 0) {
                // the blade struck something: it jolts back
                float k = reboteT / 0.15f;
                rx += 10 * k;
                dz += 0.04f * k;
            }
            dx += 0.05f * (1 - c2) * nado;
            dz += 0.06f * c * nado;
            dy += -0.02f * (1 - c2) * nado + flota + lagY;
            ry += 10 * c * nado;
            rx += 6 * c2 * nado;
            Mesh m = !armaEnMano() ? manoDerVacia
                    : (arma == ARMA_VARA ? manoVara
                            : (arma == ARMA_ARPON ? (arpT > 0.25f ? manoArponVacio : manoArpon) : manoCuchillo));
            if (arma == ARMA_ARPON && armaEnMano() && arpT > ARPON_RECARGA - 0.25f) {
                // the recoil of the shot
                float k = (arpT - (ARPON_RECARGA - 0.25f)) / 0.25f;
                rx += 14 * k;
                dz += 0.06f * k;
            }
            brazo(m, true, dx, dy, dz, rx, ry, rz, sway, bajada);
            if (arma == ARMA_VARA && armaEnMano() && varaT > 1.2f) {
                // the shock at the rod's tip
                t.postTranslate(0.02f, 0, -0.5f);
                t.postScale(0.25f, 0.25f, 0.25f);
                g3d.render(chispaAzul, t);
            }
        }

        // ---- left hand: the secondary item (or the bare glove)
        {
            float rx = 0, ry = 0, rz = 0, dz = 0, dy = 0, dx = 0;
            float nado = nadoAmt;
            Mesh m;
            switch (equipo) {
            case EQ_LINTERNA:
                m = manoLinterna;
                break;
            case EQ_BOYA:
                m = boya > 0 || inv.cant[Inventario.BOYA] <= 0 ? manoVacia : manoBoya;
                break;
            case EQ_BENGALA:
                m = manoBengala;
                break;
            case EQ_SOLDADOR:
                m = manoSoldador;
                break;
            case EQ_IMPULSOR:
                m = manoImpulsor;
                break;
            case EQ_ESCANER:
                m = manoEscaner;
                break;
            default:
                m = manoVacia;
                break;
            }
            if (boya > 0 && equipo == EQ_BOYA) {
                // holding the buoy's line: the arm reaches up
                float alza = Math.min(1, (7 - boya) * 3);
                dy += alza * 0.12f;
                rx += alza * 70;
                nado = 0;
            }
            if (impulsando) {
                // the thruster pulls the arm forward and it trembles
                dz -= 0.06f;
                rx += 8;
                float tr = j2me.FMath.sin(tiempo * 70) * 0.006f;
                dx += tr;
                dy += tr;
                nado = 0;
            }
            if (equipo == EQ_ESCANER && escObjetivo >= 0 && escProgreso > 0) {
                // aiming the scanner: raised and held still
                dy += 0.05f;
                dz -= 0.05f;
                rx += 10;
                nado *= 0.2f;
            }
            if (equipo == EQ_SOLDADOR && chispaT > 0) {
                float tr = j2me.FMath.sin(tiempo * 70) * 0.006f;
                dx += tr;
                dy += tr;
                dz -= 0.04f;
            }
            if (accIzqT > 0) {
                float p = 1 - accIzqT / accIzqDur;
                nado = 0;
                if (accIzqTipo == ACC_LANZAR) {
                    // wind back over the shoulder, then the throw
                    if (p < 0.35f) {
                        float k = suaviza(p / 0.35f);
                        rx += 35 * k;
                        dy += 0.08f * k;
                        dz += 0.1f * k;
                    } else if (p < 0.6f) {
                        float k = (p - 0.35f) / 0.25f;
                        rx += 35 - 70 * k;
                        dy += 0.08f - 0.04f * k;
                        dz += 0.1f - 0.3f * k;
                    } else {
                        float k = suaviza((p - 0.6f) / 0.4f);
                        rx += -35 * (1 - k);
                        dy += 0.04f * (1 - k);
                        dz += -0.2f * (1 - k);
                    }
                    if (p > 0.4f && equipo == EQ_BENGALA && inv.cant[Inventario.BENGALA] <= 0) {
                        m = manoVacia;
                    }
                } else if (accIzqTipo == ACC_CLIC) {
                    // a thumb on the switch: a small jerk of the wrist
                    float k = j2me.FMath.sin(j2me.FMath.PI * p);
                    rx -= 6 * k;
                    rz += 8 * k;
                } else if (accIzqTipo == ACC_AGARRAR) {
                    // reach out, close the hand, pull back
                    float k = j2me.FMath.sin(j2me.FMath.PI * p);
                    dz -= 0.22f * k;
                    dx += 0.08f * k;
                    rx -= 18 * k;
                    m = manoVacia;
                }
            }
            dx -= 0.05f * (1 - c2) * nado;
            dz += 0.06f * c * nado;
            dy += -0.02f * (1 - c2) * nado + flota + lagY;
            ry -= 10 * c * nado;
            rx += 6 * c2 * nado;
            brazo(m, false, dx, dy, dz, rx, ry, rz, sway, bajada);
            if (impulsando) {
                // a light trail of bubbles from the thruster's duct
                impBurbuja -= dt;
                if (impBurbuja <= 0) {
                    impBurbuja = 0.09f;
                    float cyw = j2me.FMath.cos(yaw * j2me.FMath.RAD), syw = j2me.FMath.sin(yaw * j2me.FMath.RAD);
                    emitirBurbuja(px + fx * 0.9f - cyw * 0.35f, py + fy * 0.9f - 0.35f, pz + fz * 0.9f + syw * 0.35f,
                            0.8f + Terreno.azar(burSemilla++, 7, 19) * 0.6f);
                }
            }
            if (escObjetivo >= 0 && escProgreso > 0 && escProgreso < 1.6f && equipo == EQ_ESCANER) {
                // the scanner's beam: a flickering glow at its lens
                t.postTranslate(0, 0.03f, -0.25f);
                float k = 0.1f + Terreno.azar((int) (tiempo * 20), 3, 79) * 0.06f;
                t.postScale(k, k, k);
                g3d.render(chispaAzul, t);
            } else if (chispaT > 0 && equipo == EQ_SOLDADOR) {
                // sparks at the welder's tip
                t.postTranslate(0, 0, -0.32f);
                float k = 0.12f + Terreno.azar((int) (tiempo * 30), 1, 77) * 0.1f;
                t.postScale(k, k, k);
                g3d.render(chispa, t);
            }
            // pool of light where the lamp points, a few metres ahead (only in the dark:
            // in daylight shallows it would just wash the picture out)
            if (equipo == EQ_LINTERNA && linterna && !fuera && oscuridad() > 0.1f) {
                t.set(tc);
                t.postTranslate(0, -0.3f, -6);
                g3d.render(haz, t);
            }
        }
    }

    private static float suaviza(float k) {
        k = k < 0 ? 0 : (k > 1 ? 1 : k);
        return k * k * (3 - 2 * k);
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

    /**
     * Inside the Remora: canopy struts and a dashboard along the bottom (the meters are
     * drawn on top of it afterwards). Cheap: a handful of filled shapes.
     */
    private void cabina(Graphics g) {
        int W = Juego.W, H = Juego.H;
        g.setColor(0x16222a);
        // canopy frame: top bar and two struts converging towards the top centre
        g.fillRect(0, 0, W, 8);
        g.fillTriangle(0, 0, 70, 0, 0, 150);
        g.fillTriangle(W, 0, W - 70, 0, W, 150);
        g.fillTriangle(150, 0, 175, 0, 30, 140);
        g.fillTriangle(W - 150, 0, W - 175, 0, W - 30, 140);
        // dashboard
        g.fillRect(0, H - 60, W, 60);
        g.fillTriangle(0, H - 60, W / 2, H - 76, W, H - 60);
        g.setColor(0x2c4250);
        g.drawLine(0, H - 61, W / 2, H - 77);
        g.drawLine(W / 2, H - 77, W, H - 61);
        // sonar: a ring with the heading line
        int sx = W - 70, sy = H - 32;
        Radar.dibujar(g, this, sx, sy, 24, inv.tiene[Inventario.H_SONAR] ? 250 : 120);
        // depth gauge
        boolean peligro = -py > remoraMax();
        g.setColor(peligro ? 0xff5040 : 0xf08a3a);
        g.drawString("RÉMORA", W / 2, H - 56, Graphics.TOP | Graphics.HCENTER);
        g.drawString("PROF " + (int) -py + " / " + (int) remoraMax() + " m", W / 2, H - 42,
                Graphics.TOP | Graphics.HCENTER);
        g.setColor(0x9fc4cc);
        if (bordoT > 0) {
            g.drawString("[] salir  L faros", W / 2, H - 27, Graphics.TOP | Graphics.HCENTER);
        } else {
            g.drawString(farosActivos() ? "FAROS ON" : (faros ? "FAROS ..." : "FAROS OFF"), W / 2, H - 27,
                    Graphics.TOP | Graphics.HCENTER);
        }
        // hull integrity (right of the dashboard, next to the sonar)
        int cx0 = W - 172, cy0 = H - 40;
        boolean critico = rCasco < 30 && ((int) (tiempo * 4) & 1) == 0;
        g.setColor(critico ? 0xff4030 : 0x9fc4cc);
        g.drawString("CASCO " + (int) rCasco + "%", cx0, cy0 - 13, Graphics.TOP | Graphics.LEFT);
        g.setColor(0x0a2430);
        g.fillRect(cx0, cy0, 70, 6);
        g.setColor(rCasco > 60 ? 0x6aff8a : (rCasco > 30 ? 0xffc040 : (critico ? 0xff2010 : 0x8a1a10)));
        g.fillRect(cx0, cy0, (int) (70 * rCasco / 100), 6);
        grietas(g);
        // defence pulse charge (R)
        int bw = 70, bx = W / 2 - bw / 2 + 14, by = H - 11;
        g.setColor(0x0a2430);
        g.fillRect(bx, by, bw, 6);
        float carga = pulsoCarga > 0 ? 1 - pulsoCarga / 8 : 1;
        g.setColor(carga >= 1 ? 0x6ff0ff : 0x2f7f90);
        g.fillRect(bx, by, (int) (bw * carga), 6);
        g.drawString("R pulso", bx - 4, by - 4, Graphics.TOP | Graphics.RIGHT);
        // the discharge: electric arcs across the canopy
        if (destello > 0) {
            g.setColor(0xbff8ff);
            for (int i = 0; i < 6; i++) {
                int x0 = W / 2, y0 = H / 2 + 20;
                for (int sgm = 0; sgm < 5; sgm++) {
                    int x1 = x0 + (int) ((Terreno.azar(i, sgm, (int) (tiempo * 30)) - 0.5f) * 90 + (i - 2.5f) * 18);
                    int y1 = y0 - 22 + (int) (Terreno.azar(sgm, i, (int) (tiempo * 30) + 7) * 10);
                    g.drawLine(x0, y0, x1, y1);
                    x0 = x1;
                    y0 = y1;
                }
            }
        }
    }

    /** cracks in the canopy glass: the more damaged the hull, the more of them */
    private void grietas(Graphics g) {
        int n = rCasco >= 60 ? 0 : (rCasco >= 40 ? 2 : (rCasco >= 20 ? 4 : 7));
        g.setColor(0xd8f0f4);
        for (int i = 0; i < n; i++) {
            // each crack starts at a fixed point near the frame and branches inwards
            int x = 40 + (int) (Terreno.azar(i, 1, 901) * (Juego.W - 80));
            int y = 14 + (int) (Terreno.azar(i, 2, 901) * 90);
            for (int k = 0; k < 5; k++) {
                int nx = x + (int) ((Terreno.azar(i, k, 902) - 0.5f) * 40);
                int ny = y + (int) (Terreno.azar(k, i, 903) * 22);
                g.drawLine(x, y, nx, ny);
                g.drawLine(x + 1, y, nx + 1, ny);
                if (k == 2) {
                    g.drawLine(nx, ny, nx + (int) ((Terreno.azar(i, k, 904) - 0.5f) * 30), ny - 10);
                }
                x = nx;
                y = ny;
            }
        }
    }

    /** the diver's mask: dark rounded corners and a thin rim, centre left clear */
    private void visor(Graphics g) {
        int W = Juego.W, H = Juego.H;
        g.setColor(0x0b151a);
        g.fillRect(0, 0, W, 3);
        g.fillRect(0, H - 3, W, 3);
        g.fillTriangle(0, 0, 64, 0, 0, 44);
        g.fillTriangle(0, 0, 28, 0, 0, 70);
        g.fillTriangle(W, 0, W - 64, 0, W, 44);
        g.fillTriangle(W, 0, W - 28, 0, W, 70);
        g.fillTriangle(0, H, 64, H, 0, H - 44);
        g.fillTriangle(0, H, 28, H, 0, H - 70);
        g.fillTriangle(W, H, W - 64, H, W, H - 44);
        g.fillTriangle(W, H, W - 28, H, W, H - 70);
        g.setColor(0x24363e);
        g.drawLine(64, 0, 0, 44);
        g.drawLine(W - 64, 0, W, 44);
        g.drawLine(64, H - 1, 0, H - 44);
        g.drawLine(W - 64, H - 1, W, H - 44);
    }

    private static final String[] RUMBOS = { "N", "NO", "O", "SO", "S", "SE", "E", "NE" };
    // cached HUD strings
    private int o2Txt = -1, profTxt = -999, minTxt = -1, mochilaTxt = -1;
    private String o2Cad, barraSup, mochilaCad, rumboTxt;

    private void dibujarHud(Graphics g) {
        if (enRemora) {
            cabina(g);
        } else if (!fuera) {
            visor(g);
        }
        // survival meters, bottom left
        int y0 = Juego.H - 52;
        boolean parpadeo = o2 < 10 && ((int) (tiempo * 4) & 1) == 0;
        if ((int) o2 != o2Txt) {
            o2Txt = (int) o2;
            o2Cad = "O2 " + o2Txt + "s";
        }
        barra(g, 8, y0, o2Cad, o2, o2Max(), parpadeo ? 0xff5040 : 0x4fd8ff);
        barra(g, 8, y0 + 12, "Salud", salud, 100, 0xff5a5a);
        barra(g, 8, y0 + 24, "Comida", comida, 100, 0xffa040);
        barra(g, 8, y0 + 36, "Agua", agua, 100, 0x5aa0ff);
        if (equipo != EQ_NADA && !enRemora && boya <= 0) {
            g.setColor(0xf08a3a);
            String h = equipo == EQ_LINTERNA ? (linterna ? "L apagar linterna" : "L encender linterna")
                    : equipo == EQ_BOYA ? "L inflar boya (x" + inv.cant[Inventario.BOYA] + ")"
                    : equipo == EQ_BENGALA ? "L lanzar bengala (x" + inv.cant[Inventario.BENGALA] + ")"
                    : equipo == EQ_SOLDADOR ? "L (mantener) soldar la Rémora"
                    : equipo == EQ_ESCANER ? "L (mantener) escanear"
                    : (impBloq ? "Impulsor: ENFRIANDO" : "L (mantener) impulsor");
            g.drawString(h, 8, y0 - 13, Graphics.TOP | Graphics.LEFT);
            if (equipo == EQ_IMPULSOR) {
                // the thruster's charge (heat): it drains while pushing; empty = overheated
                g.setColor(0x0b1d26);
                g.fillRect(140, y0 - 10, 50, 6);
                g.setColor(impBloq ? 0xff5040 : 0x6ff0ff);
                g.fillRect(141, y0 - 9, (int) (48 * impCarga / 100), 4);
            }
        }
        if (equipo == EQ_ESCANER && escObjetivo >= 0 && !enRemora) {
            // what the scanner is pointed at, and its progress
            int sx = Juego.W / 2 - 60, sy = Juego.H / 2 + 38;
            g.setColor(0x0b1d26);
            g.fillRect(sx, sy, 120, 20);
            g.setColor(0x6ff0ff);
            boolean ya = bestiario.conocido[escObjetivo];
            g.drawString(ya ? Bestiario.NOMBRE[escObjetivo] : "Escaneando...", Juego.W / 2, sy + 1,
                    Graphics.TOP | Graphics.HCENTER);
            g.fillRect(sx + 4, sy + 14, (int) (112 * (ya ? 1 : escProgreso / 1.6f)), 3);
        }
        // compass strip (top centre), depth gauge (right) and clock (top right)
        brujula(g);
        // depth next to the compass, as before: always in view, red past the Remora's limit
        int prof = (int) -py;
        if (prof != profVisto) {
            profVisto = prof;
            profCad = prof + " m";
        }
        int bx = Juego.W / 2 - 100 - 50;
        g.setColor(0x0b1d26);
        g.fillRect(bx, 4, 46, 16);
        g.setColor(0x2fb3c0);
        g.drawRect(bx, 4, 45, 15);
        g.setColor(enRemora && prof > remoraMax() ? 0xff4030 : 0xe8f4f8);
        g.drawString(profCad, bx + 23, 6, Graphics.TOP | Graphics.HCENTER);
        int minuto = (int) (horaDia() * 1440);
        if (minuto != minTxt) {
            minTxt = minuto;
            barraSup = reloj();
        }
        float l = luzDia();
        int rx0 = Juego.W / 2 + 100 + 4;
        g.setColor(0x0b1d26);
        g.fillRect(rx0, 4, 58, 16);
        g.setColor(0x2fb3c0);
        g.drawRect(rx0, 4, 57, 15);
        g.setColor(l > 0.3f ? 0xffd040 : 0xd8e0ff);
        g.fillArc(rx0 + 4, 8, 8, 8, 0, 360);
        if (l <= 0.3f) {
            g.setColor(0x0b1d26);
            g.fillArc(rx0 + 7, 7, 8, 8, 0, 360); // a crescent at night
        }
        g.setColor(0xe8f4f8);
        g.drawString(barraSup, rx0 + 54, 6, Graphics.TOP | Graphics.RIGHT);
        // inventory count, bottom right
        int tot = inv.total();
        if (tot != mochilaTxt) {
            mochilaTxt = tot;
            mochilaCad = "Mochila " + tot + "/" + inv.capacidad();
        }
        g.drawString(mochilaCad, Juego.W - 8, Juego.H - 14,
                Graphics.TOP | Graphics.RIGHT);
        // tutorial hint
        if (tutorial < TUTORIAL.length && estado == JUEGO && historia.etapa < 2) {
            String h = TUTORIAL[tutorial];
            int w = h.length() * 6 + 16;
            g.setColor(0x0b2a33);
            g.fillRect(Juego.W / 2 - w / 2, Juego.H - 78, w, 14);
            g.setColor(0x9fffa0);
            g.drawString(h, Juego.W / 2, Juego.H - 76, Graphics.TOP | Graphics.HCENTER);
        }
        // status effects
        if (ceguera > 0) {
            // blinded by a screech: the view closes in from the edges
            int bw = (int) (Math.min(1, ceguera / 1.5f) * 150);
            g.setColor(0x02060a);
            g.fillRect(0, 0, bw, Juego.H);
            g.fillRect(Juego.W - bw, 0, bw, Juego.H);
            g.fillRect(0, 0, Juego.W, bw / 2);
            g.fillRect(0, Juego.H - bw / 2, Juego.W, bw / 2);
        }
        if (veneno > 0 || aturdido > 0 || vision > 0 || mareo > 0) {
            g.setColor(veneno > 0 ? 0x9aff40 : (aturdido > 0 ? 0x8ae0ff : (vision > 0 ? 0x6affc0 : 0xd0a0ff)));
            g.drawString(veneno > 0 ? "ENVENENADO" : (aturdido > 0 ? "ATURDIDO" : (vision > 0 ? "VISIÓN NOCTURNA" : "MAREADO")),
                    8, Juego.H - 64, Graphics.TOP | Graphics.LEFT);
        }
        if (hostiles.descarga > 0) {
            // the jelly's discharge: arcs across the view
            g.setColor(0xbff0ff);
            for (int i = 0; i < 5; i++) {
                int xa = Juego.W / 2, ya = Juego.H / 2;
                for (int sg = 0; sg < 4; sg++) {
                    int xb = xa + (int) ((Terreno.azar(i, sg, (int) (tiempo * 30)) - 0.5f) * 120);
                    int yb = ya + (int) ((Terreno.azar(sg, i, (int) (tiempo * 30) + 3) - 0.5f) * 90);
                    g.drawLine(xa, ya, xb, yb);
                    xa = xb;
                    ya = yb;
                }
            }
        }
        // the mission signal: a marker on screen (or an arrow at the edge) with its distance
        if (historia.objetivo() >= 0 && estado == JUEGO) {
            marcador(g);
        }
        if (!inv.tiene[Inventario.H_RADAR] && estado == JUEGO && historia.objetivo() != Historia.LUGARES
                && px * px + pz * pz > 25 * 25) {
            marcadorCapsula(g);
        }
        if (historia.radioNueva() && estado == JUEGO && ((int) (tiempo * 2) & 1) == 0) {
            g.setColor(0xffd040);
            g.drawString("Mensaje nuevo en la radio de la cápsula", Juego.W / 2, 40, Graphics.TOP | Graphics.HCENTER);
        }
        // handheld radar (the Remora has its own on the dashboard)
        if (inv.tiene[Inventario.H_RADAR] && !enRemora && !fuera) {
            Radar.dibujar(g, this, Juego.W - 36, Juego.H - 52, 24, 60);
        }
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

    private int marcaDist = -1;
    private String marcaCad;

    /** on-screen marker towards the mission signal */
    private void marcador(Graphics g) {
        int dist = marcadorEn(g, historia.objX(), historia.objY(), historia.objZ(), 0xffd040, marcaDist, true);
        if (dist != marcaDist) {
            marcaDist = dist;
            marcaCad = (historia.objetivo() == Historia.LUGARES ? "CÁPSULA " : "SEÑAL ") + dist + " m";
        }
    }

    private int capDist = -1;
    private String capCad;

    /** the escape pod is always marked while there is no radar, so you can find your way back */
    private void marcadorCapsula(Graphics g) {
        int dist = marcadorEn(g, 0, -1, 0, 0xffffff, capDist, false);
        if (dist != capDist) {
            capDist = dist;
            capCad = "CÁPSULA " + dist + " m";
        }
    }

    /**
     * Draws a diamond marker towards world (x, y, z) - on screen if in front, at the edge
     * otherwise - with its label; returns the distance in metres.
     */
    private int marcadorEn(Graphics g, float tx, float ty, float tz, int color, int distAntes, boolean mision) {
        float ox = tx - px, oy = ty - py, oz = tz - pz;
        float dh = j2me.FMath.sqrt(ox * ox + oz * oz);
        int dist = (int) j2me.FMath.sqrt(dh * dh + oy * oy);
        float rumboObj = j2me.FMath.atan2(-ox, -oz) * 57.29578f;
        float d = Peces.angulo(rumboObj - yaw);
        float elev = j2me.FMath.atan2(oy, Math.max(dh, 0.1f)) * 57.29578f - pitch;
        int x, y;
        if (d > -40 && d < 40) {
            x = Juego.W / 2 - (int) (j2me.FMath.tan(d * j2me.FMath.RAD) * FOCAL);
        } else {
            x = d > 0 ? 14 : Juego.W - 14;
        }
        y = Juego.H / 2 - (int) (j2me.FMath.tan(Math.max(-40, Math.min(40, elev)) * j2me.FMath.RAD) * FOCAL);
        x = Math.max(14, Math.min(Juego.W - 14, x));
        y = Math.max(30, Math.min(Juego.H - 70, y));
        g.setColor(color);
        g.drawLine(x, y - 5, x + 5, y);
        g.drawLine(x + 5, y, x, y + 5);
        g.drawLine(x, y + 5, x - 5, y);
        g.drawLine(x - 5, y, x, y - 5);
        String cad = mision ? marcaCad : capCad;
        if (cad != null) {
            int ancla = x < 80 ? Graphics.LEFT : (x > Juego.W - 80 ? Graphics.RIGHT : Graphics.HCENTER);
            g.drawString(cad, ancla == Graphics.LEFT ? x - 5 : (ancla == Graphics.RIGHT ? x + 5 : x), y + 8,
                    Graphics.TOP | ancla);
        }
        return dist;
    }


    private static final String[] CARDINALES = { "N", "NE", "E", "SE", "S", "SO", "O", "NO" };

    /** heading on the compass, degrees clockwise from north */
    private float rumboGrados() {
        float h = (360 - yaw) % 360;
        return h < 0 ? h + 360 : h;
    }

    /** a strip 200 px wide showing 120 degrees around the heading, with marks for the signal */
    private void brujula(Graphics g) {
        int cx = Juego.W / 2, w = 200, y = 4;
        float h = rumboGrados();
        g.setColor(0x0b1d26);
        g.fillRect(cx - w / 2, y, w, 16);
        g.setColor(0x2fb3c0);
        g.drawRect(cx - w / 2, y, w - 1, 15);
        float ppg = w / 120f; // pixels per degree
        int base = ((int) (h - 60) / 15) * 15 - 15;
        for (int a = base; a <= h + 75; a += 15) {
            float rel = Peces.angulo(a - h);
            if (rel < -58 || rel > 58) {
                continue;
            }
            int x = cx + (int) (rel * ppg);
            int ang = ((a % 360) + 360) % 360;
            if (ang % 45 == 0) {
                g.setColor(ang == 0 ? 0xff6a4a : 0xe8f4f8);
                g.drawString(CARDINALES[ang / 45], x, y + 2, Graphics.TOP | Graphics.HCENTER);
            } else {
                g.setColor(0x6a9aa4);
                g.drawLine(x, y + 11, x, y + 14);
            }
        }
        // signal, pod and base on the strip
        if (historia.objetivo() >= 0) {
            marcaBrujula(g, historia.objX(), historia.objZ(), 0xffd040, h, cx, w, y);
        }
        marcaBrujula(g, 0, 0, 0xffffff, h, cx, w, y);
        if (hayBase) {
            marcaBrujula(g, baseX, baseZ, 0xffa0ff, h, cx, w, y);
        }
        for (int i = 0; i < NSENAL; i++) {
            if (sbPuesta[i]) {
                marcaBrujula(g, sbX[i], sbZ[i], COLOR_SENAL, h, cx, w, y);
            }
        }
        // the heading needle
        g.setColor(0xffd040);
        g.fillTriangle(cx - 4, y + 16, cx + 4, y + 16, cx, y + 11);
    }

    private void marcaBrujula(Graphics g, float tx, float tz, int color, float h, int cx, int w, int y) {
        float ox = tx - px, oz = tz - pz;
        if (ox * ox + oz * oz < 4) {
            return;
        }
        // bearing clockwise from north: north is -Z, east is +X
        float b = j2me.FMath.atan2(ox, -oz) * 57.29578f;
        float rel = Peces.angulo(b - h);
        int x = rel < -58 ? cx - w / 2 + 3 : (rel > 58 ? cx + w / 2 - 3 : cx + (int) (rel * w / 120f));
        g.setColor(color);
        g.fillTriangle(x - 3, y + 17, x + 3, y + 17, x, y + 21);
    }

    private int profVisto = -999;
    private String profCad;

    /** depth gauge on the right: the depth in big digits and a bar against the safe limit */
    private void profundimetro(Graphics g) {
        int prof = (int) -py;
        float limite = enRemora ? remoraMax() : 250;
        boolean peligro = enRemora && prof > remoraMax();
        int x = Juego.W - 26, y0 = 44, alto = 110;
        g.setColor(0x0b1d26);
        g.fillRect(x - 2, y0 - 2, 10, alto + 4);
        float f = Math.min(1, prof / limite);
        g.setColor(peligro ? 0xff4030 : (f > 0.8f ? 0xffa040 : 0x4fd8ff));
        g.fillRect(x, y0, 6, (int) (alto * f));
        g.setColor(0x2fb3c0);
        g.drawRect(x - 2, y0 - 2, 9, alto + 3);
        for (int i = 1; i < 4; i++) {
            g.drawLine(x - 4, y0 + alto * i / 4, x - 2, y0 + alto * i / 4);
        }
        if (prof != profVisto) {
            profVisto = prof;
            profCad = prof + " m";
        }
        g.setColor(peligro ? 0xff4030 : 0xe8f4f8);
        g.drawString(profCad, x + 8, y0 + alto + 6, Graphics.TOP | Graphics.RIGHT);
    }

    private String rumbo() {
        float a = yaw % 360;
        if (a < 0) {
            a += 360;
        }
        // yaw 0 looks towards -Z = north; yaw grows turning left (west)
        String[] n = RUMBOS;
        return n[(int) ((a + 22.5f) / 45) % 8];
    }

    private String accionDisponible() {
        if (px * px + pz * pz < 5.5f * 5.5f && py > -4) {
            return "[] Entrar en la cápsula";
        }
        if (cercaBase() && !enRemora) {
            return "[] Entrar en tu base";
        }
        if (enRemora) {
            // the hint only shows for a few seconds after boarding
            return bordoT > 0 ? "[] Salir de la Rémora" : null;
        }
        if (mundo.vejigaCerca(px, py, pz, sel)) {
            return mundo.vejigaFalta(sel[0], sel[1]) > 0 ? "Vejiga de aire (rellenándose)" : "[] Respirar de la vejiga de aire";
        }
        if (mundo.cajaCerca(px, py, pz, sel)) {
            return "[] Abrir caja de suministros";
        }
        if (ecos.cerca(px, py, pz) >= 0) {
            return "[] Escuchar el eco de datos";
        }
        if (balizaCerca() >= 0) {
            return "[] Recoger baliza de señal";
        }
        String resto = mundo.restoCercano(px, py, pz, 4.5f);
        if (resto != null && mundo.recogidos.get(resto) == null) {
            return "[] Revisar restos de la estación";
        }
        String lugar = historia.accion(px, py, pz);
        if (lugar != null) {
            return lugar;
        }
        if (depredadores.sobreErrante(px, py, pz)) {
            return depredadores.eCoral > 0 ? "[] Cortar coral del errante" : "Lomo del errante (sin coral)";
        }
        if (inv.tiene[Inventario.H_REMORA] && cercaRemora() && rAveriada) {
            return inv.cant[Inventario.KIT] > 0 ? "[] Reparar la Rémora (kit)" : "Rémora averiada (falta kit)";
        }
        if (inv.tiene[Inventario.H_REMORA] && cercaRemora()) {
            return "[] Subir a la Rémora";
        }
        if (armaEnMano() && (depredadores.alAlcance(px, py, pz, fx, fy, fz)
                || hostiles.alAlcance(px, py, pz, fx, fy, fz))) {
            return arma == ARMA_VARA ? "[] Descarga" : "[] Atacar con el cuchillo";
        }
        int pez = peces.alcanzable(px, py, pz);
        if (pez >= 0 && inv.total() < inv.capacidad()) {
            return "[] Atrapar: " + NOMBRE_PEZ[peces.especie[pez]];
        }
        if (armaEnMano() && peces.alAlcance(px, py, pz, fx, fy, fz) >= 0) {
            return arma == ARMA_VARA ? "[] Descarga" : "[] Atacar con el cuchillo";
        }
        if (mundo.alcanzable(px, py, pz, fx, fy, fz, sel)) {
            int tipo = mundo.chunks[sel[0]].tipo[sel[1]];
            int item = Mundo.DA[tipo];
            if (Mundo.CUCHILLO[tipo] && !cuchilloEnMano()) {
                return inv.tiene[Inventario.H_CUCHILLO] ? "Equipa el cuchillo (PDA, L)" : "Necesitas un cuchillo";
            }
            return "[] " + (Mundo.CUCHILLO[tipo] ? "Cortar " : "Recoger ") + Inventario.NOMBRE[item];
        }
        return null;
    }

    void panelCaja(Graphics g, int x, int y, int w, int h) {
        panel(g, x, y, w, h);
    }

    /** a text panel (radio messages, the ending) */
    void panelTexto(Graphics g, String[] r) {
        int h = 30 + r.length * 16;
        int y0 = Math.max(10, (Juego.H - h) / 2);
        panel(g, 30, y0, 420, h);
        g.setColor(0x7fe8f4);
        g.drawString(r[0], 44, y0 + 10, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xffffff);
        for (int i = 1; i < r.length; i++) {
            g.drawString(r[i], 44, y0 + 12 + i * 16, Graphics.TOP | Graphics.LEFT);
        }
    }

    /** the interaction label under the crosshair */
    void etiqueta(Graphics g, String s) {
        int w = s.length() * 6 + 16;
        g.setColor(0x0b1d26);
        g.fillRect(Juego.W / 2 - w / 2, Juego.H / 2 + 20, w, 14);
        g.setColor(0xffffff);
        g.drawString(s, Juego.W / 2, Juego.H / 2 + 22, Graphics.TOP | Graphics.HCENTER);
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
        int vis = 8;
        int ini = Math.max(0, Math.min(nPda - vis, posPda - vis / 2));
        int ySel = 68;
        for (int k = 0; k < vis && ini + k < nPda; k++) {
            int i = ini + k;
            int c = listaPda[i];
            int y = 68 + k * 13;
            if (i == posPda) {
                g.setColor(0x1d4a58);
                g.fillRect(90, y - 2, 300, 12);
                ySel = y;
            }
            if (c < 1000) {
                g.setColor(0xffffff);
                g.drawString(Inventario.NOMBRE[c], 100, y, Graphics.TOP | Graphics.LEFT);
                g.drawString((equipoDe(c) >= 0 && equipo == equipoDe(c) ? "[izq] " : "") + "x" + inv.cant[c], 380, y,
                        Graphics.TOP | Graphics.RIGHT);
            } else {
                g.setColor(0x9fe0ff);
                g.drawString(Inventario.HERRAMIENTA[c - 1000], 100, y, Graphics.TOP | Graphics.LEFT);
                boolean der = armaDe(c) >= 0 && arma == armaDe(c) && armaEnMano();
                boolean izq = equipoDe(c) >= 0 && equipo == equipoDe(c);
                g.drawString(der ? "[der]" : (izq ? "[izq]" : "herramienta"), 380, y, Graphics.TOP | Graphics.RIGHT);
            }
        }
        if (nPda == 0) {
            g.setColor(0x6a8a94);
            g.drawString("Mochila vacia", 100, 68, Graphics.TOP | Graphics.LEFT);
        }
        // the two hands
        g.setColor(0x0b2430);
        g.fillRect(90, 178, 300, 13);
        g.setColor(0xf0b060);
        g.drawString("Mano izq (L): " + EQ_NOMBRE[equipo], 96, 179, Graphics.TOP | Graphics.LEFT);
        g.drawString("Mano der: " + (armaEnMano() ? ARMA_NOMBRE[arma] : "-"), 384, 179, Graphics.TOP | Graphics.RIGHT);
        g.setColor(0x9fc4cc);
        g.drawString("X opciones   [] mapa   R registros   L bestiario", 96, 208, Graphics.TOP | Graphics.LEFT);
        g.drawString("Mochila " + inv.total() + "/" + inv.capacidad(), 384, 50, Graphics.TOP | Graphics.RIGHT);
        // the action menu next to the selected entry
        if (menuPda >= 0) {
            int mx = 236, my = Math.min(ySel + 12, 200 - nMenu * 13), mw = 150;
            g.setColor(0x06141c);
            g.fillRect(mx, my, mw, nMenu * 13 + 6);
            g.setColor(0x2fb3c0);
            g.drawRect(mx, my, mw - 1, nMenu * 13 + 5);
            for (int k = 0; k < nMenu; k++) {
                int y = my + 4 + k * 13;
                if (k == menuPda) {
                    g.setColor(0x1d6a78);
                    g.fillRect(mx + 2, y - 2, mw - 4, 12);
                }
                g.setColor(k == menuPda ? 0xffffff : 0xb8d4dc);
                g.drawString(menuOp[k], mx + 8, y, Graphics.TOP | Graphics.LEFT);
            }
        }
        // objectives on the right side of the screen
        String[] ob = objetivos();
        g.setColor(0x06141c);
        g.fillRect(0, 232, Juego.W, 40);
        g.setColor(0x7fe8f4);
        // the mission on its own row, the other goals in two columns below it
        g.drawString("OBJETIVOS   " + ob[0], 8, 234, Graphics.TOP | Graphics.LEFT);
        for (int i = 1; i < ob.length; i++) {
            g.setColor(ob[i].startsWith("OK") ? 0x6a8a94 : 0xffffff);
            g.drawString(ob[i], 8 + ((i - 1) % 2) * 236, 245 + ((i - 1) / 2) * 9, Graphics.TOP | Graphics.LEFT);
        }
    }

    private void dibujarFabricador(Graphics g) {
        panel(g, 30, 26, 420, 220);
        // tabs
        int nt = Inventario.CATEGORIAS.length;
        int tw = 404 / nt;
        for (int i = 0; i < nt; i++) {
            int x = 38 + i * tw;
            g.setColor(i == tabFab ? 0x1d6a78 : 0x0f2a33);
            g.fillRect(x, 32, tw - 4, 14);
            g.setColor(i == tabFab ? 0xffffff : 0x7a9aa4);
            g.drawString(Inventario.CATEGORIAS[i], x + (tw - 4) / 2, 34, Graphics.TOP | Graphics.HCENTER);
        }
        int n = recetasDeTab();
        int vis = 6;
        int ini = Math.max(0, Math.min(n - vis, selFab - vis / 2));
        for (int k = 0; k < vis && ini + k < n; k++) {
            int i = ini + k;
            Inventario.Receta r = Inventario.RECETAS[listaFab[i]];
            int y = 54 + k * 26;
            boolean hecho = r.herramienta >= 0 && inv.tiene[r.herramienta];
            boolean puede = inv.puede(r);
            if (i == selFab) {
                g.setColor(0x1d4a58);
                g.fillRect(40, y - 3, 400, 25);
            }
            g.setColor(hecho ? 0x6a8a94 : (puede ? 0x9fffa0 : 0xffffff));
            g.drawString(r.nombre + (hecho ? "  (hecho)" : (puede ? "  - listo" : "")), 48, y, Graphics.TOP | Graphics.LEFT);
            g.setColor(hecho ? 0x5a7a84 : (puede ? 0x9fc4cc : 0xd09090));
            g.drawString(inv.ingredientesCon(r), 48, y + 10, Graphics.TOP | Graphics.LEFT);
        }
        if (n > 0) {
            Inventario.Receta r = Inventario.RECETAS[listaFab[Math.min(selFab, n - 1)]];
            g.setColor(0x7fe8f4);
            g.drawString(r.efecto, 48, 214, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0x9fc4cc);
        g.drawString("L/R pestanas  X fabricar  O salir", 440, 230, Graphics.TOP | Graphics.RIGHT);
        g.drawString("Mochila " + inv.total() + "/" + inv.capacidad(), 48, 230, Graphics.TOP | Graphics.LEFT);
    }


    private void dibujarPausa(Graphics g) {
        panel(g, 100, 40, 280, 206);
        g.setColor(0x7fe8f4);
        g.drawString("PAUSA", Juego.W / 2, 50, Graphics.TOP | Graphics.HCENTER);
        String[] op = { "Continuar", "Mirar con: " + (Opciones.cruceta ? "CRUCETA" : "STICK"),
                "Prueba - viajar a: < " + (zonaViaje == SENAL ? "señal de la misión" : Mundo.nombreBioma(zonaViaje)) + " >",
                "Prueba - oxígeno infinito: " + (o2Infinito ? "SI" : "NO"),
                "Prueba - adelantar 3 h (" + reloj() + ")", "Prueba - dar materiales y herramientas",
                "Prueba - saltar misión (" + historia.etapa + ")", "Salir al título" };
        for (int i = 0; i < op.length; i++) {
            g.setColor(i == selPausa ? 0xffffff : 0x6a8a94);
            g.drawString((i == selPausa ? "> " : "  ") + op[i], Juego.W / 2, 62 + i * 16,
                    Graphics.TOP | Graphics.HCENTER);
        }
        g.setColor(0x9fc4cc);
        g.drawString(selPausa == 2 ? "<- -> elegir zona, X viajar" : (Opciones.cruceta
                ? "Cruceta: girar y mirar. X nadar" : "Stick: mirar. X nadar hacia donde miras"), Juego.W / 2, 196,
                Graphics.TOP | Graphics.HCENTER);
        g.drawString("L linterna  [] usar  /\\ PDA", Juego.W / 2, 212, Graphics.TOP | Graphics.HCENTER);
    }

    private void dibujarMuerte(Graphics g) {
        g.setColor(0x200606);
        g.fillRect(0, 100, Juego.W, 70);
        g.setColor(0xff8a7a);
        g.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE));
        g.drawString("HAS PERDIDO EL CONOCIMIENTO", Juego.W / 2, 110, Graphics.TOP | Graphics.HCENTER);
        g.setFont(Font.getDefaultFont());
        g.setColor(0xffffff);
        g.drawString("Pulsa X para despertar en la cápsula", Juego.W / 2, 145, Graphics.TOP | Graphics.HCENTER);
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
