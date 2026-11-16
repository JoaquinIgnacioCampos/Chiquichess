package Nucleo.reglas;

import java.util.Optional;

/** Estado de la partida desde el punto de vista del jugador que tiene el turno. */
public enum EstadoPartida {
    EN_CURSO(null),
    JAQUE(null),
    JAQUE_MATE(MotivoFinalizacion.JAQUE_MATE),
    TABLAS_AHOGADO(MotivoFinalizacion.AHOGADO),
    TABLAS_REPETICION(MotivoFinalizacion.REPETICION),
    TABLAS_REGLA_50_MOVIMIENTOS(MotivoFinalizacion.REGLA_50_MOVIMIENTOS),
    RENDICION(MotivoFinalizacion.RENDICION),
    TIEMPO_AGOTADO(MotivoFinalizacion.TIEMPO_AGOTADO),
    TABLAS_ACUERDO(MotivoFinalizacion.ACUERDO),
    VICTORIA_POR_GOLES(MotivoFinalizacion.GOLES),
    TABLAS_EMPATE_EN_GOLES(MotivoFinalizacion.EMPATE_EN_GOLES);

    private final MotivoFinalizacion motivo;

    EstadoPartida(MotivoFinalizacion motivo) {
        this.motivo = motivo;
    }

    public boolean estaFinalizada() {
        return motivo != null;
    }

    public boolean esTablas() {
        return motivo != null && motivo.esTablas();
    }

    public Optional<MotivoFinalizacion> motivo() {
        return Optional.ofNullable(motivo);
    }
}
