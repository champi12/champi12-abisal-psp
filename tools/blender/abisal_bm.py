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
        self.mats = mats if isinstance(mats, list) else list(mats)  # lista viva: materiales creados despues tambien cuentan
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


B = 99.0


def partir(pieza, cajas, nombre="parte"):
    """Corta una Pieza en partes animables igual que MB.buildPartes del juego: cada cara va a
    la primera caja {xmin, xmax, ymin, ymax, zmin, zmax} (en coordenadas DEL JUEGO: x, y
    arriba, z hacia delante) que contiene su centro; la última parte es el resto (el cuerpo).
    Crea los objetos "parteK#K+1" con todos los materiales de la pieza."""
    bm = pieza.bm
    bm.faces.ensure_lookup_table()
    bm.faces.index_update()
    grupos = [[] for _ in range(len(cajas) + 1)]
    for f in bm.faces:
        c = f.calc_center_median()
        gx, gy, gz = c.x, c.z, -c.y            # Blender (Z arriba, -Y delante) -> juego
        k = len(cajas)
        for i, b in enumerate(cajas):
            if b[0] <= gx < b[1] and b[2] <= gy < b[3] and b[4] <= gz < b[5]:
                k = i
                break
        grupos[k].append(f.index)
    obs = []
    for k, caras in enumerate(grupos):
        if not caras:
            print("AVISO parte", k, "vacia")
        nb = bm.copy()
        nb.faces.ensure_lookup_table()
        nb.faces.index_update()
        dentro = set(caras)
        quitar = [f for f in nb.faces if f.index not in dentro]
        bmesh.ops.delete(nb, geom=quitar, context='FACES')
        p = Pieza(f"{nombre}{k}#{k + 1}", pieza.mats)
        p.bm.free()
        p.bm = nb
        obs.append(p.crear())
    bm.free()
    return obs


def J(x, y, z):
    """punto en coordenadas del juego (x, y arriba, z adelante) -> Blender (x, -z, y)"""
    return Vector((x, -z, y))


class PiezaJ(Pieza):
    """Pieza modelada en coordenadas del juego, con las mismas primitivas que MB."""

    def tri(self, a, b, c, mat=0, doble=False):
        vs = [self.bm.verts.new(J(*p)) for p in (a, b, c)]
        self.bm.faces.new(vs).material_index = mat
        if doble:
            ws = [self.bm.verts.new(J(*p)) for p in (c, b, a)]
            self.bm.faces.new(ws).material_index = mat

    def cuad(self, a, b, c, d, mat=0, doble=False):
        vs = [self.bm.verts.new(J(*p)) for p in (a, b, c, d)]
        self.bm.faces.new(vs).material_index = mat
        if doble:
            ws = [self.bm.verts.new(J(*p)) for p in (d, c, b, a)]
            self.bm.faces.new(ws).material_index = mat

    def cajaJ(self, pos, tam, mat=0, rot=(0, 0, 0)):
        """caja centrada en pos (juego) con tamaño (x, y, z) del juego"""
        return self.caja(tuple(J(*pos)), (tam[0], tam[2], tam[1]), mat, rot=rot)

    def elip(self, pos, radios, u=8, v=4, mat=0):
        return self.esfera(tuple(J(*pos)), (radios[0], radios[2], radios[1]), u, v, mat)

    def torno(self, perfil, lados, mats=0, eje='z', centro=(0, 0, 0), aplastar=(1, 1)):
        """revoluciona un perfil [(radio, posicion en el eje), ...] alrededor del eje 'z'
        (hacia delante) o 'y' (hacia arriba) del juego. mats: un material o uno por tramo.
        aplastar: escala de las dos direcciones perpendiculares (ancho, alto)."""
        cx, cy, cz = centro
        anillos = []
        for (r, a) in perfil:
            if r < 1e-6:
                p = (cx, cy, cz + a) if eje == 'z' else (cx, cy + a, cz)
                anillos.append([self.bm.verts.new(J(*p))])
                continue
            fila = []
            for i in range(lados):
                t = i * math.tau / lados
                u, w = math.cos(t) * r * aplastar[0], math.sin(t) * r * aplastar[1]
                p = (cx + u, cy + w, cz + a) if eje == 'z' else (cx + u, cy + a, cz + w)
                fila.append(self.bm.verts.new(J(*p)))
            anillos.append(fila)
        for k in range(len(anillos) - 1):
            m = mats if isinstance(mats, int) else mats[min(k, len(mats) - 1)]
            A, Bn = anillos[k], anillos[k + 1]
            for i in range(lados):
                j = (i + 1) % lados
                if len(A) == 1 and len(Bn) == 1:
                    continue
                if len(A) == 1:
                    f = (A[0], Bn[j], Bn[i]) if eje == 'z' else (A[0], Bn[i], Bn[j])
                elif len(Bn) == 1:
                    f = (A[i], A[j], Bn[0]) if eje == 'z' else (A[j], A[i], Bn[0])
                else:
                    f = (A[i], A[j], Bn[j], Bn[i]) if eje == 'z' else (A[j], A[i], Bn[i], Bn[j])
                try:
                    self.bm.faces.new(f).material_index = m
                except ValueError:
                    pass

    def tubo(self, puntos, lados, mats=0, tapas=True, ruido=0.0, semilla=0):
        """tubo que sigue una curva [(x, y, z, radio), ...] (coordenadas del juego); cada
        anillo es perpendicular al tramo. mats: un material o uno por tramo."""
        rnd = random.Random(semilla)
        anillos = []
        n = len(puntos)
        for k, (x, y, z, r) in enumerate(puntos):
            a = puntos[max(0, k - 1)]
            b = puntos[min(n - 1, k + 1)]
            d = Vector((b[0] - a[0], b[1] - a[1], b[2] - a[2]))
            if d.length < 1e-6:
                d = Vector((0, 1, 0))
            d.normalize()
            ref = Vector((0, 0, 1)) if abs(d.z) < 0.9 else Vector((1, 0, 0))
            u = d.cross(ref).normalized()
            w = d.cross(u).normalized()
            fila = []
            for i in range(lados):
                t = i * math.tau / lados
                rr = r * (1 + rnd.uniform(-ruido, ruido))
                q = Vector((x, y, z)) + (u * math.cos(t) + w * math.sin(t)) * rr
                fila.append(self.bm.verts.new(J(q.x, q.y, q.z)))
            anillos.append(fila)
        for k in range(n - 1):
            m = mats if isinstance(mats, int) else mats[min(k, len(mats) - 1)]
            for i in range(lados):
                j = (i + 1) % lados
                try:
                    self.bm.faces.new((anillos[k][i], anillos[k][j], anillos[k + 1][j], anillos[k + 1][i])).material_index = m
                except ValueError:
                    pass
        if tapas:
            for fila in (anillos[0], list(reversed(anillos[-1]))):
                try:
                    self.bm.faces.new(fila).material_index = mats if isinstance(mats, int) else mats[0]
                except ValueError:
                    pass

    def marca(self):
        """cuántos vértices hay ahora (para girar luego solo lo creado después)"""
        return len(self.bm.verts)

    def girar(self, desde, pivote=(0, 0, 0), inclina=0.0, rumbo=0.0, inclina_z=0.0):
        """gira los vértices creados desde la marca: primero se inclina sobre el eje x (o z)
        del juego, luego gira alrededor del eje vertical; todo alrededor de pivote (juego)"""
        self.bm.verts.ensure_lookup_table()
        ax, ay, az = math.radians(inclina), math.radians(rumbo), math.radians(inclina_z)
        px_, py_, pz_ = pivote
        for v in self.bm.verts[desde:]:
            x, y, z = v.co.x - px_, v.co.z - py_, -v.co.y - pz_
            y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
            x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
            x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
            v.co = J(x + px_, y + py_, z + pz_)

    def rocaJ(self, pos, radios, semilla, rugosidad=0.25, sub=1, mat=0):
        """roca en coordenadas del juego: pos (x, y arriba, z) y radios (x, y, z)"""
        return self.roca(tuple(J(*pos)), (radios[0], radios[2], radios[1]), semilla, rugosidad, sub, mat)

    def orientar(self):
        """recalcula las normales hacia fuera (las caras dobles de aletas no se tocan)"""
        bmesh.ops.recalc_face_normals(self.bm, faces=[f for f in self.bm.faces if len(f.verts) > 3 or True])


def cascara(p, interior, exterior, cerrada, mat_in, mat_out, mat_borde, borde_abajo=True):
    """Pared gruesa a partir de dos rejillas de puntos (coordenadas del juego) del mismo tamaño
    [fila][columna]: la cara interior, la exterior y bordes que las unen donde una celda
    abierta (cerrada(i, j) == False) toca una cerrada, y en los bordes de la rejilla.
    mat_* pueden ser enteros o funciones (i, j) -> material para variar el tono."""
    filas, cols = len(interior), len(interior[0])
    vi = [[p.bm.verts.new(J(*interior[i][j])) for j in range(cols)] for i in range(filas)]
    vo = [[p.bm.verts.new(J(*exterior[i][j])) for j in range(cols)] for i in range(filas)]
    m = lambda x, i, j: x(i, j) if callable(x) else x

    def cara(vs, mat):
        try:
            p.bm.faces.new(vs).material_index = mat
        except ValueError:
            pass
    for i in range(filas - 1):
        for j in range(cols - 1):
            if not cerrada(i, j):
                continue
            cara((vo[i][j], vo[i][j + 1], vo[i + 1][j + 1], vo[i + 1][j]), m(mat_out, i, j))
            cara((vi[i + 1][j], vi[i + 1][j + 1], vi[i][j + 1], vi[i][j]), m(mat_in, i, j))
            # bordes: hacia cada vecina abierta o fuera de la rejilla
            for (di, dj, a, b) in ((-1, 0, (i, j), (i, j + 1)), (1, 0, (i + 1, j + 1), (i + 1, j)),
                                   (0, -1, (i + 1, j), (i, j)), (0, 1, (i, j + 1), (i + 1, j + 1))):
                ni, nj = i + di, j + dj
                fuera = ni < 0 or nj < 0 or ni >= filas - 1 or nj >= cols - 1
                if fuera and not borde_abajo and di == -1:
                    continue
                if fuera or not cerrada(ni, nj):
                    (ai, aj), (bi, bj) = a, b
                    cara((vi[ai][aj], vi[bi][bj], vo[bi][bj], vo[ai][aj]), m(mat_borde, i, j))
