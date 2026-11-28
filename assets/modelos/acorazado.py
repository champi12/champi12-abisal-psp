"""Acorazado (meseta y cañones): cangrejo gigante de caparazón óxido con franjas turquesa.
Piezas: cuerpo#1 (caparazón, vientre, ojos), pata#2 (cadera en el origen, hacia +z y abajo),
pinza#3 (hombro en el origen, hacia +z)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
OXIDO = material("oxido", 0x9a4e2e)
OXIDO2 = material("oxido2", 0xb05a32)
TURQ = material("turquesa", 0x2f8f8a)
VIENTRE = material("vientre", 0x6c4a3a)
OJO = material("ojos", 0x8fff9a, emision=5)
c = PiezaJ("cuerpo#1", [OXIDO, OXIDO2, TURQ, VIENTRE, OJO])
c.torno([(0.75, -0.15), (0.82, 0.05), (0.66, 0.28), (0.34, 0.44), (0, 0.5)], 8, [3, 2, 0, 1], eje='y', centro=(0, 0.55, 0), aplastar=(1.05, 0.9))
c.torno([(0, -0.35), (0.6, -0.18), (0.75, -0.15)], 8, 3, eje='y', centro=(0, 0.55, 0), aplastar=(1.05, 0.9))
for i in range(6):                       # pinchos del borde
    a = i * math.tau / 6 + 0.3
    x, z = math.cos(a) * 0.86, math.sin(a) * 0.74
    c.tri((x * 0.9, 0.62, z * 0.9 - 0.06), (x * 1.18, 0.66, z * 1.18), (x * 0.9, 0.62, z * 0.9 + 0.06), 1)
for s in (1, -1):
    c.torno([(0.03, 0), (0.025, 0.25)], 5, 3, eje='y', centro=(s * 0.16, 0.8, 0.6))
    c.elip((s * 0.16, 1.07, 0.62), (0.065, 0.065, 0.065), 5, 3, 4)
c.crear()
pa = PiezaJ("pata#2", [OXIDO, TURQ])
# muslo: baja hacia fuera; tibia: baja hasta el suelo
a1 = (0, 0, 0); a2 = (0, -0.25, 0.55); a3 = (0, -0.75, 0.85)
for (u, v, r0, r1, m) in ((a1, a2, 0.07, 0.06, 0), (a2, a3, 0.055, 0.02, 1)):
    n = 5
    def anillo(pc, r):
        return [(pc[0] + math.cos(q * math.tau / n) * r, pc[1] + math.sin(q * math.tau / n) * r * 0.8, pc[2]) for q in range(n)]
    A, Bq = anillo(u, r0), anillo(v, r1)
    for q in range(n):
        w = (q + 1) % n
        pa.cuad(A[q], A[w], Bq[w], Bq[q], m)
pa.elip(a2, (0.08, 0.08, 0.08), 5, 3, 0)
pa.crear()
pi = PiezaJ("pinza#3", [OXIDO, OXIDO2, TURQ])
pi.cajaJ((0, 0, 0.25), (0.16, 0.16, 0.5), 0)
pi.elip((0, 0.02, 0.72), (0.17, 0.12, 0.2), 6, 3, 1)
pi.cuad((-0.14, 0.06, 0.82), (0.14, 0.06, 0.82), (0.06, 0.1, 1.12), (-0.06, 0.1, 1.12), 1)       # dedo fijo
pi.cuad((-0.12, -0.06, 0.82), (-0.05, -0.12, 1.08), (0.05, -0.12, 1.08), (0.12, -0.06, 0.82), 2) # dedo móvil
pi.crear()
print("TRIS", triangulos())
