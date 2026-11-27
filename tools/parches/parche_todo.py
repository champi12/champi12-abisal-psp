#!/usr/bin/env python3
"""Aplica todos los parches pendientes a ABISAL de una vez."""
import os, sys, glob

ROOT = os.path.dirname(os.path.abspath(__file__))
CAMBIOS = 0
FALLOS = 0

def encontrar(patron):
    """Busca un archivo por patron glob. Devuelve la primera coincidencia o None."""
    for p in glob.glob(os.path.join(ROOT, patron), recursive=True):
        return p
    return None

def parchear(ruta, viejo, nuevo, descripcion):
    global CAMBIOS, FALLOS
    if not ruta:
        print(f"[FAIL] {descripcion}: archivo no encontrado")
        FALLOS += 1
        return
    if not os.path.exists(ruta):
        print(f"[FAIL] {descripcion}: {ruta} no existe")
        FALLOS += 1
        return
    texto = open(ruta, 'r', encoding='utf-8').read()
    if nuevo in texto:
        print(f"[OK]   {descripcion} (ya aplicado)")
        return
    if viejo not in texto:
        print(f"[FAIL] {descripcion}: no se encontro el texto original en {os.path.basename(ruta)}")
        FALLOS += 1
        return
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    CAMBIOS += 1
    print(f"[PARCHE] {descripcion}")

# ---------------------------------------------------------------------------
# 1. PlayerImpl.prefetch: lanzar MediaException si Media.load falla
# ---------------------------------------------------------------------------
ruta = encontrar('lib/java/**/PlayerImpl.java')
if not ruta:
    ruta = encontrar('**/PlayerImpl.java')

viejo = '''        if (state == REALIZED) {
            if (sound == 0) {
                sound = path != null ? Media.load(path) : 0;
                if (sound == 0 && data != null) {
                    sound = Media.loadData(data);
                }
            }
            state = PREFETCHED;
        }'''

nuevo = '''        if (state == REALIZED) {
            if (sound == 0) {
                sound = path != null ? Media.load(path) : 0;
                if (sound == 0 && data != null) {
                    sound = Media.loadData(data);
                }
            }
            if (sound == 0) {
                throw new MediaException("no se pudo cargar " + (path != null ? path : "(datos)"));
            }
            state = PREFETCHED;
        }'''

parchear(ruta, viejo, nuevo, 'PlayerImpl.prefetch: lanzar MediaException si falla')

# ---------------------------------------------------------------------------
# 2. Juego.run: mover Sonido.cargar() al final
# ---------------------------------------------------------------------------
ruta = encontrar('game/src/abisal/Juego.java')
if not ruta:
    ruta = encontrar('**/Juego.java')

viejo = '''        try {
            Log.d("Juego.run: cargando sonidos...");
            Sonido.cargar();
            Log.d("Juego.run: sonidos OK, cargando opciones...");
            Opciones.cargar();
            Log.d("Juego.run: opciones OK, comprobando galeria...");
            String gal = midlet.getAppProperty("Abisal-Galeria");
            if (gal != null) {
                galeria = new Galeria(gal.trim(), true);
                estado = GALERIA;
                Log.d("Juego.run: galeria directa OK");
            } else {
                Log.d("Juego.run: creando Galeria('capsula', false)...");
                fondoTitulo = new Galeria("cápsula", false);
                Log.d("Juego.run: Galeria creada");
            }
            Log.d("Juego.run: preparando titulo...");
            prepararTitulo();
            Log.d("pantalla de titulo lista; " + Log.memoria());
        } catch (Throwable e) {
            Log.d("ERROR al iniciar: " + e);
            e.printStackTrace();
        }'''

nuevo = '''        try {
            Log.d("Juego.run: cargando opciones...");
            Opciones.cargar();
            Log.d("Juego.run: opciones OK, comprobando galeria...");
            String gal = midlet.getAppProperty("Abisal-Galeria");
            if (gal != null) {
                galeria = new Galeria(gal.trim(), true);
                estado = GALERIA;
                Log.d("Juego.run: galeria directa OK");
            } else {
                Log.d("Juego.run: creando Galeria('capsula', false)...");
                fondoTitulo = new Galeria("cápsula", false);
                Log.d("Juego.run: Galeria creada");
            }
            Log.d("Juego.run: preparando titulo...");
            prepararTitulo();
            Log.d("pantalla de titulo lista; " + Log.memoria());
            Log.d("Juego.run: cargando sonidos...");
            Sonido.cargar();
            Log.d("Juego.run: sonidos OK");
        } catch (Throwable e) {
            Log.d("ERROR al iniciar: " + e);
            e.printStackTrace();
        }'''

parchear(ruta, viejo, nuevo, 'Juego.run: Sonido.cargar() movido al final')

# ---------------------------------------------------------------------------
print()
if FALLOS > 0:
    print(f"{CAMBIOS} parche(s) aplicados, {FALLOS} fallo(s).")
    print("Los fallos pueden ser porque el archivo ya esta parcheado o porque la ruta no es la esperada.")
    sys.exit(1)
elif CAMBIOS == 0:
    print("Nada que cambiar (ya estaba todo parcheado).")
else:
    print(f"{CAMBIOS} parche(s) aplicados correctamente.")
    print("Ahora compila con: cd ~/abisal && ./build.sh psp")
