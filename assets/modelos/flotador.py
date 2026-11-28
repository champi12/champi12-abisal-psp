"""Flotador de cañón, estilo bloque: bolsa de gas a la deriva con aletas cortas y palpos. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("flotador", ppm=26)
p = m.pieza("flotador", 1)
GAS = {'todo': E("puntos", 0xd8c8a0, 0xa89870)}
p.caja((0, 0.1, 0), (1.1, 0.9, 1.0), GAS)
p.caja((0, 0.1, 0), (1.3, 0.6, 0.8), GAS)
p.caja((0, 0.1, 0), (0.8, 1.1, 0.8), GAS)
p.caja((0, 0.52, 0), (0.5, 0.06, 0.5), {'todo': E("brillo", 0x9fffd0, 0x40c090)})
for s in (1, -1):
    p.caja((s * 0.75, 0.0, 0), (0.25, 0.03, 0.4), {'todo': E("aleta", 0xc8b890, 0x8a7a5a)})
    p.caja((s * 0.2, 0.1, 0.5), (0.06, 0.06, 0.03), {'todo': E("ojo", 0xf0e0b0, 0x101010)})
for k in range(4):
    a = math.radians(k * 90 + 45)
    p.caja((math.sin(a) * 0.3, -0.6, math.cos(a) * 0.3), (0.03, 0.6, 0.03), {'todo': E("liso", 0xa89870)})
m.crear()
