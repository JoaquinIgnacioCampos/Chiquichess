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
                "...A....",
                "..P.....",
                "........",
                "........");
        Piece bishop = board.pieceIn(pos("d4")).orElseThrow();

        // Act
        Set<Position> destinations = bishop.destinationsPossible(board, pos("d4"));

        // Assert
        assertTrue(destinations.contains(pos("f6")), "Captura al peón rival");
        assertFalse(destinations.contains(pos("g7")), "No salta al peón rival");
        assertFalse(destinations.contains(pos("c3")), "No cae sobre su propio peón");
        assertFalse(destinations.contains(pos("b2")), "No salta a su propio peón");
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
        assertEquals('A', bishop.code());
        assertEquals("Alfil", bishop.displayName());
        assertFalse(bishop.isKing());
        assertFalse(bishop.isPawn());
    }
}
