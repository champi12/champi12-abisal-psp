"""Decorado de las zonas nuevas en estilo bloque (base en y = 0, metros). El detalle lo pone
la textura pixel art Texturas.piedra() que el juego proyecta encima. Piezas:
Arrecife de cristal: 1-2 aguja de cristal plomado (recurso), 3 geoda abierta, 4-5 flor de cristal.
Osario de los colosos: 6-7 vértebra gigante, 8 cráneo de coloso, 9 colmillo de marfil (recurso),
10-11 flor de tuétano.
Meseta roja: 12 costra de azufre (recurso)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
from abisal_deco import *
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
    p = PiezaD(f"{nombre}#{len(piezas) + 1}", MATS)
    piezas.append(p)
    return p


# 1-2 aguja de cristal plomado: un racimo de prismas altos sobre un zócalo de sílice
for sem in (3, 8):
    p = pieza("aguja")
    rnd = random.Random(sem)
    roca(p, (0, 0.15, 0), (1.1, 0.4, 1.0), sem, [ix(SILICE[0]), ix(SILICE[1])], 3)
    cristal(p, (0, 0.2, 0), 0.32, 2.8 + rnd.uniform(0, 0.6), 4, rnd.uniform(0, 360), ix(VIDRIO[0]), ix(VIDRIO[2]))
    for i in range(5):
        a = i * 72 + rnd.uniform(-20, 20)
        ra = math.radians(a)
        d = rnd.uniform(0.3, 0.7)
        cristal(p, (math.cos(ra) * d, 0.15, math.sin(ra) * d), rnd.uniform(0.12, 0.22),
                rnd.uniform(1.0, 2.0), rnd.uniform(18, 38), a + 90, ix(VIDRIO[1 + i % 2]), ix(VIDRIO[2]))
    for i in range(3):                       # vetas de plomo en el zócalo
        a = math.radians(i * 120 + sem * 17)
        bloque(p, (math.cos(a) * 0.95, 0.12, math.sin(a) * 0.85), (0.5, 0.26, 0.42), ix(PLOMO), i * 120 + sem * 17)

# 3 geoda abierta: un cuenco de bloques de sílice con el frente roto, cristales dentro y un
# núcleo que brilla
p = pieza("geoda")
rnd = random.Random(12)
bloque(p, (0, 0.2, 0), (3.8, 0.4, 3.8), ix(SILICE[0]), 10)
for i in range(10):
    a = i * 36
    ra = math.radians(a)
    frente = 4 <= i <= 6                     # el lado roto, más bajo, para ver dentro
    h = 0.9 if frente else 2.4 + rnd.uniform(0, 0.8)
    r = 2.0
    c = (math.sin(ra) * r, h / 2, math.cos(ra) * r)
    bloque(p, c, (1.45, h, 0.55), ix(SILICE[i % 2]), a, -8 if not frente else 0)
    bloque(p, (math.sin(ra) * (r - 0.3), h - 0.1, math.cos(ra) * (r - 0.3)), (1.3, 0.14, 0.12), ix(PLOMO), a)
    if not frente and i % 2 == 0:            # remate encima del muro
        bloque(p, (c[0], h + 0.2, c[2]), (0.9, 0.4, 0.5), ix(SILICE[(i + 1) % 2]), a + 8, -8)
for i in range(12):
    a = math.radians(rnd.uniform(0, 360))
    d = rnd.uniform(0.7, 1.4)
    cristal(p, (math.sin(a) * d, 0.35 + rnd.uniform(0, 0.4), math.cos(a) * d), rnd.uniform(0.09, 0.16),
            rnd.uniform(0.5, 1.1), rnd.uniform(20, 45), math.degrees(a) + 180, ix(VIDRIO[i % 3]), ix(VIDRIO[2]))
bloque(p, (0, 0.85, 0), (0.75, 0.75, 0.75), ix(NUCLEO), 45, 20)
bloque(p, (0, 0.85, 0), (0.95, 0.4, 0.4), ix(VIDRIO[2]), 15, 0, 30)

# 4-5 flor de cristal: un tallo de vidrio con pétalos finos y una corola violeta
for sem in (4, 9):
    p = pieza("flor_cristal")
    rnd = random.Random(sem)
    h = 1.6 + rnd.uniform(0, 0.5)
    palo(p, (0, 0, 0), (0.1, h * 0.5, 0.05), 0.15, ix(VIDRIO[1]))
    palo(p, (0.1, h * 0.5, 0.05), (0, h, 0), 0.11, ix(VIDRIO[1]))
    for i in range(6):
        a = math.radians(i * 60 + sem * 9)
        c, s = math.cos(a), math.sin(a)
        plano(p, (0, h, 0), (-s, 0, c), norm((c, 0.75, s)), 0.24, 0.5, ix(VIDRIO[(i % 2) * 2]))
        plano(p, (c * 0.38, h + 0.28, s * 0.38), (-s, 0, c), norm((c, 0.5, s)), 0.14, 0.32, ix(VIDRIO[2]))
    bloque(p, (0, h + 0.06, 0), (0.24, 0.24, 0.24), ix(VIOLETA), 45)
    for i in range(3):                       # brotes al pie
        a = math.radians(i * 120 + 40)
        cristal(p, (math.cos(a) * 0.35, 0, math.sin(a) * 0.35), 0.06, 0.5, 25, i * 120 + 130, ix(VIDRIO[2]))

# 6-7 vértebra gigante: cuerpo, arco con el canal medular, apófisis y alas laterales
for sem in (6, 11):
    p = pieza("vertebra")
    rnd = random.Random(sem)
    inclina = rnd.uniform(-15, 15)
    k = p.marca()
    p.cajaJ((0, 1.2, 0), (2.3, 2.3, 1.3), ix(HUESO[0]))                 # cuerpo
    p.cajaJ((0, 1.2, 0), (2.5, 1.9, 0.5), ix(HUESO[1]))                 # reborde central
    for s in (1, -1):                                                    # arco del canal
        p.cajaJ((s * 0.6, 2.65, 0), (0.4, 1.0, 1.0), ix(HUESO[1]))
    p.cajaJ((0, 3.25, 0), (1.6, 0.4, 1.0), ix(HUESO[1]))
    p.cajaJ((0, 2.6, 0), (0.8, 0.8, 1.04), ix(HUECO))                   # el canal
    p.girar(k, (0, 0, 0), inclina, 0)
    palo(p, (0, 3.3, 0), (0, 4.4, -0.3), 0.5, ix(HUESO[2]))              # apófisis espinosa
    palo(p, (0, 4.4, -0.3), (0, 5.3, -0.8), 0.28, ix(HUESO[2]))
    for s in (1, -1):                                                    # transversas
        palo(p, (s * 0.8, 2.3, 0), (s * 2.0, 2.6, 0.1), 0.5, ix(HUESO[1]))
        palo(p, (s * 2.0, 2.6, 0.1), (s * 2.9, 2.3, 0.2), 0.3, ix(HUESO[2]))
    giro = rnd.uniform(0, 360)
    p.bm.verts.ensure_lookup_table()
    p.girar(0, (0, 0, 0), 0, giro)
    for i in range(4):                       # sedimento alrededor
        a = math.radians(i * 90 + 30)
        roca(p, (math.cos(a) * 1.5, 0.0, math.sin(a) * 1.5), (0.7, 0.25, 0.6), sem * 4 + i, ix(HUESO[2]), 1)

# 8 cráneo de coloso: bóveda escalonada, órbitas, fosas, dientes y la mandíbula caída (~9 m)
p = pieza("craneo")
p.cajaJ((0, 2.4, -0.6), (5.6, 3.4, 6.4), ix(HUESO[0]))                 # bóveda
p.cajaJ((0, 4.25, -0.9), (4.2, 0.6, 5.0), ix(HUESO[1]))                # cresta de arriba
p.cajaJ((0, 4.75, -1.4), (1.2, 0.5, 3.4), ix(HUESO[2]))
p.cajaJ((0, 2.0, 3.4), (3.6, 2.2, 2.0), ix(HUESO[0]))                  # hocico
p.cajaJ((0, 1.45, 4.6), (2.8, 1.1, 0.8), ix(HUESO[1]))
p.cajaJ((0, 2.3, -4.1), (4.0, 2.4, 0.8), ix(HUESO[2]))                 # nuca
for s in (1, -1):
    p.cajaJ((s * 2.2, 3.0, 1.5), (1.4, 1.3, 1.4), ix(HUECO))           # órbitas
    p.cajaJ((s * 2.35, 3.0, 1.5), (1.5, 1.6, 0.3), ix(HUESO[1]))       # ceja
    p.cajaJ((s * 0.65, 2.3, 4.45), (0.6, 0.8, 0.3), ix(HUECO))         # fosas
    for i in range(5):                                                   # dientes
        z = 4.4 - i * 0.85
        bloque(p, (s * (1.55 - i * 0.04), 0.4, z), (0.32, 1.2, 0.32), ix(MARFIL), 0, 0, s * 6)
k = p.marca()
for s in (1, -1):                                                        # mandíbula
    palo(p, (s * 1.6, 0.4, 3.8), (s * 2.2, 0.4, 0.5), 0.9, ix(HUESO[1]))
    palo(p, (s * 2.2, 0.4, 0.5), (s * 2.6, 0.9, -2.8), 1.0, ix(HUESO[1]))
p.girar(k, (0, 0.4, 0), 0, 0, inclina_z=8)
p.cuad((-3.0, 0.02, 4.0), (3.0, 0.02, 4.0), (3.0, 0.02, -3.5), (-3.0, 0.02, -3.5), ix(HUESO[2]))

# 9 colmillo de marfil: un colmillo curvo clavado en el sedimento (se arranca con el cuchillo)
p = pieza("colmillo")
pts = []
for i in range(5):
    t = i / 4
    pts.append((0.6 * t * t, 0.1 + 2.4 * t, -0.5 * t, 0.32 * (1 - t) + 0.05))
for i in range(4):
    palo(p, pts[i][:3], pts[i + 1][:3], (pts[i][3] + pts[i + 1][3]) * 1.4, ix(MARFIL), i * 10)
roca(p, (0, 0.05, 0), (0.6, 0.2, 0.55), 9, ix(HUESO[2]), 2)

# 10-11 flor de tuétano: tallos de hueso con una gota de luz anaranjada en la punta
for sem in (5, 13):
    p = pieza("tuetano")
    rnd = random.Random(sem)
    for i in range(4):
        a = math.radians(i * 90 + rnd.uniform(-25, 25))
        h = rnd.uniform(0.8, 1.5)
        b = (math.cos(a) * 0.15, 0, math.sin(a) * 0.15)
        m = (math.cos(a) * 0.35, h * 0.6, math.sin(a) * 0.35)
        tip = (math.cos(a) * 0.45, h, math.sin(a) * 0.45)
        palo(p, b, m, 0.13, ix(TALLO))
        palo(p, m, tip, 0.11, ix(TALLO))
        bloque(p, (tip[0], tip[1] + 0.1, tip[2]), (0.22, 0.28, 0.22), ix(TUETANO), math.degrees(a))
    bloque(p, (0, 0.06, 0), (0.6, 0.16, 0.6), ix(HUESO[1]), sem * 10)

# 12 costra de azufre: costras amarillas cristalinas sobre una roca roja de la meseta
p = pieza("azufre")
roca(p, (0, 0.3, 0), (1.0, 0.55, 0.85), 17, [ix(ROJA[0]), ix(ROJA[1])], 3)
rnd = random.Random(17)
for i in range(8):
    a = math.radians(rnd.uniform(0, 360))
    d = rnd.uniform(0.35, 0.85)
    bloque(p, (math.cos(a) * d, 0.55 + rnd.uniform(0, 0.3), math.sin(a) * d), (0.45, 0.22, 0.38),
           ix(AZUFRE[i % 2]), rnd.uniform(0, 90), rnd.uniform(-10, 10))
for i in range(4):
    a = rnd.uniform(0, 360)
    cristal(p, (math.cos(math.radians(a)) * 0.4, 0.6, math.sin(math.radians(a)) * 0.4), 0.07, 0.4, 30, a, ix(AZUFRE[2]))

for p in piezas:
    p.crear()
contar()
print("TRIS", triangulos())
