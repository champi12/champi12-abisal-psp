"""Manta de cristal (praderas): raya azul pálida de ~3 m con crestas luminosas.
Piezas: ala derecha#1 (x > 0,45), ala izquierda#2 (x < -0,45) y cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
LOMO = material("lomo", 0x9cc4d8)
VIENTRE = material("vientre", 0x6f96aa)
COLA = material("cola", 0x5a7a8c)
LUZ = material("cristales", 0xbff4ff, emision=4)
OJO = material("ojos", 0x101820)
p = PiezaJ("manta", [LOMO, VIENTRE, COLA, LUZ, OJO])
p.torno([(0, -0.75), (0.3, -0.55), (0.45, -0.1), (0.42, 0.35), (0.28, 0.62), (0, 0.75)], 10, 0, aplastar=(1, 0.3))
for s in (1, -1):
    # ala en abanico: 4 tramos arriba y abajo, con la punta curvada hacia atrás
    borde = [(s * 0.36, 0.02, 0.58), (s * 0.85, 0.09, 0.35), (s * 1.3, 0.1, 0.05), (s * 1.58, 0.06, -0.2), (s * 1.1, 0.03, -0.42), (s * 0.4, 0, -0.58)]
    raiz = (s * 0.4, 0.04, 0.0)
    for k in range(len(borde) - 1):
        p.tri(raiz, borde[k], borde[k + 1], 0)
        a, b = borde[k], borde[k + 1]
        p.tri((raiz[0], raiz[1] - 0.06, raiz[2]), (b[0], b[1] - 0.04, b[2]), (a[0], a[1] - 0.04, a[2]), 1)
    for i in range(4):
        x = 0.55 + i * 0.25
        p.cajaJ((s * x, 0.1 - i * 0.01, 0.12 - i * 0.1), (0.06, 0.03, 0.22), 3)
    p.tri((s * 0.16, 0, 0.7), (s * 0.34, -0.06, 1.08), (s * 0.05, 0, 0.76), 1)          # lóbulos cefálicos
    p.elip((s * 0.21, 0.1, 0.56), (0.05, 0.05, 0.05), 5, 3, 4)
p.cuad((0.05, 0, -0.7), (0.01, 0.03, -2.2), (-0.01, 0.03, -2.2), (-0.05, 0, -0.7), 2)
partir(p, [[0.45, B, -B, B, -B, B], [-B, -0.45, -B, B, -B, B]], "manta")
print("TRIS", triangulos())
