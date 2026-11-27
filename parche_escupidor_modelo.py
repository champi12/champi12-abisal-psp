#!/usr/bin/env python3
"""Reescribe el modelo del escupidor para quitar el aro oscuro de la boca."""
import os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
ruta = os.path.join(ROOT, 'game/src/abisal/Modelos.java')
texto = open(ruta, 'r', encoding='utf-8').read()

viejo = '''    static Mesh[] escupidor(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.rot(0, 90);
        b.lathe(new float[] { 0f, -0.4f, 0.22f, -0.32f, 0.36f, -0.1f, 0.38f, 0.1f, 0.28f, 0.3f, 0.12f, 0.38f },
                8, new int[] { 0x4a3a5a, 0x5a4a6e, 0x6a5a7e, 0x5a4a6e, 0x4a3a5a });
        b.identity();
        // mouth tube
        b.move(0, 0, 0.38f);
        b.rot(0, 90);
        b.lathe(new float[] { 0.11f, 0f, 0.13f, 0.18f, 0.1f, 0.26f, 0.06f, 0.24f }, 7, new int[] { 0x7a5a8a,
                0x9a6aaa, 0x2a1a2a });
        b.identity();
        // armour plates and tail fan
        b.color = 0x3a2e48;
        b.box(0, 0.33f, 0, 0.3f, 0.08f, 0.4f);
        b.color = 0x8a6aa0;
        b.tri(0, 0, -0.36f, 0, 0.26f, -0.62f, 0, -0.26f, -0.62f);
        b.tri(0, 0, -0.36f, 0, -0.26f, -0.62f, 0, 0.26f, -0.62f);
        // glowing acid sacs and eyes
        b.brillo = true;
        b.color = 0x9aff40;
        b.box(0.34f, -0.05f, 0.05f, 0.08f, 0.12f, 0.12f);
        b.box(-0.34f, -0.05f, 0.05f, 0.08f, 0.12f, 0.12f);
        b.color = 0xffe060;
        b.box(0.2f, 0.15f, 0.3f, 0.06f, 0.06f, 0.05f);
        b.box(-0.2f, 0.15f, 0.3f, 0.06f, 0.06f, 0.05f);
        return b.buildPartes(fog, 255, true, 0,
                new float[][] { { -99, 99, -99, 99, 0.38f, 99 }, { -99, 99, -99, 99, -99, -0.34f } });
    }'''

nuevo = '''    static Mesh[] escupidor(Fog fog) {
        MB b = new MB();
        b.jitter = 8;
        b.rot(0, 90);
        // cuerpo: lathe cerrado (sin interior visible)
        b.lathe(new float[] { 0f, -0.4f, 0.22f, -0.32f, 0.36f, -0.1f, 0.38f, 0.1f, 0.28f, 0.3f, 0.12f, 0.38f },
                8, new int[] { 0x4a3a5a, 0x5a4a6e, 0x6a5a7e, 0x5a4a6e, 0x4a3a5a });
        b.identity();
        // boca: cono cerrado que apunta hacia +Z, SIN interior visible
        // (antes era un lathe hueco que se veia como un aro oscuro)
        b.move(0, 0, 0.38f);
        b.rot(0, 90);
        b.lathe(new float[] { 0.0f, 0.0f, 0.14f, 0.02f, 0.12f, 0.14f, 0.08f, 0.22f, 0.0f, 0.24f },
                7, new int[] { 0x9a6aaa, 0x7a5a8a, 0x6a5a7e, 0x9a6aaa });
        b.identity();
        // armor plates and tail fan
        b.color = 0x3a2e48;
        b.box(0, 0.33f, 0, 0.3f, 0.08f, 0.4f);
        b.color = 0x8a6aa0;
        b.tri(0, 0, -0.36f, 0, 0.26f, -0.62f, 0, -0.26f, -0.62f);
        b.tri(0, 0, -0.36f, 0, -0.26f, -0.62f, 0, 0.26f, -0.62f);
        // glowing acid sacs and eyes
        b.brillo = true;
        b.color = 0x9aff40;
        b.box(0.34f, -0.05f, 0.05f, 0.08f, 0.12f, 0.12f);
        b.box(-0.34f, -0.05f, 0.05f, 0.08f, 0.12f, 0.12f);
        b.color = 0xffe060;
        b.box(0.2f, 0.15f, 0.3f, 0.06f, 0.06f, 0.05f);
        b.box(-0.2f, 0.15f, 0.3f, 0.06f, 0.06f, 0.05f);
        return b.buildPartes(fog, 255, true, 0,
                new float[][] { { -99, 99, -99, 99, 0.38f, 99 }, { -99, 99, -99, 99, -99, -0.34f } });
    }'''

if nuevo in texto:
    print("[OK] escupidor ya esta parcheado")
elif viejo not in texto:
    print("[FAIL] no se encontro el bloque original del escupidor")
    sys.exit(1)
else:
    texto = texto.replace(viejo, nuevo, 1)
    open(ruta, 'w', encoding='utf-8').write(texto)
    print("[PARCHE] escupidor: boca rediseñada como cono cerrado (sin aro oscuro)")
