package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;

public final class Manager {
    public static final String TONE_DEVICE_LOCATOR = "device://tone";
    public static final String MIDI_DEVICE_LOCATOR = "device://midi";

    private Manager() {
    }

    public static String[] getSupportedContentTypes(String protocol) {
        String[] s = new String[3];
        s[0] = "audio/x-wav";
        s[1] = "audio/amr";
        s[2] = "audio/midi";
        return s;
    }

    public static String[] getSupportedProtocols(String type) {
        String[] s = new String[1];
        s[0] = "resource";
        return s;
    }

    public static Player createPlayer(String locator) throws IOException, MediaException {
        if (locator == null) {
            throw new IllegalArgumentException();
        }
        if (locator.startsWith("capture:") || locator.startsWith("device:")) {
            throw new MediaException("unsupported locator " + locator);
        }
        String path = locator;
        if (path.startsWith("resource:")) {
            path = path.substring(9);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return new j2me.PlayerImpl(path, null, null);
    }

    public static Player createPlayer(InputStream in, String type) throws IOException, MediaException {
        if (in == null) {
            throw new IllegalArgumentException();
        }
        if (in instanceof j2me.ResourceInputStream) {
            j2me.ResourceInputStream r = (j2me.ResourceInputStream) in;
            return new j2me.PlayerImpl(r.path, null, type);
        }
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[2048];
        int n;
        while ((n = in.read(b, 0, b.length)) > 0) {
            bo.write(b, 0, n);
        }
        return new j2me.PlayerImpl(null, bo.toByteArray(), type);
    }

    public static void playTone(int note, int duration, int volume) throws MediaException {
    }
}
