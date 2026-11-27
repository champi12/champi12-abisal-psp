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
    c.cilindro((math.cos(a) * 0.38, math.sin(a) * 0.38, 0.42), 0.035, 0.95, 5, 0,
               rot=(math.sin(a) * 35, -math.cos(a) * 35, 0))
    c.caja((math.cos(a) * 0.72, math.sin(a) * 0.72, 0.03), (0.18, 0.18, 0.05), 2)   # pies
c.cilindro((0, 0, 0.95), 0.3, 0.4, 10, 1)
c.cilindro((0, 0, 1.17), 0.24, 0.06, 10, 2)
c.caja((0, 0.31, 0.95), (0.2, 0.04, 0.12), 2)     # placa
c.cilindro((0, 0, 0.88), 0.31, 0.08, 10, 3, tapas=False)   # franja luminosa
for i in range(3):  # luces en los pies
    a = i * math.tau / 3
    c.esfera((math.cos(a) * 0.72, math.sin(a) * 0.72, 0.08), (0.05, 0.05, 0.05), 5, 3, 3)
c.cilindro((0, 0, 1.65), 0.025, 0.95, 5, 2)       # mástil
c.crear()

a = Pieza("anillo#2", [FRANJA, OSCURO])
a.cilindro((0, 0, 0), 0.22, 0.05, 10, 0, tapas=False)
for i in range(3):
    ang = i * math.tau / 3
    a.caja((math.cos(ang) * 0.11, math.sin(ang) * 0.11, 0), (0.22, 0.02, 0.02), 1, rot=(0, 0, math.degrees(ang)))
a.esfera((0.22, 0, 0), (0.05, 0.05, 0.05), 6, 3, 1)
ob = a.crear()
ob.location = (0, 0, 1.55)

l = Pieza("luz#3", [VERDE])
l.esfera((0, 0, 0), (0.14, 0.14, 0.14), 6, 4, 0)
ob = l.crear()
ob.location = (0, 0, 2.15)
print("TRIS", triangulos())
