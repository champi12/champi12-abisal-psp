#!/usr/bin/env python3
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Partida.java')
texto = open(ruta, 'r', encoding='utf-8').read()
viejo = '''            if (o2 < 10 && !enRemora) {
                alarma();
            }'''
nuevo = '''            if (o2 < 10 && !enRemora) {
                // PARCHE TEMPORAL: comentar alarma() para diagnosticar el crash
                // alarma();
            }'''
if nuevo in texto:
    print("[OK] alarma() ya esta comentada")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque en supervivencia()")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    print("[PARCHE] alarma() comentada (temporal, para diagnosticar)")
