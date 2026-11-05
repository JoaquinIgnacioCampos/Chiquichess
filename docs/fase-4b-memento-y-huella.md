# Fase 4b: condiciones con Memento y huella de posición

Reemplaza la forma de calcular las tablas de la fase 4 (recorrer el historial en cada jugada) por
contadores que se pueden deshacer. Se mantiene todo lo demás de la fase 4: `ContextoDeJugada` sigue
incluyendo el historial y deduciendo la pieza movida y la captura de la última jugada.

## Qué

**Memento** (`Nucleo.reglas`):

- `Recuerdo`: interfaz vacía. Es la foto que crea una condición; quien la guarda no conoce su contenido.
- `CondicionConMemoria extends CondicionDeFinalizacion`: suma `registrar(contexto)`, que actualiza el
  estado y devuelve un `Recuerdo`, y `restaurar(recuerdo)`, que vuelve al estado anterior.
- `TablasPorReglaDeLos50Movimientos`: lleva un contador; su recuerdo es el valor anterior.
- `TablasPorRepeticion`: cuenta cuántas veces apareció cada huella; su recuerdo es qué huella sumó y
  cuál era la anterior.
- `Partida` es el cuidador: después de cada jugada hace que **todas** las condiciones con memoria
  registren, y recién después las evalúa en orden. Guarda los recuerdos de cada jugada para el
  deshacer de la fase 5.

**Huella de posición** (`Nucleo.jugadas.HuellaDePosicion`, hash de Zobrist): resume la foto completa
del tablero (qué pieza hay en cada casilla y a quién le toca) en un número de 64 bits. Cada pieza en
cada casilla tiene un número fijo; la huella es el XOR de todos. `de(tablero, turno)` la calcula desde
cero y `tras(huella, jugada)` la actualiza con los efectos de la jugada, sin recorrer el tablero.
No cambia la clase `Posicion` (que es una casilla) ni ninguna otra lógica del juego.

## Por qué

- **Preparar la IA.** Una IA analiza muchísimas posiciones: necesita reconocerlas rápido y deshacer
  jugadas sin recalcular. La huella le sirve para eso y queda como pieza reutilizable del núcleo.
- **Velocidad.** Con todas las condiciones activas, `Partida.mover` bajó de 18,8 a 12,0 µs por
  jugada; la repetición pasó de costar unos 7 µs a menos de 1 µs.
- **Correcto al deshacer.** Cada condición devuelve su recuerdo y vuelve exactamente al estado
  anterior, incluso si la jugada había reiniciado el contador de 50 movimientos.
- **Registrar antes de evaluar.** Partida deja de consultar condiciones en cuanto una responde. Si el
  registro dependiera de la evaluación, una jugada que da mate no quedaría anotada en la repetición y
  deshacerla desincronizaría la cuenta. Separar `registrar` de `evaluar` lo evita.
- **Sealed + huella.** `HuellaDePosicion` recorre los efectos con un `switch` sobre la interfaz
  sellada `Efecto`: si se agrega un efecto nuevo (por ejemplo para la promoción), el compilador obliga
  a actualizar la huella.

## Cuándo se rompería

- **Colisiones.** Dos posiciones distintas podrían dar la misma huella por azar. Con 64 bits la
  probabilidad es despreciable y es lo que aceptan los motores de ajedrez; si se necesitara certeza
  absoluta, habría que confirmar cada coincidencia comparando las piezas.
- **Identidad de pieza.** La huella distingue piezas por bando y nombre, como lo hacía la regla de
  repetición. Dos tipos de pieza distintos con el mismo `nombre()` se considerarían iguales.
- **Una instancia por partida.** Las condiciones con memoria vuelven a guardar estado;
  `ReglasDePartida.clasicas()` crea instancias nuevas en cada llamada.
- **Orden de restauración.** Los recuerdos deben restaurarse en orden inverso al que se crearon;
  restaurar uno intermedio deja la condición inconsistente. Partida lo garantiza al deshacer.
- **Derechos de enroque y captura al paso.** La huella no los incluye, igual que antes: dos
  posiciones con las mismas piezas pero distintos derechos se consideran iguales.

## Verificación

- 334 tests sin fallos. Se reescribieron los tests de las dos condiciones para el contrato de
  registro y restauración (9 de 50 movimientos, 9 de repetición), con el criterio de Memento: después
  de restaurar, la condición responde igual que antes de registrar. Nuevos: 6 de `HuellaDePosicion` y
  3 de `Partida` (las condiciones con memoria registran aunque otra termine antes, registran antes de
  evaluar, y una jugada inválida no registra nada).
- Prueba diferencial fuera del repo: 1000 partidas aleatorias dieron la misma secuencia de estados
  que la versión anterior en las 437.531 jugadas (110 finales por repetición, 377 por 50 movimientos).
- Huella: en 2000 partidas aleatorias, la huella actualizada jugada a jugada coincidió con la
  calculada desde cero en las 558.121 jugadas.
