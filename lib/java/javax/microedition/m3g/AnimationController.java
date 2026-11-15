package javax.microedition.m3g;

public class AnimationController extends Object3D {
    int activeStart;
    int activeEnd;
    float speed = 1;
    float weight = 1;
    float refSequenceTime;
    int refWorldTime;

    public AnimationController() {
    }

    public void setActiveInterval(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException();
        }
        activeStart = start;
        activeEnd = end;
    }

    public int getActiveIntervalStart() {
        return activeStart;
    }

    public int getActiveIntervalEnd() {
        return activeEnd;
    }

    public void setSpeed(float s, int worldTime) {
        refSequenceTime = sequenceTime(worldTime);
        refWorldTime = worldTime;
        speed = s;
    }

    public float getSpeed() {
        return speed;
    }

    public void setPosition(float seqTime, int worldTime) {
        refSequenceTime = seqTime;
        refWorldTime = worldTime;
    }

    public float getPosition(int worldTime) {
        return sequenceTime(worldTime);
    }

    public int getRefWorldTime() {
        return refWorldTime;
    }

    public void setWeight(float w) {
        if (w < 0) {
            throw new IllegalArgumentException();
        }
        weight = w;
    }

    public float getWeight() {
        return weight;
    }

    boolean isActive(int t) {
        return activeStart == activeEnd || (t >= activeStart && t < activeEnd);
    }

    float sequenceTime(int worldTime) {
        return refSequenceTime + speed * (worldTime - refWorldTime);
    }

    Object3D duplicateImpl() {
        AnimationController c = new AnimationController();
        c.copyBase(this);
        c.activeStart = activeStart;
        c.activeEnd = activeEnd;
        c.speed = speed;
        c.weight = weight;
        c.refSequenceTime = refSequenceTime;
        c.refWorldTime = refWorldTime;
        return c;
    }
}
