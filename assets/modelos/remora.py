"""La Rémora: sumergible unipersonal, casco naranja en torpedo, cúpula oscura, dos góndolas
de propulsión laterales y hélice con anillo protector, ~5 m. Piezas: hélice#1 (la caja
|x|,|y| < 0,42 y -2,49 < z < -2,41: el juego la hace girar alrededor de z = -2,45) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
NARANJA = material("naranja", 0xe8692a)
NARANJA2 = material("naranja2", 0xf07a34)
BLANCO = material("blanco", 0xd8dcdf)
OSCURO = material("oscuro", 0x3a4048)
CRISTAL = material("cupula", 0x1c3a4c, rugosidad=0.1)
MARCO = material("marco", 0x5a646c)
METAL = material("metal", 0x7a858c)
PALA = material("palas", 0x2a2e33)
FARO = material("faros", 0xfff6d8, emision=6)
LUZ = material("luz", 0x6ff0ff, emision=5)
ROJO = material("roja", 0xff4030, emision=4)
p = PiezaJ("remora", [NARANJA, NARANJA2, BLANCO, OSCURO, CRISTAL, MARCO, METAL, PALA, FARO, LUZ, ROJO])
# casco en torpedo (12 lados): proa oscura, cuerpo naranja, popa blanca
p.torno([(0, 2.55), (0.4, 2.42), (0.7, 1.85), (0.85, 0.8), (0.86, -0.4), (0.76, -1.6), (0.46, -2.3), (0.2, -2.42)], 12,
        [3, 2, 0, 1, 0, 2, 3], aplastar=(1, 0.92))
# franjas y líneas de panel
for z in (1.3, -0.9):
    p.torno([(0.875, z + 0.05), (0.875, z - 0.05)], 12, 3, aplastar=(1, 0.92))
p.cajaJ((0, 0.8, -0.2), (0.12, 0.05, 2.2), 2)                     # lomo blanco
# cúpula de burbuja con su marco y bisagras
p.elip((0, 0.62, 0.85), (0.56, 0.46, 0.98), 10, 5, 4)
p.torno([(0.6, -0.02), (0.6, 0.04)], 12, 5, eje='y', centro=(0, 0.36, 0.85), aplastar=(1, 1.72))
p.cajaJ((0, 1.03, 0.8), (0.08, 0.06, 1.7), 5)                     # arco del marco
for s in (1, -1):
    p.cajaJ((s * 0.42, 0.62, 0.15), (0.05, 0.08, 0.12), 6)
# góndolas laterales con soporte, toberas y alerón
for s in (1, -1):
    p.cajaJ((s * 0.95, -0.1, -0.2), (0.5, 0.12, 0.65), 6)
    p.torno([(0.1, -1.2), (0.28, -1.0), (0.31, 0.3), (0.21, 0.62), (0, 0.72)], 8, [3, 2, 2, 0], centro=(s * 1.38, -0.1, -0.3))
    p.torno([(0.26, -1.2), (0.26, -1.32), (0.2, -1.32)], 8, 6, centro=(s * 1.38, -0.1, -0.3))
    p.cuad((s * 1.38, 0.18, -0.9), (s * 1.38, 0.42, -1.25), (s * 1.38, 0.2, -1.35), (s * 1.38, 0.16, -1.1), 0)
    p.cajaJ((s * 1.38, -0.1, 0.38), (0.12, 0.08, 0.05), 8)          # faro de góndola
# deriva vertical y estabilizadores traseros
p.cuad((0, 0.6, -1.5), (0, 1.0, -2.05), (0, 0.55, -2.3), (0, 0.4, -1.9), 0)
for s in (1, -1):
    p.cuad((s * 0.5, 0, -1.7), (s * 1.0, -0.05, -2.15), (s * 0.85, -0.05, -2.32), (s * 0.4, 0, -2.2), 0)
# anillo protector de la hélice (en z -2.45, fuera de la caja de la hélice: radio > 0.42)
p.torno([(0.76, -2.3), (0.76, -2.6), (0.66, -2.6), (0.66, -2.3)], 12, [6, 3, 6])
for i in range(3):
    a = i * math.tau / 3 + 0.5
    p.cajaJ((math.cos(a) * 0.55, math.sin(a) * 0.55, -2.32), (0.04, 0.04, 0.18), 6)
p.torno([(0.14, -2.3), (0.1, -2.4)], 8, 3)                       # buje (fijo)
# faros delanteros, antena con luz cian, escotilla y luz roja de posición
for s in (1, -1):
    p.cajaJ((s * 0.35, -0.25, 2.27), (0.18, 0.12, 0.1), 8)
p.cajaJ((0.3, 0.9, 0.2), (0.03, 0.7, 0.03), 6)
p.elip((0.3, 1.27, 0.2), (0.05, 0.05, 0.05), 4, 2, 9)
p.torno([(0.35, 0), (0.35, 0.08), (0.25, 0.14), (0, 0.16)], 10, [2, 2, 2], eje='y', centro=(0, 0.92, -0.6))
p.cajaJ((0, 1.1, -0.6), (0.16, 0.04, 0.06), 0)
p.elip((0, 0.62, 0), (0.05, 0.05, 0.05), 4, 2, 9)
p.elip((0, 0.95, -1.95), (0.05, 0.05, 0.05), 4, 2, 10)
# hélice de 3 palas torcidas (cae en la caja de la hélice)
for i in range(3):
    a = i * math.tau / 3
    c, s_ = math.cos(a), math.sin(a)
    p.cuad((c * 0.08 - s_ * 0.06, s_ * 0.08 + c * 0.06, -2.43), (c * 0.6 - s_ * 0.1, s_ * 0.6 + c * 0.1, -2.47),
           (c * 0.6 + s_ * 0.08, s_ * 0.6 - c * 0.08, -2.43), (c * 0.08 + s_ * 0.05, s_ * 0.08 - c * 0.05, -2.47), 7)
p.torno([(0.09, -2.42), (0.09, -2.47), (0, -2.48)], 8, 6)
partir(p, [[-0.42, 0.42, -0.42, 0.42, -2.49, -2.41]], "remora")
print("TRIS", triangulos())
