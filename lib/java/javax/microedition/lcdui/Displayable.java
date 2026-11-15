package javax.microedition.lcdui;

public abstract class Displayable {
    String title;
    Command[] commands = new Command[0];
    CommandListener listener;
    boolean shown;

    Displayable() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String t) {
        title = t;
    }

    public void addCommand(Command c) {
        for (int i = 0; i < commands.length; i++) {
            if (commands[i] == c) {
                return;
            }
        }
        Command[] n = new Command[commands.length + 1];
        System.arraycopy(commands, 0, n, 0, commands.length);
        n[commands.length] = c;
        commands = n;
    }

    public void removeCommand(Command c) {
        for (int i = 0; i < commands.length; i++) {
            if (commands[i] == c) {
                Command[] n = new Command[commands.length - 1];
                System.arraycopy(commands, 0, n, 0, i);
                System.arraycopy(commands, i + 1, n, i, commands.length - i - 1);
                commands = n;
                return;
            }
        }
    }

    public void setCommandListener(CommandListener l) {
        listener = l;
    }

    public int getWidth() {
        return j2me.Gfx.screenWidth();
    }

    public int getHeight() {
        return j2me.Gfx.screenHeight();
    }

    public boolean isShown() {
        return shown;
    }

    protected void sizeChanged(int w, int h) {
    }

    void showNotify0() {
    }

    void hideNotify0() {
    }

    /** Key handling for non-canvas screens: soft keys trigger commands. */
    void key(int type, int code) {
        if (type != 1 || listener == null || commands.length == 0) {
            return;
        }
        if (code == -6 || code == -7 || code == -5) {
            Command c = commands[code == -7 && commands.length > 1 ? 1 : 0];
            listener.commandAction(c, this);
        }
    }

    void paintScreen(Graphics g) {
    }
}
