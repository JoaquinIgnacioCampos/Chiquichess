# Fase 2: reglas agrupadas e inyección en el ensamblador

## Qué

**GameRules** (`core.rules`) es un record que agrupa los movimientos legales y las
condiciones de fin. El detector de jaque no se guarda aparte: `detector()` devuelve el que usan
los movimientos legales. `GameRules.classic()` arma el ajedrez estándar (jaque mate,
ahogado, repetición, 50 movimientos) con instancias nuevas en cada llamada.

**Game** tiene un constructor principal `(board, whitePlayer, blackPlayer, rules, clock)`. Los cuatro
constructores anteriores se conservan como atajos y delegan en él. Los que reciben detector y
movimientos legales por separado ahora rechazan, con `IllegalArgumentException`, que los
movimientos legales usen otro detector.

**GameAssembler** recibe por constructor un `ColorLottery` (puerto nuevo en
`adapters.configuration`) y la `TimeSource`. El constructor sin parámetros usa
`RandomColorLottery` y `SystemTimeSource`, así que `Main` y `ChessApplication` no cambian.
Cada partida se arma con `GameRules.classic()`.

## Por qué

- **Un solo detector.** Game recibía el detector dos veces: directo y dentro de
  LegalMoves, con un comentario que pedía que fueran el mismo. Nada lo controlaba.
  Ahora es imposible armar reglas con dos detectores, y los atajos viejos lo validan.
- **Constructores que no se multiplican.** Cada dependencia opcional nueva duplicaba los
  constructores de Game (2 → 4 al sumar el reloj). Las próximas reglas (jugadas especiales)
  se agregan dentro de GameRules sin tocar la firma de Game.
- **Ensamblado testeable.** El camino que usa la interfaz (`prepareConfigured`) creaba la fuente
  de tiempo del sistema y llamaba a `ThreadLocalRandom` de forma estática: no se podía probar el
  sorteo ni el reloj del menú sin un programa temporal. Ahora entran por constructor, igual que
  el resto de las dependencias externas, y se prueban con `FixedColorLottery` y `FontOfTimeFalse`.

## Cuándo se rompería

- Si una variante necesitara varios detectores (por ejemplo, un modo con dos reyes por bando que
  se evalúen distinto), habría que volver a separarlos y definir cuál decide el final.
- Los atajos de Game siguen existiendo para no reescribir los tests actuales. Si se suman muchas
  formas de construir una partida, conviene dejar solo el constructor principal y armar las reglas
  en el ensamblado.
- `GameRules.classic()` fija el ajedrez estándar. El modo fútbol necesitará su propia
  fábrica de reglas, elegida por el ensamblador según el modo del menú.

## Verificación

- 295 tests sin fallos: los 283 anteriores y 12 nuevos (5 de GameRules, 2 de Game, 5 del
  ensamblador con sorteo y fuente inyectados).
- Los dos tests de Game que usaban `StubMovementsLegal` con un detector distinto del recibido
  se ajustaron para pasar el mismo detector: justamente es la inconsistencia que ahora se rechaza.
- El azar y el reloj del sistema solo aparecen en `RandomColorLottery`, `SystemTimeSource` y el
  constructor por defecto del ensamblador.
