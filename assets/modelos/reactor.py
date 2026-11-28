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
from abisal_deco import roca, palo
p = PiezaJ("vasija#1", MATS)
k = p.marca()
# blocky vessel: two crossed slabs make a stepped octagon, with a stepped lid
p.cajaJ((0, 3.5, 0), (4.8, 7.0, 3.0), 0)
p.cajaJ((0, 3.5, 0), (3.0, 7.0, 4.8), 0)
p.cajaJ((0, 7.2, 0), (3.6, 0.5, 2.2), 1)
p.cajaJ((0, 7.2, 0), (2.2, 0.5, 3.6), 1)
p.cajaJ((0, 7.55, 0), (1.6, 0.3, 1.6), 0)
for i in range(6):                                                  # aletas de refrigeración
    y = 1.35 + i * 0.9
    p.cajaJ((0, y, 0), (6.0, 0.25, 2.2), 1)
    p.cajaJ((0, y, 0), (2.2, 0.25, 6.0), 1)
for i in range(4):                                                  # franjas de peligro
    for s in (1, -1):
        x = -1.8 + i * 1.2
        p.cajaJ((x, 6.7, s * 2.42), (0.6, 0.3, 0.06), 2 if i % 2 else 3)
        p.cajaJ((s * 2.42, 6.7, x), (0.06, 0.3, 0.6), 3 if i % 2 else 2)
# ventana del núcleo
p.cajaJ((0, 4.0, 2.42), (1.4, 2.2, 0.12), 3)
# tubos rotos que salen de la tapa
for i, a in enumerate((0.4, 2.2, 4.1)):
    x, z = math.cos(a) * 1.3, math.sin(a) * 1.3
    palo(p, (x, 7.2, z), (x * 1.6, 8.2, z * 1.6), 0.55, 5)
    palo(p, (x * 1.6, 8.2, z * 1.6), (x * 2.6, 8.0, z * 2.6), 0.5, 5)
p.girar(k, (0, 0, 0), 24, 30)
# escombros y la grieta alrededor
for i in range(7):
    a = i * 0.9
    roca(p, (math.cos(a) * 3.6, 0.2, math.sin(a) * 3.2), (0.9, 0.5, 0.8), 40 + i, 3, 1)
p.crear()
n = PiezaJ("nucleo#2", MATS)
k = n.marca()
n.cajaJ((0, 4.0, 2.5), (1.2, 2.0, 0.04), 4)
n.cajaJ((0, 4.0, 0), (2.2, 4.0, 2.2), 4)
n.girar(k, (0, 0, 0), 24, 30)
n.crear()
print("TRIS", triangulos())
