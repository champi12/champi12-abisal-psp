"""Lanza vítrea (arrecife de cristal), estilo bloque: serpiente de vidrio (el juego la dibuja
traslúcida) con la espina luminosa por dentro y un hocico de lanza. Cuatro segmentos."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("serpiente_vitrea", ppm=18)
PIEL = E("placas", 0x9fe4ff, 0x6ab8e8)
def cabeza(p, paso, r):
    p.caja((0, 0.1, 0.25), (r * 1.2, r * 1.2, 0.5), {'todo': PIEL})
    p.caja((0, 0.1, 0.75), (0.06, 0.06, 0.6), {'todo': E("liso", 0xe8fcff)})
    for s in (1, -1):
        p.caja((s * r * 0.6, 0.16, 0.3), (0.03, 0.06, 0.06), {'todo': E("brillo", 0xff3a6a, 0xa01030)})
def segmento(p, k, paso, rk):
    p.caja((0, 0.12, -paso / 2), (0.06, 0.06, paso * 1.05), {'todo': E("brillo", 0xe0fbff, 0x80e0ff)})
serpiente(m, 4.0, 0.22, PIEL, E("liso", 0xc8f4ff), cabeza, segmento, aleta=E("aleta", 0xc8f4ff, 0x7fd8ff))
m.crear()
