package core.ai;

import core.moves.Move;

import java.time.Duration;
import java.util.Objects;

/**
 * Lo que decidió una IA: la jugada y cuánto tardaría en jugarla una persona de su nivel, contando desde
 * que empezó a pensar. Quien la hace jugar espera hasta ese momento para mover.
 */
public record GameDecision(Move move, Duration responseTime) {
    public GameDecision {
        Objects.requireNonNull(move, "jugada");
        Objects.requireNonNull(responseTime, "tiempoDeRespuesta");
        if (responseTime.isNegative()) throw new IllegalArgumentException("El tiempo no puede ser negativo");
    }
}
