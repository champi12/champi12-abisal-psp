# ABISAL — supervivencia submarina para PSP

Juego original de supervivencia y exploración submarina para PlayStation Portable
(480×272). Eres técnico de la estación de investigación *Halcyon*, destruida por un
maremoto en el planeta océano Talassa. Sobrevives en una cápsula de emergencia y tienes
que explorar un cráter submarino de unos 3 km para reparar la baliza... mientras algo
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
| Stick analógico | Mirar (o la cruceta, con "Mirar con: CRUCETA" en la pausa) |
| ✕ | Nadar hacia donde miras (para subir o bajar, mira arriba o abajo) |
| ← / → | Desplazamiento lateral (a pie; la Rémora gira como vehículo) |
| □ | Recoger / cortar / atacar / revisar restos / entrar en la cápsula / subir y bajar de la Rémora |
| △ | PDA (inventario, misión y objetivos; ✕ usar: comer, beber, botiquín, lanzar la baliza; □ tirar; R registros) |
| L | Linterna (o faros de la Rémora) |
| R (en la Rémora) | Pulso eléctrico de defensa (hiere y ahuyenta, 8 s de recarga) |
| START | Pausa y menú de pruebas |
| SELECT + R | Panel del devlog |

## La cápsula por dentro

Con □ en la escotilla entras en la cápsula: se recarga el oxígeno y se guarda la partida.
Dentro miras alrededor (stick o cruceta; con stick, ← → saltan de estación en estación) y
usas con □ lo que tienes delante:

| Estación | Para qué sirve |
|---|---|
| Fabricador | fabricar herramientas, comida, botiquines y la baliza |
| Radio | repárala con un kit; después da las misiones y marca las señales |
| Dispensador médico | un botiquín cada 10 minutos |
| Taquilla | guarda hasta 60 objetos (□ guardar, ✕ sacar) |
| Escotilla | salir (también con ○) |

## Historia y misiones

1. Fabrica el cuchillo y la linterna.
2. Repara la radio de la cápsula (kit de reparación).
3. Contenedor 7, en la meseta roja: el módulo de baliza.
4. Módulo de investigación Tetis, en las grietas termales: el núcleo de energía.
5. Cristal resonante de la fosa (la Rémora necesita el módulo de profundidad, hecho con cristal térmico).
6. Fabrica la baliza de rescate y lánzala desde la superficie (PDA, ✕): final.

La misión activa tiene un marcador en pantalla con la distancia y un punto amarillo en el
radar. El radar de la Rémora (y el escáner de radar portátil) muestra depredadores (rojo),
fauna (verde), la cápsula (blanco), la Rémora aparcada (cian) y la señal (amarillo).

## Fabricador

| Receta | Materiales | Efecto |
|---|---|---|
| Burbujín cocido | 1 burbujín crudo | +30 comida |
| Agua filtrada | 2 bulbos de alga | +30 agua |
| Cuchillo | 1 titanio, 1 cuarzo | cortar y atacar |
| Linterna | 1 cobre, 1 cuarzo | luz (L) |
| Tanque de O2 mejorado | 3 titanio, 2 cuarzo | 75 s de oxígeno |
| Aletas | 2 fibra de alga, 1 titanio, 1 coral | nadas un 30 % más rápido |
| Rémora | 4 titanio, 2 cobre, 2 cuarzo, 1 fibra | minisubmarino hasta 300 m |
| Kit de reparación | 2 cobre, 1 cuarzo, 1 fibra | repara la radio |
| Botiquín | 2 bulbos, 1 coral, 1 fibra | +50 salud |
| Escáner de radar | 2 cobre, 1 cuarzo, 1 titanio | radar en el visor |
| Módulo de profundidad | 3 titanio, 3 cristal térmico, 1 cuarzo | la Rémora baja a 900 m |
| Baliza de rescate | módulo de baliza, núcleo, cristal resonante, 2 titanio | el final |

Recursos: cobre y cuarzo (rocas con vetas), titanio (rocas grises), muestras de coral y
fibra/bulbos de alga (con cuchillo), burbujines (atrápalos con □). La partida se guarda
cada vez que entras en la cápsula; en el título aparece "Continuar".

## Zonas, fauna y ciclo de día

| Zona | Profundidad | Flora y estructuras | Fauna |
|---|---|---|---|
| Bajíos | 5–30 m | corales, anémonas, arcos, cuevas, túneles, crestas | burbujines, planeadores, cardúmenes, tortuga coralina |
| Bosque de algas | 30–95 m | algas cinta (vainas), restos de la estación | lazofines, medusas, pastores |
| **Praderas de arena** | 30–95 m | dunas, pasto marino, lirios de duna, costillares fósiles | **mantas de cristal**, **caracoles de duna** |
| Meseta roja | 95–210 m | plantas lanza, terrazas, peñascos | **dientesierras**, **acorazados** |
| Cañones | 210–460 m | pilares gigantes, flores abisales | farolillos, acorazados |
| **Grietas termales** | 210–460 m | fumarolas con humo y burbujas (queman), coral de ceniza, columnas de basalto, cristal térmico | **camarones ígneos**, **anguilas de brasa** |
| Fosa abisal | 460–900 m | flores abisales | farolillos, **Coloso de Ceniza** |

Los recursos aparecen en vetas: hay que buscarlos. Las vainas naranjas de las algas del
bosque se cortan con el cuchillo (dan fibra y un bulbo) a cualquier altura del tallo.

**Combate (cuchillo, □):** toda la fauna se puede atacar. Los peces pacíficos huyen
heridos o mueren de un golpe (el burbujín da carne). Los que tienen armas se defienden:
la medusa te quema con sus aguijones y el pastor de arena (3 golpes) te embiste con sus
cuernos. El dientesierra (4 golpes) huye herido entre golpe y golpe; el acorazado (3
golpes) responde con sus pinzas. El Coloso de Ceniza no se puede herir: huye hacia arriba.

**Historia:** en los restos de la estación *Halcyon* hay 6 registros de la tripulación
(□ para revisarlos; se releen en la PDA con R).

El día dura 20 minutos: de noche los bajíos se oscurecen y destacan la flora y los peces
luminosos (la linterna se vuelve imprescindible). En la superficie el cielo pasa por el
atardecer y la noche estrellada. El reloj está en la barra superior.

**Menú de pruebas (START):** viajar al instante a cualquier zona (← → para elegir),
oxígeno infinito y adelantar 3 horas, para probar todo sin recorrer el cráter.

## Devlog

Mientras el juego corre se escribe `devlog.txt` junto al EBOOT
(`ms0:/PSP/GAME/ABISAL/devlog.txt`). El archivo se abre y se cierra en cada línea, así
que **aunque el juego se cierre de golpe, queda escrito hasta dónde llegó**. Contiene:

- el arranque paso a paso: plataforma, gráficos, audio, memoria, máquina Java;
- la versión de firmware y la memoria libre;
- cada modelo y cada trozo del mundo que se carga, con lo que tardó;
- cada 10 s: FPS, posición, bioma, triángulos y memoria;
- avisos de juego, muertes y cualquier error con su descripción.

Con SELECT + R se ve un resumen en pantalla (FPS, ms por frame, trozos, triángulos,
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
