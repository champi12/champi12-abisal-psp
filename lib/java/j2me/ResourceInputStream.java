package j2me;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** InputStream over a resource file; remembers its path so media can be remapped. */
public final class ResourceInputStream extends ByteArrayInputStream {
    public final String path;

    private ResourceInputStream(String path, byte[] data) {
        super(data);
        this.path = path;
    }

    public static InputStream open(String path) {
        byte[] b = VM.readResource(path);
        if (b == null) {
            return null;
        }
        return new ResourceInputStream(path, b);
    }

    /** Remaining unread bytes (used when a player is created from the stream). */
    public byte[] remaining() {
        byte[] r = new byte[count - pos];
        System.arraycopy(buf, pos, r, 0, r.length);
        return r;
    }
}
