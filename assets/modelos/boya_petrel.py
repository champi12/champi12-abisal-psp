"""Boya de encuentro de la Petrel (llanura exterior, en la superficie): la nave de rescate deja
caer una boya de aterrizaje grande, con flotadores naranjas, un mástil con antena y una
cápsula de embarque colgando. Piezas: "boya#1" (flota, su origen es la línea del agua) y
"luz#2" (la baliza del mástil, parpadea)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
NARANJA = material("naranja", 0xff7a1a)
BLANCO = material("blanco", 0xe8ecf0)
GRIS = material("gris", 0x5a6470, metal=0.3)
NEGRO = material("negro", 0x1a1c20)
LUZ = material("luz", 0xff3030, emision=6)
VIDRIO = material("vidrio", 0x7ad0ff, emision=1.0)
MATS = [NARANJA, BLANCO, GRIS, NEGRO, LUZ, VIDRIO]
p = PiezaJ("boya#1", MATS)
for s in (1, -1):                                                   # dos flotadores en bloque
    x = s * 2.2
    p.cajaJ((x, 0.1, 0), (2.0, 1.6, 5.4), 0)
    p.cajaJ((x, 0.1, 0), (1.5, 2.3, 5.0), 0)
    for zz in (2.9, -2.9):
        p.cajaJ((x, 0.1, zz), (1.3, 1.3, 0.6), 0)
    p.cajaJ((x, 0.1, 0), (2.06, 0.3, 5.46), 1)                      # franja blanca
p.cajaJ((0, 0.8, 0), (4.4, 0.25, 2.4), 2)                           # cubierta
p.cajaJ((0, 0.95, 0), (2.0, 0.05, 2.0), 1)
p.cajaJ((0, 3.9, 0), (0.2, 6.0, 0.2), 2)                            # mástil
p.cajaJ((0, 5.2, 0), (1.6, 0.08, 0.08), 2)
p.cajaJ((0, 6.1, 0), (0.9, 0.2, 0.9), 1)
p.cajaJ((0, 6.3, 0), (0.5, 0.2, 0.5), 1)
# cápsula de embarque colgando bajo la cubierta (se entra desde abajo)
p.cajaJ((0, -3.4, 0), (1.9, 2.0, 1.6), 1)
p.cajaJ((0, -3.4, 0), (1.6, 2.0, 1.9), 1)
p.cajaJ((0, -2.2, 0), (1.2, 0.4, 1.2), 1)
p.cajaJ((0, -4.5, 0), (1.0, 0.3, 1.0), 3)                           # escotilla de abajo
p.cajaJ((0, -3.2, 0.96), (0.8, 0.8, 0.05), 5)                       # ventana
p.cajaJ((0, -0.7, 0), (0.16, 2.8, 0.16), 3)                         # cable
p.crear()
z = PiezaJ("luz#2", MATS)
z.cajaJ((0, 6.75, 0), (0.45, 0.45, 0.45), 4)
z.crear()
print("TRIS", triangulos())
