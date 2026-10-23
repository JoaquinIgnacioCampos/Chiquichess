package Nucleo.reglas;

/** Estado de la partida desde el punto de vista del jugador que tiene el turno. */
public enum EstadoPartida {
    EN_CURSO(false, false),
    JAQUE(false, false),
    JAQUE_MATE(true, false),
    TABLAS_AHOGADO(true, true),
    TABLAS_REPETICION(true, true),
    TABLAS_REGLA_50_MOVIMIENTOS(true, true);

    private final boolean finalizada;
    private final boolean tablas;

    EstadoPartida(boolean finalizada, boolean tablas) {
        this.finalizada = finalizada;
        this.tablas = tablas;
    }

    public boolean estaFinalizada() {
        return finalizada;
    }

    public boolean esTablas() {
        return tablas;
    }
}
