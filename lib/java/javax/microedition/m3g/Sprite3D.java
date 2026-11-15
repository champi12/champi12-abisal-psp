package javax.microedition.m3g;

public class Sprite3D extends Node {
    boolean scaled;
    Image2D image;
    Appearance appearance;

    public Sprite3D(boolean scaled, Image2D image, Appearance ap) {
        this.scaled = scaled;
        this.image = image;
        appearance = ap;
    }

    public boolean isScaled() {
        return scaled;
    }

    public void setAppearance(Appearance a) {
        appearance = a;
    }

    public Appearance getAppearance() {
        return appearance;
    }

    public void setImage(Image2D i) {
        image = i;
    }

    public Image2D getImage() {
        return image;
    }

    public void setCrop(int x, int y, int w, int h) {
    }

    Object3D duplicateImpl() {
        Sprite3D s = new Sprite3D(scaled, image, appearance);
        s.copyNode(this);
        return s;
    }
}
