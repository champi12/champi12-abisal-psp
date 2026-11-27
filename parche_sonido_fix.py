#!/usr/bin/env python3
"""Quita el salto temporal de comer.wav, veneno.wav y escupir.wav."""
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Sonido.java')
if not os.path.exists(ruta):
    print("No existe", ruta); sys.exit(1)
texto = open(ruta, 'r', encoding='utf-8').read()

# Quitar el bloque de salto (con o sin comentario)
viejo = '''            // PARCHE: saltar los 3 sonidos que cuelgan el prefetch (bug conocido)
            if (r.endsWith("comer.wav") || r.endsWith("veneno.wav") || r.endsWith("escupir.wav")) {
                Log.d("Sonido.cargar: SALTANDO " + r + " (parche)");
                continue;
            }
'''

# También intentar sin comentario (por si el parche anterior no puso el comentario)
viejo2 = '''            if (r.endsWith("comer.wav") || r.endsWith("veneno.wav") || r.endsWith("escupir.wav")) {
                Log.d("Sonido.cargar: SALTANDO " + r + " (parche)");
                continue;
            }
'''

if viejo in texto:
    texto = texto.replace(viejo, '', 1)
    print("[PARCHE] Sonido.java: quitado el salto de los 3 sonidos")
elif viejo2 in texto:
    texto = texto.replace(viejo2, '', 1)
    print("[PARCHE] Sonido.java: quitado el salto de los 3 sonidos")
else:
    print("[OK] Sonido.java: no tiene el salto (ya esta limpio)")
    sys.exit(0)

open(ruta, 'w', encoding='utf-8').write(texto)
print("Ahora compila con: ./build.sh psp")
