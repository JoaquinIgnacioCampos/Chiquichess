package Nucleo.tiempo;

/** Puerto de tiempo monotónico. El origen es arbitrario; solo importan diferencias. */
@FunctionalInterface
public interface FuenteDeTiempo {
    long ahoraEnNanos();
}
