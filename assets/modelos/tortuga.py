"""Tortuga de coral (bajíos y bosque): tortuga lenta con un arrecife en el caparazón, ~1,4 m.
Piezas: aletas #1 der-delante, #2 izq-delante, #3 der-detrás, #4 izq-detrás; cuerpo#5."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CAPA = material("caparazon", 0x6a7a4a)
ESCUDO = material("escudos", 0x8a6a3a)
PIEL = material("piel", 0x7a8a5a)
CORAL = material("coral", 0xff8a6a, emision=2)
CORAL2 = material("coral2", 0xf0d060, emision=2)
OJO = material("ojos", 0x101010)
p = PiezaJ("tortuga", [CAPA, ESCUDO, PIEL, CORAL, CORAL2, OJO])
p.torno([(0, -0.12), (0.5, -0.06), (0.56, 0.08), (0.46, 0.22), (0.26, 0.31), (0, 0.33)], 12, [2, 0, 0, 1, 1], eje='y', aplastar=(0.74, 1.25))
for i in range(5):                      # escudos del caparazón
    a = i * math.tau / 5
    p.elip((math.cos(a) * 0.26, 0.27, math.sin(a) * 0.34), (0.13, 0.04, 0.15), 6, 2, 1)
p.elip((0, 0.33, 0), (0.12, 0.04, 0.14), 6, 2, 1)
for (x, z, h, m) in ((0.15, 0.1, 0.16, 3), (-0.18, -0.15, 0.13, 4), (0.05, -0.28, 0.1, 3), (-0.08, 0.22, 0.09, 4)):
    p.torno([(0.04, 0), (0.05, h * 0.6), (0.025, h)], 5, m, eje='y', centro=(x, 0.3, z))
    p.elip((x + 0.04, 0.3 + h * 0.8, z), (0.03, 0.03, 0.03), 4, 2, m)
p.elip((0, 0.06, 0.8), (0.15, 0.13, 0.2), 7, 4, 2)            # cabeza
p.torno([(0.15, 0.55), (0.13, 0.7)], 7, 2, centro=(0, 0.06, 0))
for s in (1, -1):
    p.elip((s * 0.1, 0.13, 0.92), (0.035, 0.035, 0.035), 4, 2, 5)
    p.cuad((s * 0.38, 0, 0.38), (s * 0.95, -0.02, 0.12), (s * 0.9, -0.03, 0.0), (s * 0.4, 0, 0.05), 2)   # aletas grandes
    p.cuad((s * 0.32, 0, -0.42), (s * 0.6, -0.02, -0.68), (s * 0.45, -0.02, -0.75), (s * 0.2, 0, -0.55), 2)
partir(p, [[0.42, B, -B, B, -0.2, B], [-B, -0.42, -B, B, -0.2, B], [0.34, B, -0.1, 0.03, -B, -0.45], [-B, -0.34, -0.1, 0.03, -B, -0.45]], "tortuga")
print("TRIS", triangulos())
