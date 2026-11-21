package abisal;

/** What the player carries (limited backpack) plus the tools already built. */
public final class Inventario {
    // materials and food
    public static final int COBRE = 0, CUARZO = 1, CORAL = 2, PEZ = 3, TITANIO = 4, FIBRA = 5, BULBO = 6,
            PEZ_COCIDO = 7, AGUA = 8, KIT = 9, BOTIQUIN = 10, CRISTAL_T = 11, MODULO_BALIZA = 12, NUCLEO = 13,
            CRISTAL_R = 14, BALIZA = 15, CORAL_E = 16;
    public static final int N = 17;
    public static final String[] NOMBRE = { "Cobre", "Cuarzo", "Muestra de coral", "Burbujin crudo", "Titanio",
            "Fibra de alga", "Bulbo de alga", "Burbujin cocido", "Agua filtrada", "Kit de reparacion", "Botiquin",
            "Cristal termico", "Modulo de baliza", "Nucleo de energia", "Cristal resonante", "Baliza de rescate",
            "Coral del errante" };
    /** story items cannot be dropped from the PDA */
    static boolean deHistoria(int i) {
        return i == MODULO_BALIZA || i == NUCLEO || i == CRISTAL_R || i == BALIZA;
    }
    public static final int CAPACIDAD = 30;

    // tools: built once, they do not take backpack space
    public static final int H_CUCHILLO = 0, H_LINTERNA = 1, H_TANQUE = 2, H_ALETAS = 3, H_REMORA = 4,
            H_RADAR = 5, H_PROFUNDIDAD = 6, H_TANQUE2 = 7;
    public static final int NH = 8;
    public static final String[] HERRAMIENTA = { "Cuchillo", "Linterna", "Tanque de O2 mejorado", "Aletas",
            "Remora (minisubmarino)", "Escaner de radar", "Modulo de profundidad (Remora)", "Tanque de coral" };

    final int[] cant = new int[N];
    final boolean[] tiene = new boolean[NH];

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

    // ------------------------------------------------------------------ recipes

    /** A fabricator recipe: ingredients (item, amount) and what it makes. */
    static final class Receta {
        final String nombre;
        final String efecto;
        final int[] ing;       // item, amount, item, amount...
        final int item;        // item made, or -1
        final int herramienta; // tool made, or -1

        Receta(String nombre, String efecto, int[] ing, int item, int herramienta) {
            this.nombre = nombre;
            this.efecto = efecto;
            this.ing = ing;
            this.item = item;
            this.herramienta = herramienta;
        }
    }

    static final Receta[] RECETAS = {
            new Receta("Burbujin cocido", "+30 comida", new int[] { PEZ, 1 }, PEZ_COCIDO, -1),
            new Receta("Agua filtrada", "+30 agua", new int[] { BULBO, 2 }, AGUA, -1),
            new Receta("Cuchillo", "corta coral y algas", new int[] { TITANIO, 1, CUARZO, 1 }, -1, H_CUCHILLO),
            new Receta("Linterna", "L encender/apagar", new int[] { COBRE, 1, CUARZO, 1 }, -1, H_LINTERNA),
            new Receta("Tanque de O2 mejorado", "75 s de oxigeno", new int[] { TITANIO, 3, CUARZO, 2 }, -1,
                    H_TANQUE),
            new Receta("Aletas", "nadas un 30% mas rapido", new int[] { FIBRA, 2, TITANIO, 1, CORAL, 1 }, -1,
                    H_ALETAS),
            new Receta("Remora (minisubmarino)", "vehiculo: rapido, con aire, hasta 300 m",
                    new int[] { TITANIO, 4, COBRE, 2, CUARZO, 2, FIBRA, 1 }, -1, H_REMORA),
            new Receta("Kit de reparacion", "repara la radio de la capsula", new int[] { COBRE, 2, CUARZO, 1, FIBRA, 1 },
                    KIT, -1),
            new Receta("Botiquin", "+50 salud (PDA, X)", new int[] { BULBO, 2, CORAL, 1, FIBRA, 1 }, BOTIQUIN, -1),
            new Receta("Escaner de radar", "radar en el visor (criaturas y senales)",
                    new int[] { COBRE, 2, CUARZO, 1, TITANIO, 1 }, -1, H_RADAR),
            new Receta("Modulo de profundidad", "la Remora baja hasta 900 m",
                    new int[] { TITANIO, 3, CRISTAL_T, 3, CUARZO, 1 }, -1, H_PROFUNDIDAD),
            new Receta("Tanque de coral", "120 s de oxigeno (coral del errante)",
                    new int[] { CORAL_E, 3, TITANIO, 2, CUARZO, 1 }, -1, H_TANQUE2),
            new Receta("Baliza de rescate", "lanzala desde la superficie (PDA)",
                    new int[] { MODULO_BALIZA, 1, NUCLEO, 1, CRISTAL_R, 1, TITANIO, 2 }, BALIZA, -1), };

    boolean puede(Receta r) {
        if (r.herramienta >= 0 && tiene[r.herramienta]) {
            return false;
        }
        for (int i = 0; i < r.ing.length; i += 2) {
            if (cant[r.ing[i]] < r.ing[i + 1]) {
                return false;
            }
        }
        return true;
    }

    /** craft: ingredients are consumed first, so the result always fits in the backpack */
    boolean fabricar(Receta r) {
        if (!puede(r)) {
            return false;
        }
        for (int i = 0; i < r.ing.length; i += 2) {
            cant[r.ing[i]] -= r.ing[i + 1];
        }
        if (r.item >= 0) {
            cant[r.item]++;
        } else {
            tiene[r.herramienta] = true;
        }
        return true;
    }

    static String ingredientes(Receta r) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < r.ing.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(r.ing[i + 1]);
            sb.append(' ');
            sb.append(NOMBRE[r.ing[i]]);
        }
        return sb.toString();
    }
}
