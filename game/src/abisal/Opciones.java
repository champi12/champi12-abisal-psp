package abisal;

import javax.microedition.rms.RecordStore;

/** Player settings that persist across games (record store "abisal_opciones"). */
public final class Opciones {
    /** look with the d-pad instead of the analog stick */
    static boolean cruceta;

    private Opciones() {
    }

    static void cargar() {
        try {
            RecordStore rs = RecordStore.openRecordStore("abisal_opciones", false);
            byte[] d = rs.getRecord(1);
            rs.closeRecordStore();
            cruceta = d.length > 0 && d[0] == 1;
            Log.d("opciones cargadas: mirar con " + (cruceta ? "cruceta" : "stick"));
        } catch (Throwable e) {
            Log.d("opciones: valores por defecto (mirar con stick)");
        }
    }

    static void guardar() {
        try {
            byte[] d = { (byte) (cruceta ? 1 : 0) };
            RecordStore rs = RecordStore.openRecordStore("abisal_opciones", true);
            if (rs.getNumRecords() == 0) {
                rs.addRecord(d, 0, 1);
            } else {
                rs.setRecord(1, d, 0, 1);
            }
            rs.closeRecordStore();
            Log.d("opciones guardadas: mirar con " + (cruceta ? "cruceta" : "stick"));
        } catch (Throwable e) {
            Log.d("ERROR al guardar opciones: " + e);
        }
    }
}
