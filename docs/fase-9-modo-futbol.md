# Fase 9: modo fútbol "Posesión y gol"

Un modo de juego nuevo sobre el ajedrez: una pelota que se lleva y se mete en el arco rival. El
ajedrez clásico no cambia. Se hizo en tres commits (núcleo, ensamblado y consola, e interfaz) y uno
más para que el usuario elija el límite de jugadas y para marcar los arcos. Al final se sumaron
los pases.

## Las reglas

- **La pelota.** Es una marca en una casilla, no una pieza: no bloquea a nadie. Empieza en d5.
  - La pieza que **termina** su movimiento en la casilla de la pelota la toma. Pasar por encima no la
    toma.
  - Quien la lleva la arrastra en cada movimiento.
  - Capturar a quien la lleva se la quita. También en la captura al paso, aunque el que captura
    termine en otra casilla.
- **El gol.** Las blancas hacen gol en d8 o e8 y las negras en d1 o e1. Esas casillas se ven
  marcadas como arco.
  - El rey puede llevar la pelota pero no hace gol.
  - Un peón puede promover y hacer gol en la misma jugada (el gol lo hace la pieza nueva).
- **El pase.** Quien tiene la pelota se la puede dar a un compañero parado en una casilla que
  podría capturar si fuera un rival. Así el rey, que no hace gol, se puede librar de la pelota.
  - Torre, alfil y dama: en su línea, con el camino libre.
  - Caballo: con su salto, por arriba de quien sea.
  - Rey: a una casilla de al lado.
  - Peón: solo en diagonal hacia adelante.
  - Las piezas no se mueven y el pase gasta el turno.
  - Si el que recibe está en el arco rival y no es el rey, es gol.
- **El saque.** Después del gol la pelota vuelve al campo del que lo recibió: d5 tras un gol blanco,
  d4 tras uno negro. Las piezas no se mueven. Si hay una pieza parada en el saque, esa pieza se queda
  con la pelota.
- **El final.**
  - Gana quien llega a 3 goles.
  - Al llegar al límite de jugadas (contando las de los dos bandos) gana quien tenga más goles. Con
    los goles iguales es empate. El límite lo elige el usuario, entre 10 y 400; por defecto, 90.
  - El jaque mate gana siempre. El ahogado, la triple repetición y la regla de los 50 movimientos
    siguen valiendo.

## Qué

### 1. Núcleo

- `Board` guarda la **cancha**: la casilla de la pelota (`ball()`, `placeBall()`) y las
  casillas de los arcos con el bando que defiende cada una (`netIn()`, `markNet()`). Las dos
  consultas están en `IBoard`. En el ajedrez clásico están vacías. `copy()` las copia.
- `MoveBall(from, toward, goal)` es el cuarto `Effect`, junto a `Transfer`, `Removal` y `Replacement`.
  En un gol la pelota va directo al saque y el efecto queda marcado con `gol = true`.
  `Move.isGoal()` lo consulta.
- `BallRule` es una `SpecialRule` (Strategy):
  - No suma jugadas: en `transform` le agrega un `MoveBall` a cada jugada que toca la pelota,
    igual que `Promotion` le agrega un `Replacement`.
  - `preparePitch(board)` marca los arcos (columnas d y e de cada fila de fondo) y pone la pelota
    en el saque inicial. Después, para saber si hubo gol, la regla lee los arcos del tablero.
  - Conoce los saques. Va después de `Promotion` para ver la pieza promovida.
- `GoalsVictory` es una `EndCondition` sin estado: 3 goles, o más goles al llegar al
  límite de jugadas.
- **El límite de jugadas.** `EndCondition` suma `moveLimit()`, vacío por defecto.
  `GoalsVictory` devuelve el suyo y `Game.moveLimit()` lo expone, para que la interfaz
  muestre "Jugada 12 de 40". `GameRules.football(moveLimit)` arma las reglas con ese límite;
  `football()` usa 90.
- `Scoreboard` cuenta los goles de cada bando en las jugadas ejecutadas. `Game.scoreboard()` lo expone.
- **Quién gana.** `EndCondition` suma `winner(state, context)`.
  - Por defecto gana el que acaba de mover, como en el jaque mate, así que las condiciones que ya
    existían no cambian.
  - `GoalsVictory` lo redefine: gana el que tiene más goles.
  - `Game` le pide el ganador a la condición que terminó la partida.
- `GameState` suma `VICTORY_BY_GOALS` y `DRAW_TIED_ON_GOALS`, con los motivos `GOALS` y
  `TIED_ON_GOALS`.
- `PositionFingerprint` incluye la casilla de la pelota.
- `GameRules.football()` arma las reglas: las especiales clásicas más la pelota. Las condiciones
  son, en orden: mate, goles, ahogado, repetición y 50 movimientos.
- `FootballLayout` (Decorator) marca la cancha sobre otra disposición inicial.
- **El pase.**
  - `Pass(passer, from, receiver, toward)` es un `Effect` nuevo: verifica que las dos piezas sigan en
    su lugar y mueve la pelota. Si es gol, la jugada le suma un `MoveBall` al saque, como un gol
    llevando la pelota.
  - `Move` ahora acepta jugadas que mueven la pieza de origen **o** son un pase desde ahí.
    `pieceMoved()` devuelve al que pasa y `isPass()` las distingue.
  - `BallRule.moves()` genera los pases de quien tiene la pelota. Para saber a quién llega,
    pone un rival de mentira en la casilla del compañero, en una copia del tablero, y pregunta si la
    pieza lo podría capturar. Así cada pieza pasa con su propio movimiento de captura, sin repetir
    las reglas de movimiento.
  - `Promotion` ignora los pases (el peón no se mueve) y la regla de los 50 movimientos no cuenta el
    pase de un peón como un avance de peón.

### 2. Menú, ensamblado y consola

- `GameConfiguration.Mode` suma `FUTBOL`. El menú ofrece "Fútbol · Posesión y gol".
- `GameConfiguration` suma `moveLimit` (de 10 a 400, por defecto 90). En el menú, la sección
  "LÍMITE DE JUGADAS (FÚTBOL)" tiene un campo numérico con flechas que solo se habilita en el modo
  fútbol. Un número fuera de rango se ajusta al extremo y un texto que no es número se descarta. La
  revancha conserva el límite.
- `GameAssembler` elige las reglas y la disposición según el modo, con el límite elegido.
- **Consola:**
  - `Main futbol` juega el modo fútbol; `Main futbol 60`, con límite de 60. Sin argumentos, el clásico.
  - El tablero dibuja la pelota suelta con `o`, marca con `*` a la pieza que la lleva (`T*`) y dibuja
    las casillas vacías del arco con `#`.
  - Muestra el marcador, la jugada sobre el límite y "GOL!".
  - Un pase se escribe como un movimiento hacia el compañero (`e2 d3`) y se avisa con "Pase!".

### 3. Interfaz

- `BoardView` dibuja la pelota como un círculo blanco: grande en el centro si está suelta, chica
  en una esquina si una pieza la lleva.
- `BoardView` dibuja el arco como una red: un velo claro con líneas cruzadas y una línea de gol
  blanca y gruesa del lado del borde del tablero. Va debajo de la pieza, así no la tapa.
- Para pasar se hace clic en la pieza con la pelota y después en el compañero. Los compañeros que
  pueden recibir se marcan con un anillo blanco punteado, distinto del anillo de captura.
- El panel muestra "BLANCAS 1 - 0 NEGRAS" y "Jugada 12 de 90" (con el límite elegido), y anuncia
  "¡GOL!" en el estado.
  La modalidad dice "Partida local · Fútbol: posesión y gol".
- `BoardController.scoreboard()` está vacío en el ajedrez clásico; así el panel sabe si mostrar los
  goles.

## Por qué

- **La pelota vive en el tablero.** "Llevar la pelota" significa estar parado en su casilla. Con eso
  salen solas tres reglas: tomarla al terminar ahí, quitársela al capturar y no tomarla al pasar por
  encima. No hace falta guardar quién la tiene.
- **El gol es un efecto de la jugada (Command).** Como `MoveBall` es un efecto más, deshacer y
  rehacer un gol no necesitó código nuevo.
- **El marcador se cuenta en el historial, sin Memento.** Cada gol ya está guardado en su jugada.
  Contarlo de nuevo es simple y siempre coincide con el historial, también al deshacer.
  `GoalsVictory` no necesita memoria propia.
- **Los arcos viven en el tablero, como la pelota.** La regla los marca una vez y después los lee;
  la vista y la consola dibujan lo que tiene el tablero. Así la regla y el dibujo no pueden mostrar
  arcos distintos, y la vista no necesita conocer las reglas.
- **El pase es una jugada más.** Usa la misma notación que un movimiento, porque nadie puede moverse
  a una casilla ocupada por una pieza propia: no hay confusión posible. Por eso la consola, el clic en
  la interfaz, deshacer y la validación de jaque funcionan sin cambios. De ahí salen solas varias
  reglas: en jaque no se puede pasar (el pase no saca al rey del jaque), un pase cuenta como jugada
  legal para el ahogado, y pasarse la pelota de ida y vuelta lleva a tablas por repetición.
- **El límite lo informa la condición.** La interfaz no sabe qué condición termina la partida: le
  pregunta a `Game` si hay un límite de jugadas. Si mañana otra condición tiene un límite, se
  muestra igual.
- **La condición dice quién gana.** Antes `Game` asumía que ganaba el que acababa de mover. Al llegar al
  límite de jugadas puede ganar el que tiene el turno, así que esa decisión pasó a la condición. El valor por
  defecto conserva el comportamiento anterior.
- **El modo es otro juego de reglas, no un `if` en `Game`.** Todo lo del fútbol entra por las
  extensiones que ya existían: una regla especial, una condición de fin, un efecto y una disposición.
  `Game` solo cambió para que la condición elija al ganador.
- **La huella incluye la pelota.** Sin eso, dos posiciones con la pelota en lugares distintos
  contarían como repetidas.

## Cuándo se rompería

- **`BallRule` tiene que ir después de `Promotion`.** Si va antes, un peón que promueve en el
  arco no tiene todavía la pieza nueva y el gol no se detecta bien.
- **Un tablero con pelota pero sin arcos marcados nunca tiene gol.** Hay que armarlo con
  `FootballLayout` o llamar a `preparePitch`; poner solo la pelota no alcanza.
- **Los arcos y los saques son de 8x8.** `new BallRule()` usa d/e y d4/d5. Para otro tablero
  hay que pasarlos por el constructor.
- **Los goles para ganar (3) no se eligen.** Solo el límite de jugadas es configurable.
- **El pase depende de que el movimiento de captura de cada pieza no mire qué pieza captura.** El
  rival de mentira es un peón. Si una pieza nueva solo pudiera capturar ciertas piezas, habría que
  revisar `BallRule.reaches`.
- **Un pase desde la casilla inicial del rey no le quita el enroque**, porque el rey no se movió.

## Pruebas

- **Núcleo:** `BallRuleTest`, `GoalsVictoryTest` y `MoveBallTest`. Cubren:
  - Tomar, arrastrar y pasar por encima de la pelota.
  - El robo, también en la captura al paso.
  - Los goles de cada bando, el rey que no hace gol, la promoción con gol y el saque ocupado.
  - Deshacer y rehacer un gol.
  - Los finales, incluido el ganador que tiene el turno al llegar al límite y el mate que gana sobre el gol.
  - La huella y el marcador.
  - La cancha: los arcos marcados, que sin ellos no hay gol, y el límite que eligió quien armó las
    reglas.
  - `PassTest`: el rey que se libra de la pelota, la torre con camino libre o tapado, el caballo
    por arriba, el peón solo en diagonal hacia adelante, el pase al arco (gol, salvo al rey), sin
    pase en jaque, el peón que pasa desde la séptima sin promover, deshacer un pase y que solo pasa
    quien tiene la pelota.
  - El efecto `Pass`, la huella y la regla de los 50 con un pase de peón.
- **Adaptadores:**
  - El ensamblado según el modo y el límite.
  - El modo y el límite en el menú: se habilita solo en fútbol, viaja en la configuración, se ajusta
    si está fuera de rango y descarta lo que no es un número.
  - La consola con la pelota, el arco, el marcador, el límite, el gol y el pase.
  - La pelota, el arco y el anillo de pase en la vista del tablero (el arco debajo de la pieza).
  - El marcador, el límite y el "¡GOL!" en el panel.
- **En total:** 519 tests, 68 más que antes de la fase.
