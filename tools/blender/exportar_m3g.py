import bpy, sys
import os; sys.path.append(os.path.dirname(os.path.abspath(__file__)))
import m3g_exporter_v1_2 as m
m.register()
out = sys.argv[sys.argv.index('--') + 1]
tris = 0
for ob in bpy.context.scene.objects:
    if ob.type == 'MESH':
        t = sum(len(p.vertices) - 2 for p in ob.data.polygons)
        tris += t
        print("OBJ", ob.name, "tris", t)
print("TRIS", tris)
print("RESULT", bpy.ops.export_scene.m3g(filepath=out))
