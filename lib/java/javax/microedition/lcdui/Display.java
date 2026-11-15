package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;

public class Display {
    public static final int LIST_ELEMENT = 1;
    public static final int CHOICE_GROUP_ELEMENT = 2;
    public static final int ALERT = 3;
    public static final int COLOR_BACKGROUND = 0;
    public static final int COLOR_FOREGROUND = 1;
    public static final int COLOR_HIGHLIGHTED_BACKGROUND = 2;
    public static final int COLOR_HIGHLIGHTED_FOREGROUND = 3;
    public static final int COLOR_BORDER = 4;
    public static final int COLOR_HIGHLIGHTED_BORDER = 5;

    private static Display instance;
    private static Runnable[] serial = new Runnable[8];
    private static int serialCount;
    Displayable current;

    private Display() {
    }

    public static Display getDisplay(MIDlet m) {
        if (instance == null) {
            instance = new Display();
        }
        return instance;
    }

    public Displayable getCurrent() {
        return current;
    }

    public void setCurrent(Displayable d) {
        if (d == current || d == null) {
            return;
        }
        Displayable old = current;
        current = d;
        if (old != null) {
            old.shown = false;
            old.hideNotify0();
        }
        d.shown = true;
        d.showNotify0();
    }

    public void setCurrent(Alert a, Displayable next) {
        setCurrent(next);
    }

    public void setCurrentItem(Item item) {
    }

    public boolean isColor() {
        return true;
    }

    public int numColors() {
        return 1 << 24;
    }

    public int numAlphaLevels() {
        return 256;
    }

    public boolean vibrate(int ms) {
        return true;
    }

    public boolean flashBacklight(int ms) {
        return true;
    }

    public int getColor(int which) {
        return which == COLOR_FOREGROUND ? 0xffffff : 0;
    }

    public int getBorderStyle(boolean highlighted) {
        return Graphics.SOLID;
    }

    public int getBestImageWidth(int type) {
        return 0;
    }

    public int getBestImageHeight(int type) {
        return 0;
    }

    public void callSerially(Runnable r) {
        synchronized (Display.class) {
            if (serialCount == serial.length) {
                Runnable[] n = new Runnable[serial.length * 2];
                System.arraycopy(serial, 0, n, 0, serialCount);
                serial = n;
            }
            serial[serialCount++] = r;
        }
    }

    /** Main event loop, run on the startup thread after startApp(). */
    public static void eventLoop() {
        for (;;) {
            int ev;
            while ((ev = j2me.Gfx.pollEvent()) != 0) {
                int type = ev >>> 24;
                int code = (short) (ev & 0xffff);
                if (type == 4) {
                    javax.microedition.midlet.Launcher.requestExit();
                    continue;
                }
                if (type == 5) {
                    javax.microedition.midlet.Launcher.pause();
                    continue;
                }
                if (type == 6) {
                    javax.microedition.midlet.Launcher.resume();
                    continue;
                }
                Display d = instance;
                if (d != null && d.current != null) {
                    try {
                        d.current.key(type, code);
                    } catch (Throwable t) {
                        System.out.println("key handler: " + t);
                    }
                }
            }
            Runnable[] run = null;
            int n = 0;
            synchronized (Display.class) {
                if (serialCount > 0) {
                    run = serial;
                    n = serialCount;
                    serial = new Runnable[8];
                    serialCount = 0;
                }
            }
            for (int i = 0; i < n; i++) {
                try {
                    run[i].run();
                } catch (Throwable t) {
                    System.out.println("callSerially: " + t);
                }
            }
            Display d = instance;
            if (d != null && d.current != null) {
                Displayable c = d.current;
                try {
                    if (c instanceof Canvas) {
                        ((Canvas) c).serviceRepaints();
                    } else {
                        Graphics g = new Graphics(null);
                        c.paintScreen(g);
                        j2me.Gfx.present();
                    }
                } catch (Throwable t) {
                    System.out.println("paint: " + t);
                }
            }
            j2me.Media.dispatchEvents();
            try {
                Thread.sleep(c_sleep());
            } catch (InterruptedException e) {
            }
        }
    }

    private static int c_sleep() {
        Display d = instance;
        return (d != null && d.current != null && !(d.current instanceof Canvas)) ? 33 : 8;
    }
}
