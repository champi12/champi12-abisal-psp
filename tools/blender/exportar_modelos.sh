#!/bin/sh
# Exporta todos los modelos de assets/modelos/*.py a .m3g con Blender (3.6+ / 4.x).
#   tools/blender/exportar_modelos.sh            -> assets/modelos/<nombre>.m3g
#   VISTA=1 tools/blender/exportar_modelos.sh    -> ademas, vistas previas PNG en build/vistas
set -e
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
for f in "$ROOT"/assets/modelos/*.py; do
    n=$(basename "$f" .py)
    blender -b -P "$f" -P "$ROOT/tools/blender/exportar_m3g.py" -- "$ROOT/assets/modelos/$n.m3g" | grep -E "^OBJ|TRIS|RESULT"
    if [ -n "$VISTA" ]; then
        mkdir -p "$ROOT/build/vistas"
        blender -b -P "$f" -P "$ROOT/tools/blender/vista_previa.py" -- "$ROOT/build/vistas/$n" | grep VISTAS
    fi
done
