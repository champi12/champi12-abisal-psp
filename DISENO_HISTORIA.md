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

## 3. Estructura (actos)

### Acto I — Sobrevivir y llamar (IMPLEMENTADO)
1. Reparar la radio del módulo.
2. Recuperar el módulo de baliza (Contenedor 7, meseta roja).
3. Recuperar el núcleo del módulo Tetis (grietas termales).
4. Extraer un cristal resonante en la fosa (necesita módulo de profundidad).
5. Ensamblar y lanzar la baliza desde la superficie.
6. La nave **Albatros** responde, pero no puede posarse: cada 41 min un pulso sacude todo.

### Acto II — El Latido (IMPLEMENTADO)
7. Fabricar dos **sensores sísmicos** y colocarlos (cañones y fosa).
8. Triangular: el pulso nace bajo una estructura sellada al borde del vacío.
9. Recoger los **3 fragmentos de glifo** (praderas, meseta, grietas termales).
10. Abrir el **Santuario** con los 3 fragmentos.
11. Obtener la **Llave de resonancia**.
12. Llevarla a la **Madre del Latido**: la llave canta, ella se calma, el pulso cesa.
13. **Final**: "Talassa duerme tranquila". La Albatros baja al amanecer.

### Acto III — El cráter vivo (PENDIENTE / en diseño)
- El Latido no era un monstruo: era el **corazón del cráter**. Calmarlo no lo apaga.
- Queda **elegir**: dejar que Talassa viva (quedarte) o llamar a la flota (que lo
  desmontaría). La decisión moral que Subnautica no tiene.
- Los **colosos** (Errante, Velo, Madre) son sus órganos; el jugador puede "cantarles"
  con el **Coro** (ver §5).

---

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
