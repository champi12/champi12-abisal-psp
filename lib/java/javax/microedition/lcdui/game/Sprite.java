package javax.microedition.lcdui.game;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Sprite extends Layer {
    public static final int TRANS_NONE = 0;
    public static final int TRANS_ROT90 = 5;
    public static final int TRANS_ROT180 = 3;
    public static final int TRANS_ROT270 = 6;
    public static final int TRANS_MIRROR = 2;
    public static final int TRANS_MIRROR_ROT90 = 7;
    public static final int TRANS_MIRROR_ROT180 = 1;
    public static final int TRANS_MIRROR_ROT270 = 4;

    private Image image;
    private int frameW;
    private int frameH;
    private int cols;
    private int rawFrames;
    private int[] sequence;
    private int seqIndex;
    private int transform;
    private int refX;
    private int refY;

    public Sprite(Image img) {
        this(img, img.getWidth(), img.getHeight());
    }

    public Sprite(Image img, int fw, int fh) {
        super(fw, fh);
        setImage(img, fw, fh);
    }

    public Sprite(Sprite s) {
        super(s.width, s.height);
        image = s.image;
        frameW = s.frameW;
        frameH = s.frameH;
        cols = s.cols;
        rawFrames = s.rawFrames;
        sequence = s.sequence;
        seqIndex = s.seqIndex;
        transform = s.transform;
        x = s.x;
        y = s.y;
        visible = s.visible;
    }

    public void setImage(Image img, int fw, int fh) {
        if (fw <= 0 || fh <= 0 || img.getWidth() % fw != 0 || img.getHeight() % fh != 0) {
            throw new IllegalArgumentException();
        }
        image = img;
        frameW = fw;
        frameH = fh;
        cols = img.getWidth() / fw;
        int n = cols * (img.getHeight() / fh);
        if (sequence == null || n < rawFrames) {
            sequence = null;
            seqIndex = 0;
        }
        rawFrames = n;
        width = (transform & 4) != 0 ? fh : fw;
        height = (transform & 4) != 0 ? fw : fh;
    }

    public void defineReferencePixel(int x, int y) {
        refX = x;
        refY = y;
    }

    public void setRefPixelPosition(int x, int y) {
        this.x = x - refX;
        this.y = y - refY;
    }

    public int getRefPixelX() {
        return x + refX;
    }

    public int getRefPixelY() {
        return y + refY;
    }

    public void setFrame(int i) {
        if (i < 0 || i >= getFrameSequenceLength()) {
            throw new IndexOutOfBoundsException();
        }
        seqIndex = i;
    }

    public final int getFrame() {
        return seqIndex;
    }

    public int getRawFrameCount() {
        return rawFrames;
    }

    public int getFrameSequenceLength() {
        return sequence != null ? sequence.length : rawFrames;
    }

    public void nextFrame() {
        seqIndex = (seqIndex + 1) % getFrameSequenceLength();
    }

    public void prevFrame() {
        seqIndex = (seqIndex + getFrameSequenceLength() - 1) % getFrameSequenceLength();
    }

    public void setFrameSequence(int[] seq) {
        if (seq == null) {
            sequence = null;
        } else {
            sequence = new int[seq.length];
            System.arraycopy(seq, 0, sequence, 0, seq.length);
        }
        seqIndex = 0;
    }

    public void setTransform(int t) {
        transform = t;
        width = (t & 4) != 0 ? frameH : frameW;
        height = (t & 4) != 0 ? frameW : frameH;
    }

    public final void paint(Graphics g) {
        if (!visible) {
            return;
        }
        int f = sequence != null ? sequence[seqIndex] : seqIndex;
        int sx = (f % cols) * frameW;
        int sy = (f / cols) * frameH;
        g.drawRegion(image, sx, sy, frameW, frameH, transform, x, y, Graphics.TOP | Graphics.LEFT);
    }

    public final boolean collidesWith(Sprite s, boolean pixelLevel) {
        if (!visible || !s.visible) {
            return false;
        }
        return x < s.x + s.width && s.x < x + width && y < s.y + s.height && s.y < y + height;
    }
}
