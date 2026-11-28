"""Acechador de algas (bosque), estilo bloque: calamar emboscador de manto oliva moteado, volantes
y seis brazos. Piezas: brazos#1-#6 (cada uno desde el anillo z = -0,55, en hexágono) y manto#7."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("calamar", ppm=30)
OLI = {'todo': E("puntos", 0x6a7a3a, 0x4a5a2a), '-y': E("liso", 0xb0b880)}
ANG = (0, 60, 120, 180, 240, 300)
for k in range(6):
    a = math.radians(ANG[k])
    x, y = math.cos(a) * 0.24, math.sin(a) * 0.24
    b = m.pieza("brazo", k + 1)
    for j in range(3):
        z = -0.7 - j * 0.36
        w = 0.12 - j * 0.025
        b.caja((x * (1 + j * 0.15), y * (1 + j * 0.15), z), (w, w, 0.38), OLI)
    b.caja((x * 1.4, y * 1.4, -1.66), (0.04, 0.04, 0.16), {'todo': E("brillo", 0xd0ff80, 0x80b040)})
c = m.pieza("manto", 7)
c.caja((0, 0, 0.25), (0.7, 0.66, 1.1), OLI)
c.caja((0, 0, 0.85), (0.4, 0.4, 0.24), OLI)
c.caja((0, 0, -0.45), (0.56, 0.56, 0.3), OLI)
for s in (1, -1):
    c.caja((s * 0.46, 0, 0.35), (0.24, 0.04, 0.8), {'todo': E("aleta", 0x8a9a4a, 0x6a7a3a)})     # volantes
    c.caja((s * 0.36, 0.1, -0.35), (0.04, 0.12, 0.12), {'todo': E("ojo", 0xffe060, 0x101010)})
c.caja((0, 0.34, 0.3), (0.04, 0.1, 0.7), {'todo': E("aleta", 0x8a9a4a, 0x6a7a3a)})
m.crear()
