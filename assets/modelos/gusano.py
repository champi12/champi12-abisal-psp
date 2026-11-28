"""Gusano de arena (praderas): gusano segmentado que sale de la arena con boca de anillos de
colmillos. Piezas: cabeza#1, colmillo#2 (el juego dibuja varios), segmento#3, cola#4."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
ARENA = material("arena", 0xc8a870)
ARENA2 = material("arena2", 0xb89860)
OSCURO = material("oscuro", 0x3a2418)
COLMILLO = material("colmillo", 0xe0c890)
OJO = material("ojos", 0x101010)
c = PiezaJ("cabeza#1", [ARENA, ARENA2, OSCURO, OJO])
c.torno([(0.3, -0.3), (0.33, 0.0), (0.28, 0.25), (0.14, 0.35)], 10, [0, 1, 2])
c.torno([(0.14, 0.35), (0.09, 0.33)], 10, 2)
for k in range(6):
    a = k * 1.047
    c.elip((math.cos(a) * 0.27, math.sin(a) * 0.27, 0.18), (0.028, 0.028, 0.02), 4, 2, 3)
c.crear()
f = PiezaJ("colmillo#2", [COLMILLO, OSCURO])
f.tri((-0.08, 0, 0), (0.08, 0, 0), (0, 0.05, 0.28), 0)
f.tri((0.08, 0, 0), (-0.08, 0, 0), (0, -0.02, 0.26), 0)
f.tri((-0.03, 0.04, 0.22), (0.03, 0.04, 0.22), (0, -0.04, 0.34), 1)
f.crear()
s = PiezaJ("segmento#3", [ARENA2, OSCURO, ARENA])
s.torno([(0.28, -0.28), (0.32, -0.15), (0.32, 0.1), (0.3, 0.28)], 10, [0, 1, 2])
for k in range(5):
    a = k * 1.257
    s.tri((math.cos(a) * 0.31, math.sin(a) * 0.31, 0.05), (math.cos(a) * 0.42, math.sin(a) * 0.42, -0.08), (math.cos(a) * 0.31, math.sin(a) * 0.31, -0.12), 1)
s.crear()
t = PiezaJ("cola#4", [ARENA2, OSCURO])
t.torno([(0.26, 0.28), (0.2, 0.0), (0.08, -0.4), (0, -0.5)], 9, [0, 0, 1])
t.crear()
