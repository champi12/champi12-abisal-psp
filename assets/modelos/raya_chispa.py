"""Raya chispa (~1,2 m): rombo oscuro con vetas azules luminosas y cola de látigo."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
LOMO = material("lomo", 0x24324a)
VIENTRE = material("vientre", 0x141c26)
LUZ = material("vetas", 0x4ab8ff, emision=5)
OJO = material("ojos", 0xd0f0ff, emision=4)
p = PiezaJ("rayachispa#1", [LOMO, VIENTRE, LUZ, OJO])
p.torno([(0, -0.4), (0.25, -0.25), (0.65, 0.0), (0.3, 0.3), (0, 0.5)], 4, 0, aplastar=(1, 0.12))
p.torno([(0, -0.38), (0.2, -0.2), (0.6, 0.0), (0.26, 0.28), (0, 0.47)], 4, 1, aplastar=(1, 0.06), centro=(0, -0.02, 0))
for s in (1, -1):
    for (x, z, l) in ((0.25, 0.05, 0.36), (0.18, -0.12, 0.24), (0.38, -0.03, 0.18)):
        p.cajaJ((s * x, 0.06, z), (l, 0.01, 0.025), 2, rot=(0, 0, s * 12))
    p.elip((s * 0.06, 0.07, 0.36), (0.022, 0.022, 0.022), 4, 2, 3)
p.cuad((0.015, 0.02, -0.38), (0.004, 0.02, -1.15), (-0.004, 0.02, -1.15), (-0.015, 0.02, -0.38), 1)
p.cajaJ((0, 0.03, -1.05), (0.03, 0.03, 0.06), 2)
p.crear()
print("TRIS", triangulos())
