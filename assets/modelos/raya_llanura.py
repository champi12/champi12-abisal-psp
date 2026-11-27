"""Raya pálida de la llanura: rombo gris plano con cola de látigo."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
LOMO = material("lomo", 0xbcc4c6)
VIENTRE = material("vientre", 0x98a2a4)
COLA = material("cola", 0xa8b2b4)
OJO = material("ojos", 0xfff0c0, emision=4)
p = PiezaJ("rayallanura#1", [LOMO, VIENTRE, COLA, OJO])
p.torno([(0, -0.32), (0.3, -0.18), (0.62, -0.05), (0.32, 0.22), (0, 0.42)], 4, 0, aplastar=(1, 0.12))
p.torno([(0, -0.3), (0.28, -0.17), (0.58, -0.05), (0.3, 0.2), (0, 0.4)], 4, 1, aplastar=(1, 0.06), centro=(0, -0.02, 0))
p.cuad((0.03, 0, -0.3), (0.01, 0, -1.12), (-0.01, 0, -1.12), (-0.03, 0, -0.3), 2)
for s in (1, -1):
    p.elip((s * 0.13, 0.07, 0.26), (0.035, 0.035, 0.035), 4, 2, 3)
p.crear()
print("TRIS", triangulos())
