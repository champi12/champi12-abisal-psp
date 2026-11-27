"""Flotador de cañón: bolsa de gas a la deriva con aletas cortas y palpos colgantes, ~1,4 m."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
BOLSA = material("bolsa", 0xd0b8e0)
BOLSA2 = material("bolsa2", 0xb8a0c8)
PALPO = material("palpos", 0x8a7090)
OJO = material("ojos", 0xffffff, emision=4)
VENA = material("venas", 0xe8a0ff, emision=2)
p = PiezaJ("flotador#1", [BOLSA, BOLSA2, PALPO, OJO, VENA])
p.torno([(0, -0.4), (0.45, -0.25), (0.65, 0.1), (0.55, 0.42), (0.3, 0.6), (0, 0.66)], 12, [1, 0, 0, 0, 1], eje='y')
for i in range(6):                     # venas luminosas sobre la bolsa
    a = i * math.tau / 6
    p.cajaJ((math.cos(a) * 0.62, 0.15, math.sin(a) * 0.62), (0.04, 0.5, 0.04), 4, rot=(0, 0, 0))
for i in range(6):                     # palpos colgantes en dos tramos
    a = i * math.tau / 6 + 0.3
    x, z = math.cos(a) * 0.3, math.sin(a) * 0.3
    p.cuad((x - 0.025, -0.35, z), (x + 0.025, -0.35, z), (x * 1.15 + 0.018, -0.65, z * 1.15), (x * 1.15 - 0.018, -0.65, z * 1.15), 2)
    p.cuad((x * 1.15 - 0.018, -0.65, z * 1.15), (x * 1.15 + 0.018, -0.65, z * 1.15), (x * 1.05 + 0.008, -0.95, z * 1.05), (x * 1.05 - 0.008, -0.95, z * 1.05), 2)
for s in (1, -1):
    p.tri((s * 0.6, 0, 0.12), (s * 0.88, 0.06, -0.1), (s * 0.6, 0, -0.22), 2)
    p.elip((s * 0.2, 0.15, 0.56), (0.05, 0.05, 0.04), 5, 3, 3)
p.crear()
print("TRIS", triangulos())
