"""Rondador abisal, estilo bloque: globo negro con boca enorme, dientes de aguja y señuelo
luminoso en una caña. Piezas: señuelo#1 (y > 0,5, z > 0,25) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("rondador", ppm=32)
NEG = {'todo': E("puntos", 0x141418, 0x24242c)}
s1 = m.pieza("senuelo", 1)
s1.caja((0, 0.75, 0.55), (0.04, 0.04, 0.6), {'todo': E("liso", 0x2a2a34)})
s1.caja((0, 0.85, 0.95), (0.14, 0.14, 0.14), {'todo': E("brillo", 0xc0fff0, 0x40e0c0)})
b = m.pieza("cuerpo", 2)
b.caja((0, 0, -0.15), (1.4, 0.9, 1.3), NEG)
b.caja((0, 0, -0.15), (1.1, 1.0, 1.1), NEG)
b.caja((0, -0.1, 0.55), (1.2, 0.5, 0.2), {'todo': E("liso", 0x3a0a14)})
for s in (1, -1):
    for k in range(4):
        b.caja((s * (0.12 + k * 0.14), 0.12, 0.6), (0.03, 0.22, 0.03), {'todo': E("liso", 0xe8e8f0)})
        b.caja((s * (0.12 + k * 0.14), -0.32, 0.6), (0.03, 0.22, 0.03), {'todo': E("liso", 0xe8e8f0)})
    b.caja((s * 0.4, 0.38, 0.32), (0.08, 0.08, 0.08), {'todo': E("ojo", 0x9fffff, 0x101010)})
b.caja((0, 0.5, 0.25), (0.05, 0.12, 0.05), {'todo': E("liso", 0x2a2a34)})
b.caja((0, 0, -0.9), (0.04, 0.5, 0.3), {'todo': E("aleta", 0x24242c, 0x4a4a5a)})
m.crear()
