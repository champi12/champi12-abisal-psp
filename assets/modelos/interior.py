"""Interior de la cápsula en estilo bloque (visto desde el centro, ojo a 1,1 m): cabina
octogonal de 3,6 m con cinco puestos en los mismos rumbos que el juego (0 = -z):
sintetizador 0, consola 90, dispensador médico 180, bodega 250, escalera a la escotilla 315.
Todo son cajas y caras planas; el detalle (placas, juntas y remaches) lo pone la textura
pixel art Texturas.panel() que el juego proyecta encima, teñida por estos colores.
Antes eran 1820 triángulos y 5400 vértices: dentro de la cápsula todo está cerca de la
cámara y el recorte de la GE bajaba a 26 fps."""
import sys, os, math
import bmesh
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
limpiar()
PARED = material("pared", 0x2c4a50)
PARED2 = material("pared2", 0x243e44)
ALTA = material("alta", 0x6a8a90)
ALTA2 = material("alta2", 0x5a7a80)
TECHO = material("techo", 0x4a6066)
SUELO = material("suelo", 0x1a2a2e)
COBRE = material("cobre", 0xc07a40, metal=0.5)
CIAN = material("cian", 0x6ff0ff, emision=4)
POD = material("pod", 0x3a5860)
POD2 = material("pod2", 0x1e3036)
NAR = material("naranja", 0xd86a2a)
REJA = material("reja", 0x30383e)
TUBO = material("tubos", 0xa8b4b8)
AMBAR = material("ambar", 0xffb040, emision=4)
VERDE = material("verde", 0x5ff0e0, emision=4)
LAMPARA = material("lampara", 0xfff0d0, emision=5)
VENTANA = material("ventana", 0x9ad8ff, emision=3)
ESCALERA = material("escalera", 0x8a9298)
AMARILLO = material("amarillo", 0xe8c030)
NEGRO = material("negro", 0x202020)
LUCES = [material("l1", 0x5aff7a, emision=4), material("l2", 0xffb030, emision=4), material("l3", 0xff4030, emision=4)]
MATS = [PARED, PARED2, ALTA, ALTA2, TECHO, SUELO, COBRE, CIAN, POD, POD2, NAR, REJA, TUBO, AMBAR, VERDE, LAMPARA,
        VENTANA, ESCALERA, AMARILLO, NEGRO] + LUCES
M = {m.name: i for i, m in enumerate(MATS)}
p = PiezaJ("interior#1", MATS)
AP = 1.8                                   # apotema: distancia del centro a cada pared
RV = AP / math.cos(math.pi / 8)            # radio de las esquinas


def rumbo(gr, r):
    a = math.radians(gr)
    return (-math.sin(a) * r, -math.cos(a) * r)


def en(gr, r, pt):
    """punto local de un puesto (x lateral, y, z hacia el centro) al rumbo gr"""
    a = math.radians(gr)
    cx, cz = rumbo(gr, r)
    x, y, z = pt
    return (cx + x * math.cos(a) + z * math.sin(a), y, cz - x * math.sin(a) + z * math.cos(a))


def caja_en(gr, r, pos, tam, mat):
    k = p.marca()
    c = en(gr, r, pos)
    p.cajaJ(c, tam, M[mat])
    p.girar(k, c, 0, gr)


# ---------------------------------------------------------------- casco (caras hacia dentro)
cascara0 = len(p.bm.faces)


def esquina(i, r, y):
    """esquina i del octógono (entre la pared i y la i+1) a la distancia r del centro"""
    x, z = rumbo(i * 45 + 22.5, r * RV / AP)
    return (x, y, z)


for i in range(8):
    j = i - 1
    a = lambda r, y: esquina(j, r, y)
    b = lambda r, y: esquina(i, r, y)
    par = i % 2 == 0
    # pared baja, cornisa de cobre que sobresale, pared alta
    p.cuad(a(AP, 0), a(AP, 0.95), b(AP, 0.95), b(AP, 0), M["pared" if par else "pared2"])
    p.cuad(a(AP - 0.07, 0.95), a(AP - 0.07, 1.03), b(AP - 0.07, 1.03), b(AP - 0.07, 0.95), M["cobre"])
    p.cuad(a(AP, 0.95), a(AP - 0.07, 0.95), b(AP - 0.07, 0.95), b(AP, 0.95), M["cobre"])
    p.cuad(a(AP, 1.03), a(AP - 0.07, 1.03), b(AP - 0.07, 1.03), b(AP, 1.03), M["cobre"])
    p.cuad(a(AP, 1.03), a(AP, 2.1), b(AP, 2.1), b(AP, 1.03), M["alta" if par else "alta2"])
    # techo en dos escalones y la tapa
    p.cuad(a(AP, 2.1), a(1.5, 2.1), b(1.5, 2.1), b(AP, 2.1), M["alta2"])
    p.cuad(a(1.5, 2.1), a(1.5, 2.36), b(1.5, 2.36), b(1.5, 2.1), M["techo"])
    p.cuad(a(1.5, 2.36), a(1.15, 2.36), b(1.15, 2.36), b(1.5, 2.36), M["alta2"])
    p.cuad(a(1.15, 2.36), a(1.15, 2.6), b(1.15, 2.6), b(1.15, 2.36), M["techo"])
    p.cuad(a(1.15, 2.6), a(0.62, 2.6), b(0.62, 2.6), b(1.15, 2.6), M["techo"])
    # aro de la escotilla, amarillo y negro
    p.cuad(a(0.62, 2.6), a(0.45, 2.6), b(0.45, 2.6), b(0.62, 2.6), M["amarillo" if par else "negro"])
    p.tri((0, 2.6, 0), a(0.45, 2.6), b(0.45, 2.6), M["pod"])
    # suelo: rejilla oscura con un aro de luz junto a la pared
    p.cuad(a(AP, 0), a(1.62, 0), b(1.62, 0), b(AP, 0), M["suelo"])
    p.cuad(a(1.62, 0), a(1.5, 0), b(1.5, 0), b(1.62, 0), M["cian"])
    p.cuad(a(1.5, 0), a(0.75, 0), b(0.75, 0), b(1.5, 0), M["suelo"])
    p.tri((0, 0, 0), a(0.75, 0), b(0.75, 0), M["reja"])
# todas las caras del casco miran al ojo del buzo
p.bm.faces.ensure_lookup_table()
ojo = J(0, 1.15, 0)
for f in p.bm.faces[cascara0:]:
    f.normal_update()
    if f.normal.dot(ojo - f.calc_center_median()) < 0:
        f.normal_flip()

# ---------------------------------------------------------------- estructura
for i in range(8):
    x, _, z = esquina(i, AP - 0.05, 0)
    if i % 2 == 0:   # pilares de cobre en las esquinas pares, tubo gris en las impares
        k = p.marca()
        p.cajaJ((x, 1.05, z), (0.1, 2.1, 0.1), M["cobre"])
        p.girar(k, (x, 1.05, z), 0, i * 45 + 22.5)
    else:
        k = p.marca()
        p.cajaJ((x, 1.05, z), (0.06, 2.1, 0.06), M["tubos"])
        p.girar(k, (x, 1.05, z), 0, i * 45 + 22.5)
# tubería perimetral (arriba) y franja naranja (abajo), una caja por pared
for i in range(8):
    caja_en(i * 45, AP - 0.05, (0, 2.0, 0), (1.42, 0.07, 0.07), "tubos")
    caja_en(i * 45, AP - 0.03, (0, 0.22, 0), (1.46, 0.06, 0.04), "naranja")

# paneles con pilotos entre los puestos
for gr in (45, 135, 225, 292):
    for fy in range(2):
        for fx in range(2):
            caja_en(gr, AP - 0.03, (-0.17 + fx * 0.34, 1.32 + fy * 0.3, 0), (0.28, 0.24, 0.05),
                    "alta" if (fx + fy) % 2 == 0 else "alta2")
    for k in range(3):
        caja_en(gr, AP - 0.05, (-0.12 + k * 0.12, 1.12, 0), (0.05, 0.04, 0.03), f"l{k + 1}")

# ---------------------------------------------------------------- 0: sintetizador
R0 = AP - 0.28
caja_en(0, R0, (0, 0.5, 0), (1.0, 1.0, 0.5), "pod2")              # cuerpo
caja_en(0, R0, (0, 1.06, -0.04), (0.9, 0.12, 0.42), "pod")        # tapa
caja_en(0, R0, (0, 0.62, 0.26), (0.84, 0.06, 0.04), "cobre")      # moldura
caja_en(0, R0, (0, 0.38, 0.26), (0.6, 0.36, 0.03), "negro")       # boca del fabricador
caja_en(0, R0, (0, 0.38, 0.27), (0.5, 0.04, 0.02), "cian")
caja_en(0, AP - 0.06, (0, 1.42, 0), (0.62, 0.34, 0.04), "verde")  # pantalla
caja_en(0, AP - 0.07, (0, 1.42, 0), (0.72, 0.44, 0.03), "pod2")
caja_en(0, R0, (0.38, 1.16, 0.08), (0.08, 0.08, 0.08), "cian")    # piloto
# ---------------------------------------------------------------- 90: consola (radio)
R1 = AP - 0.3
caja_en(90, R1, (0, 0.3, 0), (0.9, 0.6, 0.44), "pod2")
caja_en(90, R1, (0, 0.63, 0.05), (1.0, 0.06, 0.56), "pod")
caja_en(90, R1, (0, 0.67, 0.08), (0.8, 0.02, 0.4), "ambar")       # mesa holográfica
caja_en(90, R1, (0, 0.82, -0.1), (0.08, 0.26, 0.08), "cian")
caja_en(90, R1, (0, 0.3, 0.23), (0.7, 0.04, 0.02), "cobre")
caja_en(90, AP - 0.06, (0, 1.4, 0), (0.5, 0.3, 0.04), "ambar")    # altavoz y pantalla
caja_en(90, AP - 0.07, (0, 1.4, 0), (0.6, 0.4, 0.03), "pod2")
# ---------------------------------------------------------------- 180: dispensador médico
R2 = AP - 0.22
caja_en(180, R2, (0, 0.55, 0), (0.8, 1.1, 0.4), "pod2")
caja_en(180, R2, (0, 0.62, 0.17), (0.56, 0.6, 0.08), "negro")     # hueco
caja_en(180, R2, (0, 0.34, 0.2), (0.5, 0.05, 0.04), "cobre")
caja_en(180, R2, (0, 1.14, 0), (0.86, 0.08, 0.46), "pod")
caja_en(180, R2, (0, 0.94, 0.2), (0.44, 0.04, 0.02), "verde")
# ---------------------------------------------------------------- 250: bodega
R3 = AP - 0.3
caja_en(250, R3, (0, 0.62, 0), (1.0, 1.24, 0.5), "pod2")
for s in (-1, 1):
    caja_en(250, R3, (s * 0.245, 0.62, 0.26), (0.47, 1.12, 0.03), "pod")   # dos puertas
    caja_en(250, R3, (s * 0.06, 0.66, 0.29), (0.04, 0.2, 0.04), "cobre")   # tiradores
caja_en(250, R3, (0, 1.06, 0.29), (0.7, 0.05, 0.02), "cian")
caja_en(250, R3, (0, 1.28, -0.02), (1.06, 0.08, 0.54), "pod")
# ojo de buey a la derecha de la bodega
caja_en(212, AP - 0.05, (0, 1.6, 0), (0.42, 0.42, 0.03), "ventana")
caja_en(212, AP - 0.03, (0, 1.6, 0), (0.56, 0.56, 0.04), "cobre")
# ---------------------------------------------------------------- 315: escalera a la escotilla
for s in (-0.22, 0.22):
    caja_en(315, 1.5, (s, 1.3, 0), (0.06, 2.6, 0.06), "escalera")
for i in range(6):
    caja_en(315, 1.5, (0, 0.32 + i * 0.38, 0), (0.44, 0.05, 0.05), "escalera")
# ---------------------------------------------------------------- techo: volante y lámparas
p.cajaJ((0, 2.57, 0), (0.34, 0.04, 0.05), M["negro"])
p.cajaJ((0, 2.57, 0), (0.05, 0.04, 0.34), M["negro"])
p.cajaJ((0, 2.55, 0), (0.08, 0.04, 0.08), M["cobre"])
for i in range(3):
    gr = i * 120 + 60
    caja_en(gr, 0.9, (0, 2.58, 0), (0.34, 0.04, 0.14), "lampara")
# ---------------------------------------------------------------- asiento central
p.cajaJ((0, 0.25, 0), (0.08, 0.5, 0.08), M["reja"])
p.cajaJ((0, 0.04, 0), (0.4, 0.08, 0.4), M["reja"])
p.cajaJ((0, 0.53, 0.04), (0.36, 0.07, 0.34), M["pod"])
p.cajaJ((0, 0.8, 0.24), (0.36, 0.42, 0.06), M["pod"])
p.cajaJ((0, 0.82, 0.275), (0.05, 0.36, 0.01), M["naranja"])
# the diver's eye never moves (only turns): a face whose back looks at it is never seen
p.bm.faces.ensure_lookup_table()
ojo = J(0, 1.15, 0)
ocultas = []
for f in p.bm.faces:
    f.normal_update()
    if f.normal.dot(ojo - f.calc_center_median()) <= 1e-4:
        ocultas.append(f)
bmesh.ops.delete(p.bm, geom=ocultas, context='FACES')
p.crear()
print("TRIS", triangulos())
