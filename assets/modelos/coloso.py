"""Coloso de Ceniza, estilo bloque: manta abisal colosal (~60 m) de alas de ceniza y tres
señuelos luminosos bajo la boca. Piezas: ala derecha#1 (x > 8), ala izquierda#2 (x < -8),
cola#3 (|x| < 3, z < -15), señuelos#4 (|x| < 3, y < -3, z > 13), cuerpo#5."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("coloso", ppm=2.2)
CEN = {'todo': E("placas", 0x3a3634, 0x2a2624), '+y': E("hueso", 0x5a5450, 0x3a3634)}
for s, n in ((1, 1), (-1, 2)):
    w = m.pieza("ala", n)
    w.caja((s * 14, 3, 3), (12, 1.6, 14), CEN)
    w.caja((s * 24, 3.6, 1), (10, 1.0, 9), CEN)
    w.caja((s * 18, 4.4, 3), (8, 0.3, 0.8), {'todo': E("brillo", 0xff8a3a, 0xc04010)})
c = m.pieza("cola", 3)
c.caja((0, 0.5, -28), (1.6, 1.0, 24), {'todo': E("placas", 0x3a3634, 0x2a2624)})
c.caja((0, 0.6, -39), (0.4, 2.0, 2.0), {'todo': E("brillo", 0xff8a3a, 0xc04010)})
l = m.pieza("senuelos", 4)
for x in (-1.8, 0, 1.8):
    l.caja((x, -5.5, 16), (0.3, 7, 0.3), {'todo': E("liso", 0x2a2624)})
    l.caja((x, -9.3, 16), (1.2, 1.2, 1.2), {'todo': E("brillo", 0xffe080, 0xff9030)})
b = m.pieza("cuerpo", 5)
b.caja((0, 1, 1), (16, 5, 30), CEN)
b.caja((0, 2.5, 2), (12, 3, 24), CEN)
b.caja((0, -0.6, 18), (14, 2.2, 3), {'todo': E("liso", 0x1a0a08)})                     # boca
for s in (1, -1):
    b.caja((s * 5.5, 2.8, 16.5), (1.4, 1.2, 1.0), {'todo': E("brillo", 0xff6a2a, 0xa02008)})
    b.caja((s * 7.6, 0.5, 13), (0.6, 1.4, 6), {'todo': E("rejilla", 0x2a2624, 0x5a3020)})   # branquias
b.caja((0, 4.1, 0), (0.8, 0.4, 22), {'todo': E("brillo", 0xff8a3a, 0xc04010)})
m.crear()
