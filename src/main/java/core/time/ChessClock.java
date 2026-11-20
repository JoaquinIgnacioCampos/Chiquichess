package core.time;

import core.side.Side;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Descuenta tiempo por consultas y cambios de turno, sin ejecutar tareas propias. */
public class ChessClock implements GameClock {
    private final TimeSource font;
    private final Map<Side, Duration> remainingTimes = new EnumMap<>(Side.class);
    private boolean started;
    private Side activo;
    private long turnStart;

    public ChessClock(Duration whiteTime, Duration blackTime, TimeSource font) {
        this.font = Objects.requireNonNull(font, "font");
        remainingTimes.put(Side.WHITE, validateDuration(whiteTime));
        remainingTimes.put(Side.BLACK, validateDuration(blackTime));
    }

    private static Duration validateDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("The initial time must be positive");
        }
        return duration;
    }

    @Override
    public void start(Side color) {
        Objects.requireNonNull(color, "color");
        if (started) {
            throw new IllegalStateException("The clock already belongs to a started game");
        }
        turnStart = font.nowInNanos();
        activo = color;
        started = true;
    }

    @Override
    public void changeTurn() {
        if (activo == null) {
            throw new IllegalStateException("The clock is not running");
        }
        long now = font.nowInNanos();
        remainingTimes.put(activo, remainingActive(now));
        activo = activo.opposite();
        turnStart = now;
    }

    @Override
    public void stop() {
        if (activo != null) {
            remainingTimes.put(activo, remainingActive(font.nowInNanos()));
            activo = null;
        }
    }

    @Override
    public Optional<Duration> timeRemaining(Side color) {
        Objects.requireNonNull(color, "color");
        return Optional.of(color == activo
                ? remainingActive(font.nowInNanos())
                : remainingTimes.get(color));
    }

    private Duration remainingActive(long now) {
        Duration remaining = remainingTimes.get(activo).minusNanos(now - turnStart);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
