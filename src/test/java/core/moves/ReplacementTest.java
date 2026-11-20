package core.moves;

import core.side.Side;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.pieces.Queen;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class ReplacementTest {

    private final Piece pawn = new Pawn(Side.WHITE);
    private final Piece queen = new Queen(Side.WHITE);

    @Test
    void applyChangesPieceOfSquare() {
        // Arrange
        Board board = BoardFake.withAPiece("e8", pawn);

        // Act
        new Replacement(pawn, queen, pos("e8")).apply(board);

        // Assert
        assertSame(queen, board.pieceIn(pos("e8")).orElseThrow());
        assertEquals(1, board.pieces().size());
    }

    @Test
    void revertReturnsPreviousPiece() {
        // Arrange
        Board board = BoardFake.withAPiece("e8", pawn);
        Replacement replacement = new Replacement(pawn, queen, pos("e8"));
        replacement.apply(board);

        // Act
        replacement.revert(board);

        // Assert
        assertSame(pawn, board.pieceIn(pos("e8")).orElseThrow());
    }

    @Test
    void ifNotIsPieceExpectedNotPlaysBoard() {
        // Arrange: en e8 hay otro peón
        Board board = BoardFake.withAPiece("e8", new Pawn(Side.WHITE));
        Piece before = board.pieceIn(pos("e8")).orElseThrow();

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new Replacement(pawn, queen, pos("e8")).apply(board));
        assertSame(before, board.pieceIn(pos("e8")).orElseThrow());
        assertThrows(IllegalStateException.class, () -> new Replacement(pawn, queen, pos("e8")).revert(board));
    }
}
