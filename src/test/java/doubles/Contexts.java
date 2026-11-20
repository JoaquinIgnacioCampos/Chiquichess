package doubles;

import core.side.Side;
import core.moves.Move;
import core.rules.MoveContext;
import core.board.IBoard;
import core.board.Board;

import java.util.List;

/** Atajos para armar un MoveContext en los tests de las condiciones de finalización. */
public final class Contexts {

    private Contexts() {
    }

    /** Contexto en el que solo importan los dos booleanos (para jaque mate y ahogado). */
    public static MoveContext con(boolean inCheck, boolean hasMovementsLegal) {
        return new MoveContext(BoardFake.empty(), Side.WHITE, List.of(),
                inCheck, hasMovementsLegal);
    }

    /** Contexto de la evaluación inicial (todavía no se jugó nada). */
    public static MoveContext initial(IBoard board, Side turn) {
        return withHistory(board, turn, List.of());
    }

    /** Contexto con un historial dado; las jugadas no se aplican, solo se informan. */
    public static MoveContext withHistory(IBoard board, Side turn, List<Move> moves) {
        return new MoveContext(board, turn, moves, false, true);
    }

    /**
     * Juega la secuencia sobre el tablero (empezando las blancas) y devuelve el contexto que
     * recibirían las condiciones después de la última jugada.
     */
    public static MoveContext afterPlay(Board board, String... moves) {
        List<Move> executed = MovesOfTest.play(board, moves);
        Side turn = executed.size() % 2 == 0 ? Side.WHITE : Side.BLACK;
        return withHistory(board, turn, executed);
    }
}
