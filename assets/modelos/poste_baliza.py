"""Baliza de señal plantada en el fondo: trípode, poste y luz verde."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
METAL = material("metal", 0x8a9298)
OSCURO = material("oscuro", 0x3a4046)
NAR = material("naranja", 0xe8692a)
LUZ = material("luz", 0x7aff7a, emision=6)
p = PiezaJ("baliza#1", [METAL, OSCURO, NAR, LUZ])
p.torno([(0.025, 0.05), (0.02, 1.2)], 6, 0, eje='y')
for i in range(3):
    a = i * math.tau / 3
    p.cajaJ((math.cos(a) * 0.12, 0.14, math.sin(a) * 0.12), (0.03, 0.03, 0.3), 1, rot=(-25, 0, -math.degrees(a) + 90))
    p.cajaJ((math.cos(a) * 0.22, 0.02, math.sin(a) * 0.22), (0.06, 0.04, 0.06), 1)
p.torno([(0.05, 0.95), (0.05, 1.07)], 8, 2, eje='y')
p.torno([(0.04, 1.18), (0.045, 1.28), (0, 1.33)], 8, 3, eje='y')
p.crear()
