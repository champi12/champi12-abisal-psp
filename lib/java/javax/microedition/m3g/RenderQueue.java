package javax.microedition.m3g;

/** Collects submeshes for one render call, then draws them sorted by layer. */
final class RenderQueue {
    Mesh[] meshes = new Mesh[64];
    int[] subs = new int[64];
    float[][] worlds = new float[64][];
    float[] alphas = new float[64];
    int[] keys = new int[64];
    int count;

    void clear() {
        for (int i = 0; i < count; i++) {
            meshes[i] = null;
        }
        count = 0;
    }

    void add(Mesh m, int sub, float[] world, float alpha) {
        if (count == meshes.length) {
            int n = count * 2;
            Mesh[] nm = new Mesh[n];
            int[] ns = new int[n];
            float[][] nw = new float[n][];
            float[] na = new float[n];
            int[] nk = new int[n];
            System.arraycopy(meshes, 0, nm, 0, count);
            System.arraycopy(subs, 0, ns, 0, count);
            System.arraycopy(worlds, 0, nw, 0, count);
            System.arraycopy(alphas, 0, na, 0, count);
            System.arraycopy(keys, 0, nk, 0, count);
            meshes = nm;
            subs = ns;
            worlds = nw;
            alphas = na;
            keys = nk;
        }
        float[] w = worlds[count];
        if (w == null) {
            w = new float[16];
            worlds[count] = w;
        }
        System.arraycopy(world, 0, w, 0, 16);
        meshes[count] = m;
        subs[count] = sub;
        alphas[count] = alpha;
        Appearance ap = m.appearances[sub];
        CompositingMode cm = ap.compositing;
        boolean blended = cm != null && cm.blending != CompositingMode.REPLACE;
        keys[count] = ((ap.layer + 64) << 1) | (blended ? 1 : 0);
        count++;
    }

    /** Stable insertion sort by key. */
    void sort() {
        for (int i = 1; i < count; i++) {
            int k = keys[i];
            if (keys[i - 1] <= k) {
                continue;
            }
            Mesh m = meshes[i];
            int s = subs[i];
            float[] w = worlds[i];
            float a = alphas[i];
            int j = i - 1;
            while (j >= 0 && keys[j] > k) {
                keys[j + 1] = keys[j];
                meshes[j + 1] = meshes[j];
                subs[j + 1] = subs[j];
                worlds[j + 1] = worlds[j];
                alphas[j + 1] = alphas[j];
                j--;
            }
            keys[j + 1] = k;
            meshes[j + 1] = m;
            subs[j + 1] = s;
            worlds[j + 1] = w;
            alphas[j + 1] = a;
        }
    }
}
