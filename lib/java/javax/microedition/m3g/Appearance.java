package javax.microedition.m3g;

import java.util.Vector;

public class Appearance extends Object3D {
    int layer;
    CompositingMode compositing;
    PolygonMode polygon;
    Material material;
    Fog fog;
    final Texture2D[] textures = new Texture2D[2];

    public Appearance() {
    }

    public void setLayer(int l) {
        if (l < -63 || l > 63) {
            throw new IndexOutOfBoundsException();
        }
        layer = l;
    }

    public int getLayer() {
        return layer;
    }

    public void setFog(Fog f) {
        fog = f;
    }

    public Fog getFog() {
        return fog;
    }

    public void setPolygonMode(PolygonMode p) {
        polygon = p;
    }

    public PolygonMode getPolygonMode() {
        return polygon;
    }

    public void setCompositingMode(CompositingMode c) {
        compositing = c;
    }

    public CompositingMode getCompositingMode() {
        return compositing;
    }

    public void setTexture(int i, Texture2D t) {
        if (i < 0 || i >= textures.length) {
            throw new IndexOutOfBoundsException();
        }
        textures[i] = t;
    }

    public Texture2D getTexture(int i) {
        if (i < 0 || i >= textures.length) {
            throw new IndexOutOfBoundsException();
        }
        return textures[i];
    }

    public void setMaterial(Material m) {
        material = m;
    }

    public Material getMaterial() {
        return material;
    }

    void collectReferences(Vector v) {
        super.collectReferences(v);
        if (compositing != null) {
            v.addElement(compositing);
        }
        if (polygon != null) {
            v.addElement(polygon);
        }
        if (material != null) {
            v.addElement(material);
        }
        if (fog != null) {
            v.addElement(fog);
        }
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] != null) {
                v.addElement(textures[i]);
            }
        }
    }

    Object3D duplicateImpl() {
        Appearance a = new Appearance();
        a.copyBase(this);
        a.layer = layer;
        a.compositing = compositing;
        a.polygon = polygon;
        a.material = material;
        a.fog = fog;
        a.textures[0] = textures[0];
        a.textures[1] = textures[1];
        return a;
    }
}
