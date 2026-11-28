"""Velo abisal, estilo bloque: lámina ancha violeta oscura con manchas luminosas y volantes.
Piezas: cabeza#1, cuerpo#2, cola#3 (cada una sigue su +z local)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("velo", ppm=14)
VIO = {'todo': E("puntos", 0x4a2a6a, 0x9a6aff), '-y': E("liso", 0x3a1a5a)}
c = m.pieza("cabeza", 1)
c.caja((0, 0, 0.6), (4.0, 0.9, 3.6), VIO)
c.caja((0, 0.1, 2.6), (2.2, 0.7, 1.0), VIO)
for s in (1, -1):
    c.caja((s * 0.8, 0.4, 2.8), (0.3, 0.2, 0.3), {'todo': E("brillo", 0xe0b0ff, 0x9a6aff)})
b = m.pieza("cuerpo", 2)
b.caja((0, 0, 0), (6.4, 0.6, 2.9), VIO)
for s in (1, -1):
    b.caja((s * 3.3, 0, 0), (0.6, 0.1, 2.8), {'todo': E("aleta", 0x6a3a9a, 0xc090ff)})
    b.caja((s * 1.6, 0.31, 0), (0.4, 0.02, 0.4), {'todo': E("brillo", 0xe0b0ff, 0x9a6aff)})
t = m.pieza("cola", 3)
t.caja((0, 0, -1.6), (3.6, 0.5, 5.6), VIO)
t.caja((0, 0, -4.4), (1.6, 0.2, 0.8), {'todo': E("aleta", 0x6a3a9a, 0xc090ff)})
t.caja((0, 0.26, -1.6), (0.3, 0.02, 0.3), {'todo': E("brillo", 0xe0b0ff, 0x9a6aff)})
m.crear()
