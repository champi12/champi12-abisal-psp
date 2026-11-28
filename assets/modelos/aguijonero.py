"""Aguijonero, estilo bloque: dardo plateado violeta con pico afilado. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("aguijonero", ppm=60)
p = m.pieza("aguijonero", 1)
p.caja((0, 0, 0), (0.1, 0.16, 0.5), {'todo': E("escamas", 0xa890d0, 0x6a58a0)})
p.caja((0, 0, 0.4), (0.03, 0.03, 0.4), {'todo': E("liso", 0xe8e8f0)})
p.caja((0, 0, -0.32), (0.015, 0.22, 0.12), {'todo': E("aleta", 0xa890d0, 0xd0b8ff)})
for s in (1, -1):
    p.caja((s * 0.051, 0.03, 0.14), (0.004, 0.04, 0.04), {'todo': E("brillo", 0xff60c0, 0xa02070)})
m.crear()
