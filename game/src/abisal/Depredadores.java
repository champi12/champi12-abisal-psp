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
    static final String[] NOMBRE = { "dientesierra", "anguila de brasa", "acechador de algas", "mordedor de pasto" };

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
    private final Mesh[] mColosoP, mErrante;
    private final Mesh mCardumen;

    // reef wanderer: one huge gentle giant drifting through the kelp, meadows and plateau
    boolean eVivo;
    float ex, ey, ez, eRumbo, eFase;
    static final float ESC_ERRANTE = 2.0f;
    /** a lurker hiding in grass patch k of the wanderer's back (-1 = not on it) */
    final int[] dParche = { -1, -1 };
    /** coral left on its back to harvest (only found here), and its parasites */
    int eCoral;
    private final Mesh mParasitos;
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
        mCardumen = Modelos.cardumen(fog);
        mParasitos = Modelos.parasitos(fog);
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
            if (d > 130) {
                dVivo[i] = false;
                continue;
            }
            float vel;
            float tx, ty, tz;
            dT[i] -= dt;
            switch (dEstado[i]) {
            case CARGAR:
                vel = dTipo[i] == 1 ? 10 : (dTipo[i] == 2 ? 11 : (dTipo[i] == 3 ? 9.5f : 8.5f));
                tx = ox;
                ty = oy;
                tz = oz;
                if (!bajoAgua || d > 45) {
                    dEstado[i] = MERODEAR;
                } else if (d < 2.2f) {
                    p.danar(dTipo[i] == 1 ? 18 : (dTipo[i] == 2 ? 16 : (dTipo[i] == 3 ? 10 : 22)),
                            "Te mordio: " + NOMBRE[dTipo[i]]);
                    dEstado[i] = RETIRARSE;
                    dT[i] = 6;
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
                if (dh < 2.4f && aT[i] <= 0) {
                    p.danar(12, "Un acorazado te golpeo con sus pinzas");
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
                p.mundo.empujar(tmp, 0.9f);
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
                        p.danar(55, "El Coloso de Ceniza te embistio");
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

        // ---------------------------------------------------------- reef wanderer
        boolean suyo = bio == Terreno.BOSQUE || bio == Terreno.PRADERA || bio == Terreno.MESETA;
        if (!eVivo) {
            if (suyo && azar() < 0.003f && puntoEn(px, pz, 70, 110, bio, bio, punto)) {
                eVivo = true;
                ex = punto[0];
                ez = punto[1];
                ey = Terreno.altura(ex, ez) + 16;
                eRumbo = azar() * 360;
                eCoral = 5;
                Log.d("errante de arrecife aparece en " + (int) ex + "," + (int) ey + "," + (int) ez);
            }
        } else {
            float ox = px - ex, oz = pz - ez;
            if (ox * ox + oz * oz > 260 * 260) {
                eVivo = false;
            } else {
                // a slow cruise in wide curves, turning back at the edge of its waters
                eFase += dt;
                eRumbo += FMath.sin(eFase * 0.07f) * 4 * dt;
                float r = eRumbo * FMath.RAD;
                float nx = ex + FMath.cos(r) * 1.2f * dt, nz = ez + FMath.sin(r) * 1.2f * dt;
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
                if (dTipo[i] == 1) {
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
                if (azar() < 0.6f) {
                    p.danar(8, "Un acorazado te golpeo con sus pinzas");
                }
                aEstado[i] = HUIR;
                aT[i] = 4;
                Log.d("cuchillazo a un acorazado: contraataca, vida " + aVida[i]);
                return "Cuchillazo! El acorazado se defiende";
            }
        }
        return null;
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
        float ox = cx - px, oy = cy - py, oz = cz - pz;
        if (cVivo && ox * ox + oy * oy + oz * oz < (r + 12) * (r + 12)) {
            // it cannot be hurt, but the shock makes it break off its charge
            n++;
            cEstado = RETIRARSE;
            cT = 8;
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
            if (aVivo[i] && delante(ax[i] - px, ay[i] + 0.5f - py, az[i] - pz, fx, fy, fz, 2.6f)) {
                return true;
            }
        }
        return false;
    }

    private static boolean delante(float ox, float oy, float oz, float fx, float fy, float fz, float r) {
        float d2 = ox * ox + oy * oy + oz * oz;
        if (d2 > r * r) {
            return false;
        }
        float d = FMath.sqrt(d2);
        return d < 1.2f || (ox * fx + oy * fy + oz * fz) > d * 0.4f;
    }

    void dibujar(Graphics3D g3d, float px, float pz, float fx, float fz, float lejos) {
        dibujados = 0;
        for (int i = 0; i < ND; i++) {
            if (!dVivo[i] || !visible(dx[i] - px, dz[i] - pz, fx, fz, lejos, 3)) {
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
            for (int q = 0; q < 3; q++) {
                float a = eFase * (0.3f + q * 0.1f) + q * 2.1f;
                tp.set(t);
                tp.postTranslate(FMath.sin(a) * 1.6f, 3.35f, -2 + q * 2.5f + FMath.cos(a * 0.7f) * 1.5f);
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
    private void dibujarSerpiente(Graphics3D g3d, int tipo, boolean carga, int i) {
        if (tipo == 2) {
            dibujarCalamar(g3d, carga, i);
            return;
        }
        if (tipo == 3) {
            dibujarSapo(g3d, carga, i);
            return;
        }
        float frec = carga ? 9 : 5, amp = carga ? 22 : 14;
        Mesh[] seg = tipo == 1 ? segAnguila : (tipo == 2 ? segAcechador : (tipo == 3 ? segMordedor : segDiente));
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
        float r = eRumbo * FMath.RAD;
        for (int k = -1; k <= 1; k += 2) {
            float cx0 = ex + FMath.cos(r) * k * 4 * ESC_ERRANTE, cz0 = ez + FMath.sin(r) * k * 4 * ESC_ERRANTE;
            float dx = q[0] - cx0, dy = q[1] - ey, dz = q[2] - cz0;
            float d2 = dx * dx + dy * dy + dz * dz;
            float rr = 4.2f * ESC_ERRANTE + rj;
            if (d2 < rr * rr && d2 > 1e-4f) {
                float d = FMath.sqrt(d2);
                q[0] = cx0 + dx / d * rr;
                q[1] = ey + dy / d * rr;
                q[2] = cz0 + dz / d * rr;
            }
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
                    if (Terreno.azar((int) refugio[0], (int) refugio[2], 909) > 0.5f) {
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
            sT[i] -= dt;
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
                    escupir(i, px, py, pz);
                }
            } else {
                // idle: drift around its home
                sRumbo[i] += 25 * dt;
                float r = sRumbo[i] * FMath.RAD;
                sx[i] += (shx[i] + FMath.sin(r) * 2.5f - sx[i]) * Math.min(1, dt * 0.8f);
                sz[i] += (shz[i] + FMath.cos(r) * 2.5f - sz[i]) * Math.min(1, dt * 0.8f);
                sy[i] += (shy[i] + FMath.sin(tiempo + i) * 0.4f - sy[i]) * Math.min(1, dt);
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
                p.danar(7, "Te alcanzo el acido de un escupidor");
            } else if (bt[b] <= 0 || by[b] < Terreno.altura(bx[b], bz[b])) {
                bVivo[b] = false;
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
                Sonido.tocar(Sonido.BURBUJAS);
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
            float hincha = sT[i] < 0.5f ? 1 + (0.5f - sT[i]) * 0.8f : 1;
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

    /** the diver cannot swim through creatures: push (q) out of their bodies */
    void empujarCriaturas(float[] q, float rj) {
        for (int i = 0; i < ND; i++) {
            if (dVivo[i]) {
                empujarEsfera(q, dx[i], dy[i], dz[i], (dTipo[i] == 3 ? 0.35f : 0.6f) + rj);
            }
        }
        for (int i = 0; i < NA; i++) {
            if (aVivo[i]) {
                empujarEsfera(q, ax[i], ay[i] + 0.5f, az[i], 0.9f + rj);
            }
        }
        for (int i = 0; i < NS; i++) {
            if (sVivo[i]) {
                empujarEsfera(q, sx[i], sy[i], sz[i], 0.45f + rj);
            }
        }
        if (cVivo) {
            empujarEsfera(q, cx, cy, cz, 7 + rj);
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
}
