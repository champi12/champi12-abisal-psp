"""Vista previa: renderiza el modelo actual desde tres angulos (Cycles CPU)."""
import bpy, sys, math
from mathutils import Vector
args = sys.argv[sys.argv.index('--') + 1:]
salida = args[0]
esc = bpy.context.scene
esc.render.engine = 'CYCLES'
esc.cycles.device = 'CPU'
esc.cycles.samples = 32
esc.cycles.use_denoising = False
esc.render.resolution_x, esc.render.resolution_y = 480, 360
esc.world.use_nodes = True
bg = esc.world.node_tree.nodes["Background"]
bg.inputs[0].default_value = (0.03, 0.18, 0.28, 1)
bg.inputs[1].default_value = 1.2
sol = bpy.data.lights.new("sol", 'SUN'); sol.energy = 3.5; sol.use_shadow = False
so = bpy.data.objects.new("sol", sol); esc.collection.objects.link(so)
so.rotation_euler = (math.radians(40), math.radians(10), math.radians(30))
rel = bpy.data.lights.new("relleno", 'SUN'); rel.energy = 1.6; rel.use_shadow = False
ro = bpy.data.objects.new("relleno", rel); esc.collection.objects.link(ro)
ro.rotation_euler = (math.radians(-120), math.radians(-20), math.radians(200))
cam = bpy.data.cameras.new("cam"); cam.lens = 50
co = bpy.data.objects.new("cam", cam); esc.collection.objects.link(co); esc.camera = co
vistas = [(Vector((0.9, -1.0, 0.45)), "34"), (Vector((1.3, 0.0, 0.05)), "lado"), (Vector((0.0, -1.3, 0.2)), "frente")]
for pos, nom in vistas:
    co.location = pos
    d = Vector((0, 0.05, 0)) - pos
    co.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()
    esc.render.filepath = f"{salida}_{nom}.png"
    bpy.ops.render.render(write_still=True)
print("VISTAS OK")
