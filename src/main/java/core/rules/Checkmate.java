package core.rules;

import java.util.Optional;

/** Jaque mate: el jugador con el turno está en jaque y no tiene ninguna jugada legal. */
public class Checkmate implements EndCondition {

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        return context.inCheck() && !context.hasMovementsLegal()
                ? Optional.of(GameState.CHECK_MATE)
                : Optional.empty();
    }
}
