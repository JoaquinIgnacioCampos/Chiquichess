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

class RookTest {

    @Test
    void inCenterOfAEmptyBoardHas14Destinations() {
        // Arrange
        Rook rook = new Rook(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", rook);

        // Act
        Set<Position> destinations = rook.destinationsPossible(board, pos("d4"));

        // Assert
        assertEquals(14, destinations.size());
    }

    @Test
    void onlyMovesInRowsAndColumns() {
        // Arrange
        Rook rook = new Rook(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", rook);

        // Act
        Set<Position> destinations = rook.destinationsPossible(board, pos("d4"));

        // Assert
        for (Position destination : destinations) {
            assertTrue(destination.column() == 3 || destination.row() == 3, "Destination not orthogonal: " + destination);
        }
    }

    @Test
    void piecesOwnBlockAndRivalsCanCapture() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "...P....",
                "........",
                ".p.R....",
                "........",
                "........",
                "........");
        Piece rook = board.pieceIn(pos("d4")).orElseThrow();

        // Act
        Set<Position> destinations = rook.destinationsPossible(board, pos("d4"));

        // Assert
        assertTrue(destinations.contains(pos("d5")));
        assertFalse(destinations.contains(pos("d6")), "It cannot land on its own pawn");
        assertFalse(destinations.contains(pos("d7")), "It cannot jump its own pawn");
        assertTrue(destinations.contains(pos("b4")), "It can capture the rival pawn");
        assertFalse(destinations.contains(pos("a4")), "It cannot jump the rival pawn");
    }

    @Test
    void inInitialPositionNotCanMoveSelf() {
        // Arrange
        Board board = new core.game.StandardLayout().createBoard();
        Piece rook = board.pieceIn(pos("a1")).orElseThrow();

        // Act
        Set<Position> destinations = rook.destinationsPossible(board, pos("a1"));

        // Assert
        assertEquals(Set.of(), destinations);
    }

    @Test
    void identifiesAsRook() {
        // Arrange
        Rook rook = new Rook(Side.BLACK);

        // Assert
        assertEquals('R', rook.code());
        assertEquals("Rook", rook.displayName());
        assertEquals(Side.BLACK, rook.color());
        assertFalse(rook.isKing());
        assertFalse(rook.isPawn());
    }

    @Test
    void squaresAttackedAreTheirDestinations() {
        // Arrange
        Rook rook = new Rook(Side.WHITE);
        Board board = BoardFake.withAPiece("a1", rook);

        // Act
        Set<Position> attacked = rook.squaresAttacked(board, pos("a1"));

        // Assert
        assertEquals(rook.destinationsPossible(board, pos("a1")), attacked);
        assertTrue(attacked.containsAll(positions("a8", "h1")));
    }
}
