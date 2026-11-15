package java.io;

public class DataInputStream extends InputStream implements DataInput {
    protected InputStream in;

    public DataInputStream(InputStream in) {
        this.in = in;
    }

    public int read() throws IOException {
        return in.read();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    public int available() throws IOException {
        return in.available();
    }

    public void close() throws IOException {
        in.close();
    }

    public void mark(int limit) {
        in.mark(limit);
    }

    public void reset() throws IOException {
        in.reset();
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    public final void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(byte[] b, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int k = in.read(b, off + n, len - n);
            if (k < 0) {
                throw new EOFException();
            }
            n += k;
        }
    }

    public final int skipBytes(int n) throws IOException {
        int t = 0;
        while (t < n) {
            long k = in.skip(n - t);
            if (k <= 0) {
                break;
            }
            t += (int) k;
        }
        return t;
    }

    private int r() throws IOException {
        int c = in.read();
        if (c < 0) {
            throw new EOFException();
        }
        return c;
    }

    public final boolean readBoolean() throws IOException {
        return r() != 0;
    }

    public final byte readByte() throws IOException {
        return (byte) r();
    }

    public final int readUnsignedByte() throws IOException {
        return r();
    }

    public final short readShort() throws IOException {
        int a = r();
        return (short) ((a << 8) | r());
    }

    public final int readUnsignedShort() throws IOException {
        int a = r();
        return (a << 8) | r();
    }

    public final char readChar() throws IOException {
        int a = r();
        return (char) ((a << 8) | r());
    }

    public final int readInt() throws IOException {
        int a = r();
        int b = r();
        int c = r();
        return (a << 24) | (b << 16) | (c << 8) | r();
    }

    public final long readLong() throws IOException {
        long hi = readInt();
        long lo = readInt() & 0xffffffffL;
        return (hi << 32) | lo;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final String readUTF() throws IOException {
        return readUTF(this);
    }

    public static final String readUTF(DataInput in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] b = new byte[len];
        in.readFully(b, 0, len);
        return new String(j2me.VM.decodeUTF8(b, 0, len));
    }
}
