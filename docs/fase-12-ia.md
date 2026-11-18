# Fase 12: IA con Minimax

Se puede jugar contra la computadora, en la ventana y en la consola, en el ajedrez clásico y en el
modo fútbol, con tres dificultades. Se hizo en dos commits: el núcleo y los adaptadores.

## Cómo juega

- **Minimax con poda alfa-beta.** Imagina sus jugadas, las respuestas del rival, sus respuestas a
  esas... y elige la que deja la mejor posición suponiendo que el rival también juega lo mejor. La
  poda alfa-beta deja de mirar una rama apenas sabe que el rival no la va a permitir: elige lo mismo
  que Minimax puro, mirando muchas menos posiciones.
- **Profundización iterativa.** Busca a 1 jugada, después a 2, a 3... hasta que se le acaba el tiempo,
  y se queda con la última búsqueda completa. Siempre tiene una jugada lista, y el tiempo de cada
  dificultad se respeta.
- **Quietud.** Al llegar al límite, sigue mirando solo capturas y goles: así no corta el análisis en
  medio de un cambio de piezas (por ejemplo, "me como un peón" sin ver que después le comen la dama).
- **Gana lo más rápido posible.** Un mate (o el tercer gol) vale más cuanto antes llega. Si va
  perdiendo, al revés: lo demora todo lo que puede.
- **Qué es una buena posición** (cuando deja de mirar hacia adelante):
  - material: peón 100, caballo 320, alfil 330, torre 500, dama 900;
  - caballos, alfiles y peones valen un poco más cerca del centro, y los peones al avanzar;
  - fútbol: cada gol de diferencia vale 1200 (más que una dama), tener la pelota suma, y más cuanto
    más cerca está del arco rival.

## Las dificultades

Las tres usan el mismo buscador. Cambian en cuánto piensan y en cuánto se equivocan:

| | Fácil | Medio | Difícil |
|---|---|---|---|
| Mira hasta | 2 jugadas | 3 jugadas + quietud | lo que entre en el tiempo (~4) + quietud |
| Tiempo de cálculo | ~10 ms | hasta 0,8 s | 1,5 s |
| Error al elegir | hasta 1,5 peones | hasta 1/4 de peón | ninguno |

- **El error no es azar puro.** A cada jugada se le suma un error al azar y se elige la de mayor
  puntaje resultante. El fácil puede elegir una jugada hasta un peón y medio peor que la mejor, pero
  nunca regala una pieza que vio.
- **Entre jugadas iguales elige al azar**, así dos partidas no son idénticas.
- **Pausa mínima:** en la ventana, todas tardan al menos 0,7 s en mover, para que se sienta un rival y
  no un salto.
- **Con reloj**, piensa como mucho 1/30 de lo que le queda: nunca pierde por tiempo.

## Qué

### 1. Núcleo (`Nucleo.ia`)

| Clase | Qué hace |
|---|---|
| `SituacionDePartida` | Lo que la IA necesita: copia del tablero, turno, jugadas hechas y reglas. La da `Partida.situacion()`. |
| `BuscadorMinimax` | Negamax con alfa-beta, profundización iterativa y quietud. |
| `EvaluadorDePosicion` | El puntaje de una posición. |
| `Dificultad` | Profundidad, tiempo, error y quietud de cada nivel. |
| `EstrategiaDeJuego` | Interfaz (Strategy): elegir jugada y responder tablas. |
| `JugadorArtificial` | La implementación: busca según la dificultad y elige con el error. El azar y el tiempo se inyectan. |

- `Movimiento.de(jugada)` convierte la jugada elegida en el movimiento que se le pide a la partida.
- **Los finales los deciden las reglas.** El buscador usa las mismas condiciones de fin que la
  partida (jaque mate, ahogado, victoria por goles): por eso sirve igual para el clásico y el fútbol,
  sin casos especiales. Las que guardan memoria (repetición, 50 movimientos) no se miran al buscar.

### 2. Adaptadores

- **`RivalArtificial`** (sin JavaFX): la IA sentada a la mesa. Juega con `Partida.mover` como una
  persona, responde las ofertas de tablas y deshace y rehace de a dos jugadas.
- **Ventana:**
  - `TurnoDeLaIa` hace pensar a la IA en otro hilo y juega en el de la interfaz. Si la partida cambia
    mientras piensa (deshacer, tiempo, volver al menú), lo pensado se descarta.
  - `ControladorTablero` no deja tocar las piezas mientras le toca a la IA, ni rendirse u ofrecer
    tablas en su nombre.
  - El panel muestra "IA PENSANDO…" y la tarjeta de la IA no tiene botones activos.
  - Menú: "IA · la computadora" y la dificultad, que es obligatoria contra la IA.
- **Consola:** `ia` como argumento, con `facil`, `medio` o `dificil` (por defecto, medio). Por
  ejemplo: `futbol 60 ia dificil`. La IA juega con negras y la consola muestra su jugada.
- La IA es el Jugador 2: usa su equipo y se llama "IA (medio)", según la dificultad.

## Decisiones

- **Deshacer contra la IA deshace dos jugadas** (la de la IA y la tuya), así vuelve a tocarte. Si la
  IA todavía no había respondido, deshace solo la tuya.
- **Tablas:** la IA las acepta solo si va perdiendo por 3 peones o más. Nunca se rinde ni ofrece.
- **La IA también en la consola:** para el núcleo es un jugador más; cada adaptador decide cómo
  esperarla (la consola la espera, la ventana la hace pensar aparte).

## Por qué

- **La búsqueda va en el núcleo** porque es lógica del juego: sabe de reglas, no de pantallas. Los
  adaptadores solo conocen la interfaz `EstrategiaDeJuego`.
- **Busca sobre una copia y nunca sobre `Partida`.** Si imaginara jugadas en la partida real,
  cada una dispararía eventos: sonaría la hinchada y se movería la pantalla. Además, la copia permite
  pensar en otro hilo sin que la interfaz vea estados a medias.
- **Aplicar y deshacer en vez de copiar.** Cada jugada imaginada se aplica y se deshace con sus
  `Efecto` (el Command de la fase 3) sobre el mismo tablero copiado, sin copiarlo de nuevo.
- **El azar y el tiempo inyectados**, como el sorteo y el reloj: los tests son repetibles.
- **Un único buscador para las tres dificultades.** Un fácil totalmente al azar regala piezas sin
  sentido; con el mismo buscador, cada nivel juega "bien para su nivel".

## Rendimiento

Medido haciendo jugar a la IA contra sí misma 40 jugadas:

- La primera versión miraba ~17.000 posiciones por segundo: verificaba que todas las jugadas fueran
  legales antes de probarlas, incluso las que la poda después descarta.
- Verificar la legalidad recién al probar cada jugada, y cambiar streams por ciclos en las consultas
  de `Jugada` y en el detector de jaque, la llevó a ~80.000 por segundo.
- Lo que más cuesta ahora es el modelo del tablero (un mapa de posiciones) y que cada pieza calcule
  sus casillas atacadas en un conjunto nuevo. Si hiciera falta un difícil más fuerte, ese es el
  próximo paso: representar el tablero con un arreglo.

## Cuándo se rompería

- **Repetición y 50 movimientos:** la búsqueda no los ve. Una IA que va ganando podría repetir
  posiciones y terminar en tablas sin darse cuenta.
- **Una regla nueva que guarde memoria** tampoco la vería la búsqueda (solo usa las condiciones sin
  memoria).
- **Reglas especiales con estado propio** no serían seguras para pensar en otro hilo. Hoy ninguna lo
  tiene: todas calculan desde el historial.

## Pruebas

- `JugadorArtificialTest` (núcleo):
  - todas las dificultades ven el mate en 1;
  - el medio da mate en 2 aunque el rival se defienda lo mejor posible;
  - el fácil se come una dama sin defensa y el medio no deja su dama atacada;
  - en fútbol mete el gol cuando puede;
  - con la misma semilla elige lo mismo, y el difícil corta a tiempo y juega algo legal;
  - pensar no toca la partida ni dispara eventos;
  - tablas solo si va claramente perdiendo; sin jugadas no elige; la promoción lleva la pieza.
  - El tiempo es falso: los resultados no dependen de la velocidad de la máquina.
- `RivalArtificialTest`: juega solo en su turno, la fracción del reloj, deshacer y rehacer de a dos,
  respuesta a las tablas.
- `TurnoDeLaIaTest` (sin hilos de verdad): empieza si juega con blancas, responde a cada jugada,
  descarta lo pensado si la partida cambió y no juega después de cerrarse.
- `PartidaContraLaIaEnLaVistaTest`: el tablero no responde en el turno de la IA, nadie se rinde ni
  ofrece tablas en su nombre, deshacer vuelve al turno de la persona, el panel dice "IA PENSANDO…".
- Menú, ensamblado y consola: la dificultad es obligatoria contra la IA y viaja en la configuración;
  la IA es el Jugador 2 con su color y su nombre; en la consola juega sola y deshacer vuelve al turno
  de la persona.
- A mano: la IA difícil con blancas, con hilos y reloj reales, juega a los ~1,5 s desde el hilo de la
  interfaz y ese tiempo sale de su reloj.
