"""Gusano de arena (praderas), estilo bloque: gusano segmentado con boca de anillos de colmillos.
Piezas: cabeza#1, colmillo#2 (el juego dibuja varios), segmento#3, cola#4."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("gusano", ppm=40)
PIEL = {'todo': E("placas", 0xc8a070, 0x987048), '-y': E("liso", 0xe0c8a0)}
c = m.pieza("cabeza", 1)
c.caja((0, 0, 0), (0.62, 0.62, 0.5), PIEL)
c.caja((0, 0, 0.27), (0.5, 0.5, 0.1), {'todo': E("liso", 0x5a1a10)})
for s in (1, -1):
    c.caja((s * 0.2, 0.2, 0.2), (0.08, 0.08, 0.08), {'todo': E("ojo", 0xff6030, 0x101010)})
k = m.pieza("colmillo", 2)
k.caja((0, 0, 0.17), (0.08, 0.06, 0.34), {'todo': E("hueso", 0xf0e8d0, 0xc0b090)})
g = m.pieza("segmento", 3)
g.caja((0, 0, 0), (0.7, 0.72, 0.5), PIEL)
g.caja((0.36, 0.1, 0), (0.1, 0.1, 0.1), {'todo': E("hueso", 0xf0e8d0, 0xc0b090)})
g.caja((0, 0.38, 0), (0.5, 0.06, 0.4), {'todo': E("puntos", 0xd8b080, 0x8a6040)})
t = m.pieza("cola", 4)
t.caja((0, 0, -0.1), (0.44, 0.44, 0.6), PIEL)
t.caja((0, 0, -0.42), (0.2, 0.2, 0.16), PIEL)
m.crear()
