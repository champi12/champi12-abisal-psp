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

    private Guardado() {
    }

    static boolean existe() {
        try {
            RecordStore rs = RecordStore.openRecordStore(NOMBRE, false);
            boolean hay = rs.getNumRecords() > 0;
            rs.closeRecordStore();
            return hay;
        } catch (Throwable e) {
            return false;
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
            o.writeInt(p.equipo);
            o.writeInt(p.arma);
            o.flush();
            byte[] d = bo.toByteArray();
            RecordStore rs = RecordStore.openRecordStore(NOMBRE, true);
            if (rs.getNumRecords() == 0) {
                rs.addRecord(d, 0, d.length);
            } else {
                rs.setRecord(1, d, 0, d.length);
            }
            rs.closeRecordStore();
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
            RecordStore rs = RecordStore.openRecordStore(NOMBRE, false);
            byte[] d = rs.getRecord(1);
            rs.closeRecordStore();
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
                        p.equipo = in.readInt();
                        p.arma = in.readInt();
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
