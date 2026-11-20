package integration;

import core.side.Side;
import core.game.StandardLayout;
import core.game.Player;
import core.game.InvalidMoveException;
import core.game.Game;
import core.game.GameInProgress;
import core.pieces.Piece;
import core.rules.GameRules;
import core.time.NoClock;
import adapters.GameAssembler;
import org.junit.jupiter.api.Test;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** El enroque jugado en partidas reales, con las reglas clásicas y el deshacer habilitado. */
class CastlingInGameTest {

    private Game game() {
        return new Game(new StandardLayout().createBoard(), new Player("Ana", Side.WHITE),
                new Player("Beto", Side.BLACK), GameRules.classic().withUndo(), new NoClock());
    }

    private void play(GameInProgress game, String... moves) {
        for (String move : moves) {
            game.move(move(move.substring(0, 2), move.substring(2, 4)));
        }
    }

    /** Los dos bandos sacan el caballo y el alfil del flanco de rey: quedan listos para el enroque corto. */
    private static final String[] FLANKS_OF_KING_FREE = {"e2e4", "e7e5", "g1f3", "g8f6", "f1c4", "f8c5"};

    @Test
    void kingShowsCastlingBetweenTheirDestinations() {
        // Arrange
        GameInProgress game = game();
        play(game, FLANKS_OF_KING_FREE);

        // Act + Assert
        assertTrue(game.movementsLegal(pos("e1")).contains(pos("g1")));
    }

    @Test
    void bothSidesCastleShort() {
        // Arrange
        GameInProgress game = game();
        play(game, FLANKS_OF_KING_FREE);
        Piece whiteRook = game.board().pieceIn(pos("h1")).orElseThrow();

        // Act
        play(game, "e1g1", "e8g8");

        // Assert
        assertTrue(game.board().pieceIn(pos("g1")).orElseThrow().isKing());
        assertSame(whiteRook, game.board().pieceIn(pos("f1")).orElseThrow());
        assertTrue(game.board().pieceIn(pos("g8")).orElseThrow().isKing());
        assertTrue(game.board().pieceIn(pos("f8")).orElseThrow().isRook());
        assertEquals(move("e8", "g8"), game.history().getLast());
        assertEquals(Side.WHITE, game.turn());
    }

    @Test
    void whiteCastleLength() {
        // Arrange
        GameInProgress game = game();
        play(game, "d2d4", "d7d5", "b1c3", "b8c6", "c1f4", "c8f5", "d1d2", "d8d7");

        // Act
        play(game, "e1c1");

        // Assert
        assertTrue(game.board().pieceIn(pos("c1")).orElseThrow().isKing());
        assertTrue(game.board().pieceIn(pos("d1")).orElseThrow().isRook());
        assertTrue(game.board().pieceIn(pos("a1")).isEmpty());
    }

    @Test
    void undoCastlingReturnsKingAndRookAndRightToCastle() {
        // Arrange
        GameInProgress game = game();
        play(game, FLANKS_OF_KING_FREE);
        play(game, "e1g1");

        // Act
        game.undo();

        // Assert
        assertTrue(game.board().pieceIn(pos("e1")).orElseThrow().isKing());
        assertTrue(game.board().pieceIn(pos("h1")).orElseThrow().isRook());
        assertTrue(game.movementsLegal(pos("e1")).contains(pos("g1")));
    }

    @Test
    void ifKingWasAndReturnedLosesRightToCastle() {
        // Arrange
        GameInProgress game = game();
        play(game, FLANKS_OF_KING_FREE);
        play(game, "e1e2", "f6g8", "e2e1", "g8f6");

        // Act
        InvalidMoveException rejection = assertThrows(InvalidMoveException.class,
                () -> play(game, "e1g1"));

        // Assert
        assertEquals(InvalidMoveException.Reason.MOVIMIENTO_NO_PERMITIDO, rejection.reason());
        assertFalse(game.movementsLegal(pos("e1")).contains(pos("g1")));
    }

    @Test
    void gameBuiltByAssemblerAllowsCastle() {
        // Arrange
        GameInProgress game = new GameAssembler().createWithoutClock();
        play(game, FLANKS_OF_KING_FREE);

        // Act
        play(game, "e1g1");

        // Assert
        assertTrue(game.board().pieceIn(pos("g1")).orElseThrow().isKing());
    }
}
