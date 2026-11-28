"""La Rémora, estilo bloque: sumergible unipersonal naranja con cúpula oscura, dos góndolas de
propulsión, deriva, faros y hélice con anillo, ~5 m. Piezas: hélice#1 (centrada en
x = y = 0, z = -2,45: el juego la hace girar) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("remora", ppm=16)
NAR = {'todo': E("placas", 0xe8692a, 0xf07a34), '-y': E("liso", 0xd8dcdf)}
BLA = {'todo': E("placas", 0xd8dcdf, 0xb8bcbf)}
OSC = {'todo': E("liso", 0x3a4048)}
h = m.pieza("helice", 1)
for i in range(3):
    h.caja((0, 0.3, -2.45), (0.14, 0.55, 0.03), {'todo': E("liso", 0x2a2e33)}, rot=(0, 0, i * 120 + 15), pivote=(0, 0, -2.45))
h.caja((0, 0, -2.45), (0.14, 0.14, 0.08), {'todo': E("liso", 0x7a858c)})
c = m.pieza("casco", 2)
c.caja((0, 0, 0.1), (1.6, 1.4, 3.2), NAR)                       # cuerpo
c.caja((0, -0.05, 1.95), (1.2, 1.1, 0.6), OSC)                  # proa oscura
c.caja((0, -0.05, 2.35), (0.7, 0.6, 0.3), OSC)
c.caja((0, 0, -1.75), (1.2, 1.1, 0.6), BLA)                     # popa blanca
c.caja((0, 0, -2.2), (0.5, 0.5, 0.3), {'todo': E("liso", 0x7a858c)})
c.caja((0, 0.8, 0.85), (1.0, 0.45, 1.6), {'todo': E("brillo", 0x2a5a74, 0x1c3a4c)})   # cúpula
c.caja((0, 1.05, 0.8), (0.1, 0.06, 1.7), {'todo': E("liso", 0x5a646c)})
c.caja((0, 0.74, -0.6), (0.6, 0.1, 0.6), BLA)                  # escotilla
for s in (1, -1):
    c.caja((s * 0.95, -0.1, -0.2), (0.4, 0.12, 0.6), {'todo': E("liso", 0x7a858c)})            # soporte
    c.caja((s * 1.38, -0.1, -0.3), (0.5, 0.5, 1.8), NAR)                                      # góndola
    c.caja((s * 1.38, -0.1, -1.3), (0.4, 0.4, 0.2), OSC)                                      # tobera
    c.caja((s * 1.38, -0.1, 0.62), (0.3, 0.2, 0.06), {'todo': E("brillo", 0xfff6d8, 0xffe080)})  # faro
    c.caja((s * 0.35, -0.25, 2.52), (0.18, 0.12, 0.06), {'todo': E("brillo", 0xfff6d8, 0xffe080)})
    c.caja((s * 0.75, -0.02, -2.0), (0.5, 0.04, 0.4), NAR)                                      # estabilizador
c.caja((0, 0.8, -1.9), (0.04, 0.55, 0.6), NAR)                                                   # deriva
c.caja((0, 0, -2.45), (1.5, 0.12, 0.25), {'todo': E("liso", 0x7a858c)})                          # anillo (arriba/abajo/lados)
c.caja((0, 0.71, -2.45), (1.5, 0.12, 0.25), {'todo': E("liso", 0x7a858c)})
c.caja((0, -0.71, -2.45), (1.5, 0.12, 0.25), {'todo': E("liso", 0x7a858c)})
for s in (1, -1):
    c.caja((s * 0.71, 0, -2.45), (0.12, 1.5, 0.25), {'todo': E("liso", 0x7a858c)})
c.caja((0.3, 1.1, 0.2), (0.03, 0.6, 0.03), {'todo': E("liso", 0x7a858c)})                       # antena
c.caja((0.3, 1.42, 0.2), (0.08, 0.08, 0.08), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
c.caja((0, 1.1, -1.95), (0.08, 0.08, 0.08), {'todo': E("brillo", 0xff4030, 0xa01010)})
m.crear()
