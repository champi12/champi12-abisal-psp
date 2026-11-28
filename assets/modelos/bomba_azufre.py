"""Bomba de azufre (meseta roja), estilo bloque: saco vivo de gas sulfuroso con púas, vetas
que brillan y una cara pequeña. Una pieza centrada en el origen (el juego la hincha)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("bomba_azufre", ppm=22)
p = m.pieza("bomba", 1)
PIEL = {'todo': E("puntos", 0x8a5a2a, 0x5a3a1a), '+y': E("puntos", 0xa06a30, 0x6a3e1e)}
p.caja((0, 0, 0), (0.8, 0.7, 0.8), PIEL)
p.caja((0, 0, 0), (0.6, 0.9, 0.6), PIEL)
p.caja((0, 0, 0), (0.95, 0.5, 0.6), PIEL)
p.caja((0, 0, 0), (0.6, 0.5, 0.95), PIEL)
for (x, y, z) in ((0.41, 0.1, 0.1), (-0.41, -0.05, -0.1), (0.1, 0.36, 0.2), (-0.15, 0.36, -0.2), (0.2, -0.1, 0.41),
                  (-0.25, 0.1, -0.41)):
    p.caja((x, y, z), (0.14, 0.14, 0.14), {'todo': E("brillo", 0xfff060, 0xffb020)})
for (x, y, z, rx, rz) in ((0.5, 0.2, 0, 0, -50), (-0.5, 0.1, 0.1, 0, 50), (0, 0.55, 0, 0, 0), (0.1, 0.2, 0.5, 50, 0),
                          (-0.1, 0.25, -0.5, -50, 0), (0.35, -0.35, 0.3, 30, -40), (-0.3, -0.35, -0.3, -30, 40)):
    p.caja((x, y, z), (0.06, 0.28, 0.06), {'todo': E("hueso", 0xd8c890, 0xa89860)}, rot=(rx, 0, rz))
p.caja((0, -0.05, 0.48), (0.24, 0.1, 0.04), {'todo': E("liso", 0x2a1410)})
for s in (1, -1):
    p.caja((s * 0.15, 0.12, 0.48), (0.08, 0.08, 0.03), {'todo': E("ojo", 0xffe040, 0x101010)})
for k in range(4):
    p.caja((0.2 * (k - 1.5), -0.55, 0.1 * (k % 2)), (0.04, 0.3, 0.04), {'todo': E("liso", 0x6a3e1e)})
m.crear()
