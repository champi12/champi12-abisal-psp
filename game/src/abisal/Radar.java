package abisal;

import javax.microedition.lcdui.Graphics;

/**
 * Sonar / radar display: a disc with the view direction up. Blips: predators (red), the
 * Ash Colossus (big red), peaceful fauna (green), the escape pod (white), the parked
 * Remora (cyan) and the mission signal (yellow, pinned to the rim when out of range).
 * A sweep line turns; blips fade after it passes, like a real sonar.
 */
final class Radar {
    private Radar() {
    }

    static void dibujar(Graphics g, Partida p, int cx, int cy, int r, float rango) {
        g.setColor(0x0a2a24);
        g.fillArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);
        g.setColor(0x1f6a52);
        g.drawArc(cx - r / 2, cy - r / 2, r, r, 0, 360);
        g.setColor(0x3fdc9a);
        g.drawArc(cx - r, cy - r, 2 * r, 2 * r, 0, 360);
        // sweep: one turn every 2 s
        float barrido = (p.tiempo * 180) % 360;
        float ba = barrido * j2me.FMath.RAD;
        g.drawLine(cx, cy, cx + (int) (j2me.FMath.sin(ba) * r), cy - (int) (j2me.FMath.cos(ba) * r));
        float sy = j2me.FMath.sin(p.yaw * j2me.FMath.RAD), cyw = j2me.FMath.cos(p.yaw * j2me.FMath.RAD);
        Depredadores d = p.depredadores;
        for (int i = 0; i < Depredadores.ND; i++) {
            if (d.dVivo[i]) {
                punto(g, p, cx, cy, r, rango, d.dx[i], d.dz[i], 0xff4a3a, 2, sy, cyw, barrido, false);
            }
        }
        for (int i = 0; i < Depredadores.NA; i++) {
            if (d.aVivo[i]) {
                punto(g, p, cx, cy, r, rango, d.ax[i], d.az[i], 0xff8a3a, 2, sy, cyw, barrido, false);
            }
        }
        if (d.cVivo) {
            punto(g, p, cx, cy, r, rango, d.cx, d.cz, 0xff2020, 4, sy, cyw, barrido, true);
        }
        Peces pc = p.peces;
        for (int i = 0; i < Peces.N; i++) {
            if (pc.vivo[i]) {
                punto(g, p, cx, cy, r, rango, pc.x[i], pc.z[i], 0x6aff8a, 1, sy, cyw, barrido, false);
            }
        }
        punto(g, p, cx, cy, r, rango, 0, 0, 0xffffff, 2, sy, cyw, barrido, true);
        if (p.inv.tiene[Inventario.H_REMORA] && !p.enRemora) {
            punto(g, p, cx, cy, r, rango, p.rx, p.rz, 0x5ff0ff, 2, sy, cyw, barrido, true);
        }
        if (p.historia.objetivo() >= 0) {
            punto(g, p, cx, cy, r, rango, p.historia.objX(), p.historia.objZ(), 0xffe040, 2, sy, cyw, barrido, true);
        }
        // the diver
        g.setColor(0xffffff);
        g.fillRect(cx - 1, cy - 1, 3, 3);
    }

    /**
     * One blip at world (x, z). "fijo" blips (pod, Remora, signal) are always shown and
     * pinned to the rim when out of range; creature blips only within range, brightest
     * right after the sweep passes.
     */
    private static void punto(Graphics g, Partida p, int cx, int cy, int r, float rango, float x, float z, int color,
            int tam, float sy, float cyw, float barrido, boolean fijo) {
        float ox = x - p.px, oz = z - p.pz;
        // view frame: forward (-sin yaw, -cos yaw), right (cos yaw, -sin yaw)
        float ad = -(ox * sy + oz * cyw);
        float de = ox * cyw - oz * sy;
        float d = j2me.FMath.sqrt(ad * ad + de * de);
        if (d > rango) {
            if (!fijo) {
                return;
            }
            ad = ad / d * rango;
            de = de / d * rango;
        }
        int bx = cx + (int) (de / rango * (r - 2));
        int by = cy - (int) (ad / rango * (r - 2));
        if (!fijo) {
            // fade with the time since the sweep went over the blip
            float ang = j2me.FMath.atan2(de, ad) * 57.29578f;
            float atras = (barrido - ang + 720) % 360;
            if (atras > 300) {
                return;
            }
            color = Terreno.mezcla(color, 0x0a2a24, atras / 300);
        }
        g.setColor(color);
        g.fillRect(bx - tam / 2, by - tam / 2, tam + 1, tam + 1);
    }
}
