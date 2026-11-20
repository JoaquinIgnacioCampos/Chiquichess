package core.movements.pawn;

import core.side.Side;
import core.movements.MovementRule;
import core.board.IBoard;
import core.board.Position;
import java.util.HashSet;
import java.util.Set;

/**
 * Decorator: restringe los destinos a casillas con una pieza rival (solo se mueve si captura).
 * Las casillas atacadas son todas las que la regla interna alcanzaría, haya o no una pieza:
 * así el peón amenaza sus diagonales aunque estén vacías.
 */
public class OnlyIfCapture implements MovementRule {

    private final MovementRule regla;

    public OnlyIfCapture(MovementRule regla) {
        this.regla = regla;
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        Set<Position> destinations = new HashSet<>(regla.destinationsPossible(board, origin, color));
        destinations.removeIf(destination -> board.pieceIn(destination).isEmpty());
        return destinations;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        return regla.squaresAttacked(board, origin, color);
    }
}
