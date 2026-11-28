"""Chillón de cañón: raya-murciélago correosa de boca enorme y alas con garfios, ~2 m.
Piezas: ala derecha#1 (x > 0,32), ala izquierda#2 (x < -0,32), cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CUERO = material("cuero", 0x4a3a3a)
ALA = material("alas", 0x5e4844)
NERVIO = material("nervios", 0x3a2a2a)
GARFIO = material("garfios", 0xd0c8b8)
BOCA = material("boca", 0x8a3a4a)
OJO = material("ojos", 0xff6a2a, emision=5)
p = PiezaJ("chillon", [CUERO, ALA, NERVIO, GARFIO, BOCA, OJO])
p.torno([(0, -0.5), (0.24, -0.35), (0.35, -0.05), (0.33, 0.2), (0.22, 0.42), (0, 0.52)], 9, 0, aplastar=(1, 0.55))
for s in (1, -1):
    borde = [(s * 0.3, 0, 0.32), (s * 0.85, 0.12, 0.28), (s * 1.32, 0.15, 0.1), (s * 1.2, 0.07, -0.12),
             (s * 1.12, 0.05, -0.42), (s * 0.7, 0.02, -0.28), (s * 0.3, 0, -0.32)]
    raiz = (s * 0.32, 0.02, 0)
    for k in range(len(borde) - 1):
        p.tri(raiz, borde[k], borde[k + 1], 1)
    for k in (1, 2, 4):
        b = borde[k]
        p.cuad((raiz[0], raiz[1] + 0.01, raiz[2] - 0.01), (raiz[0], raiz[1] + 0.01, raiz[2] + 0.01),
               (b[0], b[1] + 0.01, b[2] + 0.01), (b[0], b[1] + 0.01, b[2] - 0.01), 2)
    p.tri((s * 1.3, 0.15, 0.1), (s * 1.48, 0.05, 0.22), (s * 1.3, 0.1, 0.0), 3)
    p.elip((s * 0.15, 0.12, 0.4), (0.05, 0.05, 0.05), 4, 2, 5)
p.cuad((-0.25, -0.05, 0.45), (0.25, -0.05, 0.45), (0.18, -0.22, 0.62), (-0.18, -0.22, 0.62), 4)
for i in range(6):
    x = -0.18 + i * 0.072
    p.tri((x - 0.022, -0.05, 0.5), (x + 0.022, -0.05, 0.5), (x, -0.14, 0.52), 3)
p.cuad((0, 0.05, -0.45), (0, 0.32, -1.25), (0, 0.05, -0.9), (0, -0.12, -1.05), 2)
partir(p, [[0.32, 99, -99, 99, -99, 99], [-99, -0.32, -99, 99, -99, 99]], "chillon")
