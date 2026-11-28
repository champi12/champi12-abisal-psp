"""Medusa rayo: campana cuadrada azul con cuatro tentáculos en zigzag crepitantes, ~1,6 m.
Piezas: tentáculos#1 (y < -0,02), campana#2. Translúcida."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
AZUL = material("azul", 0x5a8aff, emision=2)
AZUL2 = material("azul2", 0x7aa8ff, emision=2)
TAPA = material("tapa", 0xa0d0ff, emision=2)
NUCLEO = material("nucleo", 0xe0f4ff, emision=6)
RAYO = material("rayos", 0x8ae0ff, emision=5)
p = PiezaJ("medusarayo", [AZUL, AZUL2, TAPA, NUCLEO, RAYO])
s0, s1, h = 0.55, 0.25, 0.6
c = [(-1, -1), (1, -1), (1, 1), (-1, 1)]
for i in range(4):
    j = (i + 1) % 4
    m = (c[i][0] * (s0 + s1) / 2 * 1.08, h * 0.5, c[i][1] * (s0 + s1) / 2 * 1.08)
    # cada cara en dos tramos con un leve abombado
    p.cuad((c[i][0] * s0, 0, c[i][1] * s0), (c[j][0] * s0, 0, c[j][1] * s0),
           (c[j][0] * 0.42, h * 0.5, c[j][1] * 0.42), (c[i][0] * 0.42, h * 0.5, c[i][1] * 0.42), i % 2)
    p.cuad((c[i][0] * 0.42, h * 0.5, c[i][1] * 0.42), (c[j][0] * 0.42, h * 0.5, c[j][1] * 0.42),
           (c[j][0] * s1, h, c[j][1] * s1), (c[i][0] * s1, h, c[i][1] * s1), (i + 1) % 2)
    p.elip((c[i][0] * s0, 0.01, c[i][1] * s0), (0.05, 0.03, 0.05), 4, 2, 4)
p.cuad((-s1, h, -s1), (s1, h, -s1), (s1, h, s1), (-s1, h, s1), 2)
p.elip((0, 0.3, 0), (0.15, 0.12, 0.15), 6, 3, 3)
for i in range(4):
    x, z = c[i][0] * s0 * 0.9, c[i][1] * s0 * 0.9
    y = -0.03
    for k in range(5):
        dx = 0.12 if k % 2 == 0 else -0.12
        p.cuad((x - 0.025, y, z), (x + 0.025, y, z), (x + dx + 0.02, y - 0.3, z), (x + dx - 0.02, y - 0.3, z), 4)
        p.cuad((x, y, z - 0.025), (x, y, z + 0.025), (x + dx, y - 0.3, z + 0.02), (x + dx, y - 0.3, z - 0.02), 4)
        x += dx; y -= 0.3
partir(p, [[-99, 99, -99, -0.02, -99, 99]], "medusarayo")
