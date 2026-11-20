package core.game;

public class InvalidMoveException extends RuntimeException {

    public enum Reason {
        SIN_PIEZA_EN_ORIGEN,
        NO_ES_SU_TURNO,
        MOVIMIENTO_NO_PERMITIDO,
        DEJA_AL_REY_EN_JAQUE,
        PARTIDA_FINALIZADA
    }

    private static final long serialVersionUID = 1L;

    private final Reason reason;

    public InvalidMoveException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}