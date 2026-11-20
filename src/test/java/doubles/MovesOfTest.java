package doubles;

import core.moves.Move;
import core.board.Board;

import java.util.ArrayList;
import java.util.List;

import static doubles.Squares.pos;

/** Aplica una secuencia de jugadas comunes ("e2e4") sobre un tablero y devuelve las jugadas ejecutadas. */
public final class MovesOfTest {

    private MovesOfTest() {
    }

    public static List<Move> play(Board board, String... moves) {
        List<Move> executed = new ArrayList<>();
        for (String texto : moves) {
            Move move = Move.normal(board, pos(texto.substring(0, 2)), pos(texto.substring(2, 4)));
            move.apply(board);
            executed.add(move);
        }
        return executed;
    }
}
