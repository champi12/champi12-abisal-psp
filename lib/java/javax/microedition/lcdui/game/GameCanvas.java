package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/**
 * The off-screen buffer of a GameCanvas is the display back buffer itself; the runtime
 * keeps its contents across flushGraphics() so incremental drawing behaves as on phones.
 */
public abstract class GameCanvas extends Canvas {
    public static final int UP_PRESSED = 1 << UP;
    public static final int DOWN_PRESSED = 1 << DOWN;
    public static final int LEFT_PRESSED = 1 << LEFT;
    public static final int RIGHT_PRESSED = 1 << RIGHT;
    public static final int FIRE_PRESSED = 1 << FIRE;
    public static final int GAME_A_PRESSED = 1 << GAME_A;
    public static final int GAME_B_PRESSED = 1 << GAME_B;
    public static final int GAME_C_PRESSED = 1 << GAME_C;
    public static final int GAME_D_PRESSED = 1 << GAME_D;

    private final boolean suppressKeys;

    protected GameCanvas(boolean suppressKeyEvents) {
        suppressKeys = suppressKeyEvents;
    }

    protected Graphics getGraphics() {
        return javax.microedition.lcdui.LcduiAccess.screenGraphics();
    }

    public int getKeyStates() {
        int s = javax.microedition.lcdui.LcduiAccess.takeKeyStates(this);
        return s;
    }

    public void paint(Graphics g) {
    }

    public void flushGraphics() {
        j2me.Gfx.present();
    }

    public void flushGraphics(int x, int y, int w, int h) {
        j2me.Gfx.present();
    }

    protected void keyPressed(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    final boolean suppressesKeys() {
        return suppressKeys;
    }
}
