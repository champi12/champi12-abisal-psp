"""Dientesierra (bajíos y meseta): serpiente marina gris azulada de 4,5 m con aletas
dorsales naranjas, mandíbula de dientes y ojos ámbar. Cuatro segmentos (cabeza#1 ... cola#4),
cada uno desde z = 0 hacia atrás; el juego los encadena y ondula."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
LOMO = material("lomo", 0x4a5560)
LOMO2 = material("lomo2", 0x5a6672)
VIENTRE = material("vientre", 0x8a9098)
ALETA = material("aletas", 0xe0702c)
BOCA = material("boca", 0x8a3a30)
DIENTE = material("dientes", 0xf2ead8)
OJO = material("ojos", 0xffb02e, emision=5)
L = 4.5
paso = L / 4
def radio(t):
    return 0.36 * (1 - t * 0.8) + 0.04
for k in range(4):
    p = PiezaJ(f"segmento{k}#{k + 1}", [LOMO, LOMO2, VIENTRE, ALETA, BOCA, DIENTE, OJO])
    perfil = []
    for j in range(4):
        t = (k + j / 3) / 4
        z = -paso * j / 3 + (0.08 if j == 0 else 0) - (0.08 if j == 3 else 0)
        perfil.append((radio(t), z))
    p.torno(perfil, 8, [k % 2, (k + 1) % 2, k % 2], centro=(0, 0.15, 0), aplastar=(1, 0.9))
    for j in range(3):                   # escamas claras del vientre
        t = (k + (j + 0.5) / 3) / 4
        p.cajaJ((0, 0.15 - radio(t) * 0.85, -paso * (j + 0.5) / 3), (radio(t) * 1.1, 0.04, paso / 3 * 0.9), 2)
    for j in range(3):                   # aleta dorsal en sierra
        t = (k + (j + 0.5) / 3) / 4
        zm = -paso * (j + 0.5) / 3
        y0 = 0.15 + radio(t) * 0.85
        h = 0.38 * (1 - t * 0.6)
        p.tri((0, y0, zm + 0.17), (0, y0 + h, zm - 0.06), (0, y0, zm - 0.2), 3)
    if k == 3:
        p.cuad((0, 0.15, -paso + 0.05), (0, 0.65, -paso - 0.7), (0, 0.15, -paso - 0.4), (0, -0.25, -paso - 0.6), 3)
    if k == 0:                           # cabeza en cuña, boca roja, dientes y ojos
        y = 0.15
        p.cuad((-0.36, y - 0.13, 0), (0.36, y - 0.13, 0), (0.16, y - 0.03, 0.8), (-0.16, y - 0.03, 0.8), 0)
        p.cuad((-0.36, y + 0.17, 0), (-0.16, y - 0.03, 0.8), (0.16, y - 0.03, 0.8), (0.36, y + 0.17, 0), 1)
        p.tri((-0.36, y - 0.13, 0), (-0.16, y - 0.03, 0.8), (-0.36, y + 0.17, 0), 0)
        p.tri((0.36, y - 0.13, 0), (0.36, y + 0.17, 0), (0.16, y - 0.03, 0.8), 0)
        p.cuad((-0.32, y - 0.2, 0), (-0.15, y - 0.42, 0.62), (0.15, y - 0.42, 0.62), (0.32, y - 0.2, 0), 4)
        for i in range(5):
            x = -0.2 + i * 0.1
            p.tri((x - 0.035, y - 0.11, 0.52), (x + 0.035, y - 0.11, 0.52), (x, y - 0.3, 0.52), 5)
            p.tri((x - 0.03, y - 0.38, 0.5), (x + 0.03, y - 0.38, 0.5), (x, y - 0.2, 0.5), 5)
        for s in (1, -1):
            p.elip((s * 0.29, y + 0.06, 0.2), (0.06, 0.06, 0.09), 5, 3, 6)
            p.tri((s * 0.3, y + 0.15, 0.05), (s * 0.45, y + 0.32, -0.2), (s * 0.25, y + 0.18, -0.15), 3)   # crestas
    p.crear()
print("TRIS", triangulos())
