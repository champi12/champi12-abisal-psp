"""Pez espejo (arrecife de cristal): pez hacha alto y plano, de escamas que reflejan como
vidrio, con una línea luminosa en el costado. ~0,45 m. Piezas: cola#1 (z < -0,18) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
PLATA = material("plata", 0xc8e8f4, emision=0.5)
LOMO = material("lomo", 0x5a7aa0)
LINEA = material("linea", 0x9ff4ff, emision=5)
OJO = material("ojo", 0x101820)
ALETA = material("aleta", 0xa8d8f0, emision=0.8)
p = PiezaJ("espejo", [PLATA, LOMO, LINEA, OJO, ALETA])
p.torno([(0, -0.2), (0.05, -0.15), (0.09, -0.04), (0.09, 0.06), (0.06, 0.15), (0, 0.21)], 8, [1, 0, 0, 0, 1],
        aplastar=(0.45, 1.9))
for s in (1, -1):
    p.cuad((s * 0.042, 0.0, 0.14), (s * 0.042, 0.02, 0.14), (s * 0.042, 0.02, -0.15), (s * 0.042, 0.0, -0.15), 2)
    p.elip((s * 0.03, 0.07, 0.13), (0.022, 0.03, 0.022), 5, 3, 3)
p.cuad((0, 0, -0.18), (0, 0.15, -0.34), (0, 0, -0.27), (0, -0.15, -0.34), 4, doble=True)
p.tri((0, 0.16, 0.05), (0, 0.25, -0.08), (0, 0.14, -0.12), 4, doble=True)
p.tri((0, -0.16, 0.02), (0, -0.24, -0.1), (0, -0.13, -0.12), 4, doble=True)
partir(p, [[-B, B, -B, B, -B, -0.18]], "espejo")
print("TRIS", triangulos())
