package j2me;

import java.io.OutputStream;

public final class ConsoleOutputStream extends OutputStream {
    private final byte[] one = new byte[1];

    public void write(int b) {
        one[0] = (byte) b;
        write0(one, 0, 1);
    }

    public void write(byte[] b, int off, int len) {
        write0(b, off, len);
    }

    private static native void write0(byte[] b, int off, int len);
}
