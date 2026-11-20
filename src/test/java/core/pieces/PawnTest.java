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

class PawnTest {

    @Test
    void whiteInItsInitialRowCanAdvanceAOrTwoSquares() {
        // Arrange
        Pawn pawn = new Pawn(Side.WHITE);
        Board board = BoardFake.withAPiece("e2", pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e2"));

        // Assert
        assertEquals(positions("e3", "e4"), destinations);
    }

    @Test
    void whiteOutsideOfItsInitialRowOnlyAdvancesASquare() {
        // Arrange
        Pawn pawn = new Pawn(Side.WHITE);
        Board board = BoardFake.withAPiece("e3", pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e3"));

        // Assert
        assertEquals(positions("e4"), destinations);
    }

    @Test
    void blackAdvancesTowardRowsSmaller() {
        // Arrange
        Pawn pawn = new Pawn(Side.BLACK);
        Board board = BoardFake.withAPiece("e7", pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e7"));

        // Assert
        assertEquals(positions("e6", "e5"), destinations);
    }

    @Test
    void blackOutsideOfItsInitialRowOnlyAdvancesASquare() {
        // Arrange
        Pawn pawn = new Pawn(Side.BLACK);
        Board board = BoardFake.withAPiece("e6", pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e6"));

        // Assert
        assertEquals(positions("e5"), destinations);
    }

    @Test
    void notCanCaptureTowardForward() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "....p...",
                "....P...",
                "........");
        Piece pawn = board.pieceIn(pos("e2")).orElseThrow();

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e2"));

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void advanceDoubleNotJumpsAPieceInMiddle() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "....N...",
                "....P...",
                "........");
        Piece pawn = board.pieceIn(pos("e2")).orElseThrow();

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e2"));

        // Assert
        assertFalse(destinations.contains(pos("e4")));
    }

    @Test
    void withSecondOccupiedSquareOnlyAdvancesA() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "....p...",
                "........",
                "....P...",
                "........");
        Piece pawn = board.pieceIn(pos("e2")).orElseThrow();

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e2"));

        // Assert
        assertEquals(positions("e3"), destinations);
    }

    @Test
    void capturePiecesRivalsInDiagonal() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...p.p..",
                "....P...",
                "........",
                "........",
                "........");
        Piece pawn = board.pieceIn(pos("e4")).orElseThrow();

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e4"));

        // Assert
        assertEquals(positions("d5", "e5", "f5"), destinations);
    }

    @Test
    void notCapturePiecesOwnInDiagonal() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "...P.P..",
                "....P...",
                "........",
                "........",
                "........");
        Piece pawn = board.pieceIn(pos("e4")).orElseThrow();

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e4"));

        // Assert
        assertEquals(positions("e5"), destinations);
    }

    @Test
    void withoutPiecesInDiagonalsNotCanMoveSelfInDiagonal() {
        // Arrange
        Pawn pawn = new Pawn(Side.WHITE);
        Board board = BoardFake.withAPiece("e4", pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e4"));

        // Assert
        assertFalse(destinations.contains(pos("d5")));
        assertFalse(destinations.contains(pos("f5")));
    }

    @Test
    void attacksTheirDiagonalsAlthoughAreEmptyButNotSquareOfForward() {
        // Arrange
        Pawn pawn = new Pawn(Side.WHITE);
        Board board = BoardFake.withAPiece("e4", pawn);

        // Act
        Set<Position> attacked = pawn.squaresAttacked(board, pos("e4"));

        // Assert
        assertEquals(positions("d5", "f5"), attacked);
    }

    @Test
    void blackPawnAttacksTowardDown() {
        // Arrange
        Pawn pawn = new Pawn(Side.BLACK);
        Board board = BoardFake.withAPiece("e5", pawn);

        // Act
        Set<Position> attacked = pawn.squaresAttacked(board, pos("e5"));

        // Assert
        assertEquals(positions("d4", "f4"), attacked);
    }

    @Test
    void inBorderOnlyAttacksADiagonal() {
        // Arrange
        Pawn pawn = new Pawn(Side.WHITE);
        Board board = BoardFake.withAPiece("a4", pawn);

        // Act
        Set<Position> attacked = pawn.squaresAttacked(board, pos("a4"));

        // Assert
        assertEquals(positions("b5"), attacked);
    }

    @Test
    void inLastRowNotHasMovementsOwn() {
        // Arrange: en una partida no llega a quedarse ahí, porque la regla de promoción lo reemplaza
        // al llegar; esto verifica solo el movimiento propio de la pieza
        Pawn pawn = new Pawn(Side.WHITE);
        Board board = BoardFake.withAPiece("e8", pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e8"));

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void inABoardMoreHeightBlackStartsInPenultimateRow() {
        // Arrange: tablero de 8 columnas x 10 filas, la fila inicial negra es la 9
        Pawn pawn = new Pawn(Side.BLACK);
        Board board = new Board(8, 10);
        board.place(pos("e9"), pawn);

        // Act
        Set<Position> destinations = pawn.destinationsPossible(board, pos("e9"));

        // Assert
        assertEquals(positions("e8", "e7"), destinations);
    }

    @Test
    void identifiesAsPawn() {
        // Arrange
        Pawn pawn = new Pawn(Side.WHITE);

        // Assert
        assertTrue(pawn.isPawn());
        assertFalse(pawn.isKing());
        assertEquals('P', pawn.code());
        assertEquals("Pawn", pawn.displayName());
    }
}
