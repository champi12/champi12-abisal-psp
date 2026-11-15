package abisal;

/**
 * PSP controls as seen by the game. The runtime maps the buttons to MIDP key codes
 * (cross -5, circle -7, start -6, triangle '0', square '9', L '1', R '3', select '#',
 * d-pad -1..-4); the analog stick is read directly.
 */
public final class Entrada {
    public static final int ARRIBA = 1, ABAJO = 2, IZQ = 4, DER = 8, X = 16, CIRCULO = 32, CUADRADO = 64,
            TRIANGULO = 128, L = 256, R = 512, START = 1024, SELECT = 2048;

    int abajo;      // buttons held
    int pulsado;    // buttons pressed this frame
    /** analog stick, -1..1 with a dead zone */
    float ax, ay;

    static int boton(int k) {
        switch (k) {
        case -1: return ARRIBA;
        case -2: return ABAJO;
        case -3: return IZQ;
        case -4: return DER;
        case -5: return X;
        case -7: return CIRCULO;
        case -6: return START;
        case '0': return TRIANGULO;
        case '9': return CUADRADO;
        case '1': return L;
        case '3': return R;
        case '#': return SELECT;
        default: return 0;
        }
    }

    synchronized void presionar(int k) {
        int b = boton(k);
        abajo |= b;
        pulsado |= b;
    }

    synchronized void soltar(int k) {
        abajo &= ~boton(k);
    }

    synchronized void finFrame() {
        pulsado = 0;
    }

    public boolean tiene(int b) {
        return (abajo & b) != 0;
    }

    public boolean pulso(int b) {
        return (pulsado & b) != 0;
    }

    void leerAnalogico() {
        int a = j2me.Pad.analog();
        ax = eje((a >> 8) & 255);
        ay = eje(a & 255);
    }

    private static float eje(int v) {
        float f = (v - 128) / 127f;
        float dz = 0.22f;
        if (f > dz) {
            return (f - dz) / (1 - dz);
        }
        if (f < -dz) {
            return (f + dz) / (1 - dz);
        }
        return 0;
    }
}
