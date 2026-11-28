"""Caja de suministros: contenedor estanco de la estación con bandas naranjas, asas,
cierre y luz de estado (se abre con [] y da materiales)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
METAL = material("metal", 0x8a9298)
TAPA = material("tapa", 0x6a7278)
NAR = material("naranja", 0xe8692a)
OSC = material("oscuro", 0x2a2e32)
LUZ = material("luz", 0x7aff6a, emision=5)
BLANCO = material("blanco", 0xe0e4e6)
p = PiezaJ("caja#1", [METAL, TAPA, NAR, OSC, LUZ, BLANCO])
p.cajaJ((0, 0.32, 0), (1.0, 0.64, 0.72), 0)
p.cajaJ((0, 0.68, 0), (1.04, 0.1, 0.76), 1)
for x in (-0.3, 0.3):
    p.cajaJ((x, 0.34, 0), (0.12, 0.66, 0.74), 2)
for s in (1, -1):
    p.cajaJ((s * 0.53, 0.45, 0), (0.06, 0.08, 0.28), 3)
    p.cajaJ((s * 0.4, 0.04, s * 0.3), (0.12, 0.08, 0.12), 3)
p.cajaJ((0, 0.6, 0.38), (0.16, 0.12, 0.04), 3)
p.cajaJ((0, 0.32, 0.365), (0.3, 0.18, 0.01), 5)                     # placa con el logo
p.cajaJ((0.38, 0.75, 0.26), (0.08, 0.06, 0.08), 4)
p.crear()
