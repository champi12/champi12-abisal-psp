"""Parásitos: dos chupadores rojos de patas finas (se pegan al casco o al errante)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
ROJO = material("rojo", 0xc83a22)
PATA = material("patas", 0x8a2214)
OJO = material("ojos", 0xffe040, emision=5)
p = PiezaJ("parasitos#1", [ROJO, PATA, OJO])
rnd = random.Random(9)
for i in range(2):
    x, z = (rnd.random() - 0.5) * 0.9, (rnd.random() - 0.5) * 0.9
    p.elip((x, 0.08, z), (0.12, 0.065, 0.11), 6, 3, 0)
    for k in (-1, 1):
        for dz in (0.06, -0.01, -0.07):
            p.tri((x, 0.06, z + dz), (x + k * 0.22, -0.06, z + dz * 1.6), (x, 0.06, z + dz - 0.03), 1)
    for s in (1, -1):
        p.elip((x + s * 0.04, 0.13, z + 0.09), (0.02, 0.02, 0.02), 4, 2, 2)
p.crear()
