package java.lang;

import java.io.InputStream;

public final class Class {
    long vmClass;
    private String name;

    private Class() {
    }

    public static Class forName(String name) throws ClassNotFoundException {
        Class c = forName0(name.replace('.', '/'));
        if (c == null) {
            throw new ClassNotFoundException(name);
        }
        return c;
    }

    private static native Class forName0(String internalName);

    private native String getName0();

    private native Object newInstance0();

    public native boolean isInstance(Object o);

    public native boolean isAssignableFrom(Class c);

    public native boolean isInterface();

    public native boolean isArray();

    public String getName() {
        if (name == null) {
            name = getName0().replace('/', '.');
        }
        return name;
    }

    public Object newInstance() throws InstantiationException, IllegalAccessException {
        Object o = newInstance0();
        if (o == null) {
            throw new InstantiationException(getName());
        }
        return o;
    }

    public InputStream getResourceAsStream(String res) {
        if (res.length() > 0 && res.charAt(0) == '/') {
            res = res.substring(1);
        } else {
            String n = getName();
            int p = n.lastIndexOf('.');
            if (p >= 0) {
                res = n.substring(0, p).replace('.', '/') + "/" + res;
            }
        }
        return j2me.ResourceInputStream.open(res);
    }

    public String toString() {
        return (isInterface() ? "interface " : "class ") + getName();
    }
}
