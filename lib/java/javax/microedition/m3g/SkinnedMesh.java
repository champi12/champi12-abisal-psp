package javax.microedition.m3g;

public class SkinnedMesh extends Mesh {
    Group skeleton;

    public SkinnedMesh(VertexBuffer vb, IndexBuffer ib, Appearance ap, Group skeleton) {
        super(vb, ib, ap);
        this.skeleton = skeleton;
    }

    public SkinnedMesh(VertexBuffer vb, IndexBuffer[] ibs, Appearance[] aps, Group skeleton) {
        super(vb, ibs, aps);
        this.skeleton = skeleton;
    }

    public Group getSkeleton() {
        return skeleton;
    }

    public void addTransform(Node bone, int weight, int first, int count) {
    }

    public void getBoneTransform(Node bone, Transform t) {
        t.setIdentity();
    }

    public int getBoneVertices(Node bone, int[] indices, float[] weights) {
        return 0;
    }
}
