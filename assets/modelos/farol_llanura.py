"""Farol de la llanura: pez pálido y ciego que lleva un señuelo de luz fría."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
PIEL = material("piel", 0xc4c9c0)
GRIS = material("gris", 0x93a096)
ALETA = material("aleta", 0xb7c0b6)
LUZ = material("senuelo", 0xeaffd2, emision=6)
p = PiezaJ("farolllanura#1", [PIEL, GRIS, ALETA, LUZ])
p.torno([(0, -0.34), (0.12, -0.25), (0.19, -0.05), (0.2, 0.12), (0.15, 0.27), (0, 0.36)], 9, 0, aplastar=(1, 0.85))
p.cuad((-0.13, 0.03, 0.28), (0.13, 0.03, 0.28), (0.1, -0.06, 0.42), (-0.1, -0.06, 0.42), 1)     # mandíbula
for i in range(5):                                                                           # dientes
    x = -0.08 + i * 0.04
    p.tri((x - 0.012, -0.05, 0.4), (x + 0.012, -0.05, 0.4), (x, 0.0, 0.39), 2)
p.cuad((0, 0.13, -0.28), (0, -0.1, -0.28), (0, -0.17, -0.52), (0, 0.17, -0.52), 2)
for s in (1, -1):
    p.cuad((s * 0.16, 0.02, 0.06), (s * 0.34, -0.04, -0.02), (s * 0.3, -0.1, -0.14), (s * 0.14, -0.02, -0.06), 2)
# caña del señuelo en tres tramos curvos
pts = [(0, 0.15, 0.18), (0.02, 0.33, 0.28), (0.04, 0.43, 0.4), (0.05, 0.43, 0.48)]
for a, b in zip(pts, pts[1:]):
    p.cuad((a[0] - 0.012, a[1], a[2]), (a[0] + 0.012, a[1], a[2]), (b[0] + 0.01, b[1], b[2]), (b[0] - 0.01, b[1], b[2]), 1)
p.elip((0.05, 0.42, 0.5), (0.07, 0.07, 0.07), 7, 4, 3)
p.crear()
print("TRIS", triangulos())
