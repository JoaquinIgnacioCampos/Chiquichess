package core.movements.pawn;

import core.side.Side;
import core.movements.MovementRule;
import core.board.IBoard;
import core.board.Position;
import java.util.Set;

/** Decorator: la regla interna solo aplica si se cumple una condición (ej. el peón está en su fila inicial). */
public class OnlyIf implements MovementRule {

    @FunctionalInterface
    public interface Condition {
        boolean meets(IBoard board, Position origin, Side color);
    }

    private final Condition condition;
    private final MovementRule regla;

    public OnlyIf(Condition condition, MovementRule regla) {
        this.condition = condition;
        this.regla = regla;
    }

    @Override
    public Set<Position> destinationsPossible(IBoard board, Position origin, Side color) {
        return condition.meets(board, origin, color)
                ? regla.destinationsPossible(board, origin, color)
                : Set.of();
    }

    @Override
    public Set<Position> squaresAttacked(IBoard board, Position origin, Side color) {
        return condition.meets(board, origin, color)
                ? regla.squaresAttacked(board, origin, color)
                : Set.of();
    }
}
