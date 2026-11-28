"""Farolillo (fosa), estilo bloque: pez oscuro con órganos luminosos en los flancos.
Piezas: cola#1 (z < -0,14, se agita) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("farolillo", ppm=60)
OSC = {'todo': E("escamas", 0x1a2030, 0x2a3048)}
c = m.pieza("cola", 1)
c.caja((0, 0, -0.25), (0.015, 0.22, 0.14), {'todo': E("aleta", 0x2a3048, 0x4a5070)})
b = m.pieza("cuerpo", 2)
b.caja((0, 0.03, 0.02), (0.2, 0.26, 0.32), OSC)
b.caja((0, 0.0, -0.12), (0.12, 0.16, 0.06), OSC)
for s in (1, -1):
    for k in range(3):
        b.caja((s * 0.101, -0.05, 0.1 - k * 0.08), (0.004, 0.03, 0.03), {'todo': E("brillo", 0x9ff8ff, 0x3ac0e0)})
    b.caja((s * 0.101, 0.08, 0.13), (0.004, 0.05, 0.05), {'todo': E("ojo", 0xc0f0ff, 0x101010)})
b.caja((0, 0.2, 0.12), (0.015, 0.06, 0.015), {'todo': E("liso", 0x2a3048)})
b.caja((0, 0.24, 0.15), (0.04, 0.04, 0.04), {'todo': E("brillo", 0xffffff, 0x9ff8ff)})
m.crear()
