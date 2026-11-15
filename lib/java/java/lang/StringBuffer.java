package java.lang;

public final class StringBuffer extends AbstractStringBuilder implements CharSequence {
    public StringBuffer() {
        super(16);
    }

    public StringBuffer(int cap) {
        super(cap);
    }

    public StringBuffer(String s) {
        super(s.length() + 16);
        appendStr(s);
    }

    public StringBuffer append(Object o) {
        appendStr(String.valueOf(o));
        return this;
    }

    public StringBuffer append(String s) {
        appendStr(s);
        return this;
    }

    public StringBuffer append(StringBuffer s) {
        appendStr(s == null ? null : s.toString());
        return this;
    }

    public StringBuffer append(char[] s) {
        appendChars(s, 0, s.length);
        return this;
    }

    public StringBuffer append(char[] s, int off, int n) {
        appendChars(s, off, n);
        return this;
    }

    public StringBuffer append(boolean b) {
        appendStr(b ? "true" : "false");
        return this;
    }

    public StringBuffer append(char c) {
        appendChar(c);
        return this;
    }

    public StringBuffer append(int i) {
        appendStr(Integer.toString(i));
        return this;
    }

    public StringBuffer append(long l) {
        appendStr(Long.toString(l));
        return this;
    }

    public StringBuffer append(float f) {
        appendStr(Float.toString(f));
        return this;
    }

    public StringBuffer append(double d) {
        appendStr(Double.toString(d));
        return this;
    }

    public StringBuffer insert(int at, String s) {
        insertStr(at, s);
        return this;
    }

    public StringBuffer insert(int at, char c) {
        insertStr(at, String.valueOf(c));
        return this;
    }

    public StringBuffer insert(int at, int i) {
        insertStr(at, Integer.toString(i));
        return this;
    }

    public StringBuffer insert(int at, Object o) {
        insertStr(at, String.valueOf(o));
        return this;
    }

    public StringBuffer delete(int start, int end) {
        deleteRange(start, end);
        return this;
    }

    public StringBuffer deleteCharAt(int i) {
        if (i < 0 || i >= count) {
            throw new StringIndexOutOfBoundsException();
        }
        deleteRange(i, i + 1);
        return this;
    }

    public StringBuffer reverse() {
        reverseChars();
        return this;
    }

    public String toString() {
        return new String(value, 0, count);
    }
}
