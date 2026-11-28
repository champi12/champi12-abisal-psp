"""Pastor de arena (praderas), estilo bloque: gran pastador de caparazón de placas, ~2,5 m.
Piezas: 4 aletas (#1 der-delante, #2 izq-delante, #3 der-detrás, #4 izq-detrás), cabeza#5, cuerpo#6."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("pastor", ppm=26)
CAP = {'todo': E("placas", 0xb89058, 0x8a6838), '-y': E("liso", 0xe0c898)}
for (n, s, z) in ((1, 1, 0.55), (2, -1, 0.55), (3, 1, -0.55), (4, -1, -0.55)):
    a = m.pieza("aleta", n)
    a.caja((s * 1.35, -0.15, z), (0.8, 0.08, 0.34), {'todo': E("aleta", 0xa88050, 0xd8b080)})
h = m.pieza("cabeza", 5)
h.caja((0, -0.05, 1.4), (0.6, 0.5, 0.9), CAP)
for s in (1, -1):
    h.caja((s * 0.24, 0.12, 1.8), (0.08, 0.08, 0.06), {'todo': E("ojo", 0xf0e0b0, 0x101010)})
    h.caja((s * 0.3, 0.3, 1.2), (0.06, 0.3, 0.06), {'todo': E("hueso", 0xf0e8d0, 0xb0a080)}, rot=(-30, 0, s * 20),
           pivote=(s * 0.3, 0.15, 1.2))
b = m.pieza("cuerpo", 6)
b.caja((0, 0.05, 0), (1.8, 0.8, 2.1), CAP)
b.caja((0, 0.4, 0), (1.4, 0.3, 1.7), CAP)
b.caja((0, 0.55, 0), (0.9, 0.08, 1.2), {'todo': E("placas", 0xd8b070, 0xa88050)})
m.crear()
