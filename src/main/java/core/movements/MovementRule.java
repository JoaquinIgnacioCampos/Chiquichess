package core.movements;

import core.side.Side;
import core.board.IBoard;
import core.board.Position;
import java.util.Set;

/**
 * Estrategia de movimiento. Es la pieza clave de la composición: los movimientos
 * base (deslizamiento, salto) y los modificadores (OnlyIfEmpty, OnlyIfCapture, OnlyIf)
 * implementan esta interfaz y se combinan entre sí (Composite + Decorator).
 *
 * Se distinguen dos conceptos porque no siempre coinciden:
 *  - destinationsPossible: a dónde puede ir la pieza (incluye capturas).
 *  - squaresAttacked: qué casillas amenaza. Es lo que se usa para detectar jaque.
 *    Ejemplo: el peón avanza a casillas vacías pero ataca las diagonales.
 */
public interface MovementRule {

    Set<Position> destinationsPossible(IBoard board, Position origin, Side color);

    Set<Position> squaresAttacked(IBoard board, Position origin, Side color);
}
