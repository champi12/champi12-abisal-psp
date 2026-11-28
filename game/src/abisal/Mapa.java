package abisal;

import javax.microedition.lcdui.Graphics;

/**
 * The PDA map ([] in the PDA): the crater seen from above, north up. Only the cells the
 * diver has been near are shown (fog of war); each shows the colour of its zone. Markers:
 * the diver (an arrow), the pod, the base, the parked Remora, the mission signal and the
 * places already found.
 */
final class Mapa {
    static final int N = 64;
    static final float MEDIO = 2750, CELDA = 2 * MEDIO / N;
    static final int PX = 3, TAM = N * PX;
    final boolean[] visto = new boolean[N * N];
    private final int[] color = new int[N * N];
    private int filasListas;
    private int ultimaCelda = -1;

    static int colorBioma(int b) {
        switch (b) {
        case Terreno.BAJIOS:
            return 0x3fa8a0;
        case Terreno.BOSQUE:
            return 0x2f7a4a;
        case Terreno.PRADERA:
            return 0xb0a060;
        case Terreno.MESETA:
            return 0xa04a32;
        case Terreno.CANONES:
            return 0x5a4a7a;
        case Terreno.TERMAL:
            return 0xc8602a;
        case Terreno.ABISMO:
            return 0x23356a;
        case Terreno.LLANURA:
            return 0x8d968f;
        case Terreno.CRISTAL:
            return 0x6ab8d8;
        case Terreno.OSARIO:
            return 0xc8c0a8;
        default:
            return 0x0c1628;
        }
    }

    /** zone colours are worked out a couple of rows per frame (no hitch when opening) */
    void preparar() {
        if (filasListas >= N) {
            return;
        }
        for (int q = 0; q < 2 && filasListas < N; q++) {
            int j = filasListas++;
            for (int i = 0; i < N; i++) {
                float x = -MEDIO + (i + 0.5f) * CELDA, z = -MEDIO + (j + 0.5f) * CELDA;
                color[j * N + i] = colorBioma(Terreno.bioma(x, z));
            }
        }
    }

    /** the diver is at (x, z): its cell and the ones around it are explored */
    void marcar(float x, float z) {
        int i = (int) ((x + MEDIO) / CELDA), j = (int) ((z + MEDIO) / CELDA);
        int c = j * N + i;
        if (c == ultimaCelda) {
            return;
        }
        ultimaCelda = c;
        for (int dj = -1; dj <= 1; dj++) {
            for (int di = -1; di <= 1; di++) {
                int a = i + di, b = j + dj;
                if (a >= 0 && a < N && b >= 0 && b < N) {
                    visto[b * N + a] = true;
                }
            }
        }
    }

    boolean vista(float x, float z) {
        int i = (int) ((x + MEDIO) / CELDA), j = (int) ((z + MEDIO) / CELDA);
        return i >= 0 && i < N && j >= 0 && j < N && visto[j * N + i];
    }

    int explorado() {
        int n = 0, total = 0;
        for (int k = 0; k < N * N; k++) {
            int i = k % N, j = k / N;
            float x = -MEDIO + (i + 0.5f) * CELDA, z = -MEDIO + (j + 0.5f) * CELDA;
            if (x * x + z * z < Terreno.BORDE * Terreno.BORDE) {
                total++;
                if (visto[k]) {
                    n++;
                }
            }
        }
        return total > 0 ? n * 100 / total : 0;
    }

    private int pct = -1;

    /** O, triangle or [] go back to the PDA */
    boolean actualizar(Entrada in) {
        if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO) || in.pulso(Entrada.CUADRADO)) {
            pct = -1;
            return false;
        }
        return true;
    }

    /** unexplored: a dim blue inside the crater, darker beyond its rim */
    private int niebla(int k) {
        return color[k] == 0x0c1628 ? 0x05090f : 0x122430;
    }

    private static int mx(float x) {
        return 20 + (int) ((x + MEDIO) / (2 * MEDIO) * TAM);
    }

    private static int my(float z) {
        return 40 + (int) ((z + MEDIO) / (2 * MEDIO) * TAM);
    }

    void dibujar(Graphics g, Partida p) {
        while (filasListas < N) {
            preparar();
        }
        if (pct < 0) {
            pct = explorado();
        }
        p.panelCaja(g, 10, 22, 460, 232);
        g.setColor(0x7fe8f4);
        g.drawString("MAPA DEL CRÁTER   explorado " + pct + "%", 20, 27, Graphics.TOP | Graphics.LEFT);
        // the cells, a row at a time in runs of one colour
        for (int j = 0; j < N; j++) {
            int i = 0;
            while (i < N) {
                int k = j * N + i;
                int c = visto[k] ? color[k] : niebla(k);
                int i0 = i;
                while (i < N && (visto[j * N + i] ? color[j * N + i] : niebla(j * N + i)) == c) {
                    i++;
                }
                g.setColor(c);
                g.fillRect(20 + i0 * PX, 40 + j * PX, (i - i0) * PX, PX);
            }
        }
        g.setColor(0x2fb3c0);
        g.drawRect(19, 39, TAM + 1, TAM + 1);
        // places found so far
        Historia h = p.historia;
        g.setColor(0xffb060);
        for (int i = 0; i < Historia.LUGARES; i++) {
            if (h.tomado[i] || vista(h.lx[i], h.lz[i])) {
                g.fillRect(mx(h.lx[i]) - 1, my(h.lz[i]) - 1, 3, 3);
            }
        }
        g.setColor(0xc080ff);
        for (int i = 0; i < Ecos.N; i++) {
            if (p.ecos.hallado[i]) {
                g.drawRect(mx(p.ecos.x[i]) - 1, my(p.ecos.z[i]) - 1, 2, 2);
            }
        }
        for (int i = 0; i < Partida.NSENAL; i++) {
            if (p.sbPuesta[i]) {
                g.setColor(Partida.COLOR_SENAL);
                g.drawRect(mx(p.sbX[i]) - 2, my(p.sbZ[i]) - 2, 4, 4);
                g.drawString("" + (i + 1), mx(p.sbX[i]) + 4, my(p.sbZ[i]) - 5, Graphics.TOP | Graphics.LEFT);
            }
        }
        if (p.hayBase) {
            g.setColor(0xffa0ff);
            g.fillRect(mx(p.baseX) - 2, my(p.baseZ) - 2, 5, 5);
        }
        if (p.inv.tiene[Inventario.H_REMORA] && !p.enRemora) {
            g.setColor(0x5ff0ff);
            g.fillRect(mx(p.rx) - 2, my(p.rz) - 2, 5, 5);
        }
        g.setColor(0xffffff);
        g.fillRect(mx(0) - 2, my(0) - 2, 5, 5);
        if (h.objetivo() >= 0 && ((int) (p.tiempo * 3) & 1) == 0) {
            int sx = mx(h.objX()), sy = my(h.objZ());
            g.setColor(0xffe040);
            g.drawRect(sx - 3, sy - 3, 6, 6);
        }
        // the diver: an arrow pointing where it looks
        float a = p.yaw * j2me.FMath.RAD;
        float fx = -j2me.FMath.sin(a), fz = -j2me.FMath.cos(a);
        int cx = mx(p.px), cy = my(p.pz);
        g.setColor(0xff5a3a);
        g.fillTriangle(cx + (int) (fx * 6), cy + (int) (fz * 6), cx + (int) (-fz * 3 - fx * 3),
                cy + (int) (fx * 3 - fz * 3), cx + (int) (fz * 3 - fx * 3), cy + (int) (-fx * 3 - fz * 3));
        // legend
        int lx = 230, ly = 44;
        leyenda(g, lx, ly, 0xffffff, "Módulo");
        leyenda(g, lx, ly + 14, 0xff5a3a, "Tu");
        leyenda(g, lx, ly + 28, 0x5ff0ff, "Rémora");
        leyenda(g, lx, ly + 42, 0xffa0ff, "Base");
        leyenda(g, lx, ly + 56, 0xffe040, "Señal de la misión");
        leyenda(g, lx, ly + 70, 0xffb060, "Lugares encontrados");
        leyenda(g, lx, ly + 84, Partida.COLOR_SENAL, "Tus balizas de señal");
        int[] zonas = { Terreno.BAJIOS, Terreno.BOSQUE, Terreno.PRADERA, Terreno.MESETA, Terreno.CANONES,
                Terreno.TERMAL, Terreno.ABISMO };
        for (int i = 0; i < zonas.length; i++) {
            leyenda(g, lx + (i % 2) * 112, ly + 104 + (i / 2) * 14, colorBioma(zonas[i]), Mundo.nombreBioma(zonas[i]));
        }
        g.setColor(0x9fc4cc);
        g.drawString("Zona: " + Mundo.nombreBioma(Terreno.bioma(p.px, p.pz)) + "   " + (int) -p.py + " m", lx, ly + 164,
                Graphics.TOP | Graphics.LEFT);
        g.drawString("Norte arriba     O volver", lx, ly + 180, Graphics.TOP | Graphics.LEFT);
    }

    private static void leyenda(Graphics g, int x, int y, int c, String s) {
        g.setColor(c);
        g.fillRect(x, y + 2, 7, 7);
        g.setColor(0xd8ecf0);
        g.drawString(s, x + 12, y, Graphics.TOP | Graphics.LEFT);
    }
}
