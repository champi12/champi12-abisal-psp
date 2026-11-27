"""Caballito de arena (bajíos): caballito dorado con cola enroscada, ~0,5 m."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
ORO = material("oro", 0xd8b060)
ORO2 = material("oro2", 0xb88a50)
CLARO = material("claro", 0xf0d090)
OJO = material("ojo", 0x121a1e)
p = PiezaJ("caballito#1", [ORO, ORO2, CLARO, OJO])
p.torno([(0, -0.24), (0.05, -0.18), (0.1, -0.05), (0.12, 0.08), (0.09, 0.18), (0, 0.22)], 8, [1, 1, 0, 0, 2], eje='y', aplastar=(0.85, 1))
for i in range(5):                      # anillos del tronco
    p.torno([(0.105, 0), (0.115, 0.012)], 8, 1, eje='y', centro=(0, -0.12 + i * 0.06, 0), aplastar=(0.85, 1))
p.elip((0, 0.23, 0.03), (0.075, 0.085, 0.085), 7, 4, 2)
p.torno([(0.035, 0.08), (0.03, 0.18), (0.022, 0.24)], 6, 0, centro=(0, 0.21, 0))   # hocico tubular
for s in (1, -1):
    p.elip((s * 0.05, 0.255, 0.05), (0.024, 0.024, 0.024), 4, 2, 3)
p.tri((0, 0.3, -0.02), (0, 0.36, -0.06), (0, 0.29, -0.08), 1)        # corona
for i in range(8):                      # cola enroscada
    a = i * 0.62
    k = 1 - i * 0.08
    p.elip((0, -0.26 + math.sin(a) * 0.055, -0.02 + math.cos(a) * 0.065), (0.033 * k, 0.033 * k, 0.033 * k), 4, 2, 1)
p.cuad((0, 0.0, -0.1), (0, 0.14, -0.13), (0, 0.03, -0.2), (0, -0.1, -0.13), 2)   # aleta dorsal
p.crear()
print("TRIS", triangulos())
