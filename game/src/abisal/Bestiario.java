package abisal;

import javax.microedition.lcdui.Graphics;

/**
 * The bestiary: every creature and plant the hand scanner can record. Hold L with the
 * scanner pointed at something for a moment; its entry (a short field note) then shows
 * in the PDA (L inside the PDA). Codes: 0-13 small fauna (the Peces species), 14-28 big
 * and dangerous fauna, 29-46 flora.
 */
final class Bestiario {
    static final int PECES = 0, DEP = 14, ACORAZADO = 18, ESCUPIDOR = 19, COLOSO = 20, GUARDIAN = 21,
            MEDUSA_LUNA = 22, ERRANTE = 23, VELO = 24, AGUIJONERO = 25, MEDUSA_RAYO = 26, CHILLON = 27,
            RONDADOR = 28, FLORA = 29, GUSANO = 47, RAYA = 48, CABALLITO = 49, FAROL_L = 50, RAYA_P = 51,
            LANZA = 52, ROEHUESOS = 53, PECES2 = 54, FLORA2 = 64, BOMBA = 72;
    /** the new zones' scenery with an entry, in code order from FLORA2 */
    static final int[] FLORA2_TIPO = { Mundo.P_AGUJA, Mundo.P_GEODA, Mundo.P_FLORCRISTAL, Mundo.P_VERTEBRA,
            Mundo.P_CRANEO, Mundo.P_COLMILLO, Mundo.P_TUETANO, Mundo.P_AZUFRE };
    /** prop kinds with an entry, in code order from FLORA */
    static final int[] FLORA_TIPO = { Mundo.P_ABANICO, Mundo.P_TUBOS, Mundo.P_ALGA, Mundo.P_LANZA, Mundo.P_FLOR,
            Mundo.P_PASTO, Mundo.P_ESPONJA, Mundo.P_TUBOG, Mundo.P_LIRIO, Mundo.P_CORALCENIZA, Mundo.P_ANEMONA,
            Mundo.P_HIERBA, Mundo.P_ESPINO, Mundo.P_HONGO, Mundo.P_VEJIGA, Mundo.P_CRISTALT, Mundo.P_OPALO,
            Mundo.P_FUMAROLA };
    /**
     * A code is a creature unless it falls inside the flora block. Anything at or above
     * GUSANO is an extra creature (worm, rays, seahorse, lanternfish): with a plain range
     * test those ended up labelled as plants.
     */
    static boolean esFauna(int c) {
        return c < FLORA || (c >= GUSANO && c < FLORA2) || c >= BOMBA;
    }

    /** 0 = small fauna, 1 = large fauna, 2 = flora, 3 = minerals and vents */
    static int grupo(int c) {
        if (c >= FLORA2 && c < BOMBA) {
            int t = FLORA2_TIPO[c - FLORA2];
            return t == Mundo.P_FLORCRISTAL || t == Mundo.P_TUETANO ? 2 : 3;
        }
        if (!esFauna(c)) {
            int idx = c - FLORA;
            if (idx >= 0 && idx < FLORA_TIPO.length) {
                int tipo = FLORA_TIPO[idx];
                if (tipo == Mundo.P_CRISTALT || tipo == Mundo.P_OPALO || tipo == Mundo.P_FUMAROLA) {
                    return 3;
                }
            }
            return 2;
        }
        return (c < DEP || c == RAYA || c == CABALLITO || c == RAYA_P || c == FAROL_L || (c >= PECES2 && c < FLORA2)) ? 0 : 1;
    }

    static int colorGrupo(int g) {
        switch (g) {
        case 0:
            return 0x9fe0ff;
        case 1:
            return 0xffb08a;
        case 2:
            return 0x9fffa0;
        default:
            return 0xd9c9a4;
        }
    }

    static final String[] GRUPO = { "Fauna", "Fauna mayor", "Flora", "Minerales y fumarolas" };

    static final String[] NOMBRE = {
            // small fauna (Peces species order)
            "Burbujín", "Farolillo", "Lazofín", "Planeador", "Medusa", "Cardumen plateado", "Pastor de arena",
            "Manta de cristal", "Caracol de duna", "Camarones igneos", "Tortuga coralina", "Pez candil",
            "Flotador de gas", "Erizo globo",
            // big fauna
            "Dientesierra", "Anguila de brasa", "Acechador de algas", "Mordedor de pasto", "Acorazado",
            "Escupidor de cueva", "Coloso de Ceniza", "Guardián del Vacío", "Medusa luna", "Errante de arrecife",
            "Velo abisal", "Aguijonero", "Medusa rayo", "Chillón de cañón", "Rondador abisal",
            // flora
            "Coral abanico", "Coral de tubos", "Alga cinta", "Planta lanza", "Flor abisal", "Pasto marino",
            "Esponja", "Tubo gigante", "Lirio de duna", "Coral de ceniza", "Anémona", "Hierba alta",
            "Espino urticante", "Hongo de cueva", "Vejiga de aire", "Cristal térmico", "Ópalo de cueva",
            "Fumarola",
            // 0.25
            "Gusano de arena", "Raya chispa", "Caballito de mar", "Farol de llanura", "Raya pálida",
            // the new zones
            "Lanza vítrea", "Roehuesos", "Pez espejo", "Raspa", "Pez prisma", "Aguja de cuarzo", "Pez lápida",
            "Candela de hueso", "Pez teja", "Pez ceniza", "Pez ojo", "Pez murciélago",
            "Aguja de cristal plomado", "Geoda abierta", "Flor de cristal", "Vértebra de coloso",
            "Cráneo de coloso", "Colmillo de marfil", "Flor de tuétano", "Costra de azufre",
            "Bomba de azufre" };
    static final String[][] TEXTO = {
            { "Pez rechoncho de tres colas que vive en bancos.", "Su saco de gas brilla un poco. Comestible:",
                    "atrapalo con la mano y cocinalo." },
            { "Pequeño pez de las profundidades con una lampara", "en la frente. Crudo te da visión nocturna",
                    "durante un rato." },
            { "Un disco con dos cintas largas que ondulan.", "Vive entre las algas del bosque. Jugoso:",
                    "da agua además de comida." },
            { "Planea con alas anchas sobre los bajíos.", "Inofensivo y muy asustadizo." },
            { "Campana translucida que deriva sin rumbo.", "Sus tentaculos queman a quien la ataca." },
            { "Cientos de peces plateados que giran juntos.", "Se separan y se vuelven a unir al pasar." },
            { "Herbivoro grande con cuernos que pasta en el", "fondo. Pacifico, pero embiste si lo hieres." },
            { "Manta transparente que planea sobre las dunas.", "Se ve su esqueleto de cristal por dentro." },
            { "Caracol lento que se entierra en la arena.", "Muy salado: quita un poco de agua." },
            { "Enjambres rojos que viven sobre las fumarolas.", "Crudos pican; asados son buena comida." },
            { "Tortuga lenta con un arrecife en el caparazon.", "No le teme al buceador." },
            { "Sale solo de noche en los bajíos. Su luz", "atrae a otros peces. Reconforta al comerlo." },
            { "Globo de gas que deriva por los cañones.", "Al reventarlo libera aire respirable." },
            { "Pez redondo de la meseta y los cañones. Si te", "acercas se hincha en una bola de espinas:",
                    "no lo toques inflado." },
            { "Depredador de la meseta con una cresta de", "dientes. Huye cuando esta herido y vuelve." },
            { "Anguila de las grietas termales con el cuerpo", "al rojo. Muerde y se esconde en el calor." },
            { "Calamar que acecha entre las algas. Se lanza", "con los brazos abiertos." },
            { "Sapo marino que se esconde en la hierba alta.", "Salta sobre quien pasa cerca." },
            { "Cangrejo enorme con caparazon de placas.", "Lento, pero sus pinzas golpean fuerte." },
            { "Vive pegado al techo de las cuevas. Escupe", "ácido a quien entra en su refugio." },
            { "Gigante de la fosa con alas de ceniza y señuelos", "luminosos. Muy resistente: hacen falta", "muchos golpes para abatirlo." },
            { "Leviatan acorazado del borde del vacío.", "Patrulla y carga contra todo lo que se mueve.", "Se le puede abatir, pero aguanta mucho." },
            { "Medusa gigante y pacifica que solo aparece de", "noche. Su luz ilumina el agua a su pasó." },
            { "Gigante pacifico con un jardin en el lomo.", "Allí crece un coral único, y viven parasitos",
                    "y mordedores escondidos en la hierba." },
            { "Cinta viva de unos 36 m que ondula en la fosa.", "Los rondadores evitan su brillo: nadar a su",
                    "lado es un refugio en lo profundo." },
            { "Peces aguja que cazan en grupo de noche en los", "bajíos. Su picadura envenena. Defienden",
                    "su zona y no te siguen lejos." },
            { "Medusa que acumula carga y la suelta en una", "descarga que aturde." },
            { "Criatura de los cañones cuyo chillido", "desorienta. Ataca cuando estas confuso." },
            { "Cazador de la fosa que roba el aire de su", "presa con un mordisco." },
            { "Coral en forma de abanico. Con el cuchillo", "da muestras de coral." },
            { "Racimos de tubos de coral. Con el cuchillo", "da muestras de coral." },
            { "Alga gigante del bosque. Algunas llevan vainas", "naranjas: dan fibra y bulbos." },
            { "Planta rigida en forma de lanza de la meseta.", "Crece en grupos sobre las terrazas." },
            { "Flor de las profundidades que brilla en la", "oscuridad de los cañones y la fosa." },
            { "Praderas de hierba baja que se mece con la", "corriente." },
            { "Esponja porosa que filtra el agua. Abunda en", "casi todas las zonas." },
            { "Tubos enormes que crecen en la meseta y los", "cañones." },
            { "Flor de las praderas: su fruto se arranca con", "la mano y quita la sed." },
            { "Coral gris que crece junto a las fumarolas,", "resistente al agua hirviendo." },
            { "Anémona de tentaculos suaves de los bajíos.", "Inofensiva para el buceador." },
            { "Hierba espesa de las praderas y el bosque.", "Los mordedores se esconden dentro." },
            { "Arbusto de pinchos que pica al rozarlo.", "Mejor rodearlo." },
            { "Hongo que crece dentro de las cuevas. Cura,", "pero marea durante un rato." },
            { "Planta con sacos de gas respirable. Pulsa [] a", "su lado para respirar: +20 s de aire.",
                    "Tarda minuto y medio en rellenarse." },
            { "Cristal que se forma en las grietas termales.", "Necesario para la profundidad de la Rémora." },
            { "Geoda iridiscente que solo aparece dentro de", "cuevas y túneles. Rara." },
            { "Chimenea del fondo que expulsa agua hirviendo.", "No nades por encima de la columna." },
            { "Gusano de las praderas que espera enterrado en", "la arena abierta. Sale de golpe con cuatro",
                    "mandibulas cuando nadas por encima." },
            { "Raya de alas anchas que solo sale de noche en el", "bosque y las praderas. Sus franjas azules",
                    "guardan carga: tocarla da una descarga que aturde." },
            { "Pez vertical de cola rizada que se mece entre la", "hierba de los bajíos. Pequeño y sabroso:",
                    "atrápalo con la mano." },
            { "Pez ciego de la llanura exterior. Lleva un", "señuelo frío que brilla en la oscuridad;",
                    "no se come, pero su luz delata corrientes." },
            { "Raya plana de alas anchas que planea sobre la", "arena pálida del exterior. Su cola larga",
                    "marca por dónde pasa sin tocar el fondo." },
            { "Serpiente de vidrio del arrecife de cristal. De", "día la luz atraviesa su cuerpo y es casi",
                    "invisible: solo se ve su espina luminosa.", "De noche duerme entre las agujas." },
            { "Anguila pálida del osario con placas de hueso.", "Sale de noche, en grupo, a roer los",
                    "restos de los colosos... y lo que se mueva." },
            { "Pez hacha de escamas como vidrio, con una línea", "que brilla. Vive en el arrecife de cristal.",
                    "Comestible." },
            { "Carroñero del osario: su mitad trasera es casi", "solo espinas. No se come." },
            { "Pececillo triangular de vidrio violeta. Nada en", "grupos entre las flores de cristal. Comestible." },
            { "Pez largo y fino como una astilla de cuarzo,", "con un hocico de aguja. Pica si lo agarras." },
            { "Pez plano que se tumba sobre el sedimento gris", "del osario como una losa. Comestible." },
            { "Pez pálido con tres espinas que arden como", "velas. Su luz guía entre los huesos." },
            { "Pez acorazado de placas rojas, como las terrazas", "de la meseta. Comestible y sabroso." },
            { "Pez negro de las grietas con rendijas de brasa", "en los costados. Comestible, si lo dejas enfriar." },
            { "Pez de la fosa con un ojo enorme que brilla.", "Ve en la oscuridad total. Comestible." },
            { "Pez de los cañones con aletas como alas.", "Planea entre los pilares. No se come." },
            { "Prismas de vidrio cargado de plomo. Con el", "cuchillo da cristal plomado: frena la",
                    "radiación (visores y trajes)." },
            { "Roca hueca partida con cristales dentro y un", "núcleo que brilla. Se puede nadar cerca." },
            { "Flor de vidrio que brilla en el arrecife.", "Se mece muy poco con la corriente." },
            { "Vértebra de un coloso muerto hace siglos, medio", "enterrada en el sedimento del osario." },
            { "El cráneo de un coloso antiguo. Mide más que", "la Rémora. Algo vive en sus órbitas." },
            { "Colmillo de coloso clavado en el fondo. Con el", "cuchillo da marfil: duro, ligero y aislante." },
            { "Tallos huecos de hueso con una gota de luz", "anaranjada en la punta. Solo en el osario." },
            { "Costras amarillas sobre la roca roja de la", "meseta. Dan azufre para la cerámica." },
            { "Saco vivo de gas sulfuroso de la meseta. Sigue", "a lo que se mueve, se hincha y revienta.",
                    "Si se hincha, aléjate nadando. Mejor", "abatirla de lejos con el arpón." } };
    static final int N = NOMBRE.length;
    /** the notes re-wrapped to the width of the entry panel */
    static final String[][] LINEAS = new String[N][];
    static {
        for (int i = 0; i < N; i++) {
            StringBuffer sb = new StringBuffer();
            for (int k = 0; k < TEXTO[i].length; k++) {
                if (k > 0) {
                    sb.append(' ');
                }
                sb.append(TEXTO[i][k]);
            }
            LINEAS[i] = envolver(sb.toString(), 40);
        }
    }

    private static String[] envolver(String s, int ancho) {
        String[] tmp = new String[12];
        int n = 0, ini = 0;
        while (ini < s.length() && n < tmp.length) {
            int fin = Math.min(s.length(), ini + ancho);
            if (fin < s.length()) {
                int esp = s.lastIndexOf(' ', fin);
                if (esp > ini) {
                    fin = esp;
                }
            }
            tmp[n++] = s.substring(ini, fin).trim();
            ini = fin + 1;
        }
        String[] r = new String[n];
        System.arraycopy(tmp, 0, r, 0, n);
        return r;
    }

    final boolean[] conocido = new boolean[N];
    int sel;

    int total() {
        int n = 0;
        for (int i = 0; i < N; i++) {
            if (conocido[i]) {
                n++;
            }
        }
        return n;
    }

    private int mejor;
    private float mejorD;

    /** keep the closest candidate within the scanner's cone */
    private void candidato(Partida p, float x, float y, float z, int codigo, float radio) {
        float ox = x - p.px, oy = y - p.py, oz = z - p.pz;
        float d = j2me.FMath.sqrt(ox * ox + oy * oy + oz * oz);
        float alcance = 10 + radio;
        if (d > alcance || d >= mejorD) {
            return;
        }
        if (d > radio + 0.5f && (ox * p.fx + oy * p.fy + oz * p.fz) < d * 0.88f) {
            return;
        }
        mejorD = d;
        mejor = codigo;
    }

    /** the code of what the scanner points at (closest in its cone), or -1 */
    int apuntado(Partida p) {
        mejor = -1;
        mejorD = 99;
        Peces pc = p.peces;
        for (int i = 0; i < Peces.N; i++) {
            if (pc.vivo[i]) {
                int e = pc.especie[i];
                int cod = e >= 18 ? PECES2 + e - 18
                        : (e == 14 ? RAYA : (e == 15 ? CABALLITO : (e == 16 ? FAROL_L : (e == 17 ? RAYA_P : PECES + e))));
                candidato(p, pc.x[i], pc.y[i], pc.z[i], cod, Peces.RADIO[e]);
            }
        }
        Depredadores d = p.depredadores;
        for (int i = 0; i < Depredadores.ND; i++) {
            if (d.dVivo[i]) {
                if (!(d.dTipo[i] == Depredadores.GUSANO && d.dOculto[i])) {
                    int t = d.dTipo[i];
                    candidato(p, d.dx[i], d.dy[i], d.dz[i], t == Depredadores.GUSANO ? GUSANO
                            : (t == Depredadores.VITREA ? LANZA : (t == Depredadores.ROEHUESOS ? ROEHUESOS : DEP + t)), 1);
                }
            }
        }
        for (int i = 0; i < Depredadores.NA; i++) {
            if (d.aVivo[i]) {
                candidato(p, d.ax[i], d.ay[i] + 0.5f, d.az[i], ACORAZADO, 1.5f);
            }
        }
        for (int i = 0; i < Depredadores.NS; i++) {
            if (d.sVivo[i]) {
                candidato(p, d.sx[i], d.sy[i], d.sz[i], ESCUPIDOR, 0.5f);
            }
        }
        if (d.cVivo) {
            candidato(p, d.cx, d.cy, d.cz, COLOSO, 20);
        }
        if (d.gVivo) {
            candidato(p, d.gx, d.gy, d.gz, GUARDIAN, 8);
        }
        if (d.mlVivo) {
            candidato(p, d.mlx, d.mly, d.mlz, MEDUSA_LUNA, 8);
        }
        if (d.eVivo) {
            candidato(p, d.ex, d.ey, d.ez, ERRANTE, 14);
        }
        if (d.vVivo) {
            for (int k = 0; k < Depredadores.NV; k += 2) {
                candidato(p, d.vsx[k], d.vsy[k], d.vsz[k], VELO, 3);
            }
        }
        Hostiles h = p.hostiles;
        for (int i = 0; i < Hostiles.NAG; i++) {
            if (h.agVivo[i]) {
                candidato(p, h.agx[i], h.agy[i], h.agz[i], AGUIJONERO, 0.4f);
            }
        }
        for (int i = 0; i < Hostiles.NMR; i++) {
            if (h.mrVivo[i]) {
                candidato(p, h.mrx[i], h.mry[i], h.mrz[i], MEDUSA_RAYO, 1);
            }
        }
        if (h.chVivo) {
            candidato(p, h.chx, h.chy, h.chz, CHILLON, 1);
        }
        for (int i = 0; i < Hostiles.NBO; i++) {
            if (h.boVivo[i]) {
                candidato(p, h.box[i], h.boy[i], h.boz[i], BOMBA, 0.6f);
            }
        }
        for (int i = 0; i < Hostiles.NRO; i++) {
            if (h.roVivo[i]) {
                candidato(p, h.rox[i], h.roy[i], h.roz[i], RONDADOR, 1);
            }
        }
        // plants: only if no animal is in the cone (animals move away, plants wait)
        if (mejor < 0) {
            Mundo m = p.mundo;
            for (int i = 0; i < m.nChunks; i++) {
                Mundo.Chunk c = m.chunks[i];
                float cxm = (c.cx + 0.5f) * Mundo.LADO - p.px, czm = (c.cz + 0.5f) * Mundo.LADO - p.pz;
                if (cxm * cxm + czm * czm > 50 * 50) {
                    continue;
                }
                for (int k = 0; k < c.n; k++) {
                    if (c.quitado[k]) {
                        continue;
                    }
                    int f = codigoFlora(c.tipo[k]);
                    if (f >= 0) {
                        candidato(p, c.x[k], c.y[k] + 0.8f * c.esc[k], c.z[k], f, 0.8f * c.esc[k]);
                    }
                }
            }
        }
        return mejor;
    }

    static int codigoFlora(int tipo) {
        if (tipo == Mundo.P_VAINAS) {
            tipo = Mundo.P_ALGA;
        }
        for (int i = 0; i < FLORA_TIPO.length; i++) {
            if (FLORA_TIPO[i] == tipo) {
                return FLORA + i;
            }
        }
        for (int i = 0; i < FLORA2_TIPO.length; i++) {
            if (FLORA2_TIPO[i] == tipo) {
                return FLORA2 + i;
            }
        }
        return -1;
    }

    /** PDA bestiary screen: up/down choose, O back */
    boolean actualizar(Entrada in) {
        if (in.pulso(Entrada.CIRCULO) || in.pulso(Entrada.TRIANGULO) || in.pulso(Entrada.L)) {
            return false;
        }
        if (in.pulso(Entrada.ABAJO)) {
            sel = (sel + 1) % N;
        }
        if (in.pulso(Entrada.ARRIBA)) {
            sel = (sel + N - 1) % N;
        }
        if (in.pulso(Entrada.DER)) {
            sel = Math.min(N - 1, sel + 10);
        }
        if (in.pulso(Entrada.IZQ)) {
            sel = Math.max(0, sel - 10);
        }
        return true;
    }

    void dibujar(Graphics g, Partida p) {
        p.panelCaja(g, 20, 20, 440, 232);
        g.setColor(0x7fe8f4);
        g.drawString("BESTIARIO  " + total() + "/" + N, 30, 28, Graphics.TOP | Graphics.LEFT);
        int vis = 14;
        int ini = Math.max(0, Math.min(N - vis, sel - vis / 2));
        for (int k = 0; k < vis && ini + k < N; k++) {
            int i = ini + k;
            int y = 46 + k * 13;
            if (i == sel) {
                g.setColor(0x1d4a58);
                g.fillRect(26, y - 2, 160, 12);
            }
            int gr = grupo(i);
            g.setColor(conocido[i] ? colorGrupo(gr) : 0x5a7a84);
            g.drawString(conocido[i] ? NOMBRE[i] : "- - - - -", 32, y, Graphics.TOP | Graphics.LEFT);
        }
        // the entry
        g.setColor(0x0b2430);
        g.fillRect(194, 44, 258, 180);
        if (conocido[sel]) {
            g.setColor(0xffffff);
            g.drawString(NOMBRE[sel], 202, 50, Graphics.TOP | Graphics.LEFT);
            int gra = grupo(sel);
            g.setColor(colorGrupo(gra));
            g.drawString(GRUPO[gra], 202, 64, Graphics.TOP | Graphics.LEFT);
            g.setColor(0xd8ecf0);
            String[] t = LINEAS[sel];
            for (int i = 0; i < t.length; i++) {
                g.drawString(t[i], 202, 84 + i * 14, Graphics.TOP | Graphics.LEFT);
            }
        } else {
            g.setColor(0x6a8a94);
            g.drawString("Sin registrar.", 202, 50, Graphics.TOP | Graphics.LEFT);
            g.drawString("Equipa el escáner de mano y", 202, 70, Graphics.TOP | Graphics.LEFT);
            g.drawString("mantenlo (L) apuntando a algo.", 202, 84, Graphics.TOP | Graphics.LEFT);
        }
        g.setColor(0x9fc4cc);
        g.drawString("arriba/abajo elegir   <- -> saltar   O volver", 30, 236, Graphics.TOP | Graphics.LEFT);
    }
}
