"""Flora y decorado vivo del mundo en estilo bloque: tallos y ramas de cajas, hojas y frondas
de planos rectos escalonados (como en Blockbench), puntas de luz en cubos. El detalle
(fibras, motas, nervios) lo pone la textura pixel art Texturas.fibra() que el juego proyecta
encima, teñida por estos colores. Piezas (base en y = 0, mismas medidas que antes, así el
balanceo con la corriente y los choques siguen igual):
1-2 coral abanico, 3-4 coral de tubos, 5-7 alga cinta, 8-9 alga con vainas, 10-11 planta
lanza, 12-13 flor abisal, 14-16 pasto marino, 17-18 esponja, 19-20 tubo gigante,
21-22 lirio de duna, 23-24 costillas fósiles, 25-26 coral de ceniza, 27-28 anémona,
29-30 hierba alta, 31 hongo luminoso, 32-33 espino urticante."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *
import random
limpiar()
_m = {}
MATS = []


def col(h, e=0.0):
    k = (h, e)
    if k not in _m:
        _m[k] = len(_m)
        MATS.append(material(f"c{h:06x}{'e' if e else ''}", h, emision=e))
    return _m[k]


def mezcla(a, b, t):
    t = max(0, min(1, t))
    return tuple(int(((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) for s in (16, 8, 0))


def hx(t3):
    return (t3[0] << 16) | (t3[1] << 8) | t3[2]


def mez(a, b, t, pasos=4):
    t = round(t * pasos) / pasos
    return col(hx(mezcla(a, b, t)))


class Pieza(PiezaJ):
    def crear(self):
        """quita las caras enterradas (bajo y = -0,02) y las bases planas sobre el suelo"""
        import bmesh
        fuera = []
        for f in self.bm.faces:
            f.normal_update()
            ys = [v.co.z for v in f.verts]
            if max(ys) < -0.02 or (f.normal.z < -0.9 and max(ys) < 0.03):
                fuera.append(f)
        bmesh.ops.delete(self.bm, geom=fuera, context='FACES')
        return super().crear()


piezas = []


def pieza(nombre):
    p = Pieza(f"{nombre}#{len(piezas) + 1}", MATS)
    piezas.append(p)
    return p


def bloque(p, c, tam, mat, rumbo=0.0, inclina=0.0, inclina_z=0.0, pivote=None):
    k = p.marca()
    p.cajaJ(c, tam, mat)
    p.girar(k, pivote or c, inclina, rumbo, inclina_z)


def palo(p, a, b, g, mat, giro=0.0):
    """caja de sección g x g entre los puntos a y b (un tramo de tallo, rama o hueso)"""
    dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    L = math.sqrt(dx * dx + dy * dy + dz * dz)
    if L < 1e-6:
        return
    c = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2)
    th = math.degrees(math.acos(max(-1, min(1, dy / L))))
    ry = math.degrees(math.atan2(dx, dz))
    k = p.marca()
    p.cajaJ(c, (g, L + g * 0.5, g), mat)
    p.girar(k, c, 0, giro)          # giro propio alrededor del tramo
    p.girar(k, c, th, ry)


def plano(p, base, dirx, diry, ancho, alto, mat):
    """rectángulo plano (se ve por las dos caras al cargarse) desde base hacia diry"""
    bx, by, bz = base
    ex = (dirx[0] * ancho / 2, dirx[1] * ancho / 2, dirx[2] * ancho / 2)
    ey = (diry[0] * alto, diry[1] * alto, diry[2] * alto)
    a = (bx - ex[0], by - ex[1], bz - ex[2])
    b = (bx + ex[0], by + ex[1], bz + ex[2])
    p.cuad(a, b, (b[0] + ey[0], b[1] + ey[1], b[2] + ey[2]), (a[0] + ey[0], a[1] + ey[1], a[2] + ey[2]), mat)


def norm(v):
    L = math.sqrt(sum(x * x for x in v)) or 1
    return tuple(x / L for x in v)


# ------------------------------------------------ 1-2 coral abanico: ramas de bloque en abanico
for sem in (1, 6):
    p = pieza("abanico")
    rnd = random.Random(sem)
    base, punta = 0x6a3fb0, 0xe46fb4
    bloque(p, (0, 0.22, 0), (0.26, 0.5, 0.22), col(base))
    for i in range(9):
        a = math.radians(-66 + 132 * i / 8 + rnd.uniform(-5, 5))
        ln = 1.3 + rnd.random() * 0.6
        dx, dy = math.sin(a), math.cos(a)
        prev = (0, 0.42, 0)
        for k in range(3):
            t = (k + 1) / 3
            cur = (dx * ln * t, 0.42 + dy * ln * t, ((k + i) % 2 - 0.5) * 0.06)
            palo(p, prev, cur, 0.13 - k * 0.03, mez(base, punta, t))
            prev = cur
        if i % 2 == 0:
            bloque(p, prev, (0.1, 0.1, 0.1), col(0xffa0e0, 2), i * 20)
        # ramita lateral en el segundo tramo
        if i % 2 == 1:
            m0 = (dx * ln * 0.45, 0.42 + dy * ln * 0.45, 0)
            palo(p, m0, (m0[0] + dy * 0.3 * (1 if i > 4 else -1) + dx * 0.25, m0[1] + 0.3, 0.04), 0.07, mez(base, punta, 0.6))
# ------------------------------------------------ 3-4 coral de tubos: chimeneas cuadradas
for sem in (2, 9):
    p = pieza("tubos")
    rnd = random.Random(sem)
    for i in range(6):
        a = i * 60 + rnd.random() * 30
        d = 0 if i == 0 else 0.35 + rnd.random() * 0.25
        h = 0.6 + rnd.random() * 1.1
        r = 0.12 + rnd.random() * 0.08
        k0 = p.marca()
        p.cajaJ((0, h / 2 - 0.05, d), (r * 2, h + 0.1, r * 2), col(0xe0663c))
        p.cajaJ((0, h * 0.35, d), (r * 2.2, 0.08, r * 2.2), col(0xc85432))            # anillo
        p.cajaJ((0, h + 0.04, d), (r * 2.4, 0.08, r * 2.4), col(0xd85a36))          # labio
        p.cajaJ((0, h + 0.085, d), (r * 1.5, 0.02, r * 1.5), col(0x3a1018))         # boca oscura
        p.cajaJ((0, h + 0.09, d + r * 1.2), (r * 2.4, 0.03, 0.04), col(0x5ff2ff, 3))  # borde de luz
        p.girar(k0, (0, 0, 0), d * 25, a)


# ------------------------------------------------ 5-9 algas cinta (9-12 m) y algas con vainas
def tallo(p, sem, vainas, ox, oz, esc, n):
    """un tallo de kelp en (ox, oz): garra de raíces, tallo de cajas y n frondas planas en
    tres tramos (ancho, más ancho, punta), de verde oliva abajo a dorado arriba"""
    rnd = random.Random(sem)
    alto = (9 + rnd.random() * 3) * esc
    for i in range(2):
        ang = i * math.pi + rnd.random() * 0.5
        c, sn = math.cos(ang), math.sin(ang)
        palo(p, (ox - c * 0.45, -0.02, oz - sn * 0.45), (ox + c * 0.45, -0.02, oz + sn * 0.45), 0.1, col(0x3a3418))
    pts = []
    for k in range(5):
        t = k / 4
        pts.append((ox + math.sin(t * 3 + sem) * 0.5 * t, alto * t, oz + math.cos(t * 2.3 + sem) * 0.4 * t))
    for k in range(4):
        palo(p, pts[k], pts[k + 1], 0.13 - k * 0.015, mez(0x3c4a1c, 0x8a8a30, k / 4), k * 15)
    for i in range(n):
        t = 0.06 + 0.92 * i / n
        k = t * 4
        k0 = int(k)
        f = k - k0
        x0, y0, z0 = pts[k0]
        x1, y1, z1 = pts[min(4, k0 + 1)]
        bx, by, bz = x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, z0 + (z1 - z0) * f
        ang = i * 2.4 + sem
        dx, dz = math.cos(ang), math.sin(ang)
        largo = (1.8 + rnd.random() * 0.9) * (1.1 - t * 0.3) * (0.6 + 0.4 * esc)
        sube = 0.55 + rnd.random() * 0.35
        ancho = 0.75 + rnd.random() * 0.2
        d = norm((dx, sube, dz))
        lado = norm((-dz, 0, dx))
        tono = mez(0x4a5a1e, 0xb0a040, t, 5)
        tono2 = mez(0x405018, 0x98903a, t, 5)
        if i % 4 == 0:     # vejiga de flotación
            bloque(p, (bx + dx * 0.12, by + 0.06, bz + dz * 0.12), (0.14, 0.2, 0.14), col(0x8a8a3a), math.degrees(ang))
        # la fronda: tres placas rectas escalonadas que se doblan un poco hacia fuera
        o = (bx + dx * 0.1, by + 0.05, bz + dz * 0.1)
        for q, (w, l) in enumerate(((0.85, 0.55), (0.6, 0.45))):
            dq = norm((d[0], d[1] - q * 0.18, d[2]))
            plano(p, o, lado, dq, ancho * w, largo * l, tono if q % 2 == 0 else tono2)
            o = (o[0] + dq[0] * largo * l, o[1] + dq[1] * largo * l, o[2] + dq[2] * largo * l)
    if vainas:
        x, _, z = pts[-1]
        for i in range(3):
            a = i * 2.1 + sem
            for k in range(2):
                bloque(p, (x * (0.62 + i * 0.12) + math.cos(a) * 0.45 + k * 0.14, alto * (0.62 + i * 0.12) - k * 0.26,
                           z * (0.62 + i * 0.12) + math.sin(a) * 0.45), (0.3, 0.4, 0.3),
                       col(0xffc060 if k == 1 else 0xff8a30, 4), a * 30)


def alga(sem, vainas):
    p = pieza("alga_vainas" if vainas else "alga")
    rnd = random.Random(sem * 7)
    tallos = 2 if vainas else 3
    for k in range(tallos):
        if k == 0:
            ox = oz = 0
            esc = 1.0
        else:
            a = rnd.uniform(0, 6.28)
            d = 1.1 + rnd.random() * 0.9
            ox, oz = math.cos(a) * d, math.sin(a) * d
            esc = 0.6 + rnd.random() * 0.3
        tallo(p, sem * 3 + k, vainas and k == 0, ox, oz, esc, 9 if k == 0 else 6)


for sem in (4, 11, 17):
    alga(sem, False)
for sem in (5, 13):
    alga(sem, True)
# ------------------------------------------------ 10-11 planta lanza: hojas rectas rojas de punta dorada
for sem in (3, 8):
    p = pieza("lanza")
    rnd = random.Random(sem)
    for i in range(8):
        a = i * 45 + rnd.random() * 30
        incl = 8 + rnd.random() * 26
        h = 1.2 + rnd.random() * 1.1
        k0 = p.marca()
        plano(p, (0, 0, 0.15), (1, 0, 0), (0, 1, 0), 0.3, h * 0.5, col(0xa8302c))
        plano(p, (0, h * 0.5, 0.15), (1, 0, 0), (0, 1, 0), 0.2, h * 0.32, col(0xd04a36))
        plano(p, (0, h * 0.82, 0.15), (1, 0, 0), (0, 1, 0), 0.1, h * 0.18, col(0xffd25a, 3))
        p.girar(k0, (0, 0, 0), incl, a)
    bloque(p, (0, 0.08, 0), (0.6, 0.2, 0.6), col(0x5a2a26), sem * 10)
    bloque(p, (0, 0.2, 0), (0.36, 0.1, 0.36), col(0x6a3430), sem * 10 + 45)
# ------------------------------------------------ 12-13 flor abisal
for sem in (2, 7):
    p = pieza("flor")
    bloque(p, (0, 0.55, 0), (0.1, 1.1, 0.1), col(0x8d8fa8))
    for i in range(3):
        k0 = p.marca()
        plano(p, (0, 0.05, 0.04), (1, 0, 0), norm((0, 0.4, 1)), 0.26, 0.55, col(0x3a3f66))
        p.girar(k0, (0, 0, 0), 0, i * 120)
    for i in range(6):
        k0 = p.marca()
        m = col(0xb46cff if i % 2 == 0 else 0x9a52f0, 3)
        plano(p, (0, 1.1, 0.06), (1, 0, 0), norm((0, 1, 0.75)), 0.24, 0.5, m)
        p.girar(k0, (0, 1.1, 0), 0, i * 60)
    bloque(p, (0, 1.18, 0), (0.18, 0.18, 0.18), col(0xfff0a8, 4), 45)
# ------------------------------------------------ 14-16 pasto marino: hojas rectas cruzadas
for sem in (1, 5, 9):
    p = pieza("pasto")
    rnd = random.Random(sem)
    for i in range(16):
        x, z = rnd.uniform(-0.8, 0.8), rnd.uniform(-0.8, 0.8)
        h = 0.5 + rnd.random() * 0.7
        ang = rnd.uniform(0, math.pi)
        m = mez(0x6e9a3a, 0x9ab84a, rnd.random())
        inc = rnd.uniform(-0.2, 0.2)
        plano(p, (x, 0, z), (math.cos(ang), 0, math.sin(ang)), norm((inc, 1, 0)), 0.17, h * 0.6, m)
        plano(p, (x + inc * h * 0.6, h * 0.6, z), (math.cos(ang), 0, math.sin(ang)), norm((inc * 1.5, 1, 0)), 0.1, h * 0.4, m)
# ------------------------------------------------ 17-18 esponja: barril escalonado con poros
for sem in (1, 4):
    p = pieza("esponja")
    rnd = random.Random(sem)
    g = sem * 20
    bloque(p, (0, 0.2, 0), (0.8, 0.4, 0.8), col(0xb07a3a), g)
    bloque(p, (0, 0.62, 0), (1.0, 0.45, 1.0), col(0xc48a44), g)
    bloque(p, (0, 0.95, 0), (0.92, 0.22, 0.92), col(0xd09a50), g)
    bloque(p, (0, 1.065, 0), (0.6, 0.02, 0.6), col(0x3a2014), g)          # el hueco de arriba
    for i in range(6):
        cara = i % 4
        y = 0.45 + (i // 4) * 0.25 + rnd.uniform(-0.05, 0.05)
        a = math.radians(g + cara * 90)
        r = 0.505
        bloque(p, (math.sin(a) * r, y, math.cos(a) * r), (0.12, 0.12, 0.02), col(0x4a2a1a), g + cara * 90)
# ------------------------------------------------ 19-20 tubos gigantes
for sem in (2, 7):
    p = pieza("tubo_gigante")
    rnd = random.Random(sem)
    for i in range(5):
        a = i * 72 + rnd.random() * 30
        d = 0 if i == 0 else 0.8 + rnd.random() * 0.6
        h = 2.2 + rnd.random() * 2
        r = 0.32 + rnd.random() * 0.15
        k0 = p.marca()
        p.cajaJ((0, h * 0.3 - 0.1, d), (r * 2.5, h * 0.6 + 0.2, r * 2.5), col(0x7a2422))
        p.cajaJ((0, h * 0.8, d), (r * 2.1, h * 0.4 + 0.05, r * 2.1), col(0xa83a30))
        p.cajaJ((0, h * 0.45, d), (r * 2.6, 0.1, r * 2.6), col(0x8a2a26))
        p.cajaJ((0, h + 0.05, d), (r * 2.5, 0.1, r * 2.5), col(0xc04a36))
        p.cajaJ((0, h + 0.105, d), (r * 1.6, 0.02, r * 1.6), col(0xff9a4a, 4))
        p.girar(k0, (0, 0, 0), d * 12, a)
# ------------------------------------------------ 21-22 lirio de duna
for sem in (1, 6):
    p = pieza("lirio")
    rnd = random.Random(sem)
    for i in range(3):
        a = i * 2.1 + sem
        bx, bz = math.cos(a) * 0.35, math.sin(a) * 0.35
        alto = 1.8 + rnd.random() * 0.9
        tx, tz = bx * 1.8, bz * 1.8
        mid = ((bx + tx) / 2, alto * 0.55, (bz + tz) / 2)
        palo(p, (bx, 0, bz), mid, 0.06, col(0x5e8a4a))
        palo(p, mid, (tx, alto, tz), 0.05, col(0x5e8a4a))
        bloque(p, (tx, alto + 0.08, tz), (0.26, 0.16, 0.26), col(0xe8e0f0), math.degrees(a))     # copa
        bloque(p, (tx, alto + 0.22, tz), (0.44, 0.12, 0.44), col(0xf4eefa), math.degrees(a))
        bloque(p, (tx, alto + 0.27, tz), (0.16, 0.16, 0.16), col(0xc080ff, 4), math.degrees(a) + 45)
    for i in range(5):
        a = i * 1.26 + sem
        lx, lz = math.cos(a), math.sin(a)
        plano(p, (lx * 0.05, 0.03, lz * 0.05), (-lz, 0, lx), norm((lx, 0.3, lz)), 0.22, 0.85, col(0x4f7a3c))
# ------------------------------------------------ 23-24 costillas fósiles (16 m a lo largo de x)
for sem in (1, 4):
    p = pieza("costillas")
    hueso = [col(0xd8d0bc), col(0xcfc6b0), col(0xc4bca6)]
    esp = []
    for k in range(7):
        t = k / 6
        esp.append((-8 + 16 * t, 5.2 - abs(t - 0.45) * 3.5, math.sin(t * 3 + sem) * 0.4, 0.45 - abs(t - 0.4) * 0.35))
    for k in range(6):
        a, b = esp[k], esp[k + 1]
        palo(p, a[:3], b[:3], (a[3] + b[3]) * 0.9, hueso[k % 3])
        bloque(p, b[:3], (b[3] * 1.2, b[3] * 2.6, b[3] * 1.2), hueso[(k + 1) % 3])   # vértebra
    for k in range(1, 6):
        x, top, z, _ = esp[k]
        alc = 3.6 + math.sin(k * 1.3) * 0.5
        for lado in (-1, 1):
            prev = None
            for j in range(4):
                u = j / 3
                ang = u * 1.75
                cur = (x + u * 0.6, top * math.cos(ang * 0.9) - u * 0.8, z + lado * alc * math.sin(ang))
                if prev:
                    palo(p, prev, cur, 0.36 - u * 0.14, hueso[k % 3])
                prev = cur
    cz = esp[6][2]
    bloque(p, (8.8, 1.6, cz), (2.8, 2.2, 2.4), hueso[1])               # cráneo
    bloque(p, (9.9, 1.2, cz), (0.9, 0.5, 1.6), hueso[2])               # hocico
    bloque(p, (9.9, 0.85, cz), (0.8, 0.2, 1.4), hueso[0])              # mandíbula
    for s in (-1, 1):
        bloque(p, (9.6, 1.95, cz + s * 0.6), (0.6, 0.45, 0.4), col(0x3a342c))     # cuencas
# ------------------------------------------------ 25-26 coral de ceniza (grietas termales)
for sem in (3, 8):
    p = pieza("coral_ceniza")
    rnd = random.Random(sem)
    for i in range(6):
        a = i * 1.05 + sem
        d = 0.2 + rnd.random() * 0.5
        h = 0.9 + rnd.random() * 1.2
        x, z = math.cos(a) * d, math.sin(a) * d
        g = math.degrees(a)
        bloque(p, (x, h * 0.3, z), (0.3, h * 0.6 + 0.05, 0.3), col(0x2a2426), g)
        bloque(p, (x + 0.05, h * 0.78, z + 0.02), (0.22, h * 0.4, 0.22), col(0x3a2c2a), g + 10)
        bloque(p, (x + 0.06, h + 0.02, z + 0.03), (0.2, 0.1, 0.2), col(0xff6a20 if i % 2 == 0 else 0xffa040, 4), g + 20)
# ------------------------------------------------ 27-28 anémonas
for sem in (1, 2):
    p = pieza("anemona")
    c0 = 0xe0604a if sem % 2 == 0 else 0x9a5ae0
    claro = hx(mezcla(c0, 0xffffff, 0.3))
    bloque(p, (0, 0.14, 0), (0.42, 0.28, 0.42), col(0x6a4a3a), sem * 20)
    bloque(p, (0, 0.34, 0), (0.52, 0.12, 0.52), col(hx(mezcla(c0, 0x402020, 0.4))), sem * 20)
    bloque(p, (0, 0.405, 0), (0.24, 0.03, 0.24), col(hx(mezcla(c0, 0xffffff, 0.6)), 4), sem * 20)
    for i in range(10):
        a = i * math.tau / 10 + sem
        c, s = math.cos(a), math.sin(a)
        r0 = 0.17 + (i % 2) * 0.05
        m = col(c0 if i % 2 == 0 else claro)
        mid = (c * (r0 + 0.1), 0.6, s * (r0 + 0.1))
        palo(p, (c * r0, 0.38, s * r0), mid, 0.05, m)
        palo(p, mid, (c * (r0 + 0.22), 0.8, s * (r0 + 0.22)), 0.035, m)
# ------------------------------------------------ 29-30 hierba alta (2 m) donde se esconden mordedores
for sem in (1, 4):
    p = pieza("hierba")
    rnd = random.Random(sem * 601)
    for i in range(34):
        a = rnd.random() * 6.283
        d = math.sqrt(rnd.random()) * 1.4
        x, z = math.cos(a) * d, math.sin(a) * d
        h = 0.8 + rnd.random() * 0.5
        inc = rnd.uniform(-0.4, 0.4)
        ang = rnd.random() * 6.283
        m = mez(0x6a7a30, 0x9a5a2a, rnd.random())
        dirx = (math.cos(ang), 0, math.sin(ang))
        up = norm((inc * math.cos(a) * 0.7, 1, inc * math.sin(a) * 0.7))
        plano(p, (x, 0, z), dirx, up, 0.3, h * 0.55, m)
        top = (x + up[0] * h * 0.55, up[1] * h * 0.55, z + up[2] * h * 0.55)
        plano(p, top, dirx, norm((up[0] * 1.6, 1, up[2] * 1.6)), 0.16, h * 0.45, m)
# ------------------------------------------------ 31 hongos luminosos
p = pieza("hongo")
for i in range(5):
    x, z = ((i % 3) - 1) * 0.2, (i // 3 - 0.5) * 0.22
    h = 0.15 + i * 0.05
    bloque(p, (x, h / 2, z), (0.05, h, 0.05), col(0xd8d0c0), i * 17)
    bloque(p, (x, h + 0.02, z), (0.24, 0.05, 0.24), col(0x9affc0 if i % 2 == 0 else 0xc0ffe0, 4), i * 17)
    bloque(p, (x, h + 0.06, z), (0.14, 0.04, 0.14), col(0xc0ffe0 if i % 2 == 0 else 0x9affc0, 4), i * 17)
# ------------------------------------------------ 32-33 espino urticante
for sem in (1, 5):
    p = pieza("espino")
    rnd = random.Random(sem)
    for i in range(7):
        a = i * 0.9 + sem
        inc = 0.25 + rnd.random() * 0.35
        h = 0.8 + rnd.random() * 0.5
        tx, tz = math.cos(a) * inc, math.sin(a) * inc
        palo(p, (0, 0, 0), (tx, h, tz), 0.045, col(0xc8c0d8), math.degrees(a))
        for k in range(1, 4):
            f = k / 4
            x, y, z = tx * f, h * f, tz * f
            s = 1 if k % 2 else -1
            plano(p, (x, y, z), (0, 1, 0), norm((s * -tz, 0.3, s * tx)), 0.04, 0.13, col(0xd060ff, 3))
        bloque(p, (tx, h, tz), (0.11, 0.13, 0.11), col(0xf0b0ff, 4), math.degrees(a))
for p in piezas:
    p.crear()
print("PIEZAS", len(piezas), "TRIS", triangulos())
for p in piezas:
    pass
for ob in bpy.data.objects:
    print("PIEZA", ob.name, len(ob.data.polygons) * 2)
