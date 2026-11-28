"""Errante de arrecife, estilo bloque: deriva gigante (~20 m) azul pizarra con percebes, un
jardín en el lomo y puntos luminosos en el vientre. Piezas: aletas #1 der-delante, #2
izq-delante, #3 der-detrás, #4 izq-detrás, cola#5, cuerpo#6. Los parches de hierba del lomo
siguen en Modelos.PARCHE_X/Z (allí se esconden mordedores)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("errante", ppm=3.2)
PIZ = {'todo': E("placas", 0x4a5a6a, 0x3a4858), '-y': E("puntos", 0x6a7a8a, 0x9ff8ff)}
ALE = {'todo': E("aleta", 0x4a5a6a, 0x6a7a8a)}
for (n, s, z) in ((1, 1, 3.5), (2, -1, 3.5), (3, 1, -2.4), (4, -1, -2.4)):
    a = m.pieza("aleta", n)
    if z > 0:
        a.caja((s * 5.8, -0.9, z), (5.0, 0.6, 3.6), ALE)
    else:
        a.caja((s * 4.5, 0, z), (2.8, 0.5, 6.2), ALE)
c = m.pieza("cola", 5)
c.caja((0, 0.1, -11.2), (9, 0.3, 2.4), ALE)
b = m.pieza("cuerpo", 6)
b.caja((0, 0.4, 0), (6.6, 4.6, 19), PIZ)
b.caja((0, 0.4, 0), (5.4, 5.6, 16), PIZ)
b.caja((0, 0.2, 9.6), (4.6, 3.6, 1.6), PIZ)
for s in (1, -1):
    b.caja((s * 2.6, 1.4, 9.9), (0.6, 0.6, 0.6), {'todo': E("ojo", 0xd8e8f0, 0x101820)})
import random
r = random.Random(8)
for k in range(14):                                         # percebes
    b.caja((r.choice((-1, 1)) * 3.3, r.uniform(-1, 2), r.uniform(-8, 8)), (0.3, 0.4, 0.4), {'todo': E("hueso", 0xd8d0c0, 0xa8a090)})
JARDIN = [(0xff8a6a, 0xd85a4a), (0xf0d060, 0xc0a040), (0x9a6ad8, 0x6a4aa8), (0x6ad890, 0x40a060)]
for k in range(18):                                         # el jardín del lomo
    x, z = r.uniform(-2.3, 2.3), r.uniform(-7, 7)
    h = r.uniform(0.5, 1.4)
    c1, c2 = JARDIN[k % 4]
    b.caja((x, 3.25 + h / 2, z), (0.3, h, 0.3), {'todo': E("brillo" if k % 3 == 0 else "franjas", c1, c2)})
for k in range(6):                                          # parches de hierba
    b.caja((r.uniform(-2, 2), 3.3, r.uniform(-6, 6)), (1.4, 0.3, 1.4), {'todo': E("franjas", 0x5a8a3a, 0x3a6a2a)})
m.crear()
