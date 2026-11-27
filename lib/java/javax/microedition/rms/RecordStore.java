package javax.microedition.rms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

/**
 * Record stores are kept in memory and written to one file per store (via natives)
 * whenever they change.
 */
public class RecordStore {
    public static final int AUTHMODE_PRIVATE = 0;
    public static final int AUTHMODE_ANY = 1;

    private static final Vector open = new Vector();
    private final String name;
    private int[] ids = new int[0];
    private byte[][] data = new byte[0][];
    private int nextId = 1;
    private int version;
    private long modified;
    private int openCount;

    private RecordStore(String name) {
        this.name = name;
    }

    private static String fileName(String name) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                sb.append('_').append(Integer.toHexString(c));
            }
        }
        return sb.toString();
    }

    public static synchronized RecordStore openRecordStore(String name, boolean create)
            throws RecordStoreException {
        for (int i = 0; i < open.size(); i++) {
            RecordStore rs = (RecordStore) open.elementAt(i);
            if (rs.name.equals(name)) {
                rs.openCount++;
                return rs;
            }
        }
        byte[] raw = j2me.Storage.read(fileName(name));
        RecordStore rs = new RecordStore(name);
        if (raw == null) {
            if (!create) {
                throw new RecordStoreNotFoundException(name);
            }
            rs.save();
        } else {
            rs.load(raw);
        }
        rs.openCount = 1;
        open.addElement(rs);
        return rs;
    }

    public static RecordStore openRecordStore(String name, boolean create, int auth, boolean writable)
            throws RecordStoreException {
        return openRecordStore(name, create);
    }

    public static RecordStore openRecordStore(String name, String vendor, String suite)
            throws RecordStoreException {
        return openRecordStore(name, false);
    }

    public static synchronized void deleteRecordStore(String name) throws RecordStoreException {
        for (int i = 0; i < open.size(); i++) {
            RecordStore rs = (RecordStore) open.elementAt(i);
            if (rs.name.equals(name)) {
                open.removeElementAt(i);
                break;
            }
        }
        if (!j2me.Storage.delete(fileName(name))) {
            throw new RecordStoreNotFoundException(name);
        }
    }

    public static String[] listRecordStores() {
        return null;
    }

    private void load(byte[] raw) throws RecordStoreException {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
            nextId = in.readInt();
            version = in.readInt();
            modified = in.readLong();
            int n = in.readInt();
            if (n < 0 || n > 100000) {
                throw new RecordStoreException("corrupt record store " + name + " (n=" + n + ")");
            }
            ids = new int[n];
            data = new byte[n][];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                int len = in.readInt();
                if (len < -1 || len > 10000000) {
                    throw new RecordStoreException("corrupt record store " + name + " (len=" + len + ")");
                }
                if (len >= 0) {
                    data[i] = new byte[len];
                    in.readFully(data[i]);
                }
            }
        } catch (java.io.IOException e) {
            throw new RecordStoreException("corrupt record store " + name);
        } catch (NegativeArraySizeException e) {
            throw new RecordStoreException("corrupt record store " + name);
        } catch (OutOfMemoryError e) {
            throw new RecordStoreException("record store " + name + " too large");
        }
    }

    private void save() throws RecordStoreException {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bo);
            out.writeInt(nextId);
            out.writeInt(version);
            out.writeLong(modified);
            out.writeInt(ids.length);
            for (int i = 0; i < ids.length; i++) {
                out.writeInt(ids[i]);
                if (data[i] == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(data[i].length);
                    out.write(data[i], 0, data[i].length);
                }
            }
            if (!j2me.Storage.write(fileName(name), bo.toByteArray())) {
                throw new RecordStoreFullException();
            }
        } catch (java.io.IOException e) {
            throw new RecordStoreException(e.toString());
        }
    }

    private void check() throws RecordStoreNotOpenException {
        if (openCount <= 0) {
            throw new RecordStoreNotOpenException();
        }
    }

    private int find(int id) throws InvalidRecordIDException {
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == id) {
                return i;
            }
        }
        throw new InvalidRecordIDException();
    }

    private void changed() throws RecordStoreException {
        version++;
        modified = System.currentTimeMillis();
        save();
    }

    public synchronized void closeRecordStore() throws RecordStoreException {
        check();
        if (--openCount == 0) {
            open.removeElement(this);
        }
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public long getLastModified() {
        return modified;
    }

    public synchronized int getNumRecords() throws RecordStoreNotOpenException {
        check();
        return ids.length;
    }

    public int getSize() {
        int s = 0;
        for (int i = 0; i < data.length; i++) {
            s += data[i] == null ? 0 : data[i].length;
        }
        return s;
    }

    public int getSizeAvailable() {
        return 1 << 20;
    }

    public synchronized int getNextRecordID() throws RecordStoreException {
        check();
        return nextId;
    }

    public synchronized int addRecord(byte[] d, int off, int len) throws RecordStoreException {
        check();
        int n = ids.length;
        int[] ni = new int[n + 1];
        byte[][] nd = new byte[n + 1][];
        System.arraycopy(ids, 0, ni, 0, n);
        System.arraycopy(data, 0, nd, 0, n);
        ni[n] = nextId++;
        if (d != null) {
            nd[n] = new byte[len];
            System.arraycopy(d, off, nd[n], 0, len);
        } else {
            nd[n] = new byte[0];
        }
        ids = ni;
        data = nd;
        changed();
        return ni[n];
    }

    public synchronized void deleteRecord(int id) throws RecordStoreException {
        check();
        int i = find(id);
        int n = ids.length;
        int[] ni = new int[n - 1];
        byte[][] nd = new byte[n - 1][];
        System.arraycopy(ids, 0, ni, 0, i);
        System.arraycopy(ids, i + 1, ni, i, n - i - 1);
        System.arraycopy(data, 0, nd, 0, i);
        System.arraycopy(data, i + 1, nd, i, n - i - 1);
        ids = ni;
        data = nd;
        changed();
    }

    public synchronized int getRecordSize(int id) throws RecordStoreException {
        check();
        byte[] d = data[find(id)];
        return d == null ? 0 : d.length;
    }

    public synchronized int getRecord(int id, byte[] buf, int off) throws RecordStoreException {
        check();
        byte[] d = data[find(id)];
        if (d == null) {
            return 0;
        }
        System.arraycopy(d, 0, buf, off, d.length);
        return d.length;
    }

    public synchronized byte[] getRecord(int id) throws RecordStoreException {
        check();
        byte[] d = data[find(id)];
        if (d == null || d.length == 0) {
            return null;
        }
        byte[] r = new byte[d.length];
        System.arraycopy(d, 0, r, 0, d.length);
        return r;
    }

    public synchronized void setRecord(int id, byte[] d, int off, int len) throws RecordStoreException {
        check();
        int i = find(id);
        data[i] = new byte[len];
        if (d != null) {
            System.arraycopy(d, off, data[i], 0, len);
        }
        changed();
    }

    public synchronized RecordEnumeration enumerateRecords(RecordFilter f, RecordComparator c, boolean keep)
            throws RecordStoreNotOpenException {
        check();
        int[] copy = new int[ids.length];
        System.arraycopy(ids, 0, copy, 0, ids.length);
        return new RecordEnumerationImpl(this, copy);
    }

    public void addRecordListener(RecordListener l) {
    }

    public void removeRecordListener(RecordListener l) {
    }

    public void setMode(int auth, boolean writable) {
    }
}

class RecordEnumerationImpl implements RecordEnumeration {
    private final RecordStore rs;
    private final int[] ids;
    private int pos;

    RecordEnumerationImpl(RecordStore rs, int[] ids) {
        this.rs = rs;
        this.ids = ids;
    }

    public int numRecords() {
        return ids.length;
    }

    public byte[] nextRecord() throws RecordStoreException {
        return rs.getRecord(nextRecordId());
    }

    public int nextRecordId() throws InvalidRecordIDException {
        if (pos >= ids.length) {
            throw new InvalidRecordIDException();
        }
        return ids[pos++];
    }

    public byte[] previousRecord() throws RecordStoreException {
        return rs.getRecord(previousRecordId());
    }

    public int previousRecordId() throws InvalidRecordIDException {
        if (pos <= 0) {
            throw new InvalidRecordIDException();
        }
        return ids[--pos];
    }

    public boolean hasNextElement() {
        return pos < ids.length;
    }

    public boolean hasPreviousElement() {
        return pos > 0;
    }

    public void reset() {
        pos = 0;
    }

    public void rebuild() {
    }

    public void keepUpdated(boolean k) {
    }

    public boolean isKeptUpdated() {
        return false;
    }

    public void destroy() {
    }
}
