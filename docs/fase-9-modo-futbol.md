# Fase 9: modo fútbol "Posesión y gol"

Un modo de juego nuevo sobre el ajedrez: una pelota que se lleva y se mete en el arco rival. El
ajedrez clásico no cambia. Se hizo en tres commits (núcleo, ensamblado y consola, e interfaz) y uno
más para que el usuario elija el límite de jugadas y para marcar los arcos.

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
- **El saque.** Después del gol la pelota vuelve al campo del que lo recibió: d5 tras un gol blanco,
  d4 tras uno negro. Las piezas no se mueven. Si hay una pieza parada en el saque, esa pieza se queda
  con la pelota.
- **El final.**
  - Gana quien llega a 3 goles.
  - Al llegar al límite de jugadas (contando las de los dos bandos) gana quien tenga más goles. Con
    los goles iguales es empate. El límite lo elige el usuario, entre 10 y 400; por defecto, 90.
  - El jaque mate gana siempre. El ahogado, la triple repetición y la regla de los 50 movimientos
    siguen valiendo.
- **Quedó para después:** los pases.

## Qué

### 1. Núcleo

- `Tablero` guarda la **cancha**: la casilla de la pelota (`pelota()`, `colocarPelota()`) y las
  casillas de los arcos con el bando que defiende cada una (`arcoEn()`, `marcarArco()`). Las dos
  consultas están en `ITablero`. En el ajedrez clásico están vacías. `copiar()` las copia.
- `MoverPelota(desde, hacia, gol)` es el cuarto `Efecto`, junto a `Traslado`, `Retiro` y `Reemplazo`.
  En un gol la pelota va directo al saque y el efecto queda marcado con `gol = true`.
  `Jugada.esGol()` lo consulta.
- `ReglaDeLaPelota` es una `ReglaEspecial` (Strategy):
  - No suma jugadas: en `transformar` le agrega un `MoverPelota` a cada jugada que toca la pelota,
    igual que `Promocion` le agrega un `Reemplazo`.
  - `prepararCancha(tablero)` marca los arcos (columnas d y e de cada fila de fondo) y pone la pelota
    en el saque inicial. Después, para saber si hubo gol, la regla lee los arcos del tablero.
  - Conoce los saques. Va después de `Promocion` para ver la pieza promovida.
- `VictoriaPorGoles` es una `CondicionDeFinalizacion` sin estado: 3 goles, o más goles al llegar al
  límite de jugadas.
- **El límite de jugadas.** `CondicionDeFinalizacion` suma `limiteDeJugadas()`, vacío por defecto.
  `VictoriaPorGoles` devuelve el suyo y `Partida.limiteDeJugadas()` lo expone, para que la interfaz
  muestre "Jugada 12 de 40". `ReglasDePartida.futbol(limite)` arma las reglas con ese límite;
  `futbol()` usa 90.
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
- `DisposicionDeFutbol` (Decorator) marca la cancha sobre otra disposición inicial.

### 2. Menú, ensamblado y consola

- `ConfiguracionPartida.Modo` suma `FUTBOL`. El menú ofrece "Fútbol · Posesión y gol".
- `ConfiguracionPartida` suma `limiteDeJugadas` (de 10 a 400, por defecto 90). En el menú, la sección
  "LÍMITE DE JUGADAS (FÚTBOL)" tiene un campo numérico con flechas que solo se habilita en el modo
  fútbol. Un número fuera de rango se ajusta al extremo y un texto que no es número se descarta. La
  revancha conserva el límite.
- `EnsambladorPartida` elige las reglas y la disposición según el modo, con el límite elegido.
- **Consola:**
  - `Main futbol` juega el modo fútbol; `Main futbol 60`, con límite de 60. Sin argumentos, el clásico.
  - El tablero dibuja la pelota suelta con `o`, marca con `*` a la pieza que la lleva (`T*`) y dibuja
    las casillas vacías del arco con `#`.
  - Muestra el marcador, la jugada sobre el límite y "GOL!".

### 3. Interfaz

- `VistaTablero` dibuja la pelota como un círculo blanco: grande en el centro si está suelta, chica
  en una esquina si una pieza la lleva.
- `VistaTablero` dibuja el arco como una red: un velo claro con líneas cruzadas y una línea de gol
  blanca y gruesa del lado del borde del tablero. Va debajo de la pieza, así no la tapa.
- El panel muestra "BLANCAS 1 - 0 NEGRAS" y "Jugada 12 de 90" (con el límite elegido), y anuncia
  "¡GOL!" en el estado.
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
- **Los arcos viven en el tablero, como la pelota.** La regla los marca una vez y después los lee;
  la vista y la consola dibujan lo que tiene el tablero. Así la regla y el dibujo no pueden mostrar
  arcos distintos, y la vista no necesita conocer las reglas.
- **El límite lo informa la condición.** La interfaz no sabe qué condición termina la partida: le
  pregunta a `Partida` si hay un límite de jugadas. Si mañana otra condición tiene un límite, se
  muestra igual.
- **La condición dice quién gana.** Antes `Partida` asumía que ganaba el que acababa de mover. Al llegar al
  límite de jugadas puede ganar el que tiene el turno, así que esa decisión pasó a la condición. El valor por
  defecto conserva el comportamiento anterior.
- **El modo es otro juego de reglas, no un `if` en `Partida`.** Todo lo del fútbol entra por las
  extensiones que ya existían: una regla especial, una condición de fin, un efecto y una disposición.
  `Partida` solo cambió para que la condición elija al ganador.
- **La huella incluye la pelota.** Sin eso, dos posiciones con la pelota en lugares distintos
  contarían como repetidas.

## Cuándo se rompería

- **`ReglaDeLaPelota` tiene que ir después de `Promocion`.** Si va antes, un peón que promueve en el
  arco no tiene todavía la pieza nueva y el gol no se detecta bien.
- **Un tablero con pelota pero sin arcos marcados nunca tiene gol.** Hay que armarlo con
  `DisposicionDeFutbol` o llamar a `prepararCancha`; poner solo la pelota no alcanza.
- **Los arcos y los saques son de 8x8.** `new ReglaDeLaPelota()` usa d/e y d4/d5. Para otro tablero
  hay que pasarlos por el constructor.
- **Los goles para ganar (3) no se eligen.** Solo el límite de jugadas es configurable.

## Pruebas

- **Núcleo:** `ReglaDeLaPelotaTest`, `VictoriaPorGolesTest` y `MoverPelotaTest`. Cubren:
  - Tomar, arrastrar y pasar por encima de la pelota.
  - El robo, también en la captura al paso.
  - Los goles de cada bando, el rey que no hace gol, la promoción con gol y el saque ocupado.
  - Deshacer y rehacer un gol.
  - Los finales, incluido el ganador que tiene el turno al llegar al límite y el mate que gana sobre el gol.
  - La huella y el marcador.
  - La cancha: los arcos marcados, que sin ellos no hay gol, y el límite que eligió quien armó las
    reglas.
- **Adaptadores:**
  - El ensamblado según el modo y el límite.
  - El modo y el límite en el menú: se habilita solo en fútbol, viaja en la configuración, se ajusta
    si está fuera de rango y descarta lo que no es un número.
  - La consola con la pelota, el arco, el marcador, el límite y el gol.
  - La pelota y el arco en la vista del tablero (el arco debajo de la pieza).
  - El marcador, el límite y el "¡GOL!" en el panel.
- **En total:** 503 tests, 52 más que antes de la fase.
