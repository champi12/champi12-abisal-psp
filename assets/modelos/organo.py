"""Órgano del coloso (osario): el cráneo de un coloso antiguo, de 14 m, convertido por los
antiguos en un instrumento: tubos de hueso se alzan de la bóveda y su interior brilla.
Piezas: "craneo#1" (cráneo, tubos y altar) y "diapason#2" (el diapasón de hueso que flota
sobre el altar hasta que lo tomas; su origen es el centro del diapasón)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
HUESO = material("hueso", 0xd8d0b8)
HUESO2 = material("hueso2", 0xb8b098)
HUECO = material("hueco", 0x1c1620)
LUZ = material("luz", 0xc89aff, emision=5)
MARFIL = material("marfil", 0xfff4dc, emision=1.0)
GLIFO = material("glifo", 0xb07aff, emision=4)
MATS = [HUESO, HUESO2, HUECO, LUZ, MARFIL, GLIFO]
p = PiezaJ("craneo#1", MATS)
# bóveda del cráneo, abierta por delante (se entra por la boca)
p.torno([(0, -7.5), (2.6, -7.0), (4.4, -4.6), (5.4, -1.2), (5.6, 2.0), (4.8, 4.8), (2.8, 6.8), (0, 7.4)], 14,
        0, eje='z', centro=(0, 4.2, 0), aplastar=(1.0, 0.72))
p.elip((0, 4.0, 0.5), (4.4, 2.9, 5.8), 12, 6, 2)                 # interior en sombra (se ve por las órbitas)
for s in (1, -1):
    p.elip((s * 3.6, 5.4, 3.0), (1.5, 1.3, 1.4), 8, 5, 2)          # órbitas
    p.elip((s * 3.4, 5.3, 2.6), (0.6, 0.6, 0.6), 6, 4, 3)          # una luz dentro de cada una
    for i in range(6):                                              # colmillos
        z = 6.2 - i * 1.4
        p.tubo([(s * (2.9 - i * 0.05), 1.6, z, 0.4), (s * 3.0, -0.2, z + 0.2, 0.08)], 6, 4, tapas=False)
# tubos de órgano de hueso sobre la bóveda, de mayor a menor
for i in range(7):
    x = (i - 3) * 1.1
    h = 4.5 + (3 - abs(i - 3)) * 1.8
    p.torno([(0.42, 0), (0.38, h * 0.9), (0.55, h)], 8, 1, eje='y', centro=(x, 6.6, -1.5))
    p.torno([(0.3, h - 0.05), (0, h - 0.4)], 8, 3, eje='y', centro=(x, 6.6, -1.5))
# altar dentro de la boca, con glifos
p.cajaJ((0, 0.6, 3.6), (2.4, 1.2, 1.6), 1)
for k in (-0.7, 0, 0.7):
    p.cajaJ((k, 1.21, 3.6), (0.3, 0.02, 0.9), 5)
p.cuad((-6, 0.02, -8), (6, 0.02, -8), (6, 0.02, 8), (-6, 0.02, 8), 1)
p.crear()
# el diapasón de hueso: dos púas y un mango, con glifos que brillan
d = PiezaJ("diapason#2", MATS)
d.tubo([(0, -0.6, 0, 0.07), (0, 0.0, 0, 0.08)], 6, 4)
for s in (1, -1):
    d.tubo([(0, 0.0, 0, 0.08), (s * 0.2, 0.15, 0, 0.07), (s * 0.22, 0.8, 0, 0.06)], 6, 4)
d.elip((0, 0.05, 0), (0.12, 0.12, 0.12), 6, 4, 5)
ob = d.crear()
ob.location = J(0, 2.2, 3.6)
print("TRIS", triangulos())
