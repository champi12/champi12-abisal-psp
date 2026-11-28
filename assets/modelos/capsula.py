"""Cápsula de escape: flota en la superficie. Base de flotación con aros de cobre, torre,
cúpula de cristal, cuatro paneles solares en brazos y mástil con baliza cian."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CASCO = material("casco", 0x2e4a52)
COBRE = material("cobre", 0xb8763e, metal=0.5)
OSCURO = material("oscuro", 0x162228)
CRISTAL = material("cristal", 0x7fd0dc, rugosidad=0.1)
MARCO = material("marco", 0x3c5a62)
PANEL = material("panel", 0x1a3a4a)
CELDA = material("celdas", 0x24506a)
LUZ = material("luz", 0x6ff0ff, emision=6)
NARANJA = material("naranja", 0xe8692a)
p = PiezaJ("capsula#1", [CASCO, COBRE, OSCURO, CRISTAL, MARCO, PANEL, CELDA, LUZ, NARANJA])
p.torno([(0, -0.6), (2.2, -0.7), (2.8, -0.4), (2.9, 0.2), (2.6, 0.5), (0, 0.6)], 16, [2, 0, 1, 0, 0], eje='y')
p.torno([(2.92, -0.1), (2.92, 0.05)], 16, 8, eje='y')                     # franja naranja de flotación
for i in range(8):                                                       # asas de agarre
    a = i * math.tau / 8 + 0.2
    p.cajaJ((math.cos(a) * 2.75, 0.32, math.sin(a) * 2.75), (0.08, 0.08, 0.4), 1)
p.torno([(1.4, 0.5), (1.5, 0.7), (1.5, 2.1), (1.3, 2.4)], 12, [1, 0, 1], eje='y')
for i in range(6):                                                       # ojos de buey de la torre
    a = i * math.tau / 6
    p.elip((math.cos(a) * 1.5, 1.4, math.sin(a) * 1.5), (0.22, 0.22, 0.22), 6, 3, 3)
    p.torno([(0.26, 0), (0.26, 0.04)], 8, 1, eje='y', centro=(math.cos(a) * 1.49, 1.38, math.sin(a) * 1.49))
p.torno([(1.3, 0), (1.25, 0.3), (1.05, 0.6), (0.7, 0.85), (0.3, 1.0), (0, 1.05)], 12, 3, eje='y', centro=(0, 2.4, 0))
for i in range(4):                                                       # nervios de la cúpula
    a = i * math.tau / 4
    p.cajaJ((math.cos(a) * 0.95, 3.05, math.sin(a) * 0.95), (0.06, 0.06, 0.06), 4)
for i in range(4):                                                       # brazos y paneles solares
    a = i * math.tau / 4 + math.pi / 4
    c, s = math.cos(a), math.sin(a)
    for k in range(5):
        r = 2.2 + k * 0.1
        p.cajaJ((c * r, 0.55, s * r), (0.1, 0.08, 0.1), 1)
    cx, cz = c * 2.85, s * 2.85
    p.cuad((cx - s * 0.48 - c * 0.3, 0.6, cz + c * 0.48 - s * 0.3), (cx + s * 0.48 - c * 0.3, 0.6, cz - c * 0.48 - s * 0.3),
           (cx + s * 0.48 + c * 0.3, 0.6, cz - c * 0.48 + s * 0.3), (cx - s * 0.48 + c * 0.3, 0.6, cz + c * 0.48 + s * 0.3), 6)
    p.cajaJ((cx, 0.57, cz), (0.08, 0.04, 0.08), 5)
p.torno([(0.05, 0), (0.04, 2.2)], 6, 1, eje='y', centro=(0.8, 2.4, 0.5))  # mástil
p.elip((0.8, 4.7, 0.5), (0.15, 0.15, 0.15), 6, 3, 7)
p.crear()
print("TRIS", triangulos())
