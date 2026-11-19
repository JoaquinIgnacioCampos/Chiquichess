# ChiquiChess: justificación de las decisiones de diseño

Cada decisión responde tres preguntas: **qué** hicimos, **por qué** y **cuándo se rompería** (en qué
situación dejaría de servir y habría que cambiarla). Son decisiones propias del grupo. El detalle de
cada una está en el documento de la fase en que se tomó (se indica entre paréntesis), y el diagrama
de clases está en [`docs/uml/`](uml/README.md).

## Índice

1. [El núcleo no conoce el exterior: puertos](#1-el-núcleo-no-conoce-el-exterior-puertos)
2. [Inyección de dependencias manual con un único lugar de ensamblado](#2-inyección-de-dependencias-manual-con-un-único-lugar-de-ensamblado)
3. [Paquetes por concepto y dependencias en un solo sentido](#3-paquetes-por-concepto-y-dependencias-en-un-solo-sentido)
4. [Las piezas se componen con reglas de movimiento](#4-las-piezas-se-componen-con-reglas-de-movimiento)
5. [La jugada como lista de efectos (Command)](#5-la-jugada-como-lista-de-efectos-command)
6. [Reglas especiales intercambiables (Strategy)](#6-reglas-especiales-intercambiables-strategy)
7. [Condiciones de fin intercambiables (Strategy)](#7-condiciones-de-fin-intercambiables-strategy)
8. [Memoria de las condiciones con Memento y huella de posición](#8-memoria-de-las-condiciones-con-memento-y-huella-de-posición)
9. [Deshacer y rehacer validados en el núcleo](#9-deshacer-y-rehacer-validados-en-el-núcleo)
10. [Los derechos se deducen del historial](#10-los-derechos-se-deducen-del-historial)
11. [Eventos de partida (Observer)](#11-eventos-de-partida-observer)
12. [Reloj como puerto y partida sin reloj (Null Object)](#12-reloj-como-puerto-y-partida-sin-reloj-null-object)
13. [El núcleo informa con códigos; los textos son del adaptador](#13-el-núcleo-informa-con-códigos-los-textos-son-del-adaptador)
14. [Modo fútbol como otro juego de reglas](#14-modo-fútbol-como-otro-juego-de-reglas)
15. [La IA en el núcleo, detrás de una interfaz](#15-la-ia-en-el-núcleo-detrás-de-una-interfaz)
16. [Adaptadores reemplazables: representación de piezas y sonido](#16-adaptadores-reemplazables-representación-de-piezas-y-sonido)
17. [Partida coordina; cada responsabilidad en su clase](#17-partida-coordina-cada-responsabilidad-en-su-clase)
18. [Puertos de entrada: los adaptadores no conocen la clase Partida](#18-puertos-de-entrada-los-adaptadores-no-conocen-la-clase-partida)
- [Cómo se extiende: pieza nueva y otro tamaño de tablero](#cómo-se-extiende)
- [Concesiones conocidas](#concesiones-conocidas)

---

## 1. El núcleo no conoce el exterior: puertos

**Qué.** El paquete `Nucleo` no importa JavaFX, Java Sound, archivos, consola ni hilos, y no lee el
reloj del sistema ni usa azar por su cuenta. Lo que necesita del exterior lo pide por interfaces
propias: `FuenteDeTiempo` (la hora), `ObservadorDePartida` (avisar qué pasó) y un `Random` inyectado
(el azar de la IA).

**Por qué.** Es lo que pide la consigna. Además, los 50 archivos de test del núcleo corren sin
pantalla, sin parlantes y con relojes falsos, así que son rápidos y repetibles. La misma partida
funciona en la consola y en la ventana sin cambios.

**Cuándo se rompería.** Si una regla necesitara algo externo que hoy no tiene puerto (por ejemplo,
consultar una base de aperturas en disco), habría que crear un puerto nuevo en el núcleo e
implementarlo en un adaptador. Leerlo directamente desde el núcleo rompería la separación.

## 2. Inyección de dependencias manual con un único lugar de ensamblado

**Qué.** `Partida` recibe todo por un único constructor: tablero, jugadores, `ReglasDePartida` y reloj.
`ReglasDePartida` agrupa los movimientos legales, el detector de jaque y las condiciones de fin.
`EnsambladorPartida` (y `AplicacionAjedrez` para la ventana) es el único lugar que elige
implementaciones concretas. No usamos un contenedor de inyección de dependencias. *(fase 2, javafx paso 1)*

**Por qué.**
- Agrupar las reglas evita que los constructores de `Partida` se multipliquen con cada dependencia
  nueva. Tuvo cinco constructores (cuatro atajos para los tests); quedó uno solo, y los tests arman
  sus reglas con `new ReglasDePartida(detector, condiciones)`.
- Con un solo constructor, el detector de jaque sale siempre de los movimientos legales: no hay forma
  de pasarle a la partida dos detectores distintos.
- Con el azar y el reloj entrando por constructor, el ensamblado también se prueba.

**Cuándo se rompería.** Si una variante necesitara dos detectores de jaque distintos, habría que
separarlos y decidir cuál define el final.

## 3. Paquetes por concepto y dependencias en un solo sentido

**Qué.** El núcleo está dividido en `bando`, `tablero`, `piezas`, `movimientos`, `jugadas`,
`reglas`, `juego`, `tiempo` e `ia`. Los tipos básicos (`Bando`, `Posicion`) viven en los paquetes de
más abajo. *(fase 1)*

**Por qué.**
- Con los tipos básicos en paquetes de alto nivel teníamos 7 ciclos de dependencia; los bajamos a 2.
- Las dependencias van `adaptadores → juego → reglas → piezas/movimientos/tablero → bando`.
- La IA es un cliente de la partida: `ia → juego`, nunca al revés. Por eso `SituacionDePartida`, lo
  que la partida le entrega a la IA, vive en `juego`.
- `Bando` también evita el choque de nombres con `javafx.scene.paint.Color`.

**Cuándo se rompería.** Quedan dos ciclos propios del dominio, que aceptamos:
- `piezas ↔ tablero`: el tablero guarda piezas y cada pieza consulta el tablero.
- `piezas ↔ movimientos`: las reglas miran qué pieza ocupa cada casilla.

Cortarlos exigiría interfaces que solo existirían para eso. Si `Bando` dejara de tener dos valores
(una variante de 3 jugadores), habría que revisar `opuesto()` y la dirección de avance.

## 4. Las piezas se componen con reglas de movimiento

**Qué.** Una `Pieza` es un color más una `ReglaMovimiento`. Las reglas se combinan:
- `Deslizamiento` y `SaltoFijo` son los movimientos básicos;
- `ReglaCompuesta` suma varias reglas;
- `SoloSiVacio`, `SoloSiCaptura` y `SoloSi` son decoradores que restringen otra regla.

`ReglasBase` ofrece las combinaciones comunes. La reina es `ortogonales + diagonales`; el peón
combina avances y capturas restringidas.

Cada subclase declara además su nombre, su código y su `valor()` material (peón 100, dama 900).

**Por qué.**
- **Composición sobre herencia:** una pieza nueva combina reglas existentes, sin reescribir cómo se
  mueve.
- La herencia de `Pieza` es de un solo nivel y solo aporta nombre, código y valor.
- **El valor es de la pieza, no de la IA.** Antes el evaluador lo decidía con un `switch` sobre la
  letra, y una pieza nueva valía 0 sin que nadie lo notara. Como `valor()` es abstracto, una pieza
  nueva no compila hasta declararlo.
- Lo probamos: una pieza "arzobispo" (alfil + caballo) funcionó, dio jaque mate y la IA jugó con
  ella sin modificar ninguna clase del núcleo.

**Cuándo se rompería.** Un movimiento que no se pueda expresar como desplazamientos desde el origen
(por ejemplo, saltar sobre una pieza para capturarla, como en las damas) necesita una
`ReglaMovimiento` nueva. Eso es una clase nueva, no un cambio. Si el movimiento además mueve otra
pieza, va como `ReglaEspecial` (decisión 6).

## 5. La jugada como lista de efectos (Command)

**Qué.** Una `Jugada` es un origen, un destino y una lista de `Efecto`: `Traslado`, `Retiro`,
`Reemplazo`, `MoverPelota` y `Pase`. Cada efecto sabe aplicarse y revertirse. `Efecto` es una
interfaz sellada. *(fase 3)*

**Por qué.**
- Captura, enroque, captura al paso, promoción, gol y pase son la misma clase con otros efectos.
  Agregarlas no cambió `Jugada` ni `Partida`.
- Deshacer no reconstruye nada: cada efecto se revierte.
- El control de "no dejar al rey en jaque" simula exactamente los efectos que se van a aplicar.
- La IA aplica y deshace jugadas imaginadas sin copiar el tablero cada vez.

**Cuándo se rompería.**
- Que `Efecto` sea sellada es una excepción consciente a "agregar sin modificar": un efecto nuevo
  obliga a declararlo. Lo preferimos porque el compilador marca los lugares que deben tratarlo (la
  huella de posición y los rasgos de los eventos), en vez de que fallen en silencio.
- Una jugada que capture dos piezas necesitaría que `capturada()` devuelva una lista.
- Una jugada que no mueva ninguna pieza (pasar el turno) se rechaza hoy al construirla.

## 6. Reglas especiales intercambiables (Strategy)

**Qué.** `Enroque`, `CapturaAlPaso`, `Promocion` y `ReglaDeLaPelota` implementan `ReglaEspecial`.
Cada una puede **sumar** jugadas o **transformar** las ya calculadas. `MovimientosLegales` las aplica
en orden. *(fases 6, 7, 7b y 9)*

**Por qué.**
- Una regla nueva es una clase nueva en la lista de `ReglasDePartida`. La captura al paso se agregó
  sin tocar `Partida`, `Jugada`, `MovimientosLegales` ni la huella.
- La promoción **transforma** en vez de sumar. Si sumara, el peón podría llegar a la última fila y
  quedarse como peón.
- Las piezas a las que se puede promover se reciben por constructor.

**Cuándo se rompería.**
- El orden importa: `ReglaDeLaPelota` tiene que ir después de `Promocion`, o un peón que promueve en
  el arco no hace gol.
- La promoción identifica la pieza elegida por su código (`Pieza.codigo()`). Dos piezas con el mismo
  código serían indistinguibles.

## 7. Condiciones de fin intercambiables (Strategy)

**Qué.** `JaqueMate`, `Ahogado`, `TablasPorRepeticion`, `TablasPorReglaDeLos50Movimientos` y
`VictoriaPorGoles` implementan `CondicionDeFinalizacion`. Cada una evalúa un `ContextoDeJugada` y
**decide quién gana**. *(fases 4 y 9)*

**Por qué.**
- `Partida` no sabe qué condiciones existen: las recorre en orden y gana la primera que se cumple.
- Que la condición decida el ganador permitió el modo fútbol, donde al llegar al límite de jugadas
  puede ganar el que tiene el turno.
- La condición también informa si hay un límite de jugadas y si se juega con goles (`marcador`).
  Así la interfaz, la consola y el sonido muestran el límite y el marcador sin saber qué condición
  los impone, y sin deducir el modo fútbol preguntando si hay pelota en el tablero.

**Cuándo se rompería.** Si una condición necesitara datos que no están en el historial (por ejemplo,
el tiempo usado en cada jugada), habría que sumarlos a `ContextoDeJugada`, no guardarlos dentro de la
condición.

## 8. Memoria de las condiciones con Memento y huella de posición

**Qué.** Las condiciones que necesitan contar (repetición, 50 movimientos) implementan
`CondicionConMemoria`: en cada jugada devuelven un `Recuerdo`, y al deshacer se restauran con él
(Memento). Las posiciones se identifican con `HuellaDePosicion`, un número de 64 bits que se
actualiza jugada a jugada (técnica de Zobrist). *(fase 4b)*

**Por qué.**
- **Velocidad:** `Partida.mover` pasó de 18,8 a 12,0 µs por jugada, lo que importa para la IA.
- **Deshacer sin desincronizar:** cada condición vuelve exactamente a su estado anterior.
- **Registrar antes de evaluar:** una jugada que da mate igual queda anotada en la repetición.

**Cuándo se rompería.**
- Dos posiciones distintas podrían dar la misma huella. Con 64 bits es despreciable, y es lo que
  aceptan los motores de ajedrez.
- La huella no incluye los derechos de enroque ni de captura al paso. Es una simplificación
  aceptada.
- Los recuerdos deben restaurarse en orden inverso. Lo garantiza `Partida`.

## 9. Deshacer y rehacer validados en el núcleo

**Qué.** `Partida` deshace coordinando el Command (decisión 5) y el Memento (decisión 8).
`HistorialDeJugadas` guarda las pilas de hechas y deshechas, y `ArbitroDeFinales` restaura las
condiciones con memoria. La regla de cuándo se puede deshacer está en `Partida`, no en la
interfaz. *(fase 5)*

**Por qué.**
- La validación vale igual desde la ventana, la consola, los tests o la IA.
- Un final producido por una jugada (mate, tablas automáticas, goles) se puede deshacer. Una
  decisión de los jugadores (rendición, acuerdo, tiempo) no.
- Contra la IA se deshacen dos jugadas, para que vuelva a tocarle al humano.

**Cuándo se rompería.**
- **Con reloj está prohibido**, porque permitiría ganar tiempo deshaciendo. Para permitirlo habría
  que decidir cómo se restaura el tiempo de cada jugador.
- **En una partida en red** habría que pedir conformidad al rival.

## 10. Los derechos se deducen del historial

**Qué.** No se guarda "el rey todavía puede enrocar" ni "este peón se puede capturar al paso". El
enroque mira en el historial si el rey o la torre se movieron, y la captura al paso mira la última
jugada. *(fases 6 y 7)*

**Por qué.** Al deshacer, los derechos vuelven solos: no hay estado que restaurar. Además, las
reglas especiales no tienen estado, así que la IA las puede usar desde otro hilo sin riesgo.

**Cuándo se rompería.** En un tablero armado a mano sin historial, el rey y las torres se consideran
sin mover. Variantes como Chess960 necesitarían otra regla de enroque.

## 11. Eventos de partida (Observer)

**Qué.** `Partida` publica `EventoDePartida`: jugada hecha, jugada deshecha, jugada rehecha y partida
finalizada. Cada evento trae rasgos (captura, enroque, promoción, pase, gol). La interfaz es
sellada. Escuchan el sonido, el panel, el cartel de gol y el turno de la IA. *(fase 10)*

**Por qué.**
- **El núcleo ya sabe qué pasó.** Si cada adaptador lo dedujera comparando antes y después, esa
  lógica se repetiría.
- **El evento dice qué pasó y no cómo se muestra**, así que el núcleo sigue sin saber que existen el
  sonido o la pantalla.
- **Es un puerto de salida:** las dependencias siguen apuntando hacia el núcleo.

**Cuándo se rompería.**
- Un observador que lanza una excepción corta la orden que la provocó. A propósito, `Partida` no
  esconde errores.
- Los avisos llegan en el hilo de quien movió. Con la IA pensando en otro hilo, `TurnoDeLaIa`
  devuelve la jugada al hilo de JavaFX antes de mover.

## 12. Reloj como puerto y partida sin reloj (Null Object)

**Qué.** `RelojPartida` tiene dos implementaciones:
- `RelojAjedrez`, que mide con una `FuenteDeTiempo` inyectada;
- `SinReloj`, un Null Object.

`Partida` no pregunta si hay reloj. *(decisiones finales de reloj e historial)*

**Por qué.** Los tests controlan el tiempo con una fuente falsa, sin esperar. Sin reloj no aparecen
`if` en la lógica de la partida.

**Cuándo se rompería.** Incrementos por jugada, pausas o controles por etapas necesitarían otra
implementación de `RelojPartida`, o ampliar la interfaz si el reloj tuviera que enterarse de la
jugada.

## 13. El núcleo informa con códigos; los textos son del adaptador

**Qué.** Los rechazos (`MovimientoInvalidoException.Motivo`, `AccionInvalidaException.Motivo`) y los
finales (`MotivoFinalizacion`) son enums. Cada adaptador tiene su catálogo de textos:
`MensajesDeLaInterfaz` y `MensajesDeConsola`. *(catálogo de mensajes)*

**Por qué.** Los textos son presentación: la ventana usa tildes y voseo, y la consola no. Un motivo
nuevo es una línea en cada catálogo, sin tocar las vistas.

**Cuándo se rompería.** Si el juego se tradujera a otros idiomas, cada catálogo pasaría a leer
archivos de recursos. La estructura no cambia.

## 14. Modo fútbol como otro juego de reglas

**Qué.** El modo "posesión y gol" no agrega ningún `if` en `Partida`. Todo entra por los puntos de
extensión que ya existían:
- una regla especial (`ReglaDeLaPelota`);
- una condición de fin (`VictoriaPorGoles`);
- dos efectos (`MoverPelota` y `Pase`);
- una disposición que decora a la estándar (`DisposicionDeFutbol`, patrón Decorator).

La pelota y los arcos viven en el tablero. *(fase 9)*

**Por qué.**
- Es la prueba de que el diseño escala: un modo de juego entero se sumó con clases nuevas.
- El gol es un efecto, así que deshacer un gol no necesitó código nuevo.
- El marcador se cuenta del historial, así que no necesita memoria propia.

**Cuándo se rompería.** Los arcos y los saques por defecto son de 8x8. Para otro tamaño hay que
pasarlos por el constructor de `ReglaDeLaPelota`. Los goles para ganar (3) están fijos en la
fábrica.

## 15. La IA en el núcleo, detrás de una interfaz

**Qué.** `JugadorArtificial` implementa `EstrategiaDeJuego` con:
- búsqueda minimax (negamax con poda alfa-beta, profundización iterativa y quietud);
- tres dificultades con tiempos de respuesta parecidos a los de una persona.

Los adaptadores solo conocen la interfaz. La IA recibe una `SituacionDePartida` con una **copia** del
tablero. *(fase 12)*

**Por qué.**
- La búsqueda es lógica del juego: conoce reglas, no pantallas.
- Al pensar sobre una copia, las jugadas imaginadas no disparan eventos ni sonidos, y puede pensar
  en otro hilo.
- El azar y el tiempo están inyectados, así que los tests de la IA son repetibles.
- Para el núcleo, la IA es un jugador más: mueve con `partida.mover(...)`.

**Cuándo se rompería.**
- La búsqueda no ve las condiciones con memoria (repetición, 50 movimientos): podría repetir
  posiciones sin darse cuenta.
- Una regla especial con estado propio no sería segura para pensar en otro hilo.
- El evaluador premia centralizar a las "piezas menores", que reconoce por valor (hasta 400). Una
  pieza nueva que valga poco pero no convenga en el centro necesitaría otro criterio.

## 16. Adaptadores reemplazables: representación de piezas y sonido

**Qué.**
- **Piezas:** `RepresentacionPiezas` tiene dos implementaciones. `RepresentacionConSprites` dibuja
  los sprites de cada club y, si falta uno, usa `RepresentacionUnicode` como respaldo.
- **Sonido:** se separa en tres partes:
  - `GuionDeSonidos` decide **qué** suena con cada evento;
  - `CatalogoDeSonidos` decide **qué archivo** es, con sonidos propios de cada club;
  - `ReproductorDeAudio` decide **cómo** se reproduce (`ReproductorJavaSound`).

  *(fases 10 y 11)*

**Por qué.**
- Cada parte se cambia o se prueba sola, sin parlantes.
- Cambiar un sonido o una imagen es dejar un archivo con el nombre correcto, sin tocar código.
- Una pieza sin sprite o un sonido que falta no rompen el juego.
- Usamos Java Sound porque viene con el JDK; JavaFX Media sería una dependencia nueva.

**Cuándo se rompería.**
- Java Sound solo lee WAV, AIFF y AU. Para MP3 haría falta otra implementación de
  `ReproductorDeAudio`.
- Un archivo con el nombre mal escrito no da error: la pieza se ve en Unicode.

## 17. Partida coordina; cada responsabilidad en su clase

**Qué.** `Partida` se ocupa de los turnos, de validar y aplicar las jugadas, y de deshacer y
rehacer. Lo demás lo delega en clases privadas del paquete `juego`:
- `ArbitroDeFinales` decide si la partida terminó y quién ganó, y coordina la memoria de las
  condiciones;
- `HistorialDeJugadas` guarda las jugadas hechas y deshechas;
- `OfertaDeTablas` maneja la oferta pendiente y quién puede ofrecer o responder;
- `AvisosDePartida` mantiene los observadores y les envía los eventos.

**Por qué.**
- **Responsabilidad única:** `Partida` había llegado a 508 líneas y cinco constructores. Cada
  regla nueva de finales, de tablas o de avisos obligaba a abrirla. Ahora cada cambio tiene su
  lugar.
- Las clases nuevas son privadas del paquete, así que hacia afuera nada cambió: los adaptadores y
  los tests siguen usando `Partida` igual.
- `ArbitroDeFinales` no guarda el estado de la partida: recibe la posición y devuelve un veredicto,
  así que es fácil de seguir y de probar.

**Cuándo se rompería.**
- Las ofertas de tablas son una clase simple con un oferente opcional, no un patrón State. Si
  aparecieran más estados (por ejemplo, ofertas con vencimiento o contraofertas), convendría
  modelar cada estado como una clase.
- Si otra clase necesitara decidir finales sin una `Partida` (la IA ya lo hace a su manera, sobre su
  copia), habría que hacer público el árbitro.

## 18. Puertos de entrada: los adaptadores no conocen la clase Partida

**Qué.** Los adaptadores reciben una de dos interfaces del núcleo:
- `ConsultaDePartida`: tablero, turno, estado, marcador, movimientos legales y suscripción a los
  eventos. Es de solo lectura.
- `PartidaEnJuego`: la extiende con las órdenes de los jugadores (mover, deshacer, rehacer, rendirse,
  tablas).

`Partida` implementa `PartidaEnJuego`. El sonido recibe `ConsultaDePartida`; la consola, el
controlador del tablero y la IA reciben `PartidaEnJuego`. Solo `EnsambladorPartida` construye y
conoce la `Partida` concreta.

**Por qué.**
- **Inversión de dependencias (la D de SOLID), también del lado de entrada.** Los adaptadores
  dependen de una abstracción del núcleo, no de su implementación. Una partida en red o un doble de
  prueba podrían implementar la misma interfaz sin cambiar ningún adaptador.
- **Segregación de interfaces (la I de SOLID).** Cada adaptador recibe solo lo que necesita. El
  sonido no puede mover piezas ni rendir a nadie, y eso lo garantiza el compilador.
- Es el mismo criterio que ya usábamos para los puertos de salida (`FuenteDeTiempo`,
  `ObservadorDePartida`): el núcleo define las interfaces y los adaptadores se adaptan a ellas.

**Cuándo se rompería.**
- Un método público nuevo de `Partida` que un adaptador necesite hay que sumarlo también a la
  interfaz que corresponda.
- Las interfaces se partieron por rol (leer o jugar). Si apareciera un adaptador que solo necesita,
  por ejemplo, los eventos, convendría una interfaz más chica todavía en vez de darle la de consulta
  entera.

---

## Cómo se extiende

### Una pieza nueva

1. Crear una subclase de `Pieza` que combine reglas existentes. Por ejemplo, un arzobispo:
   `new ReglaCompuesta(ReglasBase.diagonales(SIN_LIMITE), ReglasBase.saltosDeCaballo())`, con su
   `nombre()`, su `codigo()` y su `valor()` (el compilador los exige).
2. Ponerla en el tablero con una `DisposicionInicial` nueva, o sumarla a las opciones de `Promocion`.
3. **No hace falta modificar el núcleo.** Jaque, mate, tablas, deshacer y la IA la tratan como a
   cualquier otra pieza; la IA la valora con su `valor()`. Lo prueba `EvaluadorDePosicionTest`, con
   una pieza inventada.

Opcional: un sprite en `CatalogoSprites`. Si no tiene, se ve en Unicode.

### Otro tamaño de tablero

1. Crear una `DisposicionInicial` nueva que haga `new Tablero(ancho, alto)` y coloque las piezas.
2. El resto ya usa `ancho()` y `alto()`: el peón (fila inicial y promoción), el enroque, la notación
   y la vista.
3. En el modo fútbol hay que pasar los arcos y los saques a `ReglaDeLaPelota`.

## Concesiones conocidas

Son decisiones que tomamos sabiendo su costo. Las anotamos para poder revisarlas:

| Concesión | Por qué la aceptamos | Cuándo habría que cambiarla |
|---|---|---|
| `Pieza` tiene `esRey()`, `esPeon()` y `esTorre()` | El enroque, el jaque y la promoción necesitan reconocer esos roles | Si una regla nueva necesitara reconocer otra pieza: convendría preguntar por capacidades y no por tipo |
