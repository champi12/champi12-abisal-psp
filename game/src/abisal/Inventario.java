package abisal;

/** What the player carries (limited backpack) plus the tools already built. */
public final class Inventario {
    // materials and food
    public static final int COBRE = 0, CUARZO = 1, CORAL = 2, PEZ = 3, TITANIO = 4, FIBRA = 5, BULBO = 6,
            PEZ_COCIDO = 7, AGUA = 8, KIT = 9, BOTIQUIN = 10, CRISTAL_T = 11, MODULO_BALIZA = 12, NUCLEO = 13,
            CRISTAL_R = 14, BALIZA = 15, CORAL_E = 16, OPALO = 17, LAMINA = 18, CABLE = 19, VIDRIO = 20,
            CUERDA = 21, BATERIA = 22, CIRCUITO = 23, HABITAT = 24, SENSOR = 25, LLAVE = 26, FRAGMENTO = 27,
            LAZOFIN = 28, FAROLILLO = 29, CARACOL = 30, CAMARONES = 31, CAMARONES_ASADOS = 32, CANDIL = 33,
            FRUTO = 34, HONGO = 35, BOYA = 36;
    public static final int N = 37;
    public static final String[] NOMBRE = { "Cobre", "Cuarzo", "Muestra de coral", "Burbujin crudo", "Titanio",
            "Fibra de alga", "Bulbo de alga", "Burbujin cocido", "Agua filtrada", "Kit de reparacion", "Botiquin",
            "Cristal termico", "Modulo de baliza", "Nucleo de energia", "Cristal resonante", "Baliza de rescate",
            "Coral del errante", "Opalo de cueva", "Lamina de titanio", "Cable de cobre", "Vidrio", "Cuerda de fibra",
            "Bateria", "Circuito", "Modulo de habitat", "Sensor sismico", "Llave de resonancia",
            "Fragmento de glifo",
            "Lazofin crudo", "Farolillo crudo", "Caracol de duna", "Camarones igneos", "Camarones asados", "Pez candil",
            "Fruto de lirio", "Hongo de cueva",
            "Boya de emergencia" };
    /** short names for the fabricator's ingredient lines */
    static final String[] CORTO = { "Cobre", "Cuarzo", "Coral", "Burbujin", "Titanio", "Fibra", "Bulbo", "Cocido",
            "Agua", "Kit", "Botiquin", "Cr.termico", "Modulo", "Nucleo", "Cr.resonante", "Baliza", "Coral errante",
            "Opalo", "Lamina", "Cable", "Vidrio", "Cuerda", "Bateria", "Circuito", "Habitat", "Sensor", "Llave", "Fragmento",
            "Lazofin", "Farolillo", "Caracol", "Camarones", "Cam.asados", "Candil", "Fruto", "Hongo", "Boya" };

    /** story items cannot be dropped from the PDA */
    static boolean deHistoria(int i) {
        return i == MODULO_BALIZA || i == NUCLEO || i == CRISTAL_R || i == BALIZA || i == LLAVE || i == SENSOR || i == FRAGMENTO;
    }
    public static final int CAPACIDAD = 36;

    // tools: built once, they do not take backpack space
    public static final int H_CUCHILLO = 0, H_LINTERNA = 1, H_TANQUE = 2, H_ALETAS = 3, H_REMORA = 4,
            H_RADAR = 5, H_PROFUNDIDAD = 6, H_TANQUE2 = 7, H_LINTERNA2 = 8;
    public static final int NH = 9;
    public static final String[] HERRAMIENTA = { "Cuchillo", "Linterna", "Tanque de O2 mejorado", "Aletas",
            "Remora (minisubmarino)", "Escaner de radar", "Modulo de profundidad (Remora)", "Tanque de coral",
            "Linterna potente" };

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
        final int cat;         // fabricator tab (CATEGORIAS)

        Receta(int cat, String nombre, String efecto, int[] ing, int item, int herramienta) {
            this.cat = cat;
            this.nombre = nombre;
            this.efecto = efecto;
            this.ing = ing;
            this.item = item;
            this.herramienta = herramienta;
        }
    }

    /**
     * Recipes. Raw materials become components first (plates, wire, glass, rope, batteries,
     * circuits) and every tool needs its own mix of components, so each one has its own
     * path through the crater.
     */
    static final String[] CATEGORIAS = { "COMPONENTES", "SUPERVIVENCIA", "HERRAMIENTAS", "VEHIC./BASE", "MISION" };

    static final Receta[] RECETAS = {
            // components
            new Receta(0, "Lamina de titanio", "componente", new int[] { TITANIO, 2 }, LAMINA, -1),
            new Receta(0, "Cable de cobre", "componente", new int[] { COBRE, 2 }, CABLE, -1),
            new Receta(0, "Vidrio", "componente", new int[] { CUARZO, 2 }, VIDRIO, -1),
            new Receta(0, "Cuerda de fibra", "componente", new int[] { FIBRA, 3 }, CUERDA, -1),
            new Receta(0, "Bateria", "componente (energia)", new int[] { COBRE, 1, BULBO, 1 }, BATERIA, -1),
            new Receta(0, "Circuito", "componente (electronica)", new int[] { CABLE, 1, VIDRIO, 1, CORAL, 1 }, CIRCUITO,
                    -1),
            // food and medicine
            new Receta(1, "Burbujin cocido", "+30 comida", new int[] { PEZ, 1 }, PEZ_COCIDO, -1),
            new Receta(1, "Camarones asados", "+28 comida (ya no pican)", new int[] { CAMARONES, 1 }, CAMARONES_ASADOS, -1),
            new Receta(1, "Agua filtrada", "+30 agua", new int[] { BULBO, 2 }, AGUA, -1),
            new Receta(1, "Boya de emergencia", "te sube rapido a la superficie (PDA, X)",
                    new int[] { BULBO, 2, CUERDA, 1 }, BOYA, -1),
            new Receta(1, "Botiquin", "+50 salud (PDA, X)", new int[] { BULBO, 2, CORAL, 1, FIBRA, 1 }, BOTIQUIN, -1),
            // tools
            new Receta(2, "Cuchillo", "corta coral y algas, ataca", new int[] { TITANIO, 1, CUARZO, 1 }, -1, H_CUCHILLO),
            new Receta(2, "Linterna", "L encender/apagar", new int[] { BATERIA, 1, VIDRIO, 1 }, -1, H_LINTERNA),
            new Receta(1, "Tanque de O2 mejorado", "75 s de oxigeno", new int[] { LAMINA, 2, VIDRIO, 1 }, -1, H_TANQUE),
            new Receta(1, "Aletas", "nadas un 30% mas rapido", new int[] { CUERDA, 2, CORAL, 1 }, -1, H_ALETAS),
            new Receta(2, "Kit de reparacion", "repara la radio de la capsula", new int[] { CIRCUITO, 1, CABLE, 1 }, KIT,
                    -1),
            new Receta(2, "Escaner de radar", "radar en el visor (criaturas y senales)",
                    new int[] { CIRCUITO, 1, BATERIA, 1, VIDRIO, 1 }, -1, H_RADAR),
            new Receta(3, "Remora (minisubmarino)", "vehiculo: rapido, con aire, hasta 300 m",
                    new int[] { LAMINA, 3, VIDRIO, 2, BATERIA, 1, CIRCUITO, 1, CUERDA, 1 }, -1, H_REMORA),
            new Receta(3, "Modulo de profundidad", "la Remora baja hasta 900 m",
                    new int[] { LAMINA, 2, CRISTAL_T, 3, CIRCUITO, 1 }, -1, H_PROFUNDIDAD),
            new Receta(1, "Tanque de coral", "120 s de oxigeno (coral del errante)",
                    new int[] { CORAL_E, 3, LAMINA, 1, VIDRIO, 1 }, -1, H_TANQUE2),
            new Receta(2, "Linterna potente", "ilumina mas (opalo de cueva)", new int[] { OPALO, 2, BATERIA, 1, VIDRIO, 1 },
                    -1, H_LINTERNA2),
            new Receta(3, "Modulo de habitat", "tu base: colocala desde la PDA (X)",
                    new int[] { LAMINA, 4, VIDRIO, 2, CIRCUITO, 1, BATERIA, 1 }, HABITAT, -1),
            new Receta(4, "Sensor sismico", "mide el Latido (segundo acto)",
                    new int[] { LAMINA, 1, CIRCUITO, 1, BATERIA, 1 }, SENSOR, -1),
            new Receta(4, "Baliza de rescate", "lanzala desde la superficie (PDA)",
                    new int[] { MODULO_BALIZA, 1, NUCLEO, 1, CRISTAL_R, 1, LAMINA, 1, CIRCUITO, 1 }, BALIZA, -1), };

    /** "Lamina 1/3, Vidrio 2/2": have/need for each ingredient */
    String ingredientesCon(Receta r) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < r.ing.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(CORTO[r.ing[i]]).append(' ').append(cant[r.ing[i]]).append('/').append(r.ing[i + 1]);
        }
        return sb.toString();
    }

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
            sb.append(CORTO[r.ing[i]]);
        }
        return sb.toString();
    }
}
