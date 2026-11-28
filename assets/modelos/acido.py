"""Proyectil de ácido del escupidor: una gota verde luminosa con núcleo claro."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
A = material("acido", 0x9aff40, emision=5)
N = material("nucleo", 0xe8ffb0, emision=6)
p = PiezaJ("acido#1", [A, N])
p.elip((0, 0, -0.03), (0.13, 0.13, 0.17), 8, 5, 0)
p.elip((0, 0, 0.02), (0.06, 0.06, 0.06), 6, 3, 1)
p.crear()
