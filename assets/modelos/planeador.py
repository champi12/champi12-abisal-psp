"""Planeador de arena (bajíos), estilo bloque: raya pequeña color arena con manchas azules
que brillan, ~1,2 m. Piezas: ala derecha#1 (x > 0,2), ala izquierda#2 (x < -0,2) y cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("planeador", ppm=36)
ALA = {'todo': E("puntos", 0xc9a65a, 0x8a6e3a), '+y': E("puntos", 0xd8b86a, 0x5ac8ff)}
for s, num in ((1, 1), (-1, 2)):
    w = m.pieza("ala", num)
    w.caja((s * 0.38, 0.01, 0.0), (0.36, 0.03, 0.4), ALA)
    w.caja((s * 0.58, 0.0, -0.08), (0.14, 0.02, 0.18), ALA)
    w.caja((s * 0.4, 0.03, -0.04), (0.06, 0.01, 0.06), {'todo': E("brillo", 0x7ad8ff, 0x3a98e0)})
b = m.pieza("cuerpo", 3)
b.caja((0, 0, 0), (0.4, 0.12, 0.62), {'todo': E("puntos", 0xd8b86a, 0x8a6e3a), '-y': E("liso", 0xf0e0c0)})
for s in (1, -1):
    b.caja((s * 0.08, 0.07, 0.24), (0.04, 0.04, 0.04), {'todo': E("ojo", 0xd8c890, 0x101418)})
b.caja((0, 0.01, -0.6), (0.03, 0.03, 0.6), {'todo': E("liso", 0x8a6e3a)})
b.caja((0, 0.05, -0.62), (0.02, 0.06, 0.1), {'todo': E("aleta", 0x8a6e3a, 0xc9a65a)})
m.crear()
