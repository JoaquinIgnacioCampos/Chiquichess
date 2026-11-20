package core.pieces;

import core.side.Side;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

class KingTest {

    @Test
    void inCenterMovesASquareIn8Directions() {
        // Arrange
        King king = new King(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", king);

        // Act
        Set<Position> destinations = king.destinationsPossible(board, pos("d4"));

        // Assert
        assertEquals(positions("c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5"), destinations);
    }

    @Test
    void inCornerHas3Destinations() {
        // Arrange
        King king = new King(Side.WHITE);
        Board board = BoardFake.withAPiece("a1", king);

        // Act
        Set<Position> destinations = king.destinationsPossible(board, pos("a1"));

        // Assert
        assertEquals(positions("a2", "b1", "b2"), destinations);
    }

    @Test
    void notCanGoOnPiecesOwnButIfCaptureRivals() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...p....",
                "...KP...",
                "........",
                "........",
                "........");
        Piece king = board.pieceIn(pos("d4")).orElseThrow();

        // Act
        Set<Position> destinations = king.destinationsPossible(board, pos("d4"));

        // Assert
        assertTrue(destinations.contains(pos("d5")));
        assertFalse(destinations.contains(pos("e4")));
    }

    @Test
    void isOnlyThatIdentifiesAsKing() {
        // Arrange
        King king = new King(Side.BLACK);

        // Assert
        assertTrue(king.isKing());
        assertFalse(king.isPawn());
        assertEquals('K', king.code());
        assertEquals("King", king.displayName());
    }
}
