"""Raya chispa, estilo bloque: rombo oscuro con vetas azules luminosas y cola de látigo. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("raya_chispa", ppm=30)
p = m.pieza("rayachispa", 1)
OSC = {'todo': E("puntos", 0x1a2440, 0x2a3a60), '-y': E("liso", 0x3a4a70)}
p.caja((0, 0, 0), (0.7, 0.14, 0.9), OSC)
for s in (1, -1):
    p.caja((s * 0.48, 0, -0.05), (0.36, 0.08, 0.6), OSC)
    p.caja((s * 0.48, 0.05, -0.05), (0.3, 0.02, 0.04), {'todo': E("brillo", 0x6ad8ff, 0x2a80ff)})
p.caja((0, 0.08, 0.1), (0.04, 0.02, 0.7), {'todo': E("brillo", 0x6ad8ff, 0x2a80ff)})
p.caja((0, 0, -0.85), (0.04, 0.04, 0.6), {'todo': E("liso", 0x1a2440)})
for s in (1, -1):
    p.caja((s * 0.12, 0.08, 0.36), (0.05, 0.05, 0.05), {'todo': E("ojo", 0x9ff8ff, 0x101010)})
m.crear()
