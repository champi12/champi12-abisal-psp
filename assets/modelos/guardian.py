"""Guardián del Vacío, estilo bloque: cadena colosal de segmentos acorazados con cráneo plano y
líneas luminosas cian. Piezas: cráneo#1, segmentos#2-#7 (desde z = 0,06 hacia atrás, con aletas
en el primero), cola#8."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("guardian", ppm=26)
ARM = {'todo': E("placas", 0x3a4a5a, 0x2a3644), '-y': E("liso", 0x5a6a7a)}
CIAN = {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)}
c = m.pieza("craneo", 1)
c.caja((0, 0.1, 0.4), (1.36, 0.8, 1.6), ARM)
c.caja((0, 0.4, 0.5), (1.0, 0.3, 1.2), ARM)
c.caja((0, -0.25, 1.05), (1.1, 0.2, 0.5), {'todo': E("liso", 0x0a1018)})
for s in (1, -1):
    c.caja((s * 0.5, 0.3, 1.0), (0.2, 0.12, 0.1), CIAN)
c.caja((0, 0.56, 0.4), (0.06, 0.04, 1.2), CIAN)
for k in range(6):
    sg = m.pieza("segmento%d" % k, k + 2)
    w = 0.76 - k * 0.1
    hgt = 0.92 - k * 0.12
    sg.caja((0, 0.18 - k * 0.03, -0.4), (w, hgt, 0.9), ARM)
    sg.caja((0, 0.18 - k * 0.03 + hgt / 2, -0.4), (0.06, 0.04, 0.8), CIAN)
    if k == 0:
        for s in (1, -1):
            sg.caja((s * 1.3, 0.1, -0.4), (1.5, 0.08, 0.7), {'todo': E("aleta", 0x3a4a5a, 0x5a6a7a)})
            sg.caja((s * 1.3, 0.16, -0.4), (1.2, 0.02, 0.06), CIAN)
t = m.pieza("cola", 8)
t.caja((0, 0.1, -0.5), (0.04, 0.9, 1.0), {'todo': E("aleta", 0x3a4a5a, 0x6ff0ff)})
m.crear()
