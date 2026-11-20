package adapters;

import core.time.TimeSource;

/** Tiempo del sistema monotónico: no depende de cambios de fecha u hora civil. */
public class SystemTimeSource implements TimeSource {
    @Override
    public long nowInNanos() {
        return System.nanoTime();
    }
}
