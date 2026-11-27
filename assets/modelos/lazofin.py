"""Lazofín (bosque de algas): disco dorado con tres ojos y dos cintas largas, ~0,7 m.
Piezas: cinta lejana#1 (z < -1,1), cinta cercana#2 (z < -0,3) y cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
ORO = material("disco", 0xffd248)
ORO2 = material("disco2", 0xf0b83a)
CINTA = material("cinta", 0xd8407a)
OJO = material("ojos", 0x101418)
LUZ = material("manchas", 0xfff2a0, emision=4)
p = PiezaJ("lazofin", [ORO, ORO2, CINTA, OJO, LUZ])
seg, r, g = 16, 0.4, 0.17
for i in range(seg):
    a0, a1 = i * math.tau / seg, (i + 1) * math.tau / seg
    y0, z0, y1, z1 = math.sin(a0) * r, math.cos(a0) * r, math.sin(a1) * r, math.cos(a1) * r
    p.tri((g, 0, 0), (g * 0.3, y0, z0), (g * 0.3, y1, z1), 0, False)
    p.tri((-g, 0, 0), (-g * 0.3, y1, z1), (-g * 0.3, y0, z0), 1, False)
    p.cuad((g * 0.3, y0, z0), (-g * 0.3, y0, z0), (-g * 0.3, y1, z1), (g * 0.3, y1, z1), 2, False)
for s in (-1, 1):                     # dos cintas onduladas, en 6 tramos
    y = s * 0.12
    for k in range(6):
        z0, z1 = -0.3 - k * 0.28, -0.3 - (k + 1) * 0.28
        w0, w1 = 0.055 * (1 - k * 0.13), 0.055 * (1 - (k + 1) * 0.13)
        o0, o1 = math.sin(k * 1.1) * 0.07 * s, math.sin((k + 1) * 1.1) * 0.07 * s
        p.cuad((0, y + o0 - w0, z0), (0, y + o0 + w0, z0), (0, y + o1 + w1, z1), (0, y + o1 - w1, z1), 2)
for i in (-1, 0, 1):
    p.elip((0, i * 0.09 + 0.04, 0.35 - abs(i) * 0.025), (0.055, 0.03, 0.03), 5, 3, 3)
for i in range(4):
    a = i * 1.57 + 0.5
    for sx in (1, -1):
        p.elip((sx * 0.13, math.sin(a) * 0.2, math.cos(a) * 0.2), (0.012, 0.035, 0.035), 5, 2, 4)
partir(p, [[-B, B, -B, B, -B, -1.1], [-B, B, -B, B, -B, -0.3]], "lazofin")
print("TRIS", triangulos())
