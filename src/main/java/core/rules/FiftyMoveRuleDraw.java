package core.rules;

import core.moves.Move;

import java.util.Optional;

/**
 * Tablas si pasan 50 movimientos de cada jugador sin ninguna captura ni movimiento de peón.
 *
 * Lleva un contador de jugadas seguidas sin progreso. Cada registro devuelve como recuerdo el valor
 * anterior del contador, así deshacer una jugada lo deja exactamente como estaba, incluso si esa
 * jugada lo había vuelto a cero.
 */
public class FiftyMoveRuleDraw implements StatefulCondition {

    /** Memento: el valor del contador antes del registro. */
    private record CounterPrevious(FiftyMoveRuleDraw owner, int value) implements Memento {
    }

    private final int looseMoveLimit;
    private int movesWithoutProgress = 0;

    public FiftyMoveRuleDraw() {
        this(50);
    }

    public FiftyMoveRuleDraw(int movesPerPlayer) {
        if (movesPerPlayer < 1) {
            throw new IllegalArgumentException("It must be at least 1 move");
        }
        this.looseMoveLimit = movesPerPlayer * 2;
    }

    @Override
    public Memento register(MoveContext context) {
        Memento previous = new CounterPrevious(this, movesWithoutProgress);
        Optional<Move> last = context.lastMove();
        if (last.isEmpty()) {
            movesWithoutProgress = 0; // posición inicial: todavía no hubo ninguna jugada
        } else if (last.get().captured().isPresent()
                || (!last.get().isPass() && last.get().pieceMoved().isPawn())) {
            // Captura o avance de peón. Un pase de un peón no cuenta: el peón no se movió.
            movesWithoutProgress = 0;
        } else {
            movesWithoutProgress++;
        }
        return previous;
    }

    @Override
    public void restore(Memento memento) {
        if (!(memento instanceof CounterPrevious previous) || previous.owner() != this) {
            throw new IllegalArgumentException("The memento does not belong to this condition");
        }
        movesWithoutProgress = previous.value();
    }

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        return movesWithoutProgress >= looseMoveLimit
                ? Optional.of(GameState.DRAW_RULE_50_MOVEMENTS)
                : Optional.empty();
    }
}
