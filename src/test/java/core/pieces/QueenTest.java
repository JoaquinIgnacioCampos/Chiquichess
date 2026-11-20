package core.pieces;

import core.side.Side;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static doubles.Squares.pos;
import static org.junit.jupiter.api.Assertions.*;

class QueenTest {

    @Test
    void inD4OfAEmptyBoardHas27Destinations() {
        // Arrange
        Queen queen = new Queen(Side.WHITE);
        Board board = BoardFake.withAPiece("d4", queen);

        // Act
        Set<Position> destinations = queen.destinationsPossible(board, pos("d4"));

        // Assert
        assertEquals(27, destinations.size());
    }

    @Test
    void movesAsRookMoreBishop() {
        // Arrange
        Board board = BoardFake.empty();
        Set<Position> rookMoreBishop = new HashSet<>();
        rookMoreBishop.addAll(new Rook(Side.WHITE).destinationsPossible(board, pos("c3")));
        rookMoreBishop.addAll(new Bishop(Side.WHITE).destinationsPossible(board, pos("c3")));

        // Act
        Set<Position> destinationsQueen = new Queen(Side.WHITE).destinationsPossible(board, pos("c3"));

        // Assert
        assertEquals(rookMoreBishop, destinationsQueen);
    }

    @Test
    void piecesBlockInEachDirection() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...p....",
                "...QP...",
                "..P.....",
                "........",
                "........");
        Piece queen = board.pieceIn(pos("d4")).orElseThrow();

        // Act
        Set<Position> destinations = queen.destinationsPossible(board, pos("d4"));

        // Assert
        assertTrue(destinations.contains(pos("d5")), "Captures upward");
        assertFalse(destinations.contains(pos("d6")));
        assertFalse(destinations.contains(pos("e4")), "Blocked on the right by own piece");
        assertFalse(destinations.contains(pos("c3")), "Blocked diagonally by own piece");
    }

    @Test
    void identifiesAsQueen() {
        // Arrange
        Queen queen = new Queen(Side.WHITE);

        // Assert
        assertEquals('Q', queen.code());
        assertEquals("Queen", queen.displayName());
        assertFalse(queen.isKing());
    }
}
