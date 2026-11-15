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
    private final Mesh malla;
    private final Transform t = new Transform();
    private int semilla = 99;
    int dibujados;

    Peces(Fog fog) {
        malla = Modelos.pezBurbujin(fog);
    }

    private float azar() {
        semilla = semilla * 1103515245 + 12345;
        return ((semilla >>> 8) & 0xffff) / 65535f;
    }

    /** fish only live in the shallows and the kelp forest */
    private static boolean habitat(float x, float z) {
        return Terreno.bioma(x, z) <= Terreno.BOSQUE;
    }

    private void nacer(int i, float px, float py, float pz) {
        float a = azar() * 6.2832f;
        float d = 18 + azar() * 20;
        float nx = px + (float) Math.cos(a) * d;
        float nz = pz + (float) Math.sin(a) * d;
        if (!habitat(nx, nz)) {
            vivo[i] = false;
            return;
        }
        float suelo = Terreno.altura(nx, nz);
        x[i] = nx;
        z[i] = nz;
        y[i] = Math.min(-1.5f, Math.max(suelo + 1.5f, py + (azar() - 0.5f) * 8));
        dir[i] = azar() * 360;
        vel[i] = 1.2f + azar();
        fase[i] = azar() * 6;
        vivo[i] = true;
    }

    void actualizar(float dt, float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
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
                float huir = (float) Math.toDegrees(Math.atan2(dz, dx));
                dir[i] += angulo(huir - dir[i]) * Math.min(1, dt * 4);
                v *= 2.6f;
            } else {
                dir[i] += (azar() - 0.5f) * 90 * dt;
            }
            float r = (float) Math.toRadians(dir[i]);
            float nx = x[i] + (float) Math.cos(r) * v * dt;
            float nz = z[i] + (float) Math.sin(r) * v * dt;
            if (!habitat(nx, nz)) {
                dir[i] += 180;
                continue;
            }
            x[i] = nx;
            z[i] = nz;
            fase[i] += dt * 3;
            float suelo = Terreno.altura(x[i], z[i]) + 1.0f;
            y[i] += (float) Math.sin(fase[i]) * 0.3f * dt;
            if (y[i] < suelo) {
                y[i] = suelo;
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
            t.postRotate((float) Math.sin(fase[i] * 3) * 8, 0, 1, 0);
            g3d.render(malla, t);
            dibujados++;
        }
    }

    /** index of a fish within reach of the hand, or -1 */
    int alcanzable(float px, float py, float pz) {
        for (int i = 0; i < N; i++) {
            if (!vivo[i]) {
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
