"""Errante de arrecife: deriva gigante de ~20 m, azul pizarra con percebes y un jardín en el
lomo (abanicos, tubos, anémonas, algas) y puntos luminosos en el vientre. Piezas: aletas
#1 der-delante, #2 izq-delante, #3 der-detrás, #4 izq-detrás, cola#5, cuerpo#6.
Los parches de hierba del lomo coinciden con Modelos.PARCHE_X/Z (allí se esconden mordedores)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
P1 = material("piel", 0x46586a)
P2 = material("piel2", 0x56687a)
P3 = material("vientre", 0x7a8a98)
PERCEBE = material("percebes", 0xb8b0a0)
OJO = material("ojos", 0x101418)
LUZ = material("luces", 0x7ff0ff, emision=5)
ALETA = material("aletas", 0x3e5062)
HIERBA = material("hierba", 0x6a7a28)
HIERBA2 = material("hierba2", 0x9a7a2a)
CORAL = material("coral", 0xe05a8a)
CORAL2 = material("coral2", 0xd8a040)
ANEMONA = material("anemonas", 0x6affe0, emision=3)
p = PiezaJ("errante", [P1, P2, P3, PERCEBE, OJO, LUZ, ALETA, HIERBA, HIERBA2, CORAL, CORAL2, ANEMONA])
def radio(z):
    t = (z + 10.5) / 20.8
    t = max(0, min(1, t))
    return 3.5 * math.sin(t * math.pi) * (1 if t < 0.55 else 1 - (t - 0.55) * 0.5)
perfil = [(0, -10.5), (1.2, -9), (2.6, -6), (3.4, -2), (3.5, 1), (3.0, 5), (2.0, 8), (0.8, 9.8), (0, 10.3)]
p.torno(perfil, 14, [0, 0, 1, 1, 0, 1, 2, 2])
rnd = random.Random(501)
for i in range(14):
    z = -6 + i * 1.0
    a = rnd.uniform(0.4, 2.7)
    r = radio(z)
    p.elip((math.cos(a) * r * 0.95, math.sin(a) * r * 0.95, z), (0.2, 0.14, 0.2), 5, 2, 3)
for s in (1, -1):
    p.elip((s * 1.75, 0.4, 8.2), (0.26, 0.3, 0.3), 6, 3, 4)
p.cajaJ((0, -0.6, 9.4), (1.2, 0.12, 0.6), 4)
for i in range(8):
    z = -6 + i * 1.8
    for s in (1, -1):
        p.elip((s * 1.2, -radio(z) * 0.88, z), (0.16, 0.08, 0.16), 4, 2, 5)
p.cuad((3.2, -0.5, 5.5), (8.5, -1.2, 3.5), (8.0, -1.3, 1.5), (3.2, -0.6, 2.5), 6)
p.cuad((-3.2, -0.5, 5.5), (-3.2, -0.6, 2.5), (-8.0, -1.3, 1.5), (-8.5, -1.2, 3.5), 6)
p.cuad((3.2, -0.6, -3.0), (6.0, -1.0, -4.5), (5.6, -1.0, -5.8), (3.2, -0.6, -5.0), 6)
p.cuad((-3.2, -0.6, -3.0), (-3.2, -0.6, -5.0), (-5.6, -1.0, -5.8), (-6.0, -1.0, -4.5), 6)
p.cuad((0, 0, -10.0), (4.5, 0.2, -12.5), (0, 0.1, -11.2), (-4.5, 0.2, -12.5), 6)
PX = [-1.2, 1.1, 0.2, -0.9, 1.3, -0.1]
PZ = [-4.5, -2.5, -0.5, 2.0, 4.0, 6.5]
for pch in range(6):                    # parches de hierba alta
    px, pz = PX[pch], PZ[pch]
    for i in range(22):
        a = rnd.uniform(0, math.tau); d = math.sqrt(rnd.random())
        x, z = px + math.cos(a) * d, pz + math.sin(a) * d
        rz = radio(z)
        y = math.sqrt(max(0, rz * rz - x * x)) - 0.05
        h = 0.65 + rnd.random() * 0.45
        ang = rnd.uniform(0, math.tau)
        bx, bz = math.cos(ang) * 0.16, math.sin(ang) * 0.16
        inc = (rnd.random() - 0.5) * 0.4
        p.tri((x - bx, y, z - bz), (x + bx, y, z + bz), (x + inc, y + h, z + inc * 0.5), 7 + i % 2)
for i in range(22):                     # jardín: abanicos, tubos, anémonas, algas
    z = -5.5 + rnd.random() * 12.5
    x = (rnd.random() - 0.5) * 4.4
    if any((x - PX[k]) ** 2 + (z - PZ[k]) ** 2 < 1.3 ** 2 for k in range(6)):
        continue
    rz = radio(z)
    y = math.sqrt(max(0, rz * rz - x * x)) - 0.08
    tipo = i % 5
    if tipo == 0:
        a = rnd.uniform(0, math.pi)
        p.tri((x, y, z), (x - math.cos(a) * 0.5, y + 0.9, z - math.sin(a) * 0.5), (x + math.cos(a) * 0.5, y + 0.9, z + math.sin(a) * 0.5), 9)
    elif tipo == 1:
        p.torno([(0.1, 0), (0.12, 0.55), (0.08, 0.6)], 5, 10, eje='y', centro=(x, y, z))
        p.torno([(0.08, 0), (0.09, 0.4), (0.06, 0.44)], 5, 10, eje='y', centro=(x + 0.18, y, z + 0.1))
    elif tipo == 2:
        for k in range(3):
            p.torno([(0.04, 0), (0.05, 0.3 + k * 0.08), (0, 0.36 + k * 0.08)], 4, 11, eje='y', centro=(x + (k - 1) * 0.12, y, z + (k % 2) * 0.1))
    elif tipo == 3:
        a = rnd.uniform(0, math.tau)
        p.cuad((x - 0.08, y, z), (x + 0.08, y, z), (x + 0.18 * math.cos(a), y + 1.8, z + 0.25), (x, y + 1.8, z + 0.25), 7)
    else:
        p.elip((x, y, z), (0.25, 0.18, 0.25), 6, 2, 10)
partir(p, [[3.15, 99, -99, 99, 0, 99], [-99, -3.15, -99, 99, 0, 99], [3.15, 99, -99, 99, -99, 0], [-99, -3.15, -99, 99, -99, 0], [-99, 99, -99, 0.25, -99, -10.2]], "errante")
