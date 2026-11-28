"""Velo abisal: criatura en forma de lámina ancha violeta oscura con manchas luminosas y
volantes. Piezas: cabeza#1, cuerpo#2, cola#3 (cada una sigue su +z local)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
OSC = material("piel", 0x2a1e48)
OSC2 = material("piel2", 0x34265a)
VOLANTE = material("volantes", 0x8a3a9a)
FILO = material("filo", 0x4a3478)
LUZ = material("manchas", 0x60e8ff, emision=5)
LUZ2 = material("cresta", 0xb080ff, emision=4)
OJO = material("ojos", 0xffb040, emision=5)
c = PiezaJ("cabeza#1", [OSC2, FILO, OJO, LUZ])
c.elip((0, 0, 0), (2.2, 0.65, 1.8), 10, 4, 0)
for s in (-1, 1):
    c.cuad((s * 1.0, 0, 1.3), (s * 1.6, 0, 2.4), (s * 1.2, -0.05, 3.1), (s * 0.7, 0, 2.0), 1)   # palpos
    c.elip((s * 0.6, 0.45, 1.2), (0.14, 0.1, 0.14), 5, 3, 2)
    c.elip((s * 1.1, 0.35, 0.9), (0.1, 0.08, 0.1), 4, 2, 2)
c.cajaJ((0, 0.6, -0.4), (0.5, 0.05, 0.2), 3)
c.crear()
for parte, nombre in ((1, "cuerpo#2"), (2, "cola#3")):
    cola = parte == 2
    ancho = 1.6 if cola else 3.2
    p = PiezaJ(nombre, [OSC, VOLANTE, LUZ, LUZ2, FILO])
    p.elip((0, 0, 0), (ancho, 0.25 if cola else 0.35, 1.7), 10, 3, 0)
    for s in (-1, 1):                       # volantes ondulados en los bordes
        for k in range(4):
            z0, z1 = -1.2 + k * 0.6, -0.6 + k * 0.6
            o = 0.15 if k % 2 == 0 else -0.1
            p.cuad((s * (ancho - 0.1), -0.1, z0), (s * (ancho + 0.4), -0.2 + o, z0 + 0.1), (s * (ancho + 0.4), -0.2 - o, z1 - 0.1), (s * (ancho - 0.1), -0.1, z1), 1)
        p.elip((s * ancho * 0.55, 0.22 if cola else 0.32, 0.4), (0.22, 0.04, 0.22), 6, 2, 2)
        p.elip((s * ancho * 0.28, 0.24 if cola else 0.36, -0.6), (0.15, 0.04, 0.15), 5, 2, 2)
    p.cajaJ((0, 0.25 if cola else 0.36, 0), (0.1, 0.04, 2.6), 3)
    if cola:
        p.cuad((0, 0, -1.6), (0.12, 0.02, -4.4), (-0.12, 0.02, -4.4), (0, 0, -1.6), 4)
        p.cuad((-0.6, 0, -4.2), (0.6, 0, -4.2), (0, 0.02, -4.8), (0, 0.02, -4.8), 4)
    p.crear()
