# Fase 6: enroque

## Qué

- `SpecialRule` (Strategy, `core.rules`): una regla que suma jugadas además del movimiento propio
  de cada pieza. Recibe tablero, origen, historial y el detector de la partida.
- `Castling`: el rey va dos casillas hacia una torre y la torre pasa al otro lado, en una `Move` de
  dos traslados. Se juega moviendo el rey (e1 → g1 corto, e1 → c1 largo). Condiciones: rey y torre en
  la fila base y sin moverse; casillas del medio vacías; rey sin jaque y sin pasar ni caer en una
  casilla atacada.
- `LegalMoves` recibe las reglas especiales por constructor y suma variantes con historial;
  las variantes sin historial son `final` para que un doble no sobreescriba un método que Game ya
  no llama. `GameRules.classic()` incluye el enroque.
- `Piece.isRook()`, igual que `isKing()` y `isPawn()`.

## Por qué

- **Sin derechos de enroque guardados.** Si el rey o la torre se movieron se deduce del historial,
  por instancia de pieza. Al deshacer, el derecho vuelve solo.
- **Una jugada más.** El enroque es una `Move` con dos traslados: aplicarlo, deshacerlo, la huella
  de posición y el control de jaque funcionan sin cambios. Game no cambió su lógica.
- **Un solo detector.** La regla recibe el detector de LegalMoves en cada llamada.

## Cuándo se rompería

- En un tablero armado a mano, sin historial, el rey y las torres se consideran sin mover.
- La regla es la del ajedrez clásico generalizada a cualquier tamaño (el rey avanza dos casillas
  hacia la torre). Variantes como Chess960 necesitarían otra regla.
- La repetición no distingue derechos de enroque (simplificación ya documentada).
- Los atajos de Game con detector y condiciones no incluyen reglas especiales; el juego real usa
  `GameRules.classic()`.

## Verificación

- 391 tests: 23 nuevos (15 de Castling, 6 en partidas reales, 2 de LegalMoves).
- Prueba aleatoria de deshacer/rehacer con enroques: 2000 partidas, 164 enroques, 182.483 deshacer
  y 18.645 rehacer, todos consistentes; los estados finales coinciden con partidas rejugadas.
- Siguen siendo 2 los ciclos entre paquetes.
