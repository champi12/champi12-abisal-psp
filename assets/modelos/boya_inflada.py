"""Boya de emergencia inflada sobre la cabeza, con su cordón hasta la mano izquierda."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
NAR = material("naranja", 0xe8692a)
NAR2 = material("naranja2", 0xf08a3a)
BLANCO = material("banda", 0xe8e8e4)
CORDON = material("cordon", 0x3a3a3a)
LUZ = material("luz", 0xffd080, emision=5)
p = PiezaJ("boya#1", [NAR, NAR2, BLANCO, CORDON, LUZ])
p.torno([(0, -0.25), (0.28, -0.15), (0.32, 0.05), (0.25, 0.22), (0, 0.3)], 10, [0, 1, 2, 0], eje='y')
p.torno([(0.06, -0.25), (0.05, -0.3)], 6, 3, eje='y')
p.torno([(0.012, -0.3), (0.012, -1.25)], 4, 3, eje='y')
p.elip((0, 0.3, 0), (0.05, 0.04, 0.05), 4, 2, 4)
p.crear()
