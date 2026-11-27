"""Cristal resonante (historia): un racimo de cristales violetas sobre roca negra, ~4 m.

Piezas: "roca#1" (pedestal de roca), "cristales#2" (laten con el Latido: el juego los
escala) y "astillas#3" (tres esquirlas que flotan y giran alrededor).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
ROCA = material("roca", 0x2e2a3a, rugosidad=1.0)
VETA = material("veta", 0x6a4aa0, emision=1.5)
C1 = material("cristal1", 0xc89cff, emision=2.5)
C2 = material("cristal2", 0x9a6af0, emision=1.8)
C3 = material("cristal3", 0x7a4ad8, emision=1.2)

r = Pieza("roca#1", [ROCA, VETA])
r.roca((0, 0, 0.2), (1.9, 1.6, 0.75), 77, 0.28, 1, 0)
for i in range(5):
    a = i * 1.3
    r.roca((math.cos(a) * 1.9, math.sin(a) * 1.6, 0.05), (0.6, 0.5, 0.35), 80 + i, 0.3, 0, 0)
for i in range(4):  # vetas luminosas en la roca
    a = i * 1.6 + 0.4
    r.caja((math.cos(a) * 1.2, math.sin(a) * 1.0, 0.65), (0.9, 0.08, 0.05), 1, rot=(0, -12, math.degrees(a) + 90))
r.crear()

c = Pieza("cristales#2", [C1, C2, C3])
c.prisma((0, 0, 0.5), 0.42, 3.8, 6, 0.3, 0)                   # el central
import random
rnd = random.Random(9)
for i in range(8):
    a = i * math.tau / 8 + rnd.uniform(-0.2, 0.2)
    d = rnd.uniform(0.6, 1.1)
    h = rnd.uniform(1.2, 2.4)
    inc = rnd.uniform(20, 38)
    c.prisma((math.cos(a) * d, math.sin(a) * d, 0.45), rnd.uniform(0.16, 0.3), h, 6, 0.35, 1 + i % 2,
             rot=(-math.sin(a) * inc, math.cos(a) * inc, rnd.uniform(0, 60)))
c.crear()

s = Pieza("astillas#3", [C1])
for i in range(3):
    a = i * math.tau / 3
    s.prisma((math.cos(a) * 1.6, math.sin(a) * 1.6, 2.6 + i * 0.35), 0.1, 0.5, 4, 0.5, 0,
             rot=(rnd.uniform(-40, 40), rnd.uniform(-40, 40), 0))
s.crear()
print("TRIS", triangulos())
