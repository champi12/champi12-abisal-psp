"""Cardumen de platillos: ocho pececillos plateados en formación (una sola malla)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
PLATA = material("plata", 0xc8d6e0, metal=0.3)
LOMO = material("lomo", 0x7a98b0)
LUZ = material("franja", 0x9fe8ff, emision=3)
p = PiezaJ("cardumen", [PLATA, LOMO, LUZ])
rnd = random.Random(61)
for i in range(8):
    ox, oy, oz = (rnd.random() - 0.5) * 1.6, (rnd.random() - 0.5) * 0.9, (rnd.random() - 0.5) * 1.8
    p.torno([(0, -0.14), (0.045, -0.07), (0.055, 0.03), (0.035, 0.1), (0, 0.13)], 5, [1, 0, 0, 0], centro=(ox, oy, oz), aplastar=(0.6, 1))
    p.cuad((ox, oy, oz - 0.12), (ox, oy + 0.06, oz - 0.22), (ox, oy, oz - 0.18), (ox, oy - 0.06, oz - 0.22), 1)
    p.cajaJ((ox, oy + 0.02, oz + 0.01), (0.07, 0.012, 0.16), 2)
p.crear().name = "cardumen#1"
print("TRIS", triangulos())
