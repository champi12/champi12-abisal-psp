"""Chillón de cañón, estilo bloque: raya-murciélago correosa de boca enorme y alas con garfios.
Piezas: ala derecha#1 (x > 0,32), ala izquierda#2 (x < -0,32), cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("chillon", ppm=32)
PIEL = {'todo': E("puntos", 0x5a4a5a, 0x3a2a3a)}
for s, n in ((1, 1), (-1, 2)):
    w = m.pieza("ala", n)
    w.caja((s * 0.75, 0.08, -0.05), (0.8, 0.04, 0.6), {'todo': E("aleta", 0x5a4a5a, 0x7a5a6a)})
    w.caja((s * 1.3, 0.08, 0.15), (0.3, 0.04, 0.3), {'todo': E("aleta", 0x5a4a5a, 0x7a5a6a)})
    w.caja((s * 1.42, 0.08, 0.3), (0.04, 0.04, 0.12), {'todo': E("liso", 0xe0d8c8)})
b = m.pieza("cuerpo", 3)
b.caja((0, 0.04, 0), (0.6, 0.36, 0.9), PIEL)
b.caja((0, -0.05, 0.45), (0.56, 0.1, 0.3), {'todo': E("liso", 0x8a1a2a)})                # bocaza
for s in (1, -1):
    b.caja((s * 0.18, 0.22, 0.38), (0.08, 0.06, 0.06), {'todo': E("ojo", 0xff5a3a, 0x101010)})
    for k in range(3):
        b.caja((s * (0.08 + k * 0.08), 0.02, 0.6), (0.02, 0.06, 0.02), {'todo': E("liso", 0xf0e8d0)})
b.caja((0, 0, -0.85), (0.06, 0.06, 0.8), {'todo': E("liso", 0x3a2a3a)})
m.crear()
