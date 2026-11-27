#!/usr/bin/env python3
"""Hace lineaLibre mas denso (cada 0.15 m en vez de 0.6 m)."""
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Mundo.java')
texto = open(ruta, 'r', encoding='utf-8').read()

viejo = '''    /** true if a straight line between two points does not cross rock walls (a sample every 0.6 m) */
    boolean lineaLibre(float ax, float ay, float az, float bx, float by, float bz) {
        float lx = bx - ax, ly = by - ay, lz = bz - az;
        int n = Math.min(40, 2 + (int) (j2me.FMath.sqrt(lx * lx + ly * ly + lz * lz) / 0.6f));
        for (int i = 1; i < n; i++) {
            float f = (float) i / n;
            if (enRoca(ax + (bx - ax) * f, ay + (by - ay) * f, az + (bz - az) * f)) {
                return false;
            }
        }
        return true;
    }'''

nuevo = '''    /** true if a straight line between two points does not cross rock walls (a sample every 0.15 m) */
    boolean lineaLibre(float ax, float ay, float az, float bx, float by, float bz) {
        float lx = bx - ax, ly = by - ay, lz = bz - az;
        float dist = j2me.FMath.sqrt(lx * lx + ly * ly + lz * lz);
        int n = Math.min(200, 2 + (int) (dist / 0.15f));
        for (int i = 1; i < n; i++) {
            float f = (float) i / n;
            if (enRoca(ax + (bx - ax) * f, ay + (by - ay) * f, az + (bz - az) * f)) {
                return false;
            }
        }
        return true;
    }'''

if nuevo in texto:
    print("[OK] lineaLibre ya esta parcheado")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque original en Mundo.java")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    print("[PARCHE] lineaLibre: muestreo cada 0.15 m (detecta paredes finas)")
