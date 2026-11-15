package abisal;

/**
 * devlog: every line goes to System.out, which the runtime writes to the console and to
 * devlog.txt next to the EBOOT (opened and closed per line, so it survives a crash).
 */
public final class Log {
    private static final long T0 = System.currentTimeMillis();

    private Log() {
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

    /** Java heap in use / total, in KB, plus free system memory */
    public static String memoria() {
        Runtime r = Runtime.getRuntime();
        long total = r.totalMemory() / 1024;
        long usada = total - r.freeMemory() / 1024;
        int sis = j2me.Pad.memLibre();
        return "heap " + usada + "/" + total + " KB" + (sis > 0 ? ", sistema libre " + sis + " KB" : "");
    }
}
