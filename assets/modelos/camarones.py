"""Camarones ígneos, estilo bloque: nueve camarones rojos de cola luminosa. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("camarones", ppm=60)
p = m.pieza("camarones", 1)
import random
r = random.Random(5)
for k in range(9):
    x, y, z = r.uniform(-0.75, 0.75), r.uniform(-0.38, 0.38), r.uniform(-0.9, 0.9)
    p.caja((x, y, z), (0.05, 0.05, 0.14), {'todo': E("placas", 0xd8401a, 0xa82a10)})
    p.caja((x, y, z - 0.09), (0.04, 0.03, 0.05), {'todo': E("brillo", 0xffa040, 0xff5010)})
    p.caja((x, y + 0.01, z + 0.1), (0.06, 0.004, 0.06), {'todo': E("liso", 0xff8a5a)})
m.crear()
