package core.ai;

import core.side.Side;
import core.game.GameSituation;
import core.moves.Move;
import core.movements.CompositeRule;
import core.movements.BaseRules;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.pieces.Queen;
import core.pieces.King;
import core.rules.GameRules;
import core.board.Board;
import doubles.FontOfTimeFalse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El material lo declara cada pieza: una pieza que no existe en el ajedrez clásico cuenta para la IA
 * con su propio valor, sin tocar el evaluador.
 */
class PositionEvaluatorTest {

    /** Pieza inventada para la prueba: se mueve como el rey y vale 700. */
    private static final class Elephant extends Piece {
        Elephant(Side color) {
            super(color, new CompositeRule(BaseRules.orthogonal(1), BaseRules.diagonals(1)));
        }

        @Override
        public String displayName() {
            return "Elefante";
        }

        @Override
        public char code() {
            return 'E';
        }

        @Override
        public int value() {
            return 700;
        }
    }

    private static Board kingsIn(String white, String black) {
        Board board = new Board(8, 8);
        board.place(pos(white), new King(Side.WHITE));
        board.place(pos(black), new King(Side.BLACK));
        return board;
    }

    @Test
    void materialIsValueThatDeclaresEachPiece() {
        // Arrange: solo los reyes (que no cuentan) y una pieza nueva, que vale más que una pieza menor
        Board board = kingsIn("a1", "h8");
        board.place(pos("d4"), new Elephant(Side.WHITE));

        // Act
        int forWhite = new PositionEvaluator(board).evaluate(board, Side.WHITE, List.of());

        // Assert: 700 y nada de bonificación por el centro, que es solo para piezas menores y peones
        assertEquals(700, forWhite);
    }

    @Test
    void aiPrefersCaptureNewPieceIfIsMore() {
        // Arrange: la dama negra puede comer un peón (d2) o el elefante (g5), los dos sin defensa
        Board board = kingsIn("a1", "h8");
        board.place(pos("d5"), new Queen(Side.BLACK));
        board.place(pos("d2"), new Pawn(Side.WHITE));
        board.place(pos("g5"), new Elephant(Side.WHITE));
        GameSituation situation = new GameSituation(board, Side.BLACK, List.of(),
                GameRules.classic());
        // Fácil mira una sola jugada: compara directamente lo que gana con cada captura. (Medio encuentra
        // algo mejor: Dama e5 da jaque y ataca al elefante a la vez.) El azar nunca lo distrae.
        Random withoutOversights = new Random(1) {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
        ArtificialPlayer ia = new ArtificialPlayer(Difficulty.FACIL, withoutOversights, new FontOfTimeFalse());

        // Act
        Move chosen = ia.chooseMove(situation);

        // Assert
        assertEquals(pos("g5"), chosen.destination());
    }
}
