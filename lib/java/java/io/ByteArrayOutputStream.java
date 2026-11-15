package java.io;

public class ByteArrayOutputStream extends OutputStream {
    protected byte[] buf;
    protected int count;

    public ByteArrayOutputStream() {
        this(32);
    }

    public ByteArrayOutputStream(int size) {
        buf = new byte[size];
    }

    private void ensure(int n) {
        if (n > buf.length) {
            int s = buf.length * 2;
            if (s < n) {
                s = n;
            }
            byte[] b = new byte[s];
            System.arraycopy(buf, 0, b, 0, count);
            buf = b;
        }
    }

    public synchronized void write(int b) {
        ensure(count + 1);
        buf[count++] = (byte) b;
    }

    public synchronized void write(byte[] b, int off, int len) {
        ensure(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    public synchronized void reset() {
        count = 0;
    }

    public synchronized byte[] toByteArray() {
        byte[] b = new byte[count];
        System.arraycopy(buf, 0, b, 0, count);
        return b;
    }

    public int size() {
        return count;
    }

    public String toString() {
        return new String(buf, 0, count);
    }

    public void close() {
    }
}
