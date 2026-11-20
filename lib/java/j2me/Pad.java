package j2me;

/** Direct access to the PSP analog stick (the PC test build reads it from the key script). */
public final class Pad {
    private Pad() {
    }

    /** (x << 8) | y, each axis 0..255 with 128 at rest. */
    public static native int analog();

    /** false: the game redraws the whole screen every frame, so the PSP renderer can skip
     * copying the last frame into the new back buffer */
    public static native void conservarPantalla(boolean on);

    /** watchdog: what the game is doing (see fase_java_nombre in plat_psp.c) */
    public static native void fase(int f);

    /** free system memory outside the Java heap, in KB (0 on the PC build) */
    public static native int memLibre();

    /** firmware version as reported by the kernel (0 on the PC build) */
    public static native int firmware();
}
