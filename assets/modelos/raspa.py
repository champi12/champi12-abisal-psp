"""Raspa (osario): pez carroñero alargado y pálido; la mitad trasera es casi solo espinas y
costillas a la vista, como si se estuviera comiendo a sí mismo. ~0,6 m. Piezas: cola#1
(z < -0,18) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
PIEL = material("piel", 0xa89c88)
HUESO = material("hueso", 0xeee4cc)
OJO = material("ojo", 0xff6a30, emision=4)
BOCA = material("boca", 0x3a2a24)
p = PiezaJ("raspa", [PIEL, HUESO, OJO, BOCA])
p.torno([(0, -0.12), (0.06, -0.08), (0.1, 0.05), (0.09, 0.2), (0.05, 0.3), (0, 0.34)], 8, 0, aplastar=(0.85, 1.0))
p.cuad((-0.05, -0.03, 0.3), (0.05, -0.03, 0.3), (0.03, -0.05, 0.36), (-0.03, -0.05, 0.36), 3)
for s in (1, -1):
    p.elip((s * 0.06, 0.04, 0.24), (0.025, 0.025, 0.025), 5, 3, 2)
# la espina y las costillas de la mitad trasera
p.tubo([(0, 0.02, -0.08, 0.022), (0, 0.02, -0.3, 0.016), (0, 0.04, -0.42, 0.01)], 4, 1)
for k in range(5):
    z = -0.1 - k * 0.055
    r = 0.08 * (1 - k * 0.15)
    for s in (1, -1):
        p.tubo([(0, 0.02, z, 0.008), (s * r, -0.01, z - 0.01, 0.008), (s * r * 0.5, -0.06, z - 0.02, 0.006)], 3, 1, tapas=False)
p.cuad((0, 0.04, -0.4), (0, 0.13, -0.52), (0, 0.04, -0.47), (0, -0.06, -0.52), 1, doble=True)
p.tri((0, 0.1, 0.18), (0, 0.17, 0.02), (0, 0.08, -0.04), 0, doble=True)
partir(p, [[-B, B, -B, B, -B, -0.18]], "raspa")
print("TRIS", triangulos())
