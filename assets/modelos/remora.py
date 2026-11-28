"""La Rémora, estilo bloque: sumergible unipersonal de casco redondeado (anillos de placas
remachadas, naranja con proa y popa blancas), cúpula de burbuja escalonada con reflejos, dos
turbinas laterales carenadas con rejilla (no son misiles: empujan agua), faros, antena y
hélice con anillo, ~5 m. Piezas: hélice#1 (centrada en x = y = 0, z = -2,45) y casco#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()

m = Modelo("remora", ppm=22)
NAR = {'todo': E("panel", 0xe8692a, 0xc85a20)}
BLA = {'todo': E("panel", 0xe0e4e7, 0xb8bcbf)}
OSC = {'todo': E("casco", 0x3a4048, 0x2c3036)}
MET = {'todo': E("casco", 0x7a858c, 0x5a646c)}
GLAS = {'todo': E("cupula", 0x2a6a8a, 0x10283a)}
h = m.pieza("helice", 1)
for i in range(3):
    h.caja((0, 0.3, -2.45), (0.16, 0.55, 0.03), {'todo': E("casco", 0x2a2e33, 0x1a1c20)}, rot=(0, 0, i * 120 + 15),
           pivote=(0, 0, -2.45))
h.caja((0, 0, -2.45), (0.16, 0.16, 0.1), MET)
c = m.pieza("casco", 2)
tubo_bloque(c, (0, 0, 0.1), 0.8, 2.6, NAR, lados=8)              # casco principal
tubo_bloque(c, (0, 0, 1.65), 0.64, 0.6, BLA, lados=8)            # proa
tubo_bloque(c, (0, 0, 2.08), 0.42, 0.35, OSC, lados=8)
c.caja((0, 0, 2.28), (0.55, 0.55, 0.08), OSC)
tubo_bloque(c, (0, 0, -1.45), 0.64, 0.5, BLA, lados=8)           # popa
tubo_bloque(c, (0, 0, -1.9), 0.42, 0.45, OSC, lados=8)
tubo_bloque(c, (0, 0, -2.45), 0.72, 0.3, {'todo': E("rejilla", 0x5a646c, 0x8a959c)}, lados=8)   # anillo de la hélice
for k in range(3):                                                 # radios del anillo
    a = math.radians(k * 120 + 75)
    c.caja((math.sin(a) * 0.4, math.cos(a) * 0.4, -2.35), (0.05, 0.65, 0.05), MET, rot=(0, 0, -(k * 120 + 75)),
           pivote=(math.sin(a) * 0.4, math.cos(a) * 0.4, -2.35))
# cúpula de burbuja escalonada, con marco
c.caja((0, 0.86, 0.85), (1.0, 0.18, 1.6), GLAS)
c.caja((0, 1.02, 0.85), (0.84, 0.16, 1.36), GLAS)
c.caja((0, 1.16, 0.85), (0.58, 0.12, 0.96), GLAS)
c.caja((0, 1.24, 0.85), (0.08, 0.06, 1.0), MET)
c.caja((0, 0.78, 0.85), (1.08, 0.06, 1.7), MET)
# escotilla y franja
c.caja((0, 0.84, -0.6), (0.6, 0.08, 0.6), {'todo': E("panel", 0xe0e4e7, 0x9aa0a4)})
c.caja((0, 0.9, -0.6), (0.18, 0.06, 0.1), OSC)
# turbinas laterales: pequeñas, carenadas y pegadas al casco, con rejilla delante y detrás
for s in (1, -1):
    c.caja((s * 0.86, -0.18, -0.35), (0.22, 0.1, 0.36), MET)
    tubo_bloque(c, (s * 1.08, -0.18, -0.35), 0.22, 0.6, NAR, lados=8)
    c.caja((s * 1.08, -0.18, -0.04), (0.34, 0.34, 0.02), {'todo': E("rejilla", 0x2a2e33, 0x6a747c)})
    c.caja((s * 1.08, -0.18, -0.66), (0.34, 0.34, 0.02), {'todo': E("rejilla", 0x2a2e33, 0x6a747c)})
    c.caja((s * 1.08, -0.18, -0.02), (0.1, 0.1, 0.02), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
    c.caja((s * 0.35, -0.28, 2.3), (0.2, 0.12, 0.06), {'todo': E("brillo", 0xfff6d8, 0xffe080)})   # faros
    c.caja((s * 0.7, -0.05, -1.9), (0.55, 0.04, 0.4), NAR)                                          # estabilizador
c.caja((0, 0.75, -1.85), (0.05, 0.6, 0.6), NAR)                                                    # deriva
c.caja((0.3, 1.1, 0.0), (0.03, 0.6, 0.03), MET)                                                     # antena
c.caja((0.3, 1.42, 0.0), (0.08, 0.08, 0.08), {'todo': E("brillo", 0x6ff0ff, 0x30a0c0)})
c.caja((0, 1.08, -1.9), (0.08, 0.08, 0.08), {'todo': E("brillo", 0xff4030, 0xa01010)})
m.crear()
