package javax.microedition.m3g;

public class MorphingMesh extends Mesh {
    VertexBuffer[] targets;
    float[] weights;

    public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer ib, Appearance ap) {
        super(base, ib, ap);
        this.targets = targets;
        weights = new float[targets.length];
    }

    public MorphingMesh(VertexBuffer base, VertexBuffer[] targets, IndexBuffer[] ibs, Appearance[] aps) {
        super(base, ibs, aps);
        this.targets = targets;
        weights = new float[targets.length];
    }

    public VertexBuffer getMorphTarget(int i) {
        return targets[i];
    }

    public int getMorphTargetCount() {
        return targets.length;
    }

    public void setWeights(float[] w) {
        System.arraycopy(w, 0, weights, 0, weights.length);
    }

    public void getWeights(float[] w) {
        System.arraycopy(weights, 0, w, 0, weights.length);
    }
}
