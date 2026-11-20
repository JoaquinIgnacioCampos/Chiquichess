package core.game;

/** Rechazo de una orden que no es un movimiento de pieza. */
public class InvalidActionException extends RuntimeException {
    public enum Reason {
        FINISHED_GAME,
        NOT_YOUR_TURN,
        OFFER_PENDING,
        WITHOUT_OFFER,
        IS_OWN_OFFER,
        /** La partida no habilita deshacer: no se eligió esa opción o se juega con tiempo. */
        UNDO_NOT_ALLOWED,
        NOTHING_TO_UNDO,
        NOTHING_TO_REDO
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
