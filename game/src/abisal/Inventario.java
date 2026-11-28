package abisal;

/** What the player carries (limited backpack) plus the tools already built. */
public final class Inventario {
    // materials and food
    public static final int COBRE = 0, CUARZO = 1, CORAL = 2, PEZ = 3, TITANIO = 4, FIBRA = 5, BULBO = 6,
            PEZ_COCIDO = 7, AGUA = 8, KIT = 9, BOTIQUIN = 10, CRISTAL_T = 11, MODULO_BALIZA = 12, NUCLEO = 13,
            CRISTAL_R = 14, BALIZA = 15, CORAL_E = 16, OPALO = 17, LAMINA = 18, CABLE = 19, VIDRIO = 20,
            CUERDA = 21, BATERIA = 22, CIRCUITO = 23, HABITAT = 24, SENSOR = 25, LLAVE = 26, FRAGMENTO = 27,
            LAZOFIN = 28, FAROLILLO = 29, CARACOL = 30, CAMARONES = 31, CAMARONES_ASADOS = 32, CANDIL = 33,
            FRUTO = 34, HONGO = 35, BOYA = 36, BENGALA = 37, SENAL = 38, SOPA = 39, CABALLITO_CRUDO = 40,
            LAZOFIN_COCIDO = 41, FAROLILLO_COCIDO = 42, CARACOL_COCIDO = 43, CANDIL_COCIDO = 44, CABALLITO_COCIDO = 45,
            ALEACION = 46, MECANISMO = 47, AZUFRE = 48, CERAMICA = 49, BIOPOLIMERO = 50, URANIO = 51, VIROTE = 52;
    public static final int N = 53;
    public static final String[] NOMBRE = { "Cobre", "Cuarzo", "Muestra de coral", "Pez crudo", "Titanio",
            "Fibra de alga", "Bulbo de alga", "Pez cocido", "Agua filtrada", "Kit de reparación", "Botiquín",
            "Cristal térmico", "Módulo de baliza", "Núcleo de energía", "Cristal resonante", "Baliza de rescate",
            "Coral del errante", "Ópalo de cueva", "Lámina de titanio", "Cable de cobre", "Vidrio", "Cuerda de fibra",
            "Batería", "Circuito", "Módulo de hábitat", "Sensor sísmico", "Llave de resonancia",
            "Fragmento de glifo",
            "Lazofín crudo", "Farolillo crudo", "Caracol de duna", "Camarones igneos", "Camarones asados", "Pez candil",
            "Fruto de lirio", "Hongo de cueva",
            "Boya de emergencia", "Bengala", "Baliza de señal", "Sopa de algas", "Caballito crudo",
            "Lazofín cocido", "Farolillo cocido", "Caracol cocido", "Candil cocido", "Caballito cocido",
            "Aleación de titanio", "Mecanismo", "Azufre", "Cerámica", "Biopolímero", "Uranio", "Virotes de arpón" };
    /** short names for the fabricator's ingredient lines */
    static final String[] CORTO = { "Cobre", "Cuarzo", "Coral", "Pez", "Titanio", "Fibra", "Bulbo", "Cocido",
            "Agua", "Kit", "Botiquín", "Cr.térmico", "Módulo", "Núcleo", "Cr.resonante", "Baliza", "Coral errante",
            "Ópalo", "Lámina", "Cable", "Vidrio", "Cuerda", "Batería", "Circuito", "Hábitat", "Sensor", "Llave", "Fragmento",
            "Lazofín", "Farolillo", "Caracol", "Camarones", "Cam.asados", "Candil", "Fruto", "Hongo", "Boya", "Bengala", "Baliza señal", "Sopa", "Caballito",
            "Laz.cocido", "Far.cocido", "Car.cocido", "Can.cocido", "Cab.cocido", "Aleación", "Mecanismo", "Azufre",
            "Cerámica", "Biopolímero", "Uranio", "Virotes" };

    /** story items cannot be dropped from the PDA */
    static boolean deHistoria(int i) {
        return i == MODULO_BALIZA || i == NUCLEO || i == CRISTAL_R || i == BALIZA || i == LLAVE || i == SENSOR || i == FRAGMENTO;
    }
    public static final int CAPACIDAD = 36;

    /** backpack space: 48 with the bigger backpack */
    int capacidad() {
        return tiene[H_MOCHILA] ? 48 : CAPACIDAD;
    }

    // tools: built once, they do not take backpack space
    public static final int H_CUCHILLO = 0, H_LINTERNA = 1, H_TANQUE = 2, H_ALETAS = 3, H_REMORA = 4,
            H_RADAR = 5, H_PROFUNDIDAD = 6, H_TANQUE2 = 7, H_LINTERNA2 = 8, H_MOCHILA = 9,
            H_GRANJA = 10, H_RECARGA = 11, H_SOLDADOR = 12, H_IMPULSOR = 13,
            H_VARA = 14, H_ESCANER = 15, H_ARPON = 16, H_TURBO = 17, H_SONAR = 18,
            H_TRAJE_RAD = 19, H_TRAJE_RAD2 = 20, H_ESCUDO_REMORA = 21, H_BATEL = 22;
    public static final int NH = 23;
    public static final String[] HERRAMIENTA = { "Cuchillo", "Linterna", "Tanque de O2 mejorado", "Aletas",
            "Rémora (minisubmarino)", "Escáner de radar", "Módulo de profundidad (Rémora)", "Tanque de coral",
            "Linterna mejorada", "Mochila ampliada",
            "Granja de algas (base)", "Sala de recarga (base)", "Soldador", "Impulsor de mano",
            "Vara de descarga", "Escáner de mano", "Arpón",
            "Turbo de la Rémora", "Sonar de largo alcance", "Traje anti-radiación ligero",
            "Traje anti-radiación pesado", "Blindaje anti-radiación (Rémora)", "Batel (submarino de gran profundidad)" };

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
        if (total() >= capacidad()) {
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
        final int cantidad;    // how many units one craft yields (ammo comes in batches)

        Receta(int cat, String nombre, String efecto, int[] ing, int item, int herramienta) {
            this(cat, nombre, efecto, ing, item, herramienta, 1);
        }

        Receta(int cat, String nombre, String efecto, int[] ing, int item, int herramienta, int cantidad) {
            this.cat = cat;
            this.nombre = nombre;
            this.efecto = efecto;
            this.ing = ing;
            this.item = item;
            this.herramienta = herramienta;
            this.cantidad = cantidad;
        }
    }

    /**
     * Recipes. Raw materials become components first (plates, wire, glass, rope, batteries,
     * circuits) and every tool needs its own mix of components, so each one has its own
     * path through the crater.
     */
    static final String[] CATEGORIAS = { "COMPONENTES", "SUPERVIVENCIA", "HERRAMIENTAS", "VEHÍC./BASE", "MISIÓN" };

    static final Receta[] RECETAS = {
            // components
            new Receta(0, "Lámina de titanio", "componente", new int[] { TITANIO, 2 }, LAMINA, -1),
            new Receta(0, "Cable de cobre", "componente", new int[] { COBRE, 2 }, CABLE, -1),
            new Receta(0, "Vidrio", "componente", new int[] { CUARZO, 2 }, VIDRIO, -1),
            new Receta(0, "Cuerda de fibra", "componente", new int[] { FIBRA, 3 }, CUERDA, -1),
            new Receta(0, "Batería", "componente (energía)", new int[] { COBRE, 1, BULBO, 1 }, BATERIA, -1),
            new Receta(0, "Circuito", "componente (electrónica)", new int[] { CABLE, 1, VIDRIO, 1, CORAL, 1 }, CIRCUITO,
                    -1),
            new Receta(0, "Aleación de titanio", "componente avanzado (lámina + cable)",
                    new int[] { LAMINA, 1, CABLE, 1 }, ALEACION, -1),
            new Receta(0, "Mecanismo", "componente avanzado (aleación + circuito)",
                    new int[] { ALEACION, 1, CIRCUITO, 1 }, MECANISMO, -1),
            new Receta(0, "Cerámica", "componente (aguanta el calor)", new int[] { CUARZO, 2, AZUFRE, 1 }, CERAMICA, -1),
            new Receta(0, "Biopolímero", "componente (flexible y ligero)", new int[] { FIBRA, 2, BULBO, 1 }, BIOPOLIMERO,
                    -1),
            // food and medicine
            new Receta(1, "Pez cocido", "+30 comida", new int[] { PEZ, 1 }, PEZ_COCIDO, -1),
            new Receta(1, "Camarones asados", "+28 comida (ya no pican)", new int[] { CAMARONES, 1 }, CAMARONES_ASADOS, -1),
            new Receta(1, "Lazofín cocido", "+20 comida, +16 agua", new int[] { LAZOFIN, 1 }, LAZOFIN_COCIDO, -1),
            new Receta(1, "Farolillo cocido", "+20 comida, +4 agua", new int[] { FAROLILLO, 1 }, FAROLILLO_COCIDO, -1),
            new Receta(1, "Caracol cocido", "+28 comida, +2 agua", new int[] { CARACOL, 1 }, CARACOL_COCIDO, -1),
            new Receta(1, "Candil cocido", "+22 comida, +6 salud", new int[] { CANDIL, 1 }, CANDIL_COCIDO, -1),
            new Receta(1, "Caballito cocido", "+20 comida, +12 agua", new int[] { CABALLITO_CRUDO, 1 }, CABALLITO_COCIDO, -1),
            new Receta(1, "Sopa de algas", "+25 comida, +25 agua", new int[] { BULBO, 2, FIBRA, 1, AGUA, 1 }, SOPA, -1),
            new Receta(1, "Agua filtrada", "+30 agua", new int[] { BULBO, 2 }, AGUA, -1),
            new Receta(1, "Boya de emergencia", "te sube rápido a la superficie (PDA, X)",
                    new int[] { BULBO, 2, CUERDA, 1 }, BOYA, -1),
            new Receta(1, "Mochila ampliada", "48 espacios en la mochila",
                    new int[] { CUERDA, 3, FIBRA, 2, LAMINA, 1 }, -1, H_MOCHILA),
            new Receta(1, "Bengala", "luz que lanzas (en la mano: R)", new int[] { BULBO, 1, COBRE, 1 }, BENGALA, -1),
            new Receta(1, "Botiquín", "+50 salud (PDA, X)", new int[] { BULBO, 2, CORAL, 1, FIBRA, 1 }, BOTIQUIN, -1),
            // tools
            new Receta(2, "Cuchillo", "corta coral y algas, ataca", new int[] { TITANIO, 1, CUARZO, 1 }, -1, H_CUCHILLO),
            new Receta(2, "Linterna", "L encender/apagar (gasta batería)", new int[] { BATERIA, 1, VIDRIO, 1 }, -1,
                    H_LINTERNA),
            new Receta(1, "Tanque de O2 mejorado", "75 s de oxígeno", new int[] { CERAMICA, 1, LAMINA, 1 }, -1, H_TANQUE),
            new Receta(1, "Aletas", "nadas un 30% más rápido", new int[] { BIOPOLIMERO, 1, CUERDA, 1 }, -1, H_ALETAS),
            new Receta(2, "Kit de reparación", "repara la radio de la cápsula", new int[] { CIRCUITO, 1, CABLE, 1 }, KIT,
                    -1),
            new Receta(2, "Escáner de mano", "registra fauna y flora (en la mano: L)",
                    new int[] { CIRCUITO, 1, VIDRIO, 1, BATERIA, 1 }, -1, H_ESCANER),
            new Receta(2, "Arpón", "arma a distancia: dispara con [] (recarga 1,3 s)",
                    new int[] { MECANISMO, 1, ALEACION, 1, CUERDA, 2 }, -1, H_ARPON),
            new Receta(1, "Virotes de arpón", "10 virotes (el arpón gasta uno por disparo)",
                    new int[] { TITANIO, 1, CABLE, 1 }, VIROTE, -1, 10),
            new Receta(2, "Vara de descarga", "arma: aturde y hiere (no corta coral)",
                    new int[] { MECANISMO, 1, ALEACION, 1, CRISTAL_T, 1, BATERIA, 1 }, -1, H_VARA),
            new Receta(2, "Soldador", "repara la Rémora (en la mano: R)",
                    new int[] { MECANISMO, 1, ALEACION, 1, BATERIA, 1 }, -1, H_SOLDADOR),
            new Receta(2, "Impulsor de mano", "nadas muy rápido (en la mano: R)",
                    new int[] { MECANISMO, 2, ALEACION, 1, BATERIA, 1 }, -1, H_IMPULSOR),
            new Receta(2, "Escáner de radar", "radar en el visor (criaturas y señales)",
                    new int[] { MECANISMO, 1, VIDRIO, 1 }, -1, H_RADAR),
            new Receta(2, "Traje anti-radiación ligero", "reduce un 70% la radiación (pasivo)",
                    new int[] { CERAMICA, 2, BIOPOLIMERO, 2, CABLE, 1 }, -1, H_TRAJE_RAD),
            new Receta(2, "Traje anti-radiación pesado", "reduce un 95% la radiación (pasivo)",
                    new int[] { CERAMICA, 4, ALEACION, 2, URANIO, 1 }, -1, H_TRAJE_RAD2),
            new Receta(3, "Blindaje anti-radiación (Rémora)", "el casco de la Rémora aguanta la radiación",
                    new int[] { ALEACION, 4, CERAMICA, 3, URANIO, 1 }, -1, H_ESCUDO_REMORA),
            new Receta(3, "Batel (submarino de gran profundidad)",
                    "sustituye a la Rémora: 700 m, blindado contra la radiación y más resistente",
                    new int[] { ALEACION, 5, CERAMICA, 4, MECANISMO, 3, URANIO, 2 }, -1, H_BATEL),
            new Receta(3, "Rémora (minisubmarino)", "vehículo: rápido, con aire, hasta 300 m",
                    new int[] { MECANISMO, 3, ALEACION, 3, VIDRIO, 2 }, -1, H_REMORA),
            new Receta(3, "Turbo de la Rémora", "la Rémora va un 30% más rápido",
                    new int[] { BATERIA, 2, CIRCUITO, 1, LAMINA, 2, OPALO, 1 }, -1, H_TURBO),
            new Receta(3, "Sonar de largo alcance", "radar de la Rémora a 250 m (cuevas y ecos)",
                    new int[] { CIRCUITO, 2, CABLE, 2, VIDRIO, 1 }, -1, H_SONAR),
            new Receta(3, "Módulo de profundidad", "la Rémora baja hasta 900 m",
                    new int[] { LAMINA, 2, CRISTAL_T, 3, CIRCUITO, 1 }, -1, H_PROFUNDIDAD),
            new Receta(1, "Tanque de coral", "120 s de oxígeno (coral del errante)",
                    new int[] { CORAL_E, 3, LAMINA, 1, VIDRIO, 1 }, -1, H_TANQUE2),
            new Receta(2, "Mejora de linterna", "haz más ancho y fuerte, batería más larga (sobre tu linterna)",
                    new int[] { OPALO, 2, BATERIA, 1, VIDRIO, 1 }, -1, H_LINTERNA2),
            new Receta(3, "Módulo de hábitat", "tu base: colócala desde la PDA (X)",
                    new int[] { LAMINA, 4, VIDRIO, 2, CIRCUITO, 1, BATERIA, 1 }, HABITAT, -1),
            new Receta(3, "Baliza de señal", "marca un sitio en la brujula y el mapa (PDA)",
                    new int[] { COBRE, 1, BATERIA, 1, VIDRIO, 1 }, SENAL, -1),
            new Receta(3, "Granja de algas", "módulo de la base: cultiva bulbos y agua",
                    new int[] { FIBRA, 3, BULBO, 2, VIDRIO, 2, LAMINA, 1 }, -1, H_GRANJA),
            new Receta(3, "Sala de recarga", "módulo de la base: cura y repara la Rémora",
                    new int[] { BATERIA, 2, CIRCUITO, 1, CABLE, 2, LAMINA, 2 }, -1, H_RECARGA),
            new Receta(4, "Sensor sísmico", "mide el Latido (segundo acto)",
                    new int[] { LAMINA, 1, CIRCUITO, 1, BATERIA, 1 }, SENSOR, -1),
            new Receta(4, "Baliza de rescate", "lánzala desde la superficie (PDA)",
                    new int[] { MODULO_BALIZA, 1, NUCLEO, 1, CRISTAL_R, 1, LAMINA, 1, CIRCUITO, 1 }, BALIZA, -1), };

    /** "Lámina 1/3, Vidrio 2/2": have/need for each ingredient */
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
        if (r.herramienta == H_LINTERNA2 && !tiene[H_LINTERNA]) {
            return false; // an upgrade of the lamp: the lamp comes first
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
            cant[r.item] += r.cantidad;
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
