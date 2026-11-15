package javax.microedition.m3g;

public class KeyframeSequence extends Object3D {
    public static final int CONSTANT = 192;
    public static final int LOOP = 193;
    public static final int LINEAR = 176;
    public static final int SLERP = 177;
    public static final int SPLINE = 178;
    public static final int SQUAD = 179;
    public static final int STEP = 180;

    final int interpolation;
    final int keyframeCount;
    final int componentCount;
    final int[] times;
    final float[] values;
    int repeatMode = CONSTANT;
    int duration;
    int first;
    int last;

    public KeyframeSequence(int numKeyframes, int numComponents, int interpolation) {
        if (numKeyframes < 1 || numComponents < 1 || interpolation < LINEAR || interpolation > STEP) {
            throw new IllegalArgumentException();
        }
        keyframeCount = numKeyframes;
        componentCount = numComponents;
        this.interpolation = interpolation;
        times = new int[numKeyframes];
        values = new float[numKeyframes * numComponents];
        first = 0;
        last = numKeyframes - 1;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public int getKeyframeCount() {
        return keyframeCount;
    }

    public int getInterpolationType() {
        return interpolation;
    }

    public void setKeyframe(int i, int time, float[] v) {
        if (i < 0 || i >= keyframeCount) {
            throw new IndexOutOfBoundsException();
        }
        times[i] = time;
        System.arraycopy(v, 0, values, i * componentCount, componentCount);
    }

    public int getKeyframe(int i, float[] v) {
        if (v != null) {
            System.arraycopy(values, i * componentCount, v, 0, componentCount);
        }
        return times[i];
    }

    public void setValidRange(int f, int l) {
        if (f < 0 || f >= keyframeCount || l < 0 || l >= keyframeCount) {
            throw new IndexOutOfBoundsException();
        }
        first = f;
        last = l;
    }

    public int getValidRangeFirst() {
        return first;
    }

    public int getValidRangeLast() {
        return last;
    }

    public void setDuration(int d) {
        if (d <= 0) {
            throw new IllegalArgumentException();
        }
        duration = d;
    }

    public int getDuration() {
        return duration;
    }

    public void setRepeatMode(int m) {
        if (m != CONSTANT && m != LOOP) {
            throw new IllegalArgumentException();
        }
        repeatMode = m;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    /** Sample the sequence at time t into out; returns the component count. */
    int sample(float t, float[] out) {
        int n = componentCount;
        int lo = first;
        int hi = last;
        if (hi < lo) {
            hi += keyframeCount;
        }
        int count = hi - lo + 1;
        if (repeatMode == LOOP && duration > 0) {
            t = t % duration;
            if (t < 0) {
                t += duration;
            }
        }
        // find keyframe segment
        int k0 = lo % keyframeCount;
        if (t <= times[k0] || count == 1) {
            if (repeatMode == LOOP && count > 1 && t < times[k0]) {
                int kl = hi % keyframeCount;
                float span = duration - times[kl] + times[k0];
                float s = span > 0 ? (t + duration - times[kl]) / span : 0;
                interpolate(kl, k0, s, out);
                return n;
            }
            System.arraycopy(values, k0 * n, out, 0, n);
            return n;
        }
        for (int j = 0; j < count - 1; j++) {
            int a = (lo + j) % keyframeCount;
            int b = (lo + j + 1) % keyframeCount;
            if (t < times[b]) {
                float span = times[b] - times[a];
                float s = span > 0 ? (t - times[a]) / span : 0;
                interpolate(a, b, s, out);
                return n;
            }
        }
        int kl = hi % keyframeCount;
        if (repeatMode == LOOP && count > 1) {
            float span = duration - times[kl] + times[k0];
            float s = span > 0 ? (t - times[kl]) / span : 0;
            interpolate(kl, k0, s, out);
            return n;
        }
        System.arraycopy(values, kl * n, out, 0, n);
        return n;
    }

    private void interpolate(int a, int b, float s, float[] out) {
        int n = componentCount;
        if (interpolation == STEP) {
            System.arraycopy(values, a * n, out, 0, n);
            return;
        }
        if ((interpolation == SLERP || interpolation == SQUAD) && n == 4) {
            slerp(a * n, b * n, s, out);
            return;
        }
        for (int i = 0; i < n; i++) {
            float va = values[a * n + i];
            out[i] = va + (values[b * n + i] - va) * s;
        }
    }

    private void slerp(int a, int b, float s, float[] out) {
        float ax = values[a];
        float ay = values[a + 1];
        float az = values[a + 2];
        float aw = values[a + 3];
        float bx = values[b];
        float by = values[b + 1];
        float bz = values[b + 2];
        float bw = values[b + 3];
        float dot = ax * bx + ay * by + az * bz + aw * bw;
        if (dot < 0) {
            dot = -dot;
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
        }
        float k0;
        float k1;
        if (dot > 0.9995f) {
            k0 = 1 - s;
            k1 = s;
        } else {
            double th = Math.acos(dot);
            double sn = Math.sin(th);
            k0 = (float) (Math.sin((1 - s) * th) / sn);
            k1 = (float) (Math.sin(s * th) / sn);
        }
        out[0] = ax * k0 + bx * k1;
        out[1] = ay * k0 + by * k1;
        out[2] = az * k0 + bz * k1;
        out[3] = aw * k0 + bw * k1;
    }

    Object3D duplicateImpl() {
        KeyframeSequence k = new KeyframeSequence(keyframeCount, componentCount, interpolation);
        k.copyBase(this);
        System.arraycopy(times, 0, k.times, 0, times.length);
        System.arraycopy(values, 0, k.values, 0, values.length);
        k.repeatMode = repeatMode;
        k.duration = duration;
        k.first = first;
        k.last = last;
        return k;
    }
}
