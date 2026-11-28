"""El Batel, estilo bloque: batiscafo clásico, nada que ver con la Rémora. Arriba un flotador
alargado gris azulado de placas remachadas con su torreta, timón y hélice carenada; debajo,
colgada de un armazón, la esfera de presión de marfil con ojos de buey y aros de cobre, dos
focos y patines de apoyo, ~5 m. Piezas: hélice#1 (centrada en x = y = 0, z = -2,68) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("batel", ppm=20)
CAS = {'todo': E("panel", 0x4a5c66, 0x3c4c54)}
OSC = {'todo': E("casco", 0x2c383e, 0x20282c)}
COB = {'todo': E("panel", 0xb07a3a, 0x8a6532)}
MAR = {'todo': E("hueso", 0xe8dcc0, 0xc8b898)}
h = m.pieza("helice", 1)
for i in range(4):
    h.caja((0, 0.3, -2.68), (0.18, 0.46, 0.04), {'todo': E("casco", 0x2a2e33, 0x1a1c20)}, rot=(0, 0, i * 90 + 20),
           pivote=(0, 0, -2.68))
h.caja((0, 0, -2.68), (0.18, 0.18, 0.12), COB)
c = m.pieza("casco", 2)
# flotador: largo y redondeado, con proa y popa más estrechas
tubo_bloque(c, (0, 0, 0), 0.72, 3.2, CAS, lados=10)
tubo_bloque(c, (0, 0, 1.85), 0.52, 0.5, CAS, lados=10)
tubo_bloque(c, (0, 0, 2.2), 0.3, 0.2, OSC, lados=8)
tubo_bloque(c, (0, 0, -1.85), 0.52, 0.5, CAS, lados=10)
tubo_bloque(c, (0, 0, -2.68), 0.62, 0.3, {'todo': E("rejilla", 0x2c383e, 0x5a6a74)}, lados=10)   # carenado
for z in (-1.2, 0, 1.2):
    tubo_bloque(c, (0, 0, z), 0.76, 0.12, OSC, lados=10)                          # costillas
c.caja((0, 0.85, 0.6), (0.4, 0.3, 0.7), CAS)                                      # torreta
c.caja((0, 1.02, 0.6), (0.3, 0.04, 0.5), {'todo': E("panel", 0x5a6c76, 0x3c4c54)})
c.caja((0, 1.08, 0.6), (0.07, 0.07, 0.07), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
c.caja((0, 0.7, -2.2), (0.05, 0.6, 0.5), CAS)                                      # timón
for s in (1, -1):
    c.caja((s * 0.55, -0.05, -2.2), (0.5, 0.04, 0.35), CAS)
# armazón que cuelga la esfera
for (x, z) in ((0.35, 0.95), (-0.35, 0.95), (0.35, -0.25), (-0.35, -0.25)):
    c.caja((x, -0.85, z), (0.07, 0.6, 0.07), OSC)
# esfera de presión: anillos de radio decreciente arriba y abajo
R, YC, ZC = 0.78, -1.55, 0.35
for k in range(-3, 4):
    y = k * 0.2
    r = (R * R - y * y) ** 0.5
    if r < 0.2:
        continue
    tubo_bloque(c, (0, YC + y, ZC), r, 0.21, MAR, lados=10, eje='y')
tubo_bloque(c, (0, YC, ZC), R + 0.03, 0.08, COB, lados=10, eje='y')               # aro de cobre (ecuador)
c.caja((0, YC + 0.05, ZC + R - 0.02), (0.34, 0.34, 0.06), COB)                     # ojo de buey delantero
c.caja((0, YC + 0.05, ZC + R + 0.01), (0.24, 0.24, 0.04), {'todo': E("cupula", 0x6ad8ff, 0x1a4a6a)})
for s in (1, -1):
    c.caja((s * (R - 0.02), YC + 0.05, ZC), (0.06, 0.26, 0.26), COB)               # ojos de buey laterales
    c.caja((s * (R + 0.01), YC + 0.05, ZC), (0.04, 0.18, 0.18), {'todo': E("cupula", 0x6ad8ff, 0x1a4a6a)})
    c.caja((s * 0.6, YC - 0.3, ZC + 0.6), (0.18, 0.16, 0.22), OSC)                 # focos
    c.caja((s * 0.6, YC - 0.3, ZC + 0.72), (0.14, 0.12, 0.03), {'todo': E("brillo", 0xfff6d8, 0xffe080)})
    c.caja((s * 0.55, YC - 0.85, ZC), (0.08, 0.08, 1.8), OSC)                      # patines
    for z in (-0.5, 0.5):
        c.caja((s * 0.5, YC - 0.62, ZC + z), (0.06, 0.4, 0.06), OSC)
m.crear()
