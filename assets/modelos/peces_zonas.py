"""Peces propios de cada zona en estilo bloque (cajas con textura pixel art del atlas común).
Cada pez son dos piezas: cola (detrás de z = -0,18, gira sobre ese punto) y cuerpo,
numeradas de dos en dos: 1-2 pez prisma (arrecife de cristal), 3-4 aguja de cuarzo (arrecife
de cristal), 5-6 pez lápida (osario), 7-8 candela de hueso (osario), 9-10 pez teja (meseta
roja), 11-12 pez ceniza (grietas termales), 13-14 pez ojo (fosa abisal), 15-16 pez
murciélago (cañones)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("peces_zonas", ppm=60)
n = [0]


def pez(nombre, lomo, vientre, aleta, ojo, ancho, alto, largo, cola_tam=(0.016, 0.14, 0.1), extra=None,
        cola_extra=None):
    """cuerpo escalonado (cabeza, tronco, tramo trasero) y cola en V detrás de z = -0,18"""
    cola = m.pieza(nombre + "_cola", n[0] * 2 + 1)
    cuerpo = m.pieza(nombre, n[0] * 2 + 2)
    n[0] += 1
    ESC = {'todo': E("escamas", lomo, vientre), '-y': E("liso", vientre)}
    z0 = -0.16
    zf = z0 + largo
    # tramo trasero, tronco y cabeza (cada uno un poco más estrecho hacia los extremos)
    cuerpo.caja((0, 0, z0 + largo * 0.15), (ancho * 0.7, alto * 0.72, largo * 0.34), ESC)
    # the boxes overlap a little: with exact joins a sliver of water showed through the belly
    cuerpo.caja((0, 0, z0 + largo * 0.5), (ancho, alto, largo * 0.5), ESC)
    cuerpo.caja((0, -alto * 0.03, zf - largo * 0.14), (ancho * 0.85, alto * 0.82, largo * 0.28),
                {'todo': E("escamas", lomo, vientre), '+z': E("liso", vientre)})
    for s in (1, -1):
        cuerpo.caja((s * ancho * 0.43, alto * 0.12, zf - largo * 0.1), (0.012, alto * 0.22, alto * 0.22),
                    {'todo': E("ojo", ojo, 0x101010)})
        cuerpo.caja((s * ancho * 0.6, -alto * 0.15, z0 + largo * 0.6), (ancho * 0.4, 0.012, largo * 0.18),
                    {'todo': E("aleta", aleta, lomo)}, rot=(0, 0, s * -20))
    cuerpo.caja((0, alto * 0.62, z0 + largo * 0.45), (0.014, alto * 0.3, largo * 0.35), {'todo': E("aleta", aleta, lomo)})
    if extra:
        extra(cuerpo, ancho, alto, largo, z0, zf)
    # cola: pedúnculo y aleta en V
    cola.caja((0, 0, -0.21), (ancho * 0.3, alto * 0.35, 0.07), ESC)
    cola.caja((0, cola_tam[1] * 0.35, -0.27), cola_tam, {'todo': E("aleta", aleta, lomo)}, rot=(28, 0, 0),
              pivote=(0, 0, -0.24))
    cola.caja((0, -cola_tam[1] * 0.35, -0.27), cola_tam, {'todo': E("aleta", aleta, lomo)}, rot=(-28, 0, 0),
              pivote=(0, 0, -0.24))
    if cola_extra:
        cola_extra(cola)


# 1-2 pez prisma: vidrio violeta, un tercer ojo y espinas de luz
def prisma(p, an, al, la, z0, zf):
    p.caja((0, al * 0.55, zf - la * 0.15), (0.03, 0.03, 0.03), {'todo': E("brillo", 0xffffff, 0xd8b0ff)})
    for k in range(3):
        p.caja((0, -al * 0.6, z0 + la * (0.3 + k * 0.2)), (0.012, 0.05, 0.012), {'todo': E("brillo", 0xe0b0ff, 0xb070ff)})
pez("prisma", 0x9070d8, 0xd8c0ff, 0xc8a8ff, 0xffffff, 0.08, 0.2, 0.34, extra=prisma)


# 2: aguja de cuarzo: larga y fina, hocico de aguja y fila de ojos
def aguja(p, an, al, la, z0, zf):
    p.caja((0, 0, zf + 0.12), (0.02, 0.02, 0.24), {'todo': E("liso", 0xf0fcff)})
    for k in range(4):
        p.caja((0, al * 0.52, zf - 0.04 - k * 0.06), (0.025, 0.012, 0.025), {'todo': E("brillo", 0x40ffd0, 0x20a090)})
pez("aguja", 0x80c0e0, 0xe0f8ff, 0xa0e0ff, 0x40ffd0, 0.07, 0.08, 0.5, cola_tam=(0.012, 0.1, 0.08), extra=aguja)


# 3: pez lápida: plano y ancho, ojos con tallo
def lapida(p, an, al, la, z0, zf):
    for s in (1, -1):
        p.caja((s * 0.05, al * 0.9, zf - 0.06), (0.012, 0.08, 0.012), {'todo': E("liso", 0x8a8274)})
        p.caja((s * 0.05, al * 1.3, zf - 0.06), (0.03, 0.03, 0.03), {'todo': E("brillo", 0xffd070, 0xc08030)})
    for k in range(3):
        p.caja((0, al * 0.52, z0 + la * (0.25 + k * 0.22)), (an * 0.7, 0.012, 0.05), {'todo': E("puntos", 0x6a6458, 0x3a3630)})
pez("lapida", 0x9a9282, 0xc8c0b0, 0x8a8274, 0xffd070, 0.3, 0.07, 0.4, extra=lapida)


# 4: candela de hueso: tres espinas con una llama naranja
def candela(p, an, al, la, z0, zf):
    for k in range(3):
        z = zf - 0.08 - k * 0.1
        p.caja((0, al * 0.75, z), (0.02, 0.12, 0.02), {'todo': E("hueso", 0xe8dcc0, 0xb8a888)})
        p.caja((0, al * 0.75 + 0.08, z), (0.04, 0.05, 0.04), {'todo': E("brillo", 0xffb060, 0xff6a20)})
pez("candela", 0xd8ccb0, 0xf0e8d8, 0xc0b498, 0x2a1a10, 0.12, 0.16, 0.36, extra=candela)


# 5: pez teja: placas rojas en el lomo y seis remos
def teja(p, an, al, la, z0, zf):
    for k in range(4):
        p.caja((0, al * 0.52, zf - 0.05 - k * 0.08), (an * 1.05, 0.025, 0.07), {'todo': E("franjas", 0xc05a3a, 0x8a3c2c)},
               rot=(-14, 0, 0))
    for s in (1, -1):
        for k in range(3):
            p.caja((s * an * 0.65, -al * 0.42, z0 + la * (0.3 + k * 0.2)), (0.08, 0.014, 0.04),
                   {'todo': E("aleta", 0xd07848, 0xf0b080)}, rot=(0, 0, s * -25))
pez("teja", 0x8a3c2c, 0xe8c8a0, 0xd07848, 0xffe060, 0.17, 0.2, 0.38, extra=teja)


# 6: pez ceniza: negro con grietas de brasa en los costados
def ceniza(p, an, al, la, z0, zf):
    for s in (1, -1):
        for k in range(3):
            p.caja((s * an * 0.51, -0.01 + k * 0.015, z0 + la * (0.3 + k * 0.18)), (0.006, 0.02, 0.06),
                   {'todo': E("brillo", 0xffc040, 0xff4a10)}, rot=(25, 0, 0))
pez("ceniza", 0x262022, 0x3a2e2a, 0x4a3a34, 0xffc040, 0.14, 0.17, 0.36, extra=ceniza)


# 7: pez ojo: un ojo enorme en la frente, colmillos y bigotes con luz
def ojo(p, an, al, la, z0, zf):
    p.caja((0, al * 0.2, zf + 0.005), (0.09, 0.09, 0.02), {'todo': E("brillo", 0x9ff8ff, 0x40a0c0)})
    p.caja((0, al * 0.2, zf + 0.012), (0.045, 0.045, 0.012), {'todo': E("liso", 0x0a1830)})   # la pupila
    for s in (1, -1):
        for k in range(2):
            p.caja((s * (0.015 + k * 0.025), -al * 0.35, zf), (0.01, 0.045, 0.01), {'todo': E("liso", 0xf0f0e0)})
        p.caja((s * 0.1, -al * 0.5, zf - 0.02), (0.1, 0.01, 0.01), {'todo': E("liso", 0x2a3a6a)}, rot=(0, s * 20, -s * 35))
        p.caja((s * 0.15, -al * 0.75, zf), (0.025, 0.025, 0.025), {'todo': E("brillo", 0x9ff8ff, 0x60e0f0)})
pez("ojo", 0x1a2440, 0x2a3a6a, 0x3a5a90, 0x0a1830, 0.18, 0.22, 0.34, extra=ojo)


# 8: pez murciélago: aletas como alas y tres ojos rojos
def murcielago(p, an, al, la, z0, zf):
    for s in (1, -1):
        p.caja((s * (an * 0.5 + 0.13), 0.01, z0 + la * 0.55), (0.26, 0.012, 0.18), {'todo': E("aleta", 0x6a5a8a, 0x4a4060)},
               rot=(0, 0, s * 8))
    p.caja((0, al * 0.4, zf - 0.03), (0.025, 0.025, 0.012), {'todo': E("brillo", 0xff4060, 0xa01030)})
pez("murcielago", 0x4a4060, 0x5a4a78, 0x6a5a8a, 0xff4060, 0.14, 0.12, 0.3, extra=murcielago)

m.crear()
