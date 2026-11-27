"""El Santuario (historia): un anillo de pilares tallados alrededor de un altar, ~20 m.

Piezas: "templo#1" (escalinata, pilares, dinteles, altar y glifos), "nucleo#2" (el
núcleo de luz sobre el altar: late) y "anillos#3" (tres aros de glifos que giran
alrededor del núcleo).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
PIEDRA = material("piedra", 0x5a5468, rugosidad=0.9)
PIEDRA2 = material("piedra2", 0x3e384c, rugosidad=1.0)
GLIFO = material("glifo", 0xb07aff, emision=3.0)
NUCLEO = material("nucleo", 0xe0c0ff, emision=8.0)

t = Pieza("templo#1", [PIEDRA, PIEDRA2, GLIFO])
# plataforma escalonada octogonal
t.cilindro((0, 0, 0.25), 11.5, 0.5, 8, 1)
t.cilindro((0, 0, 0.7), 10.5, 0.4, 8, 0)
# pilares: altos y bajos alternos, con capitel, banda de glifos y alguno roto
for i in range(8):
    a = i * math.tau / 8
    x, y = math.cos(a) * 9, math.sin(a) * 9
    h = 9 if i % 2 == 0 else 7
    if i == 5:
        h = 4.2                                  # un pilar caído...
        t.cilindro((x * 0.78, y * 0.78 + 1.5, 1.3), 0.75, 4.5, 6, 0, rot=(80, 0, math.degrees(a)))
    t.cilindro((x, y, 0.9 + h / 2), 0.85, h, 6, 0, radio2=0.7)
    t.cilindro((x, y, 0.9 + h + 0.25), 1.1, 0.5, 6, 1)
    t.cilindro((x, y, 0.9 + h * 0.55), 0.9, 0.22, 6, 2)
    t.caja((x, y, 0.9 + h * 0.3), (0.2, 0.2, 1.4), 2, rot=(0, 0, math.degrees(a)))
# dinteles entre los pilares altos
for i in range(0, 8, 2):
    a0, a1 = i * math.tau / 8, (i + 2) * math.tau / 8
    if i == 4:
        continue                                 # el que sostenía el pilar caído
    am = (a0 + a1) / 2
    lar = 2 * 9 * math.sin(math.pi / 4)
    t.caja((math.cos(am) * 9 * math.cos(math.pi / 4), math.sin(am) * 9 * math.cos(math.pi / 4), 10.3),
           (lar + 1.6, 1.0, 0.7), 1, rot=(0, 0, math.degrees(am) + 90))
# altar con escalones y un círculo de glifos alrededor
t.cilindro((0, 0, 1.3), 3.0, 0.8, 8, 1)
t.cilindro((0, 0, 2.0), 2.2, 0.6, 8, 0, radio2=1.6)
t.cilindro((0, 0, 0.92), 6.0, 0.05, 16, 2, tapas=False)
for i in range(8):
    a = i * math.tau / 8 + 0.39
    t.caja((math.cos(a) * 4.5, math.sin(a) * 4.5, 0.93), (0.9, 0.18, 0.04), 2, rot=(0, 0, math.degrees(a)))
t.crear()

n = Pieza("nucleo#2", [NUCLEO])
n.esfera((0, 0, 0), (0.85, 0.85, 0.85), 10, 6, 0)
ob = n.crear()
ob.location = (0, 0, 3.4)

r = Pieza("anillos#3", [GLIFO])
for k, (rr, inc) in enumerate(((1.5, 20), (1.9, -35), (2.3, 60))):
    v = r.cilindro((0, 0, 0), rr, 0.08, 14, 0, tapas=False, rot=(inc, 0, k * 50))
ob = r.crear()
ob.location = (0, 0, 3.4)
print("TRIS", triangulos())
