"""Rocas y minerales del mundo en estilo bloque: montones de cajas giradas, sin curvas. El
detalle (sillares, grietas, motas) lo pone la textura pixel art Texturas.piedra() que el
juego proyecta encima, teñida por estos colores. Piezas:
1-2 roca, 3-4 veta de cobre, 5 cuarzo, 6-8 cresta de arrecife, 9-11 arco de piedra,
12-13 pilar de cañón, 14-15 basalto, 16-17 fumarola, 18 ópalo, 19 cristal térmico.
Todas con la base en y = 0 y las mismas medidas que los modelos de antes."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
GRIS = [material("gris1", 0x7d7a80), material("gris2", 0x6e6b72), material("gris3", 0x8e8b92)]
PARDO = [material("pardo1", 0x8a8478), material("pardo2", 0x7a746a), material("pardo3", 0x9a9486)]
COBRE = material("cobre", 0xd27a3c, metal=0.5)
COBRE2 = material("cobre2", 0x3aa088)
CUARZO = material("cuarzo", 0xf2d6e8, emision=2)
CUARZO2 = material("cuarzo2", 0xffffff, emision=3)
CANON = [material("canon1", 0x3e3c4e), material("canon2", 0x4a485e), material("canon3", 0x55536a)]
VETA = material("veta", 0x45e6ff, emision=4)
BASALTO = [material("bas1", 0x3a3436), material("bas2", 0x4a3a36), material("bas3", 0x5a4a44)]
FUMA = [material("fum1", 0x2c2628), material("fum2", 0x362e2e), material("fum3", 0x40302a), material("fum4", 0x4a3226), material("fum5", 0x6a2a14)]
LAVA = material("lava", 0xff7a1e, emision=5)
LAVA2 = material("lava2", 0xffc060, emision=6)
OPALO = [material("opalo1", 0x5ae8f0, emision=4), material("opalo2", 0xb07aff, emision=4)]
TERMICO = [material("term1", 0xff9a30, emision=4), material("term2", 0xffc060, emision=4)]
NEGRA = material("negra", 0x2c2626)
MATS = GRIS + PARDO + [COBRE, COBRE2, CUARZO, CUARZO2] + CANON + [VETA] + BASALTO + FUMA + [LAVA, LAVA2] + OPALO + TERMICO + [NEGRA]
I = {id(m): i for i, m in enumerate(MATS)}
ix = lambda m: I[id(m)]


class Pieza(PiezaJ):
    def crear(self):
        """quita lo que nunca se ve: caras enterradas y bases que miran al suelo"""
        import bmesh
        fuera = []
        for f in self.bm.faces:
            f.normal_update()
            c = f.calc_center_median()
            ys = [v.co.z for v in f.verts]       # Blender z = altura del juego
            if max(ys) < -0.02 or (f.normal.z < -0.9 and c.z < 0.2):
                fuera.append(f)
        bmesh.ops.delete(self.bm, geom=fuera, context='FACES')
        return super().crear()


def pieza(nombre, num):
    return Pieza(f"{nombre}#{num}", MATS)


def bloque(p, c, tam, mat, rumbo=0.0, inclina=0.0, inclina_z=0.0, pivote=None):
    k = p.marca()
    p.cajaJ(c, tam, mat)
    p.girar(k, pivote or c, inclina, rumbo, inclina_z)


def roca(p, c, radios, sem, mats, trozos=4):
    """montón de cajas: un bloque grande, otros que asoman por los lados y uno encima"""
    rnd = random.Random(sem)
    rx, ry, rz = radios
    cx, cy, cz = c
    m = lambda k: ix(mats[k % len(mats)]) if isinstance(mats, list) else ix(mats)
    bloque(p, (cx, cy - ry * 0.15, cz), (rx * 1.6, ry * 1.5, rz * 1.6), m(0), rnd.uniform(-20, 20))
    for k in range(trozos):
        a = k * 6.283 / trozos + rnd.uniform(-0.5, 0.5)
        s = rnd.uniform(0.5, 0.75)
        bloque(p, (cx + math.cos(a) * rx * 0.62, cy - ry * (0.35 - s * 0.2), cz + math.sin(a) * rz * 0.62),
               (rx * s * 1.3, ry * s * 1.4, rz * s * 1.2), m(k + 1), rnd.uniform(0, 90), rnd.uniform(-8, 8))
    bloque(p, (cx + rnd.uniform(-0.15, 0.15) * rx, cy + ry * 0.7, cz + rnd.uniform(-0.15, 0.15) * rz),
           (rx * 1.0, ry * 0.55, rz * 0.95), m(trozos + 1), rnd.uniform(-30, 30), rnd.uniform(-6, 6))


def cristal(p, base, r, h, inclina, rumbo, mat, mat_punta=None):
    """prisma cuadrado con la punta escalonada"""
    k = p.marca()
    bx, by, bz = base
    p.cajaJ((bx, by + h * 0.36, bz), (r * 1.6, h * 0.72, r * 1.6), mat)
    p.cajaJ((bx, by + h * 0.8, bz), (r * 1.1, h * 0.16, r * 1.1), mat_punta if mat_punta is not None else mat)
    if r >= 0.1:
        p.cajaJ((bx, by + h * 0.94, bz), (r * 0.55, h * 0.12, r * 0.55), mat_punta if mat_punta is not None else mat)
    p.girar(k, base, inclina, rumbo + 45)


# 1-2 roca (radios 1,6 x 1,1 x 1,4, centro a 0,4 m)
for n, sem in ((1, 21), (2, 33)):
    p = pieza("roca", n)
    roca(p, (0, 0.4, 0), (1.6, 1.1, 1.4), sem, GRIS[n % 3:] + GRIS[:n % 3], 4)
    rnd = random.Random(sem)
    for k in range(3):                       # lajas sueltas al pie
        a = rnd.uniform(0, 6.28)
        bloque(p, (math.cos(a) * 1.7, 0.05, math.sin(a) * 1.5), (0.55, 0.3, 0.45), ix(GRIS[k % 3]), rnd.uniform(0, 90))
    p.crear()
# 3-4 vetas de cobre, 5 cuarzo
for n, sem, cuarzo in ((3, 3, False), (4, 5, False), (5, 8, True)):
    p = pieza("veta", n)
    roca(p, (0, 0.35, 0), (1.0, 0.8, 0.9), sem, [GRIS[sem % 3], GRIS[(sem + 1) % 3]], 3)
    for i in range(6):
        a = i * 61 + sem * 13
        e = 25 + (i * 37 % 40)
        if cuarzo:
            cristal(p, (0, 0.35, 0), 0.13 + (i % 3) * 0.04, 1.2 + (i % 2) * 0.4, e, a, ix(CUARZO), ix(CUARZO2))
        else:
            ra, re = math.radians(a), math.radians(90 - e)
            d = (math.cos(re) * math.sin(ra), math.sin(re), math.cos(re) * math.cos(ra))
            c = (d[0] * 1.0, 0.35 + d[1] * 0.85, d[2] * 1.0)
            bloque(p, c, (0.5, 0.3, 0.42), ix(COBRE), a, -e * 0.5)
            if i % 3 == 0:                   # pátina verde del cobre
                bloque(p, (c[0], c[1] + 0.16, c[2]), (0.2, 0.05, 0.18), ix(COBRE2), a + 20, -e * 0.5)
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
        roca(p, (x, s * 0.25, rnd.uniform(-0.6, 0.6)), (s * 1.2, s * 0.9, s), sem * 13 + i,
             [PARDO[i % 3], PARDO[(i + 1) % 3]], 1)
    p.crear()
# 9-11 arcos de piedra (8 m de ancho, 4,6 m de alto, sobre el eje x): sillares a lo largo del arco
for n, sem in ((9, 1), (10, 4), (11, 7)):
    p = pieza("arco", n)
    rnd = random.Random(sem)
    pts = [(-4.2, -1.5, 0, 1.5)]
    for k in range(9):
        t = k / 8
        a = math.pi * (1 - t)
        g = (1.35 - 0.45 * math.sin(t * math.pi)) * (0.9 + rnd.random() * 0.25)
        pts.append((math.cos(a) * 4.0, math.sin(a) * 4.6, rnd.uniform(-0.15, 0.15), g))
    pts.append((4.2, -1.5, 0, 1.5))
    for k in range(len(pts) - 1):
        (x0, y0, z0, g0), (x1, y1, z1, g1) = pts[k], pts[k + 1]
        largo = math.hypot(x1 - x0, y1 - y0) + 0.35
        g = (g0 + g1) * 0.5
        ang = math.degrees(math.atan2(y1 - y0, x1 - x0))
        c = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
        bloque(p, c, (largo, g * 1.5, g * 1.7), ix(PARDO[k % 3]), rnd.uniform(-6, 6), 0, ang)
    for i in range(4):
        lado = -1 if i < 2 else 1
        roca(p, (lado * (4.2 + rnd.random() * 1.5), 0.1, rnd.uniform(-1.5, 1.5)),
             (0.9 + rnd.random() * 0.6, 0.6, 0.8), sem * 5 + i, [PARDO[i % 3]], 1)
    p.crear()
# 12-13 pilares de cañón (~20 m): tramos de bloque cada vez más finos, con vetas cian
for n, sem in ((12, 5), (13, 12)):
    p = pieza("pilar", n)
    rnd = random.Random(sem)
    pts = []
    for k in range(7):
        y = 20 * k / 6 if k else -1.5
        pts.append((rnd.uniform(-0.4, 0.4), y, rnd.uniform(-0.4, 0.4), 3.4 - k * 0.3 + rnd.random() * 0.6))
    for k in range(6):
        (x0, y0, z0, r0), (x1, y1, z1, r1) = pts[k], pts[k + 1]
        r = (r0 + r1) * 0.5 * 0.88
        c = ((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)
        alto = y1 - y0 + 0.4
        km = p.marca()
        p.cajaJ(c, (r * 2, alto, r * 2), ix(CANON[k % 3]))
        if k in (1, 2, 4):                   # veta de luz en una cara del tramo
            p.cajaJ((c[0] + r + 0.02, c[1], c[2] + r * 0.3), (0.06, alto * 0.8, 0.3), ix(VETA))
        # ledge between sections: a slightly wider slab
        p.cajaJ((c[0], y1 - 0.1, c[2]), (r * 2.15, 0.35, r * 2.15), ix(CANON[(k + 1) % 3]))
        p.girar(km, c, 0, k * 23 + rnd.uniform(-8, 8))
    roca(p, (0, 20, 0), (1.9, 1.2, 1.7), sem, CANON, 2)
    p.crear()
# 14-15 basalto: columnas cuadradas de alturas distintas
for n, sem in ((14, 2), (15, 7)):
    p = pieza("basalto", n)
    rnd = random.Random(sem)
    for i in range(7):
        x, z = (0, 0) if i == 0 else (math.cos((i - 1) * 1.0472) * 0.95, math.sin((i - 1) * 1.0472) * 0.95)
        h = 1.5 + rnd.random() * 3.5
        giro = rnd.uniform(0, 30)
        bloque(p, (x, (h - 0.5) / 2, z), (0.82, h + 0.5, 0.82), ix(BASALTO[i % 2]), giro)
        bloque(p, (x, h + 0.04, z), (0.66, 0.1, 0.66), ix(BASALTO[2]), giro)
    p.crear()
# 16-17 fumarolas (~4,5 m) con boca de lava
for n, sem in ((16, 2), (17, 9)):
    p = pieza("fumarola", n)
    rnd = random.Random(sem)
    perfil = [(1.3, -0.3), (1.0, 1.2), (0.75, 2.6), (0.55, 3.8), (0.45, 4.3)]
    for k in range(4):
        (r0, y0), (r1, y1) = perfil[k], perfil[k + 1]
        r = (r0 + r1) * 0.5
        bloque(p, (0, (y0 + y1) / 2, 0), (r * 1.8, y1 - y0 + 0.2, r * 1.8), ix(FUMA[k]), k * 19 + sem)
    for s in range(4):                       # borde del cráter
        a = math.radians(s * 90 + sem * 19 + 57)
        bloque(p, (math.cos(a) * 0.36, 4.42, math.sin(a) * 0.36), (0.2, 0.24, 0.7), ix(FUMA[4]), s * 90 + sem * 19 + 57 + 90)
    bloque(p, (0, 4.36, 0), (0.55, 0.06, 0.55), ix(LAVA), sem * 19 + 57)
    bloque(p, (0, 4.4, 0), (0.3, 0.06, 0.3), ix(LAVA2), sem * 19 + 12)
    for i in range(5):
        a = i * 1.7 + sem
        bloque(p, (math.cos(a) * 0.95, 0.8 + i * 0.65, math.sin(a) * 0.95), (0.6, 0.5, 0.6), ix(FUMA[1]), rnd.uniform(0, 90))
    for i in range(3):                       # grietas de lava por el flanco
        a = i * 2.1 + sem
        bloque(p, (math.cos(a) * 0.86, 2.0 + i * 0.5, math.sin(a) * 0.86), (0.08, 0.9, 0.08), ix(LAVA), math.degrees(a))
    p.crear()
# 18 ópalo
p = pieza("opalo", 18)
roca(p, (0, 0.1, 0), (0.45, 0.3, 0.4), 41, [NEGRA], 2)
for i in range(5):
    a = i * 1.25
    cristal(p, (math.cos(a) * 0.12, 0.18, math.sin(a) * 0.12), 0.07, 0.38, math.cos(a) * 25, math.degrees(a),
            ix(OPALO[i % 2]), ix(OPALO[(i + 1) % 2]))
p.crear()
# 19 cristal térmico
p = pieza("cristal_termico", 19)
roca(p, (0, 0.1, 0), (0.7, 0.35, 0.6), 31, [NEGRA], 3)
for i in range(5):
    a = i * 1.3
    cristal(p, (math.cos(a) * 0.25, 0.2, math.sin(a) * 0.25), 0.1, 0.6 + i * 0.1, 25 + i * 5, math.degrees(a),
            ix(TERMICO[i % 2]), ix(TERMICO[1]))
p.crear()
for ob in bpy.data.objects:
    print("PIEZA", ob.name, len(ob.data.polygons) * 2)
print("TRIS", triangulos())
