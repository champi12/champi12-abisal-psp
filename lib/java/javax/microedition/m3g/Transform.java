package javax.microedition.m3g;

/** 4x4 matrix, row-major, operating on column vectors. */
public class Transform {
    final float[] m = new float[16];

    public Transform() {
        setIdentity();
    }

    public Transform(Transform t) {
        set(t);
    }

    public void setIdentity() {
        for (int i = 0; i < 16; i++) {
            m[i] = (i % 5 == 0) ? 1 : 0;
        }
    }

    public void set(Transform t) {
        if (t == null) {
            setIdentity();
        } else {
            System.arraycopy(t.m, 0, m, 0, 16);
        }
    }

    public void set(float[] v) {
        if (v.length < 16) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(v, 0, m, 0, 16);
    }

    public void get(float[] v) {
        if (v.length < 16) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(m, 0, v, 0, 16);
    }

    public void invert() {
        if (!invert(m, m)) {
            throw new ArithmeticException("singular matrix");
        }
    }

    static boolean invert(float[] a, float[] out) {
        float[] inv = new float[16];
        inv[0] = a[5] * a[10] * a[15] - a[5] * a[11] * a[14] - a[9] * a[6] * a[15] + a[9] * a[7] * a[14]
                + a[13] * a[6] * a[11] - a[13] * a[7] * a[10];
        inv[4] = -a[4] * a[10] * a[15] + a[4] * a[11] * a[14] + a[8] * a[6] * a[15] - a[8] * a[7] * a[14]
                - a[12] * a[6] * a[11] + a[12] * a[7] * a[10];
        inv[8] = a[4] * a[9] * a[15] - a[4] * a[11] * a[13] - a[8] * a[5] * a[15] + a[8] * a[7] * a[13]
                + a[12] * a[5] * a[11] - a[12] * a[7] * a[9];
        inv[12] = -a[4] * a[9] * a[14] + a[4] * a[10] * a[13] + a[8] * a[5] * a[14] - a[8] * a[6] * a[13]
                - a[12] * a[5] * a[10] + a[12] * a[6] * a[9];
        inv[1] = -a[1] * a[10] * a[15] + a[1] * a[11] * a[14] + a[9] * a[2] * a[15] - a[9] * a[3] * a[14]
                - a[13] * a[2] * a[11] + a[13] * a[3] * a[10];
        inv[5] = a[0] * a[10] * a[15] - a[0] * a[11] * a[14] - a[8] * a[2] * a[15] + a[8] * a[3] * a[14]
                + a[12] * a[2] * a[11] - a[12] * a[3] * a[10];
        inv[9] = -a[0] * a[9] * a[15] + a[0] * a[11] * a[13] + a[8] * a[1] * a[15] - a[8] * a[3] * a[13]
                - a[12] * a[1] * a[11] + a[12] * a[3] * a[9];
        inv[13] = a[0] * a[9] * a[14] - a[0] * a[10] * a[13] - a[8] * a[1] * a[14] + a[8] * a[2] * a[13]
                + a[12] * a[1] * a[10] - a[12] * a[2] * a[9];
        inv[2] = a[1] * a[6] * a[15] - a[1] * a[7] * a[14] - a[5] * a[2] * a[15] + a[5] * a[3] * a[14]
                + a[13] * a[2] * a[7] - a[13] * a[3] * a[6];
        inv[6] = -a[0] * a[6] * a[15] + a[0] * a[7] * a[14] + a[4] * a[2] * a[15] - a[4] * a[3] * a[14]
                - a[12] * a[2] * a[7] + a[12] * a[3] * a[6];
        inv[10] = a[0] * a[5] * a[15] - a[0] * a[7] * a[13] - a[4] * a[1] * a[15] + a[4] * a[3] * a[13]
                + a[12] * a[1] * a[7] - a[12] * a[3] * a[5];
        inv[14] = -a[0] * a[5] * a[14] + a[0] * a[6] * a[13] + a[4] * a[1] * a[14] - a[4] * a[2] * a[13]
                - a[12] * a[1] * a[6] + a[12] * a[2] * a[5];
        inv[3] = -a[1] * a[6] * a[11] + a[1] * a[7] * a[10] + a[5] * a[2] * a[11] - a[5] * a[3] * a[10]
                - a[9] * a[2] * a[7] + a[9] * a[3] * a[6];
        inv[7] = a[0] * a[6] * a[11] - a[0] * a[7] * a[10] - a[4] * a[2] * a[11] + a[4] * a[3] * a[10]
                + a[8] * a[2] * a[7] - a[8] * a[3] * a[6];
        inv[11] = -a[0] * a[5] * a[11] + a[0] * a[7] * a[9] + a[4] * a[1] * a[11] - a[4] * a[3] * a[9]
                - a[8] * a[1] * a[7] + a[8] * a[3] * a[5];
        inv[15] = a[0] * a[5] * a[10] - a[0] * a[6] * a[9] - a[4] * a[1] * a[10] + a[4] * a[2] * a[9]
                + a[8] * a[1] * a[6] - a[8] * a[2] * a[5];
        float det = a[0] * inv[0] + a[1] * inv[4] + a[2] * inv[8] + a[3] * inv[12];
        if (det == 0) {
            return false;
        }
        det = 1.0f / det;
        for (int i = 0; i < 16; i++) {
            out[i] = inv[i] * det;
        }
        return true;
    }

    public void transpose() {
        for (int r = 0; r < 4; r++) {
            for (int c = r + 1; c < 4; c++) {
                float t = m[r * 4 + c];
                m[r * 4 + c] = m[c * 4 + r];
                m[c * 4 + r] = t;
            }
        }
    }

    private static final float[] scratch = new float[16];

    /** out = a * b (all row-major 4x4); out may alias a or b. */
    static void mul(float[] a, float[] b, float[] out) {
        float[] t = scratch;
        mulInto(a, b, t);
        System.arraycopy(t, 0, out, 0, 16);
    }

    static void mulInto(float[] a, float[] b, float[] t) {
        for (int r = 0; r < 4; r++) {
            float a0 = a[r * 4];
            float a1 = a[r * 4 + 1];
            float a2 = a[r * 4 + 2];
            float a3 = a[r * 4 + 3];
            t[r * 4] = a0 * b[0] + a1 * b[4] + a2 * b[8] + a3 * b[12];
            t[r * 4 + 1] = a0 * b[1] + a1 * b[5] + a2 * b[9] + a3 * b[13];
            t[r * 4 + 2] = a0 * b[2] + a1 * b[6] + a2 * b[10] + a3 * b[14];
            t[r * 4 + 3] = a0 * b[3] + a1 * b[7] + a2 * b[11] + a3 * b[15];
        }
    }

    public void postMultiply(Transform t) {
        mul(m, t.m, m);
    }

    void postMultiply(float[] b) {
        mul(m, b, m);
    }

    void preMultiply(float[] b) {
        mul(b, m, m);
    }

    public void postScale(float sx, float sy, float sz) {
        for (int r = 0; r < 4; r++) {
            m[r * 4] *= sx;
            m[r * 4 + 1] *= sy;
            m[r * 4 + 2] *= sz;
        }
    }

    public void postTranslate(float tx, float ty, float tz) {
        for (int r = 0; r < 4; r++) {
            m[r * 4 + 3] += m[r * 4] * tx + m[r * 4 + 1] * ty + m[r * 4 + 2] * tz;
        }
    }

    public void postRotate(float angle, float ax, float ay, float az) {
        if (angle == 0) {
            return;
        }
        float[] r = new float[16];
        rotation(angle, ax, ay, az, r);
        mul(m, r, m);
    }

    public void postRotateQuat(float qx, float qy, float qz, float qw) {
        float[] r = new float[16];
        quatMatrix(qx, qy, qz, qw, r);
        mul(m, r, m);
    }

    static void rotation(float angle, float ax, float ay, float az, float[] r) {
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        for (int i = 0; i < 16; i++) {
            r[i] = (i % 5 == 0) ? 1 : 0;
        }
        if (len == 0 || angle == 0) {
            return;
        }
        ax /= len;
        ay /= len;
        az /= len;
        double rad = angle * Math.PI / 180.0;
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float t = 1 - c;
        r[0] = t * ax * ax + c;
        r[1] = t * ax * ay - s * az;
        r[2] = t * ax * az + s * ay;
        r[4] = t * ax * ay + s * az;
        r[5] = t * ay * ay + c;
        r[6] = t * ay * az - s * ax;
        r[8] = t * ax * az - s * ay;
        r[9] = t * ay * az + s * ax;
        r[10] = t * az * az + c;
    }

    static void quatMatrix(float x, float y, float z, float w, float[] r) {
        float n = x * x + y * y + z * z + w * w;
        for (int i = 0; i < 16; i++) {
            r[i] = (i % 5 == 0) ? 1 : 0;
        }
        if (n == 0) {
            return;
        }
        float s = 2.0f / n;
        float xx = x * x * s;
        float yy = y * y * s;
        float zz = z * z * s;
        float xy = x * y * s;
        float xz = x * z * s;
        float yz = y * z * s;
        float wx = w * x * s;
        float wy = w * y * s;
        float wz = w * z * s;
        r[0] = 1 - yy - zz;
        r[1] = xy - wz;
        r[2] = xz + wy;
        r[4] = xy + wz;
        r[5] = 1 - xx - zz;
        r[6] = yz - wx;
        r[8] = xz - wy;
        r[9] = yz + wx;
        r[10] = 1 - xx - yy;
    }

    public void transform(float[] v) {
        if (v.length % 4 != 0) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < v.length; i += 4) {
            float x = v[i];
            float y = v[i + 1];
            float z = v[i + 2];
            float w = v[i + 3];
            v[i] = m[0] * x + m[1] * y + m[2] * z + m[3] * w;
            v[i + 1] = m[4] * x + m[5] * y + m[6] * z + m[7] * w;
            v[i + 2] = m[8] * x + m[9] * y + m[10] * z + m[11] * w;
            v[i + 3] = m[12] * x + m[13] * y + m[14] * z + m[15] * w;
        }
    }

    public void transform(VertexArray in, float[] out, boolean w) {
        int n = in.vertexCount;
        int cc = in.componentCount;
        float[] v = new float[4];
        for (int i = 0; i < n; i++) {
            v[0] = in.value(i, 0);
            v[1] = cc > 1 ? in.value(i, 1) : 0;
            v[2] = cc > 2 ? in.value(i, 2) : 0;
            v[3] = cc > 3 ? in.value(i, 3) : (w ? 1 : 0);
            for (int r = 0; r < 4; r++) {
                out[i * 4 + r] = m[r * 4] * v[0] + m[r * 4 + 1] * v[1] + m[r * 4 + 2] * v[2] + m[r * 4 + 3] * v[3];
            }
        }
    }
}
