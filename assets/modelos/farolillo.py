"""Farolillo (cañones y fosa): pez oscuro de flancos con órganos luminosos, ~0,4 m.
Piezas: cola#1 (z < -0,14, se agita) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CUERPO = material("cuerpo", 0x1c1d2c)
ALETA = material("aleta", 0x2c2e4c)
LUZ = material("organos", 0x3ff0ff, emision=5)
OJO = material("ojos", 0xe8fff4, emision=4)
p = PiezaJ("farolillo", [CUERPO, ALETA, LUZ, OJO])
p.torno([(0, -0.2), (0.05, -0.17), (0.09, -0.08), (0.11, 0.03), (0.1, 0.1), (0.07, 0.16), (0, 0.2)], 8, 0, aplastar=(0.85, 1.0))
p.cuad((0, 0.02, -0.18), (0, 0.13, -0.33), (0, 0.06, -0.29), (0, 0, -0.22), 1)      # cola en horquilla
p.cuad((0, -0.02, -0.18), (0, 0, -0.22), (0, -0.06, -0.29), (0, -0.13, -0.33), 1)
p.tri((0, 0.1, 0.05), (0, 0.19, -0.07), (0, 0.08, -0.1), 1)                              # aleta dorsal espinosa
p.tri((0, 0.13, -0.0), (0, 0.17, -0.03), (0, 0.11, -0.04), 1)
p.tri((0.08, -0.05, 0.04), (0.14, -0.09, -0.04), (0.07, -0.06, -0.03), 1)                # pectorales
p.tri((-0.08, -0.05, 0.04), (-0.07, -0.06, -0.03), (-0.14, -0.09, -0.04), 1)
for i in range(5):
    z = 0.1 - i * 0.055
    for s in (1, -1):
        p.elip((s * (0.1 - i * 0.008), -0.025, z), (0.018, 0.016, 0.018), 4, 2, 2)
for s in (1, -1):
    p.elip((s * 0.06, 0.04, 0.13), (0.026, 0.03, 0.03), 5, 3, 3)
partir(p, [[-B, B, -B, B, -B, -0.14]], "farolillo")
print("TRIS", triangulos())
