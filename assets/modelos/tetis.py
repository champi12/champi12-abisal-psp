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
c = Pieza("casco#1", [GRIS, GRIS2, OSCURO, NARANJA, VIDRIO, ROCA])
# cuerpo principal a lo largo de X (12 lados), con tapa redondeada en -X
c.cilindro((0, 0, R + 0.3), R, L, 12, 0, rot=(0, 90, 0), tapas=False)
c.esfera((-L / 2, 0, R + 0.3), (1.0, R, R), 12, 4, 0)
# interior oscuro visible por el extremo desgarrado
c.cilindro((L / 2 - 0.4, 0, R + 0.3), R - 0.1, 0.1, 12, 2, rot=(0, 90, 0))
# anillos estructurales y franja naranja
for x in (-2.6, -0.4, 1.8):
    c.cilindro((x, 0, R + 0.3), R + 0.08, 0.25, 12, 1, rot=(0, 90, 0))
c.cilindro((-1.5, 0, R + 0.3), R + 0.03, 0.4, 12, 3, rot=(0, 90, 0))
# borde desgarrado: dientes de chapa doblados hacia fuera
import random
rnd = random.Random(5)
for i in range(10):
    a = i * math.tau / 10
    y, z = math.cos(a) * R, math.sin(a) * R + R + 0.3
    lar = rnd.uniform(0.4, 1.1)
    c.caja((L / 2 + lar / 2 - 0.1, y, z), (lar, 0.5, 0.06), 0,
           rot=(math.degrees(a) + 90, rnd.uniform(-25, 10), rnd.uniform(-15, 15)))
# ojos de buey (ventanas) a los dos lados
for x in (-2.0, -0.9, 0.6, 2.4):
    for s in (1, -1):
        c.cilindro((x, s * (R + 0.02), R + 0.6), 0.3, 0.08, 8, 4, rot=(90, 0, 0))
# escotilla superior y patas de apoyo hundidas en la roca
c.cilindro((-0.3, 0, 2 * R + 0.35), 0.55, 0.25, 8, 1)
for x in (-2.8, 2.2):
    for s in (1, -1):
        c.caja((x, s * 1.1, 0.4), (0.2, 0.2, 1.1), 1, rot=(s * 20, 0, 0))
# escombros y rocas volcánicas alrededor
for i in range(6):
    a = i * 1.1
    c.roca((math.cos(a) * 4.2, math.sin(a) * 3.0, 0.0), (0.8, 0.6, 0.45), 60 + i, 0.3, 0, 5)
for i in range(4):
    c.caja((L / 2 + 1.2 + i * 0.5, (i - 1.5) * 0.9, 0.08), (0.5, 0.35, 0.05), 1, rot=(0, rnd.uniform(-20, 20), i * 40))
c.crear()

a = Pieza("antena#2", [GRIS2, VIDRIO])
a.cilindro((0, 0, 0.6), 0.06, 1.2, 6, 0)                 # mástil (el giro es sobre este eje)
a.cilindro((0.25, 0, 1.25), 0.5, 0.12, 10, 1, rot=(0, 70, 0), radio2=0.15)   # plato
a.caja((0.5, 0, 1.3), (0.4, 0.04, 0.04), 0)
ob = a.crear()
ob.location = (-1.2, 0, 2 * R + 0.3)

p = Pieza("paneles#3", [PANEL])
for (x, w) in ((-1.6, 1.3), (1.2, 0.8)):
    p.caja((x, -(R + 0.04), R + 0.05), (w, 0.04, 0.35), 0)
p.caja((0.2, R + 0.04, R + 0.05), (1.6, 0.04, 0.3), 0)
p.crear()

b = Pieza("baliza#4", [ROJA, GRIS2])
b.esfera((0, 0, 0.12), (0.14, 0.14, 0.14), 6, 4, 0)
b.cilindro((0, 0, 0), 0.12, 0.08, 6, 1)
ob = b.crear()
ob.location = (2.8, 0, 2 * R + 0.25)
print("TRIS", triangulos())
