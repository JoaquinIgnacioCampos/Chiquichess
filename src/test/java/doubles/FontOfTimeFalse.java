package doubles;

import core.time.TimeSource;
import java.time.Duration;

/** Tiempo monotónico controlado por el test, sin esperas reales. */
public class FontOfTimeFalse implements TimeSource {
    private long nanos;

    @Override
    public long nowInNanos() {
        return nanos;
    }

    public void advance(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("El tiempo no puede retroceder");
        }
        nanos = Math.addExact(nanos, duration.toNanos());
    }
}
