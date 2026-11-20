package doubles;

import core.side.Side;
import core.moves.Move;
import core.rules.StatefulCondition;
import core.rules.MoveContext;
import core.rules.Memento;
import core.board.Board;

import java.util.ArrayList;
import java.util.List;

/**
 * Hace con una condición con memoria lo mismo que Partida: registra la posición inicial y después
 * cada jugada. Las jugadas se aplican de verdad sobre el tablero (empiezan las blancas).
 */
public final class RecordOfMoves {

    /** Recuerdos en orden (el primero es el de la posición inicial) y el contexto de la última posición. */
    public record Result(List<Memento> mementos, MoveContext lastContext) {
    }

    private RecordOfMoves() {
    }

    public static Result register(StatefulCondition condition, Board board, String... moves) {
        List<Memento> mementos = new ArrayList<>();
        MoveContext context = Contexts.initial(board, Side.WHITE);
        mementos.add(condition.register(context));
        List<Move> made = new ArrayList<>();
        for (String move : moves) {
            made.addAll(MovesOfTest.play(board, move));
            Side turn = made.size() % 2 == 0 ? Side.WHITE : Side.BLACK;
            context = Contexts.withHistory(board, turn, made);
            mementos.add(condition.register(context));
        }
        return new Result(mementos, context);
    }

    /** Registra jugadas armadas a mano, sin aplicarlas (para condiciones que solo miran el historial). */
    public static Result registerWithoutApply(StatefulCondition condition, Board board, List<Move> moves) {
        List<Memento> mementos = new ArrayList<>();
        MoveContext context = Contexts.initial(board, Side.WHITE);
        mementos.add(condition.register(context));
        for (int i = 1; i <= moves.size(); i++) {
            Side turn = i % 2 == 0 ? Side.WHITE : Side.BLACK;
            context = Contexts.withHistory(board, turn, moves.subList(0, i));
            mementos.add(condition.register(context));
        }
        return new Result(mementos, context);
    }
}
