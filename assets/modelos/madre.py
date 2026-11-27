"""La Madre del Latido (historia): criatura colosal medio enterrada en la fosa, ~70 m.

Piezas: "cuerpo#1" (cúpula de placas con venas luminosas), "tentaculo#2" (un tentáculo:
el juego dibuja seis alrededor de la cúpula y los mece) y "corazon#3" (el ojo-corazón
de la cima: late con el pulso).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
PIEL = material("piel", 0x6a6274, rugosidad=0.7)
PLACA = material("placa", 0x8a8094, rugosidad=0.6)
VENA = material("vena", 0xff5a8a, emision=3.0)
VENA2 = material("vena2", 0xb07aff, emision=3.0)
CORAZON = material("corazon", 0xffd0e8, emision=8.0)
OSCURO = material("pliegue", 0x3a3442)

PERFIL = [(34, -4), (32, 6), (26, 16), (16, 24), (6, 28), (0, 29)]

def radio(z):
    for (r0, z0), (r1, z1) in zip(PERFIL, PERFIL[1:]):
        if z <= z1:
            k = max(0, min(1, (z - z0) / (z1 - z0)))
            return r0 + (r1 - r0) * k
    return 0

c = Pieza("cuerpo#1", [PIEL, PLACA, VENA, VENA2, OSCURO])
# cúpula por anillos (16 lados), con placas alternas y pliegues entre ellas
import bmesh as _b
bm = c.bm
LADOS = 16
anillos = []
for (r, z) in PERFIL:
    fila = []
    for i in range(LADOS):
        a = i * math.tau / LADOS
        rr = r * (1 + 0.05 * math.sin(i * 3 + z))
        fila.append(bm.verts.new((math.cos(a) * rr, math.sin(a) * rr, z)))
    anillos.append(fila)
for k in range(len(anillos) - 1):
    for i in range(LADOS):
        j = (i + 1) % LADOS
        f = bm.faces.new((anillos[k][i], anillos[k][j], anillos[k + 1][j], anillos[k + 1][i]))
        f.material_index = 1 if (i + k) % 2 == 0 else 0
# venas: tiras luminosas que suben serpenteando
for i in range(10):
    a = i * math.tau / 10
    for k in range(5):
        z0, z1 = -3 + k * 5.6, -3 + (k + 1) * 5.6
        a0, a1 = a + k * 0.12, a + (k + 1) * 0.12
        r0, r1 = radio(z0) + 0.4, radio(z1) + 0.4
        w = 1.1
        p = [(math.cos(a0) * r0, math.sin(a0) * r0, z0), (math.cos(a1) * r1, math.sin(a1) * r1, z1),
             (math.cos(a1 + w / r1) * r1, math.sin(a1 + w / r1) * r1, z1),
             (math.cos(a0 + w / r0) * r0, math.sin(a0 + w / r0) * r0, z0)]
        c.quad(p, 2 + i % 2, doble=False)
# pliegues alrededor del ojo y la base enterrada (montículos)
c.cilindro((0, 0, 28.6), 6.5, 1.2, 12, 4, radio2=4.5)
for i in range(8):
    a = i * math.tau / 8 + 0.2
    c.roca((math.cos(a) * 35, math.sin(a) * 35, -2), (8, 5, 3), 300 + i, 0.25, 0, 4)
c.crear()

t = Pieza("tentaculo#2", [PIEL, VENA, OSCURO])
# un tentáculo que sale de la cúpula: segmentos que se afinan y se curvan hacia fuera (+X)
SEG, LADOS_T = 8, 6
prev = None
import bmesh
for s in range(SEG + 1):
    u = s / SEG
    r = 2.6 * (1 - u) + 0.3
    cx = 8 * u * u
    cz = 18 * u
    fila = [t.bm.verts.new((cx + math.cos(i * math.tau / LADOS_T) * r * 0.9,
                            math.sin(i * math.tau / LADOS_T) * r, cz)) for i in range(LADOS_T)]
    if prev:
        for i in range(LADOS_T):
            j = (i + 1) % LADOS_T
            f = t.bm.faces.new((prev[i], prev[j], fila[j], fila[i]))
            f.material_index = 2 if s % 2 == 0 else 0
    prev = fila
t.bm.faces.new(list(reversed(prev))).material_index = 1  # punta luminosa
for s in range(1, SEG, 2):  # ventosas luminosas en la cara interior
    u = s / SEG
    t.esfera((8 * u * u - (2.6 * (1 - u) + 0.3) * 0.85, 0, 18 * u), (0.35, 0.5, 0.5), 5, 3, 1)
t.crear()

h = Pieza("corazon#3", [CORAZON, VENA])
h.esfera((0, 0, 0), (4, 4, 2), 12, 5, 0)
h.esfera((0, 0, 1.4), (1.5, 1.5, 0.9), 8, 4, 1)   # pupila
ob = h.crear()
ob.location = (0, 0, 29)
print("TRIS", triangulos())
