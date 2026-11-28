"""Túnel de roca: arco de 130 grados (radio 11 m) por el que se nada de punta a punta, con
PARED GRUESA (de 2,2 a 2,95 m del eje, dentro de la pared de choque del juego), bocas con
reborde, bloques sobre el lomo y nódulos luminosos. Dos variantes: tunel#1, #2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
ROCAS = [material("arena1", 0xc4b088), material("arena2", 0xbaa67e), material("arena3", 0xb29e76), material("arena4", 0xc8b68e)]
DENTRO = material("dentro", 0x7a6a4c)
BORDE = material("borde", 0x9a885e)
LUZ = material("nodulos", 0x6affc0, emision=4)
MATS = ROCAS + [DENTRO, BORDE, LUZ]
R, ANG = 11.0, 65.0
ANILLOS, PERFIL = 17, 9
VERT = 1.45
for v, semilla in enumerate((1, 8)):
    rnd = random.Random(semilla)
    p = PiezaJ(f"tunel{v}#{v + 1}", MATS)
    def punto(k, j, rr):
        t = k / (ANILLOS - 1)
        th = math.radians(-ANG + 2 * ANG * t)
        ux, uz = math.sin(th), -math.cos(th)
        px, pz = R * ux, R + R * uz
        a = math.pi * j / (PERFIL - 1)
        c = math.cos(a) * rr
        return (px + ux * c, math.sin(a) * rr * VERT - 0.6, pz + uz * c)
    ri = [[2.2 * (1 + rnd.uniform(-0.04, 0.04)) for j in range(PERFIL)] for k in range(ANILLOS)]
    ro = [[2.92 * (1 + rnd.uniform(-0.03, 0.02)) for j in range(PERFIL)] for k in range(ANILLOS)]
    for k in (0, ANILLOS - 1):                   # las bocas: reborde más grueso
        for j in range(PERFIL):
            ri[k][j] *= 1.05
    interior = [[punto(k, j, ri[k][j]) for j in range(PERFIL)] for k in range(ANILLOS)]
    exterior = [[punto(k, j, ro[k][j]) for j in range(PERFIL)] for k in range(ANILLOS)]
    tono = lambda k, j: (k * 5 + j * 3 + semilla) % 4
    cascara(p, interior, exterior, lambda k, j: True, 4, tono, 5)
    # faldones hasta la arena a los dos lados (que no se vea el canto flotando)
    for k in range(ANILLOS - 1):
        for j in (0, PERFIL - 1):
            a0, a1 = exterior[k][j], exterior[k + 1][j]
            p.cuad(a0, a1, (a1[0], a1[1] - 1.0, a1[2]), (a0[0], a0[1] - 1.0, a0[2]), 5, doble=True)
    for i in range(6):                           # bloques sobre el lomo
        th = math.radians(-55 + i * 22)
        p.rocaJ((R * math.sin(th), 3.25, R - R * math.cos(th)), (1.3, 0.7, 1.1), semilla * 7 + i, 0.3, 1, i % 4)
    for i in range(8):                           # nódulos luminosos en las paredes de dentro
        th = math.radians(-56 + i * 16)
        lado = 1.95 if i % 2 == 0 else -1.95
        p.elip((R * math.sin(th) + math.sin(th) * lado, 0.2 + (i % 3) * 0.5, R - R * math.cos(th) - math.cos(th) * lado),
               (0.2, 0.12, 0.2), 5, 3, 6)
    p.crear()
print("TRIS", triangulos())
