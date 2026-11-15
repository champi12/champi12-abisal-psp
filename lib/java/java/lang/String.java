package java.lang;

/** Immutable UTF-16 string (CLDC 1.1 subset). Layout is shared with the C runtime. */
public final class String implements CharSequence, Comparable {
    char[] value;
    int offset;
    int count;
    private int hash;

    public String() {
        value = new char[0];
    }

    public String(String s) {
        value = s.value;
        offset = s.offset;
        count = s.count;
    }

    public String(char[] v) {
        this(v, 0, v.length);
    }

    public String(char[] v, int off, int len) {
        if (off < 0 || len < 0 || off > v.length - len) {
            throw new StringIndexOutOfBoundsException();
        }
        value = new char[len];
        System.arraycopy(v, off, value, 0, len);
        count = len;
    }

    public String(byte[] b) {
        this(b, 0, b.length);
    }

    public String(byte[] b, int off, int len) {
        value = j2me.VM.decodeUTF8(b, off, len);
        count = value.length;
    }

    public String(byte[] b, String enc) {
        this(b, 0, b.length);
    }

    public String(byte[] b, int off, int len, String enc) {
        this(b, off, len);
    }

    String(int off, int len, char[] v) {
        value = v;
        offset = off;
        count = len;
    }

    public String(StringBuffer sb) {
        this(sb.getValue(), 0, sb.length());
    }

    public int length() {
        return count;
    }

    public char charAt(int i) {
        if (i < 0 || i >= count) {
            throw new StringIndexOutOfBoundsException();
        }
        return value[offset + i];
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcEnd > count || srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException();
        }
        System.arraycopy(value, offset + srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    public byte[] getBytes() {
        return j2me.VM.encodeUTF8(value, offset, count);
    }

    public byte[] getBytes(String enc) {
        return getBytes();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof String)) {
            return false;
        }
        String s = (String) o;
        if (s.count != count) {
            return false;
        }
        char[] a = value;
        char[] b = s.value;
        int i = offset;
        int j = s.offset;
        for (int n = count; n > 0; n--) {
            if (a[i++] != b[j++]) {
                return false;
            }
        }
        return true;
    }

    public boolean equalsIgnoreCase(String s) {
        if (s == null || s.count != count) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            char a = value[offset + i];
            char b = s.value[s.offset + i];
            if (a != b && Character.toLowerCase(a) != Character.toLowerCase(b)
                    && Character.toUpperCase(a) != Character.toUpperCase(b)) {
                return false;
            }
        }
        return true;
    }

    public int compareTo(String s) {
        int n = count < s.count ? count : s.count;
        for (int i = 0; i < n; i++) {
            int d = value[offset + i] - s.value[s.offset + i];
            if (d != 0) {
                return d;
            }
        }
        return count - s.count;
    }

    public int compareTo(Object o) {
        return compareTo((String) o);
    }

    public boolean regionMatches(boolean ignoreCase, int toff, String other, int ooff, int len) {
        if (toff < 0 || ooff < 0 || toff > count - len || ooff > other.count - len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char a = value[offset + toff + i];
            char b = other.value[other.offset + ooff + i];
            if (a == b) {
                continue;
            }
            if (ignoreCase && (Character.toLowerCase(a) == Character.toLowerCase(b)
                    || Character.toUpperCase(a) == Character.toUpperCase(b))) {
                continue;
            }
            return false;
        }
        return true;
    }

    public boolean startsWith(String p, int off) {
        return regionMatches(false, off, p, 0, p.count);
    }

    public boolean startsWith(String p) {
        return startsWith(p, 0);
    }

    public boolean endsWith(String s) {
        return startsWith(s, count - s.count);
    }

    public int hashCode() {
        int h = hash;
        if (h == 0) {
            for (int i = 0; i < count; i++) {
                h = 31 * h + value[offset + i];
            }
            hash = h;
        }
        return h;
    }

    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public int indexOf(int ch, int from) {
        if (from < 0) {
            from = 0;
        }
        for (int i = from; i < count; i++) {
            if (value[offset + i] == ch) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, count - 1);
    }

    public int lastIndexOf(int ch, int from) {
        if (from >= count) {
            from = count - 1;
        }
        for (int i = from; i >= 0; i--) {
            if (value[offset + i] == ch) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(String s) {
        return indexOf(s, 0);
    }

    public int indexOf(String s, int from) {
        if (from < 0) {
            from = 0;
        }
        for (int i = from; i <= count - s.count; i++) {
            if (regionMatches(false, i, s, 0, s.count)) {
                return i;
            }
        }
        return -1;
    }

    public String substring(int begin) {
        return substring(begin, count);
    }

    public String substring(int begin, int end) {
        if (begin < 0 || end > count || begin > end) {
            throw new StringIndexOutOfBoundsException();
        }
        if (begin == 0 && end == count) {
            return this;
        }
        return new String(offset + begin, end - begin, value);
    }

    public String concat(String s) {
        if (s.count == 0) {
            return this;
        }
        char[] v = new char[count + s.count];
        System.arraycopy(value, offset, v, 0, count);
        System.arraycopy(s.value, s.offset, v, count, s.count);
        return new String(0, v.length, v);
    }

    public String replace(char oldChar, char newChar) {
        if (indexOf(oldChar) < 0) {
            return this;
        }
        char[] v = new char[count];
        for (int i = 0; i < count; i++) {
            char c = value[offset + i];
            v[i] = c == oldChar ? newChar : c;
        }
        return new String(0, count, v);
    }

    public String toLowerCase() {
        char[] v = new char[count];
        for (int i = 0; i < count; i++) {
            v[i] = Character.toLowerCase(value[offset + i]);
        }
        return new String(0, count, v);
    }

    public String toUpperCase() {
        char[] v = new char[count];
        for (int i = 0; i < count; i++) {
            v[i] = Character.toUpperCase(value[offset + i]);
        }
        return new String(0, count, v);
    }

    public String trim() {
        int b = 0;
        int e = count;
        while (b < e && value[offset + b] <= ' ') {
            b++;
        }
        while (e > b && value[offset + e - 1] <= ' ') {
            e--;
        }
        return substring(b, e);
    }

    public String toString() {
        return this;
    }

    public char[] toCharArray() {
        char[] v = new char[count];
        System.arraycopy(value, offset, v, 0, count);
        return v;
    }

    public String intern() {
        return j2me.VM.intern(this);
    }

    public static String valueOf(Object o) {
        return o == null ? "null" : o.toString();
    }

    public static String valueOf(char[] d) {
        return new String(d);
    }

    public static String valueOf(char[] d, int off, int n) {
        return new String(d, off, n);
    }

    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }

    public static String valueOf(char c) {
        char[] v = new char[1];
        v[0] = c;
        return new String(0, 1, v);
    }

    public static String valueOf(int i) {
        return Integer.toString(i);
    }

    public static String valueOf(long l) {
        return Long.toString(l);
    }

    public static String valueOf(float f) {
        return Float.toString(f);
    }

    public static String valueOf(double d) {
        return Double.toString(d);
    }
}
