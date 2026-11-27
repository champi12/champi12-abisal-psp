"""Medusa luna: campana violeta luminosa con brazos y tentáculos, ~0,8 m. Translúcida.
Piezas: tentáculos#1 (y < -0,05) y campana#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CAMPANA = material("campana", 0xc060e0, emision=1.5)
BORDE = material("borde", 0xf0aaff, emision=2.5)
NUCLEO = material("nucleo", 0xffe0ff, emision=5)
TENT = material("tentaculos", 0xe090f0, emision=2)
p = PiezaJ("medusa", [CAMPANA, BORDE, NUCLEO, TENT])
p.torno([(0.43, 0.0), (0.41, 0.1), (0.35, 0.2), (0.25, 0.3), (0.12, 0.36), (0, 0.38)], 14, [1, 0, 0, 0, 0], eje='y')
p.torno([(0.42, 0.0), (0.28, -0.04), (0, -0.06)], 14, 0, eje='y')
p.elip((0, 0.14, 0), (0.13, 0.11, 0.13), 8, 4, 2)
for i in range(10):
    a = i * math.tau / 10
    p.elip((math.cos(a) * 0.38, 0.03, math.sin(a) * 0.38), (0.035, 0.02, 0.035), 4, 2, 1)
for i in range(10):                    # tentáculos finos en 3 tramos ondulados
    a = i * math.tau / 10
    ca, sa = math.cos(a), math.sin(a)
    l = 0.6 + (i % 3) * 0.22
    prev = (ca * 0.4, -0.06, sa * 0.4)
    for k in range(1, 4):
        t = k / 3
        r = 0.4 - 0.18 * t
        cur = (ca * r + math.sin(k * 2 + i) * 0.03, -0.06 - l * t, sa * r)
        w = 0.025 * (1 - t * 0.6)
        p.cuad((prev[0] - sa * w, prev[1], prev[2] + ca * w), (prev[0] + sa * w, prev[1], prev[2] - ca * w),
               (cur[0] + sa * w * 0.6, cur[1], cur[2] - ca * w * 0.6), (cur[0] - sa * w * 0.6, cur[1], cur[2] + ca * w * 0.6), 3)
        prev = cur
for i in range(4):                     # brazos orales ondulados
    a = i * 1.5708 + 0.39
    x, z = math.cos(a) * 0.12, math.sin(a) * 0.12
    p.cuad((x - 0.04, -0.06, z), (x + 0.04, -0.06, z), (x * 0.5 + 0.02, -0.32, z * 0.5), (x * 0.5 - 0.02, -0.32, z * 0.5), 1)
    p.cuad((x * 0.5 - 0.02, -0.32, z * 0.5), (x * 0.5 + 0.02, -0.32, z * 0.5), (x * 0.2 + 0.01, -0.58, z * 0.2), (x * 0.2 - 0.01, -0.58, z * 0.2), 1)
partir(p, [[-B, B, -B, -0.05, -B, B]], "medusa")
print("TRIS", triangulos())
