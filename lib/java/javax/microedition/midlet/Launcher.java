package javax.microedition.midlet;

import javax.microedition.lcdui.Display;

/** Entry point invoked by the C runtime: instantiates the MIDlet and runs the event loop. */
public final class Launcher {
    static MIDlet current;

    private Launcher() {
    }

    public static void main() {
        String spec = j2me.VM.getAppProperty("MIDlet-1");
        String cls = null;
        if (spec != null) {
            int p = spec.lastIndexOf(',');
            cls = spec.substring(p + 1).trim();
        }
        if (cls == null || cls.length() == 0) {
            System.out.println("No MIDlet-1 entry in manifest");
            return;
        }
        try {
            Class c = Class.forName(cls);
            MIDlet m = (MIDlet) c.newInstance();
            current = m;
            m.startApp();
        } catch (Throwable t) {
            System.out.println("MIDlet start failed: " + t);
            t.printStackTrace();
            return;
        }
        Display.eventLoop();
    }

    /** Called by the event loop when the user asks to leave (HOME). */
    public static void requestExit() {
        MIDlet m = current;
        if (m != null && !m.destroyed) {
            try {
                m.destroyApp(true);
            } catch (Throwable t) {
            }
        }
        exit();
    }

    public static void pause() {
        MIDlet m = current;
        if (m != null) {
            m.pauseApp();
        }
    }

    public static void resume() {
        MIDlet m = current;
        if (m != null) {
            try {
                m.startApp();
            } catch (Throwable t) {
            }
        }
    }

    static native void exit();
}
