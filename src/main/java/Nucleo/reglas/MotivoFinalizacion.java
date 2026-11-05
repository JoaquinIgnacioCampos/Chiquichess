package Nucleo.reglas;

/** La causa del final es independiente del color ganador. */
public enum MotivoFinalizacion {
    JAQUE_MATE(false),
    AHOGADO(true),
    REPETICION(true),
    REGLA_50_MOVIMIENTOS(true),
    RENDICION(false),
    TIEMPO_AGOTADO(false),
    ACUERDO(true);

    private final boolean tablas;

    MotivoFinalizacion(boolean tablas) {
        this.tablas = tablas;
    }

    public boolean esTablas() {
        return tablas;
    }
}
