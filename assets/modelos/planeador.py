"""Planeador de arena (bajíos): raya pequeña arena con manchas azules, ~1,2 m.
Piezas: ala derecha#1 (x > 0,2), ala izquierda#2 (x < -0,2) y cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
ARENA = material("arena", 0xd8b86a)
ALA = material("ala", 0xc9a65a)
COLA = material("cola", 0x8a6e3a)
OJO = material("ojos", 0x101418)
LUZ = material("manchas", 0x5ac8ff, emision=4)
p = PiezaJ("planeador", [ARENA, ALA, COLA, OJO, LUZ])
p.torno([(0, -0.33), (0.13, -0.24), (0.21, -0.05), (0.2, 0.12), (0.13, 0.26), (0, 0.33)], 10, 0, aplastar=(1, 0.32))
for s in (1, -1):                     # ala: tres tramos con borde ondulado
    pts = [(s * 0.17, 0, 0.22), (s * 0.42, 0.03, 0.08), (s * 0.64, 0.03, -0.06), (s * 0.48, 0.01, -0.16), (s * 0.17, 0, -0.27)]
    for k in range(1, 4):
        p.tri(pts[0], pts[k], pts[k + 1], 1)
    for (x, z, rr) in ((0.38, -0.02, 0.06), (0.27, -0.13, 0.045), (0.5, -0.07, 0.04)):
        p.elip((s * x, 0.035, z), (rr, 0.008, rr), 6, 2, 4)
    p.elip((s * 0.08, 0.06, 0.24), (0.03, 0.03, 0.03), 5, 3, 3)
p.cuad((0.035, 0, -0.29), (0.008, 0.015, -0.9), (-0.008, 0.015, -0.9), (-0.035, 0, -0.29), 2)
p.tri((0, 0.02, -0.55), (0, 0.08, -0.62), (0, 0.02, -0.68), 2)
partir(p, [[0.2, B, -B, B, -B, B], [-B, -0.2, -B, B, -B, B]], "planeador")
print("TRIS", triangulos())
