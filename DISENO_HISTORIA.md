# ABISAL — Documento de historia y dirección

> Documento **interno** (para nosotros, no va dentro del juego). Sirve para saber
> qué historia estamos contando, qué ya está hecho y hacia dónde vamos.

---

## 1. Premisa

**Talassa** es un planeta océano. La estación de investigación **Halcyon** orbitaba su
cráter cuando un maremoto la partió. Tú eres su **xenobiólogo**: no un soldado, no un
técnico de guerra. Llevas un cuchillo de muestras, un escáner y la manía de anotarlo todo.

Bajo el cráter, cada **41 minutos**, el fondo **late**. La tripulación lo llamó **el
Latido**. No es geología: es un organismo. Tu trabajo no es escapar, sino **entender qué
respira ahí abajo** antes de que el cráter se parta.

**Gancho único:** el cráter no es un escenario, es un ser vivo. La fauna gigante (los
colosos) son sus simbiontes; el Santuario es su cuna; la Madre es su corazón.

---

## 2. Personajes (los registros de la tripulación)

| Personaje | Rol | Función narrativa |
|---|---|---|
| **Dra. Inés Vargas** | Bióloga | Descubre el Latido; intuye que es una voz. |
| **Teo Ruiz** | Ingeniero | Repara y fabrica; los restos de la estación. |
| **Comandante Okafor** | Mando | Ordena evacuar; sospecha que la estación lo despertó. |
| **Mara Lund** | Piloto | Ve las luces de los glifos. |
| **Tú** | Xenobiólogo | El único que queda; el que escucha. |

---

## 3. Estructura (4 actos, v0.40)

El cráter tiene **10 zonas, una de cada tipo** (regiones únicas alrededor de los
Bajíos), y cada etapa te manda a una zona distinta. Las zonas radiactivas (Cañones,
Grietas termales, Osario, Fosa) piden equipo hecho con materiales de *otras* zonas.

| Zona | Profundidad | Material propio | Depredadores (horario) |
|---|---|---|---|
| Bajíos (centro) | 5-27 m | cuarzo, titanio, cobre, coral | aguijoneros (solo de noche) |
| Bosque de algas (NE) | ~55 m | fibra, bulbos | acechador (de noche), medusa rayo (de día) |
| Praderas de arena (SO) | ~45 m | fruto de lirio | mordedor de pasto, gusano de arena |
| Arrecife de cristal (N) | ~130 m | **cristal plomado** | lanza vítrea (de día) |
| Meseta roja (S) | ~150 m | **azufre** | dientesierra, bomba de azufre (kamikaze) |
| Cañones (O) | ~300 m | ópalo | chillón (de noche), escupidor |
| Grietas termales (E) | ~320 m | cristal térmico, **uranio** | anguila de brasa |
| Llanura exterior (ENE) | ~240 m | titanio, ópalo | acorazado, guardián del borde |
| Osario de los colosos (NNE) | ~400 m | **marfil** | roehuesos (de noche, en grupo) |
| Fosa abisal (SSO) | hasta 850 m | — | rondador, Coloso de Ceniza |

### Acto I — Sobrevivir
0. Cuchillo y linterna (Bajíos: cuarzo, titanio y cobre junto a la cápsula).
1. Reparar la radio.
2. Contenedor 7 en el **bosque de algas**: módulo de baliza.
3. Módulo Tetis en las **praderas de arena**: núcleo de energía.
4. Cristal resonante en el **arrecife de cristal** (130 m: se desbloquea la Rémora).
5. Lanzar la baliza desde la superficie. Responde la **Petrel**, la segunda nave: la
   Albatros vino antes y no volvió.

### Acto II — El Latido
6. Sensor sísmico en la **meseta roja** (cerámica: azufre de la meseta).
7. Segundo sensor en la **llanura exterior**.
8. Entrar en el puente del **pecio Albatros**: su caja negra marca tres piedras talladas.
9-11. Fragmentos de glifo en los **cañones**, las **grietas termales** (traje ligero:
   cristal plomado del arrecife) y el **arrecife de cristal**.

### Acto III — La cuna
12. El **órgano de los colosos** en el **osario** (Rémora con módulo de profundidad,
    450 m): los glifos encuentran su voz, el diapasón de hueso.
13. Fabricar el **Batel** (marfil del osario, uranio de las grietas): 900 m.
14. Abrir el **Santuario** al borde del vacío en la **fosa**: llave de resonancia.
15. La llave calma a la **Madre del Latido**... pero su luz se apaga.

### Acto IV — El corazón de Talassa
16. Apagar el **reactor de la Halcyon** en las **grietas termales** (traje pesado): era
    él lo que cantaba desafinado y le dolía a la Madre. Okafor lo sabía.
17. **La decisión**, en la boya de la Petrel (superficie de la llanura exterior):
    subir y volver a casa (tus datos llegan a la flota) o quedarte como guardián de
    Talassa. Dos finales.

Duración estimada: acto I 3-4 h, acto II 4-5 h, acto III 3-4 h, acto IV 1-2 h
(unas **12-15 horas** la historia, más con el bestiario y los ecos).

## 4. Lo que YA está implementado (dónde vive en el código)

| Sistema | Archivo | Estado |
|---|---|---|
| Objetivos y etapas (14) | `game/src/abisal/Historia.java` (`MISION`, `avanzar`) | ✅ |
| Mensajes de radio (13) | `Historia.java` (`RADIO`) | ✅ |
| Registros de la tripulación (14) | `game/src/abisal/Partida.java` (`REGISTROS`) | ✅ |
| Finales e inscripción del Santuario | `Historia.java` (`FINAL`, `FINAL2`, `INSCRIPCION`) | ✅ |
| Bestiario (50 fichas) | `game/src/abisal/Bestiario.java` | ✅ |
| Ecos de datos | `game/src/abisal/Ecos.java` | ✅ |
| Mapa y balizas de señal | `game/src/abisal/Mapa.java` | ✅ |
| Latido global (sonido + sacudida) | `Partida.java` (`latidoGlobalT`, `sismoT`) | ✅ |
| Latido cerca de la Madre | `Historia.latidoCerca` | ✅ |
| Mensajes de la estación ("NÚCLEO") | `Historia.java` (`RADIO`) | ✅ |

---

## 5. Ideas de dirección (lo que falta para que NO sea un clon)

1. **El Coro** (mecánica): en vez de "llamar al rescate", el aparato **canta** al Latido.
   Se desbloquea al juntar los glifos; el jugador calibra 3 notas.
2. **Simbiosis**: alimentar a un coloso lo vuelve aliado (te guía a recursos, te protege).
   El jardín del Errante es la semilla de esto.
3. **Fauna que reacciona a la luz**: apagar la linterna atrae a ciertas especies;
   encenderla las espanta. Una mecánica social, no de combate.
4. **Acto III**: el cráter se abre; el jugador elige quedarse o llamar a la flota.
5. **Flora viva**: más plantas con efecto (la anémona curativa ya está).

---

## 6. Tono y reglas de escritura

- **Español**, frases cortas, científico pero humano (Vargas escribe como bióloga).
- Nunca "misión: mata al monstruo"; siempre **entender, registrar, calmar**.
- El jugador es **observador**, no héroe de acción. El cuchillo es de muestras.
- El misterio se revela por **registros**, no por cinemáticas.

---

## 7. Estado resumido

- **Historia principal: completa** (actos I y II implementados de punta a punta).
- **Acto III: en diseño** (la decisión final y el Coro).
- **Identidad**: ya no es "el clon de Subnautica" en textos (Núcleo, Replicador,
  Santuario, Latido, Madre), pero faltan las mecánicas únicas (§5).
