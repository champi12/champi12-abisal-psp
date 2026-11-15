# ABISAL — supervivencia submarina para PSP

Juego original de supervivencia y exploración submarina para PlayStation Portable
(480×272). Eres técnico de la estación de investigación *Halcyon*, destruida por un
maremoto en el planeta océano Talassa. Sobrevives en una cápsula de emergencia y tienes
que explorar un cráter submarino de unos 2 km para reparar la baliza... mientras algo
en el abismo hace temblar el fondo del mar.

Todos los modelos, sonidos y textos son originales y se generan desde el código: no hace
falta ningún archivo externo para compilarlo.

## Probarlo (ya compilado)

1. Descarga el `.zip` de la versión.
2. **PSP:** copia la carpeta `PSP` a la raíz de la Memory Stick (queda
   `ms0:/PSP/GAME/ABISAL/EBOOT.PBP`). Necesita firmware personalizado.
3. **PPSSPP:** abre `PSP/GAME/ABISAL/EBOOT.PBP` con "Cargar...".

## Controles

| PSP | Acción |
|---|---|
| Stick analógico | Mirar / dirigir |
| ✕ (o ↑) | Nadar hacia delante (hacia donde miras) |
| ◯ (o ↓) | Nadar hacia atrás |
| ← / → | Desplazamiento lateral |
| R / L | Subir / bajar |
| □ | Recoger / atrapar / usar la cápsula |
| △ | PDA (inventario; ✕ para comer) |
| START | Pausa |
| SELECT | Panel del devlog en pantalla |

## Devlog

Mientras el juego corre se escribe `devlog.txt` junto al EBOOT
(`ms0:/PSP/GAME/ABISAL/devlog.txt`). El archivo se abre y se cierra en cada línea, así
que **aunque el juego se cierre de golpe, queda escrito hasta dónde llegó**. Contiene:

- el arranque paso a paso: plataforma, gráficos, audio, memoria, máquina Java;
- la versión de firmware y la memoria libre;
- cada modelo y cada trozo del mundo que se carga, con lo que tardó;
- cada 10 s: FPS, posición, bioma, triángulos y memoria;
- avisos de juego, muertes y cualquier error con su descripción.

Con SELECT se ve un resumen en pantalla (FPS, ms por frame, trozos, triángulos,
objetos, memoria). Si algo falla en la consola, mándanos el `devlog.txt`.

## Compilar

Requisitos (Linux): Java (JDK), Python 3 con Pillow, `make` y el SDK de PSP.

Instalar el SDK de PSP (una vez):

```sh
curl -L -O https://github.com/pspdev/pspdev/releases/latest/download/pspdev-ubuntu-latest-x86_64.tar.gz
tar xzf pspdev-ubuntu-latest-x86_64.tar.gz
export PATH=$PWD/pspdev/bin:$PATH
```

Compilar para PSP (resultado en `build/psp/PSP/GAME/ABISAL/`):

```sh
./build.sh psp
```

Versión de prueba para PC (renderiza por software, sin ventana) y volcado de fotogramas:

```sh
./build.sh pc
build/pc/abisal --data build/data --frames 600 --dump frames --every 30
```

Abrir directamente la galería de modelos en un modelo concreto:

```sh
GALERIA=coral_tubos ./build.sh psp
```

## Cómo funciona

```
game/src (Java) ──javac──► abisal.jar ──┐
lib/java (APIs J2ME propias) ──javac──►─┤── tools/j2c (bytecode → C) ──► psp-gcc ──► EBOOT.PBP
runtime/ (GC, hilos, GE, audio, devlog) ┘
```

El juego se escribe en Java contra las APIs MIDP/M3G, se traduce a C en tiempo de
compilación y se compila con el SDK de PSP junto a un runtime propio (gráficos 3D por
hardware con el GE, audio, guardado en la Memory Stick). El mundo se genera por trozos
de 48×48 m alrededor del jugador, así que un mapa de 2 km cabe en la memoria de la PSP.

- `game/src/abisal/` — el juego: `Partida` (jugador, supervivencia, HUD), `Mundo`
  (trozos y objetos), `Terreno` (fondo marino y biomas), `Modelos` (todos los modelos
  low-poly), `Peces`, `Galeria` (visor de modelos), `Log` (devlog).
- `runtime/` — máquina Java en C y plataforma PSP / PC.
- `tools/` — traductor j2c, generador de sonidos, icono.
