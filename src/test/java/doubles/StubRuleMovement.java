package doubles;

import core.side.Side;
import core.movements.MovementRule;
import core.board.IBoard;
import core.board.Position;

import java.util.Set;

/** Stub: devuelve siempre los mismos destinos y casillas atacadas, sin mirar el tablero. */
public class StubRuleMovement implements MovementRule {

    private final Set<Position> destinations;
    private final Set<Position> attacked;

    public StubRuleMovement(Set<Position> destinations, Set<Position> attacked) {
        this.destinations = Set.copyOf(destinations);
        this.attacked = Set.copyOf(attacked);
    }

    /** Variante en la que ataca las mismas casillas a las que puede ir. */
    public StubRuleMovement(Set<Position> destinations) {
        this(destinations, destinations);
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        return destinations;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        return attacked;
    }
}
