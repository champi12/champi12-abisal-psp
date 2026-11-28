"""Anguila de brasa (grietas termales): anguila negra de 4,2 m con bandas naranjas que
brillan y aletas de fuego. Cuatro segmentos (cabeza#1 ... cola#4) desde z = 0 hacia atrás."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
NEGRO = material("negro", 0x1c181a)
BANDA = material("bandas", 0xd8501a, emision=3)
ALETA = material("aletas", 0xff6a1a, emision=4)
BOCA = material("boca", 0x2a2224)
OJO = material("ojos", 0xffe070, emision=5)
L = 4.2
paso = L / 4
def radio(t):
    return 0.26 * (1 - t * 0.85) + 0.03
for k in range(4):
    p = PiezaJ(f"segmento{k}#{k + 1}", [NEGRO, BANDA, ALETA, BOCA, OJO])
    perfil = []
    for j in range(4):
        t = (k + j / 3) / 4
        z = -paso * j / 3 + (0.08 if j == 0 else 0) - (0.08 if j == 3 else 0)
        perfil.append((radio(t), z))
    q = k * 3
    p.torno(perfil, 7, [1 if (q % 3) == 1 else 0, 1 if ((q + 1) % 3) == 1 else 0, 1 if ((q + 2) % 3) == 1 else 0], centro=(0, 0.1, 0))
    for j in range(3):
        t = (k + (j + 0.5) / 3) / 4
        zm = -paso * (j + 0.5) / 3
        y0 = 0.1 + radio(t) * 0.85
        h = 0.24 * (1 - t * 0.6)
        p.tri((0, y0, zm + 0.15), (0, y0 + h, zm - 0.05), (0, y0, zm - 0.18), 2)
        p.tri((0, 0.1 - radio(t) * 0.85, zm + 0.12), (0, 0.1 - radio(t) * 0.85, zm - 0.15), (0, 0.1 - radio(t) * 0.85 - h * 0.6, zm - 0.06), 2)
    if k == 3:
        p.cuad((0, 0.1, -paso + 0.05), (0, 0.5, -paso - 0.6), (0, 0.1, -paso - 0.35), (0, -0.3, -paso - 0.55), 2)
    if k == 0:
        y = 0.1
        p.cuad((-0.24, y - 0.1, 0), (0.24, y - 0.1, 0), (0.1, y - 0.02, 0.5), (-0.1, y - 0.02, 0.5), 3)
        p.cuad((-0.24, y + 0.14, 0), (-0.1, y - 0.02, 0.5), (0.1, y - 0.02, 0.5), (0.24, y + 0.14, 0), 0)
        p.tri((-0.24, y - 0.1, 0), (-0.1, y - 0.02, 0.5), (-0.24, y + 0.14, 0), 0)
        p.tri((0.24, y - 0.1, 0), (0.24, y + 0.14, 0), (0.1, y - 0.02, 0.5), 0)
        for s in (1, -1):
            p.elip((s * 0.17, y + 0.1, 0.07), (0.05, 0.05, 0.06), 5, 3, 4)
    p.crear()
print("TRIS", triangulos())
