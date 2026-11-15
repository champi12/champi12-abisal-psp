package javax.microedition.m3g;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/** Reader for the binary M3G (JSR-184) file format. */
public class Loader {
    private static final byte[] ID = { (byte) 0xAB, 0x4A, 0x53, 0x52, 0x31, 0x38, 0x34, (byte) 0xBB, 0x0D, 0x0A,
            0x1A, 0x0A };

    private final Vector objects = new Vector();
    private final Vector referenced = new Vector();
    private byte[] d;
    private int p;

    private Loader() {
        objects.addElement(null);
    }

    public static Object3D[] load(String name) throws IOException {
        InputStream in = Loader.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException(name);
        }
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b, 0, b.length)) > 0) {
            bo.write(b, 0, n);
        }
        byte[] data = bo.toByteArray();
        return load(data, 0);
    }

    public static Object3D[] load(byte[] data, int offset) throws IOException {
        if (data == null) {
            throw new NullPointerException();
        }
        Loader l = new Loader();
        return l.parse(data, offset);
    }

    private Object3D[] parse(byte[] data, int off) throws IOException {
        for (int i = 0; i < ID.length; i++) {
            if (off + i >= data.length || data[off + i] != ID[i]) {
                return loadImage(data, off);
            }
        }
        int pos = off + ID.length;
        while (pos < data.length) {
            int comp = data[pos] & 0xff;
            int total = le32(data, pos + 1);
            int uncompressed = le32(data, pos + 5);
            int bodyLen = total - 13;
            if (bodyLen < 0 || pos + total > data.length) {
                throw new IOException("bad M3G section");
            }
            byte[] body;
            int bodyOff;
            if (comp == 0) {
                body = data;
                bodyOff = pos + 9;
            } else if (comp == 1) {
                body = j2me.G3D.inflate(data, pos + 9, bodyLen, uncompressed);
                if (body == null) {
                    throw new IOException("bad compressed section");
                }
                bodyOff = 0;
                bodyLen = body.length;
            } else {
                throw new IOException("unknown compression");
            }
            d = body;
            p = bodyOff;
            int end = bodyOff + bodyLen;
            while (p < end) {
                int type = u8();
                int len = le32(d, p);
                p += 4;
                int objEnd = p + len;
                Object3D o = readObject(type);
                objects.addElement(o);
                p = objEnd;
            }
            pos += total;
        }
        Vector roots = new Vector();
        for (int i = 1; i < objects.size(); i++) {
            Object o = objects.elementAt(i);
            if (o != null && !(o instanceof Header) && !referenced.contains(o)) {
                roots.addElement(o);
            }
        }
        Object3D[] r = new Object3D[roots.size()];
        roots.copyInto(r);
        return r;
    }

    private Object3D[] loadImage(byte[] data, int off) throws IOException {
        try {
            javax.microedition.lcdui.Image img = javax.microedition.lcdui.Image.createImage(data, off,
                    data.length - off);
            Object3D[] r = new Object3D[1];
            r[0] = new Image2D(javax.microedition.lcdui.LcduiAccess.hasAlpha(img) ? Image2D.RGBA : Image2D.RGB, img);
            return r;
        } catch (IllegalArgumentException e) {
            throw new IOException("not an M3G or image file");
        }
    }

    // ------------------------------------------------------------------ primitives

    static int le32(byte[] b, int o) {
        return (b[o] & 0xff) | ((b[o + 1] & 0xff) << 8) | ((b[o + 2] & 0xff) << 16) | ((b[o + 3] & 0xff) << 24);
    }

    private int u8() {
        return d[p++] & 0xff;
    }

    private boolean bool() {
        return u8() != 0;
    }

    private int i16() {
        int v = (d[p] & 0xff) | (d[p + 1] << 8);
        p += 2;
        return v;
    }

    private int u16() {
        return i16() & 0xffff;
    }

    private int i32() {
        int v = le32(d, p);
        p += 4;
        return v;
    }

    private float f32() {
        return Float.intBitsToFloat(i32());
    }

    private int rgb() {
        int r = u8();
        int g = u8();
        int b = u8();
        return (r << 16) | (g << 8) | b;
    }

    private int rgba() {
        int r = u8();
        int g = u8();
        int b = u8();
        int a = u8();
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private Object3D ref() throws IOException {
        int i = i32();
        if (i == 0) {
            return null;
        }
        if (i < 0 || i >= objects.size()) {
            throw new IOException("bad object reference " + i);
        }
        Object3D o = (Object3D) objects.elementAt(i);
        if (o != null && !referenced.contains(o)) {
            referenced.addElement(o);
        }
        return o;
    }

    private byte[] bytes() {
        int n = i32();
        byte[] b = new byte[n];
        System.arraycopy(d, p, b, 0, n);
        p += n;
        return b;
    }

    // ------------------------------------------------------------------ objects

    private Object3D readObject(int type) throws IOException {
        switch (type) {
            case 0:
                return readHeader();
            case 1:
                return readAnimationController();
            case 2:
                return readAnimationTrack();
            case 3:
                return readAppearance();
            case 4:
                return readBackground();
            case 5:
                return readCamera();
            case 6:
                return readCompositingMode();
            case 7:
                return readFog();
            case 8:
                return readPolygonMode();
            case 9: {
                Group g = new Group();
                readGroup(g);
                return g;
            }
            case 10:
                return readImage2D();
            case 11:
                return readTriangleStripArray();
            case 12:
                return readLight();
            case 13:
                return readMaterial();
            case 14: {
                Mesh m = new Mesh();
                readMesh(m);
                return m;
            }
            case 17:
                return readTexture2D();
            case 19:
                return readKeyframeSequence();
            case 20:
                return readVertexArray();
            case 21:
                return readVertexBuffer();
            case 22:
                return readWorld();
            case 255:
                throw new IOException("external references are not supported");
            default:
                System.out.println("M3G: skipping unsupported object type " + type);
                return null;
        }
    }

    private Object3D readHeader() {
        u8();
        u8();
        bool();
        i32();
        i32();
        return new Header();
    }

    private void readObject3D(Object3D o) throws IOException {
        o.userID = i32();
        int n = i32();
        for (int i = 0; i < n; i++) {
            Object3D t = ref();
            if (t instanceof AnimationTrack) {
                o.addAnimationTrack((AnimationTrack) t);
            }
        }
        int params = i32();
        for (int i = 0; i < params; i++) {
            i32();
            bytes();
        }
    }

    private void readTransformable(Transformable t) throws IOException {
        readObject3D(t);
        if (bool()) {
            t.tx = f32();
            t.ty = f32();
            t.tz = f32();
            t.sx = f32();
            t.sy = f32();
            t.sz = f32();
            float angle = f32();
            float ax = f32();
            float ay = f32();
            float az = f32();
            if (angle != 0 && (ax != 0 || ay != 0 || az != 0)) {
                t.setOrientation(angle, ax, ay, az);
            }
        }
        if (bool()) {
            float[] m = new float[16];
            for (int i = 0; i < 16; i++) {
                m[i] = f32();
            }
            t.generic = m;
        }
    }

    private void readNode(Node n) throws IOException {
        readTransformable(n);
        n.renderingEnabled = bool();
        n.pickingEnabled = bool();
        n.alphaFactor = u8() / 255.0f;
        n.scope = i32();
        if (bool()) {
            n.zTarget = u8();
            n.yTarget = u8();
            n.zRef = (Node) ref();
            n.yRef = (Node) ref();
        }
    }

    private void readGroup(Group g) throws IOException {
        readNode(g);
        int n = i32();
        for (int i = 0; i < n; i++) {
            Node c = (Node) ref();
            if (c != null && c.parent == null) {
                g.addChild(c);
            }
        }
    }

    private Object3D readWorld() throws IOException {
        World w = new World();
        readGroup(w);
        w.activeCamera = (Camera) ref();
        w.background = (Background) ref();
        return w;
    }

    private Object3D readCamera() throws IOException {
        Camera c = new Camera();
        readNode(c);
        int type = u8();
        if (type == Camera.GENERIC) {
            for (int i = 0; i < 16; i++) {
                c.matrix[i] = f32();
            }
            c.projection = Camera.GENERIC;
        } else {
            c.fovy = f32();
            c.aspect = f32();
            c.near = f32();
            c.far = f32();
            c.projection = type;
        }
        return c;
    }

    private Object3D readLight() throws IOException {
        Light l = new Light();
        readNode(l);
        l.attConst = f32();
        l.attLinear = f32();
        l.attQuad = f32();
        l.color = rgb();
        l.mode = u8();
        l.intensity = f32();
        l.spotAngle = f32();
        l.spotExponent = f32();
        return l;
    }

    private Object3D readBackground() throws IOException {
        Background b = new Background();
        readObject3D(b);
        b.color = rgba();
        b.image = (Image2D) ref();
        b.modeX = u8();
        b.modeY = u8();
        b.cropX = i32();
        b.cropY = i32();
        b.cropW = i32();
        b.cropH = i32();
        b.depthClear = bool();
        b.colorClear = bool();
        return b;
    }

    private Object3D readAppearance() throws IOException {
        Appearance a = new Appearance();
        readObject3D(a);
        a.layer = (byte) u8();
        a.compositing = (CompositingMode) ref();
        a.fog = (Fog) ref();
        a.polygon = (PolygonMode) ref();
        a.material = (Material) ref();
        int n = i32();
        for (int i = 0; i < n; i++) {
            Texture2D t = (Texture2D) ref();
            if (i < a.textures.length) {
                a.textures[i] = t;
            }
        }
        return a;
    }

    private Object3D readCompositingMode() throws IOException {
        CompositingMode c = new CompositingMode();
        readObject3D(c);
        c.depthTest = bool();
        c.depthWrite = bool();
        c.colorWrite = bool();
        c.alphaWrite = bool();
        c.blending = u8();
        c.alphaThreshold = u8() / 255.0f;
        c.offsetFactor = f32();
        c.offsetUnits = f32();
        return c;
    }

    private Object3D readFog() throws IOException {
        Fog f = new Fog();
        readObject3D(f);
        f.color = rgb();
        f.mode = u8();
        if (f.mode == Fog.EXPONENTIAL) {
            f.density = f32();
        } else {
            f.near = f32();
            f.far = f32();
        }
        return f;
    }

    private Object3D readPolygonMode() throws IOException {
        PolygonMode m = new PolygonMode();
        readObject3D(m);
        m.culling = u8();
        m.shading = u8();
        m.winding = u8();
        m.twoSided = bool();
        m.localCamera = bool();
        m.perspectiveCorrection = bool();
        return m;
    }

    private Object3D readMaterial() throws IOException {
        Material m = new Material();
        readObject3D(m);
        m.ambient = rgb();
        m.diffuse = rgba();
        m.emissive = rgb();
        m.specular = rgb();
        m.shininess = f32();
        m.tracking = bool();
        return m;
    }

    private Object3D readImage2D() throws IOException {
        Image2D tmp = new Image2D(Image2D.RGB, 1, 1);
        readObject3D(tmp);
        int format = u8();
        boolean mutable = bool();
        int w = i32();
        int h = i32();
        Image2D img;
        if (mutable) {
            img = new Image2D(format, w, h);
        } else {
            byte[] palette = bytes();
            byte[] pixels = bytes();
            if (palette.length > 0) {
                img = new Image2D(format, w, h, pixels, palette);
            } else {
                img = new Image2D(format, w, h, pixels);
            }
        }
        img.copyBase(tmp);
        return img;
    }

    private Object3D readTexture2D() throws IOException {
        Holder h = new Holder();
        readTransformable(h);
        Image2D img = (Image2D) ref();
        Texture2D t = new Texture2D(img);
        t.copyTransformable(h);
        t.blendColor = rgb();
        t.blending = u8();
        t.wrapS = u8();
        t.wrapT = u8();
        t.levelFilter = u8();
        t.imageFilter = u8();
        return t;
    }

    private Object3D readKeyframeSequence() throws IOException {
        Holder h = new Holder();
        readObject3D(h);
        int interp = u8();
        int repeat = u8();
        int encoding = u8();
        int duration = i32();
        int first = i32();
        int last = i32();
        int comps = i32();
        int frames = i32();
        KeyframeSequence k = new KeyframeSequence(frames, comps, interp);
        k.copyBase(h);
        k.repeatMode = repeat;
        k.duration = duration;
        float[] v = new float[comps];
        if (encoding == 0) {
            for (int i = 0; i < frames; i++) {
                int t = i32();
                for (int c = 0; c < comps; c++) {
                    v[c] = f32();
                }
                k.setKeyframe(i, t, v);
            }
        } else {
            float[] bias = new float[comps];
            float[] scale = new float[comps];
            for (int c = 0; c < comps; c++) {
                bias[c] = f32();
            }
            for (int c = 0; c < comps; c++) {
                scale[c] = f32();
            }
            for (int i = 0; i < frames; i++) {
                int t = i32();
                for (int c = 0; c < comps; c++) {
                    if (encoding == 1) {
                        v[c] = bias[c] + scale[c] * (u8() / 255.0f);
                    } else {
                        v[c] = bias[c] + scale[c] * (u16() / 65535.0f);
                    }
                }
                k.setKeyframe(i, t, v);
            }
        }
        if (first < frames && last < frames) {
            k.first = first;
            k.last = last;
        }
        return k;
    }

    private Object3D readAnimationController() throws IOException {
        AnimationController c = new AnimationController();
        readObject3D(c);
        c.speed = f32();
        c.weight = f32();
        c.activeStart = i32();
        c.activeEnd = i32();
        c.refSequenceTime = f32();
        c.refWorldTime = i32();
        return c;
    }

    private Object3D readAnimationTrack() throws IOException {
        Holder h = new Holder();
        readObject3D(h);
        KeyframeSequence ks = (KeyframeSequence) ref();
        AnimationController ac = (AnimationController) ref();
        int prop = i32();
        AnimationTrack t = new AnimationTrack(ks, prop);
        t.copyBase(h);
        t.controller = ac;
        return t;
    }

    private Object3D readVertexArray() throws IOException {
        Holder h = new Holder();
        readObject3D(h);
        int size = u8();
        int comps = u8();
        int encoding = u8();
        int count = u16();
        VertexArray a = new VertexArray(count, comps, size);
        a.copyBase(h);
        int n = count * comps;
        if (size == 1) {
            byte[] b = a.bytes;
            for (int i = 0; i < n; i++) {
                b[i] = (byte) u8();
            }
            if (encoding == 1) {
                for (int i = comps; i < n; i++) {
                    b[i] = (byte) (b[i] + b[i - comps]);
                }
            }
        } else {
            short[] s = a.shorts;
            for (int i = 0; i < n; i++) {
                s[i] = (short) i16();
            }
            if (encoding == 1) {
                for (int i = comps; i < n; i++) {
                    s[i] = (short) (s[i] + s[i - comps]);
                }
            }
        }
        return a;
    }

    private Object3D readVertexBuffer() throws IOException {
        VertexBuffer vb = new VertexBuffer();
        readObject3D(vb);
        vb.defaultColor = rgba();
        VertexArray pos = (VertexArray) ref();
        float[] bias = new float[3];
        bias[0] = f32();
        bias[1] = f32();
        bias[2] = f32();
        float scale = f32();
        vb.setPositions(pos, scale, bias);
        vb.setNormals((VertexArray) ref());
        vb.setColors((VertexArray) ref());
        int n = i32();
        for (int i = 0; i < n; i++) {
            VertexArray tc = (VertexArray) ref();
            float[] tb = new float[3];
            tb[0] = f32();
            tb[1] = f32();
            tb[2] = f32();
            float ts = f32();
            if (i < 2) {
                vb.setTexCoords(i, tc, ts, tb);
            }
        }
        return vb;
    }

    private Object3D readTriangleStripArray() throws IOException {
        Holder h = new Holder();
        readObject3D(h);
        int encoding = u8();
        int start = 0;
        int[] indices = null;
        switch (encoding) {
            case 0:
                start = i32();
                break;
            case 1:
                start = u8();
                break;
            case 2:
                start = u16();
                break;
            case 128: {
                int n = i32();
                indices = new int[n];
                for (int i = 0; i < n; i++) {
                    indices[i] = i32();
                }
                break;
            }
            case 129: {
                int n = i32();
                indices = new int[n];
                for (int i = 0; i < n; i++) {
                    indices[i] = u8();
                }
                break;
            }
            case 130: {
                int n = i32();
                indices = new int[n];
                for (int i = 0; i < n; i++) {
                    indices[i] = u16();
                }
                break;
            }
            default:
                throw new IOException("bad index encoding");
        }
        int ns = i32();
        int[] lengths = new int[ns];
        for (int i = 0; i < ns; i++) {
            lengths[i] = i32();
        }
        TriangleStripArray t = indices == null ? new TriangleStripArray(start, lengths)
                : new TriangleStripArray(indices, lengths);
        t.copyBase(h);
        return t;
    }

    private void readMesh(Mesh m) throws IOException {
        readNode(m);
        m.vertices = (VertexBuffer) ref();
        int n = i32();
        m.submeshes = new IndexBuffer[n];
        m.appearances = new Appearance[n];
        for (int i = 0; i < n; i++) {
            m.submeshes[i] = (IndexBuffer) ref();
            m.appearances[i] = (Appearance) ref();
        }
    }

    /** Placeholder object used to read Object3D/Transformable fields before construction. */
    static final class Holder extends Transformable {
        Object3D duplicateImpl() {
            return new Holder();
        }
    }

    static final class Header extends Object3D {
        Object3D duplicateImpl() {
            return new Header();
        }
    }
}
