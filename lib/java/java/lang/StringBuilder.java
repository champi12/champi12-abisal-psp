package java.lang;

public final class StringBuilder extends AbstractStringBuilder implements CharSequence {
    public StringBuilder() {
        super(16);
    }

    public StringBuilder(int cap) {
        super(cap);
    }

    public StringBuilder(String s) {
        super(s.length() + 16);
        appendStr(s);
    }

    public StringBuilder append(Object o) {
        appendStr(String.valueOf(o));
        return this;
    }

    public StringBuilder append(String s) {
        appendStr(s);
        return this;
    }

    public StringBuilder append(StringBuffer s) {
        appendStr(s == null ? null : s.toString());
        return this;
    }

    public StringBuilder append(char[] s) {
        appendChars(s, 0, s.length);
        return this;
    }

    public StringBuilder append(char[] s, int off, int n) {
        appendChars(s, off, n);
        return this;
    }

    public StringBuilder append(boolean b) {
        appendStr(b ? "true" : "false");
        return this;
    }

    public StringBuilder append(char c) {
        appendChar(c);
        return this;
    }

    public StringBuilder append(int i) {
        appendStr(Integer.toString(i));
        return this;
    }

    public StringBuilder append(long l) {
        appendStr(Long.toString(l));
        return this;
    }

    public StringBuilder append(float f) {
        appendStr(Float.toString(f));
        return this;
    }

    public StringBuilder append(double d) {
        appendStr(Double.toString(d));
        return this;
    }

    public StringBuilder insert(int at, String s) {
        insertStr(at, s);
        return this;
    }

    public StringBuilder insert(int at, char c) {
        insertStr(at, String.valueOf(c));
        return this;
    }

    public StringBuilder insert(int at, int i) {
        insertStr(at, Integer.toString(i));
        return this;
    }

    public StringBuilder insert(int at, Object o) {
        insertStr(at, String.valueOf(o));
        return this;
    }

    public StringBuilder delete(int start, int end) {
        deleteRange(start, end);
        return this;
    }

    public StringBuilder deleteCharAt(int i) {
        if (i < 0 || i >= count) {
            throw new StringIndexOutOfBoundsException();
        }
        deleteRange(i, i + 1);
        return this;
    }

    public StringBuilder reverse() {
        reverseChars();
        return this;
    }

    public String toString() {
        return new String(value, 0, count);
    }
}
