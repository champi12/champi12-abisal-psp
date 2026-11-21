package j2me;

/** Glue between javax.microedition.media players and the native sound mixer. */
public final class Media {
    private static PlayerImpl[] players = new PlayerImpl[16];
    private static int count;

    private Media() {
    }

    static synchronized void register(PlayerImpl p) {
        for (int i = 0; i < count; i++) {
            if (players[i] == p) {
                return;
            }
        }
        if (count == players.length) {
            PlayerImpl[] n = new PlayerImpl[count * 2];
            System.arraycopy(players, 0, n, 0, count);
            players = n;
        }
        players[count++] = p;
    }

    static synchronized void unregister(PlayerImpl p) {
        for (int i = 0; i < count; i++) {
            if (players[i] == p) {
                players[i] = players[--count];
                players[count] = null;
                return;
            }
        }
    }

    private static PlayerImpl[] copia = new PlayerImpl[16];

    /** Called from the event loop: deliver end-of-media events. */
    public static void dispatchEvents() {
        PlayerImpl[] ps;
        int n;
        synchronized (Media.class) {
            n = count;
            if (copia.length < n) {
                copia = new PlayerImpl[n + 8];
            }
            ps = copia;
            System.arraycopy(players, 0, ps, 0, n);
        }
        for (int i = 0; i < n; i++) {
            ps[i].poll();
        }
    }

    // natives: sounds are decoded PCM loaded from converted resources
    static native int load(String path);

    static native int loadData(byte[] data);

    static native void free(int sound);

    static native int play(int sound, int loops, int volume);

    static native void stop(int voice);

    static native boolean playing(int voice);

    static native void setVolume(int voice, int volume);

    static native int durationMs(int sound);

    static native int positionMs(int voice);
}
