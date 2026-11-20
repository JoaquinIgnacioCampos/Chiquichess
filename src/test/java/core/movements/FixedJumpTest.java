package core.movements;

import core.side.Side;
import core.board.Position;
import core.board.Board;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

class FixedJumpTest {

    private static final int[][] JUMPS_OF_KNIGHT = {
            {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};

    @Test
    void arrivesToAllOffsetsInAEmptyBoard() {
        // Arrange
        FixedJump jump = new FixedJump(JUMPS_OF_KNIGHT);

        // Act
        Set<Position> destinations = jump.destinationsPossible(BoardFake.empty(), pos("d4"), Side.WHITE);

        // Assert
        assertEquals(positions("e6", "f5", "c6", "b5", "e2", "f3", "c2", "b3"), destinations);
    }

    @Test
    void discardsDestinationsOutsideOfBoard() {
        // Arrange
        FixedJump jump = new FixedJump(JUMPS_OF_KNIGHT);

        // Act
        Set<Position> destinations = jump.destinationsPossible(BoardFake.empty(), pos("a1"), Side.WHITE);

        // Assert
        assertEquals(positions("b3", "c2"), destinations);
    }

    @Test
    void jumpsByAboveOfPiecesThatItSurround() {
        // Arrange: un caballo en d4 completamente rodeado de piezas
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "..ppp...",
                "..pNp...",
                "..ppp...",
                "........",
                "........");
        FixedJump jump = new FixedJump(JUMPS_OF_KNIGHT);

        // Act
        Set<Position> destinations = jump.destinationsPossible(board, pos("d4"), Side.WHITE);

        // Assert
        assertEquals(8, destinations.size());
    }

    @Test
    void notCanFallOnAOwnPieceButIfOnARival() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "..P.....",
                ".....p..",
                "...N....",
                "........",
                "........",
                "........");
        FixedJump jump = new FixedJump(JUMPS_OF_KNIGHT);

        // Act
        Set<Position> destinations = jump.destinationsPossible(board, pos("d4"), Side.WHITE);

        // Assert
        assertFalse(destinations.contains(pos("c6")));
        assertTrue(destinations.contains(pos("f5")));
    }

    @Test
    void copyOffsetsForThatNotModifyFromOutside() {
        // Arrange
        int[][] offsets = {{1, 2}};
        FixedJump jump = new FixedJump(offsets);

        // Act
        offsets[0][0] = 2;

        // Assert
        assertEquals(positions("e6"), jump.destinationsPossible(BoardFake.empty(), pos("d4"), Side.WHITE));
    }

    @Test
    void squaresAttackedMatchWithDestinations() {
        // Arrange
        FixedJump jump = new FixedJump(JUMPS_OF_KNIGHT);

        // Act
        Set<Position> destinations = jump.destinationsPossible(BoardFake.empty(), pos("b1"), Side.WHITE);
        Set<Position> attacked = jump.squaresAttacked(BoardFake.empty(), pos("b1"), Side.WHITE);

        // Assert
        assertEquals(destinations, attacked);
    }
}
