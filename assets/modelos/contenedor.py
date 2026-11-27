"""Contenedor 7 (historia): contenedor de carga hundido en la meseta roja, ~6 m.

Piezas: "cuerpo#1" (casco ondulado, puerta cerrada, numero 7, franja de peligro, barro),
"puerta#2" (la puerta abierta, con la bisagra en su origen: se mece con la corriente) y
"luz#3" (luz del transpondedor, parpadea).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
OXIDO = material("oxido", 0xb06a34, rugosidad=0.8)
COSTILLA = material("costilla", 0x7c4824, rugosidad=0.8)
BLANCO = material("pintura", 0xe6e0d2)
AMARILLO = material("peligro", 0xe8c020)
NEGRO = material("hueco", 0x1a1410)
BARRO = material("barro", 0x7a4a36, rugosidad=1.0)
LUZ = material("luz", 0xffd040, emision=4.0)

L, AN, AL = 6.2, 2.6, 2.7   # largo (X), ancho (Y), alto (Z)
# (la inclinación del contenedor la pone el juego al dibujarlo)

c = Pieza("cuerpo#1", [OXIDO, COSTILLA, BLANCO, AMARILLO, NEGRO, BARRO])
# caja principal: paredes, con el extremo +X abierto (se ve el interior oscuro)
c.caja((0, 0, AL), (L, AN, 0.12), 0)                         # techo
c.caja((0, AN / 2, AL / 2), (L, 0.12, AL), 0)                # lados
c.caja((0, -AN / 2, AL / 2), (L, 0.12, AL), 0)
c.caja((-L / 2, 0, AL / 2), (0.12, AN, AL), 0)               # fondo
c.caja((0, 0, 0.06), (L, AN, 0.12), 0)                       # piso
c.caja((L / 2 - 0.3, 0, AL / 2), (0.05, AN - 0.2, AL - 0.2), 4)  # interior en sombra
# corrugado: costillas verticales en los lados y transversales en el techo
for i in range(8):
    x = -L / 2 + 0.45 + i * (L - 0.9) / 7
    for s in (1, -1):
        c.caja((x, s * (AN / 2 + 0.04), AL / 2), (0.14, 0.06, AL - 0.15), 1)
    c.caja((x, 0, AL + 0.05), (0.14, AN - 0.1, 0.05), 1)
# marcos de las esquinas
for x in (-L / 2, L / 2):
    for y in (-AN / 2, AN / 2):
        c.caja((x, y, AL / 2), (0.2, 0.2, AL + 0.05), 1)
# hoja de puerta cerrada (lado -Y del extremo abierto) con sus barras de cierre
c.caja((L / 2 + 0.05, AN / 4, AL / 2), (0.08, AN / 2 - 0.05, AL - 0.15), 0)
for k in (-0.25, 0.25):
    c.caja((L / 2 + 0.12, AN / 4 + k, AL / 2), (0.05, 0.05, AL - 0.3), 1)
# numero "7" grande en el costado +Y y franja de peligro
y7 = -AN / 2 - 0.08
c.caja((0.3, y7, AL - 0.5), (1.1, 0.03, 0.26), 2)
c.caja((0.58, y7, AL - 1.25), (0.26, 0.03, 1.3), 2, rot=(0, 18, 0))
c.caja((-1.9, y7, 0.45), (1.8, 0.03, 0.22), 3)
for i in range(5):
    c.caja((-2.6 + i * 0.36, y7 - 0.005, 0.45), (0.14, 0.03, 0.24), 4, rot=(0, 35, 0))
# barro acumulado contra la base y algunas piedras
for i, (x, y, sx) in enumerate(((-2.4, 1.45, 1.6), (0.6, 1.5, 1.3), (2.2, -1.45, 1.4), (-1.4, -1.5, 1.2),
                                 (3.5, 0.7, 1.0), (-3.4, -0.4, 0.9))):
    c.roca((x, y, -0.12), (sx, 0.6, 0.26), 40 + i, 0.2, 1, 5)
c.ruido(0.02, 7, lambda co: co.z > 0.2)  # chapa abollada
c.crear()

# puerta abierta: la bisagra en el origen del objeto (esquina +Y del extremo abierto)
p = Pieza("puerta#2", [OXIDO, COSTILLA])
# cerrada se extiende hacia +Y desde la bisagra (tapa la mitad -Y del hueco)
p.caja((0.05, (AN / 2 - 0.05) / 2, 0), (0.08, AN / 2 - 0.05, AL - 0.15), 0)
for k in (0.3, 0.9):
    p.caja((0.12, k, 0), (0.05, 0.05, AL - 0.3), 1)
p.caja((0.12, AN / 2 - 0.15, 0.4), (0.06, 0.1, 0.1), 1)   # manilla
ob = p.crear()
ob.location = (L / 2, -AN / 2 + 0.02, AL / 2)
ob.rotation_euler = (0, 0, math.radians(-105))

# luz del transpondedor sobre el techo, en el extremo cerrado
z = Pieza("luz#3", [LUZ, COSTILLA])
z.caja((0, 0, 0.12), (0.26, 0.26, 0.22), 0)
z.caja((0, 0, 0), (0.34, 0.34, 0.06), 1)
ob = z.crear()
ob.location = (-L / 2 + 0.5, 0, AL + 0.1)

print("TRIS", triangulos())
