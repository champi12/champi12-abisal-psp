"""Escupidor de cueva: pez blindado y redondo con boca de tubo y sacos de ácido luminosos,
~0,9 m. Piezas: tubo de la boca#1 (z > 0,38), cola#2 (z < -0,34), cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CUERPO = material("cuerpo", 0x5a4a6e)
PLACA = material("placas", 0x3a2e48)
TUBO = material("tubo", 0x9a6aaa)
ACIDO = material("acido", 0x9aff40, emision=5)
OJO = material("ojos", 0xffe060, emision=4)
COLA = material("cola", 0x8a6aa0)
p = PiezaJ("escupidor", [CUERPO, PLACA, TUBO, ACIDO, OJO, COLA])
p.torno([(0, -0.4), (0.22, -0.32), (0.36, -0.1), (0.38, 0.1), (0.28, 0.3), (0.12, 0.38)], 10, [0, 0, 1, 0, 0])
for i in range(4):                       # placas del lomo
    p.cajaJ((0, 0.33 - abs(i - 1.5) * 0.03, -0.2 + i * 0.14), (0.3 - abs(i - 1.5) * 0.05, 0.07, 0.12), 1)
p.torno([(0.09, 0.38), (0.13, 0.42), (0.14, 0.53), (0.1, 0.59), (0.05, 0.62)], 10, [2, 2, 0, 2])
p.torno([(0.05, 0.62), (0, 0.6)], 10, 3)
for s in (1, -1):
    p.elip((s * 0.34, -0.05, 0.05), (0.07, 0.12, 0.12), 6, 3, 3)
    p.elip((s * 0.2, 0.15, 0.3), (0.05, 0.05, 0.04), 5, 3, 4)
p.cuad((0, 0, -0.36), (0, 0.27, -0.62), (0, 0, -0.52), (0, -0.27, -0.62), 5)
partir(p, [[-99, 99, -99, 99, 0.38, 99], [-99, 99, -99, 99, -99, -0.34]], "escupidor")
print("TRIS", triangulos())
