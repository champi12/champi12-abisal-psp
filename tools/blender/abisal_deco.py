"""Piezas comunes del decorado en estilo bloque (como Blockbench): cajas giradas, tramos de
caja entre dos puntos, planos rectos y montones de roca. Todo en coordenadas del juego
(x, y arriba, z adelante); los materiales son índices de la lista MATS de cada modelo.
El detalle lo pone en el juego una textura pixel art proyectada (Texturas.piedra, fibra,
panel), así que aquí solo hay formas y colores."""
import math, random
import bmesh
from abisal_bm import PiezaJ, J


class PiezaD(PiezaJ):
    """pieza que al crearse quita lo que nunca se ve: caras enterradas y bases sobre el suelo"""

    def crear(self):
        fuera = []
        for f in self.bm.faces:
            f.normal_update()
            ys = [v.co.z for v in f.verts]       # Blender z = altura del juego
            if max(ys) < -0.02 or (f.normal.z < -0.9 and max(ys) < 0.2):
                fuera.append(f)
        bmesh.ops.delete(self.bm, geom=fuera, context='FACES')
        return super().crear()


def norm(v):
    L = math.sqrt(sum(x * x for x in v)) or 1
    return tuple(x / L for x in v)


def bloque(p, c, tam, mat, rumbo=0.0, inclina=0.0, inclina_z=0.0, pivote=None):
    k = p.marca()
    p.cajaJ(c, tam, mat)
    p.girar(k, pivote or c, inclina, rumbo, inclina_z)


def palo(p, a, b, g, mat, giro=0.0, g2=None):
    """caja de sección g x g (o g x g2) entre los puntos a y b"""
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    L = math.sqrt(dx * dx + dy * dy + dz * dz)
    if L < 1e-6:
        return
    c = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2)
    th = math.degrees(math.acos(max(-1, min(1, dy / L))))
    ry = math.degrees(math.atan2(dx, dz))
    k = p.marca()
    p.cajaJ(c, (g, L + g * 0.5, g2 if g2 else g), mat)
    p.girar(k, c, 0, giro)
    p.girar(k, c, th, ry)


def plano(p, base, dirx, diry, ancho, alto, mat):
    """rectángulo plano desde base (centro del borde de abajo) hacia diry"""
    bx, by, bz = base
    ex = (dirx[0] * ancho / 2, dirx[1] * ancho / 2, dirx[2] * ancho / 2)
    ey = (diry[0] * alto, diry[1] * alto, diry[2] * alto)
    a = (bx - ex[0], by - ex[1], bz - ex[2])
    b = (bx + ex[0], by + ex[1], bz + ex[2])
    p.cuad(a, b, (b[0] + ey[0], b[1] + ey[1], b[2] + ey[2]), (a[0] + ey[0], a[1] + ey[1], a[2] + ey[2]), mat)


def roca(p, c, radios, sem, mats, trozos=3):
    """montón de cajas: un bloque grande, otros que asoman por los lados y uno encima.
    mats: un índice o una lista de índices (cada bloque toma el siguiente)"""
    rnd = random.Random(sem)
    rx, ry, rz = radios
    cx, cy, cz = c
    m = (lambda k: mats[k % len(mats)]) if isinstance(mats, (list, tuple)) else (lambda k: mats)
    bloque(p, (cx, cy - ry * 0.15, cz), (rx * 1.6, ry * 1.5, rz * 1.6), m(0), rnd.uniform(-20, 20))
    for k in range(trozos):
        a = k * 6.283 / max(1, trozos) + rnd.uniform(-0.5, 0.5)
        s = rnd.uniform(0.5, 0.75)
        bloque(p, (cx + math.cos(a) * rx * 0.62, cy - ry * (0.35 - s * 0.2), cz + math.sin(a) * rz * 0.62),
               (rx * s * 1.3, ry * s * 1.4, rz * s * 1.2), m(k + 1), rnd.uniform(0, 90), rnd.uniform(-8, 8))
    bloque(p, (cx + rnd.uniform(-0.15, 0.15) * rx, cy + ry * 0.7, cz + rnd.uniform(-0.15, 0.15) * rz),
           (rx * 1.0, ry * 0.55, rz * 0.95), m(trozos + 1), rnd.uniform(-30, 30), rnd.uniform(-6, 6))


def cristal(p, base, r, h, inclina, rumbo, mat, mat_punta=None):
    """prisma cuadrado con la punta escalonada, inclinado desde su base"""
    mp = mat if mat_punta is None else mat_punta
    k = p.marca()
    bx, by, bz = base
    p.cajaJ((bx, by + h * 0.36, bz), (r * 1.6, h * 0.72, r * 1.6), mat)
    p.cajaJ((bx, by + h * 0.8, bz), (r * 1.1, h * 0.16, r * 1.1), mp)
    if r >= 0.1:
        p.cajaJ((bx, by + h * 0.94, bz), (r * 0.55, h * 0.12, r * 0.55), mp)
    p.girar(k, base, inclina, rumbo + 45)


def contar():
    import bpy
    for ob in sorted(bpy.data.objects, key=lambda o: o.name):
        print("PIEZA", ob.name, len(ob.data.polygons) * 2)
