"""Coloso de Ceniza: manta abisal colosal de alas de ceniza (~60 m) con tres señuelos
luminosos colgando bajo la boca. Piezas: ala derecha#1 (x > 8), ala izquierda#2 (x < -8),
cola#3 (|x| < 3, z < -15), señuelos#4 (|x| < 3, y < -3, z > 13), cuerpo#5."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CENIZA = material("ceniza", 0x6e6a70)
CENIZA2 = material("ceniza2", 0x5c585f)
VIENTRE = material("vientre", 0x8c8890)
BORDE = material("borde", 0x4a464e)
BOCA = material("boca", 0x1a1418)
SENUELO = material("senuelos", 0x7affd6, emision=5)
BULBO = material("bulbos", 0xd8fff2, emision=7)
OJO = material("ojos", 0xff5a3a, emision=6)
VENA = material("venas", 0xff7a4a, emision=2)
p = PiezaJ("coloso", [CENIZA, CENIZA2, VIENTRE, BORDE, BOCA, SENUELO, BULBO, OJO, VENA])
n, env = 10, 30
for s in (-1, 1):
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        x0, x1 = s * env * t0, s * env * t1
        z0f, z1f = 14 * (1 - t0 * t0) - 4 * t0, 14 * (1 - t1 * t1) - 4 * t1
        z0b, z1b = -10 * (1 - t0) + 2 * t0 - (math.sin(t0 * 9) * 1.2 if t0 > 0.5 else 0), -10 * (1 - t1) + 2 * t1 - (math.sin(t1 * 9) * 1.2 if t1 > 0.5 else 0)
        y0, y1 = math.sin(t0 * 2.6) * 3.5, math.sin(t1 * 2.6) * 3.5
        g0, g1 = 2.4 * (1 - t0) + 0.15, 2.4 * (1 - t1) + 0.15
        m = i % 2
        if s > 0:
            p.cuad((x0, y0 + g0, z0b), (x1, y1 + g1, z1b), (x1, y1 + g1, z1f), (x0, y0 + g0, z0f), m)
            p.cuad((x0, y0 - g0 * 0.5, z0b), (x0, y0 - g0 * 0.5, z0f), (x1, y1 - g1 * 0.5, z1f), (x1, y1 - g1 * 0.5, z1b), 2)
            p.cuad((x0, y0 - g0 * 0.5, z0f), (x0, y0 + g0, z0f), (x1, y1 + g1, z1f), (x1, y1 - g1 * 0.5, z1f), 3)
            p.cuad((x0, y0 + g0, z0b), (x0, y0 - g0 * 0.5, z0b), (x1, y1 - g1 * 0.5, z1b), (x1, y1 + g1, z1b), 3)
        else:
            p.cuad((x0, y0 + g0, z0b), (x0, y0 + g0, z0f), (x1, y1 + g1, z1f), (x1, y1 + g1, z1b), m)
            p.cuad((x0, y0 - g0 * 0.5, z0b), (x1, y1 - g1 * 0.5, z1b), (x1, y1 - g1 * 0.5, z1f), (x0, y0 - g0 * 0.5, z0f), 2)
            p.cuad((x0, y0 - g0 * 0.5, z0f), (x1, y1 - g1 * 0.5, z1f), (x1, y1 + g1, z1f), (x0, y0 + g0, z0f), 3)
            p.cuad((x0, y0 + g0, z0b), (x1, y1 + g1, z1b), (x1, y1 - g1 * 0.5, z1b), (x0, y0 - g0 * 0.5, z0b), 3)
        if i in (2, 4, 6):              # venas de brasa en la cara superior del ala
            xm, zm, ym, gm = (x0 + x1) / 2, (z0f + z0b) / 2, (y0 + y1) / 2, (g0 + g1) / 2
            p.cajaJ((xm, ym + gm + 0.05, zm), (abs(x1 - x0) * 0.9, 0.1, 0.5), 8)
p.elip((0, 0.6, 12.5), (5.2, 2.6, 4.4), 10, 5, 1)            # cabeza
for s in (-1, 1):                                             # lóbulos cefálicos
    p.cuad((s * 3.6, 0.5, 15.5), (s * 5.2, 0.2, 19.5), (s * 4.2, -0.4, 19.8), (s * 3.0, -0.2, 16.0), 3)
p.cuad((-3.5, -0.6, 16.6), (3.5, -0.6, 16.6), (3.2, 0.2, 16.9), (-3.2, 0.2, 16.9), 4)   # boca
for s in (-1, 1):
    p.elip((s * 3.8, 1.6, 15), (0.8, 0.5, 0.8), 6, 3, 7)
# cola de látigo con púas
for k in range(4):
    z0, z1 = -9 - k * 7.75, -9 - (k + 1) * 7.75
    w0, w1 = 1.2 * (1 - k * 0.24), 1.2 * (1 - (k + 1) * 0.24) + 0.05
    p.cuad((-w0, 0, z0), (w0, 0, z0), (w1, 0.15, z1), (-w1, 0.15, z1), 3)
    p.cuad((-w0, 0, z0), (-w1, 0.15, z1), (w1, 0.15, z1), (w0, 0, z0), 3)
    p.tri((0, 0.1, z0 - 2), (0, 1.2 * (1 - k * 0.2), z0 - 3.5), (0, 0.1, z0 - 5), 3)
# tres señuelos colgando bajo la boca
for i in (-1, 0, 1):
    x = i * 2.2
    p.cuad((x - 0.12, -1.5, 15), (x + 0.12, -1.5, 15), (x + 0.1, -9 + abs(i) * 2, 16), (x - 0.1, -9 + abs(i) * 2, 16), 5)
    p.elip((x, -9.4 + abs(i) * 2, 16), (0.7, 0.7, 0.7), 6, 4, 6)
partir(p, [[8, 99, -99, 99, -99, 99], [-99, -8, -99, 99, -99, 99], [-3, 3, -99, 99, -99, -15], [-3, 3, -99, -3, 13, 99]], "coloso")
