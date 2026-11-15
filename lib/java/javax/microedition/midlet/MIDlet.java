package javax.microedition.midlet;

public abstract class MIDlet {
    boolean destroyed;
    boolean paused;

    protected MIDlet() {
        Launcher.current = this;
    }

    protected abstract void startApp() throws MIDletStateChangeException;

    protected abstract void pauseApp();

    protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final void notifyDestroyed() {
        destroyed = true;
        Launcher.exit();
    }

    public final void notifyPaused() {
        paused = true;
    }

    public final void resumeRequest() {
        paused = false;
    }

    public final String getAppProperty(String key) {
        return j2me.VM.getAppProperty(key);
    }

    public final boolean platformRequest(String url) {
        return false;
    }

    public final int checkPermission(String perm) {
        return 1;
    }
}
