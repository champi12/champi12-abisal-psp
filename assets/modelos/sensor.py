"""Sensor sísmico (historia): trípode con cuerpo naranja y mástil, ~2 m.

Piezas: "cuerpo#1" (patas, cuerpo, mástil), "anillo#2" (anillo del hidrófono: gira
despacio mientras escucha) y "luz#3" (luz verde que destella).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
METAL = material("metal", 0x8a959c, metal=0.4)
NARANJA = material("naranja", 0xe8692a)
OSCURO = material("oscuro", 0x2a3038)
VERDE = material("verde", 0x6aff8a, emision=5.0)
FRANJA = material("franja", 0x5affa0, emision=4.0)

c = Pieza("cuerpo#1", [METAL, NARANJA, OSCURO, FRANJA])
for i in range(3):
    a = i * math.tau / 3
    c.caja((math.cos(a) * 0.38, math.sin(a) * 0.38, 0.42), (0.07, 0.07, 0.95), 0,
           rot=(math.sin(a) * 35, -math.cos(a) * 35, 0))
    c.caja((math.cos(a) * 0.72, math.sin(a) * 0.72, 0.03), (0.2, 0.2, 0.06), 2)    # pies
    c.caja((math.cos(a) * 0.72, math.sin(a) * 0.72, 0.09), (0.08, 0.08, 0.06), 3)  # luces en los pies
c.caja((0, 0, 0.97), (0.6, 0.42, 0.42), 1)                  # cuerpo naranja en bloque
c.caja((0, 0, 0.97), (0.42, 0.6, 0.42), 1)
c.caja((0, 0, 1.2), (0.44, 0.44, 0.06), 2)
c.caja((0, 0.31, 0.97), (0.22, 0.04, 0.14), 2)              # placa
c.caja((0, 0, 0.82), (0.62, 0.62, 0.06), 3)                 # franja luminosa
c.caja((0, 0, 1.65), (0.05, 0.05, 0.95), 2)                 # mástil
c.crear()

a = Pieza("anillo#2", [FRANJA, OSCURO])
for i in range(4):                                          # aro cuadrado del hidrófono
    ang = i * math.pi / 2
    a.caja((math.cos(ang) * 0.2, math.sin(ang) * 0.2, 0), (0.05, 0.4, 0.05), 0, rot=(0, 0, math.degrees(ang)))
for i in range(2):
    a.caja((0, 0, 0), (0.4, 0.025, 0.025), 1, rot=(0, 0, i * 90))
a.caja((0.2, 0, 0), (0.09, 0.09, 0.09), 1)
ob = a.crear()
ob.location = (0, 0, 1.55)

l = Pieza("luz#3", [VERDE])
l.caja((0, 0, 0), (0.22, 0.22, 0.22), 0)
ob = l.crear()
ob.location = (0, 0, 2.15)
print("TRIS", triangulos())
