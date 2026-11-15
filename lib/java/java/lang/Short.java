package java.lang;

public final class Short {
    public static final short MIN_VALUE = -32768;
    public static final short MAX_VALUE = 32767;
    private final short value;

    public Short(short v) {
        value = v;
    }

    public short shortValue() {
        return value;
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(Object o) {
        return o instanceof Short && ((Short) o).value == value;
    }

    public String toString() {
        return Integer.toString(value);
    }

    public static short parseShort(String s) {
        return parseShort(s, 10);
    }

    public static short parseShort(String s, int radix) {
        int v = Integer.parseInt(s, radix);
        if (v < MIN_VALUE || v > MAX_VALUE) {
            throw new NumberFormatException(s);
        }
        return (short) v;
    }
}
