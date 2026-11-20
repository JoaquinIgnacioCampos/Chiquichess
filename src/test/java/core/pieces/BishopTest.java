package core.pieces;

import core.side.Side;
import core.game.StandardLayout;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class BishopTest {

    @Test
    void inD4OfAEmptyBoardHas13Destinations() {
        // Arrange
        Bishop bishop = new Bishop(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", bishop);

        // Act
        Set<Position> destinations = bishop.destinationsPossible(board, pos("d4"));

        // Assert
        assertEquals(13, destinations.size());
    }

    @Test
    void onlyMovesInDiagonalAndNeverChangesOfColorOfSquare() {
        // Arrange
        Bishop bishop = new Bishop(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", bishop);
        int colorOfSquareOrigin = (3 + 3) % 2;

        // Act
        Set<Position> destinations = bishop.destinationsPossible(board, pos("d4"));

        // Assert
        for (Position destination : destinations) {
            assertEquals(Math.abs(destination.column() - 3), Math.abs(destination.row() - 3));
            assertEquals(colorOfSquareOrigin, (destination.column() + destination.row()) % 2);
        }
    }

    @Test
    void piecesOwnItBlockAndRivalsCanCapture() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                ".....p..",
                "........",
                "...B....",
                "..P.....",
                "........",
                "........");
        Piece bishop = board.pieceIn(pos("d4")).orElseThrow();

        // Act
        Set<Position> destinations = bishop.destinationsPossible(board, pos("d4"));

        // Assert
        assertTrue(destinations.contains(pos("f6")), "Captures the rival pawn");
        assertFalse(destinations.contains(pos("g7")), "It does not jump the rival pawn");
        assertFalse(destinations.contains(pos("c3")), "It does not land on its own pawn");
        assertFalse(destinations.contains(pos("b2")), "It does not jump its own pawn");
    }

    @Test
    void inInitialPositionNotCanMoveSelf() {
        // Arrange
        Board board = new StandardLayout().createBoard();
        Piece bishop = board.pieceIn(pos("c1")).orElseThrow();

        // Act
        Set<Position> destinations = bishop.destinationsPossible(board, pos("c1"));

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void identifiesAsBishop() {
        // Arrange
        Bishop bishop = new Bishop(Side.WHITE);

        // Assert
        assertEquals('B', bishop.code());
        assertEquals("Bishop", bishop.displayName());
        assertFalse(bishop.isKing());
        assertFalse(bishop.isPawn());
    }
}
