package core.rules;

/** La causa del final es independiente del color ganador. */
public enum FinishReason {
    CHECK_MATE(false),
    STALEMATE(true),
    REPETITION(true),
    RULE_50_MOVEMENTS(true),
    RESIGNATION(false),
    TIME_EXHAUSTED(false),
    AGREEMENT(true),
    /** Modo fútbol: llegó a los goles para ganar, o tenía más goles al llegar al límite de jugadas. */
    GOALS(false),
    /** Modo fútbol: se llegó al límite de jugadas con los goles iguales. */
    TIED_ON_GOALS(true);

    private final boolean draw;

    FinishReason(boolean draw) {
        this.draw = draw;
    }

    public boolean isDraw() {
        return draw;
    }
}
