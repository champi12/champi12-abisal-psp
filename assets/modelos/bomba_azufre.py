"""Bomba de azufre (meseta roja): un saco vivo de gas sulfuroso, del tamaño de un balón, con
púas y un núcleo amarillo que palpita. Se pega a lo que se mueve y revienta. Una pieza
centrada en el origen (el juego la hincha antes de estallar)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
PIEL = material("piel", 0x8a5a2a)
PIEL2 = material("piel2", 0x6a3e1e)
AZUFRE = material("azufre", 0xffe040, emision=4)
PUA = material("pua", 0xd8c890)
OJO = material("ojo", 0x101010)
p = PiezaJ("bomba#1", [PIEL, PIEL2, AZUFRE, PUA, OJO])
p.elip((0, 0, 0), (0.45, 0.4, 0.45), 10, 7, 0)
rnd = random.Random(7)
# vetas luminosas de azufre bajo la piel
for i in range(6):
    a = i * math.tau / 6
    p.elip((math.cos(a) * 0.36, rnd.uniform(-0.15, 0.15), math.sin(a) * 0.36), (0.1, 0.12, 0.1), 5, 3, 2)
# púas
for i in range(14):
    a = rnd.uniform(0, math.tau)
    e = rnd.uniform(-1.1, 1.1)
    d = (math.cos(a) * math.cos(e), math.sin(e), math.sin(a) * math.cos(e))
    p.tubo([(d[0] * 0.38, d[1] * 0.34, d[2] * 0.38, 0.05), (d[0] * 0.62, d[1] * 0.56, d[2] * 0.62, 0.008)], 4, 3)
# boca y ojos (una cara pequeña y fea delante)
p.elip((0, -0.05, 0.42), (0.12, 0.06, 0.05), 6, 3, 1)
for s in (1, -1):
    p.elip((s * 0.14, 0.1, 0.38), (0.05, 0.05, 0.04), 5, 3, 4)
# filamentos que cuelgan
for i in range(4):
    a = i * math.tau / 4 + 0.4
    p.tubo([(math.cos(a) * 0.2, -0.3, math.sin(a) * 0.2, 0.025), (math.cos(a) * 0.25, -0.7, math.sin(a) * 0.25, 0.012)], 3, 1)
p.crear()
print("TRIS", triangulos())
