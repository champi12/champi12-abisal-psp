package j2me;

/** Persistent storage for record stores (files in the save directory). */
public final class Storage {
    private Storage() {
    }

    public static native byte[] read(String name);

    public static native boolean write(String name, byte[] data);

    public static native boolean delete(String name);
}
