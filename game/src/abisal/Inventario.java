package abisal;

/** What the player carries. Slots are limited like a real backpack. */
public final class Inventario {
    public static final int COBRE = 0, CUARZO = 1, CORAL = 2, PEZ = 3;
    public static final int N = 4;
    public static final String[] NOMBRE = { "Cobre", "Cuarzo", "Muestra de coral", "Burbujin crudo" };
    public static final int CAPACIDAD = 24;

    final int[] cant = new int[N];

    int total() {
        int t = 0;
        for (int i = 0; i < N; i++) {
            t += cant[i];
        }
        return t;
    }

    boolean meter(int item) {
        if (total() >= CAPACIDAD) {
            return false;
        }
        cant[item]++;
        return true;
    }
}
