"""Roehuesos (osario): anguila pálida de 3,8 m con placas de hueso, costillas que asoman por
la piel y ojos rojos. Caza de noche en grupo. Cuatro segmentos (cabeza#1 ... cola#4)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
PIEL = material("piel", 0x8a7e72)
PIEL2 = material("piel2", 0x6e6258)
HUESO = material("hueso", 0xe6dcc4)
BOCA = material("boca", 0x3a1414)
OJO = material("ojo", 0xff2a10, emision=6)
DIENTE = material("diente", 0xfff4dc)
MATS = [PIEL, PIEL2, HUESO, BOCA, OJO, DIENTE]
L = 3.8
paso = L / 4
def radio(t):
    return 0.27 * (1 - t * 0.8) + 0.03
for k in range(4):
    p = PiezaJ(f"segmento{k}#{k + 1}", MATS)
    perfil = []
    for j in range(4):
        t = (k + j / 3) / 4
        z = -paso * j / 3 + (0.07 if j == 0 else 0) - (0.07 if j == 3 else 0)
        perfil.append((radio(t), z))
    p.torno(perfil, 7, [0, 1, 0], centro=(0, 0.1, 0))
    for j in range(3):
        t = (k + (j + 0.5) / 3) / 4
        zm = -paso * (j + 0.5) / 3
        r = radio(t)
        # placa dorsal de hueso y costillas que asoman a los lados
        p.cajaJ((0, 0.1 + r * 0.95, zm), (r * 0.9, 0.08, paso / 3 * 0.8), 2)
        p.tri((0, 0.1 + r, zm + 0.1), (0, 0.1 + r + 0.22 * (1 - t), zm - 0.08), (0, 0.1 + r, zm - 0.14), 2, doble=True)
        for s in (1, -1):
            p.tubo([(s * r * 0.7, 0.1 + r * 0.5, zm, 0.035), (s * (r + 0.1), 0.1, zm - 0.05, 0.03),
                    (s * r * 0.7, 0.1 - r * 0.6, zm - 0.08, 0.025)], 4, 2, tapas=False)
    if k == 3:
        p.tubo([(0, 0.1, -paso, 0.05), (0, 0.15, -paso - 0.5, 0.03)], 4, 2)
    if k == 0:
        y = 0.1
        p.cuad((-0.27, y - 0.12, 0), (0.27, y - 0.12, 0), (0.12, y - 0.05, 0.55), (-0.12, y - 0.05, 0.55), 3)
        p.cuad((-0.27, y + 0.16, 0), (-0.12, y - 0.0, 0.55), (0.12, y - 0.0, 0.55), (0.27, y + 0.16, 0), 2)
        p.tri((-0.27, y - 0.12, 0), (-0.12, y - 0.05, 0.55), (-0.27, y + 0.16, 0), 0)
        p.tri((0.27, y - 0.12, 0), (0.27, y + 0.16, 0), (0.12, y - 0.05, 0.55), 0)
        for s in (1, -1):
            p.elip((s * 0.19, y + 0.1, 0.1), (0.05, 0.05, 0.06), 5, 3, 4)
            for d in range(3):
                z = 0.45 - d * 0.15
                p.tri((s * (0.13 + d * 0.04), y - 0.03, z), (s * (0.13 + d * 0.04), y - 0.03, z - 0.06),
                      (s * (0.12 + d * 0.04), y - 0.15, z - 0.03), 5, doble=True)
    p.crear()
print("TRIS", triangulos())
