"""Módulo de investigación Tetis (historia): un módulo cilíndrico roto, ~8 m, en las grietas.

Piezas: "casco#1" (cilindro con ventanas, extremo desgarrado, patas y restos),
"antena#2" (mástil con plato de radar: gira a tirones, averiado),
"paneles#3" (paneles luminosos que parpadean) y "baliza#4" (luz roja que destella).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
GRIS = material("casco", 0xa8b0b6, rugosidad=0.5, metal=0.3)
GRIS2 = material("anillo", 0x6e7880, rugosidad=0.5, metal=0.3)
OSCURO = material("interior", 0x1c2228)
NARANJA = material("franja", 0xe86a2a)
VIDRIO = material("ventana", 0x3a6a80)
ROCA = material("roca", 0x2c2626, rugosidad=1.0)
PANEL = material("panel", 0x5ff0e0, emision=4.0)
ROJA = material("roja", 0xff4a30, emision=5.0)

R, L = 1.9, 7.6
ZC = R + 0.3
c = Pieza("casco#1", [GRIS, GRIS2, OSCURO, NARANJA, VIDRIO, ROCA])


def octo(x, largo, r, mat):
    """a stepped octagon along X (two crossed slabs), block style"""
    c.caja((x, 0, ZC), (largo, 2 * r, 1.3 * r), mat)
    c.caja((x, 0, ZC), (largo, 1.3 * r, 2 * r), mat)


octo(0, L, R, 0)                                         # cuerpo principal a lo largo de X
octo(-L / 2 - 0.3, 0.6, R * 0.78, 0)                     # tapa en -X, escalonada
octo(-L / 2 - 0.7, 0.3, R * 0.5, 1)
c.caja((L / 2 - 0.03, 0, ZC), (0.04, 1.25 * R, 1.9 * R), 2)   # interior oscuro en el extremo roto
c.caja((L / 2 - 0.03, 0, ZC), (0.04, 1.9 * R, 1.25 * R), 2)
for x in (-2.6, -0.4, 1.8):                              # anillos estructurales
    octo(x, 0.25, R + 0.08, 1)
octo(-1.5, 0.4, R + 0.03, 3)                             # franja naranja
import random
rnd = random.Random(5)
for i in range(10):                                      # dientes de chapa del borde roto
    a = i * math.tau / 10
    y, z = math.cos(a) * R, math.sin(a) * R + ZC
    lar = rnd.uniform(0.4, 1.1)
    c.caja((L / 2 + lar / 2 - 0.1, y, z), (lar, 0.5, 0.08), 0,
           rot=(math.degrees(a) + 90, rnd.uniform(-25, 10), rnd.uniform(-15, 15)))
for x in (-2.0, -0.9, 0.6, 2.4):                         # ventanas cuadradas a los dos lados
    for s in (1, -1):
        c.caja((x, s * (R + 0.02), R + 0.6), (0.5, 0.08, 0.5), 4)
        c.caja((x, s * (R + 0.01), R + 0.6), (0.66, 0.06, 0.66), 1)
c.caja((-0.3, 0, 2 * R + 0.42), (1.0, 1.0, 0.25), 1)    # escotilla superior
c.caja((-0.3, 0, 2 * R + 0.58), (0.6, 0.6, 0.1), 0)
for x in (-2.8, 2.2):                                    # patas hundidas en la roca
    for s in (1, -1):
        c.caja((x, s * 1.1, 0.4), (0.22, 0.22, 1.1), 1, rot=(s * 20, 0, 0))
for i in range(6):                                       # rocas volcánicas en bloque
    a = i * 1.1
    c.caja((math.cos(a) * 4.2, math.sin(a) * 3.0, 0.15), (1.3, 1.0, 0.7), 5, rot=(0, 0, i * 37))
    c.caja((math.cos(a) * 4.2 + 0.2, math.sin(a) * 3.0, 0.55), (0.7, 0.6, 0.4), 5, rot=(0, 0, i * 37 + 20))
for i in range(4):
    c.caja((L / 2 + 1.2 + i * 0.5, (i - 1.5) * 0.9, 0.08), (0.5, 0.35, 0.05), 1, rot=(0, rnd.uniform(-20, 20), i * 40))
c.crear()

a = Pieza("antena#2", [GRIS2, VIDRIO])
a.caja((0, 0, 0.6), (0.12, 0.12, 1.2), 0)               # mástil (el giro es sobre este eje)
a.caja((0.25, 0, 1.25), (0.12, 0.9, 0.9), 1, rot=(0, -20, 0))   # plato
a.caja((0.35, 0, 1.25), (0.1, 0.5, 0.5), 0, rot=(0, -20, 0))
a.caja((0.5, 0, 1.3), (0.4, 0.05, 0.05), 0)
ob = a.crear()
ob.location = (-1.2, 0, 2 * R + 0.3)

p = Pieza("paneles#3", [PANEL])
for (x, w) in ((-1.6, 1.3), (1.2, 0.8)):
    p.caja((x, -(R + 0.06), R + 0.05), (w, 0.04, 0.35), 0)
p.caja((0.2, R + 0.06, R + 0.05), (1.6, 0.04, 0.3), 0)
p.crear()

b = Pieza("baliza#4", [ROJA, GRIS2])
b.caja((0, 0, 0.14), (0.24, 0.24, 0.22), 0)
b.caja((0, 0, 0), (0.26, 0.26, 0.08), 1)
ob = b.crear()
ob.location = (2.8, 0, 2 * R + 0.25)
print("TRIS", triangulos())
