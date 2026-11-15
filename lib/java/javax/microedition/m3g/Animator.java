package javax.microedition.m3g;

import java.util.Vector;

/** Walks an object graph applying keyframe animation for one animate() call. */
final class Animator {
    final int time;
    int validity = 0x7fffffff;
    private final Vector visited = new Vector();
    private final float[] sample = new float[16];
    private final float[] accum = new float[16];

    Animator(int time) {
        this.time = time;
    }

    void visit(Object3D o) {
        if (o == null) {
            return;
        }
        for (int i = 0; i < visited.size(); i++) {
            if (visited.elementAt(i) == o) {
                return;
            }
        }
        visited.addElement(o);
        o.applyAnimation(this);
        Vector refs = new Vector();
        o.collectReferences(refs);
        for (int i = 0; i < refs.size(); i++) {
            Object3D r = (Object3D) refs.elementAt(i);
            if (r instanceof AnimationTrack || r instanceof KeyframeSequence || r instanceof AnimationController) {
                continue;
            }
            visit(r);
        }
    }

    void applyTracks(Object3D target, AnimationTrack[] tracks) {
        boolean[] done = new boolean[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            if (done[i]) {
                continue;
            }
            int prop = tracks[i].property;
            int comps = 0;
            float totalWeight = 0;
            for (int k = 0; k < 16; k++) {
                accum[k] = 0;
            }
            for (int j = i; j < tracks.length; j++) {
                AnimationTrack t = tracks[j];
                if (t.property != prop) {
                    continue;
                }
                done[j] = true;
                AnimationController c = t.controller;
                if (c == null || !c.isActive(time)) {
                    continue;
                }
                float w = c.weight;
                if (w == 0) {
                    continue;
                }
                KeyframeSequence ks = t.sequence;
                int n = ks.sample(c.sequenceTime(time), sample);
                comps = n;
                for (int k = 0; k < n; k++) {
                    accum[k] += w * sample[k];
                }
                totalWeight += w;
                validity = 0;
            }
            if (totalWeight > 0 && comps > 0) {
                float[] v = new float[comps];
                System.arraycopy(accum, 0, v, 0, comps);
                target.setAnimatedProperty(prop, v);
            }
        }
    }
}
