import bpy, sys
import os; sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import m3g_exporter_v1_2 as m
m.register()
out = sys.argv[sys.argv.index('--') + 1]
# the add-on packs the vertices using each object's bounding box: objects made by script
# have a stale (empty) box until the scene is evaluated, and their vertices came out
# scaled and clamped wrong (models invisible or misplaced in the game)
bpy.context.view_layer.update()
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        ob.data.update()
bpy.context.view_layer.update()
tris = 0
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        t = sum(len(p.vertices) - 2 for p in ob.data.polygons)
        tris += t
        print("OBJ", ob.name, "tris", t)
print("TRIS", tris)
if tris == 0:
    # the model script failed: never overwrite a good .m3g with an empty one
    print("RESULT nada que exportar: el script del modelo fallo")
    sys.exit(1)
print("RESULT", bpy.ops.export_scene.m3g(filepath=out))
