"""Montón de arena removida que delata al gusano enterrado."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
A1 = material("arena", 0xd8c088)
A2 = material("arena2", 0xc8b070)
A3 = material("borde", 0xb8a268)
p = PiezaJ("monton#1", [A1, A2, A3])
p.torno([(1.7, 0.0), (1.5, 0.06), (1.25, 0.2), (0.9, 0.45), (0.45, 0.65), (0, 0.72)], 10, [2, 1, 0, 0, 1], eje='y')
p.ruido(0.05, 3)
p.crear()
