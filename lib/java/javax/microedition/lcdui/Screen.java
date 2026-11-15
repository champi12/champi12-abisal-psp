package javax.microedition.lcdui;

public abstract class Screen extends Displayable {
    Screen() {
    }

    void paintScreen(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 0, g.width, g.height);
        g.setColor(0xffffff);
        int y = 4;
        if (title != null) {
            g.drawString(title, 4, y, Graphics.TOP | Graphics.LEFT);
            y += 14;
        }
        y = paintContent(g, y);
        for (int i = 0; i < commands.length && i < 2; i++) {
            g.drawString(commands[i].getLabel(), i == 0 ? 4 : g.width - 4, g.height - 2,
                    Graphics.BOTTOM | (i == 0 ? Graphics.LEFT : Graphics.RIGHT));
        }
    }

    int paintContent(Graphics g, int y) {
        return y;
    }
}
