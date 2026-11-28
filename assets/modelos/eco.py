"""Eco de datos de la tripulación: grabadora robusta con banda violeta luminosa y antena."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
CUERPO = material("cuerpo", 0x8a9298)
OSC = material("oscuro", 0x3a4046)
BANDA = material("banda", 0xc080ff, emision=5)
PUNTA = material("punta", 0xffffff, emision=6)
p = PiezaJ("eco#1", [CUERPO, OSC, BANDA, PUNTA])
p.torno([(0, 0), (0.22, 0.02), (0.22, 0.2)], 10, [1, 0], eje='y')
p.torno([(0.225, 0.2), (0.225, 0.26)], 10, 2, eje='y')
p.torno([(0.22, 0.26), (0.22, 0.4), (0.16, 0.48), (0, 0.5)], 10, [0, 0, 1], eje='y')
for i in range(4):                                                   # tornillos
    a = i * math.tau / 4 + 0.4
    p.elip((math.cos(a) * 0.2, 0.44, math.sin(a) * 0.2), (0.025, 0.02, 0.025), 4, 2, 1)
p.tubo([(0.08, 0.48, 0, 0.012), (0.08, 0.98, 0, 0.008)], 4, 1, False)
p.elip((0.08, 1.0, 0), (0.035, 0.035, 0.035), 4, 2, 3)
p.crear()
