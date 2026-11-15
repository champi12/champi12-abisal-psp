package javax.microedition.m3g;

public abstract class Transformable extends Object3D {
    float tx;
    float ty;
    float tz;
    float sx = 1;
    float sy = 1;
    float sz = 1;
    // orientation quaternion (x, y, z, w)
    float qx;
    float qy;
    float qz;
    float qw = 1;
    float[] generic;

    Transformable() {
    }

    public void setOrientation(float angle, float ax, float ay, float az) {
        if (angle != 0 && ax == 0 && ay == 0 && az == 0) {
            throw new IllegalArgumentException();
        }
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (len == 0 || angle == 0) {
            qx = qy = qz = 0;
            qw = 1;
            return;
        }
        double half = angle * Math.PI / 360.0;
        float s = (float) Math.sin(half) / len;
        qx = ax * s;
        qy = ay * s;
        qz = az * s;
        qw = (float) Math.cos(half);
    }

    public void preRotate(float angle, float ax, float ay, float az) {
        float ox = qx;
        float oy = qy;
        float oz = qz;
        float ow = qw;
        setOrientation(angle, ax, ay, az);
        quatMul(qx, qy, qz, qw, ox, oy, oz, ow);
    }

    public void postRotate(float angle, float ax, float ay, float az) {
        float ox = qx;
        float oy = qy;
        float oz = qz;
        float ow = qw;
        setOrientation(angle, ax, ay, az);
        quatMul(ox, oy, oz, ow, qx, qy, qz, qw);
    }

    private void quatMul(float ax, float ay, float az, float aw, float bx, float by, float bz, float bw) {
        qw = aw * bw - ax * bx - ay * by - az * bz;
        qx = aw * bx + ax * bw + ay * bz - az * by;
        qy = aw * by - ax * bz + ay * bw + az * bx;
        qz = aw * bz + ax * by - ay * bx + az * bw;
    }

    public void getOrientation(float[] out) {
        float w = qw;
        if (w > 1) {
            w = 1;
        }
        if (w < -1) {
            w = -1;
        }
        double half = Math.acos(w);
        float s = (float) Math.sin(half);
        if (s < 1e-6f) {
            out[0] = 0;
            out[1] = 0;
            out[2] = 0;
            out[3] = 0;
            return;
        }
        out[0] = (float) (half * 360.0 / Math.PI);
        out[1] = qx / s;
        out[2] = qy / s;
        out[3] = qz / s;
    }

    public void setScale(float x, float y, float z) {
        sx = x;
        sy = y;
        sz = z;
    }

    public void scale(float x, float y, float z) {
        sx *= x;
        sy *= y;
        sz *= z;
    }

    public void getScale(float[] out) {
        out[0] = sx;
        out[1] = sy;
        out[2] = sz;
    }

    public void setTranslation(float x, float y, float z) {
        tx = x;
        ty = y;
        tz = z;
    }

    public void translate(float x, float y, float z) {
        tx += x;
        ty += y;
        tz += z;
    }

    public void getTranslation(float[] out) {
        out[0] = tx;
        out[1] = ty;
        out[2] = tz;
    }

    public void setTransform(Transform t) {
        if (t == null) {
            generic = null;
        } else {
            if (generic == null) {
                generic = new float[16];
            }
            System.arraycopy(t.m, 0, generic, 0, 16);
        }
    }

    public void getTransform(Transform t) {
        if (generic == null) {
            t.setIdentity();
        } else {
            t.set(generic);
        }
    }

    public void getCompositeTransform(Transform t) {
        compose(t.m);
    }

    /** out = T * R * S * M */
    void compose(float[] out) {
        Transform.quatMatrix(qx, qy, qz, qw, out);
        for (int r = 0; r < 3; r++) {
            out[r * 4] *= sx;
            out[r * 4 + 1] *= sy;
            out[r * 4 + 2] *= sz;
        }
        out[3] = tx;
        out[7] = ty;
        out[11] = tz;
        if (generic != null) {
            Transform.mul(out, generic, out);
        }
    }

    boolean isIdentity() {
        return tx == 0 && ty == 0 && tz == 0 && sx == 1 && sy == 1 && sz == 1 && qx == 0 && qy == 0 && qz == 0
                && generic == null;
    }

    void setAnimatedProperty(int p, float[] v) {
        switch (p) {
            case AnimationTrack.TRANSLATION:
                tx = v[0];
                ty = v.length > 1 ? v[1] : 0;
                tz = v.length > 2 ? v[2] : 0;
                break;
            case AnimationTrack.SCALE:
                if (v.length >= 3) {
                    sx = v[0];
                    sy = v[1];
                    sz = v[2];
                } else {
                    sx = sy = sz = v[0];
                }
                break;
            case AnimationTrack.ORIENTATION:
                if (v.length >= 4) {
                    float n = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2] + v[3] * v[3]);
                    if (n > 0) {
                        qx = v[0] / n;
                        qy = v[1] / n;
                        qz = v[2] / n;
                        qw = v[3] / n;
                    }
                }
                break;
            default:
                super.setAnimatedProperty(p, v);
        }
    }

    void copyTransformable(Transformable f) {
        copyBase(f);
        tx = f.tx;
        ty = f.ty;
        tz = f.tz;
        sx = f.sx;
        sy = f.sy;
        sz = f.sz;
        qx = f.qx;
        qy = f.qy;
        qz = f.qz;
        qw = f.qw;
        if (f.generic != null) {
            generic = new float[16];
            System.arraycopy(f.generic, 0, generic, 0, 16);
        }
    }
}
