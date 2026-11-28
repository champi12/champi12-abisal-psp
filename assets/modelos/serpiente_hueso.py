"""Roehuesos (osario), estilo bloque: anguila pálida con placas de hueso, costillas que asoman
y ojos rojos. Cuatro segmentos."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("serpiente_hueso", ppm=18)
PIEL = E("escamas", 0x6e6258, 0x8a7e72)
def cabeza(p, paso, r):
    p.caja((0, 0.06, 0.22), (r * 1.6, r * 1.3, 0.45), {'todo': E("hueso", 0xe6dcc4, 0xb8a888)})
    p.caja((0, -0.1, 0.25), (r * 1.3, 0.07, 0.4), {'todo': E("liso", 0x3a1414)})
    for s in (1, -1):
        p.caja((s * r * 0.75, 0.15, 0.3), (0.03, 0.07, 0.07), {'todo': E("brillo", 0xff3a10, 0xa01000)})
        for k in range(3):
            p.caja((s * r * 0.5, -0.06, 0.15 + k * 0.1), (0.025, 0.07, 0.025), {'todo': E("liso", 0xfff4dc)})
def segmento(p, k, paso, rk):
    p.caja((0, 0.1 + rk, -paso / 2), (rk * 1.4, 0.06, paso * 0.85), {'todo': E("hueso", 0xe6dcc4, 0xb8a888)})
    for j in range(2):
        for s in (1, -1):
            p.caja((s * rk * 0.95, 0.1, -paso * (0.3 + j * 0.4)), (0.03, rk * 1.9, 0.03), {'todo': E("hueso", 0xeee4cc, 0xb8a888)})
serpiente(m, 3.8, 0.27, PIEL, E("liso", 0x8a7e72), cabeza, segmento)
m.crear()
