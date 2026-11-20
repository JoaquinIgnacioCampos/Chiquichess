package doubles;

import core.side.Side;
import core.moves.Move;
import core.rules.ICheckDetector;
import core.rules.LegalMoves;
import core.board.Board;

import java.util.List;

/**
 * Stub: fija la respuesta de "¿existe alguna jugada legal?" sin recorrer el tablero.
 * El resto del comportamiento es el de MovimientosLegales con el detector indicado.
 */
public class StubMovementsLegal extends LegalMoves {

    private final boolean existsSome;

    /** Usa un detector que nunca ve jaque. */
    public StubMovementsLegal(boolean existsSome) {
        this(existsSome, StubDetectorOfCheck.neverInCheck());
    }

    /** Partida exige que el detector sea el mismo que recibe ella: pasar acá esa misma instancia. */
    public StubMovementsLegal(boolean existsSome, ICheckDetector detector) {
        super(detector);
        this.existsSome = existsSome;
    }

    @Override
    public boolean existsSome(Board board, Side color, List<Move> history) {
        return existsSome;
    }
}
