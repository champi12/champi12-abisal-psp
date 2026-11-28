"""Erizo globo, estilo bloque: pez erizo verde oliva con púas oscuras y ojos ámbar. Una pieza
(se hincha en el juego)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("erizo", ppm=50)
p = m.pieza("erizo", 1)
PIEL = {'todo': E("puntos", 0x8a9a4a, 0x5a6a2a), '-y': E("liso", 0xd8d8a0)}
p.caja((0, 0, 0), (0.48, 0.44, 0.5), PIEL)
p.caja((0, 0, 0), (0.56, 0.34, 0.4), PIEL)
for (x, y, z) in ((0.3, 0.1, 0.1), (-0.3, 0.1, -0.1), (0, 0.3, 0.1), (0.1, 0.3, -0.15), (-0.12, 0.3, 0.05), (0.2, -0.2, 0.15),
                  (-0.2, -0.2, -0.12), (0.28, 0.0, -0.2), (-0.28, -0.05, 0.2), (0, 0.15, -0.3), (0.12, -0.1, 0.27)):
    p.caja((x, y, z), (0.04, 0.04, 0.04), {'todo': E("liso", 0x2a3018)})
for s in (1, -1):
    p.caja((s * 0.18, 0.08, 0.25), (0.08, 0.08, 0.02), {'todo': E("ojo", 0xffc040, 0x101010)})
p.caja((0, -0.05, 0.27), (0.1, 0.06, 0.04), {'todo': E("liso", 0x3a2010)})
p.caja((0, 0, -0.32), (0.02, 0.16, 0.12), {'todo': E("aleta", 0x8a9a4a, 0xc8c870)})
m.crear()
