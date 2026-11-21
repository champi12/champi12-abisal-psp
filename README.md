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

**Errante de arrecife:** un gigante pacífico de ~30 m que pasea despacio con un jardín de
corales y algas en el lomo. Sobre él viven parásitos que pican, y es el único lugar donde
crece el *coral del errante* (□ con el cuchillo sobre su lomo).

## Historia y misiones

**Acto I - la baliza**
1. Fabrica el cuchillo y la linterna.
2. Repara la radio de la cápsula (kit de reparación).
3. Contenedor 7, en la meseta roja: el módulo de baliza.
4. Módulo de investigación Tetis, en las grietas termales: el núcleo de energía.
5. Cristal resonante de la fosa (la Rémora necesita el módulo de profundidad).
6. Fabrica la baliza y lánzala desde la superficie (PDA, ✕).

**Acto II - el Latido**
7. La nave de rescate no puede posarse: coloca dos sensores sísmicos (cañones y fosa).
8. La fuente es un Santuario sellado: busca los tres fragmentos de glifo (praderas,
   meseta roja, grietas termales).
9. Abre el Santuario, al borde del vacío: obtienes la llave de resonancia.
10. Lleva la llave a la Madre del Latido, bajo la fosa: el final verdadero.

Al cumplir una misión, la señal te lleva de vuelta a la cápsula: la siguiente ubicación se
conoce al escuchar la radio. La misión activa tiene un marcador en pantalla y un punto
amarillo en el radar. Duración estimada: acto I unas 3-5 horas, acto II unas 3-4 horas.

**Gigantes:** Coloso de Ceniza (fosa, hostil), Guardián del Vacío (borde del vacío,
hostil), Errante de arrecife (pacífico, con un arrecife en el lomo), Medusa luna (pacífica,
solo de noche) y la Madre del Latido.

**Tu base:** fabrica un módulo de hábitat (pestaña VEHIC./BASE) y colócalo desde la PDA
(✕). Entras con □ como en la cápsula: oxígeno, guardado, fabricador, taquilla y radio.

**Pruebas (START):** "saltar misión" avanza la historia y "viajar a: señal de la misión"
te lleva junto al objetivo.

## Fabricador

Las materias primas se convierten primero en **componentes** y cada herramienta necesita
su propia mezcla de componentes. El fabricador tiene pestañas (L/R o ← →): Componentes,
Supervivencia, Herramientas, Vehículo y Misión; cada ingrediente muestra lo que tienes y lo
que hace falta (en rojo si falta). Cerca de la cápsula siempre hay unos depósitos de cuarzo.

| Componente | Materiales |
|---|---|
| Lámina de titanio | 2 titanio |
| Cable de cobre | 2 cobre |
| Vidrio | 2 cuarzo |
| Cuerda de fibra | 3 fibra de alga |
| Batería | 1 cobre, 1 bulbo de alga |
| Circuito | 1 cable, 1 vidrio, 1 coral |

| Receta | Ingredientes | Efecto |
|---|---|---|
| Burbujín cocido / Agua filtrada | 1 burbujín / 2 bulbos | comida / agua |
| Botiquín | 2 bulbos, 1 coral, 1 fibra | +50 salud |
| Cuchillo | 1 titanio, 1 cuarzo | cortar y atacar |
| Linterna | 1 batería, 1 vidrio | luz (L) |
| Tanque de O2 mejorado | 2 láminas, 1 vidrio | 75 s de oxígeno |
| Aletas | 2 cuerdas, 1 coral | +30 % de velocidad |
| Kit de reparación | 1 circuito, 1 cable | repara la radio |
| Escáner de radar | 1 circuito, 1 batería, 1 vidrio | radar en el visor |
| Rémora | 3 láminas, 2 vidrios, 1 batería, 1 circuito, 1 cuerda | minisubmarino |
| Módulo de profundidad | 2 láminas, 3 cristal térmico, 1 circuito | Rémora a 900 m |
| Tanque de coral | 3 coral del errante, 1 lámina, 1 vidrio | 120 s de oxígeno |
| Linterna potente | 2 ópalos de cueva, 1 batería, 1 vidrio | más luz |
| Baliza de rescate | módulo de baliza, núcleo, cristal resonante, 1 lámina, 1 circuito | el final |

Recursos: cobre y cuarzo (rocas con vetas), ópalo de cueva (raro, dentro de algunas cuevas y túneles), titanio (rocas grises), muestras de coral y
fibra/bulbos de alga (con cuchillo), burbujines (atrápalos con □). La partida se guarda
cada vez que entras en la cápsula; en el título aparece "Continuar".

## Zonas, fauna y ciclo de día

| Zona | Profundidad | Flora y estructuras | Fauna |
|---|---|---|---|
| Bajíos | 5–30 m | corales, anémonas, espinos urticantes (pican), arcos, cuevas, túneles, crestas | burbujines, planeadores, cardúmenes, tortuga coralina |
| Bosque de algas | 30–95 m | algas cinta (vainas), claros de hierba, espinos, restos de la estación | lazofines, medusas, pastores, **acechador de algas**, **mordedor de pasto** (oculto en la hierba), **Errante de arrecife** |
| **Praderas de arena** | 30–95 m | dunas, hierba (esconde mordedores), pasto marino, lirios de duna, espinos, costillares fósiles | **mantas de cristal**, **caracoles de duna**, **Errante de arrecife** |
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

## Suspender la consola

Se puede suspender la PSP en cualquier momento: al volver, el juego queda en el menú de
pausa y las texturas se vuelven a cargar. El guardado no se puede interrumpir (la consola
espera a que termine) y se escribe primero en un archivo temporal, así que una partida
guardada nunca queda a medias.

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
