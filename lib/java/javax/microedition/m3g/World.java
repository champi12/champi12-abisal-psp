package javax.microedition.m3g;

import java.util.Vector;

public class World extends Group {
    Camera activeCamera;
    Background background;

    public World() {
    }

    public void setActiveCamera(Camera c) {
        if (c == null) {
            throw new NullPointerException();
        }
        activeCamera = c;
    }

    public Camera getActiveCamera() {
        return activeCamera;
    }

    public void setBackground(Background b) {
        background = b;
    }

    public Background getBackground() {
        return background;
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        if (background != null) {
            v.addElement(background);
        }
    }

    Object3D duplicateImpl() {
        World w = new World();
        copyGroup(w);
        w.activeCamera = activeCamera;
        w.background = background;
        return w;
    }
}
