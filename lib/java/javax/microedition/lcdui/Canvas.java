package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;
    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42;
    public static final int KEY_POUND = 35;

    boolean repaintPending;
    int keyStates;
    int latchedKeys;

    protected Canvas() {
    }

    public boolean isDoubleBuffered() {
        return true;
    }

    public boolean hasPointerEvents() {
        return false;
    }

    public boolean hasPointerMotionEvents() {
        return false;
    }

    public boolean hasRepeatEvents() {
        return true;
    }

    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case UP:
                return -1;
            case DOWN:
                return -2;
            case LEFT:
                return -3;
            case RIGHT:
                return -4;
            case FIRE:
                return -5;
            case GAME_A:
                return KEY_NUM7;
            case GAME_B:
                return KEY_NUM9;
            case GAME_C:
                return KEY_STAR;
            case GAME_D:
                return KEY_POUND;
            default:
                throw new IllegalArgumentException();
        }
    }

    public String getKeyName(int keyCode) {
        if (keyCode >= 32 && keyCode < 127) {
            return String.valueOf((char) keyCode);
        }
        switch (keyCode) {
            case -1:
                return "UP";
            case -2:
                return "DOWN";
            case -3:
                return "LEFT";
            case -4:
                return "RIGHT";
            case -5:
                return "FIRE";
            case -6:
                return "SOFT1";
            case -7:
                return "SOFT2";
            default:
                return "KEY" + keyCode;
        }
    }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case -1:
            case KEY_NUM2:
                return UP;
            case -2:
            case KEY_NUM8:
                return DOWN;
            case -3:
            case KEY_NUM4:
                return LEFT;
            case -4:
            case KEY_NUM6:
                return RIGHT;
            case -5:
            case KEY_NUM5:
                return FIRE;
            case KEY_NUM7:
                return GAME_A;
            case KEY_NUM9:
                return GAME_B;
            case KEY_STAR:
                return GAME_C;
            case KEY_POUND:
                return GAME_D;
            default:
                return 0;
        }
    }

    public void setFullScreenMode(boolean mode) {
    }

    protected void keyPressed(int keyCode) {
    }

    protected void keyRepeated(int keyCode) {
    }

    protected void keyReleased(int keyCode) {
    }

    protected void pointerPressed(int x, int y) {
    }

    protected void pointerReleased(int x, int y) {
    }

    protected void pointerDragged(int x, int y) {
    }

    public final void repaint(int x, int y, int w, int h) {
        repaintPending = true;
    }

    public final void repaint() {
        repaintPending = true;
    }

    public final void serviceRepaints() {
        if (repaintPending && shown) {
            repaintPending = false;
            doPaint();
        }
    }

    void doPaint() {
        Graphics g = new Graphics(null);
        paint(g);
        j2me.Gfx.present();
    }

    protected void showNotify() {
    }

    protected void hideNotify() {
    }

    protected abstract void paint(Graphics g);

    void showNotify0() {
        showNotify();
        repaintPending = true;
    }

    void hideNotify0() {
        hideNotify();
    }

    static int stateBit(int gameAction) {
        return gameAction == 0 ? 0 : 1 << gameAction;
    }

    void key(int type, int code) {
        int bit = stateBit(getGameAction(code));
        if (type == 1) {
            keyStates |= bit;
            latchedKeys |= bit;
            keyPressed(code);
        } else if (type == 2) {
            keyStates &= ~bit;
            keyReleased(code);
        } else if (type == 3) {
            keyRepeated(code);
        }
    }
}
