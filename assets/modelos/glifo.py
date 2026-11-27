"""Piedra de glifo (historia): un monolito tallado por los antiguos, ~3,5 m.

Piezas: "piedra#1" (monolito con glifos incrustados y su base) y "fragmento#2" (el
fragmento de la llave: flota sobre la piedra, gira y sube y baja; desaparece al cogerlo).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
PIEDRA = material("piedra", 0x4a4458, rugosidad=0.9)
PIEDRA2 = material("piedra2", 0x3a3448, rugosidad=1.0)
GLIFO = material("glifo", 0xb07aff, emision=3.0)
FRAG = material("fragmento", 0xe0c0ff, emision=5.0)

p = Pieza("piedra#1", [PIEDRA, PIEDRA2, GLIFO])
# monolito: prisma de 6 lados, algo más estrecho arriba y con la cima rota en bisel
v = p.cilindro((0, 0, 1.5), 0.65, 3.0, 6, 0, radio2=0.5)
for vv in v:
    if vv.co.z > 2.9:
        vv.co.z += vv.co.x * 0.35
p.ruido(0.04, 3)
p.roca((0, 0, 0.05), (1.2, 1.0, 0.35), 5, 0.25, 1, 1)
for i in range(3):
    a = i * 2.1 + 0.5
    p.roca((math.cos(a) * 1.3, math.sin(a) * 1.1, 0.0), (0.4, 0.35, 0.25), 20 + i, 0.3, 0, 1)
# glifos en la cara -Y: un ojo, una espiral angular y tres marcas
y = -0.57
p.caja((0, y, 2.35), (0.5, 0.04, 0.07), 2)
p.caja((0, y, 2.15), (0.5, 0.04, 0.07), 2)
p.caja((-0.22, y, 2.25), (0.07, 0.04, 0.25), 2)
p.caja((0.22, y, 2.25), (0.07, 0.04, 0.25), 2)
p.caja((0, y, 2.25), (0.1, 0.04, 0.1), 2)
p.caja((0, y, 1.65), (0.07, 0.04, 0.6), 2)
p.caja((0.12, y, 1.38), (0.3, 0.04, 0.07), 2)
p.caja((-0.12, y, 1.92), (0.3, 0.04, 0.07), 2)
for i in range(3):
    p.caja((-0.2 + i * 0.2, y, 0.95), (0.07, 0.04, 0.18), 2)
# los otros lados: líneas de glifos más tenues
for ang in (60, 180, 300):
    a = math.radians(ang)
    p.caja((math.sin(a) * 0.55, -math.cos(a) * 0.55, 1.9), (0.4, 0.04, 0.06), 2, rot=(0, 0, ang))
p.crear()

f = Pieza("fragmento#2", [FRAG, GLIFO])
f.cilindro((0, 0, 0.18), 0.22, 0.36, 4, 0, radio2=0.0)          # octaedro: dos pirámides
f.cilindro((0, 0, -0.18), 0.0, 0.36, 4, 0, radio2=0.22)
f.cilindro((0, 0, 0), 0.32, 0.03, 8, 1, tapas=False)             # aro de luz
ob = f.crear()
ob.location = (0, 0, 3.7)
print("TRIS", triangulos())
