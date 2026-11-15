package javax.microedition.m3g;

import java.util.Vector;

public abstract class Object3D {
    int userID;
    Object userObject;
    AnimationTrack[] tracks = new AnimationTrack[0];

    Object3D() {
    }

    public final void setUserID(int id) {
        userID = id;
    }

    public final int getUserID() {
        return userID;
    }

    public final void setUserObject(Object o) {
        userObject = o;
    }

    public final Object getUserObject() {
        return userObject;
    }

    public void addAnimationTrack(AnimationTrack t) {
        if (t == null) {
            throw new NullPointerException();
        }
        AnimationTrack[] n = new AnimationTrack[tracks.length + 1];
        System.arraycopy(tracks, 0, n, 0, tracks.length);
        n[tracks.length] = t;
        tracks = n;
    }

    public void removeAnimationTrack(AnimationTrack t) {
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i] == t) {
                AnimationTrack[] n = new AnimationTrack[tracks.length - 1];
                System.arraycopy(tracks, 0, n, 0, i);
                System.arraycopy(tracks, i + 1, n, i, tracks.length - i - 1);
                tracks = n;
                return;
            }
        }
    }

    public int getAnimationTrackCount() {
        return tracks.length;
    }

    public AnimationTrack getAnimationTrack(int i) {
        return tracks[i];
    }

    /** Subclasses append the objects they reference. */
    void collectReferences(Vector v) {
        for (int i = 0; i < tracks.length; i++) {
            v.addElement(tracks[i]);
        }
    }

    public int getReferences(Object3D[] out) {
        Vector v = new Vector();
        collectReferences(v);
        if (out != null) {
            for (int i = 0; i < v.size() && i < out.length; i++) {
                out[i] = (Object3D) v.elementAt(i);
            }
        }
        return v.size();
    }

    public Object3D find(int id) {
        if (userID == id) {
            return this;
        }
        Vector v = new Vector();
        collectReferences(v);
        for (int i = 0; i < v.size(); i++) {
            Object3D o = (Object3D) v.elementAt(i);
            if (o != null) {
                Object3D r = o.find(id);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    public final int animate(int time) {
        Animator a = new Animator(time);
        a.visit(this);
        return a.validity;
    }

    /** Apply this object's own animation tracks. */
    void applyAnimation(Animator a) {
        if (tracks.length == 0) {
            return;
        }
        a.applyTracks(this, tracks);
    }

    /** Set an animated property (values already blended). */
    void setAnimatedProperty(int property, float[] v) {
    }

    public final Object3D duplicate() {
        Object3D o = duplicateImpl();
        return o;
    }

    abstract Object3D duplicateImpl();

    void copyBase(Object3D from) {
        userID = from.userID;
        userObject = from.userObject;
        tracks = new AnimationTrack[from.tracks.length];
        System.arraycopy(from.tracks, 0, tracks, 0, tracks.length);
    }
}
