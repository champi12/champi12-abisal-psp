"""Vejigas de aire (se respira de ellas): sacos translúcidos con gas luminoso sobre tallos.
Dos variantes: vejiga#1, #2. El juego las escala al vaciarse y llenarse."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
BASE = material("base", 0x5a4a3a)
TALLO = material("tallo", 0x6a7a3a)
SACO = material("saco", 0xb8f0c0)
SACO2 = material("saco2", 0xa0e0b0)
GAS = material("gas", 0x8affd0, emision=4)
for v, sem in enumerate((1, 6)):
    rnd = random.Random(sem)
    p = PiezaJ(f"vejiga{v}#{v + 1}", [BASE, TALLO, SACO, SACO2, GAS])
    p.torno([(0.45, 0), (0.35, 0.2), (0.18, 0.45), (0.1, 0.6)], 8, 0, eje='y')
    for i in range(4):
        a = i * 1.571 + rnd.random()
        r = 0.35 + rnd.random() * 0.3
        h = 0.8 + rnd.random() * 0.7
        x, z = math.cos(a) * r, math.sin(a) * r
        sr = 0.22 + rnd.random() * 0.12
        p.tubo([(0, 0.4, 0, 0.04), (x * 0.5, 0.4 + h * 0.4, z * 0.5, 0.035), (x, 0.4 + h * 0.75 - sr, z, 0.03)], 4, 1, False)
        p.elip((x, 0.4 + h * 0.75, z), (sr, sr * 1.25, sr), 8, 5, 2 + i % 2)
        p.elip((x, 0.4 + h * 0.75 + sr * 0.3, z), (sr * 0.45, sr * 0.4, sr * 0.45), 5, 3, 4)
    p.crear()
