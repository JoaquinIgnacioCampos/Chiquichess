package Nucleo.reglas;

/** La causa del final es independiente del color ganador. */
public enum MotivoFinalizacion {
    JAQUE_MATE(false),
    AHOGADO(true),
    REPETICION(true),
    REGLA_50_MOVIMIENTOS(true),
    RENDICION(false),
    TIEMPO_AGOTADO(false),
    ACUERDO(true),
    /** Modo fútbol: llegó a los goles para ganar, o tenía más goles al llegar al límite de jugadas. */
    GOLES(false),
    /** Modo fútbol: se llegó al límite de jugadas con los goles iguales. */
    EMPATE_EN_GOLES(true);

    private final boolean tablas;

    MotivoFinalizacion(boolean tablas) {
        this.tablas = tablas;
    }

    public boolean esTablas() {
        return tablas;
    }
}
