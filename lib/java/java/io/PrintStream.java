package java.io;

public class PrintStream extends OutputStream {
    private OutputStream out;

    public PrintStream(OutputStream out) {
        this.out = out;
    }

    public void write(int b) {
        try {
            out.write(b);
        } catch (IOException e) {
        }
    }

    public void write(byte[] b, int off, int len) {
        try {
            out.write(b, off, len);
        } catch (IOException e) {
        }
    }

    public void flush() {
        try {
            out.flush();
        } catch (IOException e) {
        }
    }

    public boolean checkError() {
        return false;
    }

    public void print(String s) {
        byte[] b = (s == null ? "null" : s).getBytes();
        write(b, 0, b.length);
    }

    public void print(Object o) {
        print(String.valueOf(o));
    }

    public void print(int i) {
        print(String.valueOf(i));
    }

    public void print(long l) {
        print(String.valueOf(l));
    }

    public void print(char c) {
        print(String.valueOf(c));
    }

    public void print(boolean b) {
        print(String.valueOf(b));
    }

    public void print(float f) {
        print(String.valueOf(f));
    }

    public void print(double d) {
        print(String.valueOf(d));
    }

    public void println() {
        write('\n');
        flush();
    }

    public void println(String s) {
        print(s);
        println();
    }

    public void println(Object o) {
        print(o);
        println();
    }

    public void println(int i) {
        print(i);
        println();
    }

    public void println(long l) {
        print(l);
        println();
    }

    public void println(char c) {
        print(c);
        println();
    }

    public void println(boolean b) {
        print(b);
        println();
    }

    public void println(float f) {
        print(f);
        println();
    }

    public void println(double d) {
        print(d);
        println();
    }
}
