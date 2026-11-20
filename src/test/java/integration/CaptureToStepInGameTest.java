package integration;

import core.side.Side;
import core.game.StandardLayout;
import core.game.Player;
import core.game.InvalidMoveException;
import core.game.Game;
import core.game.MoveResult;
import core.pieces.Piece;
import core.rules.GameRules;
import core.board.Board;
import core.time.NoClock;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La captura al paso jugada en partidas reales, con las reglas clásicas y el deshacer habilitado. */
class CaptureToStepInGameTest {

    private Game game(Board board) {
        return new Game(board, new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.classic().withUndo(), new NoClock());
    }

    private Game fromStart() {
        return game(new StandardLayout().createBoard());
    }

    private MoveResult play(Game game, String... moves) {
        MoveResult last = null;
        for (String move : moves) {
            last = game.move(move(move.substring(0, 2), move.substring(2, 4)));
        }
        return last;
    }

    /** El peón blanco llega a e5 y el negro de d7 avanza dos casillas, quedando a su lado. */
    private static final String[] BLACK_PAWN_TO_SIDE = {"e2e4", "a7a6", "e4e5", "d7d5"};

    @Test
    void pawnCaptureToStepAndRivalLeavesOfItsSquare() {
        // Arrange
        Game game = fromStart();
        play(game, BLACK_PAWN_TO_SIDE);
        Piece blackPawn = game.board().pieceIn(pos("d5")).orElseThrow();
        assertTrue(game.movementsLegal(pos("e5")).contains(pos("d6")));

        // Act
        MoveResult result = play(game, "e5d6");

        // Assert
        assertSame(blackPawn, result.capturedPiece().orElseThrow());
        assertTrue(game.board().pieceIn(pos("d5")).isEmpty());
        assertTrue(game.board().pieceIn(pos("d6")).orElseThrow().isPawn());
        assertEquals(31, game.board().pieces().size());
        assertEquals(move("e5", "d6"), game.history().getLast());
    }

    @Test
    void ifNotTakesAdvantageRightLoses() {
        // Arrange: las dos partes juegan otra cosa
        Game game = fromStart();
        play(game, BLACK_PAWN_TO_SIDE);
        play(game, "h2h3", "h7h6");

        // Act
        InvalidMoveException rejection = assertThrows(InvalidMoveException.class,
                () -> play(game, "e5d6"));

        // Assert
        assertEquals(InvalidMoveException.Reason.MOVE_NOT_ALLOWED, rejection.reason());
    }

    @Test
    void undoCaptureReturnsPawnAndPossibility() {
        // Arrange
        Game game = fromStart();
        play(game, BLACK_PAWN_TO_SIDE);
        Piece blackPawn = game.board().pieceIn(pos("d5")).orElseThrow();
        play(game, "e5d6");

        // Act
        game.undo();

        // Assert
        assertSame(blackPawn, game.board().pieceIn(pos("d5")).orElseThrow());
        assertTrue(game.board().pieceIn(pos("e5")).orElseThrow().isPawn());
        assertEquals(32, game.board().pieces().size());
        assertTrue(game.movementsLegal(pos("e5")).contains(pos("d6")));
    }

    @Test
    void notAllowsIfToTakeTwoPawnsKingStaysExposed() {
        // Arrange: rey blanco en a5 y torre negra en h5. Si el peón de b5 captura al paso al de c5,
        // la fila 5 queda vacía y la torre da jaque.
        Game game = game(BoardFake.from(
                "....k...",
                "..p.....",
                "........",
                "KP.....r",
                "........",
                "........",
                ".......P",
                "........"));
        play(game, "h2h3", "c7c5");

        // Act
        InvalidMoveException rejection = assertThrows(InvalidMoveException.class,
                () -> play(game, "b5c6"));

        // Assert
        assertEquals(InvalidMoveException.Reason.LEAVES_KING_IN_CHECK, rejection.reason());
        assertFalse(game.movementsLegal(pos("b5")).contains(pos("c6")));
    }
}
