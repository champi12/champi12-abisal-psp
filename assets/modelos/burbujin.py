"""Burbujín (bajíos), estilo bloque: pez rechoncho turquesa con vientre violeta, cuatro ojos
en fila, un saco de gas luminoso en el lomo y tres aletas de cola a 120 grados.
Piezas: cuerpo#1, aletas#2 (laterales) y cola#3 (detrás de z = -0,23, gira sobre ese punto)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("burbujin", ppm=40)
c = m.pieza("cuerpo", 1)
PIEL = {'todo': E("escamas", 0x2fb8b0, 0x249a94), '-y': E("liso", 0x8a5ad8)}
c.caja((0, 0, 0.02), (0.4, 0.34, 0.42), PIEL)
c.caja((0, -0.01, 0.25), (0.34, 0.28, 0.12), PIEL)
c.caja((0, 0, -0.2), (0.26, 0.24, 0.12), PIEL)
for i in range(4):
    c.caja((-0.105 + i * 0.07, 0.07, 0.31), (0.05, 0.05, 0.02), {'todo': E("ojo", 0xe8f8ff, 0x1c3050)})
c.caja((0, -0.06, 0.34), (0.06, 0.06, 0.08), {'todo': E("liso", 0x2a7c78)})          # tubo para comer
c.caja((0, 0.21, -0.02), (0.16, 0.1, 0.2), {'todo': E("brillo", 0x9ff8e8, 0x3fd0b8)})   # saco de gas
a = m.pieza("aletas", 2)
for s in (1, -1):
    a.caja((s * 0.26, -0.02, 0.05), (0.14, 0.02, 0.12), {'todo': E("aleta", 0xf2702e, 0xffa060)}, rot=(0, 0, s * -20))
a.caja((0, -0.2, 0.05), (0.02, 0.1, 0.12), {'todo': E("aleta", 0xf2702e, 0xffa060)})
k = m.pieza("cola", 3)
k.caja((0, 0, -0.3), (0.1, 0.1, 0.12), PIEL)
for (rz) in (0, 120, 240):
    k.caja((0, 0.12, -0.42), (0.02, 0.2, 0.14), {'todo': E("aleta", 0xf2702e, 0xffb070)}, rot=(0, 0, rz),
           pivote=(0, 0, -0.42))
m.crear()
