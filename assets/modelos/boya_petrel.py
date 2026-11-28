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
for s in (1, -1):                                                   # dos flotadores
    p.torno([(0, -3.0), (0.9, -2.6), (1.2, 0), (0.9, 2.6), (0, 3.0)], 10, 0, eje='z', centro=(s * 2.2, 0.1, 0))
p.cajaJ((0, 0.8, 0), (4.4, 0.25, 2.4), 2)                            # cubierta
p.cajaJ((0, 0.95, 0), (2.0, 0.05, 2.0), 1)
p.torno([(0.12, 0), (0.08, 6.0)], 6, 2, eje='y', centro=(0, 0.9, 0))  # mástil
p.cajaJ((0, 5.2, 0), (1.6, 0.06, 0.06), 2)
p.torno([(0.5, 0), (0.5, 0.15), (0, 0.6)], 8, 1, eje='y', centro=(0, 6.0, 0))
# cápsula de embarque colgando bajo la cubierta (se entra desde abajo)
p.torno([(0, -1.0), (0.9, -0.7), (1.0, 0.6), (0.6, 1.2), (0, 1.3)], 10, 1, eje='y', centro=(0, -3.4, 0))
p.cajaJ((0, -2.2, 0.95), (0.8, 0.8, 0.05), 5)
p.tubo([(0, 0.7, 0, 0.08), (0, -2.1, 0, 0.08)], 5, 3)
p.crear()
z = PiezaJ("luz#2", MATS)
z.elip((0, 6.75, 0), (0.25, 0.25, 0.25), 8, 5, 4)
z.crear()
print("TRIS", triangulos())
