package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

/**
 * Small fish around the player. They wander in loose schools, flee when the player gets
 * close and are respawned out of sight when they fall too far behind, so the sea always
 * feels alive without simulating thousands of fish.
 */
public final class Peces {
    static final int N = 24;
    final float[] x = new float[N];
    final float[] y = new float[N];
    final float[] z = new float[N];
    final float[] dir = new float[N];   // heading, degrees
    final float[] vel = new float[N];
    final float[] fase = new float[N];
    final boolean[] vivo = new boolean[N];
    final float[] suelo = new float[N];
    /** hits left before it dies, and seconds of panic after being hit */
    final int[] vida = new int[N];
    final float[] miedo = new float[N];
    /** smooth steering: wanted heading, time until a new one, current speed, turn rate
     *  (for the bank), wanted height and vertical speed */
    final float[] objetivo = new float[N], giroT = new float[N], velAct = new float[N], giro = new float[N];
    final float[] yObj = new float[N], vyPez = new float[N];
    /** cave fish: they stay within a few metres of their cave (hogar) */
    final boolean[] conHogar = new boolean[N];
    final float[] hx = new float[N], hz = new float[N];
    Mundo mundo;
    private final float[] cueva = new float[3];
    private int tic;
    /** 0 = bubblefin (shallows, kelp), 1 = lanternling (canyons, abyss), 2 = ribbonfin (kelp) */
    final int[] especie = new int[N];
    private final Mesh[] mallas = new Mesh[10];
    private final Transform t = new Transform();
    private int semilla = 99;
    int dibujados;

    Peces(Fog fog) {
        mallas[0] = Modelos.burbujinV2(fog);
        mallas[1] = Modelos.farolillo(fog);
        mallas[2] = Modelos.lazofin(fog);
        mallas[3] = Modelos.planeador(fog);
        mallas[4] = Modelos.medusa(fog);
        mallas[5] = Modelos.cardumen(fog);
        mallas[6] = Modelos.pastorArena(fog);
        mallas[7] = Modelos.mantaCristal(fog);
        mallas[8] = Modelos.caracolDuna(fog);
        mallas[9] = Modelos.camaronesIgneos(fog);
    }

    private float azar() {
        semilla = semilla * 1103515245 + 12345;
        return ((semilla >>> 8) & 0xffff) / 65535f;
    }

    /** which species lives at a point (-1: none) */
    private static int especieDe(float x, float z) {
        int b = Terreno.bioma(x, z);
        if (b <= Terreno.BOSQUE) {
            return 0;
        }
        if (b == Terreno.CANONES || b == Terreno.ABISMO) {
            return 1;
        }
        if (b == Terreno.PRADERA) {
            return 3;
        }
        if (b == Terreno.TERMAL) {
            return 9;
        }
        return -1;
    }

    private boolean habitat(int i, float x, float z) {
        int b = Terreno.bioma(x, z);
        switch (especie[i]) {
        case 2:
            return b == Terreno.BOSQUE;
        case 3:
            return b <= Terreno.BOSQUE || b == Terreno.PRADERA;
        case 4:
            return b >= Terreno.BOSQUE && b <= Terreno.ABISMO;
        case 5:
            return b <= Terreno.BOSQUE || b == Terreno.PRADERA;
        case 6:
            return b == Terreno.BOSQUE || b == Terreno.MESETA || b == Terreno.PRADERA;
        case 7:
        case 8:
            return b == Terreno.PRADERA;
        case 9:
            return b == Terreno.TERMAL;
        case 1:
            return b == Terreno.CANONES || b == Terreno.ABISMO || b == Terreno.TERMAL;
        default:
            return especieDe(x, z) == especie[i];
        }
    }

    private void nacer(int i, float px, float py, float pz) {
        float a = azar() * 6.2832f;
        float d = 18 + azar() * 20;
        float nx = px + j2me.FMath.cos(a) * d;
        float nz = pz + j2me.FMath.sin(a) * d;
        // some bubblefins live in the small caves of the shallows
        conHogar[i] = false;
        if (mundo != null && azar() < 0.45f && mundo.cuevaCercana(px, pz, 45, cueva)
                && Terreno.bioma(cueva[0], cueva[2]) == Terreno.BAJIOS) {
            especie[i] = 0;
            conHogar[i] = true;
            hx[i] = cueva[0];
            hz[i] = cueva[2];
            x[i] = cueva[0] + (azar() - 0.5f) * 3;
            z[i] = cueva[2] + (azar() - 0.5f) * 3;
            float s0 = Terreno.altura(x[i], z[i]);
            suelo[i] = s0 + 0.6f;
            y[i] = s0 + 1 + azar();
            dir[i] = azar() * 360;
            vel[i] = 0.8f + azar() * 0.5f;
            fase[i] = azar() * 6;
            vivo[i] = true;
            vida[i] = 1;
            miedo[i] = 0;
            return;
        }
        int e = especieDe(nx, nz);
        if (e < 0 && Terreno.bioma(nx, nz) == Terreno.MESETA) {
            e = 4;
        }
        if (e < 0) {
            vivo[i] = false;
            return;
        }
        int bio = Terreno.bioma(nx, nz);
        float r = azar();
        // mix of species per biome
        boolean hayPastor = false;
        for (int q = 0; q < N; q++) {
            hayPastor |= vivo[q] && especie[q] == 6;
        }
        if (bio == Terreno.BAJIOS) {
            e = r < 0.2f ? 3 : (r < 0.42f ? 5 : 0);
        } else if (bio == Terreno.BOSQUE) {
            e = r < 0.38f ? 2 : (r < 0.52f ? 0 : (r < 0.62f ? 3 : (r < 0.72f ? 4 : (r < 0.9f || hayPastor ? 5 : 6))));
        } else if (bio == Terreno.PRADERA) {
            // meadows: mantas glide over the dunes, snails graze, gliders and schools
            e = r < 0.22f ? 7 : (r < 0.45f ? 8 : (r < 0.65f ? 3 : (r < 0.85f || hayPastor ? 5 : 6)));
        } else if (bio == Terreno.TERMAL) {
            // rifts: ember shrimp swarm over the vents, a few lanternlings
            e = r < 0.65f ? 9 : 1;
        } else if (bio == Terreno.MESETA) {
            e = r < 0.75f || hayPastor ? 4 : 6;
        } else if (e == 1 && r < 0.2f) {
            e = 4;
        }
        especie[i] = e;
        float s = Terreno.altura(nx, nz);
        suelo[i] = s + 1.0f;
        x[i] = nx;
        z[i] = nz;
        y[i] = Math.min(-1.5f, Math.max(s + 1.5f, py + (azar() - 0.5f) * 8));
        dir[i] = azar() * 360;
        vel[i] = 1.2f + azar();
        fase[i] = azar() * 6;
        vivo[i] = true;
        vida[i] = e == 6 || e == 7 ? 3 : (e == 4 ? 2 : 1);
        if (e == 7) {
            y[i] = Math.max(y[i], s + 3); // mantas cruise a few metres above the dunes
        }
        miedo[i] = 0;
    }

    void actualizar(float dt, float px, float py, float pz) {
        tic++;
        for (int i = 0; i < N; i++) {
            // the expensive terrain queries run every 8 frames per fish, staggered
            boolean consultar = ((tic + i) & 7) == 0;
            if (!vivo[i]) {
                if (azar() < 0.02f) {
                    nacer(i, px, py, pz);
                    objetivo[i] = dir[i];
                    velAct[i] = vel[i] * 0.5f;
                    yObj[i] = y[i];
                    vyPez[i] = 0;
                    giro[i] = 0;
                    giroT[i] = 0;
                }
                continue;
            }
            float dx = x[i] - px;
            float dy = y[i] - py;
            float dz = z[i] - pz;
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > 50 * 50) {
                vivo[i] = false; // reborn out of sight next frames
                continue;
            }
            int e = especie[i];
            float v = vel[i];
            float giroMax = 70; // degrees per second when cruising
            if (e == 4) {
                v = 0.3f; // jellies drift
                giroMax = 15;
            } else if (e == 6) {
                v = 0.7f; // the shepherd grazes slowly
                giroMax = 30;
            } else if (e == 7) {
                v = 1.1f; // the manta glides in wide arcs
                giroMax = 30;
            } else if (e == 8) {
                v = 0.22f; // snails crawl
                giroMax = 40;
            }
            if (miedo[i] > 0) {
                miedo[i] -= dt;
            }
            boolean huye = e != 8 && (miedo[i] > 0 || (d2 < 4.5f * 4.5f && e != 4 && e != 6 && e != 7));
            giroT[i] -= dt;
            if (huye) {
                // flee: head straight away from the diver, turning hard and fast
                objetivo[i] = j2me.FMath.atan2(dz, dx) * 57.29578f;
                giroMax = 260;
                v *= 2.6f;
            } else if (conHogar[i] && (x[i] - hx[i]) * (x[i] - hx[i]) + (z[i] - hz[i]) * (z[i] - hz[i]) > 9) {
                // too far from its cave: head home
                objetivo[i] = j2me.FMath.atan2(hz[i] - z[i], hx[i] - x[i]) * 57.29578f;
            } else if (giroT[i] <= 0) {
                // wander: pick a new heading now and then, not a new jitter every frame
                objetivo[i] = dir[i] + (azar() - 0.5f) * 120;
                giroT[i] = 2 + azar() * 4;
                // and a new cruising height near the current one
                yObj[i] = y[i] + (azar() - 0.5f) * 2.5f;
            }
            // turn towards the wanted heading at a limited rate (eased near the end)
            float delta = angulo(objetivo[i] - dir[i]);
            float paso = Math.max(-giroMax * dt, Math.min(giroMax * dt, delta * Math.min(1, dt * 2.5f)));
            dir[i] += paso;
            giro[i] += (paso / Math.max(dt, 0.001f) - giro[i]) * Math.min(1, dt * 4);
            // speed eases too: a fish accelerates with a few tail beats
            velAct[i] += (v - velAct[i]) * Math.min(1, dt * (huye ? 4 : 1.5f));
            float vv = velAct[i];
            float r = dir[i] * j2me.FMath.RAD;
            float nx = x[i] + j2me.FMath.cos(r) * vv * dt;
            float nz = z[i] + j2me.FMath.sin(r) * vv * dt;
            if (consultar && !conHogar[i] && !habitat(i, nx, nz)) {
                // leaving its biome: turn around smoothly instead of snapping
                objetivo[i] = dir[i] + 180;
                giroT[i] = 3;
            }
            x[i] = nx;
            z[i] = nz;
            fase[i] += dt * (2 + vv * 2.5f);
            if (consultar) {
                suelo[i] = Terreno.altura(x[i], z[i]) + 1.0f;
            }
            // vertical: ease towards the wanted height, kept between the floor and the surface
            boolean fondo = e == 3 || e == 6 || e == 8;
            float minY = suelo[i] + (e == 8 ? -0.95f : (fondo ? 0.2f : (e == 7 ? 2.0f : 0.6f)));
            float alto = fondo ? minY + (e == 6 ? 0.8f : (e == 8 ? 0 : 0.4f)) : yObj[i];
            alto = Math.max(minY, Math.min(-1.2f, alto));
            float vyObj = (alto - y[i]) * 0.8f + j2me.FMath.sin(fase[i] * 0.5f) * (e == 4 ? 0.25f : 0.08f);
            vyObj = Math.max(-1.2f, Math.min(1.2f, vyObj));
            vyPez[i] += (vyObj - vyPez[i]) * Math.min(1, dt * 2);
            y[i] += vyPez[i] * dt;
            if (y[i] < minY - 0.4f) {
                y[i] += (minY - 0.4f - y[i]) * Math.min(1, dt * 6); // never sink into the floor
            }
            if (e == 8) {
                y[i] = minY; // the snail stays on the sand
            }
            if (y[i] > -1) {
                y[i] = -1;
            }
        }
    }

    static float angulo(float a) {
        while (a > 180) {
            a -= 360;
        }
        while (a < -180) {
            a += 360;
        }
        return a;
    }

    void dibujar(Graphics3D g3d, float px, float pz, float fx, float fz, float lejos) {
        dibujados = 0;
        for (int i = 0; i < N; i++) {
            if (!vivo[i]) {
                continue;
            }
            float ox = x[i] - px;
            float oz = z[i] - pz;
            if (ox * ox + oz * oz > lejos * lejos || (ox * fx + oz * fz) < -2) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(x[i], y[i], z[i]);
            // the model faces +Z; heading 0 = +X
            t.postRotate(90 - dir[i], 0, 1, 0);
            // lean into turns and nose up/down when climbing or diving
            float banco = Math.max(-25, Math.min(25, -giro[i] * 0.25f));
            float cabeceo = Math.max(-25, Math.min(25, -vyPez[i] * 30 / Math.max(0.4f, velAct[i])));
            t.postRotate(cabeceo, 1, 0, 0);
            t.postRotate(banco, 0, 0, 1);
            float f = fase[i];
            switch (especie[i]) {
            case 3: // glider: the wings beat as a slow roll, with a gentle pitch
                t.postRotate(j2me.FMath.sin(f * 2) * 14, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 1.3f) * 5, 1, 0, 0);
                break;
            case 4: { // jelly: the bell pulses
                float pulso = j2me.FMath.sin(f * 2.2f);
                t.postScale(1 + 0.12f * pulso, 1 - 0.14f * pulso, 1 + 0.12f * pulso);
                break;
            }
            case 5: // school: the whole group weaves
                t.postRotate(j2me.FMath.sin(f * 1.5f) * 12, 0, 1, 0);
                t.postRotate(j2me.FMath.sin(f * 1.1f) * 6, 0, 0, 1);
                break;
            case 7: // manta: slow, deep wing beats
                t.postRotate(j2me.FMath.sin(f * 0.9f) * 10, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 0.9f + 1.2f) * 6, 1, 0, 0);
                t.postScale(1, 1 + 0.25f * j2me.FMath.sin(f * 1.8f), 1);
                break;
            case 8: // snail: a slow stretch of the foot
                t.postScale(1, 1, 1 + 0.08f * j2me.FMath.sin(f * 2));
                break;
            case 9: // shrimp swarm: jittery darting
                t.postRotate(j2me.FMath.sin(f * 2.5f) * 18, 0, 1, 0);
                t.postRotate(j2me.FMath.sin(f * 1.7f) * 8, 1, 0, 0);
                break;
            case 6: // shepherd: a slow, heavy sway
                t.postRotate(j2me.FMath.sin(f * 0.8f) * 5, 0, 0, 1);
                t.postRotate(j2me.FMath.sin(f * 0.6f) * 6, 0, 1, 0);
                break;
            default: { // fish: a gentle body yaw from the tail beat, stronger when fast
                float r = j2me.FMath.sin(f * 2.4f);
                t.postRotate(r * (4 + velAct[i] * 2.5f), 0, 1, 0);
            }
            }
            g3d.render(mallas[especie[i]], t);
            dibujados++;
        }
    }

    /**
     * Knife hit on the fish in front of the diver. Returns its index (or -1); the caller
     * reads especie[] and vivo[] to know what happened. A survivor panics and flees.
     */
    int golpear(float px, float py, float pz, float fx, float fy, float fz) {
        int mejor = alAlcance(px, py, pz, fx, fy, fz);
        if (mejor >= 0) {
            vida[mejor]--;
            if (vida[mejor] <= 0) {
                vivo[mejor] = false;
            } else {
                miedo[mejor] = 6;
            }
        }
        return mejor;
    }

    /** the fish in front of the diver within knife reach, or -1 */
    int alAlcance(float px, float py, float pz, float fx, float fy, float fz) {
        int mejor = -1;
        float md = 3.0f;
        for (int i = 0; i < N; i++) {
            if (!vivo[i]) {
                continue;
            }
            float ox = x[i] - px, oy = y[i] - py, oz = z[i] - pz;
            float alcance = especie[i] == 6 ? 3.0f : 2.4f;
            float d = j2me.FMath.sqrt(ox * ox + oy * oy + oz * oz);
            if (d < alcance && d < md && (d < 0.9f || ox * fx + oy * fy + oz * fz > d * 0.55f)) {
                md = d;
                mejor = i;
            }
        }
        return mejor;
    }

    /** every fish within r metres panics (Remora pulse) */
    void asustar(float px, float py, float pz, float r) {
        for (int i = 0; i < N; i++) {
            float ox = x[i] - px, oy = y[i] - py, oz = z[i] - pz;
            if (vivo[i] && ox * ox + oy * oy + oz * oz < r * r) {
                miedo[i] = 5;
            }
        }
    }

    /** index of a catchable fish (bubblefin) within reach of the hand, or -1 */
    int alcanzable(float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
            if (!vivo[i] || especie[i] != 0) {
                continue;
            }
            float dx = x[i] - px;
            float dy = y[i] - py;
            float dz = z[i] - pz;
            if (dx * dx + dy * dy + dz * dz < 1.8f * 1.8f) {
                return i;
            }
        }
        return -1;
    }
}
