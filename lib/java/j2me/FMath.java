package j2me;

/**
 * Single-precision math in native code. The PSP's FPU only does float: every
 * java.lang.Math call works in double and is emulated in software, which is many times
 * slower. Hot code (per frame, per vertex) should use these instead.
 */
public final class FMath {
    public static final float PI = 3.14159265f;
    public static final float RAD = PI / 180f;

    private FMath() {
    }

    public static native float sqrt(float x);

    public static native float sin(float x);

    public static native float cos(float x);

    public static native float atan2(float y, float x);

    public static native float tan(float x);

    /** floor for float, without going through double */
    public static int floor(float x) {
        int i = (int) x;
        return x < i ? i - 1 : i;
    }
}
