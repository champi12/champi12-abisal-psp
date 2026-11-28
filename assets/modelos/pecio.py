"""El Albatros: la nave de rescate que nunca llegó, partida en la plataforma exterior. Casco
de ~36 m hueco con PARED GRUESA, una brecha en el centro por la que se entra, puente sobre
la proa, mástil y planchas sueltas alrededor."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
CASCO = [material("casco1", 0x6b7073), material("casco2", 0x5f6467), material("oxido", 0x8c4a28), material("oxido2", 0x7a4426)]
DENTRO = material("dentro", 0x2e3234)
BORDE = material("borde", 0x8a8f92)
PUENTE = material("puente", 0x5f6669)
CUBIERTA = material("cubierta", 0x9aa0a2)
CRISTAL = material("cristal", 0x2a4a56)
MATS = CASCO + [DENTRO, BORDE, PUENTE, CUBIERTA, CRISTAL]
p = PiezaJ("pecio#1", MATS)
AN, PER = 19, 14
rnd = random.Random(7)
def radio(k):
    r = 3.7 - abs(k - 9) * 0.12
    return r * 0.7 if (k < 2 or k > 16) else r
ri = [[0] * (PER + 1) for _ in range(AN)]
interior, exterior = [], []
for k in range(AN):
    z = -18 + k * 2
    fi, fo = [], []
    for j in range(PER + 1):
        a = 2 * math.pi * j / PER
        dz = rnd.uniform(-0.15, 0.15)
        r = radio(k)
        fo.append((math.cos(a) * r, math.sin(a) * r * 0.92, z + dz))
        fi.append((math.cos(a) * (r - 0.35), math.sin(a) * (r - 0.35) * 0.92, z + dz))
    interior.append(fi)
    exterior.append(fo)
hay = lambda k: not (8 <= k <= 10)
tono = lambda k, j: (k * 3 + j * 5) % 4
cascara(p, interior, exterior, lambda k, j: hay(k) and hay(k + 1), 4, tono, 5)
# tapas de proa y popa
for k, sentido in ((0, -1), (AN - 1, 1)):
    fila = exterior[k][:PER]
    vs = [p.bm.verts.new(J(*q)) for q in (fila if sentido > 0 else list(reversed(fila)))]
    p.bm.faces.new(vs).material_index = 0
# cuadernas rotas asomando en la brecha
for k in (7, 11):
    for j in range(0, PER, 2):
        a = 2 * math.pi * j / PER
        z = -18 + k * 2 + (1.2 if k == 7 else -1.2)
        p.cajaJ((math.cos(a) * 3.2, math.sin(a) * 2.9, z), (0.25, 0.25, 1.6 + rnd.random()), 5)
# puente de mando sobre la proa, con ventanas, y mástil
p.cajaJ((0, 4.1, 9.5), (2.6, 1.6, 3.2), 6)
p.cajaJ((0, 5.0, 9.5), (2.0, 0.25, 2.6), 7)
for x in (-0.8, 0.0, 0.8):
    p.cajaJ((x, 4.4, 11.12), (0.6, 0.5, 0.04), 8)
p.tubo([(0.9, 5.1, 8.2, 0.08), (0.9, 7.8, 8.2, 0.06)], 5, 2, True)
p.cajaJ((0.9, 7.2, 8.2), (1.2, 0.08, 0.08), 2)
# hélice partida en la popa y planchas sueltas en el fondo
p.cuad((-2.6, 1.2, -17.5), (2.6, 1.2, -17.5), (1.6, 2.6, -21), (-1.6, 2.6, -21), 1, doble=True)
for i in range(8):
    a = math.radians(40 + i * 47)
    k0 = p.marca()
    p.cajaJ((math.cos(a) * (9 + i), 0.3, math.sin(a) * (9 + i * 1.3)), (1.4 + (i % 3) * 0.6, 0.12, 0.9), i % 4)
    p.girar(k0, (math.cos(a) * (9 + i), 0.3, math.sin(a) * (9 + i * 1.3)), i * 9 - 20, i * 23)
p.crear()
print("TRIS", triangulos())
