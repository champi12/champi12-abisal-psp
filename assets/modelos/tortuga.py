"""Tortuga de coral (bajíos y bosque), estilo bloque: caparazón con escudos y un arrecife que
brilla encima, ~1,4 m. Piezas: aletas #1 der-delante, #2 izq-delante, #3 der-detrás,
#4 izq-detrás; cuerpo#5."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("tortuga", ppm=22)
PIEL = {'todo': E("puntos", 0x7a8a5a, 0x5a6a3a)}
for (num, s, delante) in ((1, 1, True), (2, -1, True), (3, 1, False), (4, -1, False)):
    a = m.pieza("aleta", num)
    if delante:
        a.caja((s * 0.66, -0.02, 0.2), (0.5, 0.05, 0.22), PIEL, rot=(0, s * 20, 0))
    else:
        a.caja((s * 0.44, -0.02, -0.6), (0.3, 0.05, 0.2), PIEL, rot=(0, s * -30, 0))
c = m.pieza("cuerpo", 5)
c.caja((0, 0.04, 0), (0.78, 0.22, 1.0), {'todo': E("liso", 0x9aa070), '+y': E("placas", 0x6a7a4a, 0x8a6a3a)})
c.caja((0, 0.2, 0), (0.62, 0.14, 0.82), {'todo': E("placas", 0x6a7a4a, 0x8a6a3a)})
c.caja((0, 0.3, 0), (0.4, 0.08, 0.5), {'todo': E("placas", 0x8a6a3a, 0x6a7a4a)})
for (x, z, h, col) in ((0.15, 0.1, 0.16, 0xff8a6a), (-0.18, -0.15, 0.13, 0xf0d060), (0.05, -0.28, 0.1, 0xff8a6a),
                       (-0.08, 0.22, 0.09, 0xf0d060)):
    c.caja((x, 0.34 + h / 2, z), (0.06, h, 0.06), {'todo': E("brillo", col, 0xffffff)})
c.caja((0, 0.06, 0.62), (0.22, 0.18, 0.28), PIEL)                                       # cabeza
for s in (1, -1):
    c.caja((s * 0.11, 0.1, 0.7), (0.02, 0.05, 0.05), {'todo': E("ojo", 0xb0b890, 0x101010)})
m.crear()
