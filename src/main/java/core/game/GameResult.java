package core.game;

import core.side.Side;
import core.rules.FinishReason;

import java.util.Objects;
import java.util.Optional;

/** Resultado final inmutable. Sin ganador significa tablas, no partida en curso. */
public record GameResult(Optional<Side> winner, FinishReason reason) {
    public GameResult {
        Objects.requireNonNull(winner, "ganador");
        Objects.requireNonNull(reason, "motivo");
        if (reason.isDraw() == winner.isPresent()) {
            throw new IllegalArgumentException("Las tablas no tienen ganador; una victoria sí");
        }
    }
}
