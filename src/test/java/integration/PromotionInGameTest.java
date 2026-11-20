package integration;

import core.side.Side;
import core.game.Player;
import core.game.MoveRequest;
import core.game.InvalidMoveException;
import core.game.Game;
import core.moves.PositionFingerprint;
import core.pieces.Piece;
import core.rules.GameState;
import core.rules.GameRules;
import core.board.Board;
import core.time.NoClock;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static doubles.Squares.move;
import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

/** La promoción jugada en partidas reales, con las reglas clásicas y el deshacer habilitado. */
class PromotionInGameTest {

    /** Peón blanco en a7 a punto de promover; torre negra en b8 para poder capturar al promover. */
    private Game withPawnInTo7() {
        Board board = BoardFake.from(
                ".t..r...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");
        return new Game(board, new Player("Ana", Side.WHITE), new Player("Beto", Side.BLACK),
                GameRules.classic().withUndo(), new NoClock());
    }

    @Test
    void withoutChoosePawnConvertsInQueen() {
        // Arrange
        Game game = withPawnInTo7();

        // Act
        game.move(move("a7", "a8"));

        // Assert
        Piece newValue = game.board().pieceIn(pos("a8")).orElseThrow();
        assertEquals('D', newValue.code());
        assertEquals(Side.WHITE, newValue.color());
        assertEquals(List.of(MoveRequest.withPromotion(pos("a7"), pos("a8"), 'D')), game.history());
    }

    @Test
    void canChooseOtherPiece() {
        // Arrange
        Game game = withPawnInTo7();

        // Act
        game.move(MoveRequest.withPromotion(pos("a7"), pos("a8"), 'C'));

        // Assert
        assertEquals('C', game.board().pieceIn(pos("a8")).orElseThrow().code());
    }

    @Test
    void canPromoteCapturing() {
        // Arrange
        Game game = withPawnInTo7();

        // Act
        var result = game.move(MoveRequest.withPromotion(pos("a7"), pos("b8"), 'T'));

        // Assert
        assertTrue(result.capturedPiece().orElseThrow().isRook());
        assertEquals('T', game.board().pieceIn(pos("b8")).orElseThrow().code());
        assertEquals(Side.WHITE, game.board().pieceIn(pos("b8")).orElseThrow().color());
    }

    @Test
    void notCanPromoteToAPieceThatNotIsBetweenOptions() {
        // Arrange
        Game game = withPawnInTo7();

        // Act
        InvalidMoveException rejection = assertThrows(InvalidMoveException.class,
                () -> game.move(MoveRequest.withPromotion(pos("a7"), pos("a8"), 'R')));

        // Assert
        assertEquals(InvalidMoveException.Reason.MOVIMIENTO_NO_PERMITIDO, rejection.reason());
        assertTrue(game.board().pieceIn(pos("a7")).orElseThrow().isPawn());
    }

    @Test
    void requestPromotionInAMoveThatNotPromotesRejects() {
        // Arrange
        Game game = withPawnInTo7();

        // Act + Assert
        assertThrows(InvalidMoveException.class,
                () -> game.move(MoveRequest.withPromotion(pos("e1"), pos("e2"), 'D')));
    }

    @Test
    void queenNewCanGiveCheck() {
        // Arrange: el rey negro está en e8, en la fila de promoción
        Game game = withPawnInTo7();

        // Act: el peón captura la torre de b8 y se convierte en dama, que ataca e8 por la fila
        game.move(MoveRequest.withPromotion(pos("a7"), pos("b8"), 'D'));

        // Assert
        assertEquals(GameState.CHECK, game.state());
    }

    @Test
    void optionsOfPromotionAreFourPiecesInOrder() {
        // Arrange
        Game game = withPawnInTo7();

        // Act
        List<Character> advancing = codes(game.promotionOptions(pos("a7"), pos("a8")));
        List<Character> capturing = codes(game.promotionOptions(pos("a7"), pos("b8")));

        // Assert
        assertEquals(List.of('D', 'T', 'A', 'C'), advancing);
        assertEquals(List.of('D', 'T', 'A', 'C'), capturing);
        assertTrue(game.promotionOptions(pos("a7"), pos("a8")).stream()
                .allMatch(piece -> piece.color() == Side.WHITE));
    }

    @Test
    void withoutPromotionNotHasOptions() {
        // Arrange
        Game game = withPawnInTo7();

        // Assert: una jugada común, un destino imposible y una pieza que no es del turno
        assertTrue(game.promotionOptions(pos("e1"), pos("e2")).isEmpty());
        assertTrue(game.promotionOptions(pos("a7"), pos("a6")).isEmpty());
        assertTrue(game.promotionOptions(pos("b8"), pos("b1")).isEmpty());
    }

    private static List<Character> codes(List<Piece> pieces) {
        return pieces.stream().map(Piece::code).toList();
    }

    @Test
    void undoPromotionReturnsPawn() {
        // Arrange
        Game game = withPawnInTo7();
        Piece pawn = game.board().pieceIn(pos("a7")).orElseThrow();
        long fingerprintBefore = PositionFingerprint.de(game.board(), game.turn());
        game.move(MoveRequest.withPromotion(pos("a7"), pos("b8"), 'D'));

        // Act
        game.undo();

        // Assert
        assertSame(pawn, game.board().pieceIn(pos("a7")).orElseThrow());
        assertTrue(game.board().pieceIn(pos("b8")).orElseThrow().isRook());
        assertEquals(fingerprintBefore, PositionFingerprint.de(game.board(), game.turn()));
    }
}
