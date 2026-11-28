"""Órgano del coloso (osario) en estilo bloque: el cráneo de un coloso antiguo, de 14 m,
convertido por los antiguos en un instrumento: tubos de hueso se alzan de la bóveda y su
interior brilla. El detalle lo pone la textura pixel art Texturas.piedra().
Piezas: "craneo#1" (cráneo, tubos y altar) y "diapason#2" (el diapasón de hueso que flota
sobre el altar hasta que lo tomas; su origen es el centro del diapasón)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
from abisal_deco import *
limpiar()
HUESO = material("hueso", 0xd8d0b8)
HUESO2 = material("hueso2", 0xb8b098)
HUECO = material("hueco", 0x1c1620)
LUZ = material("luz", 0xc89aff, emision=5)
MARFIL = material("marfil", 0xfff4dc, emision=1.0)
GLIFO = material("glifo", 0xb07aff, emision=4)
MATS = [HUESO, HUESO2, HUECO, LUZ, MARFIL, GLIFO]
p = PiezaD("craneo#1", MATS)
# bóveda escalonada (abierta por delante: se entra por la boca, entre los colmillos)
for s in (1, -1):
    p.cajaJ((s * 4.6, 3.8, -0.6), (1.8, 6.0, 12.4), 0)          # paredes de los lados
    p.cajaJ((s * 3.6, 7.1, -0.4), (1.6, 1.4, 11.0), 1)          # hombros
p.cajaJ((0, 8.0, -0.6), (6.4, 1.4, 11.6), 0)                    # techo
p.cajaJ((0, 8.9, -1.4), (4.0, 0.6, 8.0), 1)
p.cajaJ((0, 4.0, -7.0), (8.6, 7.4, 1.6), 1)                     # nuca
p.cajaJ((0, 5.6, 5.9), (7.6, 3.6, 1.4), 0)                      # frente sobre la boca
p.cajaJ((0, 4.6, 0.2), (7.0, 5.4, 11.4), 2)                     # interior en sombra
for s in (1, -1):
    p.cajaJ((s * 3.6, 5.4, 6.4), (2.2, 1.9, 0.6), 2)            # órbitas
    p.cajaJ((s * 3.5, 5.4, 6.7), (0.9, 0.9, 0.3), 3)            # una luz dentro de cada una
    p.cajaJ((s * 3.6, 6.6, 6.5), (2.6, 0.5, 0.8), 1)            # ceja
    for i in range(5):                                           # colmillos
        z = 6.2 - i * 1.5
        bloque(p, (s * (4.1 - i * 0.05), 1.0, z), (0.6, 2.2, 0.6), 4, 0, 0, s * 5)
# tubos de órgano de hueso sobre la bóveda, de mayor a menor
for i in range(7):
    x = (i - 3) * 1.1
    h = 4.5 + (3 - abs(i - 3)) * 1.8
    p.cajaJ((x, 8.4 + h / 2, -1.5), (0.8, h, 0.8), 1)
    p.cajaJ((x, 8.4 + h - 0.1, -1.5), (1.05, 0.3, 1.05), 0)     # boca del tubo
    p.cajaJ((x, 8.4 + h + 0.06, -1.5), (0.5, 0.04, 0.5), 3)     # luz dentro
# altar dentro de la boca, con glifos
p.cajaJ((0, 0.6, 3.6), (2.4, 1.2, 1.6), 1)
for k in (-0.7, 0, 0.7):
    p.cajaJ((k, 1.21, 3.6), (0.3, 0.02, 0.9), 5)
p.cuad((-6, 0.02, 8), (6, 0.02, 8), (6, 0.02, -8), (-6, 0.02, -8), 1)
p.crear()
# el diapasón de hueso: dos púas y un mango, con glifos que brillan
d = PiezaJ("diapason#2", MATS)   # (no culling: its handle hangs below its origin)
palo(d, (0, -0.6, 0), (0, 0.0, 0), 0.14, 4)
for s in (1, -1):
    palo(d, (0, 0.0, 0), (s * 0.2, 0.15, 0), 0.14, 4)
    palo(d, (s * 0.2, 0.15, 0), (s * 0.22, 0.8, 0), 0.12, 4)
d.cajaJ((0, 0.05, 0), (0.22, 0.22, 0.22), 5)
ob = d.crear()
ob.location = J(0, 2.2, 3.6)
print("TRIS", triangulos())
