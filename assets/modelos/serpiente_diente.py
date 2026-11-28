"""Dientesierra (meseta roja), estilo bloque: serpiente de 4,5 m, roja y parda, con una
cresta de dientes de sierra en el lomo y una boca llena de dientes. Cuatro segmentos."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("serpiente_diente", ppm=18)
PIEL = E("escamas", 0x8a3424, 0xc0603a)
def cabeza(p, paso, r):
    p.caja((0, 0.05, 0.22), (r * 1.6, r * 1.3, 0.45), {'todo': PIEL, '-y': E("liso", 0xd8a070)})
    p.caja((0, -0.12, 0.25), (r * 1.4, 0.08, 0.4), {'todo': E("liso", 0x3a0e0a)})
    for s in (1, -1):
        p.caja((s * r * 0.75, 0.16, 0.32), (0.03, 0.08, 0.08), {'todo': E("ojo", 0xffd040, 0x101010)})
        for k in range(4):
            p.caja((s * r * 0.55, -0.08, 0.1 + k * 0.1), (0.03, 0.08, 0.03), {'todo': E("liso", 0xf0e8d0)})
def segmento(p, k, paso, rk):
    for j in range(3):
        p.caja((0, 0.1 + rk + 0.06, -paso * (0.2 + j * 0.3)), (0.04, 0.16, 0.12), {'todo': E("hueso", 0xe8d8b0, 0xb09070)},
               rot=(-30, 0, 0))
serpiente(m, 4.5, 0.3, PIEL, E("liso", 0xd8a070), cabeza, segmento)
m.crear()
