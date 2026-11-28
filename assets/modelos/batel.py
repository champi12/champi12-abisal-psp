"""El Batel, estilo bloque: batiscafo pesado de las profundidades, casco de presión gris azulado
con costillas de refuerzo y placas de marfil, dos tanques de lastre, cabina con aro de cobre y
hélice carenada, ~5 m. Piezas: hélice#1 (centrada en x = y = 0, z = -2,68) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("batel", ppm=12)
CAS = {'todo': E("placas", 0x3c4c54, 0x4a5c66), '-y': E("liso", 0x2c383e)}
h = m.pieza("helice", 1)
for i in range(4):
    h.caja((0, 0.32, -2.68), (0.16, 0.5, 0.04), {'todo': E("liso", 0x2a2e33)}, rot=(0, 0, i * 90 + 20), pivote=(0, 0, -2.68))
h.caja((0, 0, -2.68), (0.16, 0.16, 0.1), {'todo': E("liso", 0x8a6532)})
c = m.pieza("casco", 2)
c.caja((0, 0, 0), (1.8, 1.7, 3.6), CAS)
for z in (-1.2, 0, 1.2):                                                    # costillas
    c.caja((0, 0, z), (1.9, 1.8, 0.14), {'todo': E("liso", 0x2c383e)})
c.caja((0, 0.05, 2.0), (1.4, 1.4, 0.5), {'todo': E("hueso", 0xe8dcc0, 0xb8a888)})     # proa de marfil
c.caja((0, 0.15, 2.3), (0.9, 0.8, 0.2), {'todo': E("liso", 0x8a6532)})                # aro de cobre
c.caja((0, 0.15, 2.38), (0.7, 0.6, 0.1), {'todo': E("brillo", 0x6ad8ff, 0x2a6a8a)})   # ventana
for s in (1, -1):
    c.caja((s * 1.15, -0.55, 0), (0.6, 0.6, 3.0), {'todo': E("placas", 0x8a6532, 0x6a4a22)})   # tanques
    c.caja((s * 0.5, -0.6, 2.25), (0.2, 0.14, 0.08), {'todo': E("brillo", 0xfff6d8, 0xffe080)})
c.caja((0, 0, -2.15), (1.2, 1.2, 0.7), CAS)
c.caja((0, 0, -2.68), (1.5, 0.15, 0.3), {'todo': E("liso", 0x2c383e)})                # carenado
c.caja((0, 0.72, -2.68), (1.5, 0.15, 0.3), {'todo': E("liso", 0x2c383e)})
c.caja((0, -0.72, -2.68), (1.5, 0.15, 0.3), {'todo': E("liso", 0x2c383e)})
for s in (1, -1):
    c.caja((s * 0.72, 0, -2.68), (0.15, 1.5, 0.3), {'todo': E("liso", 0x2c383e)})
c.caja((0, 1.0, -0.4), (0.5, 0.3, 0.8), CAS)                                         # torreta
c.caja((0, 1.2, -0.4), (0.08, 0.08, 0.08), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
m.crear()
