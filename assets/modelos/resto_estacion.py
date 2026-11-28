"""Restos de la estación Halcyon: un contenedor volcado, una chapa del casco doblada (ahora
con grosor y nervios) y una viga rota, metal cepillado con marcas naranjas. A lo largo de x.
Dos variantes: resto#1, #2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
import random
METAL = material("metal", 0x9aa4aa)
METAL2 = material("metal2", 0x7c868c)
CLARO = material("chapa", 0xc8ced2)
FRANJA = material("franja", 0x1fa3a8)
NARANJA = material("naranja", 0xe0702c)
OSCURO = material("oscuro", 0x3e464c)
VIGA = material("viga", 0x6e767c)
MATS = [METAL, METAL2, CLARO, FRANJA, NARANJA, OSCURO, VIGA]
for v, semilla in enumerate((1, 3)):
    rnd = random.Random(semilla)
    p = PiezaJ(f"resto{v}#{v + 1}", MATS)
    # contenedor volcado con nervios
    p.cajaJ((-1.8, 0.62, 0), (2.6, 1.3, 1.3), 0, rot=(0, 0, 0))
    for i in range(5):
        p.cajaJ((-3.0 + i * 0.6, 0.62, 0), (0.08, 1.36, 1.36), 1)
    p.cajaJ((-1.8, 1.29, 0), (2.6, 0.03, 0.3), 4)
    p.cajaJ((-0.48, 0.62, 0), (0.04, 1.1, 1.1), 5)                     # puerta oscura
    # chapa del casco doblada: dos tramos con grosor de 8 cm y bordes
    g = 0.08
    def placa(a, b, c, d, mat):
        n = (0, 0, -1)
        off = lambda q: (q[0], q[1], q[2] - g)
        p.cuad(a, b, c, d, mat)
        p.cuad(off(d), off(c), off(b), off(a), 1)
        for (u, w) in ((a, b), (b, c), (c, d), (d, a)):
            p.cuad(w, u, off(u), off(w), 6)
    cx, cz = 1.6, 0.5
    def r(x, y, z):
        a = math.radians(25)
        return (cx + x * math.cos(a) + z * math.sin(a), 0.2 + y, cz - x * math.sin(a) + z * math.cos(a))
    placa(r(-1.2, 0, -0.8), r(1.2, 0, -0.8), r(1.2, 1.6, -0.3), r(-1.2, 1.6, -0.3), 2)
    placa(r(-1.2, 1.6, -0.3), r(1.2, 1.6, -0.3), r(1.0, 2.4, 0.6), r(-1.0, 2.4, 0.6), 2)
    p.cuad(r(-1.2, 0.6, -0.64), r(1.2, 0.6, -0.64), r(1.2, 0.9, -0.54), r(-1.2, 0.9, -0.54), 3)
    for x in (-0.8, 0.0, 0.8):                                         # nervios de la chapa
        p.cuad(r(x - 0.05, 0.05, -0.92), r(x + 0.05, 0.05, -0.92), r(x + 0.05, 1.6, -0.42), r(x - 0.05, 1.6, -0.42), 6, doble=True)
    # viga rota con su extremo astillado y unos tubos
    p.cajaJ((0.2, 0.25, -1.2), (3.4, 0.25, 0.25), 6, rot=(0, -12, -35))
    p.torno([(0.08, -1.6), (0.08, 1.4)], 6, 5, centro=(0.6, 0.12, -2.0))
    for k in range(3):
        p.rocaJ((rnd.uniform(-3, 3), 0.0, rnd.uniform(-1.8, 1.8)), (0.5, 0.25, 0.4), semilla * 9 + k, 0.3, 0, 5)
    p.crear()
print("TRIS", triangulos())
