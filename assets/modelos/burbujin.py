"""Burbujín para ABISAL, modelado por script en Blender (blender -b -P burbujin.py).

Pez rechoncho (~0,55 m) de simetría triple: tres aletas de cola a 120 grados, tres
aletas laterales, cuatro ojos en fila, un tubo para comer y un saco de gas luminoso en
el lomo. Turquesa con vientre violeta y aletas naranjas. Mira hacia -Y de Blender
(el exportador lo pasa a +Z en M3G). Tres piezas: "cuerpo#1", "aletas#2" y "cola#3" (el numero es su ID en el .m3g).
"""
import bpy, bmesh, math
from mathutils import Vector, Matrix

bpy.ops.wm.read_factory_settings(use_empty=True)
esc = bpy.context.scene

def material(nombre, rgb, emision=0.0):
    m = bpy.data.materials.new(nombre)
    m.use_nodes = True
    bsdf = m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*rgb, 1)
    bsdf.inputs["Roughness"].default_value = 0.6
    if emision > 0:
        bsdf.inputs["Emission Color"].default_value = (*rgb, 1)
        bsdf.inputs["Emission Strength"].default_value = emision
    m.diffuse_color = (*rgb, 1)
    return m

def srgb(h):
    c = [((h >> s) & 255) / 255.0 for s in (16, 8, 0)]
    return tuple((x / 12.92) if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c)

M_CUERPO = material("cuerpo", srgb(0x2fb8b0))
M_VIENTRE = material("vientre", srgb(0x8a5ad8))
M_ALETA = material("aleta", srgb(0xf2702e))
M_OJO = material("ojo", srgb(0x1c3050))
M_AGALLA = material("agalla", srgb(0x2a9e98))
M_BRILLO = material("brillo", srgb(0xe8f8ff), emision=1.0)
M_SACO = material("saco", srgb(0x5ff0d8), emision=6.0)
M_TUBO = material("tubo", srgb(0x2a7c78))

def objeto(nombre, bm, mats):
    me = bpy.data.meshes.new(nombre)
    bm.to_mesh(me)
    bm.free()
    for m in mats:
        me.materials.append(m)
    ob = bpy.data.objects.new(nombre, me)
    esc.collection.objects.link(ob)
    return ob

def poner_mat(bm, caras, i):
    for f in caras:
        f.material_index = i

# ---- cuerpo: una esfera deformada en forma de gota rechoncha
bm = bmesh.new()
bmesh.ops.create_uvsphere(bm, u_segments=12, v_segments=8, radius=1.0)
for v in bm.verts:
    x, y, z = v.co
    # largo en Y (-Y = cabeza), mas ancho delante, afinado hacia la cola
    t = (y + 1) / 2                    # 0 cabeza .. 1 cola
    ancho = 0.21 * (1.05 - 0.45 * t * t)
    v.co = Vector((x * ancho, y * 0.28, z * (0.2 * (1.0 - 0.35 * t * t))))
    if z < 0:
        v.co.z *= 0.85                 # vientre algo plano
caras_vientre = [f for f in bm.faces if f.calc_center_median().z < -0.06]
poner_mat(bm, bm.faces, 0)
poner_mat(bm, caras_vientre, 1)
# ojos: cuatro en fila sobre la frente
for i in range(4):
    x = -0.105 + i * 0.07
    r = bmesh.ops.create_icosphere(bm, subdivisions=0, radius=0.03)
    for v in r["verts"]:
        v.co += Vector((x, -0.235, 0.06))
    for f in {f for v in r["verts"] for f in v.link_faces}:
        f.material_index = 2
    for v in r["verts"]:
        v.co.y = -0.235 + (v.co.y + 0.235) * 0.6
    g = [Vector((x - 0.004, -0.255, 0.074)), Vector((x + 0.01, -0.255, 0.074)),
         Vector((x + 0.01, -0.255, 0.086)), Vector((x - 0.004, -0.255, 0.086))]
    bm.faces.new([bm.verts.new(q) for q in g]).material_index = 3
# saco de gas luminoso en el lomo
s = bmesh.ops.create_uvsphere(bm, u_segments=6, v_segments=4, radius=0.07)
for v in s["verts"]:
    v.co = Vector((v.co.x, v.co.y * 1.3, v.co.z * 0.8)) + Vector((0, 0.0, 0.17))
for f in {f for v in s["verts"] for f in v.link_faces}:
    f.material_index = 4
# tubo para comer
t = bmesh.ops.create_cone(bm, cap_ends=True, segments=6, radius1=0.022, radius2=0.03, depth=0.07)
for v in t["verts"]:
    v.co = Matrix.Rotation(math.radians(90), 3, 'X') @ v.co + Vector((0, -0.29, -0.06))
for f in {f for v in t["verts"] for f in v.link_faces}:
    f.material_index = 5
# boca: un anillo oscuro alrededor del tubo; agallas: tres arcos oscuros a cada lado
for lado in (-1, 1):
    for k in range(3):
        y = -0.12 + k * 0.035
        p = [Vector((lado * 0.205, y, -0.05)), Vector((lado * 0.214, y + 0.012, 0.0)),
             Vector((lado * 0.205, y, 0.05))]
        for i in range(2):
            q0, q1 = p[i], p[i + 1]
            o = Vector((lado * 0.006, 0.008, 0))
            f = bm.faces.new([bm.verts.new(q0), bm.verts.new(q1), bm.verts.new(q1 + o), bm.verts.new(q0 + o)])
            f.material_index = 7
cuerpo = objeto("cuerpo#1", bm, [M_CUERPO, M_VIENTRE, M_OJO, M_BRILLO, M_SACO, M_TUBO, M_ALETA, M_AGALLA])
for f in cuerpo.data.polygons:
    f.use_smooth = f.material_index in (0, 1, 4)

def aleta(bm, puntos, rot, grosor=0.006, mat=0, rayos=None):
    """una aleta con algo de grosor (dos caras separadas) y radios mas oscuros"""
    n = (puntos[1] - puntos[0]).cross(puntos[2] - puntos[0]).normalized() * grosor
    a = [bm.verts.new(rot @ (q + n)) for q in puntos]
    b = [bm.verts.new(rot @ (q - n)) for q in reversed(puntos)]
    bm.faces.new(a).material_index = mat
    bm.faces.new(b).material_index = mat
    for (p0, p1) in (rayos or []):
        d = (p1 - p0).normalized().cross(n.normalized()) * 0.006
        for sg in (1, -1):
            q = [p0 + n * sg * 1.3 - d, p1 + n * sg * 1.3 - d, p1 + n * sg * 1.3 + d, p0 + n * sg * 1.3 + d]
            f = bm.faces.new([bm.verts.new(rot @ x) for x in (q if sg > 0 else list(reversed(q)))])
            f.material_index = mat + 1

M_RAYO = material("rayo", srgb(0xd8622a))

# ---- aletas laterales: tres a 120 grados (pieza aparte, se mueven al nadar)
bm = bmesh.new()
for k in range(3):
    rot = Matrix.Rotation(math.radians(k * 120 + 30), 3, 'Y')
    p = [Vector((0, -0.04, 0.17)), Vector((0, 0.08, 0.32)), Vector((0, 0.15, 0.27)), Vector((0, 0.13, 0.16))]
    aleta(bm, p, rot, rayos=[(p[0], p[1]), (p[0], p[2])])
objeto("aletas#2", bm, [M_ALETA, M_RAYO])

# ---- cola: tres aletas a 120 grados (pivote en el arranque de la cola, y = 0.24)
bm = bmesh.new()
for k in range(3):
    rot = Matrix.Rotation(math.radians(k * 120 + 90), 3, 'Y')
    p = [Vector((0, 0.23, 0)), Vector((0, 0.46, 0.21)), Vector((0, 0.53, 0.05)), Vector((0, 0.45, -0.03))]
    aleta(bm, p, rot, rayos=[(p[0], p[1]), (p[0], p[2])])
cola = objeto("cola#3", bm, [M_ALETA, M_RAYO])

# un mundo de fondo azul marino (la escena de vista previa)
esc.world = bpy.data.worlds.new("mar")
esc.world.color = srgb(0x0e3a52)
