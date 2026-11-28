"""Acorazado (llanura), estilo bloque: cangrejo gigante de caparazón óxido con franjas turquesa.
Piezas: cuerpo#1 (caparazón, vientre, ojos), pata#2 (cadera en el origen, hacia +z y abajo; el
juego dibuja seis) y pinza#3 (hombro en el origen, hacia +z; el juego dibuja dos)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("acorazado", ppm=26)
OX = {'todo': E("placas", 0xa85a2a, 0x7a3a1a), '-y': E("liso", 0xd8a878)}
b = m.pieza("cuerpo", 1)
b.caja((0, 0.55, 0), (1.9, 0.45, 1.6), OX)
b.caja((0, 0.86, 0), (1.5, 0.25, 1.3), OX)
b.caja((0, 1.03, 0), (1.0, 0.12, 0.9), OX)
for z in (-0.35, 0.05, 0.45):
    b.caja((0, 0.995, z), (1.52, 0.02, 0.08), {'todo': E("brillo", 0x5ae8d8, 0x2a9a90)})
for s in (1, -1):
    b.caja((s * 0.25, 0.9, 0.78), (0.06, 0.2, 0.06), {'todo': E("liso", 0x7a3a1a)})
    b.caja((s * 0.25, 1.02, 0.78), (0.1, 0.1, 0.1), {'todo': E("ojo", 0x1a1a1a, 0x5ae8d8)})
p = m.pieza("pata", 2)
p.caja((0, -0.05, 0.25), (0.12, 0.12, 0.5), OX)
p.caja((0, -0.4, 0.68), (0.1, 0.7, 0.1), OX, rot=(25, 0, 0), pivote=(0, -0.05, 0.5))
q = m.pieza("pinza", 3)
q.caja((0, 0, 0.3), (0.16, 0.16, 0.6), OX)
q.caja((0, 0.02, 0.85), (0.26, 0.22, 0.5), OX)
q.caja((0.05, 0.1, 1.08), (0.06, 0.06, 0.08), {'todo': E("liso", 0x3a1a0a)})
m.crear()
