package dobles;

import Nucleo.tiempo.FuenteDeTiempo;
import java.time.Duration;

/** Tiempo monotónico controlado por el test, sin esperas reales. */
public class FuenteDeTiempoFalsa implements FuenteDeTiempo {
    private long nanos;

    @Override
    public long ahoraEnNanos() {
        return nanos;
    }

    public void avanzar(Duration duracion) {
        if (duracion.isNegative()) {
            throw new IllegalArgumentException("El tiempo no puede retroceder");
        }
        nanos = Math.addExact(nanos, duracion.toNanos());
    }
}
