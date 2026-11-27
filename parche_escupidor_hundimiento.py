#!/usr/bin/env python3
"""Evita que el escupidor se hunda en la arena y se aleje de su cueva."""
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Depredadores.java')
texto = open(ruta, 'r', encoding='utf-8').read()

viejo = '''            tmp[0] = sx[i];
            tmp[1] = sy[i];
            tmp[2] = sz[i];
            if (p.mundo.empujarCerca(tmp, 0.4f)) {
                sx[i] = tmp[0];
                sy[i] = tmp[1];
                sz[i] = tmp[2];
            }'''

nuevo = '''            tmp[0] = sx[i];
            tmp[1] = sy[i];
            tmp[2] = sz[i];
            if (p.mundo.empujarCerca(tmp, 0.4f)) {
                sx[i] = tmp[0];
                sy[i] = tmp[1];
                sz[i] = tmp[2];
            }
            // PARCHE: no dejar que el escupidor baje del suelo
            float suelo = Terreno.altura(sx[i], sz[i]) + 0.5f;
            if (sy[i] < suelo) {
                sy[i] = suelo;
            }
            // PARCHE: no alejarse demasiado de su cueva
            float dxh = sx[i] - shx[i], dzh = sz[i] - shz[i];
            if (dxh * dxh + dzh * dzh > 30 * 30) {
                sx[i] += (shx[i] - sx[i]) * Math.min(1, dt * 2);
                sz[i] += (shz[i] - sz[i]) * Math.min(1, dt * 2);
            }'''

if nuevo in texto:
    print("[OK] Depredadores ya esta parcheado")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque original en Depredadores.java")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    print("[PARCHE] escupidor: no se hunde en la arena y no se aleja de la cueva")
