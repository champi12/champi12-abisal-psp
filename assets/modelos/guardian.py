"""Guardián del Vacío: cadena colosal de segmentos acorazados con cráneo plano y líneas
luminosas cian. Piezas: cráneo#1, segmentos#2-#7 (desde z = 0.06 hacia atrás, con aletas
en el primero), cola#8."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
HUESO = material("hueso", 0xb8b4a8)
HUESO2 = material("hueso2", 0x8a8678)
OSCURO = material("oscuro", 0x3a3c40)
OSCURO2 = material("oscuro2", 0x46484c)
MANDIBULA = material("mandibulas", 0x6a665c)
LUZ = material("luz", 0x5ae8ff, emision=5)
ALETA = material("aletas", 0x5a5850)
c = PiezaJ("craneo#1", [HUESO, HUESO2, OSCURO, MANDIBULA, LUZ])
c.cuad((-0.7, 0, 0), (0.7, 0, 0), (0.12, 0.05, 1.3), (-0.12, 0.05, 1.3), 1)
c.cuad((-0.7, 0, 0), (-0.12, 0.05, 1.3), (0, 0.38, 0.5), (-0.3, 0.32, 0.05), 0)
c.cuad((0.7, 0, 0), (0.3, 0.32, 0.05), (0, 0.38, 0.5), (0.12, 0.05, 1.3), 0)
c.tri((-0.3, 0.32, 0.05), (0.3, 0.32, 0.05), (0, 0.38, 0.5), 0)
c.cuad((-0.7, 0, 0), (-0.12, 0.05, 1.3), (0.12, 0.05, 1.3), (0.7, 0, 0), 2)
for s in (1, -1):                         # cuernos hacia atrás
    c.tri((s * 0.35, 0.3, 0.1), (s * 0.55, 0.6, -0.45), (s * 0.25, 0.32, -0.05), 1)
c.tri((-0.45, -0.02, 0.2), (-0.2, -0.02, 1.0), (-0.3, -0.38, 0.62), 3)
c.tri((0.45, -0.02, 0.2), (0.3, -0.38, 0.62), (0.2, -0.02, 1.0), 3)
for (x, y, z, w) in ((0.32, 0.13, 0.55, 0.08), (0.18, 0.19, 0.8, 0.06)):
    for s in (1, -1):
        c.elip((s * x, y, z), (w, w * 0.7, w * 1.2), 4, 2, 4)
c.cajaJ((0, 0.37, 0.45), (0.04, 0.02, 0.6), 4)
c.crear()
for k in range(6):
    r0, r1 = 0.42 - k * 0.05, 0.42 - (k + 1) * 0.05
    s = PiezaJ(f"segmento{k}#{k + 2}", [OSCURO if k % 2 == 0 else OSCURO2, HUESO, LUZ, ALETA])
    s.torno([(r0, 0.06), (r0 * 1.04, -0.4), (r1, -0.86)], 8, 0, centro=(0, 0.1, 0))
    s.cuad((-r0 * 0.8, 0.1 + r0 * 0.78, -0.05), (r0 * 0.8, 0.1 + r0 * 0.78, -0.05), (r1 * 0.8, 0.1 + r1 * 0.78, -0.75), (-r1 * 0.8, 0.1 + r1 * 0.78, -0.75), 1)
    s.tri((0, 0.1 + r0 * 1.0, -0.15), (0, 0.1 + r0 * 1.45, -0.55), (0, 0.1 + r0 * 0.95, -0.7), 1)   # espina dorsal
    s.cajaJ((0, 0.12 + r0 * 0.82, -0.78), (r1 * 1.4, 0.03, 0.04), 2)
    for sx in (1, -1):
        s.cajaJ((sx * r0 * 0.92, 0.1, -0.4), (0.03, 0.04, 0.5), 2)
    if k == 0:
        for sx in (1, -1):
            s.cuad((sx * r0, 0.1, -0.1), (sx * (r0 + 1.6), 0.05, -0.5), (sx * (r0 + 1.3), 0.05, -0.9), (sx * r0, 0.1, -0.7), 3)
            s.cajaJ((sx * (r0 + 1.2), 0.07, -0.55), (0.4, 0.02, 0.05), 2)
    s.crear()
t = PiezaJ("cola#8", [ALETA, LUZ])
t.cuad((0, 0.1, 0), (0, 0.6, -1.05), (0, 0.15, -0.75), (0, -0.4, -1.05), 0)
t.cajaJ((0, 0.52, -0.97), (0.03, 0.1, 0.1), 1)
t.cajaJ((0, -0.32, -0.97), (0.03, 0.1, 0.1), 1)
t.crear()
