package core.rules;

import java.util.Optional;

/** Estado de la partida desde el punto de vista del jugador que tiene el turno. */
public enum GameState {
    IN_PROGRESS(null),
    CHECK(null),
    CHECK_MATE(FinishReason.CHECK_MATE),
    DRAW_STALEMATE(FinishReason.STALEMATE),
    DRAW_REPETITION(FinishReason.REPETITION),
    DRAW_RULE_50_MOVEMENTS(FinishReason.RULE_50_MOVEMENTS),
    RESIGNATION(FinishReason.RESIGNATION),
    TIME_EXHAUSTED(FinishReason.TIME_EXHAUSTED),
    DRAW_AGREEMENT(FinishReason.AGREEMENT),
    VICTORY_BY_GOALS(FinishReason.GOALS),
    DRAW_TIED_ON_GOALS(FinishReason.TIED_ON_GOALS);

    private final FinishReason reason;

    GameState(FinishReason reason) {
        this.reason = reason;
    }

    public boolean isFinished() {
        return reason != null;
    }

    public boolean isDraw() {
        return reason != null && reason.isDraw();
    }

    public Optional<FinishReason> reason() {
        return Optional.ofNullable(reason);
    }
}
