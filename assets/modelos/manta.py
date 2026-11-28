"""Manta de cristal (praderas), estilo bloque: raya azul pálida con crestas luminosas.
Piezas: ala derecha#1 (x > 0,45), ala izquierda#2 (x < -0,45) y cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("manta", ppm=26)
ALA = {'todo': E("puntos", 0x9ac8e8, 0x6a98c0), '+y': E("placas", 0xb0d8f0, 0x88b0d0)}
for s, n in ((1, 1), (-1, 2)):
    w = m.pieza("ala", n)
    w.caja((s * 0.8, 0.04, 0.1), (0.7, 0.07, 0.9), ALA)
    w.caja((s * 1.3, 0.03, -0.05), (0.5, 0.05, 0.5), ALA)
    w.caja((s * 0.8, 0.09, 0.1), (0.5, 0.02, 0.06), {'todo': E("brillo", 0x9ff8ff, 0x40c0e0)})
b = m.pieza("cuerpo", 3)
b.caja((0, 0.01, 0.2), (0.86, 0.24, 1.6), {'todo': E("placas", 0xb0d8f0, 0x88b0d0), '-y': E("liso", 0xd8f0ff)})
b.caja((0, 0.13, 0.2), (0.04, 0.04, 1.4), {'todo': E("brillo", 0x9ff8ff, 0x40c0e0)})
b.caja((0, 0.01, -1.4), (0.06, 0.06, 1.6), {'todo': E("liso", 0x6a98c0)})
for s in (1, -1):
    b.caja((s * 0.22, 0.12, 0.88), (0.06, 0.06, 0.06), {'todo': E("ojo", 0xd8f0ff, 0x101820)})
m.crear()
