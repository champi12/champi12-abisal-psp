package javax.microedition.lcdui;

public class ImageItem extends Item {
    private Image image;

    public ImageItem(String label, Image img, int layout, String alt) {
        super(label);
        image = img;
        this.layout = layout;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image img) {
        image = img;
    }
}
