#!/usr/bin/env python3
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Partida.java')
texto = open(ruta, 'r', encoding='utf-8').read()
viejo = '''    private void transicion(String que) {
        // log first: if anything after this hangs, the devlog says how far it got
        Log.d("superficie: " + que + " (y " + py + ")");
        registrarFrame = true;
        Sonido.tocar(Sonido.BURBUJAS);
    }'''
nuevo = '''    private void transicion(String que) {
        // log first: if anything after this hangs, the devlog says how far it got
        Log.d("superficie: " + que + " (y " + py + ")");
        registrarFrame = true;
        // PARCHE TEMPORAL: comentar Sonido.tocar para diagnosticar el crash
        // Sonido.tocar(Sonido.BURBUJAS);
    }'''
if nuevo in texto:
    print("[OK] Sonido.tocar ya esta comentado en transicion()")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque en transicion()")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    print("[PARCHE] Sonido.tocar(BURBUJAS) comentado en transicion() (temporal)")
