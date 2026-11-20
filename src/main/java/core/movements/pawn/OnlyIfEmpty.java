package core.movements.pawn;

import core.side.Side;
import core.movements.MovementRule;
import core.board.IBoard;
import core.board.Position;
import java.util.HashSet;
import java.util.Set;

/**
 * Decorator: restringe los destinos a casillas libres (no permite capturar).
 * Un avance no es un ataque, por eso no aporta casillas atacadas.
 */
public class OnlyIfEmpty implements MovementRule {

    private final MovementRule regla;

    public OnlyIfEmpty(MovementRule regla) {
        this.regla = regla;
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        Set<Position> destinations = new HashSet<>(regla.destinationsPossible(board, origin, color));
        destinations.removeIf(destination -> board.pieceIn(destination).isPresent());
        return destinations;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        return Set.of();
    }
}
