package adaptadores;

import Nucleo.tiempo.FuenteDeTiempo;

/** Tiempo del sistema monotónico: no depende de cambios de fecha u hora civil. */
public class FuenteDeTiempoSistema implements FuenteDeTiempo {
    @Override
    public long ahoraEnNanos() {
        return System.nanoTime();
    }
}
