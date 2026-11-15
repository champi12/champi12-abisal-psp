package j2me;

import java.util.Hashtable;

/** Helpers used by the C runtime and by the library classes. */
public final class VM {
    private static Hashtable interned;
    private static Hashtable manifest;

    private VM() {
    }

    // -- exceptions raised by translated code ------------------------------
    static Throwable npe() {
        return new NullPointerException();
    }

    static Throwable aioobe(int index) {
        return new ArrayIndexOutOfBoundsException(Integer.toString(index));
    }

    static Throwable arith() {
        return new ArithmeticException("/ by zero");
    }

    static Throwable cce(String msg) {
        return new ClassCastException(msg);
    }

    static Throwable nase() {
        return new NegativeArraySizeException();
    }

    static Throwable ase() {
        return new ArrayStoreException();
    }

    static Throwable oome() {
        return new OutOfMemoryError();
    }

    static Throwable ame() {
        return new AbstractMethodError();
    }

    static Throwable imse() {
        return new IllegalMonitorStateException();
    }

    static String describe(Throwable t) {
        return t.toString();
    }

    // -- strings -------------------------------------------------------------
    public static char[] decodeUTF8(byte[] b, int off, int len) {
        char[] out = new char[len];
        int n = 0;
        int end = off + len;
        int i = off;
        while (i < end) {
            int c = b[i] & 0xff;
            if (c < 0x80) {
                out[n++] = (char) c;
                i++;
            } else if ((c & 0xe0) == 0xc0 && i + 1 < end) {
                out[n++] = (char) (((c & 0x1f) << 6) | (b[i + 1] & 0x3f));
                i += 2;
            } else if ((c & 0xf0) == 0xe0 && i + 2 < end) {
                out[n++] = (char) (((c & 0x0f) << 12) | ((b[i + 1] & 0x3f) << 6) | (b[i + 2] & 0x3f));
                i += 3;
            } else {
                out[n++] = (char) c;
                i++;
            }
        }
        if (n == len) {
            return out;
        }
        char[] r = new char[n];
        System.arraycopy(out, 0, r, 0, n);
        return r;
    }

    public static byte[] encodeUTF8(char[] v, int off, int len) {
        int n = 0;
        for (int i = 0; i < len; i++) {
            char c = v[off + i];
            n += (c >= 1 && c < 0x80) ? 1 : (c < 0x800 ? 2 : 3);
        }
        byte[] b = new byte[n];
        int p = 0;
        for (int i = 0; i < len; i++) {
            char c = v[off + i];
            if (c >= 1 && c < 0x80) {
                b[p++] = (byte) c;
            } else if (c < 0x800) {
                b[p++] = (byte) (0xc0 | (c >> 6));
                b[p++] = (byte) (0x80 | (c & 0x3f));
            } else {
                b[p++] = (byte) (0xe0 | (c >> 12));
                b[p++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                b[p++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        return b;
    }

    public static synchronized String intern(String s) {
        if (interned == null) {
            interned = new Hashtable();
        }
        String r = (String) interned.get(s);
        if (r == null) {
            interned.put(s, s);
            r = s;
        }
        return r;
    }

    public static native String floatToString(double v, boolean isDouble);

    public static native double parseDouble(String s);

    // -- properties -----------------------------------------------------------
    public static String getProperty(String key) {
        if (key == null) {
            return null;
        }
        if (key.equals("microedition.platform")) {
            return "PSP";
        }
        if (key.equals("microedition.configuration")) {
            return "CLDC-1.1";
        }
        if (key.equals("microedition.profiles")) {
            return "MIDP-2.0";
        }
        if (key.equals("microedition.m3g.version")) {
            return "1.1";
        }
        if (key.equals("microedition.encoding")) {
            return "UTF-8";
        }
        if (key.equals("microedition.locale")) {
            return "en";
        }
        if (key.equals("supports.video.capture") || key.equals("supports.recording")
                || key.equals("supports.audio.capture")) {
            return "false";
        }
        if (key.equals("supports.mixing")) {
            return "true";
        }
        return null;
    }

    public static synchronized String getAppProperty(String key) {
        if (manifest == null) {
            manifest = new Hashtable();
            loadProps("META-INF/MANIFEST.MF");
            loadProps("app.jad");
        }
        return (String) manifest.get(key);
    }

    private static void loadProps(String res) {
        byte[] b = readResource(res);
        if (b == null) {
            return;
        }
        String s = new String(b);
        int p = 0;
        String lastKey = null;
        while (p < s.length()) {
            int e = s.indexOf('\n', p);
            if (e < 0) {
                e = s.length();
            }
            String line = s.substring(p, e);
            if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
                line = line.substring(0, line.length() - 1);
            }
            p = e + 1;
            if (line.length() > 0 && line.charAt(0) == ' ' && lastKey != null) {
                manifest.put(lastKey, (String) manifest.get(lastKey) + line.substring(1));
                continue;
            }
            int c = line.indexOf(':');
            if (c > 0) {
                lastKey = line.substring(0, c).trim();
                manifest.put(lastKey, line.substring(c + 1).trim());
            }
        }
    }

    // -- resources ------------------------------------------------------------
    public static native byte[] readResource(String path);
}
