"""Mordedor de pasto (praderas): pez sapo achaparrado de piel rugosa óxido y gran mandíbula,
~0,8 m. Piezas: mandíbula#1 (y < -0,06, z > 0,1), aleta derecha#2, aleta izquierda#3, cuerpo#4."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
PIEL = material("piel", 0x8a4a2a)
VERRUGA = material("verrugas", 0x6a3a1e)
LABIO = material("labio", 0x9a5a32)
GARGANTA = material("garganta", 0xd0a070)
DIENTE = material("dientes", 0xf0e8d0)
OJO = material("ojos", 0xff3a2a, emision=5)
p = PiezaJ("sapo", [PIEL, VERRUGA, LABIO, GARGANTA, DIENTE, OJO])
p.elip((0, 0.02, 0), (0.3, 0.2, 0.34), 9, 5, 0)
rnd = random.Random(811)
for i in range(9):
    a = rnd.uniform(0, math.tau)
    p.elip((math.cos(a) * 0.18, 0.17, math.sin(a) * 0.22 - 0.03), (0.04, 0.03, 0.04), 4, 2, 1)
p.cuad((-0.26, 0.0, 0.3), (0.26, 0.0, 0.3), (0.2, -0.02, 0.5), (-0.2, -0.02, 0.5), 2)          # labio superior
p.cuad((-0.24, -0.12, 0.2), (-0.2, -0.1, 0.5), (0.2, -0.1, 0.5), (0.24, -0.12, 0.2), 3)        # mandíbula
p.cuad((-0.24, -0.16, 0.2), (0.24, -0.16, 0.2), (0.2, -0.14, 0.5), (-0.2, -0.14, 0.5), 3)
for i in range(5):
    x = -0.16 + i * 0.08
    p.tri((x - 0.025, -0.09, 0.46), (x + 0.025, -0.09, 0.46), (x, -0.01, 0.47), 4)
    p.tri((x - 0.025, -0.08, 0.47), (x + 0.025, -0.08, 0.47), (x, -0.0, 0.48), 4)
for s in (1, -1):
    p.cuad((s * 0.27, -0.1, 0.05), (s * 0.52, -0.21, -0.05), (s * 0.5, -0.21, -0.22), (s * 0.26, -0.1, -0.15), 1)
    p.elip((s * 0.12, 0.2, 0.2), (0.06, 0.06, 0.06), 5, 3, 5)
p.cuad((0, 0.02, -0.32), (0, 0.18, -0.52), (0, 0.02, -0.46), (0, -0.14, -0.52), 1)
partir(p, [[-99, 99, -99, -0.06, 0.1, 99], [0.25, 99, -99, 99, -99, 99], [-99, -0.25, -99, 99, -99, 99]], "sapo")
print("TRIS", triangulos())
