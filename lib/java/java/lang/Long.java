package java.lang;

public final class Long {
    public static final long MIN_VALUE = 0x8000000000000000L;
    public static final long MAX_VALUE = 0x7fffffffffffffffL;
    private final long value;

    public Long(long v) {
        value = v;
    }

    public long longValue() {
        return value;
    }

    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    public boolean equals(Object o) {
        return o instanceof Long && ((Long) o).value == value;
    }

    public String toString() {
        return toString(value, 10);
    }

    public static long parseLong(String s) {
        return parseLong(s, 10);
    }

    public static long parseLong(String s, int radix) {
        if (s == null || s.length() == 0) {
            throw new NumberFormatException(s);
        }
        int i = 0;
        boolean neg = false;
        char c0 = s.charAt(0);
        if (c0 == '-' || c0 == '+') {
            neg = c0 == '-';
            i = 1;
            if (s.length() == 1) {
                throw new NumberFormatException(s);
            }
        }
        long v = 0;
        for (; i < s.length(); i++) {
            int d = Character.digit(s.charAt(i), radix);
            if (d < 0) {
                throw new NumberFormatException(s);
            }
            v = v * radix + d;
        }
        return neg ? -v : v;
    }

    public static String toString(long l) {
        return toString(l, 10);
    }

    public static String toString(long l, int radix) {
        if (l == 0) {
            return "0";
        }
        if (radix < 2 || radix > 36) {
            radix = 10;
        }
        char[] buf = new char[65];
        int p = 65;
        boolean neg = l < 0;
        if (!neg) {
            l = -l;
        }
        while (l != 0) {
            buf[--p] = Character.forDigit((int) -(l % radix), radix);
            l /= radix;
        }
        if (neg) {
            buf[--p] = '-';
        }
        return new String(buf, p, 65 - p);
    }

    static String toUnsigned(long l, int shift) {
        char[] buf = new char[64];
        int p = 64;
        int mask = (1 << shift) - 1;
        do {
            buf[--p] = Character.forDigit((int) (l & mask), 1 << shift);
            l >>>= shift;
        } while (l != 0);
        return new String(buf, p, 64 - p);
    }
}
