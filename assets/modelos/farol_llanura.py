"""Farol de la llanura, estilo bloque: pez pálido y ciego con un señuelo de luz fría. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("farol_llanura", ppm=40)
p = m.pieza("farolllanura", 1)
PAL = {'todo': E("escamas", 0xd0d8d8, 0xa0a8a8)}
p.caja((0, 0.05, 0.05), (0.4, 0.4, 0.6), PAL)
p.caja((0, 0.0, -0.32), (0.2, 0.24, 0.2), PAL)
p.caja((0, 0.0, -0.46), (0.02, 0.3, 0.14), {'todo': E("aleta", 0xc0c8c8, 0x889090)})
p.caja((0, -0.08, 0.36), (0.3, 0.08, 0.1), {'todo': E("liso", 0x3a3a40)})
p.caja((0, 0.28, 0.25), (0.02, 0.2, 0.02), {'todo': E("liso", 0xa0a8a8)})
p.caja((0, 0.38, 0.42), (0.02, 0.02, 0.3), {'todo': E("liso", 0xa0a8a8)})
p.caja((0, 0.36, 0.56), (0.08, 0.08, 0.08), {'todo': E("brillo", 0xe0f8ff, 0x80c8ff)})
m.crear()
