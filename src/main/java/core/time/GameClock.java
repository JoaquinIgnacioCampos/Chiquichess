package core.time;

import core.side.Side;

import java.time.Duration;
import java.util.Optional;

/** Reloj de una sola partida. Cada orden y consulta se ejecuta secuencialmente. */
public interface GameClock {
    void start(Side color);
    void changeTurn();
    void stop();

    /** Vacío indica que no existe límite de tiempo. */
    Optional<Duration> timeRemaining(Side color);

    default boolean timeExhausted(Side color) {
        return timeRemaining(color).map(time -> time.isZero()).orElse(false);
    }
}
