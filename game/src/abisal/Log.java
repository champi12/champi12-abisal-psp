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

    /**
     * The devlog is only active on the test build. Building the public release (PRUEBA=0)
     * writes Abisal-Prueba: 0 in the manifest; then every Log.d returns before even creating
     * its StringBuffer, so the released game allocates nothing for logging and never touches
     * the Memory Stick for it.
     */
    private static final boolean ACTIVO;
    static {
        String v = null;
        try {
            v = j2me.VM.getAppProperty("Abisal-Prueba");
        } catch (Throwable e) {
        }
        ACTIVO = v == null || !v.equals("0");
    }

    public static void d(String msg) {
        if (!ACTIVO) {
            return;
        }
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
        return "heap " + usada + "/" + total + " KB (vertices " + javax.microedition.m3g.VertexBuffer.cacheKB() + " KB, fuera del heap "
                + j2me.Gfx.nativoKB() + " KB)"
                + (sis > 0 ? ", sistema libre " + sis + " KB" : "");
    }
}
