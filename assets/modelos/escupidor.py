"""Escupidor de cueva, estilo bloque: pez blindado y redondo con boca de tubo y sacos de ácido
luminosos. Piezas: tubo de la boca#1 (z > 0,38), cola#2 (z < -0,34), cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("escupidor", ppm=44)
ARM = {'todo': E("placas", 0x5a6a5a, 0x3a4a3a)}
t = m.pieza("boca", 1)
t.caja((0, 0, 0.5), (0.22, 0.22, 0.22), ARM)
t.caja((0, 0, 0.6), (0.16, 0.16, 0.04), {'todo': E("liso", 0x101810)})
c = m.pieza("cola", 2)
c.caja((0, 0, -0.42), (0.18, 0.24, 0.12), ARM)
c.caja((0, 0, -0.55), (0.02, 0.5, 0.14), {'todo': E("aleta", 0x5a6a5a, 0x8a9a7a)})
b = m.pieza("cuerpo", 3)
b.caja((0, 0, 0.03), (0.74, 0.66, 0.66), ARM)
b.caja((0, 0, 0.03), (0.6, 0.72, 0.5), ARM)
for s in (1, -1):
    b.caja((s * 0.38, -0.1, 0.0), (0.04, 0.2, 0.3), {'todo': E("brillo", 0x9fff40, 0x50c010)})
    b.caja((s * 0.2, 0.18, 0.36), (0.1, 0.1, 0.04), {'todo': E("ojo", 0xffe060, 0x101010)})
m.crear()
