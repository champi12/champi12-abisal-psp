"""Raya pálida de la llanura, estilo bloque: rombo gris plano con cola de látigo. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("raya_llanura", ppm=30)
p = m.pieza("rayallanura", 1)
GR = {'todo': E("puntos", 0xa8b0b0, 0x787e80), '-y': E("liso", 0xd8e0e0)}
p.caja((0, 0, 0), (0.7, 0.12, 0.8), GR)
for s in (1, -1):
    p.caja((s * 0.48, 0, -0.05), (0.32, 0.07, 0.5), GR)
    p.caja((s * 0.12, 0.07, 0.3), (0.05, 0.04, 0.05), {'todo': E("ojo", 0xd8e0e0, 0x101010)})
p.caja((0, 0, -0.75), (0.04, 0.04, 0.7), {'todo': E("liso", 0x787e80)})
m.crear()
