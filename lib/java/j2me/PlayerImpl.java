package j2me;

import javax.microedition.media.Control;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

/** Player for sampled audio converted to PCM at build time. */
public final class PlayerImpl implements Player, VolumeControl {
    private final String type;
    private final String path;
    private final byte[] data;
    private int state = UNREALIZED;
    private int sound;
    private int voice;
    private int loops = 1;
    private int level = 100;
    private boolean muted;
    private PlayerListener[] listeners = new PlayerListener[0];
    private boolean wasPlaying;

    public PlayerImpl(String path, byte[] data, String type) {
        this.path = path;
        this.data = data;
        this.type = type;
    }

    private void check() {
        if (state == CLOSED) {
            throw new IllegalStateException("closed");
        }
    }

    public synchronized void realize() throws MediaException {
        check();
        if (state == UNREALIZED) {
            state = REALIZED;
        }
    }

    public synchronized void prefetch() throws MediaException {
        check();
        if (state == UNREALIZED) {
            realize();
        }
        if (state == REALIZED) {
            if (sound == 0) {
                sound = path != null ? Media.load(path) : 0;
                if (sound == 0 && data != null) {
                    sound = Media.loadData(data);
                }
            }
            state = PREFETCHED;
        }
    }

    public synchronized void start() throws MediaException {
        check();
        if (state == STARTED) {
            return;
        }
        prefetch();
        if (sound != 0) {
            voice = Media.play(sound, loops, muted ? 0 : level);
        }
        state = STARTED;
        wasPlaying = true;
        Media.register(this);
        event(PlayerListener.STARTED, null);
    }

    public synchronized void stop() throws MediaException {
        check();
        if (state == STARTED) {
            if (voice != 0) {
                Media.stop(voice);
                voice = 0;
            }
            state = PREFETCHED;
            wasPlaying = false;
            event(PlayerListener.STOPPED, null);
        }
    }

    public synchronized void deallocate() {
        if (state == STARTED) {
            try {
                stop();
            } catch (MediaException e) {
            }
        }
        if (state == PREFETCHED) {
            state = REALIZED;
        }
    }

    public synchronized void close() {
        if (state == CLOSED) {
            return;
        }
        deallocate();
        if (sound != 0) {
            Media.free(sound);
            sound = 0;
        }
        state = CLOSED;
        Media.unregister(this);
        event(PlayerListener.CLOSED, null);
    }

    void poll() {
        boolean ended = false;
        synchronized (this) {
            if (state == STARTED && wasPlaying && (voice == 0 || !Media.playing(voice))) {
                voice = 0;
                state = PREFETCHED;
                wasPlaying = false;
                ended = true;
            }
        }
        if (ended) {
            event(PlayerListener.END_OF_MEDIA, null);
        }
    }

    private void event(String ev, Object data) {
        PlayerListener[] l = listeners;
        for (int i = 0; i < l.length; i++) {
            try {
                l[i].playerUpdate(this, ev, data);
            } catch (Throwable t) {
            }
        }
    }

    public long setMediaTime(long t) throws MediaException {
        return 0;
    }

    public long getMediaTime() {
        return voice != 0 ? Media.positionMs(voice) * 1000L : 0;
    }

    public long getDuration() {
        return sound != 0 ? Media.durationMs(sound) * 1000L : TIME_UNKNOWN;
    }

    public int getState() {
        return state;
    }

    public String getContentType() {
        return type;
    }

    public void setLoopCount(int n) {
        if (n == 0) {
            throw new IllegalArgumentException();
        }
        loops = n;
    }

    public synchronized void addPlayerListener(PlayerListener l) {
        if (l == null) {
            return;
        }
        PlayerListener[] n = new PlayerListener[listeners.length + 1];
        System.arraycopy(listeners, 0, n, 0, listeners.length);
        n[listeners.length] = l;
        listeners = n;
    }

    public synchronized void removePlayerListener(PlayerListener l) {
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == l) {
                PlayerListener[] n = new PlayerListener[listeners.length - 1];
                System.arraycopy(listeners, 0, n, 0, i);
                System.arraycopy(listeners, i + 1, n, i, listeners.length - i - 1);
                listeners = n;
                return;
            }
        }
    }

    public Control[] getControls() {
        Control[] c = new Control[1];
        c[0] = this;
        return c;
    }

    public Control getControl(String name) {
        if (name.equals("VolumeControl") || name.equals("javax.microedition.media.control.VolumeControl")) {
            return this;
        }
        return null;
    }

    public void setMute(boolean m) {
        muted = m;
        if (voice != 0) {
            Media.setVolume(voice, m ? 0 : level);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    public int setLevel(int l) {
        level = l < 0 ? 0 : (l > 100 ? 100 : l);
        if (voice != 0 && !muted) {
            Media.setVolume(voice, level);
        }
        return level;
    }

    public int getLevel() {
        return level;
    }
}
