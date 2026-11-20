package javax.microedition.m3g;

public class Camera extends Node {
    public static final int GENERIC = 48;
    public static final int PARALLEL = 49;
    public static final int PERSPECTIVE = 50;

    int projection = GENERIC;
    float fovy;
    float aspect;
    float near;
    float far;
    final float[] matrix = new float[16];

    public Camera() {
        for (int i = 0; i < 16; i++) {
            matrix[i] = (i % 5 == 0) ? 1 : 0;
        }
    }

    public void setParallel(float fovy, float aspect, float near, float far) {
        projection = PARALLEL;
        this.fovy = fovy;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
    }

    public void setPerspective(float fovy, float aspect, float near, float far) {
        if (fovy <= 0 || fovy >= 180 || aspect <= 0 || near <= 0 || far <= 0 || near == far) {
            throw new IllegalArgumentException();
        }
        projection = PERSPECTIVE;
        this.fovy = fovy;
        this.aspect = aspect;
        this.near = near;
        this.far = far;
    }

    public void setGeneric(Transform t) {
        projection = GENERIC;
        System.arraycopy(t.m, 0, matrix, 0, 16);
    }

    public int getProjection(float[] params) {
        if (params != null && projection != GENERIC) {
            params[0] = fovy;
            params[1] = aspect;
            params[2] = near;
            params[3] = far;
        }
        return projection;
    }

    public int getProjection(Transform t) {
        if (t != null) {
            projectionMatrix(t.m);
        }
        return projection;
    }

    void projectionMatrix(float[] p) {
        for (int i = 0; i < 16; i++) {
            p[i] = 0;
        }
        if (projection == PERSPECTIVE) {
            float h = j2me.FMath.tan(fovy * (j2me.FMath.PI / 360f));
            float w = aspect * h;
            p[0] = 1 / w;
            p[5] = 1 / h;
            p[10] = -(far + near) / (far - near);
            p[11] = -2 * near * far / (far - near);
            p[14] = -1;
        } else if (projection == PARALLEL) {
            float h = fovy;
            float w = aspect * h;
            p[0] = 2 / w;
            p[5] = 2 / h;
            p[10] = -2 / (far - near);
            p[11] = -(near + far) / (far - near);
            p[15] = 1;
        } else {
            System.arraycopy(matrix, 0, p, 0, 16);
        }
    }

    void setAnimatedProperty(int p, float[] v) {
        if (p == AnimationTrack.FIELD_OF_VIEW) {
            fovy = v[0];
        } else if (p == AnimationTrack.NEAR_DISTANCE) {
            near = v[0];
        } else if (p == AnimationTrack.FAR_DISTANCE) {
            far = v[0];
        } else {
            super.setAnimatedProperty(p, v);
        }
    }

    Object3D duplicateImpl() {
        Camera c = new Camera();
        c.copyNode(this);
        c.projection = projection;
        c.fovy = fovy;
        c.aspect = aspect;
        c.near = near;
        c.far = far;
        System.arraycopy(matrix, 0, c.matrix, 0, 16);
        return c;
    }
}
