package core.moves;

import core.board.Board;

/**
 * Un cambio elemental sobre el tablero que se puede aplicar y revertir exactamente.
 * Una jugada es una lista de efectos: así una captura común, un enroque (dos traslados), una
 * captura al paso (un retiro en otra casilla) o una promoción (un traslado y un reemplazo) se
 * representan con las mismas tres piezas. El modo fútbol suma dos efectos: mover la pelota y el pase.
 *
 * Cada efecto verifica que el tablero esté en el estado esperado antes de tocarlo y, si no lo está,
 * lanza IllegalStateException sin modificar nada.
 */
public sealed interface Effect permits Transfer, Removal, Replacement, MoveBall, Pass {

    void apply(Board board);

    void revert(Board board);
}
