package abisal;

import javax.microedition.m3g.Appearance;
import javax.microedition.m3g.Fog;
import javax.microedition.m3g.Group;
import javax.microedition.m3g.Loader;
import javax.microedition.m3g.Mesh;
import javax.microedition.m3g.Node;
import javax.microedition.m3g.Object3D;
import javax.microedition.m3g.Transform;

/**
 * A model made in Blender and exported with the M3G add-on (assets/modelos/*.py build
 * them, tools/blender/exportar_modelos.sh exports them). Each piece is a Mesh found by its
 * user ID (the "#N" at the end of the Blender object name). Blender is Z-up: the exporter
 * puts that conversion in each node's transform, which immediate-mode rendering ignores,
 * so it is kept here (base[]) and applied when a piece is drawn.
 */
final class ModeloM3G {
    final Mesh[] pieza;
    final Transform[] base;

    private ModeloM3G(int n) {
        pieza = new Mesh[n];
        base = new Transform[n];
    }

    /** loads the model and finds pieces 1..n by ID; null if anything is missing */
    static ModeloM3G cargar(String ruta, Fog fog, int n) {
        try {
            Object3D[] raiz = Loader.load(ruta);
            ModeloM3G m = new ModeloM3G(n);
            for (int i = 0; i < raiz.length; i++) {
                m.buscar(raiz[i], fog);
            }
            for (int i = 0; i < n; i++) {
                if (m.pieza[i] == null) {
                    Log.d("modelo " + ruta + ": falta la pieza #" + (i + 1));
                    return null;
                }
            }
            Log.d("modelo " + ruta + " cargado (" + n + " piezas)");
            return m;
        } catch (Throwable e) {
            Log.d("modelo " + ruta + " no se pudo cargar: " + e);
            return null;
        }
    }

    private void buscar(Object3D o, Fog fog) {
        if (o instanceof Mesh) {
            Mesh me = (Mesh) o;
            int id = me.getUserID();
            if (id >= 1 && id <= pieza.length) {
                pieza[id - 1] = me;
                Transform t = new Transform();
                me.getCompositeTransform(t);
                base[id - 1] = t;
                for (int k = 0; k < me.getSubmeshCount(); k++) {
                    Appearance ap = me.getAppearance(k);
                    if (ap != null) {
                        ap.setFog(fog);
                        // the add-on writes an almost white emissive colour on every material
                        // when Blender's emission is off: keep emission only where the colour
                        // matches it (the glowing parts), otherwise everything washes out white
                        javax.microedition.m3g.Material mat = ap.getMaterial();
                        if (mat != null) {
                            int em = mat.getColor(javax.microedition.m3g.Material.EMISSIVE) & 0xffffff;
                            int di = mat.getColor(javax.microedition.m3g.Material.DIFFUSE) & 0xffffff;
                            if (em != di) {
                                mat.setColor(javax.microedition.m3g.Material.EMISSIVE, 0);
                            }
                            // the add-on sets the ambient colour to a fifth of the diffuse one;
                            // the game's own models use the full colour for both, so the water's
                            // ambient light shows them the same way
                            mat.setColor(javax.microedition.m3g.Material.AMBIENT, di);
                        }
                    }
                }
            }
        }
        if (o instanceof Group) {
            Group g = (Group) o;
            for (int i = 0; i < g.getChildCount(); i++) {
                buscar(g.getChild(i), fog);
            }
        }
    }
}
