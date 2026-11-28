"""Caracol de duna, estilo bloque: concha en espiral escalonada con crestas verdes que brillan. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("caracol", ppm=50)
p = m.pieza("caracol", 1)
CON = {'todo': E("franjas", 0xd8b070, 0xa88050)}
p.caja((0, 0.05, 0.1), (0.2, 0.1, 0.5), {'todo': E("liso", 0xc0a088)})              # pie
p.caja((0, 0.27, -0.02), (0.32, 0.36, 0.38), CON)                                   # espiras
p.caja((0, 0.4, 0.02), (0.26, 0.26, 0.28), CON)
p.caja((0, 0.49, 0.05), (0.16, 0.14, 0.16), CON)
p.caja((0, 0.47, -0.02), (0.34, 0.03, 0.03), {'todo': E("brillo", 0x9fff9a, 0x40c050)})
p.caja((0, 0.3, 0.19), (0.34, 0.03, 0.03), {'todo': E("brillo", 0x9fff9a, 0x40c050)})
for s in (1, -1):
    p.caja((s * 0.05, 0.16, 0.36), (0.02, 0.12, 0.02), {'todo': E("liso", 0xc0a088)})
    p.caja((s * 0.05, 0.23, 0.36), (0.035, 0.035, 0.035), {'todo': E("ojo", 0x101010, 0x101010)})
m.crear()
