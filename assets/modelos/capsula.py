"""Cápsula de escape, estilo bloque: flota en la superficie. Base de flotación redonda con
aros de cobre y placas remachadas, torre central, cúpula de cristal escalonada, cuatro paneles
solares en brazos y mástil con baliza cian. Una pieza."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("capsula", ppm=14)
p = m.pieza("capsula", 1)
BLA = {'todo': E("panel", 0xe0e4e7, 0xa8aeb2)}
COB = {'todo': E("panel", 0xb07a3a, 0x8a6532)}
OSC = {'todo': E("casco", 0x3a4048, 0x2c3036)}
NAR = {'todo': E("panel", 0xe8692a, 0xc85a20)}
GLAS = {'todo': E("cupula", 0x5ac8e8, 0x183a50)}
tubo_bloque(p, (0, 0.0, 0), 2.4, 0.7, BLA, lados=12, eje='y')          # flotador
tubo_bloque(p, (0, -0.5, 0), 2.0, 0.4, OSC, lados=12, eje='y')         # quilla
tubo_bloque(p, (0, 0.38, 0), 2.45, 0.1, COB, lados=12, eje='y')        # aros de cobre
tubo_bloque(p, (0, -0.3, 0), 2.45, 0.1, COB, lados=12, eje='y')
tubo_bloque(p, (0, 1.1, 0), 1.25, 1.4, NAR, lados=10, eje='y')         # torre
tubo_bloque(p, (0, 1.85, 0), 1.3, 0.12, COB, lados=10, eje='y')
for k, (r, h) in enumerate(((1.15, 0.3), (0.95, 0.3), (0.7, 0.25), (0.4, 0.2))):   # cúpula escalonada
    tubo_bloque(p, (0, 2.05 + k * 0.28, 0), r, h, GLAS, lados=10, eje='y')
p.caja((0, 1.0, 1.24), (0.7, 0.9, 0.06), OSC)                            # escotilla
p.caja((0, 1.0, 1.28), (0.12, 0.12, 0.04), {'todo': E("brillo", 0xffd060, 0xc08020)})
for k in range(4):                                                       # brazos y paneles solares
    a = math.radians(45 + k * 90)
    x, z = math.sin(a), math.cos(a)
    p.caja((x * 2.0, 0.55, z * 2.0), (0.12, 0.1, 0.12), OSC)
    p.caja((x * 2.3, 0.75, z * 2.3), (0.1, 0.4, 0.1), OSC)
    p.caja((x * 2.45, 0.98, z * 2.45), (0.9, 0.05, 0.9), {'todo': E("rejilla", 0x1a3a6a, 0x3a6aa8), '-y': OSC['todo']},
           rot=(0, -math.degrees(a), 0))
p.caja((0.5, 3.4, 0.2), (0.07, 2.0, 0.07), OSC)                          # mástil y baliza
p.caja((0.5, 4.5, 0.2), (0.18, 0.18, 0.18), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
m.crear()
