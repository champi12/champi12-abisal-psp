package java.lang;

import java.io.PrintStream;

public final class System {
    public static final PrintStream out = new PrintStream(new j2me.ConsoleOutputStream());
    public static final PrintStream err = out;

    private System() {
    }

    public static native void arraycopy(Object src, int srcPos, Object dst, int dstPos, int len);

    public static native long currentTimeMillis();

    public static native int identityHashCode(Object o);

    public static native void gc();

    public static void exit(int status) {
        Runtime.getRuntime().exit(status);
    }

    public static String getProperty(String key) {
        return j2me.VM.getProperty(key);
    }
}
