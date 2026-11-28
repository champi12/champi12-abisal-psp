"""El Batel, estilo bloque: batisfera redonda, nada que ver con la Rémora. Una esfera de
presión de marfil (anillos de placas) con una cabina de cristal en burbuja delante y aros de
cobre, ojos de buey laterales, dos tanques de lastre de cobre
encima, focos, patines de apoyo y una hélice carenada detrás, ~2,8 m de diámetro.
Piezas: hélice#1 (centrada en x = y = 0, z = -1,75) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("batel", ppm=20)
MAR = {'todo': E("hueso", 0xe8dcc0, 0xc8b898)}
OSC = {'todo': E("casco", 0x2c383e, 0x20282c)}
COB = {'todo': E("panel", 0xb07a3a, 0x8a6532)}
CAS = {'todo': E("panel", 0x4a5c66, 0x3c4c54)}
h = m.pieza("helice", 1)
for i in range(4):
    h.caja((0, 0.26, -1.75), (0.16, 0.4, 0.04), {'todo': E("casco", 0x2a2e33, 0x1a1c20)}, rot=(0, 0, i * 90 + 20),
           pivote=(0, 0, -1.75))
h.caja((0, 0, -1.75), (0.16, 0.16, 0.12), COB)
c = m.pieza("casco", 2)
R = 1.35
for k in range(-4, 5):                                   # la esfera: anillos de radio decreciente
    y = k * 0.29
    r = (R * R - y * y) ** 0.5
    if r < 0.25:
        continue
    tubo_bloque(c, (0, y, 0), r, 0.3, MAR, lados=12, eje='y')
c.caja((0, R - 0.05, 0), (0.6, 0.12, 0.6), MAR)          # casquetes
c.caja((0, -R + 0.05, 0), (0.6, 0.12, 0.6), MAR)
tubo_bloque(c, (0, 0, 0), R + 0.04, 0.12, COB, lados=12, eje='y')            # aro del ecuador
# cabina: una cúpula de cristal en burbuja escalonada al frente, con marco de cobre
GLAS = {'todo': E("cupula", 0x6ad8ff, 0x1a4a6a)}
c.caja((0, 0.12, R - 0.12), (1.3, 1.05, 0.16), COB)
c.caja((0, 0.12, R - 0.02), (1.14, 0.92, 0.14), GLAS)
c.caja((0, 0.12, R + 0.1), (0.96, 0.78, 0.12), GLAS)
c.caja((0, 0.12, R + 0.2), (0.72, 0.58, 0.1), GLAS)
c.caja((0, 0.12, R + 0.27), (0.44, 0.34, 0.06), GLAS)
c.caja((0, 0.12, R + 0.18), (0.06, 0.95, 0.3), COB)                         # nervio del marco
for s in (1, -1):
    c.caja((s * (R - 0.05), 0.2, 0.2), (0.1, 0.36, 0.36), COB)              # ojos de buey laterales
    c.caja((s * (R + 0.01), 0.2, 0.2), (0.04, 0.26, 0.26), {'todo': E("cupula", 0x6ad8ff, 0x1a4a6a)})
    tubo_bloque(c, (s * 0.55, R + 0.05, -0.1), 0.2, 1.3, COB, lados=8)      # tanques de lastre
    c.caja((s * 0.5, R - 0.15, 0.3), (0.06, 0.3, 0.06), OSC)
    c.caja((s * 0.5, R - 0.15, -0.5), (0.06, 0.3, 0.06), OSC)
    c.caja((s * 0.75, -0.45, R - 0.15), (0.24, 0.2, 0.24), OSC)              # focos
    c.caja((s * 0.75, -0.45, R - 0.02), (0.18, 0.14, 0.03), {'todo': E("brillo", 0xfff6d8, 0xffe080)})
    c.caja((s * 0.7, -R - 0.15, 0), (0.1, 0.1, 2.0), OSC)                  # patines
    for z in (-0.6, 0.6):
        c.caja((s * 0.65, -R + 0.1, z), (0.07, 0.45, 0.07), OSC)
c.caja((0, R + 0.12, 0), (0.3, 0.1, 0.3), CAS)                               # escotilla
c.caja((0, R + 0.2, 0), (0.08, 0.08, 0.08), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
# eje y carenado de la hélice detrás
c.caja((0, 0, -R - 0.2), (0.22, 0.22, 0.5), CAS)
tubo_bloque(c, (0, 0, -1.75), 0.5, 0.22, {'todo': E("rejilla", 0x2c383e, 0x5a6a74)}, lados=10)
c.caja((0, 0.45, -1.5), (0.04, 0.4, 0.4), CAS)                               # timón
m.crear()
