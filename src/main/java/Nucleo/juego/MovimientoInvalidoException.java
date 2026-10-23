package Nucleo.juego;

public class MovimientoInvalidoException extends RuntimeException {

    public enum Motivo {
        SIN_PIEZA_EN_ORIGEN,
        NO_ES_SU_TURNO,
        MOVIMIENTO_NO_PERMITIDO,
        DEJA_AL_REY_EN_JAQUE,
        PARTIDA_FINALIZADA
    }

    private static final long serialVersionUID = 1L;

    private final Motivo motivo;

    public MovimientoInvalidoException(Motivo motivo) {
        super(motivo.name());
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}