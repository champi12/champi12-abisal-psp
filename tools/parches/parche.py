#!/usr/bin/env python3
"""Aplica los parches a ABISAL para arreglar el cuelgue de sonidos y los bugs del guardado."""
import os
import re
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
CAMBIOS = 0

def leer(ruta):
    with open(ruta, 'r', encoding='utf-8') as f:
        return f.read()

def escribir(ruta, texto):
    with open(ruta, 'w', encoding='utf-8') as f:
        f.write(texto)

def parchear(ruta, viejo, nuevo, descripcion):
    global CAMBIOS
    ruta_completa = os.path.join(ROOT, ruta)
    if not os.path.exists(ruta_completa):
        print(f"[SKIP] {ruta} no existe")
        return
    texto = leer(ruta_completa)
    if nuevo in texto:
        print(f"[OK]   {descripcion} (ya aplicado)")
        return
    if viejo not in texto:
        print(f"[FAIL] {descripcion}: no se encontro el texto original en {ruta}")
        return
    texto = texto.replace(viejo, nuevo, 1)
    escribir(ruta_completa, texto)
    CAMBIOS += 1
    print(f"[PARCHE] {descripcion}")

# ---------------------------------------------------------------------------
# 1. Sonido.java: saltar los 3 sonidos sospechosos + logs
# ---------------------------------------------------------------------------
viejo = '''    static void cargar() {
        for (int i = 0; i < RUTA.length; i++) {
            try {
                p[i] = Manager.createPlayer(RUTA[i]);
                p[i].prefetch();
                Log.d("sonido cargado: " + RUTA[i]);
            } catch (Throwable e) {
                Log.d("sonido FALLO: " + RUTA[i] + " (" + e + ")");
            }
        }
    }'''

nuevo = '''    static void cargar() {
        for (int i = 0; i < RUTA.length; i++) {
            String r = RUTA[i];
            // PARCHE: saltar los 3 sonidos que cuelgan el prefetch (bug conocido)
            if (r.endsWith("comer.wav") || r.endsWith("veneno.wav") || r.endsWith("escupir.wav")) {
                Log.d("Sonido.cargar: SALTANDO " + r + " (parche)");
                continue;
            }
            Log.d("Sonido.cargar: " + (i + 1) + "/" + RUTA.length + " " + r);
            try {
                p[i] = Manager.createPlayer(r);
                Log.d("Sonido.cargar: createPlayer OK " + r);
                p[i].prefetch();
                Log.d("sonido cargado: " + r);
            } catch (Throwable e) {
                Log.d("sonido FALLO: " + r + " (" + e + ")");
                e.printStackTrace();
            }
        }
        Log.d("Sonido.cargar: fin");
    }'''

parchear('game/src/abisal/Sonido.java', viejo, nuevo,
         'Sonido.cargar: saltar 3 sonidos + logs')

# ---------------------------------------------------------------------------
# 2. Juego.java: logs alrededor de Sonido.cargar
# ---------------------------------------------------------------------------
viejo = '''        try {
            Sonido.cargar();
            Opciones.cargar();
            String gal = midlet.getAppProperty("Abisal-Galeria");
            if (gal != null) {
                galeria = new Galeria(gal.trim(), true);
                estado = GALERIA;
            } else {
                fondoTitulo = new Galeria("cápsula", false);
            }
            prepararTitulo();
            Log.d("pantalla de titulo lista; " + Log.memoria());
        } catch (Throwable e) {
            Log.d("ERROR al iniciar: " + e);
            e.printStackTrace();
        }'''

nuevo = '''        try {
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
        }
        Log.d("Juego.run: entrando en el bucle principal");'''

parchear('game/src/abisal/Juego.java', viejo, nuevo,
         'Juego.run: logs alrededor de Sonido.cargar')

# ---------------------------------------------------------------------------
# 3. Juego.java: volverAlTitulo reconstruye fondoTitulo
# ---------------------------------------------------------------------------
viejo = '''        partida = null;
        fallosMemoria = 0;
        javax.microedition.m3g.VertexBuffer.liberarTodo();
        System.gc();
        try {
            if (fondoTitulo == null) {
                fondoTitulo = new Galeria("cápsula", false);
            }
            prepararTitulo();
        } catch (Throwable e3) {
            Log.d("ERROR al volver al titulo: " + e3);
        }
        estado = TITULO;'''

nuevo = '''        partida = null;
        fallosMemoria = 0;
        fondoTitulo = null;  // PARCHE: forzar reconstruccion
        javax.microedition.m3g.VertexBuffer.liberarTodo();
        System.gc();
        try {
            fondoTitulo = new Galeria("cápsula", false);
            prepararTitulo();
        } catch (Throwable e3) {
            Log.d("ERROR al volver al titulo: " + e3);
            try { prepararTitulo(); } catch (Throwable e4) { }
        }
        estado = TITULO;'''

parchear('game/src/abisal/Juego.java', viejo, nuevo,
         'Juego.volverAlTitulo: reconstruir fondoTitulo')

# ---------------------------------------------------------------------------
# 4. Juego.java: CARGANDO con try/catch alrededor de new Partida
# ---------------------------------------------------------------------------
viejo = '''            partida = new Partida(continuar);
            estado = JUGANDO;
            return;'''

nuevo = '''            try {
                partida = new Partida(continuar);
                estado = JUGANDO;
            } catch (Throwable e) {
                Log.d("ERROR al cargar la partida: " + e);
                e.printStackTrace();
                partida = null;
                continuar = false;
                try {
                    partida = new Partida(false);
                    estado = JUGANDO;
                } catch (Throwable e2) {
                    Log.d("ERROR tambien en partida nueva: " + e2);
                    volverAlTitulo();
                }
            }
            return;'''

parchear('game/src/abisal/Juego.java', viejo, nuevo,
         'Juego.CARGANDO: try/catch en new Partida')

# ---------------------------------------------------------------------------
# 5. Graphics3D.java: releaseTarget con try/finally
# ---------------------------------------------------------------------------
viejo = '''    public void releaseTarget() {
        if (target != null) {
            if (lote && queue.count > 0) {
                flush();
            }
            G3D.end();
            target = null;
        }
    }'''

nuevo = '''    private boolean flushFallo = false;

    public void releaseTarget() {
        if (target == null) return;
        try {
            if (lote && queue.count > 0 && !flushFallo) {
                flush();
            }
        } catch (Throwable e) {
            flushFallo = true;
        } finally {
            try { G3D.end(); } catch (Throwable ignore) {}
            target = null;
            queue.clear();
        }
        flushFallo = false;
    }'''

parchear('lib/java/javax/microedition/m3g/Graphics3D.java', viejo, nuevo,
         'Graphics3D.releaseTarget: try/finally')

# ---------------------------------------------------------------------------
# 6. RecordStore.java: validar load
# ---------------------------------------------------------------------------
viejo = '''    private void load(byte[] raw) throws RecordStoreException {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
            nextId = in.readInt();
            version = in.readInt();
            modified = in.readLong();
            int n = in.readInt();
            ids = new int[n];
            data = new byte[n][];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                int len = in.readInt();
                if (len >= 0) {
                    data[i] = new byte[len];
                    in.readFully(data[i]);
                }
            }
        } catch (java.io.IOException e) {
            throw new RecordStoreException("corrupt record store " + name);
        }
    }'''

nuevo = '''    private void load(byte[] raw) throws RecordStoreException {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
            nextId = in.readInt();
            version = in.readInt();
            modified = in.readLong();
            int n = in.readInt();
            if (n < 0 || n > 100000) {
                throw new RecordStoreException("corrupt record store " + name + " (n=" + n + ")");
            }
            ids = new int[n];
            data = new byte[n][];
            for (int i = 0; i < n; i++) {
                ids[i] = in.readInt();
                int len = in.readInt();
                if (len < -1 || len > 10000000) {
                    throw new RecordStoreException("corrupt record store " + name + " (len=" + len + ")");
                }
                if (len >= 0) {
                    data[i] = new byte[len];
                    in.readFully(data[i]);
                }
            }
        } catch (java.io.IOException e) {
            throw new RecordStoreException("corrupt record store " + name);
        } catch (NegativeArraySizeException e) {
            throw new RecordStoreException("corrupt record store " + name);
        } catch (OutOfMemoryError e) {
            throw new RecordStoreException("record store " + name + " too large");
        }
    }'''

parchear('lib/java/javax/microedition/rms/RecordStore.java', viejo, nuevo,
         'RecordStore.load: validar limites')

# ---------------------------------------------------------------------------
# 7. AbisalMIDlet.java: reiniciar hilo si murio
# ---------------------------------------------------------------------------
viejo = '''    protected void startApp() {
        if (juego == null) {
            juego = new Juego(this);
            Display.getDisplay(this).setCurrent(juego);
            new Thread(juego).start();
        }
        juego.pausado = false;
    }'''

nuevo = '''    private Thread hilo;

    protected void startApp() {
        if (juego == null) {
            juego = new Juego(this);
            Display.getDisplay(this).setCurrent(juego);
        }
        if (hilo == null || !hilo.isAlive()) {
            juego.corriendo = true;
            hilo = new Thread(juego);
            hilo.start();
        }
        juego.pausado = false;
    }'''

parchear('game/src/abisal/AbisalMIDlet.java', viejo, nuevo,
         'AbisalMIDlet.startApp: reiniciar hilo si murio')

# ---------------------------------------------------------------------------
# 8. plat_psp.c: plat_file_pread con validacion
# ---------------------------------------------------------------------------
viejo = '''int plat_file_pread(void *f, void *buf, int len, long offset) {
    SceUID fd = (SceUID)(intptr_t)f - 1;
    if (sceIoLseek(fd, offset, PSP_SEEK_SET) < 0) return -1;
    return sceIoRead(fd, buf, (SceSize)len);
}'''

nuevo = '''int plat_file_pread(void *f, void *buf, int len, long offset) {
    SceUID fd = (SceUID)(intptr_t)f - 1;
    if (offset < 0 || len < 0) return -1;
    SceOff sz = sceIoLseek(fd, 0, PSP_SEEK_END);
    if (sz < 0) {
        devlog("plat_file_pread: sceIoLseek(END) fallo (fd=%d)", (int)fd);
        return -1;
    }
    if (offset >= (long)sz) {
        devlog("plat_file_pread: offset %ld >= tamano %ld", offset, (long)sz);
        return 0;
    }
    if (offset + len > (long)sz) {
        devlog("plat_file_pread: recortando %d a %ld (offset %ld, tamano %ld)",
               len, (long)sz - offset, offset, (long)sz);
        len = (int)((long)sz - offset);
    }
    if (sceIoLseek(fd, offset, PSP_SEEK_SET) < 0) {
        devlog("plat_file_pread: sceIoLseek(%ld) fallo", offset);
        return -1;
    }
    int r = sceIoRead(fd, buf, (SceSize)len);
    if (r != len) {
        devlog("plat_file_pread: sceIoRead devolvio %d (pedido %d, offset %ld)", r, len, offset);
    }
    return r;
}'''

parchear('runtime/platform/psp/plat_psp.c', viejo, nuevo,
         'plat_file_pread: validar tamano')

# ---------------------------------------------------------------------------
# 9. plat_psp.c: plat_write_file con .bak
# ---------------------------------------------------------------------------
viejo = '''int plat_write_file(const char *path, const uint8_t *data, size_t len) {
    char tmp[340];
    snprintf(tmp, sizeof tmp, "%s.tmp", path);
    scePowerLock(0);
    int ok = 0;
    SceUID fd = sceIoOpen(tmp, PSP_O_WRONLY | PSP_O_CREAT | PSP_O_TRUNC, 0777);
    if (fd >= 0) {
        ok = sceIoWrite(fd, data, (SceSize)len) == (int)len;
        sceIoClose(fd);
        if (ok) {
            sceIoRemove(path);
            ok = sceIoRename(tmp, path) >= 0;
        } else {
            sceIoRemove(tmp);
        }
    }
    scePowerUnlock(0);
    return ok;
}'''

nuevo = '''int plat_write_file(const char *path, const uint8_t *data, size_t len) {
    char tmp[340], bak[340];
    snprintf(tmp, sizeof tmp, "%s.tmp", path);
    snprintf(bak, sizeof bak, "%s.bak", path);
    scePowerLock(0);
    int ok = 0;
    SceUID fd = sceIoOpen(tmp, PSP_O_WRONLY | PSP_O_CREAT | PSP_O_TRUNC, 0777);
    if (fd >= 0) {
        int escrito = sceIoWrite(fd, data, (SceSize)len);
        sceIoClose(fd);
        if (escrito == (int)len) {
            sceIoRemove(bak);
            sceIoRename(path, bak);
            ok = sceIoRename(tmp, path) >= 0;
            if (!ok) sceIoRename(bak, path);
        }
        if (!ok) sceIoRemove(tmp);
    }
    scePowerUnlock(0);
    return ok;
}'''

parchear('runtime/platform/psp/plat_psp.c', viejo, nuevo,
         'plat_write_file: .bak atomico')

# ---------------------------------------------------------------------------
# 10. plat_psp.c: plat_read_file con fallback .tmp y .bak
# ---------------------------------------------------------------------------
viejo = '''uint8_t *plat_read_file(const char *path, size_t *len) {
    SceUID fd = sceIoOpen(path, PSP_O_RDONLY, 0);
    if (fd < 0) {
        /* interrupted save (between removing the old file and renaming the new one) */
        char tmp[340];
        snprintf(tmp, sizeof tmp, "%s.tmp", path);
        fd = sceIoOpen(tmp, PSP_O_RDONLY, 0);
    }
    if (fd < 0) return NULL;'''

nuevo = '''uint8_t *plat_read_file(const char *path, size_t *len) {
    SceUID fd = sceIoOpen(path, PSP_O_RDONLY, 0);
    if (fd < 0) {
        char alt[340];
        snprintf(alt, sizeof alt, "%s.tmp", path);
        fd = sceIoOpen(alt, PSP_O_RDONLY, 0);
        if (fd < 0) {
            snprintf(alt, sizeof alt, "%s.bak", path);
            fd = sceIoOpen(alt, PSP_O_RDONLY, 0);
        }
    }
    if (fd < 0) return NULL;'''

parchear('runtime/platform/psp/plat_psp.c', viejo, nuevo,
         'plat_read_file: fallback .tmp y .bak')

# ---------------------------------------------------------------------------
print()
if CAMBIOS == 0:
    print("Nada que cambiar (ya estaba todo parcheado).")
else:
    print(f"{CAMBIOS} parche(s) aplicados correctamente.")
    print("Ahora compila con: cd ~/abisal && ./build.sh psp")
