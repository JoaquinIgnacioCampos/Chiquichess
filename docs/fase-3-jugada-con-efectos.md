# Fase 3: la jugada como Command

## Qué

Paquete nuevo `Nucleo.jugadas`, que depende solo de `tablero` y `piezas`:

- `Efecto` es una interfaz sellada con dos casos: `Traslado(pieza, origen, destino)` lleva una pieza
  a una casilla vacía y `Retiro(pieza, posicion)` la saca del tablero. Cada efecto sabe aplicarse y
  revertirse, y antes de tocar el tablero verifica que esté en el estado esperado (la pieza correcta
  en su casilla, el destino vacío). Si no lo está, lanza `IllegalStateException` sin modificar nada.
- `Jugada(origen, destino, efectos)` es el Command: la intención del jugador más la lista de efectos.
  `Jugada.normal(tablero, origen, destino)` arma la jugada común: `[Retiro de la pieza rival?, Traslado]`.
  `aplicar` es atómico: si un efecto falla a mitad de camino, revierte los anteriores.
  `piezaMovida()` y `capturada()` se deducen de los efectos.

`MovimientosLegales` ahora genera jugadas (`jugadasPosibles`, `jugadaPosible`) y el control de jaque
simula la jugada completa (`dejaAlReyEnJaque(tablero, jugada)`). Los métodos anteriores (`destinos`,
`existeAlguno` y `dejaAlReyEnJaque` con pieza y casillas) se conservan y usan las jugadas por dentro.

`Partida.mover` resuelve la jugada, la valida, la aplica y la guarda. `historial()` sigue devolviendo
`List<Movimiento>` (origen y destino de cada jugada), así que la consola y la interfaz no cambian.

## Por qué

- **Una sola forma de representar jugadas.** Una captura común, un enroque (dos traslados) y una
  captura al paso (un retiro en otra casilla que el destino) son la misma clase con otros efectos.
  Agregar esas jugadas especiales en las fases 6 y 7 no va a requerir cambiar `Jugada` ni `Partida`.
- **Deshacer sin reconstruir nada.** El Command guarda todo lo necesario para revertirse, incluida la
  pieza capturada. Es la base del deshacer/rehacer de la fase 5.
- **Validación correcta para cualquier jugada.** Antes, la simulación de jaque asumía "una pieza va
  de origen a destino". Ahora simula exactamente los efectos que se van a aplicar.
- **Efectos que se controlan solos.** Si alguna vez se intenta revertir una jugada sobre un tablero
  que no corresponde, falla enseguida en lugar de dejar el tablero corrupto.
- **Sin ciclos nuevos.** `Jugada` guarda posiciones y no un `Movimiento`, porque `Movimiento` vive en
  `juego`, que depende de `jugadas`. Los ciclos entre paquetes siguen siendo los dos del dominio.

## Cuándo se rompería

- Si una jugada necesitara cambiar una pieza por otra (la promoción del peón), haría falta un tercer
  efecto (por ejemplo `Reemplazo`). La interfaz sellada obliga a declararlo explícitamente.
- `Jugada` deduce la pieza movida del traslado que sale del origen. Una jugada sin ese traslado se
  rechaza al construirla; si una variante necesitara jugadas que no mueven ninguna pieza (pasar el
  turno, por ejemplo), habría que revisar esa regla.
- La pieza capturada es el primer `Retiro`. Una jugada que capturara dos piezas necesitaría que
  `capturada()` devuelva una lista.

## Verificación

- 318 tests sin fallos: los 295 anteriores y 23 nuevos (5 de Traslado, 4 de Retiro, 10 de Jugada y
  4 de MovimientosLegales).
- Los tests de `Jugada` siguen el criterio del Command: el tablero después de `revertir` es idéntico
  al de antes de `aplicar`, incluyendo una jugada de dos traslados (como un enroque) y una captura en
  otra casilla (como la captura al paso).
- Los 295 tests anteriores pasaron sin cambios antes de escribir los nuevos. El único ajuste fue en
  `PartidaConRelojTest`: un doble que simula una validación lenta sobreescribía la variante vieja de
  `dejaAlReyEnJaque`; ahora sobreescribe la que usa `Partida`, con la misma intención.
- El análisis de dependencias sigue dando 2 ciclos entre paquetes.
