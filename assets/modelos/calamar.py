"""Acechador de algas (bosque): calamar emboscador de manto oliva moteado, volantes
foliáceos y seis brazos, ~2,4 m. Piezas: brazos#1-#6 (cada uno desde el anillo z = -0,55)
y manto#7."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
MANTO = material("manto", 0x4f6e30)
MANCHA = material("manchas", 0x3a5426)
VOLANTE = material("volantes", 0x7a9a3a)
BRAZO = material("brazos", 0x56702e)
OJO = material("ojos", 0xd8ff40, emision=5)
VENTOSA = material("ventosas", 0xc8d890)
p = PiezaJ("calamar", [MANTO, MANCHA, VOLANTE, BRAZO, OJO, VENTOSA])
p.torno([(0, -0.6), (0.3, -0.5), (0.37, -0.15), (0.36, 0.15), (0.3, 0.5), (0.18, 0.78), (0, 0.97)], 10, [0, 0, 1, 0, 0, 1])
rnd = random.Random(4)
for i in range(10):                     # manchas
    a = rnd.uniform(0, math.tau); z = rnd.uniform(-0.3, 0.6)
    p.elip((math.cos(a) * 0.35, math.sin(a) * 0.35, z), (0.07, 0.07, 0.09), 4, 2, 1)
for i in range(5):                      # volantes foliáceos ondulados
    z = -0.35 + i * 0.25
    for s in (1, -1):
        p.cuad((s * 0.33, 0, z + 0.08), (s * 0.64, 0.12, z - 0.05), (s * 0.6, 0.06, z - 0.2), (s * 0.31, 0, z - 0.14), 2)
for s in (1, -1):
    p.elip((s * 0.3, 0.13, -0.4), (0.08, 0.1, 0.11), 5, 3, 4)
cajas = []
for i in range(6):
    a = i * 1.047
    cx, cy = math.cos(a) * 0.2, math.sin(a) * 0.2
    xs = [cx, cx * 1.3, cx * 1.5, cx * 1.4]; ys = [cy, cy * 1.3, cy * 1.5, cy * 1.4]
    zs = [-0.55, -0.95, -1.35, -1.75]; rs = [0.075, 0.062, 0.042, 0.015]
    for j in range(3):
        n = 5
        A = [(xs[j] + math.cos(q * math.tau / n) * rs[j], ys[j] + math.sin(q * math.tau / n) * rs[j], zs[j]) for q in range(n)]
        Bq = [(xs[j + 1] + math.cos(q * math.tau / n) * rs[j + 1], ys[j + 1] + math.sin(q * math.tau / n) * rs[j + 1], zs[j + 1]) for q in range(n)]
        for q in range(n):
            w = (q + 1) % n
            p.cuad(A[q], A[w], Bq[w], Bq[q], 3)
        p.elip(((xs[j] + xs[j + 1]) / 2 * 0.92, (ys[j] + ys[j + 1]) / 2 * 0.92, (zs[j] + zs[j + 1]) / 2), (0.025, 0.025, 0.025), 4, 2, 5)
    x0 = min(cx, cx * 1.5) - 0.1; x1 = max(cx, cx * 1.5) + 0.1
    y0 = min(cy, cy * 1.5) - 0.1; y1 = max(cy, cy * 1.5) + 0.1
    cajas.append([x0, x1, y0, y1, -99, -0.58])
partir(p, cajas, "calamar")
print("TRIS", triangulos())
