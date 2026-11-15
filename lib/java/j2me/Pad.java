package j2me;

/** Direct access to the PSP analog stick (the PC test build reads it from the key script). */
public final class Pad {
    private Pad() {
    }

    /** (x << 8) | y, each axis 0..255 with 128 at rest. */
    public static native int analog();
}
