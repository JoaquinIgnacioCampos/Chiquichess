package core.time;

import core.side.Side;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Null Object: partida sin límite; evita condicionales por ausencia de reloj. */
public class NoClock implements GameClock {
    @Override
    public void start(Side color) {
        Objects.requireNonNull(color, "color");
    }

    @Override
    public void changeTurn() {
    }

    @Override
    public void stop() {
    }

    @Override
    public Optional<Duration> timeRemaining(Side color) {
        Objects.requireNonNull(color, "color");
        return Optional.empty();
    }
}
