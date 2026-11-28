"""Lanza vítrea (arrecife de cristal): serpiente de vidrio de 4 m, casi transparente, con la
espina dorsal luminosa a la vista y un hocico en forma de lanza. Caza de día, cuando la luz
la vuelve invisible. Cuatro segmentos (cabeza#1 ... cola#4) desde z = 0 hacia atrás."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
VIDRIO = material("vidrio", 0x9fe4ff, emision=0.6)
VIDRIO2 = material("vidrio2", 0x6ab8e8, emision=0.4)
ESPINA = material("espina", 0xe0fbff, emision=5)
ALETA = material("aleta", 0xc8f4ff, emision=1.5)
OJO = material("ojo", 0xff3a6a, emision=6)
MATS = [VIDRIO, VIDRIO2, ESPINA, ALETA, OJO]
L = 4.0
paso = L / 4
def radio(t):
    return 0.22 * (1 - t * 0.8) + 0.03
for k in range(4):
    p = PiezaJ(f"segmento{k}#{k + 1}", MATS)
    perfil = []
    for j in range(4):
        t = (k + j / 3) / 4
        z = -paso * j / 3 + (0.06 if j == 0 else 0) - (0.06 if j == 3 else 0)
        perfil.append((radio(t), z))
    p.torno(perfil, 6, [0, 1, 0], centro=(0, 0.1, 0), aplastar=(0.8, 1.0))
    # la espina que brilla dentro: una varilla y vértebras
    p.torno([(0.04, 0.02), (0.04, -paso - 0.02)], 4, 2, centro=(0, 0.12, 0))
    for j in range(3):
        zm = -paso * (j + 0.5) / 3
        p.elip((0, 0.12, zm), (0.07, 0.07, 0.05), 4, 3, 2)
        t = (k + (j + 0.5) / 3) / 4
        h = 0.3 * (1 - t * 0.6)
        y0 = 0.1 + radio(t) * 0.9
        p.tri((0, y0, zm + 0.16), (0, y0 + h, zm - 0.12), (0, y0, zm - 0.2), 3, doble=True)
    if k == 3:
        p.cuad((0, 0.1, -paso + 0.05), (0, 0.6, -paso - 0.7), (0, 0.1, -paso - 0.3), (0, -0.4, -paso - 0.7), 3, doble=True)
    if k == 0:
        # hocico de lanza
        p.torno([(0.2, 0.0), (0.12, 0.3), (0.05, 0.75), (0, 1.1)], 6, 0, centro=(0, 0.1, 0), aplastar=(0.8, 0.7))
        for s in (1, -1):
            p.elip((s * 0.13, 0.17, 0.12), (0.05, 0.05, 0.06), 5, 3, 4)
    p.crear()
print("TRIS", triangulos())
