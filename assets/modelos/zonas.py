"""Decorado de las zonas nuevas (base en y = 0, metros). Piezas:
Arrecife de cristal: 1-2 aguja de cristal plomado (recurso), 3 geoda abierta, 4-5 flor de cristal.
Osario de los colosos: 6-7 vértebra gigante, 8 cráneo de coloso, 9 colmillo de marfil (recurso),
10-11 flor de tuétano.
Meseta roja: 12 costra de azufre (recurso)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()

VIDRIO = [material("vid1", 0x7fd8ff, emision=1.2), material("vid2", 0x5ab0f0, emision=1.0),
          material("vid3", 0xb8f0ff, emision=1.6)]
PLOMO = material("plomo", 0x4a5a78)
SILICE = [material("sil1", 0x2a3450), material("sil2", 0x343e5c)]
NUCLEO = material("nucleo", 0xc8f8ff, emision=4)
VIOLETA = material("violeta", 0xb48cff, emision=3)
HUESO = [material("hue1", 0xd8d0b8), material("hue2", 0xc4bca2), material("hue3", 0xaea68c)]
HUECO = material("hueco", 0x2a2420)
MARFIL = material("marfil", 0xf4ecd6, emision=0.6)
TUETANO = material("tuetano", 0xff9a48, emision=4)
TALLO = material("tallo", 0xbfae8a)
AZUFRE = [material("az1", 0xe8d030, emision=0.8), material("az2", 0xc8a818), material("az3", 0xf4e870, emision=1.5)]
ROJA = [material("roja1", 0x8a4636), material("roja2", 0x74403a)]
MATS = VIDRIO + [PLOMO] + SILICE + [NUCLEO, VIOLETA] + HUESO + [HUECO, MARFIL, TUETANO, TALLO] + AZUFRE + ROJA
I = {id(m): i for i, m in enumerate(MATS)}
ix = lambda m: I[id(m)]

piezas = []


def pieza(nombre):
    p = PiezaJ(f"{nombre}#{len(piezas) + 1}", MATS)
    piezas.append(p)
    return p


def prisma(p, base, r, h, inclina, rumbo, mat, lados=6):
    """un cristal: prisma de lados caras con punta"""
    k = p.marca()
    p.torno([(r * 0.85, 0), (r, h * 0.72), (r * 0.35, h * 0.92), (0, h)], lados, mat, eje='y', centro=base)
    p.girar(k, base, inclina, rumbo)


# 1-2 aguja de cristal plomado: un racimo de prismas altos sobre un zócalo de sílice
for sem in (3, 8):
    p = pieza("aguja")
    rnd = random.Random(sem)
    p.rocaJ((0, 0.15, 0), (1.1, 0.4, 1.0), sem, 0.25, 1, ix(SILICE[sem % 2]))
    prisma(p, (0, 0.2, 0), 0.32, 2.8 + rnd.uniform(0, 0.6), 4, rnd.uniform(0, 360), ix(VIDRIO[0]))
    for i in range(5):
        a = i * 72 + rnd.uniform(-20, 20)
        ra = math.radians(a)
        d = rnd.uniform(0.3, 0.7)
        prisma(p, (math.cos(ra) * d, 0.15, math.sin(ra) * d), rnd.uniform(0.12, 0.22),
               rnd.uniform(1.0, 2.0), rnd.uniform(18, 38), a + 90, ix(VIDRIO[1 + i % 2]))
    # vetas de plomo en el zócalo
    for i in range(3):
        a = math.radians(i * 120 + sem * 17)
        p.rocaJ((math.cos(a) * 0.8, 0.1, math.sin(a) * 0.7), (0.3, 0.14, 0.25), sem * 5 + i, 0.3, 0, ix(PLOMO))
    p.crear()

# 3 geoda abierta: una roca hueca partida con cristales dentro y un núcleo que brilla
p = pieza("geoda")
rnd = random.Random(12)
lados, n = 12, 6
ext, inn = [], []
for i in range(n + 1):
    t = i / n
    fi = math.radians(10 + 150 * t)  # de abajo a casi arriba: la parte de arriba está abierta
    fila_e, fila_i = [], []
    for j in range(lados + 1):
        a = j * math.tau / lados
        r = 2.2 * (1 + rnd.uniform(-0.06, 0.06))
        y = 1.6 - math.cos(fi) * 1.6
        rr = math.sin(fi)
        fila_e.append((math.cos(a) * rr * r, y * 1.0, math.sin(a) * rr * r))
        fila_i.append((math.cos(a) * rr * r * 0.78, y * 0.86 + 0.15, math.sin(a) * rr * r * 0.78))
    ext.append(fila_e)
    inn.append(fila_i)
# corte: la mitad del frente rota hacia abajo para que se vea el interior
cascara(p, inn, ext, lambda i, j: not (i >= 3 and 2 <= j <= 5), ix(SILICE[1]), ix(SILICE[0]), ix(PLOMO))
for i in range(14):
    a = math.radians(rnd.uniform(0, 360))
    d = rnd.uniform(0.6, 1.3)
    prisma(p, (math.cos(a) * d, 0.35 + rnd.uniform(0, 0.6), math.sin(a) * d), rnd.uniform(0.08, 0.16),
           rnd.uniform(0.5, 1.1), rnd.uniform(20, 50), math.degrees(a) + 180, ix(VIDRIO[i % 3]), 5)
p.elip((0, 0.7, 0), (0.45, 0.45, 0.45), 8, 5, ix(NUCLEO))
p.crear()

# 4-5 flor de cristal: un tallo de vidrio con pétalos finos y una corola violeta
for sem in (4, 9):
    p = pieza("flor_cristal")
    rnd = random.Random(sem)
    h = 1.6 + rnd.uniform(0, 0.5)
    p.tubo([(0, 0, 0, 0.09), (0.1, h * 0.5, 0.05, 0.07), (0, h, 0, 0.05)], 5, ix(VIDRIO[1]))
    for i in range(6):
        a = math.radians(i * 60 + sem * 9)
        c, s = math.cos(a), math.sin(a)
        b = (0, h, 0)
        m1 = (c * 0.45 - s * 0.12, h + 0.35, s * 0.45 + c * 0.12)
        m2 = (c * 0.45 + s * 0.12, h + 0.35, s * 0.45 - c * 0.12)
        tip = (c * 0.75, h + 0.55, s * 0.75)
        p.cuad(b, m1, tip, m2, ix(VIDRIO[(i % 2) * 2]), doble=True)
    p.elip((0, h + 0.05, 0), (0.14, 0.14, 0.14), 6, 4, ix(VIOLETA))
    # brotes pequeños al pie
    for i in range(3):
        a = math.radians(i * 120 + 40)
        prisma(p, (math.cos(a) * 0.35, 0, math.sin(a) * 0.35), 0.06, 0.5, 25, i * 120 + 130, ix(VIDRIO[2]), 4)
    p.crear()

# 6-7 vértebra gigante: cuerpo con el canal medular, apófisis y alas laterales
for sem in (6, 11):
    p = pieza("vertebra")
    rnd = random.Random(sem)
    inclina = rnd.uniform(-15, 15)
    k = p.marca()
    # cuerpo (disco grueso, eje hacia delante)
    p.torno([(0, -0.7), (1.1, -0.65), (1.25, -0.4), (1.15, 0.0), (1.25, 0.4), (1.1, 0.65), (0, 0.7)], 10,
            ix(HUESO[0]), eje='z', centro=(0, 1.2, 0))
    # arco y canal: un anillo encima
    p.torno([(0.45, -0.5), (0.75, -0.45), (0.75, 0.45), (0.45, 0.5), (0.45, -0.5)], 8, ix(HUESO[1]),
            eje='z', centro=(0, 2.6, 0))
    p.elip((0, 2.6, 0), (0.42, 0.42, 0.55), 6, 4, ix(HUECO))
    # apófisis espinosa y transversas
    p.tubo([(0, 3.2, 0, 0.32), (0, 4.4, -0.3, 0.2), (0, 5.3, -0.8, 0.08)], 6, ix(HUESO[2]))
    for s in (1, -1):
        p.tubo([(s * 0.8, 2.3, 0, 0.3), (s * 2.0, 2.6, 0.1, 0.22), (s * 2.9, 2.3, 0.2, 0.08)], 6, ix(HUESO[1]))
    p.girar(k, (0, 0, 0), inclina, rnd.uniform(0, 360))
    # medio enterrada: un poco de sedimento alrededor
    for i in range(4):
        a = math.radians(i * 90 + 30)
        p.rocaJ((math.cos(a) * 1.4, 0.0, math.sin(a) * 1.4), (0.7, 0.25, 0.6), sem * 4 + i, 0.3, 0, ix(HUESO[2]))
    p.crear()

# 8 cráneo de coloso: bóveda, órbitas, mandíbula partida y dientes (unos 9 m)
p = pieza("craneo")
p.torno([(0, -4.5), (1.4, -4.2), (2.4, -2.8), (3.0, -0.8), (3.2, 1.0), (2.8, 2.8), (1.6, 4.0), (0, 4.4)], 12,
        ix(HUESO[0]), eje='z', centro=(0, 2.3, 0), aplastar=(1.0, 0.75))
for s in (1, -1):
    p.elip((s * 2.2, 3.0, 1.6), (0.9, 0.8, 0.9), 8, 5, ix(HUECO))          # órbitas
    p.elip((s * 0.7, 1.6, 4.2), (0.35, 0.5, 0.3), 6, 4, ix(HUECO))         # fosas
    for i in range(5):                                                       # dientes de arriba
        z = 3.6 - i * 0.9
        p.tubo([(s * (1.7 - i * 0.05), 0.9, z, 0.22), (s * 1.75, -0.3, z + 0.1, 0.05)], 5, ix(MARFIL), tapas=False)
# mandíbula caída a un lado
k = p.marca()
for s in (1, -1):
    p.tubo([(s * 1.6, 0.4, 3.8, 0.5), (s * 2.2, 0.4, 0.5, 0.6), (s * 2.6, 0.9, -2.8, 0.55)], 7, ix(HUESO[1]))
p.girar(k, (0, 0.4, 0), 0, 0, inclina_z=8)
p.cuad((-3.0, 0.02, -3.5), (3.0, 0.02, -3.5), (3.0, 0.02, 4.0), (-3.0, 0.02, 4.0), ix(HUESO[2]))
p.crear()

# 9 colmillo de marfil: un colmillo curvo clavado en el sedimento (se arranca con el cuchillo)
p = pieza("colmillo")
pts = []
for i in range(8):
    t = i / 7
    pts.append((0.6 * t * t, 0.1 + 2.4 * t, -0.5 * t, 0.32 * (1 - t) + 0.03))
p.tubo(pts, 7, ix(MARFIL))
p.rocaJ((0, 0.05, 0), (0.6, 0.2, 0.55), 9, 0.3, 0, ix(HUESO[2]))
p.crear()

# 10-11 flor de tuétano: tallos huecos de hueso con una gota de luz anaranjada en la punta
for sem in (5, 13):
    p = pieza("tuetano")
    rnd = random.Random(sem)
    for i in range(4):
        a = math.radians(i * 90 + rnd.uniform(-25, 25))
        h = rnd.uniform(0.8, 1.5)
        b = (math.cos(a) * 0.15, 0, math.sin(a) * 0.15)
        m = (math.cos(a) * 0.35, h * 0.6, math.sin(a) * 0.35)
        tip = (math.cos(a) * 0.45, h, math.sin(a) * 0.45)
        p.tubo([(b[0], b[1], b[2], 0.07), (m[0], m[1], m[2], 0.06), (tip[0], tip[1], tip[2], 0.08)], 5, ix(TALLO))
        p.elip((tip[0], tip[1] + 0.1, tip[2]), (0.13, 0.17, 0.13), 6, 4, ix(TUETANO))
    p.rocaJ((0, 0.02, 0), (0.35, 0.1, 0.35), sem, 0.3, 0, ix(HUESO[1]))
    p.crear()

# 12 costra de azufre: costras amarillas cristalinas sobre una roca roja de la meseta
p = pieza("azufre")
p.rocaJ((0, 0.3, 0), (1.0, 0.55, 0.85), 17, 0.25, 1, ix(ROJA[0]))
rnd = random.Random(17)
for i in range(9):
    a = math.radians(rnd.uniform(0, 360))
    d = rnd.uniform(0.3, 0.8)
    p.rocaJ((math.cos(a) * d, 0.45 + rnd.uniform(0, 0.3), math.sin(a) * d), (0.28, 0.14, 0.24), 30 + i, 0.35, 0,
            ix(AZUFRE[i % 2]))
for i in range(5):
    a = rnd.uniform(0, 360)
    prisma(p, (math.cos(math.radians(a)) * 0.4, 0.55, math.sin(math.radians(a)) * 0.4), 0.06, 0.35, 30, a,
           ix(AZUFRE[2]), 4)
p.crear()

print("TRIS", triangulos())
