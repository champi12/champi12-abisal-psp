"""Cueva pequeña: cúpula de roca de ~7 m y 3 m de alto, abierta hacia +z, con PARED GRUESA
(entre el 93 % y el 112 % del radio, dentro de la pared de choque del juego), borde de roca en
la boca, nódulos luminosos dentro y bloques sobre el techo. Tres variantes: cueva1#1..#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
ROCAS = [material("roca1", 0x857f74), material("roca2", 0x7a746a), material("roca3", 0x726c62), material("roca4", 0x8c857a)]
DENTRO = material("dentro", 0x4a463f)
BORDE = material("borde", 0x7a7468)
LUZ = material("nodulos", 0x7affc8, emision=4)
MATS = ROCAS + [DENTRO, BORDE, LUZ]
RX, RY = 3.6, 3.0
SEG, AN = 16, 6
for v, semilla in enumerate((1, 6, 11)):
    rnd = random.Random(semilla)
    p = PiezaJ(f"cueva{v}#{v + 1}", MATS)
    ruido_in = [[rnd.uniform(-0.03, 0.03) for _ in range(SEG + 1)] for _ in range(AN + 1)]
    ruido_out = [[rnd.uniform(-0.05, 0.04) for _ in range(SEG + 1)] for _ in range(AN + 1)]
    for j in range(AN + 1):
        ruido_in[j][SEG] = ruido_in[j][0]
        ruido_out[j][SEG] = ruido_out[j][0]
    def punto(j, i, k):
        e = math.pi / 2 * j / AN
        a = 2 * math.pi * i / SEG
        return (math.cos(e) * math.sin(a) * RX * k, math.sin(e) * RY * k - 0.4, math.cos(e) * math.cos(a) * RX * k)
    interior = [[punto(j, i, 0.93 + ruido_in[j][i]) for i in range(SEG + 1)] for j in range(AN + 1)]
    exterior = [[punto(j, i, 1.11 + ruido_out[j][i]) for i in range(SEG + 1)] for j in range(AN + 1)]
    # la boca: celdas cuyo acimut está a menos de ~40 grados de +z, en la parte baja
    def cerrada(j, i):
        a = 360.0 * (i + 0.5) / SEG
        a = a - 360 if a > 180 else a
        return not (abs(a) < 42 and j < 4)
    tono = lambda j, i: (i * 7 + j * 3 + semilla) % 4
    cascara(p, interior, exterior, cerrada, 4, tono, 5, borde_abajo=False)
    # bloques sobre el techo y en la boca
    for k in range(3):
        a = rnd.uniform(0.6, 5.6)
        p.rocaJ((math.sin(a) * 1.5, 2.35, math.cos(a) * 1.5), (0.9, 0.6, 0.45), semilla * 3 + k, 0.3, 1, k % 4)
    for s in (1, -1):
        p.rocaJ((s * 1.9, 0.0, 3.0), (0.7, 0.6, 0.5), semilla * 5 + s, 0.3, 0, 1)
    # nódulos luminosos en la pared interior (al fondo)
    for k in range(5):
        a = math.pi * (0.65 + k * 0.17)
        r = 3.25
        p.elip((math.sin(a) * r, 0.3 + k * 0.32, math.cos(a) * r), (0.17, 0.13, 0.17), 5, 3, 6)
    p.crear()
print("TRIS", triangulos())
