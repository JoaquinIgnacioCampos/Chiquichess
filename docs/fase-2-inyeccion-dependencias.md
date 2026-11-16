# Fase 2: reglas agrupadas e inyección en el ensamblador

## Qué

**ReglasDePartida** (`Nucleo.reglas`) es un record que agrupa los movimientos legales y las
condiciones de fin. El detector de jaque no se guarda aparte: `detector()` devuelve el que usan
los movimientos legales. `ReglasDePartida.clasicas()` arma el ajedrez estándar (jaque mate,
ahogado, repetición, 50 movimientos) con instancias nuevas en cada llamada.

**Partida** tiene un constructor principal `(tablero, blancas, negras, reglas, reloj)`. Los cuatro
constructores anteriores se conservan como atajos y delegan en él. Los que reciben detector y
movimientos legales por separado ahora rechazan, con `IllegalArgumentException`, que los
movimientos legales usen otro detector.

**EnsambladorPartida** recibe por constructor un `Sorteo` (puerto nuevo en
`adaptadores.configuracion`) y la `FuenteDeTiempo`. El constructor sin parámetros usa
`SorteoAleatorio` y `FuenteDeTiempoSistema`, así que `Main` y `AplicacionAjedrez` no cambian.
Cada partida se arma con `ReglasDePartida.clasicas()`.

## Por qué

- **Un solo detector.** Partida recibía el detector dos veces: directo y dentro de
  MovimientosLegales, con un comentario que pedía que fueran el mismo. Nada lo controlaba.
  Ahora es imposible armar reglas con dos detectores, y los atajos viejos lo validan.
- **Constructores que no se multiplican.** Cada dependencia opcional nueva duplicaba los
  constructores de Partida (2 → 4 al sumar el reloj). Las próximas reglas (jugadas especiales)
  se agregan dentro de ReglasDePartida sin tocar la firma de Partida.
- **Ensamblado testeable.** El camino que usa la interfaz (`prepararConfigurada`) creaba la fuente
  de tiempo del sistema y llamaba a `ThreadLocalRandom` de forma estática: no se podía probar el
  sorteo ni el reloj del menú sin un programa temporal. Ahora entran por constructor, igual que
  el resto de las dependencias externas, y se prueban con `SorteoFijo` y `FuenteDeTiempoFalsa`.

## Cuándo se rompería

- Si una variante necesitara varios detectores (por ejemplo, un modo con dos reyes por bando que
  se evalúen distinto), habría que volver a separarlos y definir cuál decide el final.
- Los atajos de Partida siguen existiendo para no reescribir los tests actuales. Si se suman muchas
  formas de construir una partida, conviene dejar solo el constructor principal y armar las reglas
  en el ensamblado.
- `ReglasDePartida.clasicas()` fija el ajedrez estándar. El modo fútbol necesitará su propia
  fábrica de reglas, elegida por el ensamblador según el modo del menú.

## Verificación

- 295 tests sin fallos: los 283 anteriores y 12 nuevos (5 de ReglasDePartida, 2 de Partida, 5 del
  ensamblador con sorteo y fuente inyectados).
- Los dos tests de Partida que usaban `StubMovimientosLegales` con un detector distinto del recibido
  se ajustaron para pasar el mismo detector: justamente es la inconsistencia que ahora se rechaza.
- El azar y el reloj del sistema solo aparecen en `SorteoAleatorio`, `FuenteDeTiempoSistema` y el
  constructor por defecto del ensamblador.
