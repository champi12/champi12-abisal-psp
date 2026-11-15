package java.lang;

abstract class AbstractStringBuilder {
    char[] value;
    int count;

    AbstractStringBuilder(int cap) {
        value = new char[cap];
    }

    char[] getValue() {
        return value;
    }

    public int length() {
        return count;
    }

    public int capacity() {
        return value.length;
    }

    void ensure(int min) {
        if (min > value.length) {
            int n = value.length * 2 + 2;
            if (n < min) {
                n = min;
            }
            char[] v = new char[n];
            System.arraycopy(value, 0, v, 0, count);
            value = v;
        }
    }

    public void ensureCapacity(int min) {
        ensure(min);
    }

    public void setLength(int n) {
        if (n < 0) {
            throw new StringIndexOutOfBoundsException();
        }
        ensure(n);
        for (int i = count; i < n; i++) {
            value[i] = 0;
        }
        count = n;
    }

    public char charAt(int i) {
        if (i < 0 || i >= count) {
            throw new StringIndexOutOfBoundsException();
        }
        return value[i];
    }

    public void setCharAt(int i, char c) {
        if (i < 0 || i >= count) {
            throw new StringIndexOutOfBoundsException();
        }
        value[i] = c;
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    void appendStr(String s) {
        if (s == null) {
            s = "null";
        }
        int n = s.count;
        ensure(count + n);
        System.arraycopy(s.value, s.offset, value, count, n);
        count += n;
    }

    void appendChar(char c) {
        ensure(count + 1);
        value[count++] = c;
    }

    void appendChars(char[] s, int off, int n) {
        ensure(count + n);
        System.arraycopy(s, off, value, count, n);
        count += n;
    }

    void insertStr(int at, String s) {
        if (at < 0 || at > count) {
            throw new StringIndexOutOfBoundsException();
        }
        if (s == null) {
            s = "null";
        }
        int n = s.count;
        ensure(count + n);
        System.arraycopy(value, at, value, at + n, count - at);
        System.arraycopy(s.value, s.offset, value, at, n);
        count += n;
    }

    void deleteRange(int start, int end) {
        if (end > count) {
            end = count;
        }
        if (start < 0 || start > end) {
            throw new StringIndexOutOfBoundsException();
        }
        System.arraycopy(value, end, value, start, count - end);
        count -= end - start;
    }

    void reverseChars() {
        for (int i = 0, j = count - 1; i < j; i++, j--) {
            char t = value[i];
            value[i] = value[j];
            value[j] = t;
        }
    }

    public String toString() {
        return new String(value, 0, count);
    }
}
