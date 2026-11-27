package abisal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Enumeration;

import javax.microedition.rms.RecordStore;

/**
 * Save game in a record store (a file under save/ next to the EBOOT): the diver, the
 * meters, the backpack, the tools and every prop already taken from the world.
 */
public final class Guardado {
    static final String NOMBRE = "abisal_partida";
    static final int VERSION = 1;
    /** kept open between saves: opening/closing the store on the Memory Stick was the lag */
    private static RecordStore rs;

    private Guardado() {
    }

    private static RecordStore abrir() throws Exception {
        if (rs == null) {
            rs = RecordStore.openRecordStore(NOMBRE, true);
        }
        return rs;
    }

    static boolean existe() {
        try {
            return abrir().getNumRecords() > 0;
        } catch (Throwable e) {
            return false;
        }
    }

    // ---- background writer -------------------------------------------------------------
    // Writing 1.3 KB to the Memory Stick takes 150-600 ms on the real console. That used to
    // freeze the frame, so the data is serialized on the game thread (a millisecond) and the
    // slow write is handed to a worker thread. Only the newest save matters, so one slot.
    private static final Object LOCK = new Object();
    private static Thread escritor;
    private static byte[] cola;
    private static boolean colaLista;

    private static final class Escritor implements Runnable {
        public void run() {
            for (;;) {
                byte[] d = null;
                synchronized (LOCK) {
                    if (colaLista) {
                        d = cola;
                        cola = null;
                        colaLista = false;
                    }
                }
                if (d == null) {
                    try {
                        Thread.sleep(50);
                    } catch (Throwable e) {
                    }
                    continue;
                }
                long t0 = System.currentTimeMillis();
                try {
                    RecordStore r = abrir();
                    if (r.getNumRecords() == 0) {
                        r.addRecord(d, 0, d.length);
                    } else {
                        r.setRecord(1, d, 0, d.length);
                    }
                    Log.d("guardado (hilo): " + d.length + " bytes en " + (System.currentTimeMillis() - t0) + " ms");
                } catch (Throwable e) {
                    Log.d("ERROR al guardar (hilo): " + e);
                }
            }
        }
    }

    private static void arrancarEscritor() {
        if (escritor != null) {
            return;
        }
        Thread t = new Thread(new Escritor());
        try {
            t.start();
            escritor = t;
        } catch (Throwable e) {
            Log.d("no se pudo lanzar el hilo de guardado: " + e);
        }
    }

    static boolean guardar(Partida p) {
        int faseAntes = Log.fase(Log.F_GUARDAR);
        try {
            return guardar0(p);
        } finally {
            Log.fase(faseAntes);
        }
    }

    private static boolean guardar0(Partida p) {
        long t0 = System.currentTimeMillis();
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeInt(VERSION);
            o.writeFloat(p.px);
            o.writeFloat(p.py);
            o.writeFloat(p.pz);
            o.writeFloat(p.yaw);
            o.writeFloat(p.pitch);
            o.writeFloat(p.o2);
            o.writeFloat(p.salud);
            o.writeFloat(p.comida);
            o.writeFloat(p.agua);
            o.writeFloat(p.tiempo);
            o.writeInt(p.muertes);
            o.writeInt(Inventario.N);
            for (int i = 0; i < Inventario.N; i++) {
                o.writeInt(p.inv.cant[i]);
            }
            o.writeInt(Inventario.NH);
            for (int i = 0; i < Inventario.NH; i++) {
                o.writeBoolean(p.inv.tiene[i]);
            }
            o.writeInt(p.mundo.recogidos.size());
            for (Enumeration e = p.mundo.recogidos.keys(); e.hasMoreElements();) {
                o.writeUTF((String) e.nextElement());
            }
            // added in 0.13 (older saves simply end here)
            Historia h = p.historia;
            o.writeInt(h.etapa);
            o.writeBoolean(h.radioReparada);
            o.writeInt(h.radioOida);
            for (int i = 0; i < 3; i++) { // the act I places (0.13 layout)
                o.writeBoolean(h.tomado[i]);
            }
            o.writeInt(Inventario.N);
            for (int i = 0; i < Inventario.N; i++) {
                o.writeInt(p.interior.taquilla[i]);
            }
            o.writeFloat(p.interior.botiquinT);
            o.writeFloat(p.rx);
            o.writeFloat(p.ry);
            o.writeFloat(p.rz);
            // added in 0.17: act II places and the base
            o.writeInt(Historia.LUGARES);
            for (int i = 0; i < Historia.LUGARES; i++) {
                o.writeBoolean(h.tomado[i]);
            }
            o.writeBoolean(p.hayBase);
            o.writeFloat(p.baseX);
            o.writeFloat(p.baseY);
            o.writeFloat(p.baseZ);
            // added in 0.21: the Remora's hull
            o.writeFloat(p.rCasco);
            o.writeBoolean(p.rAveriada);
            o.writeInt(p.cosecha);
            o.writeFloat(p.granjaT);
            o.writeInt(100 + p.equipo); // 0.22 numbering (the lamp is slot 1)
            o.writeInt(p.arma);
            // 0.22: the bestiary
            o.writeInt(Bestiario.N);
            for (int i = 0; i < Bestiario.N; i++) {
                o.writeBoolean(p.bestiario.conocido[i]);
            }
            // 0.24: the signal beacons, then the explored map (one bit per cell)
            o.writeInt(Partida.NSENAL);
            for (int i = 0; i < Partida.NSENAL; i++) {
                o.writeBoolean(p.sbPuesta[i]);
                o.writeFloat(p.sbX[i]);
                o.writeFloat(p.sbY[i]);
                o.writeFloat(p.sbZ[i]);
            }
            o.writeInt(Mapa.N * Mapa.N);
            for (int i = 0; i < Mapa.N * Mapa.N; i += 8) {
                int b = 0;
                for (int k = 0; k < 8; k++) {
                    if (p.mapa.visto[i + k]) {
                        b |= 1 << k;
                    }
                }
                o.writeByte(b);
            }
            // 0.25: data echoes found
            o.writeInt(Ecos.N);
            for (int i = 0; i < Ecos.N; i++) {
                o.writeBoolean(p.ecos.hallado[i]);
            }
            o.flush();
            byte[] d = bo.toByteArray();
            arrancarEscritor();
            if (escritor != null) {
                // hand it over: the write happens on the worker, the game keeps its frame
                synchronized (LOCK) {
                    cola = d;
                    colaLista = true;
                }
                Log.d("partida guardada: " + d.length + " bytes en " + (System.currentTimeMillis() - t0)
                        + " ms (escritura en segundo plano)");
                return true;
            }
            RecordStore r = abrir();
            if (r.getNumRecords() == 0) {
                r.addRecord(d, 0, d.length);
            } else {
                r.setRecord(1, d, 0, d.length);
            }
            Log.d("partida guardada: " + d.length + " bytes en " + (System.currentTimeMillis() - t0) + " ms");
            return true;
        } catch (Throwable e) {
            Log.d("ERROR al guardar: " + e);
            return false;
        }
    }

    /** fills p from the save; returns false (and leaves p as a new game) on any problem */
    static boolean cargar(Partida p) {
        try {
            RecordStore r = abrir();
            byte[] d = r.getRecord(1);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(d));
            int v = in.readInt();
            if (v != VERSION) {
                Log.d("partida guardada de otra version (" + v + "), se ignora");
                return false;
            }
            p.px = in.readFloat();
            p.py = in.readFloat();
            p.pz = in.readFloat();
            p.yaw = in.readFloat();
            p.pitch = in.readFloat();
            p.o2 = in.readFloat();
            p.salud = in.readFloat();
            p.comida = in.readFloat();
            p.agua = in.readFloat();
            p.tiempo = in.readFloat();
            p.muertes = in.readInt();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int c = in.readInt();
                if (i < Inventario.N) {
                    p.inv.cant[i] = c;
                }
            }
            n = in.readInt();
            for (int i = 0; i < n; i++) {
                boolean b = in.readBoolean();
                if (i < Inventario.NH) {
                    p.inv.tiene[i] = b;
                }
            }
            n = in.readInt();
            for (int i = 0; i < n; i++) {
                p.mundo.recogidos.put(in.readUTF(), Boolean.TRUE);
            }
            try {
                Historia h = p.historia;
                h.etapa = in.readInt();
                h.radioReparada = in.readBoolean();
                h.radioOida = in.readInt();
                for (int i = 0; i < 3; i++) {
                    h.tomado[i] = in.readBoolean();
                }
                int nt = in.readInt();
                for (int i = 0; i < nt; i++) {
                    int c = in.readInt();
                    if (i < Inventario.N) {
                        p.interior.taquilla[i] = c;
                    }
                }
                p.interior.botiquinT = in.readFloat();
                p.rx = in.readFloat();
                p.ry = in.readFloat();
                p.rz = in.readFloat();
                try {
                    int nl = in.readInt();
                    for (int i = 0; i < nl; i++) {
                        boolean b = in.readBoolean();
                        if (i < Historia.LUGARES) {
                            h.tomado[i] = b;
                        }
                    }
                    p.hayBase = in.readBoolean();
                    p.baseX = in.readFloat();
                    p.baseY = in.readFloat();
                    p.baseZ = in.readFloat();
                    try {
                        p.rCasco = in.readFloat();
                        p.rAveriada = in.readBoolean();
                        p.cosecha = in.readInt();
                        p.granjaT = in.readFloat();
                        int eq = in.readInt();
                        p.equipo = eq >= 100 ? eq - 100 : (eq > 0 ? eq + 1 : 0);
                        p.arma = in.readInt();
                        try {
                            int nb = in.readInt();
                            for (int i = 0; i < nb; i++) {
                                boolean b = in.readBoolean();
                                if (i < Bestiario.N) {
                                    p.bestiario.conocido[i] = b;
                                }
                            }
                            try {
                                int ns = in.readInt();
                                for (int i = 0; i < ns; i++) {
                                    boolean b = in.readBoolean();
                                    float x = in.readFloat(), y = in.readFloat(), z = in.readFloat();
                                    if (i < Partida.NSENAL) {
                                        p.sbPuesta[i] = b;
                                        p.sbX[i] = x;
                                        p.sbY[i] = y;
                                        p.sbZ[i] = z;
                                    }
                                }
                                int nm = in.readInt();
                                for (int i = 0; i < nm; i += 8) {
                                    int b = in.readByte();
                                    for (int k = 0; k < 8 && i + k < Mapa.N * Mapa.N; k++) {
                                        p.mapa.visto[i + k] = (b & (1 << k)) != 0;
                                    }
                                }
                                try {
                                    int ne = in.readInt();
                                    for (int i = 0; i < ne; i++) {
                                        boolean b = in.readBoolean();
                                        if (i < Ecos.N) {
                                            p.ecos.hallado[i] = b;
                                        }
                                    }
                                } catch (java.io.IOException e6) {
                                    Log.d("partida anterior a 0.25: ecos sin encontrar");
                                }
                            } catch (java.io.IOException e5) {
                                Log.d("partida anterior a 0.24: mapa sin explorar");
                            }
                        } catch (java.io.IOException e4) {
                            Log.d("partida anterior a 0.22: bestiario vacio");
                        }
                    } catch (java.io.IOException e3) {
                        p.rCasco = 100;
                    }
                } catch (java.io.IOException e2) {
                    Log.d("partida anterior a 0.17: sin segundo acto ni base guardados");
                }
            } catch (java.io.IOException e) {
                Log.d("partida de una version anterior: historia desde el principio");
            }
            Log.d("partida cargada: " + d.length + " bytes, " + n + " objetos recogidos");
            return true;
        } catch (Throwable e) {
            Log.d("ERROR al cargar la partida: " + e);
            return false;
        }
    }
}
