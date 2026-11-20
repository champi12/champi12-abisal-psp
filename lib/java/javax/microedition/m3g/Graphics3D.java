package javax.microedition.m3g;

import java.util.Hashtable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.LcduiAccess;
import j2me.G3D;

public class Graphics3D {
    public static final int ANTIALIAS = 2;
    public static final int DITHER = 4;
    public static final int TRUE_COLOR = 8;
    public static final int OVERWRITE = 16;

    private static Graphics3D instance;
    private Object target;
    private int vx;
    private int vy;
    private int vw;
    private int vh;
    private float depthNear = 0;
    private float depthFar = 1;
    private Camera camera;
    private final float[] cameraWorld = new float[16];
    private final float[] view = new float[16];
    private final float[] scratchWorld = new float[16];
    private final float[] proj = new float[16];
    private Light[] lights = new Light[8];
    private float[][] lightWorld = new float[8][];
    private int lightCount;
    private final RenderQueue queue = new RenderQueue();
    private final float[] mv = new float[16];
    private final int[] state = new int[32];
    private final float[] lightData = new float[8 * 16];
    private boolean lightsDirty = true;

    private Graphics3D() {
        for (int i = 0; i < 16; i++) {
            view[i] = cameraWorld[i] = (i % 5 == 0) ? 1 : 0;
        }
    }

    public static Graphics3D getInstance() {
        if (instance == null) {
            instance = new Graphics3D();
        }
        return instance;
    }

    public void bindTarget(Object t) {
        bindTarget(t, true, 0);
    }

    public void bindTarget(Object t, boolean depth, int hints) {
        if (t == null) {
            throw new NullPointerException();
        }
        if (target != null) {
            throw new IllegalStateException("target already bound");
        }
        int w;
        int h;
        int cx;
        int cy;
        int cw;
        int ch;
        if (t instanceof Graphics) {
            Graphics g = (Graphics) t;
            if (LcduiAccess.target(g) != null) {
                w = LcduiAccess.target(g).getWidth();
                h = LcduiAccess.target(g).getHeight();
            } else {
                w = j2me.Gfx.screenWidth();
                h = j2me.Gfx.screenHeight();
            }
            cx = LcduiAccess.clipX(g);
            cy = LcduiAccess.clipY(g);
            cw = LcduiAccess.clipW(g);
            ch = LcduiAccess.clipH(g);
        } else if (t instanceof Image2D) {
            w = ((Image2D) t).width;
            h = ((Image2D) t).height;
            cx = 0;
            cy = 0;
            cw = w;
            ch = h;
        } else {
            throw new IllegalArgumentException();
        }
        target = t;
        queue.clear();
        vx = 0;
        vy = 0;
        vw = w;
        vh = h;
        G3D.begin(vx, vy, vw, vh, cx, cy, cw, ch);
        lightsDirty = true;
    }

    public Object getTarget() {
        return target;
    }

    /**
     * Batch mode (extension for the PSP): render(Node, Transform) only queues the meshes;
     * the whole frame is set up (projection, lights) and drawn once, sorted by layer, when
     * the target is released. Saves the per-call setup that dominated the frame time.
     */
    private boolean lote;

    public void setModoLote(boolean on) {
        lote = on;
    }

    public void releaseTarget() {
        if (target != null) {
            if (lote && queue.count > 0) {
                flush();
            }
            G3D.end();
            target = null;
        }
    }

    public boolean isDepthBufferEnabled() {
        return true;
    }

    public int getHints() {
        return 0;
    }

    public void setViewport(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException();
        }
        vx = x;
        vy = y;
        vw = w;
        vh = h;
        if (target != null) {
            G3D.viewport(x, y, w, h);
        }
    }

    public int getViewportX() {
        return vx;
    }

    public int getViewportY() {
        return vy;
    }

    public int getViewportWidth() {
        return vw;
    }

    public int getViewportHeight() {
        return vh;
    }

    public void setDepthRange(float n, float f) {
        depthNear = n;
        depthFar = f;
    }

    public float getDepthRangeNear() {
        return depthNear;
    }

    public float getDepthRangeFar() {
        return depthFar;
    }

    public void clear(Background bg) {
        checkTarget();
        if (bg == null) {
            G3D.clear(0, true, true);
        } else {
            G3D.clear(bg.color, bg.colorClear, bg.depthClear);
        }
    }

    private void checkTarget() {
        if (target == null) {
            throw new IllegalStateException("no target bound");
        }
    }

    public void setCamera(Camera c, Transform t) {
        camera = c;
        if (t == null) {
            for (int i = 0; i < 16; i++) {
                cameraWorld[i] = (i % 5 == 0) ? 1 : 0;
            }
        } else {
            System.arraycopy(t.m, 0, cameraWorld, 0, 16);
        }
        if (!Transform.invert(cameraWorld, view)) {
            throw new ArithmeticException("camera transform not invertible");
        }
        lightsDirty = true;
    }

    public Camera getCamera(Transform t) {
        if (t != null) {
            t.set(cameraWorld);
        }
        return camera;
    }

    public int addLight(Light l, Transform t) {
        if (l == null) {
            throw new NullPointerException();
        }
        if (lightCount == lights.length) {
            Light[] nl = new Light[lightCount * 2];
            float[][] nw = new float[lightCount * 2][];
            System.arraycopy(lights, 0, nl, 0, lightCount);
            System.arraycopy(lightWorld, 0, nw, 0, lightCount);
            lights = nl;
            lightWorld = nw;
        }
        lights[lightCount] = l;
        float[] w = new float[16];
        if (t == null) {
            for (int i = 0; i < 16; i++) {
                w[i] = (i % 5 == 0) ? 1 : 0;
            }
        } else {
            System.arraycopy(t.m, 0, w, 0, 16);
        }
        lightWorld[lightCount] = w;
        lightsDirty = true;
        return lightCount++;
    }

    public void setLight(int i, Light l, Transform t) {
        lights[i] = l;
        if (t != null) {
            System.arraycopy(t.m, 0, lightWorld[i], 0, 16);
        }
        lightsDirty = true;
    }

    public void resetLights() {
        for (int i = 0; i < lightCount; i++) {
            lights[i] = null;
        }
        lightCount = 0;
        lightsDirty = true;
    }

    public int getLightCount() {
        return lightCount;
    }

    public Light getLight(int i, Transform t) {
        if (t != null) {
            t.set(lightWorld[i]);
        }
        return lights[i];
    }

    public static Hashtable getProperties() {
        Hashtable h = new Hashtable();
        h.put("supportAntialiasing", Boolean.FALSE);
        h.put("supportTrueColor", Boolean.TRUE);
        h.put("supportDithering", Boolean.FALSE);
        h.put("supportMipmapping", Boolean.FALSE);
        h.put("supportPerspectiveCorrection", Boolean.TRUE);
        h.put("supportLocalCameraLighting", Boolean.FALSE);
        h.put("maxLights", new Integer(8));
        h.put("maxViewportWidth", new Integer(512));
        h.put("maxViewportHeight", new Integer(512));
        h.put("maxViewportDimension", new Integer(512));
        h.put("maxTextureDimension", new Integer(512));
        h.put("maxSpriteCropDimension", new Integer(512));
        h.put("maxTransformsPerVertex", new Integer(1));
        h.put("numTextureUnits", new Integer(1));
        return h;
    }

    // ------------------------------------------------------------------ rendering

    private void setupFrame() {
        if (camera != null) {
            camera.projectionMatrix(proj);
        } else {
            for (int i = 0; i < 16; i++) {
                proj[i] = (i % 5 == 0) ? 1 : 0;
            }
        }
        G3D.setProjection(proj);
        if (lightsDirty) {
            uploadLights();
            lightsDirty = false;
        }
    }

    private void uploadLights() {
        int n = 0;
        float[] d = lightData;
        float[] t = new float[16];
        for (int i = 0; i < lightCount && n < 8; i++) {
            Light l = lights[i];
            if (l == null || !l.renderingEnabled) {
                continue;
            }
            Transform.mulInto(view, lightWorld[i], t);
            int o = n * 16;
            d[o] = l.mode;
            float in = l.intensity;
            d[o + 1] = ((l.color >> 16) & 0xff) / 255.0f * in;
            d[o + 2] = ((l.color >> 8) & 0xff) / 255.0f * in;
            d[o + 3] = (l.color & 0xff) / 255.0f * in;
            d[o + 4] = t[3];
            d[o + 5] = t[7];
            d[o + 6] = t[11];
            // light points along its local -Z axis
            float dx = -t[2];
            float dy = -t[6];
            float dz = -t[10];
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0) {
                dx /= len;
                dy /= len;
                dz /= len;
            }
            d[o + 7] = dx;
            d[o + 8] = dy;
            d[o + 9] = dz;
            d[o + 10] = l.attConst;
            d[o + 11] = l.attLinear;
            d[o + 12] = l.attQuad;
            d[o + 13] = l.spotAngle;
            d[o + 14] = l.spotExponent;
            n++;
        }
        G3D.setLights(n, d);
    }

    public void render(World world) {
        checkTarget();
        if (world == null) {
            throw new NullPointerException();
        }
        Camera c = world.activeCamera;
        if (c == null) {
            throw new IllegalStateException("no active camera");
        }
        clear(world.background);
        Transform ct = new Transform();
        c.getTransformTo(world, ct);
        Camera savedCam = camera;
        setCamera(c, ct);
        resetLights();
        collectLights(world, new float[16], true);
        queue.clear();
        float[] id = new float[16];
        for (int i = 0; i < 16; i++) {
            id[i] = (i % 5 == 0) ? 1 : 0;
        }
        world.render(queue, id, world.alphaFactor, -1);
        flush();
        camera = savedCam;
    }

    private void collectLights(Node n, float[] parent, boolean root) {
        float[] w = new float[16];
        if (root) {
            for (int i = 0; i < 16; i++) {
                w[i] = (i % 5 == 0) ? 1 : 0;
            }
        } else {
            n.compose(w);
            Transform.mulInto(parent, w, w);
        }
        if (n instanceof Light) {
            if (n.renderingEnabled) {
                Transform t = new Transform();
                t.set(w);
                addLight((Light) n, t);
            }
        } else if (n instanceof Group) {
            Group g = (Group) n;
            for (int i = 0; i < g.childCount; i++) {
                collectLights(g.children[i], w, false);
            }
        }
    }

    public void render(Node node, Transform t) {
        checkTarget();
        if (node == null) {
            throw new NullPointerException();
        }
        // the queue copies the matrix, so one scratch array is enough (no garbage per call)
        float[] w = scratchWorld;
        if (t == null) {
            for (int i = 0; i < 16; i++) {
                w[i] = (i % 5 == 0) ? 1 : 0;
            }
        } else {
            System.arraycopy(t.m, 0, w, 0, 16);
        }
        if (lote) {
            if (node.renderingEnabled) {
                node.render(queue, w, node.alphaFactor, -1);
            }
            return;
        }
        queue.clear();
        if (node.renderingEnabled) {
            node.render(queue, w, node.alphaFactor, -1);
        }
        flush();
    }

    public void render(VertexBuffer vb, IndexBuffer ib, Appearance ap, Transform t) {
        render(vb, ib, ap, t, -1);
    }

    public void render(VertexBuffer vb, IndexBuffer ib, Appearance ap, Transform t, int scope) {
        checkTarget();
        Mesh m = new Mesh(vb, ib, ap);
        render(m, t);
    }

    private void flush() {
        if (queue.count == 0) {
            return;
        }
        setupFrame();
        queue.sort();
        for (int i = 0; i < queue.count; i++) {
            drawSubmesh(queue.meshes[i], queue.subs[i], queue.worlds[i], queue.alphas[i]);
        }
        queue.clear();
    }

    private void drawSubmesh(Mesh mesh, int sub, float[] world, float alpha) {
        VertexBuffer vb = mesh.vertices;
        int[] verts = vb.vertices();
        if (verts == null) {
            return;
        }
        IndexBuffer ib = mesh.submeshes[sub];
        Appearance ap = mesh.appearances[sub];
        Transform.mulInto(view, world, mv);
        int[] s = state;
        CompositingMode cm = ap.compositing;
        PolygonMode pm = ap.polygon;
        Material mat = ap.material;
        int flags = 0;
        if (cm == null) {
            s[0] = CompositingMode.REPLACE;
            s[1] = 0;
            flags |= 1 | 2 | 4 | 8;
        } else {
            s[0] = cm.blending;
            s[1] = (int) (cm.alphaThreshold * 255 + 0.5f);
            if (cm.depthTest) {
                flags |= 1;
            }
            if (cm.depthWrite) {
                flags |= 2;
            }
            if (cm.colorWrite) {
                flags |= 4;
            }
            if (cm.alphaWrite) {
                flags |= 8;
            }
        }
        if (pm == null) {
            s[3] = PolygonMode.CULL_BACK;
            s[4] = PolygonMode.WINDING_CCW;
            flags |= 128;
        } else {
            s[3] = pm.culling;
            s[4] = pm.winding;
            if (pm.twoSided) {
                flags |= 64;
            }
            if (pm.shading == PolygonMode.SHADE_SMOOTH) {
                flags |= 128;
            }
            if (pm.perspectiveCorrection) {
                flags |= 512;
            }
        }
        if (mat != null) {
            flags |= 16;
            if (mat.tracking) {
                flags |= 32;
            }
            s[5] = mat.ambient;
            s[6] = mat.diffuse;
            s[7] = mat.emissive;
            s[8] = mat.specular;
            s[9] = Float.floatToIntBits(mat.shininess);
        }
        Texture2D tex = ap.textures[0];
        int[] texPix = null;
        int tw = 0;
        int th = 0;
        if (tex != null && (vb.cacheFlags & VertexBuffer.HAS_TEX) != 0) {
            flags |= 256;
            Image2D img = tex.image;
            texPix = img.pixels;
            tw = img.width;
            th = img.height;
            s[10] = tex.blending;
            s[11] = tex.blendColor;
            s[12] = tex.wrapS;
            s[13] = tex.wrapT;
            s[14] = tex.imageFilter;
            s[17] = img.hasAlpha() ? 1 : 0;
            s[18] = img.format;
        }
        s[2] = flags;
        s[15] = (int) (alpha * 255 + 0.5f);
        s[16] = vb.cacheFlags;
        Fog fog = ap.fog;
        if (fog != null) {
            s[19] = fog.mode;
            s[20] = fog.color;
            s[21] = Float.floatToIntBits(fog.density);
            s[22] = Float.floatToIntBits(fog.near);
            s[23] = Float.floatToIntBits(fog.far);
        } else {
            s[19] = 0;
        }
        G3D.draw(verts, vb.positions.vertexCount, ib.triangles, ib.triangleIndexCount, mv, s, texPix, tw, th);
    }
}
