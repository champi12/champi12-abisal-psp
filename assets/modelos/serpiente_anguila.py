"""Anguila de brasa (grietas termales), estilo bloque: negra con bandas de brasa que brillan,
aletas de fuego y ojos amarillos. Cuatro segmentos."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("serpiente_anguila", ppm=18)
PIEL = E("escamas", 0x1c181a, 0x2c2224)
def cabeza(p, paso, r):
    p.caja((0, 0.08, 0.2), (r * 1.6, r * 1.4, 0.4), {'todo': PIEL})
    for s in (1, -1):
        p.caja((s * r * 0.75, 0.16, 0.28), (0.03, 0.07, 0.07), {'todo': E("brillo", 0xffe070, 0xffa020)})
def segmento(p, k, paso, rk):
    for j in range(2):
        p.caja((0, 0.1, -paso * (0.3 + j * 0.4)), (rk * 1.85, rk * 2.05, 0.06), {'todo': E("brillo", 0xff8a2a, 0xd8401a)})
serpiente(m, 4.2, 0.27, PIEL, E("liso", 0x2a2224), cabeza, segmento, aleta=E("brillo", 0xff6a1a, 0xd8401a))
m.crear()
