"""Caballito de arena (bajíos), estilo bloque: caballito dorado con la cola enroscada, ~0,5 m.
Una pieza (el juego lo mece de pie)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("caballito", ppm=60)
p = m.pieza("caballito", 1)
ORO = {'todo': E("franjas", 0xd8b060, 0xb08840), '-y': E("liso", 0xf0d890)}
p.caja((0, 0.3, 0.0), (0.08, 0.16, 0.1), ORO)                     # pecho
p.caja((0, 0.16, -0.02), (0.07, 0.14, 0.08), ORO)
p.caja((0, 0.05, -0.05), (0.05, 0.1, 0.06), ORO)
p.caja((0, -0.02, -0.02), (0.04, 0.05, 0.05), ORO)                # cola enroscada
p.caja((0, -0.03, 0.03), (0.035, 0.04, 0.05), ORO)
p.caja((0, 0.42, 0.03), (0.07, 0.08, 0.09), ORO)                  # cabeza
p.caja((0, 0.4, 0.1), (0.03, 0.03, 0.08), ORO)                    # hocico
for s in (1, -1):
    p.caja((s * 0.036, 0.44, 0.05), (0.01, 0.025, 0.025), {'todo': E("ojo", 0xf0e0b0, 0x201008)})
p.caja((0, 0.27, -0.06), (0.012, 0.12, 0.04), {'todo': E("aleta", 0xf0d890, 0xd8b060)})
p.caja((0, 0.5, 0.02), (0.012, 0.05, 0.05), {'todo': E("aleta", 0xf0d890, 0xd8b060)})
m.crear()
