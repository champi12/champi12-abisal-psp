package abisal;

/**
 * devlog: every line goes to System.out, which the runtime writes to the console and to
 * devlog.txt next to the EBOOT (opened and closed per line, so it survives a crash).
 */
public final class Log {
    private static final long T0 = System.currentTimeMillis();

    private Log() {
    }

    // watchdog phases (names in runtime/platform/psp/plat_psp.c)
    public static final int F_LOGICA = 1, F_3D = 2, F_2D = 3, F_PRESENTAR = 4, F_TROZO = 5, F_SONIDO = 6,
            F_GUARDAR = 7, F_TITULO = 8, F_CARGAR = 9;
    static int faseActual;

    /** tell the watchdog what the game is doing; returns the previous phase */
    public static int fase(int f) {
        int antes = faseActual;
        faseActual = f;
        j2me.Pad.fase(f);
        return antes;
    }

    public static void d(String msg) {
        long t = System.currentTimeMillis() - T0;
        StringBuffer sb = new StringBuffer(msg.length() + 16);
        sb.append("[J ");
        sb.append(t / 1000);
        sb.append('.');
        long ms = t % 1000;
        if (ms < 100) {
            sb.append('0');
        }
        if (ms < 10) {
            sb.append('0');
        }
        sb.append(ms);
        sb.append("] ");
        sb.append(msg);
        System.out.println(sb.toString());
    }

    /** live Java heap in KB (collects first: only for load-time measurements) */
    public static int vivo() {
        System.gc();
        Runtime r = Runtime.getRuntime();
        return (int) ((r.totalMemory() - r.freeMemory()) / 1024);
    }

    /** Java heap in use / total, in KB, plus free system memory */
    public static String memoria() {
        Runtime r = Runtime.getRuntime();
        long total = r.totalMemory() / 1024;
        long usada = total - r.freeMemory() / 1024;
        int sis = j2me.Pad.memLibre();
        return "heap " + usada + "/" + total + " KB (vertices " + javax.microedition.m3g.VertexBuffer.cacheKB() + " KB)"
                + (sis > 0 ? ", sistema libre " + sis + " KB" : "");
    }
}
