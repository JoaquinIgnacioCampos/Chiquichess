package core.game;

public class InvalidMoveException extends RuntimeException {

    public enum Reason {
        NO_PIECE_AT_ORIGIN,
        NOT_YOUR_TURN,
        MOVE_NOT_ALLOWED,
        LEAVES_KING_IN_CHECK,
        FINISHED_GAME
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