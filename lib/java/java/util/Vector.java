package java.util;

public class Vector {
    protected Object[] elementData;
    protected int elementCount;
    protected int capacityIncrement;

    public Vector() {
        this(10);
    }

    public Vector(int cap) {
        this(cap, 0);
    }

    public Vector(int cap, int inc) {
        elementData = new Object[cap];
        capacityIncrement = inc;
    }

    private void grow(int min) {
        if (min > elementData.length) {
            int n = capacityIncrement > 0 ? elementData.length + capacityIncrement : elementData.length * 2;
            if (n < min) {
                n = min;
            }
            Object[] d = new Object[n];
            System.arraycopy(elementData, 0, d, 0, elementCount);
            elementData = d;
        }
    }

    public synchronized int size() {
        return elementCount;
    }

    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }

    public synchronized void addElement(Object o) {
        grow(elementCount + 1);
        elementData[elementCount++] = o;
    }

    public synchronized Object elementAt(int i) {
        if (i >= elementCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return elementData[i];
    }

    public synchronized Object firstElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData[0];
    }

    public synchronized Object lastElement() {
        if (elementCount == 0) {
            throw new NoSuchElementException();
        }
        return elementData[elementCount - 1];
    }

    public synchronized void setElementAt(Object o, int i) {
        if (i >= elementCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        elementData[i] = o;
    }

    public synchronized void insertElementAt(Object o, int i) {
        if (i > elementCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        grow(elementCount + 1);
        System.arraycopy(elementData, i, elementData, i + 1, elementCount - i);
        elementData[i] = o;
        elementCount++;
    }

    public synchronized void removeElementAt(int i) {
        if (i >= elementCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        System.arraycopy(elementData, i + 1, elementData, i, elementCount - i - 1);
        elementData[--elementCount] = null;
    }

    public synchronized boolean removeElement(Object o) {
        int i = indexOf(o);
        if (i < 0) {
            return false;
        }
        removeElementAt(i);
        return true;
    }

    public synchronized void removeAllElements() {
        for (int i = 0; i < elementCount; i++) {
            elementData[i] = null;
        }
        elementCount = 0;
    }

    public int indexOf(Object o) {
        return indexOf(o, 0);
    }

    public synchronized int indexOf(Object o, int from) {
        for (int i = from; i < elementCount; i++) {
            if (o == null ? elementData[i] == null : o.equals(elementData[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public synchronized void copyInto(Object[] a) {
        System.arraycopy(elementData, 0, a, 0, elementCount);
    }

    public synchronized void setSize(int n) {
        grow(n);
        for (int i = n; i < elementCount; i++) {
            elementData[i] = null;
        }
        elementCount = n;
    }

    public synchronized Enumeration elements() {
        return new VectorEnumeration(this);
    }
}

class VectorEnumeration implements Enumeration {
    private final Vector v;
    private int i;

    VectorEnumeration(Vector v) {
        this.v = v;
    }

    public boolean hasMoreElements() {
        return i < v.size();
    }

    public Object nextElement() {
        return v.elementAt(i++);
    }
}
