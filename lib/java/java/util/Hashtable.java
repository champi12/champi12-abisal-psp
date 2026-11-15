package java.util;

public class Hashtable {
    private Object[] keys;
    private Object[] values;
    private int count;

    public Hashtable() {
        this(16);
    }

    public Hashtable(int cap) {
        int n = 16;
        while (n < cap * 2) {
            n <<= 1;
        }
        keys = new Object[n];
        values = new Object[n];
    }

    private int slot(Object k) {
        int mask = keys.length - 1;
        int i = k.hashCode() & mask;
        while (keys[i] != null && !keys[i].equals(k)) {
            i = (i + 1) & mask;
        }
        return i;
    }

    public synchronized Object get(Object k) {
        return values[slot(k)];
    }

    public synchronized boolean containsKey(Object k) {
        return keys[slot(k)] != null;
    }

    public synchronized Object put(Object k, Object v) {
        if (k == null || v == null) {
            throw new NullPointerException();
        }
        if ((count + 1) * 2 > keys.length) {
            rehash();
        }
        int i = slot(k);
        Object old = values[i];
        if (keys[i] == null) {
            count++;
        }
        keys[i] = k;
        values[i] = v;
        return old;
    }

    private void rehash() {
        Object[] ok = keys;
        Object[] ov = values;
        keys = new Object[ok.length * 2];
        values = new Object[ok.length * 2];
        for (int i = 0; i < ok.length; i++) {
            if (ok[i] != null) {
                int s = slot(ok[i]);
                keys[s] = ok[i];
                values[s] = ov[i];
            }
        }
    }

    public synchronized Object remove(Object k) {
        int i = slot(k);
        if (keys[i] == null) {
            return null;
        }
        Object old = values[i];
        keys[i] = null;
        values[i] = null;
        count--;
        // reinsert following cluster
        int mask = keys.length - 1;
        int j = (i + 1) & mask;
        while (keys[j] != null) {
            Object kk = keys[j];
            Object vv = values[j];
            keys[j] = null;
            values[j] = null;
            int s = slot(kk);
            keys[s] = kk;
            values[s] = vv;
            j = (j + 1) & mask;
        }
        return old;
    }

    public synchronized int size() {
        return count;
    }

    public synchronized boolean isEmpty() {
        return count == 0;
    }

    public synchronized void clear() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = null;
            values[i] = null;
        }
        count = 0;
    }

    public synchronized Enumeration keys() {
        Vector v = new Vector();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                v.addElement(keys[i]);
            }
        }
        return v.elements();
    }

    public synchronized Enumeration elements() {
        Vector v = new Vector();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                v.addElement(values[i]);
            }
        }
        return v.elements();
    }
}
