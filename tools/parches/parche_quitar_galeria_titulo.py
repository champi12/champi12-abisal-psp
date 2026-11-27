#!/usr/bin/env python3
"""Quita la galeria del titulo para ahorrar memoria."""
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Juego.java')
texto = open(ruta, 'r', encoding='utf-8').read()

# Quitar la creacion de fondoTitulo en Juego.run
viejo = '''            Log.d("Juego.run: creando Galeria('capsula', false)...");
                fondoTitulo = new Galeria("cápsula", false);
                Log.d("Juego.run: Galeria creada");'''

nuevo = '''            Log.d("Juego.run: galeria del titulo desactivada (ahorro de memoria)");'''

if nuevo in texto:
    print("[OK] Juego.run ya tiene la galeria desactivada")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque en Juego.run")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    print("[PARCHE] Juego.run: galeria del titulo desactivada")

# Quitar el uso de fondoTitulo en el bucle del titulo (linea de fondoTitulo.actualizar/dibujar)
viejo2 = '''            fondoTitulo.actualizar(in, dt);
            fondoTitulo.dibujar(g);
            dibujarTitulo(g);'''

nuevo2 = '''            dibujarTitulo(g);'''

if nuevo2 in texto:
    print("[OK] bucle del titulo ya no usa fondoTitulo")
elif viejo2 not in texto:
    print("[FAIL] no se encontro el bucle del titulo")
else:
    texto = texto.replace(viejo2, nuevo2, 1)
    print("[PARCHE] bucle del titulo: ya no dibuja fondoTitulo")

# Quitar el uso de fondoTitulo al volver al titulo (volverAlTitulo)
viejo3 = '''            fondoTitulo = new Galeria("cápsula", false);
            prepararTitulo();'''

nuevo3 = '''            prepararTitulo();'''

if nuevo3 in texto:
    print("[OK] volverAlTitulo ya no crea fondoTitulo")
elif viejo3 in texto:
    texto = texto.replace(viejo3, nuevo3, 1)
    print("[PARCHE] volverAlTitulo: ya no crea fondoTitulo")

open(ruta, 'w', encoding='utf-8').write(texto)
print("Ahora compila con: ./build.sh psp")
