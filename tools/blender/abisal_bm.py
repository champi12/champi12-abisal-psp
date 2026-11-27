"""Ayudas comunes para los modelos de ABISAL hechos por script en Blender.

Uso, al principio de un assets/modelos/<nombre>.py:

    import sys, os
    sys.path.insert(0, os.path.join(os.path.dirname(bpy.data.filepath or __file__), "..", "..", "tools", "blender"))
    from abisal_bm import *

Convenciones: metros, Z arriba (el exportador lo pasa a Y arriba en M3G), el suelo en z = 0.
Cada pieza animable es un objeto "nombre#N" (N = su ID en el .m3g). Los colores salen de
los materiales (una submalla por material), así que conviene usar pocos.
"""
import bpy, bmesh, math, random
from mathutils import Vector, Matrix

esc = bpy.context.scene


def limpiar():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    global esc
    esc = bpy.context.scene


def srgb(h):
    c = [((h >> s) & 255) / 255.0 for s in (16, 8, 0)]
    return tuple((x / 12.92) if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c)


def material(nombre, color_hex, emision=0.0, rugosidad=0.6, metal=0.0):
    rgb = srgb(color_hex)
    m = bpy.data.materials.new(nombre)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*rgb, 1)
    bsdf.inputs["Roughness"].default_value = rugosidad
    bsdf.inputs["Metallic"].default_value = metal
    if emision > 0:
        bsdf.inputs["Emission Color"].default_value = (*rgb, 1)
        bsdf.inputs["Emission Strength"].default_value = emision
    else:
        bsdf.inputs["Emission Color"].default_value = (0, 0, 0, 1)
    m.diffuse_color = (*rgb, 1)
    return m


class Pieza:
    """Una malla en construcción con su lista de materiales."""

    def __init__(self, nombre, mats):
        self.nombre = nombre
        self.mats = list(mats)
        self.bm = bmesh.new()

    def _mat(self, caras, i):
        for f in caras:
            f.material_index = i

    def _transformar(self, verts, pos=(0, 0, 0), rot=(0, 0, 0), esc=(1, 1, 1)):
        m = (Matrix.Translation(Vector(pos)) @ Matrix.Rotation(math.radians(rot[2]), 4, 'Z')
             @ Matrix.Rotation(math.radians(rot[1]), 4, 'Y') @ Matrix.Rotation(math.radians(rot[0]), 4, 'X')
             @ Matrix.Diagonal(Vector((*esc, 1))))
        bmesh.ops.transform(self.bm, matrix=m, verts=verts)

    def _caras(self, verts):
        return list({f for v in verts for f in v.link_faces})

    def caja(self, pos, tam, mat=0, rot=(0, 0, 0)):
        r = bmesh.ops.create_cube(self.bm, size=1.0)
        self._transformar(r["verts"], pos, rot, tam)
        self._mat(self._caras(r["verts"]), mat)
        return r["verts"]

    def cilindro(self, pos, radio, alto, lados=8, mat=0, rot=(0, 0, 0), radio2=None, tapas=True):
        r = bmesh.ops.create_cone(self.bm, cap_ends=tapas, cap_tris=False, segments=lados,
                                  radius1=radio, radius2=radio if radio2 is None else radio2, depth=alto)
        self._transformar(r["verts"], pos, rot)
        self._mat(self._caras(r["verts"]), mat)
        return r["verts"]

    def esfera(self, pos, radios, u=8, v=5, mat=0, rot=(0, 0, 0)):
        r = bmesh.ops.create_uvsphere(self.bm, u_segments=u, v_segments=v, radius=1.0)
        self._transformar(r["verts"], pos, rot, radios)
        self._mat(self._caras(r["verts"]), mat)
        return r["verts"]

    def roca(self, pos, radios, semilla, rugosidad=0.25, sub=1, mat=0, rot=(0, 0, 0)):
        """icosfera deformada: rocas y pedestales de piedra"""
        r = bmesh.ops.create_icosphere(self.bm, subdivisions=sub, radius=1.0)
        rnd = random.Random(semilla)
        for v in r["verts"]:
            v.co *= 1 + rnd.uniform(-rugosidad, rugosidad)
        self._transformar(r["verts"], pos, rot, radios)
        self._mat(self._caras(r["verts"]), mat)
        return r["verts"]

    def prisma(self, pos, radio, alto, lados=6, punta=0.35, mat=0, rot=(0, 0, 0)):
        """cristal: prisma con punta (la base en pos, crece hacia +Z local)"""
        cuerpo = alto * (1 - punta)
        r = bmesh.ops.create_cone(self.bm, cap_ends=True, cap_tris=False, segments=lados,
                                  radius1=radio, radius2=radio * 0.85, depth=cuerpo)
        bmesh.ops.translate(self.bm, verts=r["verts"], vec=Vector((0, 0, cuerpo / 2)))
        vs = list(r["verts"])
        p = bmesh.ops.create_cone(self.bm, cap_ends=True, cap_tris=True, segments=lados,
                                  radius1=radio * 0.85, radius2=0.0, depth=alto * punta)
        bmesh.ops.translate(self.bm, verts=p["verts"], vec=Vector((0, 0, cuerpo + alto * punta / 2)))
        vs += p["verts"]
        self._transformar(vs, pos, rot)
        self._mat(self._caras(vs), mat)
        return vs

    def quad(self, pts, mat=0, doble=True):
        vs = [self.bm.verts.new(Vector(p)) for p in pts]
        f = self.bm.faces.new(vs)
        f.material_index = mat
        if doble:
            vs2 = [self.bm.verts.new(Vector(p)) for p in reversed(pts)]
            self.bm.faces.new(vs2).material_index = mat
        return vs

    def ruido(self, cantidad, semilla, filtro=None):
        """desplaza un poco los vértices: quita el aspecto de figura geométrica"""
        rnd = random.Random(semilla)
        for v in self.bm.verts:
            if filtro is None or filtro(v.co):
                v.co += Vector((rnd.uniform(-1, 1), rnd.uniform(-1, 1), rnd.uniform(-1, 1))) * cantidad

    def crear(self):
        bmesh.ops.remove_doubles(self.bm, verts=self.bm.verts, dist=1e-5)
        me = bpy.data.meshes.new(self.nombre)
        self.bm.to_mesh(me)
        self.bm.free()
        for m in self.mats:
            me.materials.append(m)
        for p in me.polygons:
            p.use_smooth = False
        ob = bpy.data.objects.new(self.nombre, me)
        esc.collection.objects.link(ob)
        return ob


def triangulos():
    n = 0
    for ob in esc.objects:
        if ob.type == 'MESH':
            n += sum(len(p.vertices) - 2 for p in ob.data.polygons)
    return n
