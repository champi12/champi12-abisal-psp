"""Lazofín (bosque), estilo bloque: disco dorado con tres ojos y dos cintas largas.
Piezas: cinta lejana#1 (z < -1,1), cinta cercana#2 (z < -0,3) y cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("lazofin", ppm=40)
ORO = {'todo': E("escamas", 0xd8a840, 0xa87828), '-y': E("liso", 0xf0d890)}
c1 = m.pieza("cinta_lejos", 1)
c1.caja((0, 0, -1.55), (0.02, 0.22, 0.84), {'todo': E("aleta", 0xf0c060, 0xff8a3a)})
c2 = m.pieza("cinta_cerca", 2)
c2.caja((0, 0, -0.72), (0.02, 0.36, 0.84), {'todo': E("aleta", 0xf0c060, 0xffb060)})
b = m.pieza("cuerpo", 3)
b.caja((0, 0, 0), (0.22, 0.62, 0.62), ORO)
b.caja((0, 0, 0), (0.18, 0.78, 0.42), ORO)
b.caja((0, 0, 0), (0.18, 0.42, 0.78), ORO)
for k, y in enumerate((0.16, 0.0, -0.16)):
    b.caja((0.12, y, 0.28), (0.02, 0.07, 0.07), {'todo': E("ojo", 0xf8f0d0, 0x101010)})
    b.caja((-0.12, y, 0.28), (0.02, 0.07, 0.07), {'todo': E("ojo", 0xf8f0d0, 0x101010)})
b.caja((0, 0.4, 0), (0.02, 0.12, 0.3), {'todo': E("aleta", 0xffb060, 0xd88a30)})
m.crear()
