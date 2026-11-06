# Fase 9: modo fútbol "Posesión y gol"

Un modo de juego nuevo sobre el ajedrez: una pelota que se lleva y se mete en el arco rival. El
ajedrez clásico no cambia. Se hizo en tres commits: núcleo, ensamblado y consola, e interfaz.

## Las reglas

- **La pelota.** Es una marca en una casilla, no una pieza: no bloquea a nadie. Empieza en d5.
  - La pieza que **termina** su movimiento en la casilla de la pelota la toma. Pasar por encima no la
    toma.
  - Quien la lleva la arrastra en cada movimiento.
  - Capturar a quien la lleva se la quita. También en la captura al paso, aunque el que captura
    termine en otra casilla.
- **El gol.** Las blancas hacen gol en d8 o e8 y las negras en d1 o e1.
  - El rey puede llevar la pelota pero no hace gol.
  - Un peón puede promover y hacer gol en la misma jugada (el gol lo hace la pieza nueva).
- **El saque.** Después del gol la pelota vuelve al campo del que lo recibió: d5 tras un gol blanco,
  d4 tras uno negro. Las piezas no se mueven. Si hay una pieza parada en el saque, esa pieza se queda
  con la pelota.
- **El final.**
  - Gana quien llega a 3 goles.
  - En la jugada 90 (contando las de los dos bandos) gana quien tenga más goles. Con los goles
    iguales es empate.
  - El jaque mate gana siempre. El ahogado, la triple repetición y la regla de los 50 movimientos
    siguen valiendo.
- **Quedó para después:** los pases.

## Qué

### 1. Núcleo

- `Tablero` guarda la casilla de la pelota: `pelota()`, que está en `ITablero`, y `colocarPelota()`.
  En el ajedrez clásico está vacía. `copiar()` la copia.
- `MoverPelota(desde, hacia, gol)` es el cuarto `Efecto`, junto a `Traslado`, `Retiro` y `Reemplazo`.
  En un gol la pelota va directo al saque y el efecto queda marcado con `gol = true`.
  `Jugada.esGol()` lo consulta.
- `ReglaDeLaPelota` es una `ReglaEspecial` (Strategy):
  - No suma jugadas: en `transformar` le agrega un `MoverPelota` a cada jugada que toca la pelota,
    igual que `Promocion` le agrega un `Reemplazo`.
  - Conoce los arcos y los saques. Va después de `Promocion` para ver la pieza promovida.
- `VictoriaPorGoles` es una `CondicionDeFinalizacion` sin estado: 3 goles, o más goles en la jugada 90.
- `Marcador` cuenta los goles de cada bando en las jugadas ejecutadas. `Partida.marcador()` lo expone.
- **Quién gana.** `CondicionDeFinalizacion` suma `ganador(estado, contexto)`.
  - Por defecto gana el que acaba de mover, como en el jaque mate, así que las condiciones que ya
    existían no cambian.
  - `VictoriaPorGoles` lo redefine: gana el que tiene más goles.
  - `Partida` le pide el ganador a la condición que terminó la partida.
- `EstadoPartida` suma `VICTORIA_POR_GOLES` y `TABLAS_EMPATE_EN_GOLES`, con los motivos `GOLES` y
  `EMPATE_EN_GOLES`.
- `HuellaDePosicion` incluye la casilla de la pelota.
- `ReglasDePartida.futbol()` arma las reglas: las especiales clásicas más la pelota. Las condiciones
  son, en orden: mate, goles, ahogado, repetición y 50 movimientos.
- `DisposicionConPelota` (Decorator) pone la pelota en el saque sobre otra disposición inicial.

### 2. Menú, ensamblado y consola

- `ConfiguracionPartida.Modo` suma `FUTBOL`. El menú ofrece "Fútbol · Posesión y gol".
- `EnsambladorPartida` elige las reglas y la disposición según el modo.
- **Consola:**
  - `Main futbol` juega el modo fútbol; sin argumentos, el clásico.
  - El tablero dibuja la pelota suelta con `o` y marca con `*` a la pieza que la lleva (`T*`).
  - Muestra el marcador, la jugada sobre 90 y "GOL!".

### 3. Interfaz

- `VistaTablero` dibuja la pelota como un círculo blanco: grande en el centro si está suelta, chica
  en una esquina si una pieza la lleva.
- El panel muestra "BLANCAS 1 - 0 NEGRAS" y "Jugada 12 de 90", y anuncia "¡GOL!" en el estado.
  La modalidad dice "Partida local · Fútbol: posesión y gol".
- `ControladorTablero.marcador()` está vacío en el ajedrez clásico; así el panel sabe si mostrar los
  goles.

## Por qué

- **La pelota vive en el tablero.** "Llevar la pelota" significa estar parado en su casilla. Con eso
  salen solas tres reglas: tomarla al terminar ahí, quitársela al capturar y no tomarla al pasar por
  encima. No hace falta guardar quién la tiene.
- **El gol es un efecto de la jugada (Command).** Como `MoverPelota` es un efecto más, deshacer y
  rehacer un gol no necesitó código nuevo.
- **El marcador se cuenta en el historial, sin Memento.** Cada gol ya está guardado en su jugada.
  Contarlo de nuevo es simple y siempre coincide con el historial, también al deshacer.
  `VictoriaPorGoles` no necesita memoria propia.
  - **Esto cambia respecto de la presentación:** la diapositiva 16 decía que el marcador era "una
    condición con memoria (Memento)". Hay que corregirla.
- **La condición dice quién gana.** Antes `Partida` asumía que ganaba el que acababa de mover. En la
  jugada 90 puede ganar el que tiene el turno, así que esa decisión pasó a la condición. El valor por
  defecto conserva el comportamiento anterior.
- **El modo es otro juego de reglas, no un `if` en `Partida`.** Todo lo del fútbol entra por las
  extensiones que ya existían: una regla especial, una condición de fin, un efecto y una disposición.
  `Partida` solo cambió para que la condición elija al ganador.
- **La huella incluye la pelota.** Sin eso, dos posiciones con la pelota en lugares distintos
  contarían como repetidas.

## Cuándo se rompería

- **`ReglaDeLaPelota` tiene que ir después de `Promocion`.** Si va antes, un peón que promueve en el
  arco no tiene todavía la pieza nueva y el gol no se detecta bien.
- **El "90" de la consola y del panel está escrito a mano.** Si `VictoriaPorGoles` se arma con otro
  límite, esos textos no se enteran.
- **Los arcos y los saques son de 8x8.** `new ReglaDeLaPelota()` usa d/e y d4/d5. Para otro tablero
  hay que pasarlos por el constructor.
- **Los arcos no se marcan en el tablero gráfico.** El jugador tiene que saber que son d8/e8 y d1/e1.

## Pruebas

- **Núcleo:** `ReglaDeLaPelotaTest`, `VictoriaPorGolesTest` y `MoverPelotaTest`. Cubren:
  - Tomar, arrastrar y pasar por encima de la pelota.
  - El robo, también en la captura al paso.
  - Los goles de cada bando, el rey que no hace gol, la promoción con gol y el saque ocupado.
  - Deshacer y rehacer un gol.
  - Los finales, incluido el ganador que tiene el turno en la jugada 90 y el mate que gana sobre el gol.
  - La huella y el marcador.
- **Adaptadores:**
  - El ensamblado según el modo.
  - El modo en el menú.
  - La consola con la pelota, el marcador y el gol.
  - La pelota en la vista del tablero.
  - El marcador y el "¡GOL!" en el panel.
- **En total:** 491 tests, 40 más que antes de la fase.
