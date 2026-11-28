#!/bin/sh
# Build ABISAL from source.
#   ./build.sh [psp|pc]
# Output: build/psp/PSP/GAME/ABISAL/ (copy to ms0:/PSP/GAME/) or build/pc/abisal
set -e
TARGET="${1:-psp}"
ROOT=$(cd "$(dirname "$0")" && pwd)
B="$ROOT/build"
rm -rf "$B/libclasses" "$B/gameclasses" "$B/jar" "$B/gen"
mkdir -p "$B/libclasses" "$B/gameclasses" "$B/emptyboot" "$B/jar"

echo "== compiling the J2ME runtime library"
find "$ROOT/lib/java" -name '*.java' > "$B/lib_sources.txt"
javac -nowarn -source 8 -target 8 -bootclasspath "$B/emptyboot" -extdirs "" -encoding UTF-8 \
    -d "$B/libclasses" @"$B/lib_sources.txt" 2>&1 | grep -v -e '^Picked up' -e 'warning' || true

echo "== compiling the game"
find "$ROOT/game/src" -name '*.java' > "$B/game_sources.txt"
javac -nowarn -source 8 -target 8 -bootclasspath "$B/libclasses" -extdirs "" -encoding UTF-8 \
    -d "$B/gameclasses" @"$B/game_sources.txt" 2>&1 | grep -v -e '^Picked up' -e 'warning' || true
[ -f "$B/gameclasses/abisal/AbisalMIDlet.class" ] || { echo "game compilation failed" >&2; exit 1; }

echo "== generating sounds"
mkdir -p "$B/res/snd" "$B/rawsnd"
rm -f "$B/rawsnd"/*.wav
python3 "$ROOT/tools/assets/gen_sounds.py" "$B/rawsnd"
# every WAV the game ships is normalised here: downmixed/resampled to 22 kHz mono, loops
# closed (splice + tiny fade) and packed as IMA ADPCM when 4 bits are inaudible there.
# The runtime decodes ADPCM back to PCM at load, so playback is unchanged.
python3 "$ROOT/tools/assets/convert_audio.py" "$ROOT/assets/snd" "$B/res/snd"
python3 "$ROOT/tools/assets/convert_audio.py" "$B/rawsnd" "$B/res/snd"
# models made in Blender (assets/modelos/*.m3g, exported by tools/blender/exportar_modelos.sh)
mkdir -p "$B/res/modelos"
cp "$ROOT"/assets/modelos/*.m3g "$B/res/modelos/" 2>/dev/null || true

echo "== packaging game jar"
cp "$ROOT/game/MANIFEST.MF" "$B/manifest.mf"
# GALERIA=<model> opens the model viewer on that model instead of the game
[ -n "$GALERIA" ] && echo "Abisal-Galeria: $GALERIA" >> "$B/manifest.mf"
# LUGAR=<n> (pruebas) empieza la partida delante del lugar n de la historia
[ -n "$LUGAR" ] && echo "Abisal-Lugar: $LUGAR" >> "$B/manifest.mf"
# DESFILE=1 (pruebas) pone todas las especies de peces en fila delante de la camara
[ -n "$DESFILE" ] && echo "Abisal-Desfile: $DESFILE" >> "$B/manifest.mf"
# POS="x,z,rumbo" (pruebas): empieza en ese punto mirando hacia ese rumbo
[ -n "$POS" ] && echo "Abisal-Pos: $POS" >> "$B/manifest.mf"
# HERRAMIENTA="arma,equipo[,horas]" (pruebas): todas las herramientas, esas en la mano
[ -n "$HERRAMIENTA" ] && echo "Abisal-Herramienta: $HERRAMIENTA" >> "$B/manifest.mf"
# PRUEBA=0 builds the public release: no cheats in the pause menu. Default is the test build.
echo "Abisal-Prueba: ${PRUEBA:-1}" >> "$B/manifest.mf"
( cd "$B/gameclasses" && cp -r . "$B/jar/" )
( cd "$B/res" && cp -r . "$B/jar/" )
rm -f "$B/abisal.jar"
( cd "$B/jar" && jar cfm "$B/abisal.jar" "$B/manifest.mf" . )

echo "== translating bytecode to C"
python3 "$ROOT/tools/j2c/j2c.py" -o "$B/gen" "$B/libclasses" "$B/abisal.jar"

echo "== extracting resources"
rm -rf "$B/data"
python3 "$ROOT/tools/assets/prepare_assets.py" "$B/abisal.jar" "$B/data"

JOBS=$(nproc 2>/dev/null || echo 2)
if [ "$TARGET" = "pc" ]; then
    make -C "$ROOT" -f Makefile.pc -j"$JOBS" GEN="$B/gen" OUT="$B/pc" ${PROF:+GC_PROF=$PROF}
    echo "built $B/pc/abisal  (run: $B/pc/abisal --data $B/data)"
else
    if ! command -v psp-config >/dev/null 2>&1; then
        echo "psp-config not found: install the PSP SDK (https://github.com/pspdev/pspdev) and add it to PATH" >&2
        exit 1
    fi
    mkdir -p "$B/psp"
    python3 "$ROOT/tools/assets/make_icon.py" "$B/psp/ICON0.PNG" || true
    make -C "$B/psp" -f "$ROOT/Makefile.psp" -j"$JOBS" ROOT="$ROOT" GEN="$B/gen"
    if [ "${PRUEBA:-1}" = "0" ]; then
        DEST="$B/psp/PSP/GAME/ABISAL"
        echo "== build PUBLICA (sin atajos de prueba)"
        # the runtime checks this file: a public build keeps no devlog at all
        touch "$B/data/publico.flag"
    else
        DEST="$B/psp/PSP/GAME/ABISAL_PRUEBA"
        echo "== build DE PRUEBA (atajos en el menu de pausa)"
        rm -f "$B/data/publico.flag"
    fi
    mkdir -p "$DEST"
    cp "$B/psp/EBOOT.PBP" "$DEST/"
    rm -rf "$DEST/data"
    cp -r "$B/data" "$DEST/data"
    echo "built $DEST  (copy the ABISAL folder to ms0:/PSP/GAME/)"
fi
