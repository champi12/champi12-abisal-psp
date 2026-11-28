"""Granja de algas (módulo de la base): tanque bajo con cúpula de cristal y cultivos
luminosos dentro, unido a la base por un túnel hacia -x."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
GRIS = material("gris", 0xb8bcc0)
OSCURO = material("oscuro", 0x5a6268)
TUBO = material("tunel", 0x9aa2a8)
CRISTAL = material("cristal", 0x7ab8c4, rugosidad=0.1)
ALGA = material("algas", 0x6aff8a, emision=3)
ALGA2 = material("algas2", 0xb0ff5a, emision=3)
TIERRA = material("tierra", 0x3a5a30)
p = PiezaJ("granja#1", [GRIS, OSCURO, TUBO, CRISTAL, ALGA, ALGA2, TIERRA])
p.torno([(0, 0), (2.1, 0.05), (2.2, 0.8), (1.9, 0.9), (0, 0.95)], 14, [1, 0, 0, 6], eje='y')
p.cajaJ((-2.9, 1.4, 0), (1.4, 0.55, 0.55), 2)                               # túnel a la base
for x in (-2.4, -3.3):
    p.cajaJ((x, 1.4, 0), (0.1, 0.65, 0.65), 1)
for i in range(6):                                                         # montantes de la cúpula
    a = i * math.tau / 6
    p.cajaJ((math.cos(a) * 1.95, 1.75, math.sin(a) * 1.95), (0.08, 0.85, 0.08), 0)
for i in range(11):                                                        # cultivos: tallos con hojas
    a = i * 0.62
    r = 0.4 + (i % 3) * 0.48
    x, z = math.cos(a) * r, math.sin(a) * r
    h = 0.6 + (i % 2) * 0.3
    m = 4 + i % 2
    p.torno([(0.05, 0), (0.04, h)], 4, m, eje='y', centro=(x, 0.95, z))
    p.cuad((x, 0.95 + h * 0.4, z), (x + 0.18, 0.95 + h * 0.7, z + 0.05), (x + 0.04, 0.95 + h * 0.6, z), (x, 0.95 + h * 0.5, z), m)
    p.cuad((x, 0.95 + h * 0.6, z), (x - 0.04, 0.95 + h * 0.75, z), (x - 0.16, 0.95 + h * 0.9, z - 0.05), (x, 0.95 + h * 0.7, z), m)
p.torno([(2.1, 0), (1.6, 0.35), (0.5, 0.6), (0, 0.65)], 12, 3, eje='y', centro=(0, 2.55, 0))
p.torno([(2.12, -0.05), (2.12, 0.05)], 14, 0, eje='y', centro=(0, 2.55, 0))
p.crear()
print("TRIS", triangulos())
