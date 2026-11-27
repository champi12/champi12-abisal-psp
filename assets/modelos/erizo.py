"""Erizo globo: pez erizo verde oliva con púas oscuras y ojos ámbar; se hincha en el juego."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CUERPO = material("cuerpo", 0x8a9a4a)
BANDA = material("banda", 0xd0a040)
PUA = material("puas", 0x2a2a1e)
OJO = material("ojos", 0xffb030, emision=4)
ALETA = material("aletas", 0xc8783a)
p = PiezaJ("erizo#1", [CUERPO, BANDA, PUA, OJO, ALETA])
p.elip((0, 0, 0), (0.24, 0.22, 0.26), 10, 6, 0)
p.elip((0, 0.12, -0.02), (0.15, 0.08, 0.2), 8, 3, 1)
import random
rnd = random.Random(951)
for i in range(26):                    # púas cónicas
    a = i * 2.4
    el = (rnd.random() - 0.5) * 2.6
    d = (math.cos(a) * math.cos(el), math.sin(el), math.sin(a) * math.cos(el))
    if d[2] > 0.75:
        continue                       # la cara queda despejada
    b = (d[0] * 0.21, d[1] * 0.19, d[2] * 0.23)
    tip = (d[0] * 0.42, d[1] * 0.38, d[2] * 0.42)
    ux, uz = -d[2] * 0.035, d[0] * 0.035
    p.tri((b[0] - ux, b[1] - 0.025, b[2] - uz), (b[0] + ux, b[1] + 0.025, b[2] + uz), tip, 2)
    p.tri((b[0] + ux, b[1] - 0.025, b[2] + uz), (b[0] - ux, b[1] + 0.025, b[2] - uz), tip, 2)
for s in (1, -1):
    p.elip((s * 0.1, 0.06, 0.22), (0.045, 0.045, 0.03), 5, 3, 3)
    p.tri((s * 0.2, 0, 0.02), (s * 0.34, 0.06, -0.06), (s * 0.2, -0.05, -0.08), 4)
p.cuad((0, 0, -0.24), (0, 0.1, -0.38), (0, 0, -0.33), (0, -0.1, -0.38), 4)
p.crear()
print("TRIS", triangulos())
