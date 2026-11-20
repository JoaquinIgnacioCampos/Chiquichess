package core.board;

import core.side.Side;
import core.pieces.Piece;
import core.pieces.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;
    private final Piece whiteRook = new Rook(Side.WHITE);
    private final Piece blackRook = new Rook(Side.BLACK);

    @BeforeEach
    void prepareBoard() {
        board = new Board(8, 8);
    }

    @Test
    void rejectsDimensionsNotPositive() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Board(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new Board(8, -1));
    }

    @Test
    void allowsBoardsThatNotAreOf8X8() {
        // Act
        Board small = new Board(5, 6);

        // Assert
        assertEquals(5, small.width());
        assertEquals(6, small.height());
    }

    @Test
    void containsSquaresInsideOfLimits() {
        // Assert
        assertTrue(board.contains(new Position(0, 0)));
        assertTrue(board.contains(new Position(7, 7)));
        assertTrue(board.contains(new Position(3, 5)));
    }

    @Test
    void notContainsSquaresOutsideOfLimits() {
        // Assert
        assertFalse(board.contains(new Position(8, 0)));
        assertFalse(board.contains(new Position(0, 8)));
        assertFalse(board.contains(new Position(-1, 3)));
        assertFalse(board.contains(new Position(3, -1)));
    }

    @Test
    void aNewBoardIsEmpty() {
        // Assert
        assertTrue(board.pieces().isEmpty());
        assertTrue(board.pieceIn(new Position(4, 4)).isEmpty());
    }

    @Test
    void placeLeavesPieceInThatSquare() {
        // Act
        board.place(new Position(0, 0), whiteRook);

        // Assert
        assertEquals(Optional.of(whiteRook), board.pieceIn(new Position(0, 0)));
    }

    @Test
    void placeOnAOccupiedSquareReplacesPiece() {
        // Arrange
        board.place(new Position(0, 0), whiteRook);

        // Act
        board.place(new Position(0, 0), blackRook);

        // Assert
        assertEquals(Optional.of(blackRook), board.pieceIn(new Position(0, 0)));
        assertEquals(1, board.pieces().size());
    }

    @Test
    void placeOutsideOfBoardThrowsException() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> board.place(new Position(8, 8), whiteRook));
    }

    @Test
    void pieceInASquareOutsideOfBoardReturnsEmpty() {
        // Act
        Optional<Piece> result = board.pieceIn(new Position(-5, 20));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void removeReturnsPieceAndLeavesSquareFree() {
        // Arrange
        board.place(new Position(2, 2), whiteRook);

        // Act
        Optional<Piece> removed = board.remove(new Position(2, 2));

        // Assert
        assertEquals(Optional.of(whiteRook), removed);
        assertTrue(board.pieceIn(new Position(2, 2)).isEmpty());
    }

    @Test
    void removeOfAEmptySquareReturnsEmpty() {
        // Act
        Optional<Piece> removed = board.remove(new Position(2, 2));

        // Assert
        assertTrue(removed.isEmpty());
    }

    @Test
    void mapOfPiecesIsOfOnlyReading() {
        // Arrange
        board.place(new Position(0, 0), whiteRook);

        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> board.pieces().put(new Position(1, 1), blackRook));
    }

    @Test
    void copyReturnsABoardIndependent() {
        // Arrange
        board.place(new Position(0, 0), whiteRook);

        // Act
        Board copy = board.copy();
        copy.remove(new Position(0, 0));
        copy.place(new Position(5, 5), blackRook);

        // Assert
        assertEquals(Optional.of(whiteRook), board.pieceIn(new Position(0, 0)));
        assertTrue(board.pieceIn(new Position(5, 5)).isEmpty());
        assertEquals(board.width(), copy.width());
        assertEquals(board.height(), copy.height());
    }
}
