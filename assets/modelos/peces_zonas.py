"""Peces propios de cada zona, hechos con un mismo generador (cuerpo de torno, aletas, ojos y
detalles de cada especie). Cada pez son dos piezas: cola (z < -0,18) y cuerpo, numeradas
de dos en dos: 1-2 pez prisma (arrecife de cristal), 3-4 aguja de cuarzo (arrecife de
cristal), 5-6 pez lápida (osario), 7-8 candela de hueso (osario), 9-10 pez teja (meseta roja),
11-12 pez ceniza (grietas termales), 13-14 pez ojo (fosa abisal), 15-16 pez murciélago (cañones)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
MATS = []
_m = {}
def col(h, e=0.0):
    if (h, e) not in _m:
        _m[(h, e)] = len(MATS)
        MATS.append(material(f"p{h:06x}{'e' if e else ''}", h, emision=e))
    return _m[(h, e)]

n_pez = 0


def pez(nombre, perfil, aplastar, cuerpo, aleta, ojo, extra=None, cola=0.16, dorsal=0.1):
    """perfil: [(radio, z)] de la cola a la boca; la cola empieza en z = -0,18"""
    global n_pez
    p = PiezaJ(nombre, MATS)
    p.torno(perfil, 8, cuerpo, aplastar=aplastar)
    zb = perfil[-1][1]
    hy = max(r for r, z in perfil) * aplastar[1]
    hx = max(r for r, z in perfil) * aplastar[0]
    for s in (1, -1):
        p.elip((s * hx * 0.75, hy * 0.35, zb - 0.07), (0.025, 0.028, 0.022), 5, 3, ojo)
        p.tri((s * hx * 0.9, -hy * 0.1, zb - 0.12), (s * (hx + 0.08), -hy * 0.3, zb - 0.22),
              (s * hx * 0.8, -hy * 0.3, zb - 0.2), aleta, doble=True)
    p.cuad((0, 0, -0.18), (0, cola, -0.18 - cola), (0, 0, -0.18 - cola * 0.6), (0, -cola, -0.18 - cola), aleta, doble=True)
    p.tri((0, hy * 0.9, zb - 0.1), (0, hy + dorsal, zb - 0.22), (0, hy * 0.8, -0.12), aleta, doble=True)
    if extra:
        extra(p, hx, hy, zb)
    partir(p, [[-B, B, -B, B, -B, -0.18]], nombre, desde=n_pez * 2)
    n_pez += 1


# 1-2 pez prisma: pequeño, triangular, de vidrio violeta con el borde luminoso
def prisma(p, hx, hy, zb):
    p.tubo([(0, hy * 0.95, zb - 0.02, 0.012), (0, hy * 0.9, -0.15, 0.012)], 3, col(0xe0b0ff, 4))
    p.tubo([(0, -hy * 0.95, zb - 0.02, 0.012), (0, -hy * 0.9, -0.15, 0.012)], 3, col(0xe0b0ff, 4))
    p.elip((0, hy * 0.55, zb - 0.04), (0.02, 0.02, 0.02), 4, 3, col(0xffffff, 5))       # tercer ojo
    for k in (-1, 0, 1):                                                                # tres colas de luz
        p.tubo([(0, k * 0.08, -0.14, 0.012), (k * 0.03, k * 0.16, -0.34, 0.006)], 3, col(0xe0b0ff, 4), tapas=False)
pez("prisma", [(0, -0.19), (0.05, -0.12), (0.1, 0.0), (0.06, 0.12), (0, 0.18)], (0.35, 1.6),
    col(0xb090ff, 1.0), col(0xd8c0ff, 1.5), col(0x200830), prisma, 0.12, 0.04)

# 3-4 aguja de cuarzo: largo y fino, cristalino, con un hocico de aguja
def aguja(p, hx, hy, zb):
    p.tubo([(0, 0, zb - 0.02, 0.02), (0, 0, zb + 0.22, 0.004)], 4, col(0xf0fcff, 2))
    for k in range(4):                                                                  # fila de ojos
        p.elip((0, hy * 0.85, zb - 0.06 - k * 0.07), (0.014, 0.014, 0.014), 4, 3, col(0x40ffd0, 5))
pez("aguja", [(0, -0.2), (0.03, -0.15), (0.045, 0.0), (0.045, 0.18), (0.03, 0.3), (0, 0.34)], (0.9, 1.0),
    col(0xc8f0ff, 1.2), col(0x90d8f8, 1), col(0x0a1a28), aguja, 0.1, 0.05)

# 5-6 pez lápida: plano y ancho, pegado al fondo, gris con manchas de ceniza
def lapida(p, hx, hy, zb):
    for s in (1, -1):                                                                   # ojos con tallo
        p.tubo([(s * 0.05, hy * 0.8, zb - 0.06, 0.008), (s * 0.08, hy + 0.12, zb - 0.02, 0.006)], 3, col(0x8a8274))
        p.elip((s * 0.08, hy + 0.13, zb - 0.02), (0.02, 0.02, 0.02), 4, 3, col(0xffd070, 4))
        for i in range(5):                                                              # volante del borde
            z = 0.15 - i * 0.07
            p.tri((s * hx * 0.95, 0, z), (s * (hx + 0.06), 0.01, z - 0.035), (s * hx * 0.95, 0, z - 0.07),
                  col(0x7a7062), doble=True)
    for i in range(4):
        z = -0.1 + i * 0.09
        p.elip((0, hy * 0.92, z), (0.05, 0.012, 0.03), 5, 2, col(0x5a5448))
pez("lapida", [(0, -0.19), (0.07, -0.12), (0.15, 0.0), (0.15, 0.12), (0.08, 0.22), (0, 0.25)], (1.6, 0.4),
    col(0xa8a090), col(0x8a8274), col(0xffd070, 3), lapida, 0.13, 0.02)

# 7-8 candela de hueso: pez pálido con tres espinas que brillan naranja, como velas
def candela(p, hx, hy, zb):
    for i in range(3):
        z = 0.08 - i * 0.1
        p.tubo([(0, hy * 0.9, z, 0.014), (0, hy + 0.12, z - 0.02, 0.01)], 4, col(0xe8dcc0))
        p.elip((0, hy + 0.14, z - 0.02), (0.025, 0.035, 0.025), 5, 3, col(0xff9a40, 5))
pez("candela", [(0, -0.19), (0.05, -0.12), (0.08, 0.02), (0.07, 0.14), (0, 0.2)], (0.8, 1.1),
    col(0xd8ccb0), col(0xc0b498), col(0x2a1a10), candela, 0.13, 0.0)

# 9-10 pez teja: acorazado de placas rojas, como las terrazas de la meseta
def teja(p, hx, hy, zb):
    for s in (1, -1):                                                                   # seis remos
        for i in range(3):
            z = 0.1 - i * 0.09
            p.tubo([(s * hx * 0.9, -hy * 0.4, z, 0.01), (s * (hx + 0.1), -hy * 0.8, z - 0.02, 0.008)], 3, col(0xd07848))
            p.cuad((s * (hx + 0.08), -hy * 0.75, z), (s * (hx + 0.13), -hy * 0.75, z - 0.01),
                   (s * (hx + 0.13), -hy * 0.95, z - 0.04), (s * (hx + 0.08), -hy * 0.95, z - 0.03), col(0xd07848), doble=True)
    for i in range(4):
        z = 0.12 - i * 0.08
        p.cajaJ((0, hy * 0.6, z), (hx * 1.9, 0.03, 0.07), col(0xc05a3a))
pez("teja", [(0, -0.19), (0.07, -0.12), (0.12, 0.0), (0.12, 0.12), (0.07, 0.2), (0, 0.23)], (0.9, 1.0),
    col(0x8a3c2c), col(0xd07848), col(0xffe060, 2), teja, 0.15, 0.08)

# 11-12 pez ceniza: negro, con grietas de brasa a los lados
def ceniza(p, hx, hy, zb):
    for s in (1, -1):
        for i in range(3):
            z = 0.1 - i * 0.08
            p.cuad((s * hx * 1.01, -0.02, z), (s * hx * 1.01, 0.04, z - 0.02), (s * hx * 1.01, 0.03, z - 0.05),
                   (s * hx * 1.01, -0.03, z - 0.03), col(0xff6a1a, 5), doble=True)
pez("ceniza", [(0, -0.19), (0.05, -0.12), (0.09, 0.0), (0.09, 0.13), (0.05, 0.2), (0, 0.22)], (0.85, 1.0),
    col(0x262022), col(0x3a2e2a), col(0xffc040, 4), ceniza, 0.15, 0.07)

# 13-14 pez ojo: un ojo enorme que brilla, boca de dientes finos, cuerpo corto
def ojo(p, hx, hy, zb):
    for s in (1, -1):                                                                   # bigotes con luces
        p.tubo([(s * 0.04, -hy * 0.2, zb - 0.02, 0.006), (s * 0.18, -hy * 0.7, zb + 0.05, 0.004),
                (s * 0.24, -hy * 1.3, zb - 0.02, 0.003)], 3, col(0x2a3a6a), tapas=False)
        p.elip((s * 0.24, -hy * 1.35, zb - 0.02), (0.015, 0.015, 0.015), 4, 3, col(0x9ff8ff, 5))
    p.elip((0, hy * 0.2, zb - 0.06), (0.07, 0.07, 0.05), 7, 4, col(0x9ff8ff, 5))
    for s in (1, -1):
        p.tri((s * 0.03, -hy * 0.3, zb - 0.01), (s * 0.03, -hy * 0.75, zb - 0.03), (s * 0.02, -hy * 0.3, zb - 0.05),
              col(0xf0f0e0), doble=True)
pez("ojo", [(0, -0.19), (0.05, -0.13), (0.11, 0.0), (0.12, 0.08), (0, 0.15)], (0.8, 1.05),
    col(0x1a2440), col(0x2a3a6a), col(0x050810), ojo, 0.12, 0.05)

# 15-16 pez murciélago: aletas pectorales enormes como alas, gris violeta
def murcielago(p, hx, hy, zb):
    p.elip((0, hy * 0.75, zb - 0.05), (0.018, 0.018, 0.018), 4, 3, col(0xff4060, 5))
    for s in (1, -1):
        p.cuad((s * hx, 0, 0.1), (s * (hx + 0.32), 0.05, 0.0), (s * (hx + 0.28), 0.0, -0.14), (s * hx, -0.02, -0.08),
               col(0x6a5a8a), doble=True)
pez("murcielago", [(0, -0.19), (0.05, -0.12), (0.08, 0.0), (0.07, 0.12), (0, 0.17)], (1.0, 0.8),
    col(0x4a4060), col(0x5a4a78), col(0xff4060, 4), murcielago, 0.12, 0.04)

print("TRIS", triangulos())
