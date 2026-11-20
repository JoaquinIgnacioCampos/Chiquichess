package core.moves;

import core.side.Side;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class RemovalTest {

    private final Piece pawn = new Pawn(Side.BLACK);

    @Test
    void applyTakesPieceOfBoard() {
        // Arrange
        Board board = BoardFake.withAPiece("d5", pawn);

        // Act
        new Removal(pawn, pos("d5")).apply(board);

        // Assert
        assertTrue(board.pieceIn(pos("d5")).isEmpty());
    }

    @Test
    void revertReturnsSamePieceToItsSquare() {
        // Arrange
        Board board = BoardFake.withAPiece("d5", pawn);
        Removal withdrawal = new Removal(pawn, pos("d5"));
        withdrawal.apply(board);

        // Act
        withdrawal.revert(board);

        // Assert
        assertSame(pawn, board.pieceIn(pos("d5")).orElseThrow());
    }

    @Test
    void ifPieceNotIsThereNotPlaysBoard() {
        // Arrange
        Board board = BoardFake.empty();

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new Removal(pawn, pos("d5")).apply(board));
        assertTrue(board.pieces().isEmpty());
    }

    @Test
    void notCanReturnItIfSquareStayedOccupied() {
        // Arrange
        Board board = BoardFake.withAPiece("d5", new Pawn(Side.WHITE));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new Removal(pawn, pos("d5")).revert(board));
    }
}
