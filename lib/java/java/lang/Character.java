package java.lang;

public final class Character {
    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;
    public static final char MIN_VALUE = '\u0000';
    public static final char MAX_VALUE = '￿';
    private final char value;

    public Character(char v) {
        value = v;
    }

    public char charValue() {
        return value;
    }

    public int hashCode() {
        return value;
    }

    public boolean equals(Object o) {
        return o instanceof Character && ((Character) o).value == value;
    }

    public String toString() {
        return String.valueOf(value);
    }

    public static boolean isLowerCase(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 0xdf && c <= 0xff && c != 0xf7);
    }

    public static boolean isUpperCase(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 0xc0 && c <= 0xde && c != 0xd7);
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    public static char toLowerCase(char c) {
        if ((c >= 'A' && c <= 'Z') || (c >= 0xc0 && c <= 0xde && c != 0xd7)) {
            return (char) (c + 32);
        }
        if (c >= 0x410 && c <= 0x42f) {
            return (char) (c + 32);
        }
        return c;
    }

    public static char toUpperCase(char c) {
        if ((c >= 'a' && c <= 'z') || (c >= 0xe0 && c <= 0xfe && c != 0xf7)) {
            return (char) (c - 32);
        }
        if (c >= 0x430 && c <= 0x44f) {
            return (char) (c - 32);
        }
        return c;
    }

    public static int digit(char c, int radix) {
        int d;
        if (c >= '0' && c <= '9') {
            d = c - '0';
        } else if (c >= 'a' && c <= 'z') {
            d = c - 'a' + 10;
        } else if (c >= 'A' && c <= 'Z') {
            d = c - 'A' + 10;
        } else {
            return -1;
        }
        return d < radix ? d : -1;
    }

    public static char forDigit(int d, int radix) {
        if (d < 0 || d >= radix) {
            return 0;
        }
        return (char) (d < 10 ? '0' + d : 'a' + d - 10);
    }
}
