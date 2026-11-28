"""Rocas y minerales del mundo (con textura de roca al cargarse). Piezas:
1-2 roca, 3-4 veta de cobre, 5 cuarzo, 6-8 cresta de arrecife, 9-11 arco de piedra,
12-13 pilar de cañón, 14-15 basalto, 16-17 fumarola, 18 ópalo, 19 cristal térmico.
Todas con la base en y = 0 y las mismas medidas que los modelos hechos por código."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
GRIS = [material("gris1", 0x7d7a80), material("gris2", 0x75727a), material("gris3", 0x86838a)]
PARDO = [material("pardo1", 0x8a8478), material("pardo2", 0x7e786e), material("pardo3", 0x6e6a64)]
COBRE = material("cobre", 0xd27a3c, metal=0.5)
CUARZO = material("cuarzo", 0xf2d6e8, emision=2)
CANON = [material("canon1", 0x3e3c4e), material("canon2", 0x4a485e), material("canon3", 0x55536a)]
VETA = material("veta", 0x45e6ff, emision=4)
BASALTO = [material("bas1", 0x3a3436), material("bas2", 0x4a3a36), material("bas3", 0x5a4a44)]
FUMA = [material("fum1", 0x2c2628), material("fum2", 0x362e2e), material("fum3", 0x40302a), material("fum4", 0x4a3226), material("fum5", 0x6a2a14)]
LAVA = material("lava", 0xff7a1e, emision=5)
LAVA2 = material("lava2", 0xffc060, emision=6)
OPALO = [material("opalo1", 0x5ae8f0, emision=4), material("opalo2", 0xb07aff, emision=4)]
TERMICO = [material("term1", 0xff9a30, emision=4), material("term2", 0xffc060, emision=4)]
NEGRA = material("negra", 0x2c2626)
MATS = GRIS + PARDO + [COBRE, CUARZO] + CANON + [VETA] + BASALTO + FUMA + [LAVA, LAVA2] + OPALO + TERMICO + [NEGRA]
I = {id(m): i for i, m in enumerate(MATS)}
ix = lambda m: I[id(m)]


def pieza(nombre, num):
    return PiezaJ(f"{nombre}#{num}", MATS)


def cristal(p, base, r, h, inclina, rumbo, mat):
    k = p.marca()
    p.torno([(r * 0.9, 0), (r, h * 0.7), (0, h)], 6, mat, eje='y', centro=base)
    p.girar(k, base, inclina, rumbo)


# 1-2 roca (1,6 x 1,1 x 1,4, centro a 0,4 m)
for n, sem in ((1, 21), (2, 33)):
    p = pieza("roca", n)
    p.rocaJ((0, 0.4, 0), (1.6, 1.1, 1.4), sem, 0.22, 2, ix(GRIS[n % 3]))
    rnd = random.Random(sem)
    for k in range(3):                       # lajas y piedras sueltas al pie
        a = rnd.uniform(0, 6.28)
        p.rocaJ((math.cos(a) * 1.5, 0.0, math.sin(a) * 1.3), (0.35, 0.2, 0.3), sem * 3 + k, 0.3, 0, ix(GRIS[k % 3]))
    p.crear()
# 3-4 vetas de cobre, 5 cuarzo
for n, sem, cuarzo in ((3, 3, False), (4, 5, False), (5, 8, True)):
    p = pieza("veta", n)
    p.rocaJ((0, 0.35, 0), (1.0, 0.8, 0.9), sem, 0.22, 2, ix(GRIS[sem % 3]))
    for i in range(6):
        a = i * 61 + sem * 13
        e = 25 + (i * 37 % 40)
        if cuarzo:
            cristal(p, (0, 0.35, 0), 0.13 + (i % 3) * 0.04, 1.2 + (i % 2) * 0.4, e, a, ix(CUARZO))
        else:
            ra, re = math.radians(a), math.radians(90 - e)
            d = (math.cos(re) * math.sin(ra), math.sin(re), math.cos(re) * math.cos(ra))
            p.rocaJ((d[0] * 0.78, 0.35 + d[1] * 0.7, d[2] * 0.78), (0.3, 0.16, 0.26), sem * 7 + i, 0.3, 0, ix(COBRE))
    p.crear()
# 6-8 crestas de arrecife (10 m a lo largo de x)
for n, sem in ((6, 1), (7, 9), (8, 15)):
    p = pieza("cresta", n)
    rnd = random.Random(sem)
    for i in range(6):
        x = -5 + i * 2 + rnd.uniform(-0.4, 0.4)
        s = 1.0 + rnd.random() * 0.9
        if i in (0, 5):
            s *= 0.7
        p.rocaJ((x, s * 0.25, rnd.uniform(-0.6, 0.6)), (s * 1.2, s * 0.9, s), sem * 13 + i, 0.25, 1, ix(PARDO[i % 3]))
    p.crear()
# 9-11 arcos de piedra (8 m de ancho, 4,6 m de alto, sobre el eje x)
for n, sem in ((9, 1), (10, 4), (11, 7)):
    p = pieza("arco", n)
    rnd = random.Random(sem)
    pts = [(-4.2, -1.5, 0, 1.5)]
    for k in range(13):
        t = k / 12
        a = math.pi * (1 - t)
        g = (1.35 - 0.45 * math.sin(t * math.pi)) * (0.9 + rnd.random() * 0.25)
        pts.append((math.cos(a) * 4.0, math.sin(a) * 4.6, rnd.uniform(-0.15, 0.15), g))
    pts.append((4.2, -1.5, 0, 1.5))
    p.tubo(pts, 8, [ix(PARDO[k % 3]) for k in range(len(pts))], True, 0.12, sem)
    for i in range(4):
        lado = -1 if i < 2 else 1
        p.rocaJ((lado * (4.2 + rnd.random() * 1.5), 0.1, rnd.uniform(-1.5, 1.5)),
                (0.9 + rnd.random() * 0.6, 0.6, 0.8), sem * 5 + i, 0.25, 1, ix(PARDO[i % 3]))
    p.crear()
# 12-13 pilares de cañón (~20 m) con vetas cian
for n, sem in ((12, 5), (13, 12)):
    p = pieza("pilar", n)
    rnd = random.Random(sem)
    pts = []
    for k in range(7):
        y = 20 * k / 6 if k else -1.5
        pts.append((rnd.uniform(-0.4, 0.4), y, rnd.uniform(-0.4, 0.4), 3.4 - k * 0.3 + rnd.random() * 0.6))
    p.tubo(pts, 9, [ix(CANON[k % 3]) for k in range(7)], True, 0.1, sem)
    p.rocaJ((0, 20, 0), (1.9, 1.2, 1.7), sem, 0.25, 1, ix(CANON[0]))
    for i in range(4):
        a = math.radians(i * 90 + 20)
        y = 2 + i * 3.5
        r = 3.45 - y * 0.06
        c, s = math.cos(a), math.sin(a)
        p.cuad((c * r - s * 0.35, y, s * r + c * 0.35), (c * r + s * 0.35, y, s * r - c * 0.35),
               (c * (r - 0.15) + s * 0.75, y + 6, s * (r - 0.15) - c * 0.75), (c * (r - 0.15) + s * 0.05, y + 6, s * (r - 0.15) - c * 0.05), ix(VETA), doble=True)
    p.crear()
# 14-15 basalto: columnas hexagonales
for n, sem in ((14, 2), (15, 7)):
    p = pieza("basalto", n)
    rnd = random.Random(sem)
    for i in range(7):
        x, z = (0, 0) if i == 0 else (math.cos((i - 1) * 1.0472) * 0.95, math.sin((i - 1) * 1.0472) * 0.95)
        h = 1.5 + rnd.random() * 3.5
        p.torno([(0.5, -0.5), (0.5, h - 0.08), (0.44, h), (0, h + 0.02)], 6, [ix(BASALTO[i % 2]), ix(BASALTO[2]), ix(BASALTO[2])], eje='y', centro=(x, 0, z))
    p.crear()
# 16-17 fumarolas (~4,5 m) con boca de lava
for n, sem in ((16, 2), (17, 9)):
    p = pieza("fumarola", n)
    p.torno([(1.3, -0.3), (1.0, 1.2), (0.75, 2.6), (0.55, 3.8), (0.45, 4.5), (0.3, 4.3)], 10, [ix(m) for m in FUMA])
    rnd = random.Random(sem)
    for i in range(5):
        a = i * 1.7 + sem
        p.rocaJ((math.cos(a) * 0.95, 0.8 + i * 0.65, math.sin(a) * 0.95), (0.42, 0.36, 0.42), sem * 5 + i, 0.35, 0, ix(FUMA[1]))
    p.elip((0, 4.35, 0), (0.32, 0.12, 0.32), 8, 2, ix(LAVA))
    p.elip((0, 4.42, 0), (0.18, 0.06, 0.18), 6, 2, ix(LAVA2))
    for i in range(3):                       # grietas de lava por el flanco
        a = i * 2.1 + sem
        p.cajaJ((math.cos(a) * 0.86, 2.0 + i * 0.5, math.sin(a) * 0.86), (0.06, 0.9, 0.06), ix(LAVA), rot=(0, 0, 0))
    p.crear()
# 18 ópalo
p = pieza("opalo", 18)
p.rocaJ((0, 0.1, 0), (0.45, 0.3, 0.4), 41, 0.3, 1, ix(NEGRA))
for i in range(5):
    a = i * 1.25
    cristal(p, (math.cos(a) * 0.12, 0.18, math.sin(a) * 0.12), 0.07, 0.38, math.cos(a) * 25, math.degrees(a), ix(OPALO[i % 2]))
p.crear()
# 19 cristal térmico
p = pieza("cristal_termico", 19)
p.rocaJ((0, 0.1, 0), (0.7, 0.35, 0.6), 31, 0.3, 1, ix(NEGRA))
for i in range(5):
    a = i * 1.3
    cristal(p, (math.cos(a) * 0.25, 0.2, math.sin(a) * 0.25), 0.1, 0.6 + i * 0.1, 25 + i * 5, math.degrees(a), ix(TERMICO[i % 2]))
p.crear()
print("TRIS", triangulos())
