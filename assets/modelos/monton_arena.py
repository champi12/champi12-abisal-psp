"""Montón de arena removida que delata al gusano, estilo bloque. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("monton_arena", ppm=14)
p = m.pieza("monton", 1)
AR = {'todo': E("puntos", 0xe0cc98, 0xb89c68)}
p.caja((0, 0.15, 0), (3.0, 0.3, 2.8), AR)
p.caja((0.1, 0.38, 0.05), (2.0, 0.2, 1.8), AR)
p.caja((-0.1, 0.55, 0), (1.0, 0.16, 0.9), AR)
m.crear()
