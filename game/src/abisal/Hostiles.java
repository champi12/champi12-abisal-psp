package abisal;

import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Graphics3D;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Transform;

import j2me.FMath;

/**
 * Zone hostiles with their own attacks (none of them just bites):
 *  - needlefish (shallows, only at night): a swarm of three that dart in straight lines
 *    and sting: poison, damage over time;
 *  - lightning jelly (kelp forest, meadows): charges up when the diver is near and
 *    discharges around itself: damage and a stun (the diver moves slowly);
 *  - canyon screecher (canyons): clings to the walls, shrieks when it spots the diver
 *    (blinding the view for a few seconds) and then swoops to bite;
 *  - abyss prowler (abyss): only its glowing lure shows in the dark; when the diver comes
 *    close it lunges, bites and drains oxygen.
 */
public final class Hostiles {
    private final Transform t = new Transform(), tp = new Transform();
    private int semilla = 777;
    float tiempo;

    // needlefish swarm
    static final int NAG = 3;
    final boolean[] agVivo = new boolean[NAG];
    final float[] agx = new float[NAG], agy = new float[NAG], agz = new float[NAG];
    final float[] agvx = new float[NAG], agvy = new float[NAG], agvz = new float[NAG], agT = new float[NAG];
    final boolean[] agDardo = new boolean[NAG];
    private final Mesh mAguijonero;
    /** seconds before a new swarm may appear (after one is wiped out or leaves) */
    private float agEspera;

    // lightning jellies
    static final int NMR = 2;
    final boolean[] mrVivo = new boolean[NMR];
    final float[] mrx = new float[NMR], mry = new float[NMR], mrz = new float[NMR];
    final float[] mrCarga = new float[NMR], mrEspera = new float[NMR], mrFase = new float[NMR];
    final int[] mrVida = new int[NMR];
    private final Mesh[] mMedusaRayo;
    /** flash of a discharge (seconds), for the arcs drawn on screen */
    float descarga;

    // canyon screecher
    boolean chVivo;
    float chx, chy, chz, chvx, chvy, chvz, chT, chRumbo;
    int chEstado; // 0 perched, 1 screeching, 2 swooping, 3 leaving
    int chVida;
    private final Mesh[] mChillon;

    // abyss prowlers
    static final int NRO = 2;
    final boolean[] roVivo = new boolean[NRO];
    final float[] rox = new float[NRO], roy = new float[NRO], roz = new float[NRO], roT = new float[NRO],
            roRumbo = new float[NRO];
    final int[] roVida = new int[NRO];
    private final Mesh[] mRondador;

    int dibujados;
    private final float[] punto = new float[2];

    /** test builds only (DESFILE=2) */
    void dibujarDesfile(Graphics3D g3d, float px, float py, float pz) {
        Mesh[][] g = { { mAguijonero }, mMedusaRayo, mChillon, mRondador };
        for (int i = 0; i < g.length; i++) {
            t.setIdentity();
            t.postTranslate(px + (i - 1.5f) * 2.6f, py - 5.2f, pz - 9);
            t.postRotate(90, 0, 1, 0);
            for (int k = 0; k < g[i].length; k++) {
                g3d.render(g[i][k], t);
            }
        }
    }

    private static Mesh[] pp(String nombre, Fog fog, int piezas, int alpha) {
        return ModeloM3G.partes("/modelos/" + nombre + ".m3g", fog, piezas, alpha, true);
    }

    Hostiles(Fog fog) {
        // Blender models (assets/modelos); the code-built ones only if a file is missing
        Mesh[] m;
        mAguijonero = (m = pp("aguijonero", fog, 1, 255)) != null ? m[0] : Modelos.aguijonero(fog);
        mMedusaRayo = (m = pp("medusa_rayo", fog, 2, 200)) != null ? m : Modelos.medusaRayo(fog);
        mChillon = (m = pp("chillon", fog, 3, 255)) != null ? m : Modelos.chillon(fog);
        mRondador = (m = pp("rondador", fog, 2, 255)) != null ? m : Modelos.rondador(fog);
    }

    private float azar() {
        semilla = semilla * 1103515245 + 12345;
        return ((semilla >>> 8) & 0xffff) / 65535f;
    }

    private boolean puntoEn(float px, float pz, float dmin, float dmax, int bioA, int bioB) {
        for (int intento = 0; intento < 4; intento++) {
            float a = azar() * 6.2832f;
            float d = dmin + azar() * (dmax - dmin);
            float x = px + FMath.cos(a) * d, z = pz + FMath.sin(a) * d;
            int b = Terreno.bioma(x, z);
            if (b == bioA || b == bioB) {
                punto[0] = x;
                punto[1] = z;
                return true;
            }
        }
        return false;
    }

    private final float[] tmp = new float[3];

    void actualizar(float dt, Partida p) {
        tiempo += dt;
        float px = p.px, py = p.py, pz = p.pz;
        int bio = Terreno.bioma(px, pz);
        boolean bajo = py < -3;
        if (descarga > 0) {
            descarga -= dt;
        }
        actualizarAguijoneros(dt, p, px, py, pz, bio, bajo);
        actualizarMedusas(dt, p, px, py, pz, bio, bajo);
        actualizarChillon(dt, p, px, py, pz, bio, bajo);
        actualizarRondadores(dt, p, px, py, pz, bio, bajo);
    }

    // ------------------------------------------------------------------ needlefish

    /** the swarm's home in the shallows and how long it has been chasing */
    private float agHx, agHz, agCaza;

    private void actualizarAguijoneros(float dt, Partida p, float px, float py, float pz, int bio, boolean bajo) {
        boolean hay = false;
        for (int i = 0; i < NAG; i++) {
            hay |= agVivo[i];
        }
        if (agEspera > 0) {
            agEspera -= dt;
        }
        if (!hay) {
            if (agEspera <= 0 && Peces.noche && (bio == Terreno.BAJIOS || bio == Terreno.PRADERA) && azar() < 0.006f
                    && puntoEn(px, pz, 30, 45, bio, bio)) {
                float y = Math.min(-2, Terreno.altura(punto[0], punto[1]) + 3);
                for (int i = 0; i < NAG; i++) {
                    agVivo[i] = true;
                    agx[i] = punto[0] + (i - 1) * 1.5f;
                    agz[i] = punto[1] + (i % 2) * 1.2f;
                    agy[i] = y + i * 0.4f;
                    agT[i] = 1 + i * 0.7f;
                    agDardo[i] = false;
                }
                agHx = punto[0];
                agHz = punto[1];
                agCaza = 0;
                Log.d("aguijoneros aparecen (noche, bajios)");
                agEspera = 75; // the next swarm only after a while
            }
            return;
        }
        for (int i = 0; i < NAG; i++) {
            if (!agVivo[i]) {
                continue;
            }
            float ox = px - agx[i], oy = py - agy[i], oz = pz - agz[i];
            float d = FMath.sqrt(ox * ox + oy * oy + oz * oz);
            // a territorial swarm: it gives up when you leave its patch of shallows,
            // or after chasing you for a while, and swims back home
            float hx = agHx - agx[i], hz = agHz - agz[i];
            float dCasa = FMath.sqrt(hx * hx + hz * hz);
            float ph = px - agHx, pzh = pz - agHz;
            boolean fuera = ph * ph + pzh * pzh > 35 * 35 || (bio != Terreno.BAJIOS && bio != Terreno.PRADERA)
                    || agCaza > 50;
            if (d > 55 || !Peces.noche) {
                agVivo[i] = false;
                continue;
            }
            if (fuera) {
                agDardo[i] = false;
                float k = Math.min(1, dt * 2);
                float hl = Math.max(0.1f, dCasa);
                agvx[i] += (hx / hl * 5 - agvx[i]) * k;
                agvz[i] += (hz / hl * 5 - agvz[i]) * k;
                agvy[i] *= 1 - k;
                agx[i] += agvx[i] * dt;
                agz[i] += agvz[i] * dt;
                if (dCasa < 3) {
                    agVivo[i] = false; // back in its hiding place
                }
                continue;
            }
            if (i == 0) {
                agCaza += dt;
            }
            agT[i] -= dt;
            if (agDardo[i]) {
                // the dart: straight and fast; a sting on contact
                if (d < (p.enRemora ? 2.2f : 0.8f)) {
                    if (!p.enRemora) {
                        p.envenenar(6); // the hull keeps the poison out
                    }
                    p.danar(3, p.enRemora ? "Un aguijonero choca contra el casco" : "Te pico un aguijonero: veneno");
                    agDardo[i] = false;
                    agT[i] = 2.5f;
                } else if (agT[i] <= 0) {
                    agDardo[i] = false;
                    agT[i] = 1.5f + azar();
                }
            } else {
                // circling at a distance, then a dart when its turn comes
                float a = tiempo * 0.9f + i * 2.1f;
                float tx = px + FMath.cos(a) * 7 - agx[i], ty = py + FMath.sin(a * 1.3f) - agy[i],
                        tz = pz + FMath.sin(a) * 7 - agz[i];
                float tl = Math.max(0.1f, FMath.sqrt(tx * tx + ty * ty + tz * tz));
                float k = Math.min(1, dt * 2);
                agvx[i] += (tx / tl * 4 - agvx[i]) * k;
                agvy[i] += (ty / tl * 4 - agvy[i]) * k;
                agvz[i] += (tz / tl * 4 - agvz[i]) * k;
                if (bajo && d < 14 && agT[i] <= 0) {
                    agDardo[i] = true;
                    agT[i] = 1.2f;
                    agvx[i] = ox / d * 16;
                    agvy[i] = oy / d * 16;
                    agvz[i] = oz / d * 16;
                    Sonido.tocar(Sonido.TAJO);
                }
            }
            agx[i] += agvx[i] * dt;
            agy[i] += agvy[i] * dt;
            agz[i] += agvz[i] * dt;
            tmp[0] = agx[i];
            tmp[1] = agy[i];
            tmp[2] = agz[i];
            if (p.mundo.empujarCerca(tmp, 0.4f)) {
                agx[i] = tmp[0];
                agy[i] = tmp[1];
                agz[i] = tmp[2];
            }
            float suelo = Terreno.altura(agx[i], agz[i]) + 0.5f;
            agy[i] = Math.max(suelo, Math.min(-1.5f, agy[i]));
        }
    }

    // ------------------------------------------------------------------ lightning jellies

    private void actualizarMedusas(float dt, Partida p, float px, float py, float pz, int bio, boolean bajo) {
        for (int i = 0; i < NMR; i++) {
            if (!mrVivo[i]) {
                boolean suya = bio == Terreno.BOSQUE || bio == Terreno.PRADERA;
                if (suya && azar() < 0.004f && puntoEn(px, pz, 30, 55, Terreno.BOSQUE, Terreno.PRADERA)) {
                    mrVivo[i] = true;
                    mrx[i] = punto[0];
                    mrz[i] = punto[1];
                    mry[i] = Math.min(-4, Terreno.altura(mrx[i], mrz[i]) + 5 + azar() * 6);
                    mrCarga[i] = 0;
                    mrEspera[i] = 0;
                    mrVida[i] = 2;
                    Log.d("medusa rayo aparece en " + (int) mrx[i] + "," + (int) mry[i] + "," + (int) mrz[i]);
                }
                continue;
            }
            float ox = px - mrx[i], oy = py - mry[i], oz = pz - mrz[i];
            float d2 = ox * ox + oy * oy + oz * oz;
            if (d2 > 120 * 120) {
                mrVivo[i] = false;
                continue;
            }
            mrFase[i] += dt;
            // a slow drift
            mrx[i] += FMath.sin(mrFase[i] * 0.3f + i) * 0.3f * dt;
            mrz[i] += FMath.cos(mrFase[i] * 0.25f + i) * 0.3f * dt;
            mry[i] += FMath.sin(mrFase[i] * 0.8f) * 0.25f * dt;
            if (mrEspera[i] > 0) {
                mrEspera[i] -= dt;
                mrCarga[i] = 0;
            } else if (bajo && d2 < 6 * 6) {
                // the diver is close: it charges up (it glows brighter), then discharges
                mrCarga[i] += dt;
                if (mrCarga[i] > 1.4f) {
                    mrCarga[i] = 0;
                    mrEspera[i] = 6;
                    descarga = 0.4f;
                    Sonido.tocar(Sonido.DESCARGA);
                    if (d2 < 6.5f * 6.5f) {
                        p.aturdir(3);
                        p.danar(8, "Descarga de una medusa rayo: aturdido");
                    }
                }
            } else {
                mrCarga[i] = Math.max(0, mrCarga[i] - dt);
            }
        }
    }

    // ------------------------------------------------------------------ canyon screecher

    private void actualizarChillon(float dt, Partida p, float px, float py, float pz, int bio, boolean bajo) {
        if (!chVivo) {
            if (bio == Terreno.CANONES && azar() < 0.004f && puntoEn(px, pz, 40, 60, Terreno.CANONES, Terreno.CANONES)) {
                chVivo = true;
                chx = punto[0];
                chz = punto[1];
                chy = Terreno.altura(chx, chz) + 1.5f;
                chEstado = 0;
                chVida = 3;
                chRumbo = azar() * 360;
                Log.d("chillon de canon aparece en " + (int) chx + "," + (int) chy + "," + (int) chz);
            }
            return;
        }
        float ox = px - chx, oy = py - chy, oz = pz - chz;
        float d = FMath.sqrt(ox * ox + oy * oy + oz * oz);
        if (d > 140) {
            chVivo = false;
            return;
        }
        chT -= dt;
        float vel = 0, tx = 0, ty = 0, tz = 0;
        switch (chEstado) {
        case 0: // perched: it waits until the diver is in sight
            if (bajo && d < 25) {
                chEstado = 1;
                chT = 1.0f;
                Sonido.tocar(Sonido.CHILLIDO);
                p.cegar(p.enRemora ? 1.5f : 4);
                p.avisar("Un chillido te desorienta!");
            }
            break;
        case 1: // screeching
            if (chT <= 0) {
                chEstado = 2;
                chT = 6;
            }
            break;
        case 2: // swooping at the diver
            vel = 9;
            tx = ox;
            ty = oy;
            tz = oz;
            if (d < 1.8f) {
                p.danar(10, "Te mordió un chillón de cañón");
                chEstado = 3;
                chT = 5;
            } else if (chT <= 0) {
                chEstado = 3;
                chT = 4;
            }
            break;
        default: // leaving, then perches again further on
            vel = 6;
            tx = -ox;
            ty = 0.5f;
            tz = -oz;
            if (chT <= 0) {
                chEstado = 0;
                chy = Terreno.altura(chx, chz) + 1.5f;
            }
            break;
        }
        if (vel > 0) {
            float tl = Math.max(0.01f, FMath.sqrt(tx * tx + ty * ty + tz * tz));
            float k = Math.min(1, dt * 2.5f);
            chvx += (tx / tl * vel - chvx) * k;
            chvy += (ty / tl * vel - chvy) * k;
            chvz += (tz / tl * vel - chvz) * k;
            chRumbo = FMath.atan2(chvx, chvz) * 57.29578f;
        } else {
            chvx = chvy = chvz = 0;
            chRumbo += Peces.angulo(FMath.atan2(ox, oz) * 57.29578f - chRumbo) * Math.min(1, dt * 2);
        }
        chx += chvx * dt;
        chy += chvy * dt;
        chz += chvz * dt;
        tmp[0] = chx;
        tmp[1] = chy;
        tmp[2] = chz;
        if (p.mundo.empujarCerca(tmp, 0.6f)) {
            chx = tmp[0];
            chy = tmp[1];
            chz = tmp[2];
        }
        chy = Math.max(Terreno.altura(chx, chz) + 0.8f, Math.min(-3, chy));
    }

    // ------------------------------------------------------------------ abyss prowlers

    private void actualizarRondadores(float dt, Partida p, float px, float py, float pz, int bio, boolean bajo) {
        for (int i = 0; i < NRO; i++) {
            if (!roVivo[i]) {
                if (bio == Terreno.ABISMO && azar() < 0.005f && puntoEn(px, pz, 25, 45, Terreno.ABISMO, Terreno.ABISMO)) {
                    roVivo[i] = true;
                    rox[i] = punto[0];
                    roz[i] = punto[1];
                    roy[i] = Terreno.altura(rox[i], roz[i]) + 2;
                    roT[i] = 0;
                    roVida[i] = 3;
                    roRumbo[i] = azar() * 360;
                    Log.d("rondador abisal acecha en " + (int) rox[i] + "," + (int) roy[i] + "," + (int) roz[i]);
                }
                continue;
            }
            float ox = px - rox[i], oy = py - roy[i], oz = pz - roz[i];
            float d = FMath.sqrt(ox * ox + oy * oy + oz * oz);
            if (d > 100) {
                roVivo[i] = false;
                continue;
            }
            roT[i] -= dt;
            roRumbo[i] += Peces.angulo(FMath.atan2(ox, oz) * 57.29578f - roRumbo[i]) * Math.min(1, dt * 0.8f);
            if (p.depredadores.cercaVelo(rox[i], roy[i], roz[i])) {
                // the veil's glow keeps the prowlers off: they back away into the dark
                float hl = Math.max(0.1f, d);
                rox[i] -= ox / hl * 3 * dt;
                roy[i] -= oy / hl * 3 * dt;
                roz[i] -= oz / hl * 3 * dt;
                roT[i] = Math.max(roT[i], 1);
            }
            if (bajo && d < 8 && roT[i] <= 0) {
                // the lunge: a short, very fast snap
                float paso = Math.min(d - 1.2f, 10 * dt);
                rox[i] += ox / d * paso;
                roy[i] += oy / d * paso;
                roz[i] += oz / d * paso;
            tmp[0] = rox[i];
            tmp[1] = roy[i];
            tmp[2] = roz[i];
            if (p.mundo.empujarCerca(tmp, 0.7f)) {
                rox[i] = tmp[0];
                roy[i] = tmp[1];
                roz[i] = tmp[2];
            }
                if (d < 1.9f) {
                    p.danar(12, "Te mordió un rondador abisal");
                    p.robarOxigeno(15);
                    roT[i] = 5;
                }
            }
        }
    }

    // ------------------------------------------------------------------ combat, collisions

    /** the Remora's defence pulse: everything hostile within r metres */
    int pulso(float px, float py, float pz, float r) {
        int n = 0;
        float r2 = r * r;
        for (int i = 0; i < NAG; i++) {
            float ox = agx[i] - px, oy = agy[i] - py, oz = agz[i] - pz;
            if (agVivo[i] && ox * ox + oy * oy + oz * oz < r2) {
                agVivo[i] = false; // small fish do not survive the shock
                n++;
            }
        }
        for (int i = 0; i < NMR; i++) {
            float ox = mrx[i] - px, oy = mry[i] - py, oz = mrz[i] - pz;
            if (mrVivo[i] && ox * ox + oy * oy + oz * oz < r2) {
                mrCarga[i] = 0;
                mrEspera[i] = 10; // drained: it cannot discharge for a while
                if (--mrVida[i] <= 0) {
                    mrVivo[i] = false;
                }
                n++;
            }
        }
        float ox = chx - px, oy = chy - py, oz = chz - pz;
        if (chVivo && ox * ox + oy * oy + oz * oz < r2) {
            chEstado = 3;
            chT = 8;
            if (--chVida <= 0) {
                chVivo = false;
            }
            n++;
        }
        for (int i = 0; i < NRO; i++) {
            float qx = rox[i] - px, qy = roy[i] - py, qz = roz[i] - pz;
            if (roVivo[i] && qx * qx + qy * qy + qz * qz < r2) {
                roT[i] = 8;
                if (--roVida[i] <= 0) {
                    roVivo[i] = false;
                }
                n++;
            }
        }
        return n;
    }

    /** knife hit: returns a message, or null if nothing was in reach */
    boolean aDistancia;

    String golpear(Partida p, float px, float py, float pz, float fx, float fy, float fz) {
        for (int i = 0; i < NAG; i++) {
            if (agVivo[i] && delante(agx[i] - px, agy[i] - py, agz[i] - pz, fx, fy, fz, 2.2f)) {
                agVivo[i] = false;
                return "Abatiste un aguijonero";
            }
        }
        for (int i = 0; i < NMR; i++) {
            if (mrVivo[i] && delante(mrx[i] - px, mry[i] - py, mrz[i] - pz, fx, fy, fz, 2.4f)) {
                // touching it with a metal blade: a shock back
                if (!aDistancia) {
                    p.aturdir(1.5f);
                }
                if (--mrVida[i] <= 0) {
                    mrVivo[i] = false;
                    return "Cortaste la medusa rayo, pero te dio calambre";
                }
                return "La medusa rayo chisporrotea al cortarla";
            }
        }
        if (chVivo && delante(chx - px, chy - py, chz - pz, fx, fy, fz, 2.6f)) {
            if (--chVida <= 0) {
                chVivo = false;
                return "Abatiste al chillón de cañón";
            }
            chEstado = 3;
            chT = 6;
            return "Cuchillazo! El chillón huye";
        }
        for (int i = 0; i < NRO; i++) {
            if (roVivo[i] && delante(rox[i] - px, roy[i] - py, roz[i] - pz, fx, fy, fz, 2.6f)) {
                if (--roVida[i] <= 0) {
                    roVivo[i] = false;
                    return "Abatiste al rondador abisal";
                }
                roT[i] = 3;
                return "Cuchillazo! El rondador se repliega";
            }
        }
        return null;
    }

    boolean alAlcance(float px, float py, float pz, float fx, float fy, float fz) {
        for (int i = 0; i < NAG; i++) {
            if (agVivo[i] && delante(agx[i] - px, agy[i] - py, agz[i] - pz, fx, fy, fz, 2.2f)) {
                return true;
            }
        }
        for (int i = 0; i < NMR; i++) {
            if (mrVivo[i] && delante(mrx[i] - px, mry[i] - py, mrz[i] - pz, fx, fy, fz, 2.4f)) {
                return true;
            }
        }
        if (chVivo && delante(chx - px, chy - py, chz - pz, fx, fy, fz, 2.6f)) {
            return true;
        }
        for (int i = 0; i < NRO; i++) {
            if (roVivo[i] && delante(rox[i] - px, roy[i] - py, roz[i] - pz, fx, fy, fz, 2.6f)) {
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

    void empujar(float[] q, float rj) {
        for (int i = 0; i < NMR; i++) {
            if (mrVivo[i]) {
                Depredadores.empujarEsfera(q, mrx[i], mry[i], mrz[i], 0.6f + rj);
            }
        }
        if (chVivo) {
            Depredadores.empujarEsfera(q, chx, chy, chz, 0.6f + rj);
        }
        // the stingers swarm: without this they flew straight through the diver
        for (int i = 0; i < NAG; i++) {
            if (agVivo[i]) {
                Depredadores.empujarEsfera(q, agx[i], agy[i], agz[i], 0.5f + rj);
            }
        }
        for (int i = 0; i < NRO; i++) {
            if (roVivo[i]) {
                Depredadores.empujarEsfera(q, rox[i], roy[i], roz[i], 0.7f + rj);
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    void dibujar(Graphics3D g3d, float px, float pz, float fx, float fz, float lejos) {
        dibujados = 0;
        for (int i = 0; i < NAG; i++) {
            if (!agVivo[i] || !visible(agx[i] - px, agz[i] - pz, fx, fz, lejos)) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(agx[i], agy[i], agz[i]);
            float h = FMath.sqrt(agvx[i] * agvx[i] + agvz[i] * agvz[i]);
            t.postRotate(FMath.atan2(agvx[i], agvz[i]) * 57.29578f, 0, 1, 0);
            t.postRotate(-FMath.atan2(agvy[i], Math.max(0.01f, h)) * 57.29578f, 1, 0, 0);
            t.postRotate(FMath.sin(tiempo * 14 + i) * 8, 0, 1, 0);
            g3d.render(mAguijonero, t);
            dibujados++;
        }
        for (int i = 0; i < NMR; i++) {
            if (!mrVivo[i] || !visible(mrx[i] - px, mrz[i] - pz, fx, fz, lejos)) {
                continue;
            }
            float f = mrFase[i];
            float pulso = FMath.sin(f * 2);
            t.setIdentity();
            t.postTranslate(mrx[i], mry[i], mrz[i]);
            t.postRotate(f * 15, 0, 1, 0);
            tp.set(t);
            // charging: the bell trembles and swells
            float carga = mrCarga[i] / 1.4f;
            float temblor = carga > 0 ? FMath.sin(tiempo * 40) * 0.05f * carga : 0;
            t.postScale(1 + 0.1f * pulso + 0.25f * carga + temblor, 1 - 0.12f * pulso, 1 + 0.1f * pulso + 0.25f * carga);
            g3d.render(mMedusaRayo[1], t);
            tp.postRotate(FMath.sin(f * 2 - 1.2f) * 12, 1, 0, 0);
            tp.postRotate(FMath.cos(f * 1.5f) * 10, 0, 0, 1);
            g3d.render(mMedusaRayo[0], tp);
            dibujados++;
        }
        if (chVivo && visible(chx - px, chz - pz, fx, fz, lejos)) {
            t.setIdentity();
            t.postTranslate(chx, chy, chz);
            t.postRotate(chRumbo, 0, 1, 0);
            boolean vuela = chEstado >= 2;
            float aleteo = vuela ? FMath.sin(tiempo * 7) * 30 : (chEstado == 1 ? FMath.sin(tiempo * 30) * 8 : 15);
            if (chEstado == 1) {
                t.postRotate(-20, 1, 0, 0); // rearing up to shriek
            }
            g3d.render(mChillon[2], t);
            tp.set(t);
            tp.postTranslate(0.3f, 0, 0);
            tp.postRotate(aleteo, 0, 0, 1);
            tp.postTranslate(-0.3f, 0, 0);
            g3d.render(mChillon[0], tp);
            tp.set(t);
            tp.postTranslate(-0.3f, 0, 0);
            tp.postRotate(-aleteo, 0, 0, 1);
            tp.postTranslate(0.3f, 0, 0);
            g3d.render(mChillon[1], tp);
            dibujados++;
        }
        for (int i = 0; i < NRO; i++) {
            if (!roVivo[i] || !visible(rox[i] - px, roz[i] - pz, fx, fz, lejos)) {
                continue;
            }
            t.setIdentity();
            t.postTranslate(rox[i], roy[i] + FMath.sin(tiempo * 0.7f + i) * 0.15f, roz[i]);
            t.postRotate(roRumbo[i], 0, 1, 0);
            g3d.render(mRondador[1], t);
            // the lure sways slowly like a bait
            tp.set(t);
            tp.postTranslate(0, 0.45f, 0.2f);
            tp.postRotate(FMath.sin(tiempo * 1.2f + i) * 15, 0, 1, 0);
            tp.postRotate(FMath.sin(tiempo * 0.9f + i) * 10, 1, 0, 0);
            tp.postTranslate(0, -0.45f, -0.2f);
            g3d.render(mRondador[0], tp);
            dibujados++;
        }
    }

    private static boolean visible(float ox, float oz, float fx, float fz, float lejos) {
        float d2 = ox * ox + oz * oz;
        return d2 < (lejos + 3) * (lejos + 3) && (d2 < 9 || ox * fx + oz * fz > -3);
    }

    /** gallery: 0 needlefish, 1 lightning jelly, 2 screecher, 3 prowler */
    void dibujarMuestra(Graphics3D g3d, Transform base, int cual, float reloj) {
        tiempo = reloj;
        if (cual == 0) {
            t.set(base);
            t.postRotate(FMath.sin(reloj * 14) * 8, 0, 1, 0);
            g3d.render(mAguijonero, t);
        } else if (cual == 1) {
            mrVivo[0] = true;
            mrx[0] = mry[0] = mrz[0] = 0;
            mrFase[0] = reloj;
            dibujarUno(g3d, base, 1);
        } else if (cual == 2) {
            chEstado = ((int) reloj / 3) % 3;
            dibujarUno(g3d, base, 2);
        } else {
            dibujarUno(g3d, base, 3);
        }
    }

    private void dibujarUno(Graphics3D g3d, Transform base, int cual) {
        t.set(base);
        if (cual == 1) {
            float f = mrFase[0];
            float pulso = FMath.sin(f * 2);
            tp.set(t);
            t.postScale(1 + 0.1f * pulso, 1 - 0.12f * pulso, 1 + 0.1f * pulso);
            g3d.render(mMedusaRayo[1], t);
            tp.postRotate(FMath.sin(f * 2 - 1.2f) * 12, 1, 0, 0);
            g3d.render(mMedusaRayo[0], tp);
        } else if (cual == 2) {
            float aleteo = FMath.sin(tiempo * 7) * 30;
            g3d.render(mChillon[2], t);
            tp.set(t);
            tp.postTranslate(0.3f, 0, 0);
            tp.postRotate(aleteo, 0, 0, 1);
            tp.postTranslate(-0.3f, 0, 0);
            g3d.render(mChillon[0], tp);
            tp.set(t);
            tp.postTranslate(-0.3f, 0, 0);
            tp.postRotate(-aleteo, 0, 0, 1);
            tp.postTranslate(0.3f, 0, 0);
            g3d.render(mChillon[1], tp);
        } else {
            g3d.render(mRondador[1], t);
            tp.set(t);
            tp.postTranslate(0, 0.45f, 0.2f);
            tp.postRotate(FMath.sin(tiempo * 1.2f) * 15, 0, 1, 0);
            tp.postTranslate(0, -0.45f, -0.2f);
            g3d.render(mRondador[0], tp);
        }
    }
}
