package core.movements;

import core.side.Side;
import core.board.IBoard;
import core.board.Position;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Composite: une los destinos y ataques de varias reglas (ej. reina = torre + alfil). */
public class CompositeRule implements MovementRule {

    private final List<MovementRule> rules;

    public CompositeRule(MovementRule... rules) {
        this.rules = List.copyOf(Arrays.asList(rules));
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        Set<Position> result = new HashSet<>();
        for (MovementRule regla : rules) {
            result.addAll(regla.destinationsPossible(board, origin, color));
        }
        return result;
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        Set<Position> result = new HashSet<>();
        for (MovementRule regla : rules) {
            result.addAll(regla.squaresAttacked(board, origin, color));
        }
        return result;
    }
}
