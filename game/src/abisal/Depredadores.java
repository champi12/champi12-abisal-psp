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

    private final Mesh mDiente;
    private final Mesh mAcorazado;
    private final Mesh mColoso;
    private final Transform t = new Transform();
    private int semilla = 4242;

    // sawtooth
    final boolean[] dVivo = new boolean[ND];
    final float[] dx = new float[ND], dy = new float[ND], dz = new float[ND];
    final float[] dvx = new float[ND], dvy = new float[ND], dvz = new float[ND];
    final float[] dRumbo = new float[ND];
    final int[] dEstado = new int[ND];
    final float[] dT = new float[ND];

    // ironback crab
    final boolean[] aVivo = new boolean[NA];
    final float[] ax = new float[NA], ay = new float[NA], az = new float[NA];
    final float[] aRumbo = new float[NA];
    final int[] aEstado = new int[NA];
    final float[] aT = new float[NA];
    final float[] aFase = new float[NA];

    // ash colossus
    boolean cVivo;
    float cx, cy, cz, cvx, cvy, cvz, cRumbo;
    int cEstado;
    float cT;
    boolean cAviso;

    int dibujados;
    private float tiempo;

    Depredadores(Fog fog) {
        mDiente = Modelos.dientesierra(fog);
        mAcorazado = Modelos.acorazado(fog);
        mColoso = Modelos.colosoCeniza(fog);
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

    void actualizar(float dt, Partida p) {
        tiempo += dt;
        float px = p.px, py = p.py, pz = p.pz;
        int bio = Terreno.bioma(px, pz);
        boolean bajoAgua = py < -3;

        // ---------------------------------------------------------- sawtooth
        for (int i = 0; i < ND; i++) {
            if (!dVivo[i]) {
                if (bio == Terreno.MESETA && azar() < 0.01f
                        && puntoEn(px, pz, 45, 70, Terreno.MESETA, Terreno.MESETA, punto)) {
                    dVivo[i] = true;
                    dx[i] = punto[0];
                    dz[i] = punto[1];
                    dy[i] = Terreno.altura(dx[i], dz[i]) + 3;
                    dEstado[i] = MERODEAR;
                    dRumbo[i] = azar() * 360;
                    Log.d("dientesierra aparece en " + (int) dx[i] + "," + (int) dy[i] + "," + (int) dz[i]);
                }
                continue;
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
                vel = 8.5f;
                tx = ox;
                ty = oy;
                tz = oz;
                if (!bajoAgua || d > 45) {
                    dEstado[i] = MERODEAR;
                } else if (d < 2.2f) {
                    p.danar(22, "Te mordio un dientesierra");
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
                vel = 2.2f;
                dRumbo[i] += (azar() - 0.5f) * 60 * dt;
                tx = FMath.cos(dRumbo[i] * FMath.RAD);
                tz = FMath.sin(dRumbo[i] * FMath.RAD);
                ty = (Terreno.altura(dx[i], dz[i]) + 3 - dy[i]) * 0.3f;
                if (bajoAgua && d < 28) {
                    dEstado[i] = CARGAR;
                    Log.d("dientesierra carga contra el jugador (" + (int) d + " m)");
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
    }

    /** knife attack in front of the diver; true if something was hit */
    boolean golpear(float px, float py, float pz, float fx, float fy, float fz) {
        for (int i = 0; i < ND; i++) {
            if (dVivo[i] && delante(dx[i] - px, dy[i] - py, dz[i] - pz, fx, fy, fz, 2.8f)) {
                dEstado[i] = HUIR;
                dT[i] = 8;
                Log.d("cuchillazo a un dientesierra: huye");
                return true;
            }
        }
        for (int i = 0; i < NA; i++) {
            if (aVivo[i] && delante(ax[i] - px, ay[i] + 0.5f - py, az[i] - pz, fx, fy, fz, 2.6f)) {
                aEstado[i] = HUIR;
                aT[i] = 5;
                Log.d("cuchillazo a un acorazado: retrocede");
                return true;
            }
        }
        return false;
    }

    /** true if a creature is close enough to be hit with the knife */
    boolean alAlcance(float px, float py, float pz, float fx, float fy, float fz) {
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
            // swimming wiggle
            t.postRotate(FMath.sin(tiempo * 7 + i) * 6, 0, 1, 0);
            g3d.render(mDiente, t);
            dibujados++;
        }
        for (int i = 0; i < NA; i++) {
            if (!aVivo[i] || !visible(ax[i] - px, az[i] - pz, fx, fz, lejos, 2)) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(ax[i], ay[i] + FMath.sin(aFase[i]) * 0.05f, az[i]);
            t.postRotate(90 - aRumbo[i], 0, 1, 0);
            g3d.render(mAcorazado, t);
            dibujados++;
        }
        if (cVivo && visible(cx - px, cz - pz, fx, fz, lejos + 40, 40)) {
            orientar(cx, cy, cz, cvx, cvy, cvz);
            // slow wing beat
            t.postRotate(FMath.sin(tiempo * 0.8f) * 4, 0, 0, 1);
            g3d.render(mColoso, t);
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
}
