"""Cardumen de platillos, estilo bloque: ocho pececillos plateados en formación (una malla)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("cardumen", ppm=60)
p = m.pieza("cardumen", 1)
import random
r = random.Random(3)
for k in range(8):
    x, y, z = r.uniform(-0.3, 0.7), r.uniform(-0.3, 0.2), r.uniform(-0.7, 0.85)
    p.caja((x, y, z), (0.04, 0.12, 0.18), {'todo': E("escamas", 0xd8e0e8, 0x8a98a8)})
    p.caja((x, y, z - 0.12), (0.01, 0.08, 0.06), {'todo': E("aleta", 0xb8c8d8, 0x6a7888)})
    p.caja((x, y + 0.02, z + 0.06), (0.042, 0.02, 0.02), {'todo': E("brillo", 0x9ff8ff, 0x6ac8e0)})
m.crear()
