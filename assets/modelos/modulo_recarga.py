"""Sala de recarga (módulo de la base): cilindro con tres bornes de carga naranjas y luz
en la cima, unido a la base por un túnel hacia +x."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
GRIS = material("gris", 0xc0c4c8)
OSCURO = material("oscuro", 0x3a4248)
BASE = material("base", 0x5a6268)
TUBO = material("tunel", 0x9aa2a8)
BORNE = material("bornes", 0xffa040, emision=4)
CIMA = material("cima", 0xffd080, emision=5)
FRANJA = material("franja", 0xe0702c)
p = PiezaJ("recarga#1", [GRIS, OSCURO, BASE, TUBO, BORNE, CIMA, FRANJA])
p.torno([(0, 0), (2.1, 0.05), (2.2, 2.4), (1.6, 3.1), (0, 3.2)], 14, [2, 0, 0, 0], eje='y')
p.torno([(2.22, 1.6), (2.22, 1.72)], 14, 6, eje='y')
p.cajaJ((2.9, 1.4, 0), (1.4, 0.55, 0.55), 3)
for x in (2.4, 3.3):
    p.cajaJ((x, 1.4, 0), (0.1, 0.65, 0.65), 0)
for i in range(3):
    a = 1.6 + i * 1.4
    x, z = math.cos(a) * 2.25, math.sin(a) * 2.25
    p.cajaJ((x, 1.2, z), (0.3, 1.0, 0.3), 1)
    p.cajaJ((x, 2.3, z), (0.34, 0.12, 0.34), 4)
    p.cajaJ((x * 1.02, 1.3, z * 1.02), (0.12, 0.4, 0.12), 4)
for i in range(6):
    a = i * math.tau / 6
    p.cajaJ((math.cos(a) * 1.5, 3.0, math.sin(a) * 1.5), (0.12, 0.2, 0.12), 1)
p.torno([(0.25, 0), (0.25, 0.12), (0, 0.14)], 10, 5, eje='y', centro=(0, 3.2, 0))
p.crear()
print("TRIS", triangulos())
