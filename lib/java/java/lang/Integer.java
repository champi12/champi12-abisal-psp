package java.lang;

public final class Integer {
    public static final int MIN_VALUE = 0x80000000;
    public static final int MAX_VALUE = 0x7fffffff;
    private final int value;

    public Integer(int v) {
        value = v;
    }

    public int intValue() {
        return value;
    }

    public long longValue() {
        return value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public byte byteValue() {
        return (byte) value;
    }

    public short shortValue() {
        return (short) value;
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(Object o) {
        return o instanceof Integer && ((Integer) o).value == value;
    }

    public String toString() {
        return toString(value);
    }

    public static Integer valueOf(String s) {
        return new Integer(parseInt(s, 10));
    }

    public static Integer valueOf(String s, int radix) {
        return new Integer(parseInt(s, radix));
    }

    public static int parseInt(String s) {
        return parseInt(s, 10);
    }

    public static int parseInt(String s, int radix) {
        long v = Long.parseLong(s, radix);
        if (v < MIN_VALUE || v > MAX_VALUE) {
            throw new NumberFormatException(s);
        }
        return (int) v;
    }

    public static String toString(int i) {
        return Long.toString(i, 10);
    }

    public static String toString(int i, int radix) {
        return Long.toString(i, radix);
    }

    public static String toHexString(int i) {
        return Long.toUnsigned(i & 0xffffffffL, 4);
    }

    public static String toOctalString(int i) {
        return Long.toUnsigned(i & 0xffffffffL, 3);
    }

    public static String toBinaryString(int i) {
        return Long.toUnsigned(i & 0xffffffffL, 1);
    }
}
