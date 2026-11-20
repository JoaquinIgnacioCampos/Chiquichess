package core.game;

import core.side.Side;
import core.rules.FinishReason;

import java.util.Objects;
import java.util.Optional;

/** Resultado final inmutable. Sin ganador significa tablas, no partida en curso. */
public record GameResult(Optional<Side> winner, FinishReason reason) {
    public GameResult {
        Objects.requireNonNull(winner, "winner");
        Objects.requireNonNull(reason, "reason");
        if (reason.isDraw() == winner.isPresent()) {
            throw new IllegalArgumentException("A draw has no winner; a victory does");
        }
    }
}
