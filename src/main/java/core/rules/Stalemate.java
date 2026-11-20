package core.rules;

import java.util.Optional;

/** Ahogado (tablas): el jugador con el turno NO está en jaque pero no tiene ninguna jugada legal. */
public class Stalemate implements EndCondition {

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        return !context.inCheck() && !context.hasMovementsLegal()
                ? Optional.of(GameState.DRAW_STALEMATE)
                : Optional.empty();
    }
}
