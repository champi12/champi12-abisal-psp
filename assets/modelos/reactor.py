"""Reactor de la Halcyon (grietas termales): el núcleo de energía de la estación, caído en una
grieta y medio enterrado, inclinado. Un cilindro blindado con aletas de refrigeración, tubos
rotos y una ventana por la que se ve el núcleo verde, que todavía canta desafinado.
Piezas: "vasija#1" y "nucleo#2" (el brillo del núcleo, que se apaga al final)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
ACERO = material("acero", 0x5a6068, metal=0.3)
ACERO2 = material("acero2", 0x464a52, metal=0.3)
AMARILLO = material("peligro", 0xe8c020)
NEGRO = material("negro", 0x141416)
NUCLEO = material("nucleo", 0x6aff9a, emision=6)
OXIDO = material("oxido", 0x8a4a2a)
MATS = [ACERO, ACERO2, AMARILLO, NEGRO, NUCLEO, OXIDO]
p = PiezaJ("vasija#1", MATS)
k = p.marca()
p.torno([(0, 0), (2.2, 0.1), (2.4, 0.6), (2.4, 6.4), (2.0, 7.0), (1.0, 7.4), (0, 7.5)], 12, 0, eje='y')
for i in range(6):                                                  # aletas de refrigeración
    y = 1.2 + i * 0.9
    p.torno([(2.4, y), (3.0, y + 0.08), (3.0, y + 0.22), (2.4, y + 0.3)], 12, 1, eje='y')
for i in range(8):                                                  # franjas de peligro
    a = i * math.tau / 8
    p.cajaJ((math.cos(a) * 2.42, 6.7, math.sin(a) * 2.42), (0.5, 0.25, 0.05), 2 if i % 2 else 3)
# ventana del núcleo
p.cajaJ((0, 4.0, 2.38), (1.4, 2.2, 0.12), 3)
# tubos rotos que salen de la tapa
for i, a in enumerate((0.4, 2.2, 4.1)):
    x, z = math.cos(a) * 1.3, math.sin(a) * 1.3
    p.tubo([(x, 7.2, z, 0.3), (x * 1.6, 8.2, z * 1.6, 0.28), (x * 2.6, 8.0, z * 2.6, 0.26)], 8, 5)
p.girar(k, (0, 0, 0), 24, 30)
# escombros y la grieta alrededor
for i in range(7):
    a = i * 0.9
    p.rocaJ((math.cos(a) * 3.6, 0.2, math.sin(a) * 3.2), (0.9, 0.5, 0.8), 40 + i, 0.3, 0, 3)
p.crear()
n = PiezaJ("nucleo#2", MATS)
k = n.marca()
n.cajaJ((0, 4.0, 2.46), (1.2, 2.0, 0.04), 4)
n.elip((0, 4.0, 0), (1.3, 2.2, 1.3), 8, 5, 4)
n.girar(k, (0, 0, 0), 24, 30)
n.crear()
print("TRIS", triangulos())
