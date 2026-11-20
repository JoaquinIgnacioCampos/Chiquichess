package doubles;

import core.side.Side;
import core.movements.MovementRule;
import core.board.IBoard;
import core.board.Position;

import java.util.Set;

/** Mock: cuenta cuántas veces se le pidieron destinos y casillas atacadas. Devuelve un conjunto fijo. */
public class MockRuleMovement implements MovementRule {

    private final Set<Position> response;
    private int callsDestinations = 0;
    private int callsAttacked = 0;

    public MockRuleMovement(Set<Position> response) {
        this.response = Set.copyOf(response);
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        callsDestinations++;
        return response;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        callsAttacked++;
        return response;
    }

    public int callsDestinations() {
        return callsDestinations;
    }

    public int callsAttacked() {
        return callsAttacked;
    }
}
