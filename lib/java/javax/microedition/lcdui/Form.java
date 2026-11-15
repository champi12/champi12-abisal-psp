package javax.microedition.lcdui;

public class Form extends Screen {
    private Item[] items = new Item[0];

    public Form(String title) {
        this.title = title;
    }

    public Form(String title, Item[] items) {
        this.title = title;
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                append(items[i]);
            }
        }
    }

    public int append(Item item) {
        Item[] n = new Item[items.length + 1];
        System.arraycopy(items, 0, n, 0, items.length);
        n[items.length] = item;
        items = n;
        return items.length - 1;
    }

    public int append(String s) {
        return append(new StringItem(null, s));
    }

    public int append(Image img) {
        return append(new ImageItem(null, img, 0, null));
    }

    public Item get(int i) {
        if (i < 0 || i >= items.length) {
            throw new IndexOutOfBoundsException();
        }
        return items[i];
    }

    public void set(int i, Item item) {
        if (i < 0 || i >= items.length) {
            throw new IndexOutOfBoundsException();
        }
        items[i] = item;
    }

    public void delete(int i) {
        Item[] n = new Item[items.length - 1];
        System.arraycopy(items, 0, n, 0, i);
        System.arraycopy(items, i + 1, n, i, items.length - i - 1);
        items = n;
    }

    public void deleteAll() {
        items = new Item[0];
    }

    public int size() {
        return items.length;
    }

    public void setItemStateListener(ItemStateListener l) {
    }

    int paintContent(Graphics g, int y) {
        for (int i = 0; i < items.length; i++) {
            Item it = items[i];
            if (it instanceof ImageItem && ((ImageItem) it).getImage() != null) {
                Image img = ((ImageItem) it).getImage();
                g.drawImage(img, 4, y, Graphics.TOP | Graphics.LEFT);
                y += img.getHeight() + 2;
            } else if (it.text() != null) {
                g.drawString(it.text(), 4, y, Graphics.TOP | Graphics.LEFT);
                y += 12;
            }
        }
        return y;
    }
}
