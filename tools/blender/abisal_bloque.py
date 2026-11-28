"""Estilo bloque (como Blockbench): criaturas hechas de cajas con una textura pixel art.

Todas las criaturas comparten UN atlas de 512 x 512 (assets/modelos/atlas_criaturas.png):
cada modelo tiene una región fija (REGION) y cada cara de cada caja recibe su rectángulo
dentro de ella. El modelo se exporta con sus UV; el juego usa el atlas compartido (cargado
una sola vez en la VRAM). La pintura de cada cara se describe con un "estilo" y se guarda
en assets/modelos/atlas/<modelo>.json; tools/blender/pintar_atlas.py compone el atlas.

Uso en un script de modelo:
    from abisal_bloque import *
    m = Modelo("peces_zonas", ppm=64)
    p = m.pieza("teja", 1)
    p.caja((x, y, z), (ancho, alto, largo), {'todo': E("escamas", 0x8a3c2c, 0xd07848)})
    m.crear()     # crea los objetos (con material de atlas) y guarda el json
Coordenadas del juego: x a la derecha, y arriba, z adelante (cabeza hacia +z).
"""
import bpy, bmesh, json, math, os
from mathutils import Vector, Euler

T = 256          # (kept for the old name)
TW, TH = 512, 512
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "assets", "modelos")
# región del atlas de cada modelo (x, y, ancho, alto en pixels)
REGION = {
    "peces_zonas": (0, 0, 256, 128),
    "pez_espejo": (0, 128, 64, 32),
    "raspa": (64, 128, 64, 32),
    "bomba_azufre": (128, 128, 128, 64),
    "burbujin": (0, 192, 64, 64),
    "planeador": (64, 192, 64, 64),
    "tortuga": (128, 192, 64, 64),
    "caballito": (192, 192, 64, 32),
    "pez_candil": (0, 160, 128, 32),
    "remora": (0, 256, 256, 128),
    "batel": (256, 256, 256, 128),
    "serpiente_vitrea": (256, 0, 128, 128),
    "serpiente_hueso": (384, 0, 128, 128),
    "serpiente_diente": (256, 128, 128, 128),
    "serpiente_anguila": (384, 128, 128, 128),
}


def E(estilo, c1, c2=None):
    """estilo de una cara: escamas, liso, franjas, aleta, ojo, brillo, placas, hueso, puntos"""
    return (estilo, c1, c2 if c2 is not None else c1)


def limpiar():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob)


class Pieza:
    def __init__(self, modelo, nombre, num):
        self.m = modelo
        self.nombre = f"{nombre}#{num}"
        self.cajas = []

    def caja(self, centro, tam, estilos, rot=(0, 0, 0), pivote=None):
        """caja centrada en centro (juego) de tam (x, y, z); rot en grados (x, y, z del
        juego) alrededor de pivote (por defecto su centro). estilos: dict de caras
        '+x' '-x' '+y' '-y' '+z' '-z' o 'todo' -> E(...)."""
        self.cajas.append((centro, tam, estilos, rot, pivote))
        return self


class Modelo:
    def __init__(self, nombre, ppm=64):
        self.nombre = nombre
        self.ppm = ppm
        self.region = REGION[nombre]
        self.rects = []
        self.cur = [0, 0, 0]
        self.piezas = []

    def pieza(self, nombre, num):
        p = Pieza(self, nombre, num)
        self.piezas.append(p)
        return p

    def _reservar(self, w, h, est):
        rx, ry, rw, rh = self.region
        w, h = max(2, int(round(w))), max(2, int(round(h)))
        w, h = min(w, rw), min(h, rh)
        # faces with the same paint and size share their pixels (both sides of a fish, the
        # eyes, the fins...): the atlas holds many more models this way
        clave = (w, h) + tuple(est)
        if not hasattr(self, "_hechos"):
            self._hechos = {}
        if clave in self._hechos:
            return self._hechos[clave]
        if self.cur[0] + w > rw:
            self.cur[0], self.cur[1], self.cur[2] = 0, self.cur[1] + self.cur[2], 0
        if self.cur[1] + h > rh:
            # leave nothing behind for the exporter: a half-built model must never be saved
            for ob in list(bpy.data.objects):
                bpy.data.objects.remove(ob)
            raise RuntimeError(f"{self.nombre}: no cabe en su región del atlas {self.region}")
        r = {"x": rx + self.cur[0], "y": ry + self.cur[1], "w": w, "h": h,
             "estilo": est[0], "c1": est[1], "c2": est[2], "cara": est[3] if len(est) > 3 else "z"}
        self.cur[0] += w
        self.cur[2] = max(self.cur[2], h)
        self.rects.append(r)
        self._hechos[clave] = r
        return r

    def _material(self):
        mat = bpy.data.materials.get("atlas_criaturas")
        if mat is None:
            mat = bpy.data.materials.new("atlas_criaturas")
            mat.use_nodes = True
            nt = mat.node_tree
            tex = nt.nodes.new("ShaderNodeTexImage")
            png = os.path.join(RAIZ, "atlas_criaturas.png")
            # the exported file only needs the UVs: the game uses the shared atlas, so the
            # model embeds a tiny placeholder (embedding the 256x256 atlas in every file ran
            # the PSP out of memory while loading). ATLAS_VISTA=1 shows the real one (previews).
            if os.path.exists(png) and os.environ.get("ATLAS_VISTA"):
                tex.image = bpy.data.images.load(png)
            else:
                tex.image = bpy.data.images.new("atlas", 4, 4)
            tex.interpolation = 'Closest'
            b = nt.nodes["Principled BSDF"]
            nt.links.new(tex.outputs["Color"], b.inputs["Base Color"])
            b.inputs["Emission Color"].default_value = (0, 0, 0, 1)
            b.inputs["Roughness"].default_value = 0.8
            mat.diffuse_color = (1, 1, 1, 1)
        return mat

    def _material_brillo(self):
        """the glowing faces: same atlas, but emissive (the game draws them unlit)"""
        mat = bpy.data.materials.get("atlas_brillo")
        if mat is None:
            mat = self._material().copy()
            mat.name = "atlas_brillo"
            b = mat.node_tree.nodes["Principled BSDF"]
            b.inputs["Emission Color"].default_value = (1, 1, 1, 1)
            b.inputs["Emission Strength"].default_value = 2.0
        return mat

    def crear(self):
        caras_def = {
            '+x': [(1, -1, -1), (1, 1, -1), (1, 1, 1), (1, -1, 1)],
            '-x': [(-1, -1, 1), (-1, 1, 1), (-1, 1, -1), (-1, -1, -1)],
            '+y': [(-1, 1, -1), (-1, 1, 1), (1, 1, 1), (1, 1, -1)],
            '-y': [(-1, -1, 1), (-1, -1, -1), (1, -1, -1), (1, -1, 1)],
            '+z': [(1, -1, 1), (1, 1, 1), (-1, 1, 1), (-1, -1, 1)],
            '-z': [(-1, -1, -1), (-1, 1, -1), (1, 1, -1), (1, -1, -1)],
        }
        obs = []
        for p in self.piezas:
            bm = bmesh.new()
            uv = bm.loops.layers.uv.new("UV")
            for (centro, tam, estilos, rot, pivote) in p.cajas:
                cx, cy, cz = centro
                sx, sy, sz = tam[0] / 2, tam[1] / 2, tam[2] / 2
                R = Euler([math.radians(a) for a in rot], 'XYZ').to_matrix()
                pv = Vector(pivote) if pivote else Vector(centro)
                dims = {'+x': (tam[2], tam[1]), '-x': (tam[2], tam[1]), '+y': (tam[0], tam[2]),
                        '-y': (tam[0], tam[2]), '+z': (tam[0], tam[1]), '-z': (tam[0], tam[1])}
                for k, esq in caras_def.items():
                    est = estilos.get(k, estilos.get('todo'))
                    if est is None:
                        continue  # a face left out on purpose (hidden against another box)
                    w, h = dims[k]
                    r = self._reservar(w * self.ppm, h * self.ppm, tuple(est) + (k[1],))
                    vs = []
                    for (a, b, c) in esq:
                        q = Vector((cx + a * sx, cy + b * sy, cz + c * sz))
                        q = pv + R @ (q - pv)
                        vs.append(bm.verts.new((q.x, -q.z, q.y)))      # juego -> Blender
                    f = bm.faces.new(vs)
                    f.material_index = 1 if est[0] == "brillo" else 0
                    u0, v0 = r["x"] / TW, 1 - (r["y"] + r["h"]) / TH
                    u1, v1 = (r["x"] + r["w"]) / TW, 1 - r["y"] / TH
                    # a half-texel inset so nearest filtering never bleeds the neighbour
                    du, dv = 0.5 / TW, 0.5 / TH
                    for lp, (uu, vv) in zip(f.loops, ((u0 + du, v0 + dv), (u0 + du, v1 - dv),
                                                      (u1 - du, v1 - dv), (u1 - du, v0 + dv))):
                        lp[uv].uv = (uu, vv)
            me = bpy.data.meshes.new(p.nombre)
            bm.to_mesh(me)
            bm.free()
            me.materials.append(self._material())
            me.materials.append(self._material_brillo())
            ob = bpy.data.objects.new(p.nombre, me)
            bpy.context.scene.collection.objects.link(ob)
            obs.append(ob)
        os.makedirs(os.path.join(RAIZ, "atlas"), exist_ok=True)
        json.dump({"modelo": self.nombre, "rects": self.rects},
                  open(os.path.join(RAIZ, "atlas", self.nombre + ".json"), "w"))
        tris = sum(len(o.data.polygons) * 2 for o in obs)
        print("TRIS", tris, self.nombre, [(o.name, len(o.data.polygons) * 2) for o in obs])
        return obs


def serpiente(m, L, r, piel, vientre, cabeza=None, segmento=None, aleta=None):
    """Cuatro segmentos (pieza#1 = cabeza ... #4 = cola), cada uno en su sitio local de z = 0
    a z = -L/4 (el juego los encadena y los ondula). piel/vientre: E(...) de los costados y
    de abajo; cabeza(p, paso, r) añade la cabeza, segmento(p, k, paso, rk) los detalles de
    cada tramo, aleta: E(...) de las aletas dorsales."""
    paso = L / 4
    for k in range(4):
        p = m.pieza(f"segmento{k}", k + 1)
        rk = r * (1 - k * 0.2)
        estilos = {'todo': piel, '-y': vientre}
        p.caja((0, 0.1, -paso / 2), (rk * 1.8, rk * 2, paso * 1.08), estilos)
        if aleta is not None:
            p.caja((0, 0.1 + rk + rk * 0.35, -paso / 2), (0.02, rk * 0.7, paso * 0.7), {'todo': aleta})
        if k == 3 and aleta is not None:
            p.caja((0, 0.1, -paso - rk * 1.2), (0.02, rk * 3.2, rk * 2.2), {'todo': aleta})
        if segmento:
            segmento(p, k, paso, rk)
        if k == 0 and cabeza:
            cabeza(p, paso, r)
