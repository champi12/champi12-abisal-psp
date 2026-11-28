"""Aguijonero: dardo plateado violeta con pico afilado, ~0,7 m (una malla)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CUERPO = material("cuerpo", 0x9a8ab8)
LOMO = material("lomo", 0x6a5a8a)
VIENTRE = material("vientre", 0xd0d0e0)
OJO = material("ojos", 0xff4a8a, emision=5)
FRANJA = material("franja", 0xc060ff, emision=4)
p = PiezaJ("aguijonero#1", [CUERPO, LOMO, VIENTRE, OJO, FRANJA])
p.torno([(0, -0.3), (0.05, -0.2), (0.075, 0.05), (0.045, 0.2), (0.015, 0.45), (0, 0.6)], 6, [1, 0, 0, 2, 2], aplastar=(0.85, 1))
p.cuad((0, 0.03, -0.25), (0, 0.17, -0.43), (0, 0.04, -0.34), (0, 0, -0.3), 1)
p.cuad((0, -0.03, -0.25), (0, 0, -0.3), (0, -0.04, -0.34), (0, -0.17, -0.43), 1)
p.tri((0, 0.06, 0.05), (0, 0.13, -0.1), (0, 0.05, -0.15), 1)
for s in (1, -1):
    p.elip((s * 0.05, 0.03, 0.1), (0.018, 0.018, 0.018), 4, 2, 3)
p.cajaJ((0, 0.065, -0.05), (0.012, 0.015, 0.26), 4)
p.crear()
