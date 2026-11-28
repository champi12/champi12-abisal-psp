"""Parásitos, estilo bloque: dos chupadores rojos de patas finas. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("parasitos", ppm=60)
p = m.pieza("parasitos", 1)
for (x, z) in ((-0.3, 0.2), (0.0, -0.05)):
    p.caja((x, 0.06, z), (0.18, 0.1, 0.24), {'todo': E("placas", 0xc0303a, 0x80202a)})
    p.caja((x, 0.06, z + 0.14), (0.06, 0.06, 0.06), {'todo': E("liso", 0x40101a)})
    for s in (1, -1):
        for k in range(2):
            p.caja((x + s * 0.14, 0.0, z - 0.06 + k * 0.12), (0.12, 0.015, 0.015), {'todo': E("liso", 0x80202a)},
                   rot=(0, 0, s * -30))
m.crear()
