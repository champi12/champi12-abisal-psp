"""Camarones ígneos (grietas termales): una docena de camarones rojos de cola luminosa."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
ROJO = material("rojo", 0xc83e24)
ROJO2 = material("rojo2", 0xe8603a)
LUZ = material("cola", 0xffa040, emision=5)
p = PiezaJ("camarones#1", [ROJO, ROJO2, LUZ])
rnd = random.Random(71)
for i in range(10):
    ox, oy, oz = (rnd.random() - 0.5) * 1.6, (rnd.random() - 0.5) * 0.8, (rnd.random() - 0.5) * 1.6
    p.torno([(0, 0.1), (0.035, 0.06), (0.04, -0.02), (0.025, -0.09), (0, -0.12)], 5, [0, 0, 1, 1], centro=(ox, oy, oz))
    p.tri((ox, oy + 0.02, oz - 0.1), (ox + 0.05, oy, oz - 0.18), (ox - 0.05, oy, oz - 0.18), 2)
    p.cuad((ox + 0.01, oy + 0.01, oz + 0.09), (ox + 0.05, oy + 0.05, oz + 0.22), (ox + 0.045, oy + 0.05, oz + 0.22), (ox + 0.005, oy + 0.01, oz + 0.09), 1)
    p.cuad((ox - 0.01, oy + 0.01, oz + 0.09), (ox - 0.005, oy + 0.01, oz + 0.09), (ox - 0.045, oy + 0.05, oz + 0.22), (ox - 0.05, oy + 0.05, oz + 0.22), 1)
p.crear()
print("TRIS", triangulos())
