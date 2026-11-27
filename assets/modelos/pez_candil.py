"""Pez candil (bajíos de noche): pez redondo con vientre cálido luminoso, ~0,4 m.
Piezas: cola#1 (z < -0,18) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
AZUL = material("lomo", 0x3e4e70)
VIENTRE = material("vientre", 0xffd080, emision=4)
OJO = material("ojos", 0xfff0c0, emision=4)
ALETA = material("aleta", 0x5a6a8e)
p = PiezaJ("candil", [AZUL, VIENTRE, OJO, ALETA])
p.torno([(0, -0.2), (0.08, -0.15), (0.13, -0.03), (0.13, 0.05), (0.09, 0.13), (0, 0.18)], 9, 0, aplastar=(0.9, 1.05))
p.elip((0, -0.065, 0.02), (0.095, 0.05, 0.12), 7, 3, 1)
for s in (1, -1):
    p.elip((s * 0.075, 0.05, 0.11), (0.03, 0.032, 0.025), 5, 3, 2)
    p.tri((s * 0.11, -0.02, 0.05), (s * 0.19, -0.05, -0.03), (s * 0.1, -0.04, -0.04), 3)
p.cuad((0, 0, -0.18), (0, 0.13, -0.33), (0, 0, -0.27), (0, -0.13, -0.33), 3)
p.tri((0, 0.12, 0.04), (0, 0.19, -0.06), (0, 0.1, -0.1), 3)
partir(p, [[-B, B, -B, B, -B, -0.18]], "candil")
print("TRIS", triangulos())
