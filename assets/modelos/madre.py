"""La Madre del Latido (historia) en estilo bloque: criatura colosal medio enterrada en la
fosa, ~70 m. El detalle lo pone la textura pixel art Texturas.piedra() que el juego
proyecta encima.

Piezas: "cuerpo#1" (cúpula escalonada de placas con venas luminosas), "tentaculo#2" (un
tentáculo: el juego dibuja seis alrededor de la cúpula y los mece) y "corazon#3" (el
ojo-corazón de la cima: late con el pulso).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
from abisal_deco import *
import random

limpiar()
PIEL = material("piel", 0x6a6274, rugosidad=0.7)
PLACA = material("placa", 0x8a8094, rugosidad=0.6)
VENA = material("vena", 0xff6a9a, emision=6.0)
VENA2 = material("vena2", 0xc08aff, emision=6.0)
CORAZON = material("corazon", 0xffd0e8, emision=8.0)
OSCURO = material("pliegue", 0x3a3442)
MATS = [PIEL, PLACA, VENA, VENA2, CORAZON, OSCURO]
M = {m.name: i for i, m in enumerate(MATS)}

# (radio, altura de abajo, altura de arriba) de cada piso de la cúpula
PISOS = [(33, -4, 6), (29.5, 6, 15), (22, 15, 22), (13, 22, 26.5), (6.5, 26.5, 28.6)]
c = PiezaD("cuerpo#1", MATS)
rnd = random.Random(7)
for k, (r, y0, y1) in enumerate(PISOS):
    n = 14 if k < 3 else 10
    ancho = 2 * math.pi * r / n * 1.08
    giro = k * 13
    r_sig = PISOS[k + 1][0] if k + 1 < len(PISOS) else 0
    fondo = r - r_sig + 2.5                  # each tier reaches under the next: no gaps
    for i in range(n):
        a = i * 360 / n + giro
        ra = math.radians(a)
        extra = rnd.uniform(-0.8, 1.2)       # uneven plates: less of a stepped cake
        cx, cz = math.sin(ra) * (r - fondo / 2), math.cos(ra) * (r - fondo / 2)
        bloque(c, (cx, (y0 + y1 + extra) / 2, cz), (ancho, y1 - y0 + 0.6 + extra, fondo),
               M["placa" if (i + k) % 2 == 0 else "piel"], a + rnd.uniform(-3, 3))
        if (i + k) % 2 == 0:                 # vena luminosa en la cara de la placa
            bloque(c, (math.sin(ra) * (r + 0.2), (y0 + y1) / 2, math.cos(ra) * (r + 0.2)),
                   (2.0, y1 - y0 + 0.8, 0.6), M["vena" if i % 4 == 0 else "vena2"], a)
        elif k < 3 and i % 3 == 0:           # mancha luminosa
            bloque(c, (math.sin(ra) * (r + 0.3), y0 + (y1 - y0) * 0.6, math.cos(ra) * (r + 0.3)),
                   (2.6, 2.6, 0.8), M["vena2" if i % 2 else "vena"], a)
# tapa de la cima con el pliegue alrededor del ojo
bloque(c, (0, 28.2, 0), (9, 1.4, 9), M["pliegue"], 22)
bloque(c, (0, 28.9, 0), (6.5, 0.6, 6.5), M["pliegue"], 0)
# la base enterrada: montículos de sedimento
for i in range(8):
    a = i * math.tau / 8 + 0.2
    roca(c, (math.cos(a) * 35, -2, math.sin(a) * 35), (8, 5, 3), 300 + i, M["pliegue"], 1)

# un tentáculo: tramos de caja que se afinan y se curvan hacia fuera (+x), con ventosas
t = PiezaD("tentaculo#2", MATS)
SEG = 6
prev = None
for s in range(SEG + 1):
    u = s / SEG
    cur = (8 * u * u, 18 * u, 0)
    if prev:
        g = 2 * (2.6 * (1 - (u - 0.5 / SEG)) + 0.3)
        palo(t, prev, cur, g, M["piel" if s % 2 else "pliegue"], 0, g * 0.9)
        if s < SEG:                          # ventosa luminosa en la cara de dentro
            bloque(t, (cur[0] - g * 0.5, cur[1], 0), (0.5, 0.9, 0.9), M["vena"])
    prev = cur
bloque(t, (8, 18.4, 0), (1.6, 1.6, 1.6), M["vena"], 45)        # punta

h = PiezaD("corazon#3", MATS)
h.cajaJ((0, 0, 0), (9, 4.4, 9), M["corazon"])
h.cajaJ((0, 0, 0), (11, 2.6, 6), M["corazon"])
h.cajaJ((0, 0, 0), (6, 2.6, 11), M["corazon"])
h.cajaJ((0, 1.6, 0), (3, 2.2, 3), M["vena"])                     # pupila
for p in (c, t):
    p.crear()
ob = h.crear()
ob.location = (0, 0, 29)
contar()
print("TRIS", triangulos())
