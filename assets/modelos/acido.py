"""Proyectil de ácido del escupidor, estilo bloque: gota verde luminosa con núcleo claro. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("acido", ppm=60)
p = m.pieza("acido", 1)
p.caja((0, 0, 0), (0.22, 0.22, 0.26), {'todo': E("brillo", 0x9fff40, 0x40c010)})
p.caja((0, 0, -0.14), (0.12, 0.12, 0.12), {'todo': E("brillo", 0x7fe030, 0x30a010)})
p.caja((0, 0, 0.02), (0.1, 0.1, 0.1), {'todo': E("brillo", 0xf0ffd0, 0xc0ff80)})
m.crear()
