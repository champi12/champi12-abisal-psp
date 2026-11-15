package java.io;

public class ByteArrayInputStream extends InputStream {
    protected byte[] buf;
    protected int pos;
    protected int mark;
    protected int count;

    public ByteArrayInputStream(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public ByteArrayInputStream(byte[] buf, int off, int len) {
        this.buf = buf;
        pos = off;
        mark = off;
        count = off + len > buf.length ? buf.length : off + len;
    }

    public synchronized int read() {
        return pos < count ? (buf[pos++] & 0xff) : -1;
    }

    public synchronized int read(byte[] b, int off, int len) {
        if (pos >= count) {
            return -1;
        }
        if (len > count - pos) {
            len = count - pos;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    public synchronized long skip(long n) {
        if (n > count - pos) {
            n = count - pos;
        }
        if (n < 0) {
            return 0;
        }
        pos += (int) n;
        return n;
    }

    public synchronized int available() {
        return count - pos;
    }

    public boolean markSupported() {
        return true;
    }

    public void mark(int limit) {
        mark = pos;
    }

    public synchronized void reset() {
        pos = mark;
    }

    public void close() {
    }
}
