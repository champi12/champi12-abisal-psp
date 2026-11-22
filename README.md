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
| □ | Recoger / cortar / atacar con el arma (mano derecha) / revisar restos / entrar en la cápsula / subir y bajar de la Rémora |
| △ | PDA: inventario y herramientas. ✕ sobre algo abre un menú (usar, equipar, tirar, cancelar); R registros; L bestiario |
| L | Usar lo que llevas en la mano izquierda (linterna, boya, bengala, soldador, impulsor, escáner); en la Rémora, faros |
| R | En la Rémora, pulso eléctrico de defensa (a pie queda libre) |
| START | Pausa y menú de pruebas |
| SELECT + R | Panel del devlog |

## Las dos manos

La **mano derecha** lleva el arma: el cuchillo (corta coral y algas) o la **vara de
descarga** (aturde y hiere a lo que tengas delante, recarga 1,4 s; no corta). La **mano
izquierda** lleva un solo objeto secundario, que se usa con **L**:

| Mano izquierda | Uso (L) |
|---|---|
| Linterna | enciende y apaga |
| Boya de emergencia | se infla y te sube rápido soltando un chorro de burbujas |
| Bengalas | lanzas una: arde 90 s e ilumina de verdad lo que hay alrededor |
| Soldador | mantenlo junto a la Rémora para reparar el casco |
| Impulsor de mano | mantenlo para nadar muy rápido (suelta unas burbujas). La barra es su calor: si se vacía se sobrecalienta y se apaga hasta enfriarse |
| Escáner de mano | mantenlo apuntando a un animal o una planta (1,6 s) para registrarlo en el bestiario |

Todo se equipa desde la PDA: eliges el objeto, ✕, y en el menú "Equipar". Si se te acaba
lo que llevas (boyas, bengalas), la mano queda vacía. Una herramienta recién fabricada va
sola a la mano si la tienes libre. La PDA recuerda dónde dejaste el cursor.

**Bestiario (PDA, L):** 47 entradas de fauna y flora con notas de campo; se rellena con el
escáner.

## La Rémora: casco y averías

Cuando vas dentro, los golpes los recibe el **casco**, no tú (CASCO % en el tablero, a la
derecha). Con el casco por debajo del 60 % el vidrio se agrieta; por debajo del 30 % la
barra parpadea en rojo y los faros fallan. A 0 % la Rémora se avería y te expulsa:
repárala con un kit de reparación o con el soldador. Pasar de su profundidad máxima
también daña el casco. De noche, la Rémora aparcada muestra una baliza cian y una luz
roja que parpadea, visibles desde lejos.

## HUD

Arriba: la profundidad, una brújula en franja (los puntos cardinales se deslizan al girar,
con marcas de la señal de la misión en amarillo, la cápsula en blanco y tu base en rosa) y
la hora con el sol o la luna. Mientras no tengas el escáner de radar, la cápsula queda
marcada en pantalla con su distancia. En los primeros minutos un tutorial da consejos paso
a paso.

## La cápsula por dentro

Con □ en la escotilla entras en la cápsula: se recarga el oxígeno y se guarda la partida.
Dentro el tiempo sigue corriendo (el día avanza, y también el hambre y la sed).
Dentro miras alrededor (stick o cruceta; con stick, ← → saltan de estación en estación) y
usas con □ lo que tienes delante:

| Estación | Para qué sirve |
|---|---|
| Fabricador | fabricar herramientas, comida, botiquines y la baliza |
| Radio | repárala con un kit; después da las misiones y marca las señales |
| Dispensador médico | un botiquín cada 10 minutos |
| Taquilla | guarda hasta 60 objetos (✕ abre un menú: guardar 1 o todo, sacar 1 o todo) |
| Escotilla | salir (también con ○) |

**Errante de arrecife:** un gigante pacífico de ~40 m que pasea despacio con un jardín de
corales y hierba espesa en el lomo (puedes andar encima). Dos grupos de parásitos pican si te acercas a ellos, y es el único lugar donde
crece el *coral del errante* (□ con el cuchillo sobre su lomo).

## Fauna con ataques propios y comida con efectos

| Criatura | Zona | Ataque / efecto |
|---|---|---|
| Aguijoneros (enjambre) | bajíos, **de noche** | se lanzan en línea recta: **veneno** (daño continuo) |
| Medusa rayo | bosque, praderas | se carga y descarga en área: daño y **aturdido** (vas lento) |
| Chillón de cañón | cañones | su chillido te **ciega** unos segundos y luego muerde |
| Rondador abisal | fosa | solo se ve su señuelo; muerde y **roba oxígeno** |
| Pez candil | bajíos, **de noche** | pacífico; se atrapa y cura un poco |
| Flotador de cañón | cañones | pacífico; si lo revientas te da **aire** |

Se atrapan a mano (□): burbujín, lazofín, farolillo, caracol de duna, camarones ígneos y
pez candil. Se comen desde la PDA (✕) y algunos tienen efectos: el **farolillo** da visión
nocturna, el **caracol** es salado (quita agua), los **camarones** crudos pican (asados no),
el **hongo de cueva** cura mucho pero marea y el **fruto del lirio de duna** da comida y
agua. La **boya de emergencia** (2 bulbos y 1 cuerda) te sube rápido a la superficie.

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
| Bengala | 1 bulbo, 1 cobre | luz que lanzas |
| Vara de descarga | 2 baterías, 2 cables, 1 lámina, 1 cristal térmico | arma que aturde |
| Soldador | 1 cable, 1 batería, 1 lámina, 1 circuito | repara la Rémora |
| Impulsor de mano | 2 láminas, 2 baterías, 1 circuito, 1 cable | nadar rápido |
| Escáner de mano | 1 batería, 1 vidrio, 1 cable | bestiario |
| Módulo de hábitat | 4 láminas, 2 vidrios, 1 circuito, 1 batería | tu base |
| Granja de algas | 3 fibras, 2 bulbos, 2 vidrios, 1 lámina | módulo de la base: bulbos y agua |
| Sala de recarga | 2 baterías, 1 circuito, 2 cables, 2 láminas | módulo de la base: cura y repara la Rémora |
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
| Meseta roja | 95–210 m | plantas lanza, terrazas, peñascos, restos de la estación, cuevas y túneles | **dientesierras**, **acorazados** |
| Cañones | 210–460 m | pilares gigantes, flores abisales, cuevas y túneles (con escupidores) | farolillos, acorazados |
| **Grietas termales** | 210–460 m | fumarolas con humo y burbujas (queman), coral de ceniza, columnas de basalto, cristal térmico | **camarones ígneos**, **anguilas de brasa** |
| Fosa abisal | 460–900 m | flores abisales | farolillos, rondadores, **Coloso de Ceniza**, **Velo abisal** |

**Erizo globo:** pez redondo de la Meseta y los Cañones. No huye: si te acercas se hincha
en una bola de espinas, y pincha si lo tocas.

**Vejiga de aire:** planta rara del Bosque, la Meseta, los Cañones y la Fosa, con sacos de
gas respirable que sueltan burbujas. □ a su lado: +20 s de aire; luego tarda 90 s en
rellenarse.

**Velo abisal:** el tercer gigante pacífico. Es una cinta viva de unos 36 m, de piel
violeta con manchas luminosas, que ondula despacio sobre la fosa. Los rondadores no se
acercan a su brillo, así que nadar a su lado es un refugio en lo profundo.

**Tu base:** coloca el hábitat desde la PDA y añádele módulos. La **granja de algas**
cultiva sola (1 cosecha cada 2,5 min, hasta 6) y te la da al entrar: bulbos y agua. La
**sala de recarga** te cura al entrar y repara la Rémora si está aparcada cerca.

**Colisiones:** las cuevas, túneles, arcos y restos son sólidos también para los peces y
las criaturas. El escupidor solo dispara si te ve, y su ácido se deshace contra la roca.
Los aguijoneros defienden su zona: si te alejas o te persiguen demasiado, vuelven.

Al morir, la pantalla dice qué te mató de verdad: un golpe reciente, la falta de oxígeno,
el veneno, el hambre o la sed.

Los recursos aparecen en vetas: hay que buscarlos. Las vainas naranjas de las algas del
bosque se cortan con el cuchillo (dan fibra y un bulbo) a cualquier altura del tallo.

**Combate (cuchillo, □):** toda la fauna se puede atacar. Los peces pacíficos huyen
heridos o mueren de un golpe (el burbujín da carne). Los que tienen armas se defienden:
la medusa te quema con sus aguijones y el pastor de arena (3 golpes) te embiste con sus
cuernos. El dientesierra (4 golpes) huye herido entre golpe y golpe; el acorazado (3
golpes) responde con sus pinzas. Todos los enemigos pueden morir: el Coloso de Ceniza (14 golpes) y el Guardián del Vacío (10) aguantan mucho; el pulso de la Rémora y la vara les quitan 2. Los parásitos del errante se aplastan de un golpe.

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
