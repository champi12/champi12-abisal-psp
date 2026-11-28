"""Medusa luna, estilo bloque: campana violeta luminosa en escalones con brazos y tentáculos.
Piezas: tentáculos#1 (y < -0,05) y campana#2. Translúcida en el juego."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("medusa", ppm=40)
CAMP = {'todo': E("brillo", 0xd8a0ff, 0x8a50d8)}
t = m.pieza("tentaculos", 1)
for k in range(6):
    a = math.radians(k * 60)
    t.caja((math.sin(a) * 0.25, -0.55, math.cos(a) * 0.25), (0.03, 0.9, 0.03), {'todo': E("brillo", 0xc890ff, 0x6a40b0)})
t.caja((0, -0.3, 0), (0.16, 0.45, 0.16), {'todo': E("brillo", 0xf0d0ff, 0xa070e0)})
c = m.pieza("campana", 2)
c.caja((0, 0.05, 0), (0.84, 0.18, 0.84), CAMP)
c.caja((0, 0.2, 0), (0.66, 0.14, 0.66), CAMP)
c.caja((0, 0.32, 0), (0.42, 0.1, 0.42), CAMP)
c.caja((0, 0.0, 0), (0.3, 0.12, 0.3), {'todo': E("brillo", 0xffffff, 0xe0c0ff)})
m.crear()
