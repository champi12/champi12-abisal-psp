package java.lang;

public class Thread implements Runnable {
    public static final int MIN_PRIORITY = 1;
    public static final int NORM_PRIORITY = 5;
    public static final int MAX_PRIORITY = 10;

    private Runnable target;
    private String name;
    private int priority = NORM_PRIORITY;
    long vmThread;
    boolean started;
    boolean alive;
    private static int counter;

    public Thread() {
        this(null, null);
    }

    public Thread(Runnable target) {
        this(target, null);
    }

    public Thread(String name) {
        this(null, name);
    }

    public Thread(Runnable target, String name) {
        this.target = target;
        this.name = name != null ? name : "Thread-" + (counter++);
    }

    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public synchronized void start() {
        if (started) {
            throw new IllegalThreadStateException();
        }
        started = true;
        alive = true;
        start0();
    }

    private native void start0();

    /** Called by the runtime on the new native thread. */
    static void entry(Thread t) {
        try {
            t.run();
        } catch (Throwable e) {
            System.err.println("Uncaught exception in " + t.name + ": " + e);
        }
        t.alive = false;
    }

    public static native Thread currentThread();

    public static native void sleep(long millis) throws InterruptedException;

    public static native void yield();

    public final boolean isAlive() {
        return alive;
    }

    public final void setPriority(int p) {
        priority = p;
    }

    public final int getPriority() {
        return priority;
    }

    public final String getName() {
        return name;
    }

    public void interrupt() {
    }

    public final void join() throws InterruptedException {
        while (alive) {
            sleep(10);
        }
    }

    public static int activeCount() {
        return 1;
    }

    public String toString() {
        return "Thread[" + name + "]";
    }
}
