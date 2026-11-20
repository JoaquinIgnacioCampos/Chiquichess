package core.game;

/** Rechazo de una orden que no es un movimiento de pieza. */
public class InvalidActionException extends RuntimeException {
    public enum Reason {
        PARTIDA_FINALIZADA,
        NO_ES_SU_TURNO,
        OFERTA_PENDIENTE,
        SIN_OFERTA,
        ES_SU_PROPIA_OFERTA,
        /** La partida no habilita deshacer: no se eligió esa opción o se juega con tiempo. */
        DESHACER_NO_PERMITIDO,
        NADA_PARA_DESHACER,
        NADA_PARA_REHACER
    }

    private static final long serialVersionUID = 1L;
    private final Reason reason;

    public InvalidActionException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
