"""Mordedor de pasto (praderas), estilo bloque: pez sapo achaparrado de piel rugosa óxido y gran
mandíbula. Piezas: mandíbula#1 (y < -0,06, z > 0,1), aleta derecha#2 (x > 0,25), aleta
izquierda#3 (x < -0,25), cuerpo#4."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("sapo", ppm=50)
PIEL = {'todo': E("puntos", 0x9a5a30, 0x6a3a20), '-y': E("liso", 0xd8a878)}
j = m.pieza("mandibula", 1)
j.caja((0, -0.12, 0.3), (0.46, 0.1, 0.38), {'todo': E("puntos", 0x8a4a28, 0x5a2a18), '+y': E("liso", 0x5a1a10)})
for s in (1, -1):
    for k in range(3):
        j.caja((s * (0.08 + k * 0.06), -0.05, 0.45), (0.02, 0.05, 0.02), {'todo': E("liso", 0xf0e8d0)})
for s, n in ((1, 2), (-1, 3)):
    a = m.pieza("aleta", n)
    a.caja((s * 0.38, -0.05, -0.02), (0.26, 0.03, 0.28), {'todo': E("aleta", 0x9a5a30, 0xc88050)})
b = m.pieza("cuerpo", 4)
b.caja((0, 0.04, -0.02), (0.5, 0.4, 0.8), PIEL)
b.caja((0, 0.0, -0.4), (0.3, 0.26, 0.2), PIEL)
b.caja((0, 0.08, 0.3), (0.48, 0.22, 0.3), PIEL)
for s in (1, -1):
    b.caja((s * 0.16, 0.24, 0.3), (0.1, 0.08, 0.1), {'todo': E("ojo", 0xffd040, 0x101010)})
b.caja((0, 0.28, -0.05), (0.02, 0.1, 0.3), {'todo': E("aleta", 0x6a3a20, 0x9a5a30)})
m.crear()
