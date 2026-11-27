"""Caracol de duna (praderas): caracol de concha en espiral con crestas verdes luminosas, ~0,5 m."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
PIE = material("pie", 0xb8a080)
CONCHA = material("concha", 0xd8c8a0)
CONCHA2 = material("concha2", 0xc0a878)
LUZ = material("crestas", 0x9cff9a, emision=4)
OJO = material("ojos", 0xffffff, emision=3)
p = PiezaJ("caracol#1", [PIE, CONCHA, CONCHA2, LUZ, OJO])
p.elip((0, 0.07, 0.05), (0.16, 0.07, 0.34), 8, 3, 0)
for i in range(5):                      # espiral de concha decreciente
    k = 1 - i * 0.18
    a = i * 1.2
    p.elip((0.035 * i, 0.19 + i * 0.075, -0.05 - i * 0.025), (0.2 * k, 0.13 * k, 0.21 * k), 7, 3, 1 + i % 2)
    p.elip((0.2 * k * math.cos(a) + 0.035 * i, 0.2 + i * 0.075, -0.05 + 0.2 * k * math.sin(a) * 0.7), (0.03, 0.03, 0.03), 4, 2, 3)
for s in (1, -1):
    p.torno([(0.012, 0), (0.01, 0.08)], 4, 0, eje='y', centro=(s * 0.06, 0.14, 0.34))
    p.elip((s * 0.06, 0.23, 0.35), (0.025, 0.025, 0.025), 4, 2, 4)
p.crear()
print("TRIS", triangulos())
