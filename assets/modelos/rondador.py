"""Rondador abisal: globo negro con boca enorme, dientes de aguja y señuelo luminoso en una
caña. Piezas: señuelo#1 (y > 0,5, z > 0,25) y cuerpo#2. ~1,6 m."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
NEGRO = material("negro", 0x16161c)
BOCA = material("boca", 0x2a1a22)
DIENTE = material("dientes", 0xd8d8e0)
CANA = material("cana", 0x2a2a30)
LUZ = material("senuelo", 0x9affe0, emision=6)
OJO = material("ojos", 0xd0fff0, emision=4)
p = PiezaJ("rondador", [NEGRO, BOCA, DIENTE, CANA, LUZ, OJO])
p.elip((0, 0, 0), (0.6, 0.5, 0.65), 10, 6, 0)
p.cuad((-0.45, -0.1, 0.5), (0.45, -0.1, 0.5), (0.35, -0.45, 0.62), (-0.35, -0.45, 0.62), 1)
for i in range(8):
    x = -0.38 + i * 0.108
    l = 0.24 if i % 2 == 0 else 0.15
    p.tri((x - 0.03, -0.1, 0.56), (x + 0.03, -0.1, 0.56), (x, -0.1 - l, 0.6), 2)
    p.tri((x - 0.025, -0.45, 0.6), (x + 0.025, -0.45, 0.6), (x, -0.45 + l * 0.7, 0.6), 2)
p.cuad((0, 0, -0.6), (0, 0.42, -1.02), (0, 0, -0.85), (0, -0.37, -0.97), 3)
for s in (1, -1):
    p.tri((s * 0.55, -0.05, 0.05), (s * 0.82, -0.15, -0.15), (s * 0.5, -0.12, -0.2), 3)
    p.elip((s * 0.18, 0.18, 0.5), (0.045, 0.045, 0.035), 4, 2, 5)
pts = [(0, 0.45, 0.2), (0, 0.72, 0.45), (0, 0.88, 0.75), (0, 0.86, 0.95)]
for a, b in zip(pts, pts[1:]):
    p.cuad((a[0] - 0.02, a[1], a[2]), (a[0] + 0.02, a[1], a[2]), (b[0] + 0.015, b[1], b[2]), (b[0] - 0.015, b[1], b[2]), 3)
p.elip((0, 0.85, 1.0), (0.1, 0.1, 0.1), 6, 4, 4)
partir(p, [[-99, 99, 0.5, 99, 0.25, 99]], "rondador")
