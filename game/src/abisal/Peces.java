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
    static final int N = 14;
    final float[] x = new float[N];
    final float[] y = new float[N];
    final float[] z = new float[N];
    final float[] dir = new float[N];   // heading, degrees
    final float[] vel = new float[N];
    final float[] fase = new float[N];
    final boolean[] vivo = new boolean[N];
    final float[] suelo = new float[N];
    private int tic;
    /** 0 = bubblefin (shallows and kelp), 1 = lanternling (canyons and abyss) */
    final int[] especie = new int[N];
    private final Mesh[] mallas = new Mesh[2];
    private final Transform t = new Transform();
    private int semilla = 99;
    int dibujados;

    Peces(Fog fog) {
        mallas[0] = Modelos.burbujinV2(fog);
        mallas[1] = Modelos.farolillo(fog);
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
        return -1;
    }

    private boolean habitat(int i, float x, float z) {
        return especieDe(x, z) == especie[i];
    }

    private void nacer(int i, float px, float py, float pz) {
        float a = azar() * 6.2832f;
        float d = 18 + azar() * 20;
        float nx = px + j2me.FMath.cos(a) * d;
        float nz = pz + j2me.FMath.sin(a) * d;
        int e = especieDe(nx, nz);
        if (e < 0) {
            vivo[i] = false;
            return;
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
    }

    void actualizar(float dt, float px, float py, float pz) {
        tic++;
        for (int i = 0; i < N; i++) {
            // the expensive terrain queries run every 8 frames per fish, staggered
            boolean consultar = ((tic + i) & 7) == 0;
            if (!vivo[i]) {
                if (azar() < 0.02f) {
                    nacer(i, px, py, pz);
                }
                continue;
            }
            float dx = x[i] - px;
            float dy = y[i] - py;
            float dz = z[i] - pz;
            float d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > 50 * 50) {
                nacer(i, px, py, pz);
                continue;
            }
            float v = vel[i];
            if (d2 < 4.5f * 4.5f) {
                // flee: turn away from the player and speed up
                float huir = j2me.FMath.atan2(dz, dx) * 57.29578f;
                dir[i] += angulo(huir - dir[i]) * Math.min(1, dt * 4);
                v *= 2.6f;
            } else {
                dir[i] += (azar() - 0.5f) * 90 * dt;
            }
            float r = dir[i] * j2me.FMath.RAD;
            float nx = x[i] + j2me.FMath.cos(r) * v * dt;
            float nz = z[i] + j2me.FMath.sin(r) * v * dt;
            if (consultar && !habitat(i, nx, nz)) {
                dir[i] += 180;
                continue;
            }
            x[i] = nx;
            z[i] = nz;
            fase[i] += dt * 3;
            if (consultar) {
                suelo[i] = Terreno.altura(x[i], z[i]) + 1.0f;
            }
            y[i] += j2me.FMath.sin(fase[i]) * 0.3f * dt;
            if (y[i] < suelo[i]) {
                y[i] = suelo[i];
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
            t.postRotate(j2me.FMath.sin(fase[i] * 3) * 8, 0, 1, 0);
            g3d.render(mallas[especie[i]], t);
            dibujados++;
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
