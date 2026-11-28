"""Raspa (osario), estilo bloque: carroñero alargado y pálido; la mitad de atrás es solo
espina y costillas. Piezas: cola#1 (detrás de z = -0,18) y cuerpo#2."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "tools", "blender"))
from abisal_bloque import *
limpiar()
m = Modelo("raspa", ppm=48)
n = [0]
def pez(nombre, lomo, vientre, aleta, ojo, ancho, alto, largo, cola_tam=(0.016, 0.14, 0.1), extra=None,
        cola_extra=None):
    """cuerpo escalonado (cabeza, tronco, tramo trasero) y cola en V detrás de z = -0,18"""
    cola = m.pieza(nombre + "_cola", n[0] * 2 + 1)
    cuerpo = m.pieza(nombre, n[0] * 2 + 2)
    n[0] += 1
    ESC = {'todo': E("escamas", lomo, vientre), '-y': E("liso", vientre)}
    z0 = -0.16
    zf = z0 + largo
    # tramo trasero, tronco y cabeza (cada uno un poco más estrecho hacia los extremos)
    cuerpo.caja((0, 0, z0 + largo * 0.15), (ancho * 0.7, alto * 0.72, largo * 0.3), ESC)
    cuerpo.caja((0, 0, z0 + largo * 0.5), (ancho, alto, largo * 0.42), ESC)
    cuerpo.caja((0, -alto * 0.03, zf - largo * 0.12), (ancho * 0.85, alto * 0.82, largo * 0.24),
                {'todo': E("escamas", lomo, vientre), '+z': E("liso", vientre)})
    for s in (1, -1):
        cuerpo.caja((s * ancho * 0.43, alto * 0.12, zf - largo * 0.1), (0.012, alto * 0.22, alto * 0.22),
                    {'todo': E("ojo", ojo, 0x101010)})
        cuerpo.caja((s * ancho * 0.6, -alto * 0.15, z0 + largo * 0.6), (ancho * 0.4, 0.012, largo * 0.18),
                    {'todo': E("aleta", aleta, lomo)}, rot=(0, 0, s * -20))
    cuerpo.caja((0, alto * 0.62, z0 + largo * 0.45), (0.014, alto * 0.3, largo * 0.35), {'todo': E("aleta", aleta, lomo)})
    if extra:
        extra(cuerpo, ancho, alto, largo, z0, zf)
    # cola: pedúnculo y aleta en V
    cola.caja((0, 0, -0.21), (ancho * 0.3, alto * 0.35, 0.07), ESC)
    cola.caja((0, cola_tam[1] * 0.35, -0.27), cola_tam, {'todo': E("aleta", aleta, lomo)}, rot=(28, 0, 0),
              pivote=(0, 0, -0.24))
    cola.caja((0, -cola_tam[1] * 0.35, -0.27), cola_tam, {'todo': E("aleta", aleta, lomo)}, rot=(-28, 0, 0),
              pivote=(0, 0, -0.24))
    if cola_extra:
        cola_extra(cola)




def raspa(p, an, al, la, z0, zf):
    for k in range(4):
        z = z0 + 0.03 + k * 0.05
        for s in (1, -1):
            p.caja((s * an * 0.6, -0.01, z), (0.012, al * 0.9, 0.012), {'todo': E("hueso", 0xeee4cc, 0xb8a888)}, rot=(0, 0, s * 15))
pez("raspa", 0xa89c88, 0xd8ccb0, 0xeee4cc, 0xff6a30, 0.1, 0.13, 0.44, extra=raspa)
m.crear()
