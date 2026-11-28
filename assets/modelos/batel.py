"""El Batel: batiscafo pesado que sustituye a la Rémora en las profundidades, ~5 m.
Casco de presión grueso, dos tanques de lastre, costillas de refuerzo, cabina con aro de
cobre y hélice carenada. Piezas: hélice#1 (caja |x|,|y| < 0,6 y -2,8 < z < -2,56: gira
alrededor de z = -2,68) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
CASCO = material("casco", 0x3c4c54)
OXIDO = material("oxido", 0x8a6532)
CLARO = material("claro", 0xc4ced0)
TANQUE = material("tanques", 0x6c7c82)
CRISTAL = material("cabina", 0x142830, rugosidad=0.1)
COBRE = material("cobre", 0xc08a4a, metal=0.5)
COSTILLA = material("costillas", 0x2e3a40)
METAL = material("metal", 0x8a959c)
PALA = material("palas", 0x26313a)
FARO = material("faros", 0xfff6d8, emision=6)
LUZ = material("luz", 0x6ff0ff, emision=5)
p = PiezaJ("batel", [CASCO, OXIDO, CLARO, TANQUE, CRISTAL, COBRE, COSTILLA, METAL, PALA, FARO, LUZ])
p.torno([(0, 2.05), (0.6, 1.9), (1.2, 1.5), (1.6, 0.7), (1.55, -0.3), (1.15, -1.2), (0.55, -1.85), (0.3, -2.0)], 14,
        [1, 0, 2, 0, 0, 0, 1])
for k in range(3):                                                # costillas de refuerzo
    z = -1.15 + k * 1.05
    p.torno([(1.64 - abs(z - 0.3) * 0.12, z + 0.1), (1.64 - abs(z - 0.3) * 0.12, z - 0.1)], 14, 6)
for s in (1, -1):                                                 # tanques de lastre
    p.torno([(0, -1.75), (0.5, -1.6), (0.56, -1.2), (0.56, 1.15), (0.42, 1.5), (0, 1.62)], 10, [1, 3, 3, 3, 1],
            centro=(s * 1.5, -0.2, 0.1))
    for z in (-0.8, 0.0, 0.8):
        p.torno([(0.585, z + 0.05), (0.585, z - 0.05)], 10, 6, centro=(s * 1.5, -0.2, 0.1))
    p.cajaJ((s * 1.15, -0.1, 0.1), (0.3, 0.14, 1.8), 6)
p.elip((0, 0.55, 0.95), (0.75, 0.6, 0.85), 12, 6, 4)              # cabina
p.torno([(0.82, -0.05), (0.84, 0.05), (0.72, 0.12)], 14, 5, eje='y', centro=(0, 0.55, 0.95))
for i in range(8):                                                # remaches del aro
    a = i * math.tau / 8
    p.elip((math.cos(a) * 0.82, 0.62, 0.95 + math.sin(a) * 0.82), (0.04, 0.04, 0.04), 4, 2, 5)
p.torno([(0.62, -2.5), (0.62, -2.86), (0.5, -2.86), (0.5, -2.5)], 12, [7, 6, 7])       # carenado
p.torno([(0.3, -2.0), (0.25, -2.5), (0.12, -2.55)], 10, 6)
for i in range(4):
    a = i * math.tau / 4 + 0.4
    p.cajaJ((math.cos(a) * 0.45, math.sin(a) * 0.45, -2.52), (0.04, 0.04, 0.12), 7)
for s in (1, -1):
    p.cajaJ((s * 0.45, -0.3, 1.97), (0.16, 0.1, 0.08), 9)
    p.cajaJ((s * 0.9, 0.8, 0.0), (0.06, 0.12, 0.6), 7)                  # asideros
p.cajaJ((0.32, 1.35, 0.2), (0.03, 0.75, 0.03), 7)
p.elip((0.32, 1.75, 0.2), (0.05, 0.05, 0.05), 4, 2, 10)
for i in range(4):                                                # hélice de 4 palas
    a = i * math.tau / 4
    c, s_ = math.cos(a), math.sin(a)
    p.cuad((c * 0.07 - s_ * 0.06, s_ * 0.07 + c * 0.06, -2.64), (c * 0.46 - s_ * 0.1, s_ * 0.46 + c * 0.1, -2.72),
           (c * 0.46 + s_ * 0.1, s_ * 0.46 - c * 0.1, -2.64), (c * 0.07 + s_ * 0.06, s_ * 0.07 - c * 0.06, -2.72), 8)
p.torno([(0.08, -2.62), (0.08, -2.74), (0, -2.76)], 8, 7)
partir(p, [[-0.6, 0.6, -0.6, 0.6, -2.8, -2.56]], "batel")
print("TRIS", triangulos())
