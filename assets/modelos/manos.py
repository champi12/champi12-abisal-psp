"""Manos y herramientas en primera persona (14 piezas en un archivo).

Mismas medidas que las manos hechas por código, para que las animaciones del juego sirvan
igual: el mango de cada herramienta va a lo largo del eje Z (la herramienta apunta a -Z),
la mano se cierra alrededor de él en x = cx con radio r, y el antebrazo llega hasta el codo
en z = 0,84 (el pivote de todas las animaciones). Coordenadas del juego (PiezaJ).

Piezas: 1 cuchillo, 2 linterna, 3 boya, 4 mano izquierda vacía, 5 mano derecha vacía,
6 arpón cargado, 7 arpón vacío, 8 bengala, 9 soldador, 10 impulsor, 11 hélice del impulsor,
12 escáner, 13 vara de descarga, 14 saeta del arpón (en vuelo, a lo largo de +Z).
"""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bm import *

limpiar()
GUANTE = material("guante", 0x2c4250, rugosidad=0.8)
DEDO = material("dedos", 0x263640, rugosidad=0.8)
PALMA = material("palma", 0x46606e, rugosidad=0.9)
MANGA = material("manga", 0x30485a, rugosidad=0.7)
COSTURA = material("costura", 0x1e3040, rugosidad=0.8)
NARANJA = material("naranja", 0xe0702c)
METAL = material("metal", 0x5c666e, metal=0.5, rugosidad=0.4)
OSCURO = material("oscuro", 0x22282e)
HOJA = material("hoja", 0x9aa6ac, metal=0.6, rugosidad=0.25)
FILO = material("filo", 0xffffff, emision=2)
CIAN = material("cian", 0x6ff0ff, emision=4)
LENTE = material("lente", 0xfff2c0, emision=6)
ROJO = material("rojo", 0xc82820)
BLANCO = material("blanco", 0xc8c4bc)
AMARILLO = material("amarillo", 0xf0c040)
COBRE = material("cobre", 0xc87a3a, metal=0.5)
CALIENTE = material("caliente", 0xffb060, emision=6)
CHISPA = material("rayo", 0x80e0ff, emision=6)
PANTALLA = material("pantalla", 0x1f7078, emision=2)
GOMA = material("goma", 0x1a1c1e, rugosidad=1.0)
SIERRA = material("sierra", 0x6a767c, metal=0.6)

MATS = [GUANTE, DEDO, PALMA, MANGA, COSTURA, NARANJA, METAL, OSCURO, HOJA, FILO, CIAN, LENTE, ROJO, BLANCO,
        AMARILLO, COBRE, CALIENTE, CHISPA, PANTALLA, GOMA, SIERRA]
G, D, P, M, C, N, MT, OS, H, F, CI, L, R, B, A, CO, CA, CH, PA, GO, SI = range(len(MATS))
ARM = 0.105  # tamaño fijo del brazo: el izquierdo y el derecho iguales


def arco(p, cx, cy, z, radio, grosor, a0, a1, segs, mat, lados=5):
    """un tramo de toro alrededor del eje Z (un dedo que rodea el mango)"""
    pts = []
    for i in range(segs + 1):
        a = math.radians(a0 + (a1 - a0) * i / segs)
        pts.append((cx + math.cos(a) * radio, cy + math.sin(a) * radio, a))
    anillos = []
    for (x, y, a) in pts:
        fila = []
        for k in range(lados):
            t = k * math.tau / lados
            # el tubo: dirección radial (cos a, sin a) y el eje Z
            dx, dy, dz = math.cos(a) * math.cos(t) * grosor, math.sin(a) * math.cos(t) * grosor, math.sin(t) * grosor
            fila.append(p.bm.verts.new(J(x + dx, y + dy, z + dz)))
        anillos.append(fila)
    for i in range(segs):
        for k in range(lados):
            q = (k + 1) % lados
            f = p.bm.faces.new((anillos[i][k], anillos[i][q], anillos[i + 1][q], anillos[i + 1][k]))
            f.material_index = mat
    # puntas redondeadas
    for extremo, sentido in ((anillos[0], -1), (anillos[-1], 1)):
        try:
            p.bm.faces.new(extremo if sentido > 0 else list(reversed(extremo))).material_index = mat
        except ValueError:
            pass


def guante(p, cx, r, izquierda):
    """guante cerrado sobre un mango en x = cx de radio r, con antebrazo hasta el codo.
    El puño mide lo mismo en las dos manos: sobre un mango fino (cuchillo, vara, arpón)
    los dedos se cierran más gruesos en vez de encoger la mano entera."""
    R = 0.042                      # radio del puño (el del mango grueso de la izquierda)
    d = 2 * R + 0.026
    extra = max(0.0, R - r)        # hueco entre el mango y el puño: lo llenan los dedos
    # dorso de la mano (redondeado) y palma
    p.elip((cx, -0.004, 0.05), (d / 2 + 0.008, d / 2 + 0.006, 0.065), 7, 3, G)
    p.elip((cx, -0.016 - extra * 0.4, -0.005), (d / 2 - 0.002, 0.014, 0.045), 5, 2, P)
    # cuatro dedos que rodean el mango (dejan libre la parte de arriba para el pulgar)
    for i in range(4):
        k = 1 - i * 0.05
        fz = -0.045 + i * 0.027
        grosor = (0.0115 + extra * 0.45) * k + 0.001
        anillo = (r + 0.011 + extra * 0.55) * k + 0.002
        arco(p, cx, -0.004, fz, anillo, grosor, 115, 420, 5, D, lados=4)
        # nudillo y yema más claras
        p.elip((cx + (anillo + 0.001) * math.cos(math.radians(380)), -0.004 + (anillo + 0.001) * math.sin(math.radians(380)), fz),
               (0.009 + extra * 0.2, 0.009 + extra * 0.2, 0.01), 3, 2, P)
    # cresta de nudillos sobre el dorso
    p.elip((cx + 0.004, 0.013, -0.028), (d / 2, 0.009, 0.045), 5, 2, G)
    # pulgar: dos falanges sobre el mango (a la altura del puño, no del mango)
    p.elip((cx - 0.01, R + 0.016, -0.012), (0.016, 0.012, 0.032), 5, 3, G)
    p.elip((cx - 0.004, R + 0.01, -0.044), (0.012, 0.009, 0.018), 4, 2, P)
    # muñeca y puño del traje con su anillo de cierre
    p.torno([(0.05, 0.09), (0.054, 0.15)], 8, G, centro=(cx + 0.008, -0.008, 0), aplastar=(1, 1))
    p.torno([(0.062, 0.155), (0.064, 0.175), (0.06, 0.18)], 8, [OS, OS], centro=(cx + 0.012, -0.008, 0))
    p.torno([(0.064, 0.18), (0.066, 0.28)], 8, M, centro=(cx + 0.012, -0.008, 0))
    p.cajaJ((cx + 0.012, ARM / 2 + 0.012, 0.2), (ARM + 0.02, 0.01, 0.035), N)       # franja naranja del puño
    # antebrazo dentro de la manga, hasta el codo (fuera de pantalla)
    # la manga acaba en z = 0,74: más allá queda fuera de la pantalla (y detrás del plano
    # cercano de la cámara de las manos, lo que obligaría a recortarla cada frame)
    p.torno([(0.066, 0.28), (0.072, 0.5), (0.075, 0.74), (0, 0.745)], 8, M, centro=(cx + 0.012, -0.012, 0))
    p.cajaJ((cx + 0.012, -0.012 + 0.075, 0.5), (0.016, 0.012, 0.44), N)                # franja a lo largo
    p.cajaJ((cx + 0.012, -0.012 - 0.075, 0.5), (0.014, 0.01, 0.42), C)                 # costura de abajo
    if izquierda:
        # ordenador de buceo en la muñeca izquierda: pantalla cian
        p.cajaJ((cx + 0.012, 0.062, 0.33), (0.07, 0.022, 0.07), OS)
        p.cajaJ((cx + 0.012, 0.074, 0.33), (0.052, 0.004, 0.05), PA)


def pieza(nombre, num, cx, r, izquierda, extra):
    p = PiezaJ(f"{nombre}#{num}", MATS)
    guante(p, cx, r, izquierda)
    if extra:
        extra(p)
    p.crear()


def cuchillo_desplazado(p):
    """cuchillo de buceo: mango de goma con huecos para los dedos, guarda, hoja con punta
    recortada, sierra en el lomo y acanaladura; pomo martillo con su agujero de cordón"""
    x = 0.02
    # mango (va dentro del puño): goma con anillos de agarre
    p.torno([(0.017, 0.055), (0.019, 0.03), (0.016, 0.0), (0.019, -0.03), (0.016, -0.06), (0.02, -0.075)], 8,
            [GO, GO, GO, GO, GO], centro=(x, 0, 0), aplastar=(0.8, 1))
    # pomo martillo de acero
    p.torno([(0.02, 0.055), (0.022, 0.07), (0.018, 0.08), (0, 0.082)], 8, [MT, MT, MT], centro=(x, 0, 0))
    p.torno([(0.006, 0.083), (0.006, 0.072)], 5, OS, centro=(x, 0.012, 0))
    # guarda: placa con dos gavilanes curvados hacia la hoja
    p.cajaJ((x, 0, -0.082), (0.02, 0.07, 0.012), MT)
    p.cuad((x - 0.008, 0.035, -0.078), (x + 0.008, 0.035, -0.078), (x + 0.006, 0.05, -0.098), (x - 0.006, 0.05, -0.098), MT)
    p.cuad((x - 0.008, -0.035, -0.078), (x - 0.006, -0.054, -0.1), (x + 0.006, -0.054, -0.1), (x + 0.008, -0.035, -0.078), MT)
    p.elip((x + 0.011, 0, -0.082), (0.004, 0.005, 0.005), 4, 2, B)
    p.elip((x - 0.011, 0, -0.082), (0.004, 0.005, 0.005), 4, 2, B)
    # hoja: (z, lomo, filo). Vientre curvo y punta recortada (el lomo baja antes de la punta)
    t, te = 0.0085, 0.0014
    perfil = [(-0.09, 0.03, -0.062), (-0.15, 0.03, -0.07), (-0.22, 0.03, -0.072), (-0.27, 0.026, -0.064),
              (-0.31, 0.012, -0.046), (-0.345, -0.006, -0.026), (-0.372, -0.016, -0.016)]
    for (z0, b0, e0), (z1, b1, e1) in zip(perfil, perfil[1:]):
        p.cuad((x + t, b0, z0), (x + t, b1, z1), (x + te, e1, z1), (x + te, e0, z0), H)
        p.cuad((x - te, e0, z0), (x - te, e1, z1), (x - t, b1, z1), (x - t, b0, z0), H)
        p.cuad((x - t, b0, z0), (x - t, b1, z1), (x + t, b1, z1), (x + t, b0, z0), SI)
        p.cuad((x - te, e0, z0), (x + te, e0, z0), (x + te, e1, z1), (x - te, e1, z1), F)
    # bisel del filo: una franja más clara a lo largo de la hoja (las dos caras)
    for s_ in (1, -1):
        xb = x + s_ * (te + 0.0012)
        p.cuad((xb, -0.046, -0.1), (xb, -0.052, -0.27), (xb, -0.062, -0.27), (xb, -0.058, -0.1), F)
    # acanaladura
    for s_ in (1, -1):
        xg = x + s_ * (t + 0.0004)
        p.cuad((xg, 0.006, -0.11), (xg, 0.006, -0.24), (xg, -0.004, -0.24), (xg, -0.004, -0.11), OS)
    # sierra en el lomo: dientes triangulares
    for i in range(7):
        z = -0.1 - i * 0.022
        p.tri((x, 0.03, z), (x, 0.044, z - 0.006), (x, 0.03, z - 0.016), SI, doble=True)


def linterna(p):
    # cuerpo estriado, cabezal ensanchado, lente luminosa, interruptor
    p.torno([(0, 0.08), (0.032, 0.08), (0.034, 0.04)], 10, OS, centro=(0, 0, -0.02))
    p.torno([(0.034, 0.04), (0.034, -0.12)], 10, MT, centro=(0, 0, -0.02))
    for i in range(5):
        p.torno([(0.0355, -0.1 + i * 0.022), (0.0355, -0.092 + i * 0.022)], 10, OS, centro=(0, 0, -0.02))
    p.torno([(0.034, -0.12), (0.05, -0.18), (0.052, -0.22), (0.047, -0.225)], 10, [MT, MT, OS], centro=(0, 0, -0.02))
    p.torno([(0.046, -0.226), (0.03, -0.228), (0, -0.229)], 10, L, centro=(0, 0, -0.02))
    p.cajaJ((0, 0.036, -0.06), (0.014, 0.008, 0.022), N)                                 # interruptor
    p.elip((0, 0.0, -0.245), (0.03, 0.03, 0.01), 8, 2, L)


def boya(p):
    p.torno([(0, 0.07), (0.04, 0.07), (0.045, 0.05)], 10, OS)
    for i, (z0, z1, m) in enumerate(((0.05, 0.0, N), (0.0, -0.03, B), (-0.03, -0.12, N), (-0.12, -0.15, B), (-0.15, -0.17, OS))):
        p.torno([(0.045, z0), (0.045, z1)], 10, m)
    p.torno([(0.045, -0.17), (0.035, -0.175), (0, -0.176)], 10, OS)
    p.cajaJ((0, 0.05, -0.2), (0.012, 0.012, 0.03), R)                                    # tirador rojo
    p.torno([(0.012, -0.22), (0.012, -0.25)], 8, R, centro=(0, 0.05, 0))
    p.cajaJ((0, 0.047, -0.08), (0.01, 0.003, 0.04), CA)                                  # piloto


def arpon(cargado):
    """fusil de buceo: empuñadura de pistola con gatillo, cañón con cabezal, dos gomas que
    tensan la saeta (flojas cuando está descargado), saeta con punta de arpón, sedal y carrete"""
    def f(p):
        x = 0.02
        yb = 0.062                      # eje del cañón, sobre la mano
        # empuñadura: el puño la rodea (eje Z) y sube al cañón por delante y por detrás
        p.torno([(0.02, 0.05), (0.022, 0.02), (0.021, -0.05), (0.018, -0.07)], 8, [GO, GO, GO], centro=(x, 0.005, 0), aplastar=(0.85, 1.1))
        p.cuad((x - 0.014, 0.02, 0.045), (x + 0.014, 0.02, 0.045), (x + 0.014, yb - 0.01, 0.07), (x - 0.014, yb - 0.01, 0.07), OS)
        p.cuad((x - 0.014, 0.02, -0.065), (x - 0.014, yb - 0.01, -0.08), (x + 0.014, yb - 0.01, -0.08), (x + 0.014, 0.02, -0.065), OS)
        # gatillo y guardamonte delante del puño
        arco(p, x, yb - 0.035, -0.085, 0.028, 0.004, 200, 340, 5, OS)
        p.cajaJ((x, yb - 0.032, -0.09), (0.006, 0.026, 0.008), MT)
        # cañón de aluminio negro con franja naranja, de la culata a la boca
        p.torno([(0.0, 0.11), (0.017, 0.1), (0.017, -0.06)], 10, [OS, OS], centro=(x, yb, 0))
        p.torno([(0.017, -0.06), (0.017, -0.36)], 10, MT, centro=(x, yb, 0))
        p.torno([(0.0175, -0.09), (0.0175, -0.13)], 10, N, centro=(x, yb, 0))
        p.cajaJ((x, yb + 0.016, -0.2), (0.008, 0.006, 0.3), OS)                           # guía de la saeta
        # cabezal de la boca con los dos alojamientos de las gomas
        p.torno([(0.019, -0.36), (0.026, -0.37), (0.026, -0.41), (0.02, -0.425)], 10, [OS, OS, OS], centro=(x, yb, 0))
        for s_ in (1, -1):
            p.torno([(0.009, -0.37), (0.009, -0.41)], 6, MT, centro=(x + s_ * 0.026, yb + 0.006, 0))
        # gomas: tensas hasta el arco (detrás, sobre el cañón) o colgando flojas
        for s_ in (1, -1):
            if cargado:
                pts = [(x + s_ * 0.028, yb + 0.008, -0.39), (x + s_ * 0.02, yb + 0.018, -0.28), (x + s_ * 0.008, yb + 0.03, -0.17)]
            else:
                pts = [(x + s_ * 0.028, yb + 0.008, -0.39), (x + s_ * 0.034, yb - 0.03, -0.37), (x + s_ * 0.03, yb - 0.06, -0.34)]
            for a0, a1 in zip(pts, pts[1:]):
                n = 5
                A = [(a0[0] + math.cos(q * math.tau / n) * 0.0055, a0[1] + math.sin(q * math.tau / n) * 0.0055, a0[2]) for q in range(n)]
                Bq = [(a1[0] + math.cos(q * math.tau / n) * 0.0055, a1[1] + math.sin(q * math.tau / n) * 0.0055, a1[2]) for q in range(n)]
                for q in range(n):
                    w = (q + 1) % n
                    p.cuad(A[q], A[w], Bq[w], Bq[q], GO)
        if cargado:
            # el arco de las gomas, enganchado en la saeta
            arco(p, x, yb + 0.03, -0.17, 0.008, 0.003, 0, 180, 4, MT)
            # saeta de acero sobre la guía, con punta de arpón y aleta
            p.torno([(0.0045, -0.05), (0.0045, -0.66)], 6, B, centro=(x, yb + 0.026, 0))
            p.torno([(0.008, -0.66), (0.003, -0.7), (0, -0.73)], 6, F, centro=(x, yb + 0.026, 0))
            p.tri((x, yb + 0.03, -0.65), (x, yb + 0.05, -0.61), (x, yb + 0.03, -0.62), F, doble=True)
            # sedal: del extremo de la saeta al carrete bajo el cañón
            p.cuad((x - 0.002, yb + 0.02, -0.06), (x + 0.002, yb + 0.02, -0.06), (x + 0.002, yb - 0.012, -0.02), (x - 0.002, yb - 0.012, -0.02), B)
        # carrete bajo el cañón, delante del gatillo
        p.torno([(0.022, -0.012), (0.022, 0.012)], 10, OS, centro=(x, yb - 0.035, -0.16), eje='y')
        p.torno([(0.024, 0.012), (0.024, 0.016)], 10, N, centro=(x, yb - 0.035, -0.16), eje='y')
    return f


def bengala(p):
    p.torno([(0, 0.06), (0.022, 0.06), (0.022, -0.02)], 8, OS)
    p.torno([(0.022, -0.02), (0.022, -0.16)], 8, R)
    for z in (-0.05, -0.1):
        p.torno([(0.0225, z), (0.0225, z - 0.01)], 8, B)
    p.torno([(0.022, -0.16), (0.018, -0.18), (0, -0.185)], 8, B)
    p.elip((0, 0, -0.19), (0.01, 0.01, 0.01), 4, 2, CA)


def soldador(p):
    y = -0.005
    p.cajaJ((0, y + 0.035, -0.06), (0.045, 0.04, 0.11), MT)                              # cuerpo
    p.elip((0, y + 0.035, 0.0), (0.024, 0.022, 0.03), 6, 3, MT)
    p.cajaJ((0, y + 0.035, 0.06), (0.036, 0.036, 0.03), A)                               # batería
    for i in range(5):
        p.torno([(0.016, -0.16 - i * 0.022), (0.016, -0.168 - i * 0.022)], 8, CO, centro=(0, y + 0.035, 0))
    p.torno([(0.012, -0.15), (0.01, -0.26), (0.006, -0.3)], 8, OS, centro=(0, y + 0.035, 0))
    p.elip((0, y + 0.035, -0.305), (0.007, 0.007, 0.007), 4, 2, CA)
    p.cajaJ((0, y + 0.057, -0.04), (0.02, 0.004, 0.03), CI)                              # piloto de carga


def impulsor(p):
    c = (0, 0.02, -0.05)
    p.torno([(0, 0.08), (0.04, 0.07), (0.05, -0.05), (0.05, -0.14), (0.03, -0.18)], 10, [OS, N, N, OS], centro=c)
    p.torno([(0.1, -0.12), (0.1, -0.24)], 12, MT, centro=c)                               # tobera
    p.torno([(0.085, -0.24), (0.085, -0.12)], 12, OS, centro=c)
    for i in range(3):                                                                    # soportes
        a = i * math.tau / 3
        p.cajaJ((math.cos(a) * 0.07, 0.02 + math.sin(a) * 0.07, -0.2), (0.008, 0.008, 0.05), MT)
    p.cajaJ((0, 0.075, -0.02), (0.012, 0.004, 0.03), CI)


def helice(p):
    # palas torcidas alrededor del eje Z (el juego la gira)
    for i in range(4):
        a = i * math.tau / 4
        c, s = math.cos(a), math.sin(a)
        p.cuad((c * 0.012 - s * 0.01, s * 0.012 + c * 0.01, 0.006), (c * 0.075 - s * 0.018, s * 0.075 + c * 0.018, -0.006),
               (c * 0.075 + s * 0.012, s * 0.075 - c * 0.012, 0.006), (c * 0.012 + s * 0.008, s * 0.012 - c * 0.008, -0.004), OS)
    p.torno([(0.014, 0.01), (0.014, -0.01)], 8, MT)


def escaner(p):
    y = -0.005
    p.cajaJ((0, y + 0.04, -0.1), (0.06, 0.025, 0.12), MT)
    p.elip((0, y + 0.04, -0.03), (0.03, 0.013, 0.03), 6, 2, MT)
    p.torno([(0.022, -0.22), (0.025, -0.24)], 10, OS, centro=(0, y + 0.04, 0))
    p.elip((0, y + 0.04, -0.245), (0.019, 0.019, 0.004), 8, 2, CI)
    p.cajaJ((0, y + 0.04, 0.02), (0.062, 0.027, 0.01), A)
    p.cajaJ((0, y + 0.054, -0.08), (0.045, 0.002, 0.07), CI)                              # pantalla
    for i in range(3):
        p.cajaJ((-0.016 + i * 0.016, y + 0.054, -0.135), (0.008, 0.003, 0.008), OS)       # botones


def vara(p):
    x = 0.02
    p.torno([(0.026, 0.06), (0.026, -0.01)], 8, MT, centro=(x, 0, 0))
    p.torno([(0.018, -0.01), (0.018, -0.45)], 8, OS, centro=(x, 0, 0))
    for z in (-0.3, -0.38):
        p.torno([(0.024, z), (0.024, z - 0.008)], 8, CO, centro=(x, 0, 0))
    p.torno([(0.016, -0.45), (0.012, -0.48), (0, -0.5)], 8, CH, centro=(x, 0, 0))
    p.cajaJ((x, 0.02, -0.12), (0.004, 0.004, 0.08), CH)


pieza("cuchillo", 1, 0.02, 0.02, False, cuchillo_desplazado)
pieza("linterna", 2, 0.0, 0.042, True, linterna)
pieza("boya", 3, 0.0, 0.042, True, boya)
pieza("izq_vacia", 4, 0.0, 0.042, True, None)
pieza("der_vacia", 5, 0.02, 0.02, False, None)
pieza("arpon", 6, 0.02, 0.02, False, arpon(True))
pieza("arpon_vacio", 7, 0.02, 0.02, False, arpon(False))
pieza("bengala", 8, 0.0, 0.042, True, bengala)
pieza("soldador", 9, 0.0, 0.042, True, soldador)
pieza("impulsor", 10, 0.0, 0.042, True, impulsor)
h = PiezaJ("helice#11", MATS)
helice(h)
h.crear()
pieza("escaner", 12, 0.0, 0.042, True, escaner)
pieza("vara", 13, 0.02, 0.02, False, vara)
s = PiezaJ("saeta#14", MATS)
s.torno([(0.006, 0.45), (0.006, -0.45)], 5, B)
for (dx, dy) in ((0.03, 0.004), (0.004, 0.03)):
    s.cajaJ((0, 0, -0.42), (dx, dy, 0.04), N)
s.torno([(0.012, 0.44), (0, 0.49)], 6, F)
s.crear()
print("TRIS", triangulos())
