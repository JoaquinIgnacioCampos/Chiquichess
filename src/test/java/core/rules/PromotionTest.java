package core.rules;

import core.side.Side;
import core.moves.Move;
import core.moves.Replacement;
import core.moves.Removal;
import core.moves.Transfer;
import core.pieces.Piece;
import core.pieces.Rook;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class PromotionTest {

    private final Promotion promotion = new Promotion();

    /** Peón blanco en a7 con torre negra en b8, y los dos reyes. */
    private Board pawnToPointOfPromote() {
        return BoardFake.from(
                ".t..r...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");
    }

    private List<Character> codes(List<Move> moves) {
        return moves.stream().map(j -> j.promotion().orElseThrow().code()).toList();
    }

    @Test
    void advanceToLastRowConvertsInAMoveByEachPiece() {
        // Arrange
        Board board = pawnToPointOfPromote();
        Piece pawn = board.pieceIn(pos("a7")).orElseThrow();
        Move advance = Move.normal(board, pos("a7"), pos("a8"));

        // Act
        List<Move> variants = promotion.transform(board, advance);

        // Assert: dama primero (la elección por defecto), después torre, alfil y caballo
        assertEquals(List.of('D', 'T', 'A', 'C'), codes(variants));
        Move toQueen = variants.get(0);
        assertEquals(new Transfer(pawn, pos("a7"), pos("a8")), toQueen.effects().get(0));
        assertInstanceOf(Replacement.class, toQueen.effects().get(1));
        assertSame(pawn, toQueen.pieceMoved());
        assertEquals(Side.WHITE, toQueen.promotion().orElseThrow().color());
    }

    @Test
    void aCaptureTowardLastRowAlsoPromotesAndKeepsCapture() {
        // Arrange
        Board board = pawnToPointOfPromote();
        Piece blackRook = board.pieceIn(pos("b8")).orElseThrow();
        Move capture = Move.normal(board, pos("a7"), pos("b8"));

        // Act
        List<Move> variants = promotion.transform(board, capture);

        // Assert
        assertEquals(4, variants.size());
        assertEquals(new Removal(blackRook, pos("b8")), variants.get(0).effects().get(0));
        assertSame(blackRook, variants.get(0).captured().orElseThrow());
    }

    @Test
    void blackPromoteInRowOne() {
        // Arrange
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "p.......",
                "....R...");

        // Act
        List<Move> variants = promotion.transform(board, Move.normal(board, pos("a2"), pos("a1")));

        // Assert
        assertEquals(4, variants.size());
        assertEquals(Side.BLACK, variants.get(0).promotion().orElseThrow().color());
    }

    @Test
    void movesThatNotArriveToLastRowStayEqual() {
        // Arrange: un avance de peón común y una jugada de torre que llega a la última fila
        Board board = BoardFake.from(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "P......T",
                "....R...");
        Move advance = Move.normal(board, pos("a2"), pos("a4"));
        Move rook = Move.normal(board, pos("h2"), pos("h8"));

        // Act + Assert
        assertEquals(List.of(advance), promotion.transform(board, advance));
        assertEquals(List.of(rook), promotion.transform(board, rook));
    }

    @Test
    void notAddsMovesOwn() {
        // Act + Assert
        assertTrue(promotion.moves(pawnToPointOfPromote(), pos("a7"), List.of(), new CheckDetector()).isEmpty());
    }

    @Test
    void optionsCanConfigureAndFirstIsChosenByDefault() {
        // Arrange: una variante que solo permite promover a torre
        Promotion onlyRook = new Promotion(List.of(Rook::new));
        Board board = pawnToPointOfPromote();

        // Act
        List<Move> variants = onlyRook.transform(board, Move.normal(board, pos("a7"), pos("a8")));

        // Assert
        assertEquals(List.of('T'), codes(variants));
        assertThrows(IllegalArgumentException.class, () -> new Promotion(List.of()));
    }
}
