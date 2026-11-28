"""Flora y decorado vivo del mundo. Piezas (base en y = 0, mismas medidas que antes, así el
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
def col(h, e=0.0):
    k = (h, e)
    if k not in _m:
        _m[k] = len(_m)
        MATS.append(material(f"c{h:06x}{'e' if e else ''}", h, emision=e))
    return _m[k]
MATS = []
def mezcla(a, b, t):
    t = max(0, min(1, t))
    return tuple(int(((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) for s in (16, 8, 0))
def hx(t3):
    return (t3[0] << 16) | (t3[1] << 8) | t3[2]
def mez(a, b, t, pasos=4):
    # quantised so similar tones share a material
    t = round(t * pasos) / pasos
    return col(hx(mezcla(a, b, t)))

piezas = []
def pieza(nombre):
    p = PiezaJ(f"{nombre}#{len(piezas) + 1}", MATS)
    piezas.append(p)
    return p

# 1-2 coral abanico: ramas gruesas en abanico con red entre ellas
for sem in (1, 6):
    p = pieza("abanico")
    rnd = random.Random(sem)
    base, punta = 0x6a3fb0, 0xe46fb4
    p.torno([(0.13, 0), (0.1, 0.5)], 6, col(base), eje='y')
    puntas = []
    for i in range(13):
        a = math.radians(-70 + 140 * i / 12 + rnd.uniform(-5, 5))
        ln = 1.3 + rnd.random() * 0.6
        dx, dy = math.sin(a), math.cos(a)
        pts = []
        for k in range(4):
            t = k / 3
            pts.append((dx * ln * t, 0.45 + dy * ln * t, math.sin(t * 3 + i) * 0.05, 0.075 * (1 - t) + 0.03))
        p.tubo(pts, 4, [mez(base, punta, (k + 1) / 3) for k in range(4)], False)
        puntas.append((dx * ln * 0.6, 0.45 + dy * ln * 0.6))
    for i in range(0, 13, 3):
        x, y = puntas[i]
        p.elip((x * 1.6, (y - 0.45) * 1.6 + 0.45, 0), (0.06, 0.06, 0.06), 4, 2, col(0xffa0e0, 2))
# 3-4 coral de tubos
for sem in (2, 9):
    p = pieza("tubos")
    rnd = random.Random(sem)
    for i in range(7):
        a = i * 51.4 + rnd.random() * 30
        d = 0 if i == 0 else 0.35 + rnd.random() * 0.25
        h = 0.6 + rnd.random() * 1.1
        r = 0.12 + rnd.random() * 0.08
        k0 = p.marca()
        p.torno([(r * 1.25, -0.1), (r, h * 0.5), (r * 1.15, h)], 6, [col(0xe0663c), col(0xd85a36)], eje='y', centro=(0, 0, d))
        p.torno([(r * 1.15, h), (r * 0.82, h), (r * 0.7, h * 0.4), (0, h * 0.4)], 6, [col(0xc85432), col(0x5a1c24), col(0x3a1018)], eje='y', centro=(0, 0, d))
        p.torno([(r * 1.15, h), (r * 1.3, h + 0.05), (r * 1.05, h + 0.06)], 6, col(0x5ff2ff, 3), eje='y', centro=(0, 0, d))
        p.girar(k0, (0, 0, 0), d * 25, a)
# 5-9 algas cinta (9-12 m) y algas con vainas
def alga(sem, vainas):
    """un grupo de kelp: dos o tres tallos de alturas distintas en una sola malla (un bosque
    más denso con el mismo número de llamadas de dibujo)"""
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
        tallo(p, sem * 3 + k, vainas and k == 0, ox, oz, esc, 13 if k == 0 else 9)


def tallo(p, sem, vainas, ox, oz, esc, n):
    """un tallo de kelp gigante en (ox, oz): fijación ramificada, tallo fino ondulado y n
    frondas largas (se ensanchan y se afinan) que salen hacia arriba con una vejiga de
    flotación en su base; de verde oliva oscuro abajo a dorado arriba"""
    rnd = random.Random(sem)
    alto = (9 + rnd.random() * 3) * esc
    # fijación: raíces en garra
    for i in range(4):
        ang = i * math.tau / 4 + rnd.random() * 0.5
        c, sn = math.cos(ang), math.sin(ang)
        p.tubo([(ox, 0.25, oz, 0.07), (ox + c * 0.3, 0.1, oz + sn * 0.3, 0.05), (ox + c * 0.55, -0.05, oz + sn * 0.55, 0.03)], 4, col(0x3a3418), False)
    # tallo
    pts = []
    for k in range(13):
        t = k / 12
        pts.append((ox + math.sin(t * 3 + sem) * 0.5 * t, alto * t, oz + math.cos(t * 2.3 + sem) * 0.4 * t, 0.06 * (1 - t * 0.6) + 0.02))
    p.tubo(pts, 4, [mez(0x3c4a1c, 0x8a8a30, k / 12) for k in range(13)], False)
    # frondas a lo largo del tallo, alternando el lado y girando en espiral
    for i in range(n):
        t = 0.06 + 0.92 * i / n
        k = t * 12
        k0 = int(k)
        f = k - k0
        x0, y0, z0, _ = pts[k0]
        x1, y1, z1, _ = pts[min(12, k0 + 1)]
        bx, by, bz = x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, z0 + (z1 - z0) * f
        ang = i * 2.4 + sem
        dx, dz = math.cos(ang), math.sin(ang)
        largo = (1.4 + rnd.random() * 0.8) * (1.1 - t * 0.3) * (0.6 + 0.4 * esc)
        sube = 0.55 + rnd.random() * 0.35            # cuánto apunta hacia arriba
        ancho = 0.3 + rnd.random() * 0.1
        tono = mez(0x4a5a1e, 0xb0a040, t, 5)
        tono2 = mez(0x405018, 0x98903a, t, 5)
        # vejiga de flotación en la base de la fronda
        p.elip((bx + dx * 0.12, by + 0.05, bz + dz * 0.12), (0.08, 0.11, 0.08), 4, 2, col(0x8a8a3a))
        # la fronda: 4 tramos con el ancho en forma de hoja, curvándose hacia fuera y retorcida
        prev = None
        for q in range(5):
            u = q / 4
            w = ancho * math.sin(math.pi * min(1, u * 1.15 + 0.08))      # estrecha-ancha-estrecha
            px = bx + dx * (0.12 + largo * u)
            pz = bz + dz * (0.12 + largo * u)
            py = by + 0.05 + largo * u * sube + math.sin(u * 3 + i) * 0.06
            tw = u * 0.9 + i                                              # torsión
            ex, ez = -dz * math.cos(tw), dx * math.cos(tw)
            ey = math.sin(tw) * 0.5
            cur = ((px - ex * w, py - ey * w, pz - ez * w), (px + ex * w, py + ey * w, pz + ez * w))
            if prev:
                p.cuad(prev[0], prev[1], cur[1], cur[0], tono if q % 2 else tono2)
            prev = cur
    if vainas:
        x, _, z, _ = pts[-1]
        for i in range(3):
            a = i * 2.1 + sem
            for k in range(3):
                p.elip((x * (0.62 + i * 0.12) + math.cos(a) * 0.45 + k * 0.12, alto * (0.62 + i * 0.12) - k * 0.2,
                        z * (0.62 + i * 0.12) + math.sin(a) * 0.45), (0.17, 0.24, 0.17), 6, 3, col(0xffc060 if k == 1 else 0xff8a30, 4))
for sem in (4, 11, 17):
    alga(sem, False)
for sem in (5, 13):
    alga(sem, True)
# 10-11 planta lanza: hojas en lanza rojas de punta dorada
for sem in (3, 8):
    p = pieza("lanza")
    rnd = random.Random(sem)
    for i in range(9):
        a = i * 40 + rnd.random() * 30
        incl = 8 + rnd.random() * 26
        h = 1.2 + rnd.random() * 1.1
        w = 0.2
        k0 = p.marca()
        p.cuad((-w, 0, 0.12), (0, 0, 0.18), (0, h * 0.8, 0.15), (-w * 0.3, h * 0.45, 0.13), col(0xa8302c), doble=True)
        p.cuad((0, 0, 0.18), (w, 0, 0.12), (w * 0.3, h * 0.45, 0.13), (0, h * 0.8, 0.15), col(0xd04a36), doble=True)
        p.tri((-w * 0.5, h * 0.8, 0.15), (w * 0.5, h * 0.8, 0.15), (0, h, 0.15), col(0xffd25a, 3), doble=True)
        p.girar(k0, (0, 0, 0), incl, a)
    p.torno([(0.32, 0), (0.25, 0.18), (0, 0.22)], 7, col(0x5a2a26), eje='y')
# 12-13 flor abisal
for sem in (2, 7):
    p = pieza("flor")
    p.torno([(0.09, 0), (0.06, 0.6), (0.05, 1.1)], 6, col(0x8d8fa8), eje='y')
    for i in range(3):
        k0 = p.marca()
        p.cuad((0, 0.05, 0), (0.14, 0.08, 0.55), (0, 0.3, 0.45), (-0.14, 0.08, 0.55), col(0x3a3f66), doble=True)
        p.girar(k0, (0, 0, 0), 0, i * 120)
    for i in range(6):
        k0 = p.marca()
        m = col(0xb46cff if i % 2 == 0 else 0x9a52f0, 3)
        p.cuad((-0.12, 1.1, 0.05), (0.12, 1.1, 0.05), (0.3, 1.5, 0.34), (-0.08, 1.56, 0.38), m, doble=True)
        p.girar(k0, (0, 1.1, 0), 0, i * 60)
    p.elip((0, 1.18, 0), (0.1, 0.1, 0.1), 6, 3, col(0xfff0a8, 4))
# 14-16 pasto marino
for sem in (1, 5, 9):
    p = pieza("pasto")
    rnd = random.Random(sem)
    for i in range(14):
        x, z = rnd.uniform(-0.8, 0.8), rnd.uniform(-0.8, 0.8)
        h = 0.5 + rnd.random() * 0.7
        inc = rnd.uniform(-0.25, 0.25)
        m = mez(0x6e9a3a, 0x9ab84a, rnd.random())
        p.cuad((x - 0.05, 0, z), (x + 0.05, 0, z), (x + inc + 0.02, h * 0.6, z + inc * 0.3), (x + inc - 0.02, h * 0.6, z + inc * 0.3), m, doble=True)
        p.tri((x + inc - 0.02, h * 0.6, z + inc * 0.3), (x + inc + 0.02, h * 0.6, z + inc * 0.3), (x + inc * 1.6, h, z + inc * 0.6), m, doble=True)
# 17-18 esponja: barril con poros
for sem in (1, 4):
    p = pieza("esponja")
    rnd = random.Random(sem)
    p.torno([(0.35, 0), (0.5, 0.35), (0.55, 0.8), (0.48, 1.05)], 10, [col(0xb07a3a), col(0xc48a44), col(0xd09a50)], eje='y')
    p.torno([(0.48, 1.05), (0.38, 1.05), (0.3, 0.4), (0, 0.4)], 10, [col(0x6a4a2a), col(0x4a2a1a), col(0x3a2014)], eje='y')
    for i in range(10):
        a = rnd.uniform(0, 6.28)
        y = rnd.uniform(0.25, 0.9)
        r = 0.36 + y * 0.2
        p.elip((math.cos(a) * r, y, math.sin(a) * r), (0.05, 0.05, 0.05), 4, 2, col(0x6a4a2a))
# 19-20 tubos gigantes
for sem in (2, 7):
    p = pieza("tubo_gigante")
    rnd = random.Random(sem)
    for i in range(5):
        a = i * 72 + rnd.random() * 30
        d = 0 if i == 0 else 0.8 + rnd.random() * 0.6
        h = 2.2 + rnd.random() * 2
        r = 0.32 + rnd.random() * 0.15
        k0 = p.marca()
        p.torno([(r * 1.3, -0.2), (r, h * 0.6), (r * 1.2, h)], 7, [col(0x7a2422), col(0xa83a30)], eje='y', centro=(0, 0, d))
        p.torno([(r * 1.2, h), (r * 0.8, h), (r * 0.7, h * 0.5)], 7, [col(0xc04a36), col(0x3a1012)], eje='y', centro=(0, 0, d))
        p.torno([(r * 0.75, h * 0.62), (0, h * 0.6)], 7, col(0xff9a4a, 4), eje='y', centro=(0, 0, d))
        p.torno([(r * 1.06 + 0.02, h * 0.45), (r * 1.06 + 0.02, h * 0.45 + 0.08)], 7, col(0x8a2a26), eje='y', centro=(0, 0, d))
        p.girar(k0, (0, 0, 0), d * 12, a)
# 21-22 lirio de duna
for sem in (1, 6):
    p = pieza("lirio")
    rnd = random.Random(sem)
    for i in range(3):
        a = i * 2.1 + sem
        bx, bz = math.cos(a) * 0.35, math.sin(a) * 0.35
        alto = 1.8 + rnd.random() * 0.9
        tx, tz = bx * 1.8, bz * 1.8
        p.tubo([(bx, 0, bz, 0.035), ((bx + tx) / 2, alto * 0.55, (bz + tz) / 2, 0.03), (tx, alto, tz, 0.025)], 4, col(0x5e8a4a), False)
        p.torno([(0.02, 0), (0.22, 0.18), (0.3, 0.34)], 8, col(0xe8e0f0), eje='y', centro=(tx, alto, tz))
        p.elip((tx, alto + 0.05, tz), (0.09, 0.12, 0.09), 5, 3, col(0xc080ff, 4))
    for i in range(5):
        a = i * 1.26 + sem
        lx, lz = math.cos(a), math.sin(a)
        p.tri((0, 0.05, 0), (lx * 0.9 - lz * 0.15, 0.3, lz * 0.9 + lx * 0.15), (lx * 0.9 + lz * 0.15, 0.3, lz * 0.9 - lx * 0.15), col(0x4f7a3c), doble=True)
# 23-24 costillas fósiles (16 m a lo largo de x)
for sem in (1, 4):
    p = pieza("costillas")
    hueso = [col(0xd8d0bc), col(0xcfc6b0), col(0xc4bca6)]
    esp = []
    for k in range(7):
        t = k / 6
        esp.append((-8 + 16 * t, 5.2 - abs(t - 0.45) * 3.5, math.sin(t * 3 + sem) * 0.4, 0.45 - abs(t - 0.4) * 0.35))
    p.tubo(esp, 6, [hueso[k % 3] for k in range(7)], True)
    for k in range(1, 6):
        x, top, z, _ = esp[k]
        alc = 3.6 + math.sin(k * 1.3) * 0.5
        for lado in (-1, 1):
            pts = []
            for j in range(5):
                u = j / 4
                ang = u * 1.75
                pts.append((x + u * 0.6, top * math.cos(ang * 0.9) - u * 0.8, z + lado * alc * math.sin(ang), 0.22 - u * 0.1))
            p.tubo(pts, 4, hueso[k % 3], True)
    p.rocaJ((8.8, 1.6, esp[6][2]), (1.6, 1.2, 1.3), sem * 11, 0.15, 1, hueso[1])
    p.cajaJ((9.8, 1.25, esp[6][2]), (0.25, 0.18, 1.0), hueso[2])
    for s in (-1, 1):
        p.elip((9.3, 1.9, esp[6][2] + s * 0.55), (0.25, 0.2, 0.2), 5, 3, col(0x3a342c))      # cuencas
# 25-26 coral de ceniza (grietas termales)
for sem in (3, 8):
    p = pieza("coral_ceniza")
    rnd = random.Random(sem)
    for i in range(6):
        a = i * 1.05 + sem
        d = 0.2 + rnd.random() * 0.5
        h = 0.9 + rnd.random() * 1.2
        x, z = math.cos(a) * d, math.sin(a) * d
        p.tubo([(x, 0, z, 0.16), (x + 0.05, h * 0.6, z, 0.12), (x + 0.08, h, z + 0.04, 0.1)], 6, [col(0x2a2426), col(0x3a2c2a)], False)
        p.elip((x + 0.08, h, z + 0.04), (0.11, 0.07, 0.11), 6, 2, col(0xff6a20 if i % 2 == 0 else 0xffa040, 4))
# 27-28 anémonas
for sem in (1, 2):
    p = pieza("anemona")
    c0 = 0xe0604a if sem % 2 == 0 else 0x9a5ae0
    claro = hx(mezcla(c0, 0xffffff, 0.3))
    p.torno([(0.22, 0), (0.2, 0.3), (0.26, 0.38), (0, 0.4)], 9, [col(0x6a4a3a), col(hx(mezcla(c0, 0x402020, 0.4))), col(c0)], eje='y')
    for i in range(14):
        a = i * math.tau / 14 + sem
        c, s = math.cos(a), math.sin(a)
        r0 = 0.18 + (i % 2) * 0.04
        p.tubo([(c * r0, 0.38, s * r0, 0.025), (c * (r0 + 0.12), 0.6, s * (r0 + 0.12), 0.02), (c * (r0 + 0.22), 0.78, s * (r0 + 0.22), 0.012)],
               4, col(c0 if i % 2 == 0 else claro), False)
    p.elip((0, 0.4, 0), (0.08, 0.04, 0.08), 6, 2, col(hx(mezcla(c0, 0xffffff, 0.6)), 4))
# 29-30 hierba alta (2 m) donde se esconden mordedores
for sem in (1, 4):
    p = pieza("hierba")
    rnd = random.Random(sem * 601)
    for i in range(42):
        a = rnd.random() * 6.283
        d = math.sqrt(rnd.random()) * 1.4
        x, z = math.cos(a) * d, math.sin(a) * d
        h = 0.8 + rnd.random() * 0.5
        inc = rnd.uniform(-0.4, 0.4)
        ang = rnd.random() * 6.283
        bx, bz = math.cos(ang) * 0.16, math.sin(ang) * 0.16
        m = mez(0x6a7a30, 0x9a5a2a, rnd.random())
        mx, mz = x + inc * math.cos(a) * 0.4, z + inc * math.sin(a) * 0.4
        p.cuad((x - bx, 0, z - bz), (x + bx, 0, z + bz), (mx + bx * 0.5, h * 0.55, mz + bz * 0.5), (mx - bx * 0.5, h * 0.55, mz - bz * 0.5), m, doble=True)
        p.tri((mx - bx * 0.5, h * 0.55, mz - bz * 0.5), (mx + bx * 0.5, h * 0.55, mz + bz * 0.5), (x + inc * math.cos(a), h, z + inc * math.sin(a)), m, doble=True)
# 31 hongos luminosos
p = pieza("hongo")
for i in range(5):
    x, z = ((i % 3) - 1) * 0.2, (i // 3 - 0.5) * 0.22
    h = 0.15 + i * 0.05
    p.torno([(0.03, 0), (0.025, h)], 5, col(0xd8d0c0), eje='y', centro=(x, 0, z))
    p.torno([(0.13, h), (0.1, h + 0.05), (0, h + 0.07)], 7, col(0x9affc0 if i % 2 == 0 else 0xc0ffe0, 4), eje='y', centro=(x, 0, z))
    p.torno([(0.12, h - 0.005), (0, h - 0.01)], 7, col(0x6ab890), eje='y', centro=(x, 0, z))
# 32-33 espino urticante
for sem in (1, 5):
    p = pieza("espino")
    rnd = random.Random(sem)
    for i in range(9):
        a = i * 0.7 + sem
        inc = 0.25 + rnd.random() * 0.35
        h = 0.8 + rnd.random() * 0.5
        tx, tz = math.cos(a) * inc, math.sin(a) * inc
        p.tubo([(0, 0, 0, 0.025), (tx * 0.5, h * 0.5, tz * 0.5, 0.02), (tx, h, tz, 0.012)], 4, col(0xc8c0d8), False)
        for k in range(1, 4):
            f = k / 4
            x, y, z = tx * f, h * f, tz * f
            p.tri((x, y, z), (x + 0.12, y + 0.05, z + 0.02), (x, y + 0.04, z), col(0xd060ff, 3), doble=True)
            p.tri((x, y + 0.02, z), (x - 0.02, y + 0.07, z + 0.12), (x, y + 0.05, z), col(0xd060ff, 3), doble=True)
        p.elip((tx, h, tz), (0.06, 0.08, 0.06), 5, 3, col(0xf0b0ff, 4))
for p in piezas:
    p.crear()
print("PIEZAS", len(piezas), "TRIS", triangulos())
