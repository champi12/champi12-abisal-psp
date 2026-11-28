"""Escupidor de cueva, estilo bloque: pez globo acorazado de cueva, cuerpo redondeado de placas
pardo verdosas con vientre claro, hocico en tubo con anillos y boca oscura, dos sacos de ácido
que brillan a los lados, púas en el lomo, ojos saltones y aletas. Piezas: tubo de la boca#1
(z > 0,38), cola#2 (z < -0,34), cuerpo#3."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("escupidor", ppm=44)
ARM = {'todo': E("placas", 0x6a6a48, 0x4a4a30), '-y': E("liso", 0xc8c098)}
ACI = {'todo': E("brillo", 0xc8ff60, 0x50b010)}
t = m.pieza("boca", 1)
t.caja((0, -0.02, 0.45), (0.2, 0.2, 0.16), ARM)
t.caja((0, -0.02, 0.53), (0.24, 0.24, 0.04), {'todo': E("casco", 0x8a8a60, 0x5a5a3a)})       # anillo
t.caja((0, -0.02, 0.58), (0.17, 0.17, 0.08), ARM)
t.caja((0, -0.02, 0.62), (0.12, 0.12, 0.02), {'todo': E("liso", 0x102008)})                   # boca
c = m.pieza("cola", 2)
c.caja((0, 0.02, -0.4), (0.18, 0.2, 0.14), ARM)
c.caja((0, 0.1, -0.54), (0.03, 0.24, 0.16), {'todo': E("aleta", 0x6a6a48, 0xa8a870)}, rot=(30, 0, 0), pivote=(0, 0, -0.46))
c.caja((0, -0.06, -0.54), (0.03, 0.24, 0.16), {'todo': E("aleta", 0x6a6a48, 0xa8a870)}, rot=(-30, 0, 0), pivote=(0, 0, -0.46))
b = m.pieza("cuerpo", 3)
# cuerpo redondeado: un núcleo y tres cajas que sobresalen por cada eje (contorno octogonal)
b.caja((0, 0, 0.02), (0.5, 0.44, 0.56), ARM)
b.caja((0, 0, 0.02), (0.66, 0.28, 0.4), ARM)
b.caja((0, 0, 0.02), (0.34, 0.58, 0.4), ARM)
b.caja((0, 0, 0.02), (0.34, 0.28, 0.72), ARM)
b.caja((0, -0.29, 0.02), (0.3, 0.02, 0.36), {'todo': E("liso", 0xc8c098)})              # vientre claro
for s in (1, -1):
    b.caja((s * 0.33, -0.1, -0.02), (0.08, 0.16, 0.24), ACI)    # sacos de ácido
    b.caja((s * 0.2, 0.14, 0.33), (0.12, 0.12, 0.06), {'todo': E("brillo", 0xffe060, 0xd08a10)})   # ojos
    b.caja((s * 0.2, 0.14, 0.365), (0.05, 0.07, 0.02), {'todo': E("liso", 0x101008)})              # pupila
    b.caja((s * 0.36, 0.06, 0.12), (0.16, 0.02, 0.14), {'todo': E("aleta", 0x6a6a48, 0xa8a870)}, rot=(0, 0, s * -25))
for k in range(4):                                             # púas del lomo
    b.caja((0, 0.33, 0.18 - k * 0.12), (0.03, 0.1 + (k % 2) * 0.04, 0.03), {'todo': E("hueso", 0xe8e0c0, 0xa89870)})
m.crear()
