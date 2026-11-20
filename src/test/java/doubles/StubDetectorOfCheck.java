package doubles;

import core.side.Side;
import core.rules.ICheckDetector;
import core.board.IBoard;

/** Stub: responde siempre "en jaque" o siempre "sin jaque", sin analizar el tablero. */
public class StubDetectorOfCheck implements ICheckDetector {

    private final boolean response;

    private StubDetectorOfCheck(boolean response) {
        this.response = response;
    }

    public static StubDetectorOfCheck alwaysInCheck() {
        return new StubDetectorOfCheck(true);
    }

    public static StubDetectorOfCheck neverInCheck() {
        return new StubDetectorOfCheck(false);
    }

    @Override
    public boolean isInCheck(IBoard board, Side color) {
        return response;
    }
}
