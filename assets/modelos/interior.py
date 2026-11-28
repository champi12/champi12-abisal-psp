"""Interior de la cápsula (visto desde el centro, ojo a 1,1 m): cabina redonda de 3,6 m con
cinco puestos en los mismos rumbos que el juego (0 = -z): sintetizador 0, consola 90,
dispensador médico 180, bodega 250, escalera a la escotilla 315; paneles entre ellos."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
PARED = material("pared", 0x284248)
PARED2 = material("pared2", 0x223a40)
ALTA = material("alta", 0x6a8a90)
ALTA2 = material("alta2", 0x5e7a80)
TECHO = material("techo", 0x445a60)
SUELO = material("suelo", 0x162428)
COBRE = material("cobre", 0xb8763e, metal=0.5)
CIAN = material("cian", 0x6ff0ff, emision=4)
POD = material("pod", 0x2e4a52)
POD2 = material("pod2", 0x1a2a30)
NAR = material("naranja", 0xd86a2a)
REJA = material("reja", 0x2a3036)
BARRA = material("barra", 0x4a5258)
TUBO = material("tubos", 0xb8c0c4)
AMBAR = material("ambar", 0xffb040, emision=4)
VERDE = material("verde", 0x5ff0e0, emision=4)
LAMPARA = material("lampara", 0xfff0d0, emision=5)
VENTANA = material("ventana", 0x9ad8ff, emision=3)
ESCALERA = material("escalera", 0x8a9298)
AMARILLO = material("amarillo", 0xe8c030)
NEGRO = material("negro", 0x202020)
LUCES = [material("l1", 0x5aff7a, emision=4), material("l2", 0xffb030, emision=4), material("l3", 0xff4030, emision=4)]
MATS = [PARED, PARED2, ALTA, ALTA2, TECHO, SUELO, COBRE, CIAN, POD, POD2, NAR, REJA, BARRA, TUBO, AMBAR, VERDE, LAMPARA,
        VENTANA, ESCALERA, AMARILLO, NEGRO] + LUCES
M = {m.name: i for i, m in enumerate(MATS)}
p = PiezaJ("interior#1", MATS)
R, SEG = 1.8, 16
def en(gr, r, pts):
    """convierte puntos locales de un puesto (x lateral, y, z hacia el centro) al rumbo gr"""
    a = math.radians(gr)
    cx, cz = -math.sin(a) * r, -math.cos(a) * r
    out = []
    for (x, y, z) in pts:
        # rotación sobre y por gr: el +z local apunta al centro
        X = x * math.cos(a) + z * math.sin(a)
        Z = -x * math.sin(a) + z * math.cos(a)
        out.append((cx + X, y, cz + Z))
    return out
def caja_en(gr, r, pos, tam, mat):
    k = p.marca()
    p.cajaJ(en(gr, r, [pos])[0], tam, mat)
    c = en(gr, r, [pos])[0]
    p.girar(k, c, 0, gr)
# paredes (las caras miran hacia dentro), anillos y techo abovedado
for i in range(SEG):
    a0, a1 = 2 * math.pi * i / SEG, 2 * math.pi * (i + 1) / SEG
    c0, s0, c1, s1 = math.cos(a0), math.sin(a0), math.cos(a1), math.sin(a1)
    x0, z0, x1, z1 = c0 * R, s0 * R, c1 * R, s1 * R
    p.cuad((x0, 0, z0), (x0, 0.95, z0), (x1, 0.95, z1), (x1, 0, z1), M["pared" if i % 2 == 0 else "pared2"], doble=True)
    p.cuad((x0, 1.0, z0), (x0, 2.1, z0), (x1, 2.1, z1), (x1, 1.0, z1), M["alta" if i % 2 == 0 else "alta2"], doble=True)
    ra, rb = 1.62, 1.3
    p.cuad((x0, 2.1, z0), (c0 * ra, 2.42, s0 * ra), (c1 * ra, 2.42, s1 * ra), (x1, 2.1, z1), M["alta2"], doble=True)
    p.cuad((c0 * ra, 2.42, s0 * ra), (c0 * rb, 2.6, s0 * rb), (c1 * rb, 2.6, s1 * rb), (c1 * ra, 2.42, s1 * ra), M["techo"], doble=True)
    p.tri((0, 2.6, 0), (c0 * rb, 2.6, s0 * rb), (c1 * rb, 2.6, s1 * rb), M["techo"], doble=True)
    p.tri((0, 0, 0), (x1, 0, z1), (x0, 0, z0), M["suelo"], doble=True)
    p.cuad((x0 * 0.985, 0.93, z0 * 0.985), (x0 * 0.985, 1.02, z0 * 0.985), (x1 * 0.985, 1.02, z1 * 0.985), (x1 * 0.985, 0.93, z1 * 0.985), M["cobre"], doble=True)
    p.cuad((x0 * 0.9, 0.012, z0 * 0.9), (x1 * 0.9, 0.012, z1 * 0.9), (x1 * 0.94, 0.012, z1 * 0.94), (x0 * 0.94, 0.012, z0 * 0.94), M["cian"], doble=True)
    if i % 2 == 0:
        p.tubo([(x0 * 0.965, 0.05, z0 * 0.965, 0.035), (x0 * 0.965, 2.1, z0 * 0.965, 0.035)], 4, M["cobre"], False)
# barandilla y tubería perimetral
p.tubo([(math.cos(2 * math.pi * i / 16) * 1.7, 2.02, math.sin(2 * math.pi * i / 16) * 1.7, 0.045) for i in range(17)], 5, M["tubos"], False)
p.tubo([(math.cos(2 * math.pi * i / 16) * 1.7, 0.22, math.sin(2 * math.pi * i / 16) * 1.7, 0.04) for i in range(17)], 5, M["naranja"], False)
# suelo de rejilla y aro de la escotilla
p.cajaJ((0, 0.01, 0), (1.4, 0.02, 1.4), M["reja"])
for i in range(-3, 4):
    p.cajaJ((i * 0.4, 0.025, 0), (0.02, 0.01, 1.38), M["barra"])
for i in range(12):
    a0, a1 = 2 * math.pi * i / 12, 2 * math.pi * (i + 1) / 12
    r0, r1 = 0.52, 0.66
    p.cuad((math.cos(a0) * r0, 2.595, math.sin(a0) * r0), (math.cos(a0) * r1, 2.595, math.sin(a0) * r1),
           (math.cos(a1) * r1, 2.595, math.sin(a1) * r1), (math.cos(a1) * r0, 2.595, math.sin(a1) * r0), M["amarillo" if i % 2 == 0 else "negro"], doble=True)
# paneles de pared con pilotos entre los puestos
for gr in (36, 108, 180 + 0.0, 324):
    if gr == 180:
        continue
    for fy in range(3):
        for fx in range(2):
            caja_en(gr, 1.76, (-0.16 + fx * 0.32, 1.25 + fy * 0.26, 0), (0.15, 0.12, 0.04), M["alta" if (fx + fy) % 2 == 0 else "alta2"])
    for k in range(3):
        caja_en(gr, 1.76, (-0.12 + k * 0.12, 1.08, 0.03), (0.025, 0.02, 0.01), M[f"l{k + 1}"])
def torno_en(gr, r, perfil, lados, mats):
    c = en(gr, r, [(0, 0, 0)])[0]
    p.torno(perfil, lados, mats, eje='y', centro=c)
# 0: sintetizador con pantalla holográfica
torno_en(0, 1.55, [(0.45, 0), (0.52, 0.15), (0.52, 0.85), (0.38, 1.05)], 10, [M["pod2"], M["pod"], M["cobre"]])
caja_en(0, 1.55, (0, 1.18, 0.4), (0.5, 0.28, 0.02), M["verde"])
caja_en(0, 1.55, (0, 1.52, 0.32), (0.12, 0.12, 0.02), M["cian"])
# 90: consola central (mesa holográfica)
torno_en(90, 1.6, [(0.55, 0), (0.6, 0.2), (0.35, 0.4)], 10, [M["pod2"], M["pod"]])
caja_en(90, 1.6, (0, 0.62, 0.12), (0.5, 0.04, 0.4), M["ambar"])
caja_en(90, 1.6, (0, 0.7, 0), (0.08, 0.2, 0.08), M["cian"])
# 180: dispensador médico (la cruz holográfica la dibuja el juego aparte)
torno_en(180, 1.6, [(0.4, 0), (0.45, 0.15), (0.45, 0.85), (0.3, 1.05)], 10, [M["pod2"], M["pod"], M["cobre"]])
# 250: bodega con ranura luminosa
torno_en(250, 1.55, [(0.5, 0), (0.55, 0.1), (0.55, 1.1), (0.45, 1.25)], 10, [M["pod2"], M["pod"], M["cobre"]])
caja_en(250, 1.55, (0, 1.0, 0.52), (0.35, 0.06, 0.02), M["cian"])
# ojo de buey junto a la bodega
caja_en(252, 1.78, (0, 1.6, 0.02), (0.4, 0.4, 0.02), M["ventana"])
caja_en(252, 1.79, (0, 1.6, 0.0), (0.5, 0.5, 0.02), M["cobre"])
# 315: escalera a la escotilla
for s in (-0.22, 0.22):
    caja_en(315, 1.5, (s, 1.2, 0), (0.05, 2.4, 0.05), M["escalera"])
for i in range(6):
    caja_en(315, 1.5, (0, 0.3 + i * 0.38, 0), (0.44, 0.04, 0.04), M["escalera"])
# escotilla del techo con su volante y lámparas
p.torno([(0.5, 2.59), (0, 2.6)], 12, M["alta2"], eje='y')
p.cajaJ((0, 2.56, 0), (0.3, 0.02, 0.03), M["negro"])
p.cajaJ((0, 2.56, 0), (0.03, 0.02, 0.3), M["negro"])
for i in range(3):
    a = i * 2.094 + 0.5
    p.cajaJ((math.cos(a) * 1.05, 2.58, math.sin(a) * 1.05), (0.3, 0.03, 0.12), M["lampara"])
# asiento central
p.cajaJ((0, 0.25, 0), (0.06, 0.5, 0.06), M["reja"])
p.cajaJ((0, 0.52, 0.05), (0.28, 0.05, 0.26), M["pod"])
p.cajaJ((0, 0.8, 0.3), (0.28, 0.26, 0.05), M["pod"])
p.cajaJ((0, 0.8, 0.33), (0.04, 0.24, 0.01), M["naranja"])
p.crear()
print("TRIS", triangulos())
