"""Módulo de hábitat (la base del jugador): cúpula blanca sobre cuatro patas, ventanas
cálidas, escotilla cian en la cima y luz roja de posición."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
BLANCO = material("blanco", 0xc8ccd0)
GRIS = material("gris", 0x9aa2a8)
OSCURO = material("oscuro", 0x4a5258)
VENTANA = material("ventanas", 0xffe0a0, emision=4)
CIAN = material("escotilla", 0x5ff0ff, emision=4)
ROJA = material("roja", 0xff5a3a, emision=5)
NARANJA = material("franja", 0xe0702c)
p = PiezaJ("habitat#1", [BLANCO, GRIS, OSCURO, VENTANA, CIAN, ROJA, NARANJA])
p.torno([(0, 1.2), (2.9, 1.3), (3.0, 1.8), (3.0, 3.8), (2.6, 4.4), (0.9, 4.7), (0, 4.75)], 16, [2, 1, 0, 0, 1, 1], eje='y')
p.torno([(3.02, 2.0), (3.02, 2.12)], 16, 6, eje='y')
p.torno([(3.02, 3.2), (3.02, 3.28)], 16, 2, eje='y')
for i in range(4):                                                       # patas con pies
    a = i * math.tau / 4 + math.pi / 4
    x, z = math.cos(a) * 2.4, math.sin(a) * 2.4
    p.torno([(0.14, 0), (0.12, 1.3)], 6, 2, eje='y', centro=(x, 0, z))
    p.torno([(0.4, 0), (0.3, 0.12), (0.14, 0.18)], 8, 2, eje='y', centro=(x * 1.1, 0, z * 1.1))
    p.cajaJ((x * 0.75, 0.9, z * 0.75), (0.1, 0.1, 1.4), 2, rot=(0, 0, -math.degrees(a) + 90))
for i in range(8):                                                       # ventanas con marco
    a = i * math.tau / 8
    c, s = math.cos(a), math.sin(a)
    p.cajaJ((c * 3.03, 2.6, s * 3.03), (0.75, 0.55, 0.06), 1, rot=(0, 0, math.degrees(-a) + 90))
    p.cajaJ((c * 3.06, 2.6, s * 3.06), (0.62, 0.42, 0.04), 3, rot=(0, 0, math.degrees(-a) + 90))
p.torno([(0.65, 0), (0.65, 0.12), (0, 0.14)], 12, [1, 4], eje='y', centro=(0, 4.72, 0))
p.cajaJ((1.6, 4.6, 0), (0.15, 0.3, 0.15), 2)
p.elip((1.6, 4.82, 0), (0.1, 0.1, 0.1), 4, 2, 5)
p.cajaJ((0, 1.7, 3.0), (1.0, 1.6, 0.2), 1)                                # esclusa de entrada
p.cajaJ((0, 1.7, 3.1), (0.7, 1.3, 0.05), 2)
p.crear()
print("TRIS", triangulos())
