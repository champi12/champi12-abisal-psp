package javax.microedition.lcdui;

public class Alert extends Screen {
    public static final int FOREVER = -2;
    private String text;
    private int timeout = FOREVER;

    public Alert(String title) {
        this.title = title;
    }

    public Alert(String title, String text, Image img, AlertType type) {
        this.title = title;
        this.text = text;
    }

    public void setString(String s) {
        text = s;
    }

    public String getString() {
        return text;
    }

    public void setTimeout(int t) {
        timeout = t;
    }

    public int getTimeout() {
        return timeout;
    }

    int paintContent(Graphics g, int y) {
        if (text != null) {
            g.drawString(text, 4, y, Graphics.TOP | Graphics.LEFT);
            y += 12;
        }
        return y;
    }
}
