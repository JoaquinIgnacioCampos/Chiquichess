# Fase 7b: promoción del peón (núcleo)

## Qué

- `Replacement(previous, newValue, position)`: tercer efecto de la interfaz sellada `Effect`. Cambia la
  pieza de una casilla por otra y, al revertir, vuelve la anterior. `PositionFingerprint` lo incluye (el
  compilador lo exigió por ser una interfaz sellada).
- `Promotion` (`SpecialRule`): no suma jugadas; **transforma** cada jugada de un peón que termina en
  la última fila (avance o captura) en una variante por cada pieza posible: dama, torre, alfil y caballo.
  Para eso `SpecialRule` suma el método `transform`, que por defecto deja la jugada igual, y
  `LegalMoves` lo aplica a todas las jugadas.
- `Move.promotion()` dice en qué pieza se convirtió; `Move.con(effect)` agrega un efecto.
- `MoveRequest` suma `promotion`: el código de la pieza elegida (`D`, `T`, `A`, `C`). El constructor de
  dos parámetros sigue existiendo. `MoveRequest.withPromotion(origin, destination, code)` lo arma.
- `Game` elige la jugada según la promoción pedida; sin elección, promueve a dama. El historial
  incluye el código de promoción, así una partida se puede reproducir exactamente.
- `GameRules.classic()` incluye la promoción.

## Por qué

- **Una jugada más.** La promoción es `[Removal?, Transfer del peón, Replacement]`: deshacerla, la huella,
  el control de jaque y las tablas funcionan sin cambios. La pieza movida sigue siendo el peón, así que
  la regla de 50 movimientos y el corte de la repetición la tratan como un avance de peón.
- **Transformar en lugar de sumar.** Si la promoción sumara jugadas, el avance común del peón a la
  última fila seguiría siendo legal y el peón podría quedarse como peón. Transformar reemplaza ese avance.
- **Opciones configurables.** Las piezas posibles se reciben por constructor; la primera es la elección
  por defecto. Una variante puede limitar o cambiar las opciones sin tocar la regla.
- **Dama por defecto.** Así la interfaz y la consola actuales ya promueven sin cambios; el selector de
  pieza llega en la fase 8.

## Cuándo se rompería

- La elección se identifica por el código de la pieza (`Piece.code()`). Dos opciones con el mismo
  código serían indistinguibles.
- Si una variante permitiera que el peón se quede como peón en la última fila, habría que incluir esa
  opción explícitamente.

## Verificación

- 419 tests: 16 nuevos (3 de `Replacement`, 6 de `Promotion`, 7 en partidas reales: dama por defecto,
  elegir otra pieza, promover capturando, opción inválida, promoción pedida sin promover, jaque de la
  pieza nueva y deshacer). Se renombró el test del peón en la última fila, que ya no describe una
  ausencia de la regla.
- Fuera del repo: la huella incremental coincidió con la calculada desde cero en 550.258 jugadas con
  4465 promociones; 2000 partidas con deshacer/rehacer coincidieron con su repetición desde el historial.
