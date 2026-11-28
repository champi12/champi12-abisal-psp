"""Medusa rayo, estilo bloque: campana cuadrada azul con cuatro tentáculos en zigzag
crepitantes. Piezas: tentáculos#1 (y < -0,02), campana#2. Translúcida."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("medusa_rayo", ppm=34)
t = m.pieza("tentaculos", 1)
for k in range(4):
    a = math.radians(45 + k * 90)
    x, z = math.sin(a) * 0.35, math.cos(a) * 0.35
    for j in range(4):
        t.caja((x + (0.06 if j % 2 else -0.06), -0.18 - j * 0.33, z), (0.04, 0.36, 0.04),
               {'todo': E("brillo", 0x9fe0ff, 0x3a80ff)}, rot=(0, 0, 18 if j % 2 else -18))
c = m.pieza("campana", 2)
CAMP = {'todo': E("brillo", 0x7ab8ff, 0x2a58c8)}
c.caja((0, 0.15, 0), (1.2, 0.3, 1.2), CAMP)
c.caja((0, 0.4, 0), (0.9, 0.2, 0.9), CAMP)
c.caja((0, 0.55, 0), (0.5, 0.1, 0.5), {'todo': E("brillo", 0xe0f8ff, 0x7ab8ff)})
m.crear()
