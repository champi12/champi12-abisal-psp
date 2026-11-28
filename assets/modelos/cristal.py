"""Cristal resonante (historia): un racimo de cristales violetas sobre roca negra, ~4 m.

Piezas: "roca#1" (pedestal de roca), "cristales#2" (laten con el Latido: el juego los
escala) y "astillas#3" (tres esquirlas que flotan y giran alrededor).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
from mathutils import Vector, Euler
import random


def prisma_b(p, pos, r, h, mat, rot=(0, 0, 0)):
    """block crystal: a square prism with a stepped tip, turned about its base"""
    x, y, z = pos
    vs = list(p.caja((x, y, z + h * 0.36), (r * 1.6, r * 1.6, h * 0.72), mat))
    vs += p.caja((x, y, z + h * 0.8), (r * 1.1, r * 1.1, h * 0.16), mat)
    vs += p.caja((x, y, z + h * 0.94), (r * 0.55, r * 0.55, h * 0.12), mat)
    R = Euler([math.radians(a) for a in rot]).to_matrix()
    o = Vector(pos)
    for v in vs:
        v.co = o + R @ (v.co - o)


def roca_b(p, pos, radios, sem, mat):
    """a pile of turned boxes (Blender coordinates, z up)"""
    rnd = random.Random(sem)
    x, y, z = pos
    rx, ry, rz = radios
    p.caja((x, y, z), (rx * 1.6, ry * 1.6, rz * 1.5), mat, rot=(0, 0, rnd.uniform(0, 90)))
    p.caja((x + rnd.uniform(-0.2, 0.2) * rx, y + rnd.uniform(-0.2, 0.2) * ry, z + rz * 0.8),
           (rx * 1.0, ry * 1.0, rz * 0.6), mat, rot=(0, 0, rnd.uniform(0, 90)))

limpiar()
ROCA = material("roca", 0x2e2a3a, rugosidad=1.0)
VETA = material("veta", 0x6a4aa0, emision=1.5)
C1 = material("cristal1", 0xc89cff, emision=2.5)
C2 = material("cristal2", 0x9a6af0, emision=1.8)
C3 = material("cristal3", 0x7a4ad8, emision=1.2)

r = Pieza("roca#1", [ROCA, VETA])
roca_b(r, (0, 0, 0.2), (1.9, 1.6, 0.75), 77, 0)
for i in range(5):
    a = i * 1.3
    roca_b(r, (math.cos(a) * 1.9, math.sin(a) * 1.6, 0.05), (0.6, 0.5, 0.35), 80 + i, 0)
for i in range(4):  # vetas luminosas en la roca
    a = i * 1.6 + 0.4
    r.caja((math.cos(a) * 1.3, math.sin(a) * 1.1, 0.95), (0.9, 0.12, 0.08), 1, rot=(0, -12, math.degrees(a) + 90))
r.crear()

c = Pieza("cristales#2", [C1, C2, C3])
prisma_b(c, (0, 0, 0.5), 0.42, 3.8, 0)                   # el central
import random
rnd = random.Random(9)
for i in range(8):
    a = i * math.tau / 8 + rnd.uniform(-0.2, 0.2)
    d = rnd.uniform(0.6, 1.1)
    h = rnd.uniform(1.2, 2.4)
    inc = rnd.uniform(20, 38)
    prisma_b(c, (math.cos(a) * d, math.sin(a) * d, 0.45), rnd.uniform(0.16, 0.3), h, 1 + i % 2,
             rot=(-math.sin(a) * inc, math.cos(a) * inc, rnd.uniform(0, 60)))
c.crear()

s = Pieza("astillas#3", [C1])
for i in range(3):
    a = i * math.tau / 3
    prisma_b(s, (math.cos(a) * 1.6, math.sin(a) * 1.6, 2.6 + i * 0.35), 0.1, 0.5, 0,
             rot=(rnd.uniform(-40, 40), rnd.uniform(-40, 40), 0))
s.crear()
print("TRIS", triangulos())
