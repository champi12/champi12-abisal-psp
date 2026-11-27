"""Pastor de arena (praderas): gran pastador de caparazón de placas, ~2,5 m.
Piezas: 4 aletas (#1 der-delante, #2 izq-delante, #3 der-detrás, #4 izq-detrás), cabeza#5, cuerpo#6."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CAPA = material("caparazon", 0x6f7f5a)
PLACA = material("placas", 0x8a7a5a)
PIEL = material("piel", 0x9a8a64)
ALETA = material("aletas", 0x5e6a4a)
OJO = material("ojos", 0xa8ffb0, emision=4)
p = PiezaJ("pastor", [CAPA, PLACA, PIEL, ALETA, OJO])
p.torno([(0, -0.35), (0.85, -0.25), (1.08, 0.0), (0.95, 0.3), (0.55, 0.48), (0, 0.56)], 12, [0, 1, 0, 1, 0], eje='y', aplastar=(0.85, 1.0))
p.torno([(0.88, -0.3), (0.7, -0.38), (0, -0.4)], 12, 2, eje='y', aplastar=(0.85, 1.0))
for i in range(6):                     # crestas de las placas
    a = i * math.tau / 6 + 0.5
    p.cajaJ((math.cos(a) * 0.55, 0.42, math.sin(a) * 0.55 * 1.1), (0.35, 0.08, 0.14), 1, rot=(0, 0, math.degrees(a)))
p.elip((0, -0.05, 1.2), (0.33, 0.27, 0.32), 8, 4, 2)          # cabeza
p.torno([(0.14, 1.35), (0.11, 1.6), (0.07, 1.85), (0, 1.9)], 7, 2, centro=(0, -0.13, 0))   # hocico tamizador
for s in (1, -1):
    p.elip((s * 0.17, 0.06, 1.38), (0.05, 0.05, 0.05), 5, 3, 4)
for i in range(4):
    lado = 1 if i % 2 == 0 else -1
    z = 0.55 if i < 2 else -0.55
    x0 = lado * 0.98
    p.cuad((x0, -0.08, z + 0.18), (x0 + lado * 0.75, -0.18, z + 0.1), (x0 + lado * 0.8, -0.2, z - 0.12), (x0, -0.1, z - 0.18), 3)
    p.cuad((x0, -0.12, z + 0.18), (x0, -0.14, z - 0.18), (x0 + lado * 0.8, -0.23, z - 0.12), (x0 + lado * 0.75, -0.21, z + 0.1), 3)
partir(p, [[0.95, B, -B, B, 0, B], [-B, -0.95, -B, B, 0, B], [0.95, B, -B, B, -B, 0], [-B, -0.95, -B, B, -B, 0], [-B, B, -B, B, 1.0, B]], "pastor")
print("TRIS", triangulos())
