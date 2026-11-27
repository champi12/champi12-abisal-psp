package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

import j2me.FMath;

/**
 * Hostile fauna. Only a few individuals live near the player at any time; they are
 * spawned out of sight in their biome and removed when left far behind.
 *
 * Sawtooth (red plateau): lurks near the floor, charges when the diver comes close, bites
 * and swims off to circle back later. A knife hit makes it flee.
 * Ironback crab (red plateau, canyons): walks the floor, chases a diver who comes down to
 * its level and pinches. Loses interest if you swim up. A knife hit makes it back off.
 * Ash Colossus (abyss and the void): one huge leviathan roams the dark. It roars when it
 * notices the diver and then charges. The knife does nothing to it: flee upwards.
 */
public final class Depredadores {
    static final int ND = 2, NA = 3;
    static final int MERODEAR = 0, CARGAR = 1, RETIRARSE = 2, HUIR = 3;
    static final String[] NOMBRE = { "dientesierra", "anguila de brasa", "acechador de algas", "mordedor de pasto",
            "guardián", "gusano de arena" };
    static final int GUSANO = 5;

    // jointed parts: serpent segments (sawtooth, ember eel) and the crab's legs and claws
    private final Mesh[] segDiente, segAnguila, segAcechador, segMordedor;
    // redesigned ambushers: the squid-like kelp lurker and the toad-like grass biter
    private final Mesh[] mCalamar, mSapo;

    // cave spitters: territorial cave fish that spit acid; a few acid blobs in flight
    static final int NS = 2, NB = 6;
    final boolean[] sVivo = new boolean[NS];
    final float[] sx = new float[NS], sy = new float[NS], sz = new float[NS];
    final float[] shx = new float[NS], shy = new float[NS], shz = new float[NS];
    final float[] sT = new float[NS], sRumbo = new float[NS];
    final int[] sVida = new int[NS];
    final boolean[] bVivo = new boolean[NB];
    final float[] bx = new float[NB], by = new float[NB], bz = new float[NB];
    final float[] bvx = new float[NB], bvy = new float[NB], bvz = new float[NB], bt = new float[NB];
    private final Mesh[] mEscupidor;
    private final Mesh mAcido;
    private final float[] refugio = new float[3];
    private final Mesh mCangrejo, mPata, mPinza;
    private final Mesh[] mColosoP, mErrante, segGuardian, mMedusa;

    // the Void Guardian: an armoured leviathan (~45 m) patrolling the edge of the void
    boolean gVivo;
    float gx, gy, gz, gvx, gvy, gvz, gRumbo, gT;
    int gEstado;
    boolean gAviso;
    static final float ESC_GUARDIAN = 10;

    // the moon jelly: a huge, peaceful glowing jelly that only drifts at night
    boolean mlVivo;
    float mlx, mly, mlz, mlFase, mlRumbo;
    static final float ESC_MEDUSA = 8;
    private final Mesh mCardumen;

    // reef wanderer: one huge gentle giant drifting through the kelp, meadows and plateau
    boolean eVivo;
    float ex, ey, ez, eRumbo, eFase;
    static final float ESC_ERRANTE = 2.2f;
    /** a lurker hiding in grass patch k of the wanderer's back (-1 = not on it) */
    final int[] dParche = { -1, -1 };
    /** coral left on its back to harvest (only found here), and its parasites */
    int eCoral, eCoralCut;
    private final Mesh mParasitos;

    // abyssal veil: a peaceful ribbon giant (~36 m) undulating through the abyss; the
    // prowlers keep away from it, so its glow is a safe place in the deep
    static final int NV = 12;
    static final float SEP_VELO = 3.0f;
    boolean vVivo;
    final float[] vsx = new float[NV], vsy = new float[NV], vsz = new float[NV];
    float vRumbo, vFase, vSube;
    private final Mesh[] mVelo;
    private final Mesh[] mGusano;
    private final Mesh mGusanoMound;
    private final Transform t = new Transform();
    private final Transform tp = new Transform();
    private int semilla = 4242;

    // sawtooth
    final boolean[] dVivo = new boolean[ND];
    final float[] dx = new float[ND], dy = new float[ND], dz = new float[ND];
    final float[] dvx = new float[ND], dvy = new float[ND], dvz = new float[ND];
    final float[] dRumbo = new float[ND];
    final int[] dEstado = new int[ND];
    final float[] dT = new float[ND];
    final int[] dVida = new int[ND];
    /** 0 = sawtooth (red plateau), 1 = ember eel (thermal rifts) */
    final int[] dTipo = new int[ND];
    /** a lurker waiting still in tall grass until the diver comes close */
    final boolean[] dOculto = new boolean[ND];

    // ironback crab
    final boolean[] aVivo = new boolean[NA];
    final float[] ax = new float[NA], ay = new float[NA], az = new float[NA];
    final float[] aRumbo = new float[NA];
    final int[] aEstado = new int[NA];
    final float[] aT = new float[NA];
    final float[] aFase = new float[NA];
    final int[] aVida = new int[NA];

    // ash colossus
    boolean cVivo;
    float cx, cy, cz, cvx, cvy, cvz, cRumbo;
    int cEstado;
    /** the giants can be brought down too, but it takes many blows */
    int cVida, gVida;
    float cT;
    boolean cAviso;

    int dibujados;
    private float tiempo;

    Depredadores(Fog fog) {
        segDiente = Modelos.serpiente(fog, 0);
        segAnguila = Modelos.serpiente(fog, 1);
        segAcechador = null;
        segMordedor = null;
        mCalamar = Modelos.calamar(fog);
        mSapo = Modelos.sapo(fog);
        mEscupidor = Modelos.escupidor(fog);
        mAcido = Modelos.acido(fog);
        mCangrejo = Modelos.acorazadoCuerpo(fog);
        mPata = Modelos.acorazadoPata(fog);
        mPinza = Modelos.acorazadoPinza(fog);
        // the colossus' outer wings are separate parts: they beat slowly
        mColosoP = Modelos.constructor(20, fog).buildPartes(fog, 255, true, 0,
                new float[][] { { 8, 99, -99, 99, -99, 99 }, { -99, -8, -99, 99, -99, 99 },
                        { -3, 3, -99, 99, -99, -15 }, { -3, 3, -99, -3, 13, 99 } }); // wings, tail, lures
        mErrante = Modelos.errante(fog);
        segGuardian = Modelos.guardian(fog);
        mMedusa = Modelos.constructor(4, fog).buildPartes(fog, 175, true, 0,
                new float[][] { { -99, 99, -99, -0.05f, -99, 99 } });
        mCardumen = Modelos.cardumen(fog);
        mParasitos = Modelos.parasitos(fog);
        mGusano = Modelos.gusanoArena(fog);
        mGusanoMound = Modelos.montonArena(fog);
        mVelo = new Mesh[] { Modelos.veloParte(fog, 0), Modelos.veloParte(fog, 1), Modelos.veloParte(fog, 2) };
    }

    private float azar() {
        semilla = semilla * 1103515245 + 12345;
        return ((semilla >>> 8) & 0xffff) / 65535f;
    }

    /** a spawn point 40-70 m from the player in one of the given biomes, or false */
    private boolean puntoEn(float px, float pz, float dmin, float dmax, int bioA, int bioB, float[] out) {
        for (int intento = 0; intento < 4; intento++) {
            float a = azar() * 6.2832f;
            float d = dmin + azar() * (dmax - dmin);
            float x = px + FMath.cos(a) * d;
            float z = pz + FMath.sin(a) * d;
            int b = Terreno.bioma(x, z);
            if (b == bioA || b == bioB) {
                out[0] = x;
                out[1] = z;
                return true;
            }
        }
        return false;
    }

    private final float[] punto = new float[2];
    private final float[] tmp = new float[3];

    void actualizar(float dt, Partida p) {
        tiempo += dt;
        float px = p.px, py = p.py, pz = p.pz;
        int bio = Terreno.bioma(px, pz);
        boolean bajoAgua = py < -3;

        // ---------------------------------------------------------- sawtooth
        for (int i = 0; i < ND; i++) {
            if (!dVivo[i]) {
                boolean termal = bio == Terreno.TERMAL;
                boolean pasto = bio == Terreno.BOSQUE || bio == Terreno.PRADERA;
                boolean escondido = false;
                boolean hay = false;
                int otro = 1 - i;
                if (eVivo && dParche[otro] < 0 && azar() < 0.03f) {
                    float ox = px - ex, oz = pz - ez;
                    if (ox * ox + oz * oz < 90 * 90) {
                        // a grass biter crouched in one of the patches on its back
                        int k = (int) (azar() * Modelos.PARCHES) % Modelos.PARCHES;
                        dVivo[i] = true;
                        dTipo[i] = 3;
                        dVida[i] = 2;
                        dOculto[i] = true;
                        dParche[i] = k;
                        dEstado[i] = MERODEAR;
                        posParche(i);
                        Log.d("mordedor escondido en el lomo del errante");
                        continue;
                    }
                }
                if (bio == Terreno.PRADERA && (i & 1) == 1 && azar() < 0.05f
                        && puntoEn(px, pz, 14, 40, bio, bio, punto)) {
                    // a sand worm buried in the open dunes, waiting
                    dVivo[i] = true;
                    dParche[i] = -1;
                    dTipo[i] = GUSANO;
                    dVida[i] = 3;
                    dx[i] = punto[0];
                    dz[i] = punto[1];
                    dy[i] = Terreno.altura(dx[i], dz[i]) - 0.6f;
                    dOculto[i] = true;
                    dEstado[i] = MERODEAR;
                    dRumbo[i] = azar() * 360;
                    Log.d("gusano de arena enterrado en " + (int) dx[i] + "," + (int) dz[i]);
                    continue;
                }
                if (pasto && azar() < 0.02f && p.mundo.hierbaCercana(px, pz, 22, 60, tmp)
                        && !(dVivo[otro] && Math.abs(dx[otro] - tmp[0]) + Math.abs(dz[otro] - tmp[2]) < 6)) {
                    // a lurker crouched inside a clump of tall grass
                    punto[0] = tmp[0];
                    punto[1] = tmp[2];
                    hay = escondido = true;
                } else if ((bio == Terreno.MESETA || termal || (bio == Terreno.BOSQUE && i == 0)) && azar() < 0.01f
                        && puntoEn(px, pz, pasto ? 30 : 45, pasto ? 50 : 70, bio, bio, punto)) {
                    hay = true;
                }
                if (hay) {
                    dVivo[i] = true;
                    dParche[i] = -1;
                    // in the grass hides the small grass biter; the kelp lurker roams the groves
                    dTipo[i] = escondido ? 3 : (termal ? 1 : (pasto ? 2 : 0));
                    dVida[i] = escondido ? 2 : (termal || pasto ? 3 : 4);
                    dx[i] = punto[0];
                    dz[i] = punto[1];
                    dy[i] = Terreno.altura(dx[i], dz[i]) + (escondido ? 0.3f : 3);
                    dOculto[i] = escondido;
                    dEstado[i] = MERODEAR;
                    dRumbo[i] = azar() * 360;
                    Log.d(NOMBRE[dTipo[i]] + " aparece en " + (int) dx[i] + "," + (int) dy[i] + "," + (int) dz[i]);
                }
                continue;
            }
            if (dParche[i] >= 0) {
                if (!eVivo) {
                    dVivo[i] = false;
                    dParche[i] = -1;
                    continue;
                }
                posParche(i);
                float ox = px - dx[i], oy = py - dy[i], oz = pz - dz[i];
                if (bajoAgua && ox * ox + oy * oy + oz * oz < 7 * 7) {
                    dParche[i] = -1;
                    dOculto[i] = false;
                    dEstado[i] = CARGAR;
                    p.avisar("Algo salta de la hierba del lomo!");
                } else {
                    continue;
                }
            }
            float ox = px - dx[i], oy = py - dy[i], oz = pz - dz[i];
            float d = FMath.sqrt(ox * ox + oy * oy + oz * oz);
            if (d > 130 || (dOculto[i] && d > 70)) {
                dVivo[i] = false; // hidden ones left behind make room for others
                continue;
            }
            if (dTipo[i] == GUSANO && dOculto[i]) {
                // buried: it bursts out of the sand when something swims over it
                if (bajoAgua && ox * ox + oz * oz < 14 * 14 && oy < 14 && oy > -3) {
                    dOculto[i] = false;
                    dEstado[i] = CARGAR;
                    dy[i] = Terreno.altura(dx[i], dz[i]) + 0.3f;
                    dvy[i] = 6;
                    p.avisar("Algo sale de la arena!");
                    Sonido.tocar(Sonido.RUGIDO);
                    Log.d("gusano de arena ataca");
                }
                continue;
            }
            float vel;
            float tx, ty, tz;
            dT[i] -= dt;
            switch (dEstado[i]) {
            case CARGAR:
                vel = dTipo[i] == 1 ? 10 : (dTipo[i] == 2 ? 11 : (dTipo[i] == 3 ? 9.5f : (dTipo[i] == GUSANO ? 12 : 8.5f)));
                tx = ox;
                ty = oy;
                tz = oz;
                if (!bajoAgua || d > 45) {
                    dEstado[i] = MERODEAR;
                } else if (d < (p.enRemora ? 3.6f : 2.2f)) {
                    // aboard, the hull keeps it at arm's length: it bites the hull from there
                    // (before it could never get close enough and just circled the Remora)
                    p.danar(dTipo[i] == 1 ? 18 : (dTipo[i] == 2 ? 16 : (dTipo[i] == 3 ? 10 : (dTipo[i] == GUSANO ? 14 : 22))),
                            "Te mordió: " + NOMBRE[dTipo[i]]);
                    dEstado[i] = RETIRARSE;
                    dT[i] = 6;
                    if (d > 0.01f) {
                        dvx[i] = -ox / d * 7;
                        dvy[i] = -oy / d * 3 + 1;
                        dvz[i] = -oz / d * 7;
                    }
                }
                break;
            case RETIRARSE:
            case HUIR:
                vel = dEstado[i] == HUIR ? 9 : 6;
                tx = -ox;
                ty = 0.3f;
                tz = -oz;
                if (dT[i] <= 0) {
                    dEstado[i] = MERODEAR;
                }
                break;
            default:
                // the kelp lurker creeps low and strikes only from close by; hidden in the
                // grass it does not move at all
                boolean acecha = dTipo[i] >= 2;
                vel = dOculto[i] ? 0 : (acecha ? 1.2f : 2.2f);
                dRumbo[i] += (azar() - 0.5f) * 60 * dt;
                tx = FMath.cos(dRumbo[i] * FMath.RAD);
                tz = FMath.sin(dRumbo[i] * FMath.RAD);
                ty = (Terreno.altura(dx[i], dz[i]) + (acecha ? 1.5f : 3) - dy[i]) * 0.3f;
                if (bajoAgua && d < (dOculto[i] ? 9 : (acecha ? 14 : 28))) {
                    if (dOculto[i]) {
                        dOculto[i] = false;
                        p.avisar("Algo salta de la hierba!");
                    }
                    dEstado[i] = CARGAR;
                    Log.d(NOMBRE[dTipo[i]] + " carga contra el jugador (" + (int) d + " m)");
                }
                break;
            }
            float tl = FMath.sqrt(tx * tx + ty * ty + tz * tz);
            if (tl > 0.001f) {
                float k = Math.min(1, dt * 2.2f);
                dvx[i] += (tx / tl * vel - dvx[i]) * k;
                dvy[i] += (ty / tl * vel - dvy[i]) * k;
                dvz[i] += (tz / tl * vel - dvz[i]) * k;
            }
            dx[i] += dvx[i] * dt;
            dy[i] += dvy[i] * dt;
            dz[i] += dvz[i] * dt;
            tmp[0] = dx[i];
            tmp[1] = dy[i];
            tmp[2] = dz[i];
            if (p.mundo.empujarCerca(tmp, 0.8f)) {
                dx[i] = tmp[0];
                dy[i] = tmp[1];
                dz[i] = tmp[2];
            }
            float suelo = Terreno.altura(dx[i], dz[i]) + 0.8f;
            if (dy[i] < suelo) {
                dy[i] = suelo;
            }
            if (dy[i] > -2) {
                dy[i] = -2;
            }
        }

        // ---------------------------------------------------------- ironback crab
        for (int i = 0; i < NA; i++) {
            if (!aVivo[i]) {
                if ((bio == Terreno.MESETA || bio == Terreno.CANONES) && azar() < 0.01f
                        && puntoEn(px, pz, 35, 60, Terreno.MESETA, Terreno.CANONES, punto)) {
                    aVivo[i] = true;
                    aVida[i] = 3;
                    ax[i] = punto[0];
                    az[i] = punto[1];
                    ay[i] = Terreno.altura(ax[i], az[i]);
                    aRumbo[i] = azar() * 360;
                    aEstado[i] = MERODEAR;
                    Log.d("acorazado aparece en " + (int) ax[i] + "," + (int) az[i]);
                }
                continue;
            }
            float ox = px - ax[i], oz = pz - az[i];
            float dh = FMath.sqrt(ox * ox + oz * oz);
            if (dh > 110) {
                aVivo[i] = false;
                continue;
            }
            boolean aSuNivel = py < ay[i] + 4.5f;
            aT[i] -= dt;
            float vel = 1.0f;
            if (aEstado[i] == HUIR) {
                aRumbo[i] = FMath.atan2(-oz, -ox) / FMath.RAD;
                vel = 2.6f;
                if (aT[i] <= 0) {
                    aEstado[i] = MERODEAR;
                }
            } else if (aSuNivel && dh < 8) {
                aEstado[i] = CARGAR;
                aRumbo[i] = FMath.atan2(oz, ox) / FMath.RAD;
                vel = dh > 1.8f ? 2.2f : 0;
                if (dh < (p.enRemora ? 3.6f : 2.4f) && aT[i] <= 0) {
                    p.danar(12, "Un acorazado te golpeó con sus pinzas");
                    aT[i] = 1.6f;
                }
            } else {
                aEstado[i] = MERODEAR;
                aRumbo[i] += (azar() - 0.5f) * 40 * dt;
            }
            float nx = ax[i] + FMath.cos(aRumbo[i] * FMath.RAD) * vel * dt;
            float nz = az[i] + FMath.sin(aRumbo[i] * FMath.RAD) * vel * dt;
            int b = Terreno.bioma(nx, nz);
            if (b == Terreno.MESETA || b == Terreno.CANONES) {
                ax[i] = nx;
                az[i] = nz;
                // walk around rocks instead of through them
                tmp[0] = ax[i];
                tmp[1] = ay[i] + 0.3f;
                tmp[2] = az[i];
                p.mundo.empujarCerca(tmp, 0.9f);
                ax[i] = tmp[0];
                az[i] = tmp[2];
            } else {
                aRumbo[i] += 180;
            }
            ay[i] = Terreno.altura(ax[i], az[i]);
            aFase[i] += dt * vel * 4;
        }

        // ---------------------------------------------------------- ash colossus
        boolean abismo = bio == Terreno.ABISMO || bio == Terreno.VACIO;
        if (!cVivo) {
            if (abismo && azar() < 0.004f && puntoEn(px, pz, 140, 180, Terreno.ABISMO, Terreno.VACIO, punto)) {
                cVivo = true;
                cVida = 14;
                cx = punto[0];
                cz = punto[1];
                cy = Math.max(py - 10, Terreno.altura(cx, cz) + 20);
                cEstado = MERODEAR;
                cAviso = false;
                cRumbo = azar() * 360;
                Log.d("coloso de ceniza aparece en " + (int) cx + "," + (int) cy + "," + (int) cz);
            }
        } else {
            float ox = px - cx, oy = py - cy, oz = pz - cz;
            float d = FMath.sqrt(ox * ox + oy * oy + oz * oz);
            if (d > 260 || (!abismo && d > 180)) {
                cVivo = false;
                Log.d("coloso de ceniza se pierde en la oscuridad");
            } else {
                cT -= dt;
                float vel;
                float tx, ty, tz;
                if (cEstado == CARGAR) {
                    vel = 12;
                    tx = ox;
                    ty = oy;
                    tz = oz;
                    if (!bajoAgua || d > 110) {
                        cEstado = MERODEAR;
                    } else if (d < 9) {
                        p.danar(55, "El Coloso de Ceniza te embistió");
                        cEstado = RETIRARSE;
                        cT = 10;
                    }
                } else if (cEstado == RETIRARSE) {
                    vel = 7;
                    tx = -ox;
                    ty = 0;
                    tz = -oz;
                    if (cT <= 0) {
                        cEstado = MERODEAR;
                    }
                } else {
                    vel = 5;
                    cRumbo += 8 * dt;
                    tx = FMath.cos(cRumbo * FMath.RAD);
                    tz = FMath.sin(cRumbo * FMath.RAD);
                    ty = (py - cy) * 0.05f;
                    if (bajoAgua && d < 95 && !cAviso) {
                        cAviso = true;
                        Sonido.tocar(Sonido.RUGIDO);
                        p.avisar("Algo enorme se mueve en la oscuridad...");
                    }
                    if (bajoAgua && d < 65) {
                        cEstado = CARGAR;
                        Sonido.tocar(Sonido.RUGIDO);
                        Log.d("coloso de ceniza carga (" + (int) d + " m)");
                    }
                }
                float tl = FMath.sqrt(tx * tx + ty * ty + tz * tz);
                if (tl > 0.001f) {
                    float k = Math.min(1, dt * 0.8f);
                    cvx += (tx / tl * vel - cvx) * k;
                    cvy += (ty / tl * vel - cvy) * k;
                    cvz += (tz / tl * vel - cvz) * k;
                }
                cx += cvx * dt;
                cy += cvy * dt;
                cz += cvz * dt;
                float suelo = Terreno.altura(cx, cz) + 12;
                if (cy < suelo) {
                    cy = suelo;
                }
                if (cy > -15) {
                    cy = -15;
                }
            }
        }

        actualizarEscupidores(dt, p, px, py, pz, bajoAgua);
        actualizarGuardian(dt, p, px, py, pz, bio, bajoAgua);
        actualizarMedusa(dt, p, px, py, pz, bio);
        actualizarVelo(dt, px, py, pz, bio);

        // ---------------------------------------------------------- reef wanderer
        boolean suyo = bio == Terreno.BOSQUE || bio == Terreno.PRADERA || bio == Terreno.MESETA;
        if (!eVivo) {
            if (suyo && azar() < 0.003f && puntoEn(px, pz, 70, 110, bio, bio, punto)) {
                eVivo = true;
                ex = punto[0];
                ez = punto[1];
                ey = Terreno.altura(ex, ez) + 16;
                eRumbo = azar() * 360;
                eCoral = Math.max(0, 5 - eCoralCut);
                parVivo[0] = parVivo[1] = true;
                Log.d("errante de arrecife aparece en " + (int) ex + "," + (int) ey + "," + (int) ez);
            }
        } else {
            float ox = px - ex, oz = pz - ez;
            if (ox * ox + oz * oz > 260 * 260) {
                eVivo = false;
            } else {
                // a slow cruise in wide curves, turning back at the edge of its waters
                eFase += dt;
                eRumbo += FMath.sin(eFase * 0.07f) * 2.5f * dt;
                float r = eRumbo * FMath.RAD;
                float nx = ex + FMath.cos(r) * 0.7f * dt, nz = ez + FMath.sin(r) * 0.7f * dt;
                int b = Terreno.bioma(nx, nz);
                if (b != Terreno.BOSQUE && b != Terreno.PRADERA && b != Terreno.MESETA) {
                    eRumbo += 40 * dt;
                } else {
                    ex = nx;
                    ez = nz;
                }
                float meta = Math.min(-10, Terreno.altura(ex, ez) + 20);
                ey += (meta - ey) * Math.min(1, dt * 0.2f);
            }
        }
    }

    /** knife attack in front of the diver; true if something was hit */
    /**
     * Knife hit on a predator in front. Returns a message for the HUD, or null if there was
     * nothing to hit. The sawtooth flees when wounded and dies after 4 hits; the ironback
     * crab snaps back with its pincers when struck and dies after 3.
     */
    /** true while a harpoon bolt is the one hitting: no bite or pincer back at the diver */
    boolean aDistancia;

    String golpear(Partida p, float px, float py, float pz, float fx, float fy, float fz) {
        for (int i = 0; i < NS; i++) {
            if (sVivo[i] && delante(sx[i] - px, sy[i] - py, sz[i] - pz, fx, fy, fz, 2.4f)) {
                if (--sVida[i] <= 0) {
                    sVivo[i] = false;
                    return "Abatiste al escupidor de cueva";
                }
                sT[i] = 0.3f; // it answers at once
                return "Cuchillazo! El escupidor se revuelve";
            }
        }
        for (int i = 0; i < ND; i++) {
            if (dVivo[i] && delante(dx[i] - px, dy[i] - py, dz[i] - pz, fx, fy, fz, 2.8f)) {
                String nom = NOMBRE[dTipo[i]];
                if (--dVida[i] <= 0) {
                    dVivo[i] = false;
                    dParche[i] = -1;
                    Log.d(nom + " abatido");
                    return "Abatiste: " + nom;
                }
                if (dTipo[i] == 1 && !aDistancia) {
                    // the eel bites back before it slips away
                    p.danar(6, "La anguila de brasa se revuelve y te muerde");
                }
                dEstado[i] = HUIR;
                dT[i] = 8;
                Log.d("cuchillazo a " + nom + ": huye, vida " + dVida[i]);
                return "Cuchillazo! Huye herido: " + nom;
            }
        }
        for (int i = 0; i < NA; i++) {
            if (aVivo[i] && delante(ax[i] - px, ay[i] + 0.5f - py, az[i] - pz, fx, fy, fz, 2.6f)) {
                if (--aVida[i] <= 0) {
                    aVivo[i] = false;
                    Log.d("acorazado abatido");
                    return "Abatiste al acorazado";
                }
                // armoured: it answers the blow with its pincers before backing off
                if (azar() < 0.6f && !aDistancia) {
                    p.danar(8, "Un acorazado te golpeó con sus pinzas");
                }
                aEstado[i] = HUIR;
                aT[i] = 4;
                Log.d("cuchillazo a un acorazado: contraataca, vida " + aVida[i]);
                return "Cuchillazo! El acorazado se defiende";
            }
        }
        if (gVivo && delante(gx - px, gy - py, gz - pz, fx, fy, fz, 6)) {
            if (--gVida <= 0) {
                gVivo = false;
                Log.d("guardian del vacio abatido");
                return "Abatiste al Guardián del Vacío!";
            }
            gEstado = RETIRARSE;
            gT = 6;
            return "Cuchillazo al Guardián (" + gVida + ")";
        }
        if (cVivo && delante(cx - px, cy - py, cz - pz, fx, fy, fz, 10)) {
            if (--cVida <= 0) {
                cVivo = false;
                Log.d("coloso de ceniza abatido");
                return "Abatiste al Coloso de Ceniza!";
            }
            cEstado = RETIRARSE;
            cT = 6;
            return "Cuchillazo al Coloso (" + cVida + ")";
        }
        int q = parasitoDelante(px, py, pz, fx, fy, fz);
        if (q >= 0) {
            parVivo[q] = false;
            return "Aplastaste un grupo de parasitos";
        }
        return null;
    }

    /** the wanderer's parasite groups: alive or squashed (they come back with a new wanderer) */
    final boolean[] parVivo = { true, true };

    private final float[] parPos = new float[3];

    /** world position of parasite group q into parPos */
    private void posParasito(int q) {
        float th = (90 - eRumbo) * FMath.RAD;
        float co = FMath.cos(th), si = FMath.sin(th);
        float a = eFase * (0.3f + q * 0.1f) + q * 2.1f;
        float lx = FMath.sin(a) * 1.6f, lz = -2 + q * 4f + FMath.cos(a * 0.7f) * 1.5f;
        parPos[0] = ex + (lx * co + lz * si) * ESC_ERRANTE;
        parPos[1] = ey + 3.35f * ESC_ERRANTE;
        parPos[2] = ez + (-lx * si + lz * co) * ESC_ERRANTE;
    }

    private int parasitoDelante(float px, float py, float pz, float fx, float fy, float fz) {
        if (!eVivo) {
            return -1;
        }
        for (int q = 0; q < NPARASITOS; q++) {
            if (!parVivo[q]) {
                continue;
            }
            posParasito(q);
            if (delante(parPos[0] - px, parPos[1] - py, parPos[2] - pz, fx, fy, fz, 3)) {
                return q;
            }
        }
        return -1;
    }

    /**
     * The Remora's defence pulse: an electric discharge that wounds and scares everything
     * hostile within r metres. Returns how many creatures it reached.
     */
    int pulso(float px, float py, float pz, float r) {
        int n = 0;
        float r2 = r * r;
        for (int i = 0; i < ND; i++) {
            float ox = dx[i] - px, oy = dy[i] - py, oz = dz[i] - pz;
            if (dVivo[i] && ox * ox + oy * oy + oz * oz < r2) {
                n++;
                dVida[i] -= 2;
                if (dVida[i] <= 0) {
                    dVivo[i] = false;
                } else {
                    dEstado[i] = HUIR;
                    dT[i] = 10;
                }
            }
        }
        for (int i = 0; i < NA; i++) {
            float ox = ax[i] - px, oy = ay[i] - py, oz = az[i] - pz;
            if (aVivo[i] && ox * ox + oy * oy + oz * oz < r2) {
                n++;
                aVida[i] -= 2;
                if (aVida[i] <= 0) {
                    aVivo[i] = false;
                } else {
                    aEstado[i] = HUIR;
                    aT[i] = 6;
                }
            }
        }
        if (gVivo) {
            float gox = gx - px, goy = gy - py, goz = gz - pz;
            if (gox * gox + goy * goy + goz * goz < (r + 20) * (r + 20)) {
                n++;
                gEstado = RETIRARSE;
                gT = 10;
                gVida -= 2;
                if (gVida <= 0) {
                    gVivo = false;
                    Log.d("guardian del vacio abatido");
                }
            }
        }
        float ox = cx - px, oy = cy - py, oz = cz - pz;
        if (cVivo && ox * ox + oy * oy + oz * oz < (r + 12) * (r + 12)) {
            // the shock wounds it and makes it break off its charge
            n++;
            cEstado = RETIRARSE;
            cT = 8;
            cVida -= 2;
            if (cVida <= 0) {
                cVivo = false;
                Log.d("coloso de ceniza abatido");
            }
        }
        Log.d("pulso de la remora: " + n + " criaturas alcanzadas");
        return n;
    }

    /** true if a creature is close enough to be hit with the knife */
    boolean alAlcance(float px, float py, float pz, float fx, float fy, float fz) {
        for (int i = 0; i < NS; i++) {
            if (sVivo[i] && delante(sx[i] - px, sy[i] - py, sz[i] - pz, fx, fy, fz, 2.4f)) {
                return true;
            }
        }
        for (int i = 0; i < ND; i++) {
            if (dVivo[i] && delante(dx[i] - px, dy[i] - py, dz[i] - pz, fx, fy, fz, 2.8f)) {
                return true;
            }
        }
        for (int i = 0; i < NA; i++) {
            // the big crabs are wide and sit on the floor: a reach sized for a fish made them
            // impossible to hit when you stab down from above
            if (aVivo[i] && delante(ax[i] - px, ay[i] + 0.7f - py, az[i] - pz, fx, fy, fz, 3.8f)) {
                return true;
            }
        }
        return (gVivo && delante(gx - px, gy - py, gz - pz, fx, fy, fz, 6))
                || (cVivo && delante(cx - px, cy - py, cz - pz, fx, fy, fz, 10))
                || parasitoDelante(px, py, pz, fx, fy, fz) >= 0;
    }

    private static boolean delante(float ox, float oy, float oz, float fx, float fy, float fz, float r) {
        float d2 = ox * ox + oy * oy + oz * oz;
        if (d2 > r * r) {
            return false;
        }
        float d = FMath.sqrt(d2);
        // a wide cone: you should not have to aim perfectly at a big animal
        return d < 1.4f || (ox * fx + oy * fy + oz * fz) > d * 0.25f;
    }

    void dibujar(Graphics3D g3d, float px, float pz, float fx, float fz, float lejos) {
        dibujados = 0;
        for (int i = 0; i < ND; i++) {
            if (!dVivo[i] || !visible(dx[i] - px, dz[i] - pz, fx, fz, lejos, 3)) {
                continue;
            }
            if (dTipo[i] == GUSANO && dOculto[i]) {
                // buried: a mound of sand tells you something is down there
                t.setIdentity();
                t.postTranslate(dx[i], dy[i] + 0.15f, dz[i]);
                g3d.render(mGusanoMound, t);
                dibujados++;
                continue;
            }
            orientar(dx[i], dy[i], dz[i], dvx[i], dvy[i], dvz[i]);
            dibujarSerpiente(g3d, dTipo[i], dEstado[i] == CARGAR, i);
            dibujados++;
        }
        for (int i = 0; i < NA; i++) {
            if (!aVivo[i] || !visible(ax[i] - px, az[i] - pz, fx, fz, lejos, 2)) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(ax[i], ay[i] + FMath.sin(aFase[i]) * 0.05f, az[i]);
            t.postRotate(90 - aRumbo[i], 0, 1, 0);
            t.postRotate(FMath.sin(aFase[i]) * 4, 0, 0, 1); // waddle
            if (aEstado[i] == CARGAR && aT[i] > 1.2f) {
                // just pinched: lunge forward and dip
                float k = (aT[i] - 1.2f) / 0.4f;
                t.postTranslate(0, 0, 0.35f * k);
                t.postRotate(-14 * k, 1, 0, 0);
            }
            dibujarCangrejo(g3d, aFase[i], aEstado[i] == CARGAR && aT[i] > 1.2f ? (aT[i] - 1.2f) / 0.4f : -1);
            dibujados++;
        }
        dibujarEscupidores(g3d, px, pz, fx, fz, lejos);
        if (gVivo && visible(gx - px, gz - pz, fx, fz, lejos + 50, 50)) {
            orientar(gx, gy, gz, gvx, gvy, gvz);
            t.postScale(ESC_GUARDIAN, ESC_GUARDIAN, ESC_GUARDIAN);
            dibujarSerpiente(g3d, 4, gEstado == CARGAR, 0);
            dibujados++;
        }
        if (mlVivo && visible(mlx - px, mlz - pz, fx, fz, lejos + 30, 30)) {
            t.setIdentity();
            t.postTranslate(mlx, mly, mlz);
            t.postRotate(mlRumbo, 0, 1, 0);
            t.postScale(ESC_MEDUSA, ESC_MEDUSA, ESC_MEDUSA);
            dibujarMedusa(g3d, mlFase);
            dibujados++;
        }
        if (vVivo && visible(vsx[NV / 2] - px, vsz[NV / 2] - pz, fx, fz, lejos + 25, 25)) {
            dibujarVelo(g3d);
            dibujados++;
        }
        if (eVivo && visible(ex - px, ez - pz, fx, fz, lejos + 35, 35)) {
            t.setIdentity();
            t.postTranslate(ex, ey, ez);
            t.postRotate(90 - eRumbo, 0, 1, 0);
            t.postScale(ESC_ERRANTE, ESC_ERRANTE, ESC_ERRANTE); // ~30 m: room for its garden
            float brazada = FMath.sin(eFase * 0.9f);
            t.postRotate(brazada * 2, 1, 0, 0);
            g3d.render(mErrante[5], t);
            // flippers: slow strokes; fluke: up and down
            for (int k = 0; k < 4; k++) {
                float lado = (k & 1) == 0 ? 1 : -1;
                float zc = k < 2 ? 4 : -4;
                tp.set(t);
                tp.postTranslate(3.2f * lado, -0.5f, zc);
                tp.postRotate(brazada * (k < 2 ? 18 : 10) * lado, 0, 0, 1);
                tp.postTranslate(-3.2f * lado, 0.5f, -zc);
                g3d.render(mErrante[k], tp);
            }
            tp.set(t);
            tp.postTranslate(0, 0, -10);
            tp.postRotate(FMath.sin(eFase * 0.9f - 1) * 14, 1, 0, 0);
            tp.postTranslate(0, 0, 10);
            g3d.render(mErrante[4], tp);
            // its parasites: small red crabs scuttling over the reef garden
            for (int q = 0; q < NPARASITOS; q++) {
                if (!parVivo[q]) {
                    continue;
                }
                float a = eFase * (0.3f + q * 0.1f) + q * 2.1f;
                tp.set(t);
                tp.postTranslate(FMath.sin(a) * 1.6f, 3.35f, -2 + q * 4f + FMath.cos(a * 0.7f) * 1.5f);
                tp.postRotate(a * 57.29578f, 0, 1, 0);
                g3d.render(mParasitos, tp);
            }
            // a school of silverlings circles over its reef
            for (int q = 0; q < 2; q++) {
                float a = eFase * 0.5f + q * FMath.PI;
                tp.set(t);
                tp.postTranslate(FMath.cos(a) * 6, 5 + q, FMath.sin(a) * 7);
                tp.postRotate(-a * 57.29578f, 0, 1, 0);
                g3d.render(mCardumen, tp);
            }
            dibujados++;
        }
        if (cVivo && visible(cx - px, cz - pz, fx, fz, lejos + 40, 40)) {
            orientar(cx, cy, cz, cvx, cvy, cvz);
            dibujarColoso(g3d);
            dibujados++;
        }
    }

    private static boolean visible(float ox, float oz, float fx, float fz, float lejos, float radio) {
        return ox * ox + oz * oz < (lejos + radio) * (lejos + radio) && (ox * fx + oz * fz) > -radio - 2;
    }

    /** place the model (facing +Z) at a point, looking along its velocity */
    private void orientar(float x, float y, float z, float vx, float vy, float vz) {
        t.setIdentity();
        t.postTranslate(x, y, z);
        float h = FMath.sqrt(vx * vx + vz * vz);
        if (h > 0.01f) {
            t.postRotate(FMath.atan2(vx, vz) / FMath.RAD, 0, 1, 0);
            t.postRotate(-FMath.atan2(vy, h) / FMath.RAD, 1, 0, 0);
        }
    }

    /**
     * A serpent from its chain of segments, starting from the transform t (the creature's
     * centre, facing +Z): a wave travels from the head to the tail.
     */
    /** the Void Guardian: head, six segments and tail in a chain; slow, wide undulation */
    private void dibujarGuardian(Graphics3D g3d, boolean carga) {
        float frec = carga ? 3.2f : 1.6f, amp = carga ? 14 : 9;
        t.postTranslate(0, 0, 2.6f);
        t.postRotate(FMath.sin(tiempo * frec) * amp * 0.3f, 0, 1, 0);
        g3d.render(segGuardian[0], t);
        for (int k = 0; k < 6; k++) {
            g3d.render(segGuardian[1 + k], t);
            t.postTranslate(0, 0, -0.8f);
            t.postRotate(FMath.sin(tiempo * frec - (k + 1) * 0.8f) * amp * (0.5f + k * 0.12f), 0, 1, 0);
            t.postRotate(FMath.sin(tiempo * frec * 0.5f - k) * 2, 1, 0, 0);
        }
        g3d.render(segGuardian[7], t);
    }

    private void dibujarSerpiente(Graphics3D g3d, int tipo, boolean carga, int i) {
        if (tipo == 4) {
            dibujarGuardian(g3d, carga);
            return;
        }
        if (tipo == 2) {
            dibujarCalamar(g3d, carga, i);
            return;
        }
        if (tipo == 3) {
            dibujarSapo(g3d, carga, i);
            return;
        }
        if (tipo == GUSANO) {
            dibujarGusano(g3d, carga, i);
            return;
        }
        float frec = carga ? 9 : 5, amp = carga ? 22 : 14;
        Mesh[] seg = tipo == 4 ? segGuardian
                : (tipo == 1 ? segAnguila : (tipo == 2 ? segAcechador : (tipo == 3 ? segMordedor : segDiente)));
        float largo = tipo == 1 ? 4.2f : (tipo >= 2 ? 3.6f : 4.5f);

        t.postRotate(FMath.sin(tiempo * 2 + i) * 3, 0, 0, 1);
        t.postTranslate(0, 0, largo / 2);
        // the head swings a little against the body
        t.postRotate(FMath.sin(tiempo * frec + i * 2) * amp * 0.35f, 0, 1, 0);
        for (int k = 0; k < 4; k++) {
            g3d.render(seg[k], t);
            t.postTranslate(0, 0, -largo / 4);
            t.postRotate(FMath.sin(tiempo * frec + i * 2 - (k + 1) * 1.1f) * amp * (0.5f + k * 0.25f), 0, 1, 0);
        }
    }

    /**
     * The crab from its parts at transform t. Tripod gait: legs 0, 2, 4 swing together and
     * 1, 3, 5 half a cycle later; a leg lifts while it swings forward. ataque in 0..1 raises
     * and snaps the claws (-1 = not attacking).
     */
    private void dibujarCangrejo(Graphics3D g3d, float f, float ataque) {
        g3d.render(mCangrejo, t);
        for (int k = 0; k < 6; k++) {
            float lado = k < 3 ? 1 : -1;
            float a = (k % 3) * 32 - 32;
            float fase = f * 1.6f + (k & 1) * FMath.PI + (k < 3 ? 0 : FMath.PI);
            float paso = FMath.sin(fase) * 16;
            float alza = Math.max(0, FMath.cos(fase)) * 18;
            tp.set(t);
            tp.postTranslate(0, 0.45f, 0);
            tp.postRotate((lado > 0 ? 90 + a : -90 - a) + paso * lado, 0, 1, 0);
            tp.postTranslate(0, 0, 0.6f);
            tp.postRotate(-alza, 1, 0, 0);
            g3d.render(mPata, tp);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            float alzar = ataque >= 0 ? 35 * ataque : 6 + FMath.sin(f * 0.8f + sd) * 6;
            tp.set(t);
            tp.postTranslate(sd * 0.45f, 0.5f, 0.5f);
            tp.postRotate(sd * -20 + FMath.sin(f * 0.8f + sd * 1.3f) * 8, 0, 1, 0);
            tp.postRotate(-alzar, 1, 0, 0);
            g3d.render(mPinza, tp);
        }
    }

    /**
     * true if (x, y, z) is standing or swimming low over the wanderer's back (its reef
     * garden), in its local frame.
     */
    static final int NPARASITOS = 2;

    /** true if one of the wanderer's two parasite groups is right next to (x, y, z) */
    boolean parasitoCerca(float x, float y, float z) {
        if (!eVivo) {
            return false;
        }
        for (int q = 0; q < NPARASITOS; q++) {
            if (!parVivo[q]) {
                continue;
            }
            posParasito(q);
            float dx = x - parPos[0], dy = y - parPos[1], dz = z - parPos[2];
            if (dx * dx + dy * dy + dz * dz < 2.2f * 2.2f) {
                return true;
            }
        }
        return false;
    }

    boolean sobreErrante(float x, float y, float z) {
        if (!eVivo) {
            return false;
        }
        float r = eRumbo * FMath.RAD;
        float ox = x - ex, oz = z - ez;
        float largo = ox * FMath.cos(r) + oz * FMath.sin(r); // along the body
        float ancho = -ox * FMath.sin(r) + oz * FMath.cos(r);
        float e = ESC_ERRANTE;
        float alto = y - ey;
        return largo > -6 * e && largo < 8 * e && ancho > -2.6f * e && ancho < 2.6f * e && alto > 2.6f * e
                && alto < 3.5f * e + 3;
    }

    /** the reef wanderer is solid: push (q) out of its body (two spheres along it) */
    void empujarErrante(float[] q, float rj) {
        if (!eVivo) {
            return;
        }
        float dx0 = q[0] - ex, dz0 = q[2] - ez;
        float lejos = 16 * ESC_ERRANTE + rj;
        if (dx0 * dx0 + dz0 * dz0 > lejos * lejos) {
            return;
        }
        // the body: a chain of spheres following the hull's profile from tail to nose
        // (close together, a little larger than the hull, so the camera never gets in)
        float r = eRumbo * FMath.RAD;
        float ax = FMath.cos(r), az = FMath.sin(r);
        for (int k = 0; k < 22; k++) {
            float lz = -10f + k * 0.95f; // local units, tail (-) to nose (+)
            float rad = Modelos.radioErrante(lz) * ESC_ERRANTE;
            if (rad < 0.3f) {
                continue;
            }
            empujarEsfera(q, ex + ax * lz * ESC_ERRANTE, ey, ez + az * lz * ESC_ERRANTE, rad + 0.4f + rj);
        }
        // the skull: fat overlapping spheres so the diver cannot slip into the head from any
        // side (the nose profile is nearly zero, so the body chain alone left a hole)
        empujarEsfera(q, ex + ax * 7.5f * ESC_ERRANTE, ey, ez + az * 7.5f * ESC_ERRANTE, 2.7f * ESC_ERRANTE + rj);
        empujarEsfera(q, ex + ax * 9.0f * ESC_ERRANTE, ey, ez + az * 9.0f * ESC_ERRANTE, 2.5f * ESC_ERRANTE + rj);
        empujarEsfera(q, ex + ax * 10.4f * ESC_ERRANTE, ey, ez + az * 10.4f * ESC_ERRANTE, 1.9f * ESC_ERRANTE + rj);
        // the four flippers: three spheres along each, out to the tip, and the fluke
        float lxv = -az, lzv = ax; // its left side
        for (int k = 0; k < 4; k++) {
            float lado = (k & 1) == 0 ? 1 : -1;
            for (int j = 0; j < 3; j++) {
                float lz = k < 2 ? 3.6f - j * 0.4f : -4.2f - j * 0.4f;
                float lx = (k < 2 ? 4.6f + j * 1.7f : 4.0f + j * 0.9f) * lado;
                float wx = ex + (az * 0 + ax * lz + lxv * lx) * ESC_ERRANTE;
                float wz = ez + (az * lz + lzv * lx) * ESC_ERRANTE;
                empujarEsfera(q, wx, ey - (0.7f + j * 0.2f) * ESC_ERRANTE, wz, 0.9f * ESC_ERRANTE + rj);
            }
        }
        for (int j = 0; j < 3; j++) {
            float lz = -10.8f - j * 0.7f, lx = (j - 1) * 2.2f;
            empujarEsfera(q, ex + (ax * lz + lxv * lx) * ESC_ERRANTE, ey, ez + (az * lz + lzv * lx) * ESC_ERRANTE,
                    0.9f * ESC_ERRANTE + rj);
        }
    }

    /** gallery: one animated creature at the origin (0-2 serpents, 3 reef wanderer, 4 crab) */
    void dibujarMuestra(Graphics3D g3d, Transform base, int cual, float tiempo) {
        this.tiempo = tiempo;
        t.set(base);
        if (cual == 3) {
            eFase = tiempo;
            t.postScale(ESC_ERRANTE, ESC_ERRANTE, ESC_ERRANTE);
            g3d.render(mErrante[5], t);
            float brazada = FMath.sin(eFase * 0.9f);
            for (int k = 0; k < 4; k++) {
                float lado = (k & 1) == 0 ? 1 : -1;
                float zc = k < 2 ? 4 : -4;
                tp.set(t);
                tp.postTranslate(3.2f * lado, -0.5f, zc);
                tp.postRotate(brazada * (k < 2 ? 18 : 10) * lado, 0, 0, 1);
                tp.postTranslate(-3.2f * lado, 0.5f, -zc);
                g3d.render(mErrante[k], tp);
            }
            g3d.render(mErrante[4], t);
        } else if (cual == 4) {
            dibujarCangrejo(g3d, tiempo * 3, -1);
        } else if (cual == 7) {
            dibujarColoso(g3d);
        } else if (cual == 8) {
            t.postScale(ESC_GUARDIAN, ESC_GUARDIAN, ESC_GUARDIAN);
            dibujarSerpiente(g3d, 4, false, 0);
        } else if (cual == 9) {
            t.postScale(ESC_MEDUSA, ESC_MEDUSA, ESC_MEDUSA);
            dibujarMedusa(g3d, tiempo);
        } else if (cual == 6) {
            g3d.render(mEscupidor[2], t);
            float hincha = 1 + Math.max(0, FMath.sin(tiempo * 2)) * 0.4f;
            tp.set(t);
            tp.postTranslate(0, 0, 0.38f);
            tp.postScale(hincha, hincha, 1);
            tp.postTranslate(0, 0, -0.38f);
            g3d.render(mEscupidor[0], tp);
            tp.set(t);
            tp.postTranslate(0, 0, -0.34f);
            tp.postRotate(FMath.sin(tiempo * 5) * 22, 0, 1, 0);
            tp.postTranslate(0, 0, 0.34f);
            g3d.render(mEscupidor[1], tp);
        } else {
            dibujarSerpiente(g3d, cual == 5 ? 3 : cual, false, 0);
        }
    }

    /** kelp lurker: the mantle leads, six arms trail and wave (they whip when it charges) */
    private void dibujarCalamar(Graphics3D g3d, boolean carga, int i) {
        float frec = carga ? 7 : 2.5f;
        t.postRotate(FMath.sin(tiempo * 1.3f + i) * 4, 0, 0, 1);
        g3d.render(mCalamar[6], t);
        for (int k = 0; k < 6; k++) {
            float a = k * 1.047f;
            tp.set(t);
            tp.postTranslate(0, 0, -0.55f);
            float ola = FMath.sin(tiempo * frec - k * 0.9f + i) * (carga ? 25 : 14);
            tp.postRotate(ola * FMath.cos(a), 0, 1, 0);
            tp.postRotate(ola * FMath.sin(a), 1, 0, 0);
            tp.postTranslate(0, 0, 0.55f);
            g3d.render(mCalamar[k], tp);
        }
    }

    /** grass biter: it hops, paddles with its leg fins and snaps its big jaw */
    /** the sand worm: four jaws that open wide when it strikes, a ringed body that lashes */
    private void dibujarGusano(Graphics3D g3d, boolean carga, int i) {
        float frec = carga ? 8 : 4, amp = carga ? 20 : 10;
        tp.set(t);
        g3d.render(mGusano[0], t); // head
        // the jaws: four petals opening around the mouth
        float abre = carga ? 25 + FMath.sin(tiempo * 12) * 15 : 6;
        for (int k = 0; k < 4; k++) {
            tp.set(t);
            tp.postRotate(k * 90 + 45, 0, 0, 1);
            tp.postTranslate(0, 0.18f, 0.35f);
            tp.postRotate(-abre, 1, 0, 0);
            g3d.render(mGusano[1], tp);
        }
        for (int k = 0; k < 5; k++) {
            t.postTranslate(0, 0, -0.55f);
            t.postRotate(FMath.sin(tiempo * frec + i - k * 0.9f) * amp * (0.4f + k * 0.15f), 0, 1, 0);
            t.postRotate(FMath.sin(tiempo * frec * 0.7f - k) * 4, 1, 0, 0);
            g3d.render(mGusano[k < 4 ? 2 : 3], t);
        }
    }

    private void dibujarSapo(Graphics3D g3d, boolean carga, int i) {
        float salto = carga ? Math.abs(FMath.sin(tiempo * 6 + i)) * 0.4f : 0;
        t.postTranslate(0, salto, 0);
        t.postRotate(carga ? -salto * 30 : FMath.sin(tiempo * 1.5f + i) * 3, 1, 0, 0);
        g3d.render(mSapo[3], t);
        float boca = carga ? 25 + FMath.sin(tiempo * 14) * 20 : Math.max(0, FMath.sin(tiempo * 0.8f + i)) * 18;
        tp.set(t);
        tp.postTranslate(0, -0.1f, 0.2f);
        tp.postRotate(boca, 1, 0, 0);
        tp.postTranslate(0, 0.1f, -0.2f);
        g3d.render(mSapo[0], tp);
        float aleta = FMath.sin(tiempo * (carga ? 12 : 3) + i) * 25;
        for (int k = 1; k <= 2; k++) {
            float lado = k == 1 ? 1 : -1;
            tp.set(t);
            tp.postTranslate(0.25f * lado, -0.1f, 0);
            tp.postRotate(aleta * lado, 0, 0, 1);
            tp.postTranslate(-0.25f * lado, 0.1f, 0);
            g3d.render(mSapo[k], tp);
        }
    }

    /**
     * Cave spitters and their acid. A spitter guards one cave or tunnel (only some have
     * one): it hovers by its home, faces an intruder within 14 m and spits a blob of
     * acid every 2.5 s; up close it backs off. Acid hurts on contact.
     */
    private void actualizarEscupidores(float dt, Partida p, float px, float py, float pz, boolean bajoAgua) {
        for (int i = 0; i < NS; i++) {
            if (!sVivo[i]) {
                if (azar() < 0.02f && p.mundo.refugioCercano(px, pz, 45, refugio)) {
                    // only some caves are guarded, always the same ones
                    if (Terreno.azar((int) refugio[0], (int) refugio[2], 909) > 0.75f) {
                        continue;
                    }
                    boolean ocupado = false;
                    for (int q = 0; q < NS; q++) {
                        ocupado |= sVivo[q] && Math.abs(shx[q] - refugio[0]) + Math.abs(shz[q] - refugio[2]) < 3;
                    }
                    if (ocupado || (refugio[0] - px) * (refugio[0] - px) + (refugio[2] - pz) * (refugio[2] - pz) < 15 * 15) {
                        continue;
                    }
                    sVivo[i] = true;
                    shx[i] = sx[i] = refugio[0];
                    shz[i] = sz[i] = refugio[2];
                    shy[i] = sy[i] = refugio[1] + 1.4f;
                    sVida[i] = 2;
                    sT[i] = 1;
                    Log.d("escupidor de cueva en " + (int) sx[i] + "," + (int) sy[i] + "," + (int) sz[i]);
                }
                continue;
            }
            float ox = px - sx[i], oy = py - sy[i], oz = pz - sz[i];
            float d2 = ox * ox + oy * oy + oz * oz;
            float hx = px - shx[i], hz = pz - shz[i];
            if (hx * hx + hz * hz > 70 * 70) {
                sVivo[i] = false;
                continue;
            }
            sT[i] = Math.max(0, sT[i] - dt); // never negative: a far spitter's mouth would scale huge
            float objetivo = FMath.atan2(ox, oz) * 57.29578f;
            if (d2 < 14 * 14 && bajoAgua) {
                // face the intruder; back off when too close
                sRumbo[i] += Peces.angulo(objetivo - sRumbo[i]) * Math.min(1, dt * 4);
                if (d2 < 3 * 3) {
                    float d = FMath.sqrt(d2);
                    sx[i] -= ox / d * 2.5f * dt;
                    sz[i] -= oz / d * 2.5f * dt;
                }
                if (sT[i] <= 0) {
                    sT[i] = 2.5f;
                    // only when it can see you: not through the cave walls
                    if (p.mundo.lineaLibre(sx[i], sy[i], sz[i], px, py, pz)) {
                        escupir(i, px, py, pz);
                    } else {
                        sT[i] = 0.8f;
                    }
                }
            } else {
                // idle: drift around its home
                sRumbo[i] += 25 * dt;
                float r = sRumbo[i] * FMath.RAD;
                sx[i] += (shx[i] + FMath.sin(r) * 2.5f - sx[i]) * Math.min(1, dt * 0.8f);
                sz[i] += (shz[i] + FMath.cos(r) * 2.5f - sz[i]) * Math.min(1, dt * 0.8f);
                sy[i] += (shy[i] + FMath.sin(tiempo + i) * 0.4f - sy[i]) * Math.min(1, dt);
            }
            tmp[0] = sx[i];
            tmp[1] = sy[i];
            tmp[2] = sz[i];
            if (p.mundo.empujarCerca(tmp, 0.4f)) {
                sx[i] = tmp[0];
                sy[i] = tmp[1];
                sz[i] = tmp[2];
            }
            // PARCHE: no dejar que el escupidor baje del suelo
            float suelo = Terreno.altura(sx[i], sz[i]) + 0.5f;
            if (sy[i] < suelo) {
                sy[i] = suelo;
            }
            // PARCHE: no alejarse demasiado de su cueva
            float dxh = sx[i] - shx[i], dzh = sz[i] - shz[i];
            if (dxh * dxh + dzh * dzh > 30 * 30) {
                sx[i] += (shx[i] - sx[i]) * Math.min(1, dt * 2);
                sz[i] += (shz[i] - sz[i]) * Math.min(1, dt * 2);
            }
        }
        for (int b = 0; b < NB; b++) {
            if (!bVivo[b]) {
                continue;
            }
            bt[b] -= dt;
            bx[b] += bvx[b] * dt;
            by[b] += bvy[b] * dt;
            bz[b] += bvz[b] * dt;
            float ox = px - bx[b], oy = py - by[b], oz = pz - bz[b];
            if (ox * ox + oy * oy + oz * oz < 0.9f * 0.9f) {
                bVivo[b] = false;
                p.danar(7, "Te alcanzó el ácido de un escupidor");
            } else if (bt[b] <= 0 || by[b] < Terreno.altura(bx[b], bz[b]) || p.mundo.enRoca(bx[b], by[b], bz[b])) {
                bVivo[b] = false; // splashes on the floor or a wall
            }
        }
    }

    private void escupir(int i, float px, float py, float pz) {
        for (int b = 0; b < NB; b++) {
            if (!bVivo[b]) {
                float ox = px - sx[i], oy = py - sy[i], oz = pz - sz[i];
                float d = Math.max(0.1f, FMath.sqrt(ox * ox + oy * oy + oz * oz));
                bVivo[b] = true;
                bx[b] = sx[i] + ox / d * 0.6f;
                by[b] = sy[i] + oy / d * 0.6f;
                bz[b] = sz[i] + oz / d * 0.6f;
                bvx[b] = ox / d * 8;
                bvy[b] = oy / d * 8;
                bvz[b] = oz / d * 8;
                bt[b] = 2.5f;
                Sonido.tocar(Sonido.ESCUPIR);
                return;
            }
        }
    }

    private void dibujarEscupidores(Graphics3D g3d, float px, float pz, float fx, float fz, float lejos) {
        for (int i = 0; i < NS; i++) {
            if (!sVivo[i] || !visible(sx[i] - px, sz[i] - pz, fx, fz, lejos, 2)) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(sx[i], sy[i], sz[i]);
            t.postRotate(sRumbo[i], 0, 1, 0);
            g3d.render(mEscupidor[2], t);
            // the mouth swells just before it spits
            float hincha = sT[i] < 0.5f ? 1 + Math.max(0, 0.5f - sT[i]) * 0.8f : 1;
            tp.set(t);
            tp.postTranslate(0, 0, 0.38f);
            tp.postScale(hincha, hincha, 1);
            tp.postTranslate(0, 0, -0.38f);
            g3d.render(mEscupidor[0], tp);
            tp.set(t);
            tp.postTranslate(0, 0, -0.34f);
            tp.postRotate(FMath.sin(tiempo * 5 + i) * 22, 0, 1, 0);
            tp.postTranslate(0, 0, 0.34f);
            g3d.render(mEscupidor[1], tp);
            dibujados++;
        }
        for (int b = 0; b < NB; b++) {
            if (bVivo[b]) {
                t.setIdentity();
                t.postTranslate(bx[b], by[b], bz[b]);
                t.postRotate(tiempo * 300, 0, 1, 0);
                g3d.render(mAcido, t);
            }
        }
    }

    /** world position of grass patch dParche[i] on the wanderer's back */
    private void posParche(int i) {
        int k = dParche[i];
        float th = (90 - eRumbo) * FMath.RAD;
        float co = FMath.cos(th), si = FMath.sin(th);
        float lx = Modelos.PARCHE_X[k], lz = Modelos.PARCHE_Z[k];
        dx[i] = ex + (lx * co + lz * si) * ESC_ERRANTE;
        dz[i] = ez + (-lx * si + lz * co) * ESC_ERRANTE;
        dy[i] = ey + (Modelos.PARCHE_Y[k] + 0.2f) * ESC_ERRANTE;
        dRumbo[i] = eRumbo;
    }

    /**
     * The diver cannot swim through creatures: push (q) out of their bodies. The big ones also
     * record how far they shoved you in `emp`, so the caller can turn a wall into a real push.
     */
    void empujarCriaturas(float[] q, float rj, float[] emp, boolean nave) {
        // the small predators never shove the Remora around (it wobbled when one charged):
        // aboard, the hull pushes THEM aside and they bounce off; on foot they block the diver
        for (int i = 0; i < ND; i++) {
            if (dVivo[i]) {
                float r = (dTipo[i] == 3 ? 0.35f : 0.6f) + rj;
                if (nave) {
                    apartarDe(q, i, r);
                } else {
                    empujarEsfera(q, dx[i], dy[i], dz[i], r);
                }
            }
        }
        for (int i = 0; i < NA; i++) {
            if (aVivo[i]) {
                if (nave) {
                    // the crab is shoved along the floor, the hull keeps going
                    tmp[0] = ax[i];
                    tmp[1] = ay[i] + 0.5f;
                    tmp[2] = az[i];
                    empujarEsfera(tmp, q[0], q[1], q[2], 0.9f + rj);
                    ax[i] = tmp[0];
                    az[i] = tmp[2];
                } else {
                    empujarEsfera(q, ax[i], ay[i] + 0.5f, az[i], 0.9f + rj);
                }
            }
        }
        for (int i = 0; i < NS; i++) {
            if (sVivo[i]) {
                if (nave) {
                    tmp[0] = sx[i];
                    tmp[1] = sy[i];
                    tmp[2] = sz[i];
                    empujarEsfera(tmp, q[0], q[1], q[2], 0.45f + rj);
                    sx[i] = tmp[0];
                    sy[i] = tmp[1];
                    sz[i] = tmp[2];
                } else {
                    empujarEsfera(q, sx[i], sy[i], sz[i], 0.45f + rj);
                }
            }
        }
        if (cVivo) {
            empujarConImpulso(q, cx, cy, cz, 8.5f + rj, emp);
        }
        if (gVivo) {
            // the guardian is a 45 m chain, not a ball: several spheres along its heading, or
            // you swim straight through most of its body
            float hr = gRumbo * FMath.RAD;
            float hx = FMath.sin(hr), hz = FMath.cos(hr);
            for (int k = -3; k <= 4; k++) {
                empujarConImpulso(q, gx + hx * k * 6f, gy, gz + hz * k * 6f, 3.8f + rj, emp);
            }
        }
        if (mlVivo) {
            empujarConImpulso(q, mlx, mly + 2, mlz, 6.5f + rj, emp);
        }
        if (vVivo) {
            float hx = q[0] - vsx[NV / 2], hz = q[2] - vsz[NV / 2];
            if (hx * hx + hz * hz < 30 * 30) {
                for (int k = 0; k < NV; k++) {
                    empujarConImpulso(q, vsx[k], vsy[k] + ondaVelo(k), vsz[k], (k == 0 ? 2.2f : 1.6f) + rj, emp);
                }
            }
        }
    }

    /** creature i is inside the hull sphere (q, r): move it out and let it bounce off */
    private void apartarDe(float[] q, int i, float r) {
        float ox = dx[i] - q[0], oy = dy[i] - q[1], oz = dz[i] - q[2];
        float d2 = ox * ox + oy * oy + oz * oz;
        if (d2 >= r * r || d2 < 1e-4f) {
            return;
        }
        float d = FMath.sqrt(d2);
        float nx = ox / d, ny = oy / d, nz = oz / d;
        dx[i] = q[0] + nx * r;
        dy[i] = q[1] + ny * r;
        dz[i] = q[2] + nz * r;
        // remove the speed going into the hull and bounce a little
        float vn = dvx[i] * nx + dvy[i] * ny + dvz[i] * nz;
        if (vn < 0) {
            dvx[i] -= nx * vn * 1.4f;
            dvy[i] -= ny * vn * 1.4f;
            dvz[i] -= nz * vn * 1.4f;
        }
    }

    /** vertical wave along the veil's body (m) */
    private float ondaVelo(int k) {
        return FMath.sin(vFase * 1.2f - k * 0.65f) * (0.3f + k * 0.12f);
    }

    /** true if (x, y, z) is within its calming glow */
    boolean cercaVelo(float x, float y, float z) {
        if (!vVivo) {
            return false;
        }
        for (int k = 0; k < NV; k += 3) {
            float dx = x - vsx[k], dy = y - vsy[k], dz = z - vsz[k];
            if (dx * dx + dy * dy + dz * dz < 22 * 22) {
                return true;
            }
        }
        return false;
    }

    private void actualizarVelo(float dt, float px, float py, float pz, int bio) {
        if (!vVivo) {
            if ((bio == Terreno.ABISMO || bio == Terreno.VACIO) && azar() < 0.003f
                    && puntoEn(px, pz, 70, 110, Terreno.ABISMO, Terreno.ABISMO, punto)) {
                vVivo = true;
                vRumbo = azar() * 360;
                float y = Terreno.altura(punto[0], punto[1]) + 24;
                float r = vRumbo * FMath.RAD;
                for (int k = 0; k < NV; k++) {
                    vsx[k] = punto[0] - FMath.cos(r) * SEP_VELO * k;
                    vsz[k] = punto[1] - FMath.sin(r) * SEP_VELO * k;
                    vsy[k] = y;
                }
                Log.d("velo abisal aparece en " + (int) vsx[0] + "," + (int) y + "," + (int) vsz[0]);
            }
            return;
        }
        float ox = px - vsx[0], oz = pz - vsz[0];
        if (ox * ox + oz * oz > 280 * 280) {
            vVivo = false;
            return;
        }
        vFase += dt;
        // the head cruises in long lazy curves, rising and sinking; it stays over the abyss
        vRumbo += FMath.sin(vFase * 0.05f) * 6 * dt;
        float r = vRumbo * FMath.RAD;
        float nx = vsx[0] + FMath.cos(r) * 2.2f * dt, nz = vsz[0] + FMath.sin(r) * 2.2f * dt;
        int b = Terreno.bioma(nx, nz);
        if (b != Terreno.ABISMO && b != Terreno.VACIO) {
            vRumbo += 30 * dt;
        } else {
            vsx[0] = nx;
            vsz[0] = nz;
        }
        float meta = Terreno.altura(vsx[0], vsz[0]) + 22 + FMath.sin(vFase * 0.11f) * 8;
        vsy[0] += (meta - vsy[0]) * Math.min(1, dt * 0.25f);
        // the rest of the body follows like a chain: each segment SEP_VELO behind the last
        for (int k = 1; k < NV; k++) {
            float dx = vsx[k] - vsx[k - 1], dy = vsy[k] - vsy[k - 1], dz = vsz[k] - vsz[k - 1];
            float d = FMath.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > SEP_VELO) {
                float f = SEP_VELO / d;
                vsx[k] = vsx[k - 1] + dx * f;
                vsy[k] = vsy[k - 1] + dy * f;
                vsz[k] = vsz[k - 1] + dz * f;
            }
        }
    }

    private void dibujarVelo(Graphics3D g3d) {
        for (int k = 0; k < NV; k++) {
            // heading of this segment: towards the one in front (the head looks ahead)
            float ax = k == 0 ? vsx[0] - vsx[1] : vsx[k - 1] - vsx[k];
            float az = k == 0 ? vsz[0] - vsz[1] : vsz[k - 1] - vsz[k];
            float ay = (k == 0 ? vsy[0] - vsy[1] : vsy[k - 1] - vsy[k]) + ondaVelo(k - 1) - ondaVelo(k);
            float yaw = FMath.atan2(ax, az) * 57.29578f;
            float pitch = -FMath.atan2(ay, FMath.sqrt(ax * ax + az * az)) * 57.29578f;
            t.setIdentity();
            t.postTranslate(vsx[k], vsy[k] + ondaVelo(k), vsz[k]);
            t.postRotate(yaw, 0, 1, 0);
            t.postRotate(pitch, 1, 0, 0);
            // the wings ripple: each segment rolls a little, out of phase with the next
            t.postRotate(FMath.cos(vFase * 1.2f - k * 0.65f) * 9, 0, 0, 1);
            float ancho = k == 0 ? 1 : 1.05f - k * 0.05f;
            t.postScale(ancho, 1, 1);
            g3d.render(mVelo[k == 0 ? 0 : (k == NV - 1 ? 2 : 1)], t);
        }
    }

    static void empujarEsfera(float[] q, float x, float y, float z, float r) {
        float dx = q[0] - x, dy = q[1] - y, dz = q[2] - z;
        float d2 = dx * dx + dy * dy + dz * dz;
        if (d2 < r * r && d2 > 1e-4f) {
            float d = FMath.sqrt(d2);
            q[0] = x + dx / d * r;
            q[1] = y + dy / d * r;
            q[2] = z + dz / d * r;
        }
    }

    /** push (q) out of a sphere and add the resulting shove to emp (may be null) */
    static void empujarConImpulso(float[] q, float x, float y, float z, float r, float[] emp) {
        float ox = q[0], oy = q[1], oz = q[2];
        empujarEsfera(q, x, y, z, r);
        if (emp != null) {
            emp[0] += q[0] - ox;
            emp[1] += q[1] - oy;
            emp[2] += q[2] - oz;
        }
    }

    /** the Ash Colossus from transform t: wings, whip tail and lure tendrils animated */
    private void dibujarColoso(Graphics3D g3d) {
            // slow, heavy wing beat from the shoulders, the body rising and falling with it
        float ala = FMath.sin(tiempo * 0.8f) * 16;
        t.postTranslate(0, -ala * 0.05f, 0);
        g3d.render(mColosoP[4], t);
        // whip tail: a slow S lash from its root
        tp.set(t);
        tp.postTranslate(0, 0, -9);
        tp.postRotate(FMath.sin(tiempo * 0.9f - 1) * 14, 0, 1, 0);
        tp.postRotate(FMath.sin(tiempo * 0.8f) * 5, 1, 0, 0);
        tp.postTranslate(0, 0, 9);
        g3d.render(mColosoP[2], tp);
        // lure tendrils: they sway under the mouth, lagging behind its movement
        tp.set(t);
        tp.postTranslate(0, -1.5f, 15);
        tp.postRotate(FMath.sin(tiempo * 1.3f) * 10 + (cEstado == CARGAR ? -25 : 0), 1, 0, 0);
        tp.postRotate(FMath.sin(tiempo * 0.9f + 2) * 8, 0, 0, 1);
        tp.postTranslate(0, 1.5f, -15);
        g3d.render(mColosoP[3], tp);
        tp.set(t);
        tp.postTranslate(8, 0, 0);
        tp.postRotate(ala, 0, 0, 1);
        tp.postTranslate(-8, 0, 0);
        g3d.render(mColosoP[0], tp);
        tp.set(t);
        tp.postTranslate(-8, 0, 0);
        tp.postRotate(-ala, 0, 0, 1);
        tp.postTranslate(8, 0, 0);
        g3d.render(mColosoP[1], tp);
    }

    /**
     * The Void Guardian patrols the abyss edge and the void. Like the colossus it notices
     * the diver from afar and charges; the Remora's pulse drives it off for a while.
     */
    private void actualizarGuardian(float dt, Partida p, float px, float py, float pz, int bio, boolean bajoAgua) {
        float rr = FMath.sqrt(px * px + pz * pz);
        boolean borde = bio == Terreno.VACIO || (bio == Terreno.ABISMO && rr > 1380);
        if (!gVivo) {
            if (borde && azar() < 0.004f && puntoEn(px, pz, 150, 200, Terreno.ABISMO, Terreno.VACIO, punto)) {
                gVivo = true;
                gVida = 10;
                gx = punto[0];
                gz = punto[1];
                gy = Math.max(py - 15, Terreno.altura(gx, gz) + 25);
                gEstado = MERODEAR;
                gAviso = false;
                gRumbo = azar() * 360;
                Log.d("guardian del vacio aparece en " + (int) gx + "," + (int) gy + "," + (int) gz);
            }
            return;
        }
        float ox = px - gx, oy = py - gy, oz = pz - gz;
        float d = FMath.sqrt(ox * ox + oy * oy + oz * oz);
        if (d > 300 || (!borde && d > 200)) {
            gVivo = false;
            Log.d("el guardian del vacio vuelve a la oscuridad");
            return;
        }
        gT -= dt;
        float vel, tx, ty, tz;
        if (gEstado == CARGAR) {
            vel = 14;
            tx = ox;
            ty = oy;
            tz = oz;
            if (!bajoAgua || d > 130) {
                gEstado = MERODEAR;
            } else if (d < 14) {
                p.danar(45, "El Guardián del Vacío te embistió");
                gEstado = RETIRARSE;
                gT = 9;
            }
        } else if (gEstado == RETIRARSE || gEstado == HUIR) {
            vel = 9;
            tx = -ox;
            ty = 0.2f;
            tz = -oz;
            if (gT <= 0) {
                gEstado = MERODEAR;
            }
        } else {
            vel = 6;
            gRumbo += 10 * dt;
            tx = FMath.cos(gRumbo * FMath.RAD);
            tz = FMath.sin(gRumbo * FMath.RAD);
            ty = (py - gy) * 0.04f;
            if (bajoAgua && d < 110 && !gAviso) {
                gAviso = true;
                Sonido.tocar(Sonido.RUGIDO);
                p.avisar("Algo inmenso se enrosca en el borde del vacío...");
            }
            if (bajoAgua && d < 75) {
                gEstado = CARGAR;
                Sonido.tocar(Sonido.RUGIDO);
                Log.d("guardian del vacio carga (" + (int) d + " m)");
            }
        }
        float tl = FMath.sqrt(tx * tx + ty * ty + tz * tz);
        if (tl > 0.001f) {
            float k = Math.min(1, dt * 0.9f);
            gvx += (tx / tl * vel - gvx) * k;
            gvy += (ty / tl * vel - gvy) * k;
            gvz += (tz / tl * vel - gvz) * k;
        }
        gx += gvx * dt;
        gy += gvy * dt;
        gz += gvz * dt;
        float suelo = Terreno.altura(gx, gz) + 10;
        if (gy < suelo) {
            gy = suelo;
        }
        if (gy > -40) {
            gy = -40;
        }
    }

    /** the moon jelly drifts at night over the shallows, kelp and meadows, then fades away */
    private void actualizarMedusa(float dt, Partida p, float px, float py, float pz, int bio) {
        boolean noche = p.luzDia() < 0.3f;
        boolean suya = bio == Terreno.BAJIOS || bio == Terreno.BOSQUE || bio == Terreno.PRADERA;
        if (!mlVivo) {
            if (noche && suya && azar() < 0.004f && puntoEn(px, pz, 60, 90, bio, bio, punto)) {
                mlVivo = true;
                mlx = punto[0];
                mlz = punto[1];
                mly = Math.min(-14, Terreno.altura(mlx, mlz) + 22);
                mlRumbo = azar() * 360;
                Log.d("medusa luna aparece en " + (int) mlx + "," + (int) mly + "," + (int) mlz);
            }
            return;
        }
        float ox = px - mlx, oz = pz - mlz;
        if (!noche || ox * ox + oz * oz > 220 * 220) {
            mlVivo = false;
            return;
        }
        mlFase += dt;
        mlRumbo += 3 * dt;
        float r = mlRumbo * FMath.RAD;
        mlx += FMath.cos(r) * 0.8f * dt;
        mlz += FMath.sin(r) * 0.8f * dt;
        float meta = Math.min(-14, Terreno.altura(mlx, mlz) + 22);
        mly += (meta - mly + FMath.sin(mlFase * 0.5f) * 3) * Math.min(1, dt * 0.2f);
    }

    /** the jelly from transform t: the bell pulses, the tentacles trail behind each pulse */
    private void dibujarMedusa(Graphics3D g3d, float f) {
        float pulso = FMath.sin(f * 1.1f);
        tp.set(t);
        t.postScale(1 + 0.12f * pulso, 1 - 0.14f * pulso, 1 + 0.12f * pulso);
        g3d.render(mMedusa[1], t);
        tp.postRotate(FMath.sin(f * 1.1f - 1.5f) * 10, 1, 0, 0);
        tp.postRotate(FMath.sin(f * 0.7f) * 7, 0, 0, 1);
        tp.postScale(1, 1 + 0.1f * FMath.sin(f * 1.1f - 1), 1);
        g3d.render(mMedusa[0], tp);
    }
}
