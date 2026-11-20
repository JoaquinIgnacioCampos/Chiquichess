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

class SlideTest {

    private static final int RIGHT = 1;

    @Test
    void rejectsDirectionNull() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Slide(0, 0, 1));
    }

    @Test
    void rejectsLessOfAStep() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Slide(1, 0, 0));
    }

    @Test
    void withoutLimitAdvancesUntilBorderInAEmptyBoard() {
        // Arrange
        Slide towardRight = new Slide(RIGHT, 0, Slide.WITHOUT_LIMIT);

        // Act
        Set<Position> destinations = towardRight.destinationsPossible(BoardFake.empty(), pos("a1"), Side.WHITE);

        // Assert
        assertEquals(positions("b1", "c1", "d1", "e1", "f1", "g1", "h1"), destinations);
    }

    @Test
    void respectsAmountMaximumOfSteps() {
        // Arrange
        Slide twoSteps = new Slide(RIGHT, 0, 2);

        // Act
        Set<Position> destinations = twoSteps.destinationsPossible(BoardFake.empty(), pos("a1"), Side.WHITE);

        // Assert
        assertEquals(positions("b1", "c1"), destinations);
    }

    @Test
    void brakesBeforeOfAOwnPiece() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R...P...");
        Slide toward = new Slide(RIGHT, 0, Slide.WITHOUT_LIMIT);

        // Act
        Set<Position> destinations = toward.destinationsPossible(board, pos("a1"), Side.WHITE);

        // Assert
        assertEquals(positions("b1", "c1", "d1"), destinations);
    }

    @Test
    void includesPieceRivalAsCaptureAndBrakesThere() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R...p.p.");
        Slide toward = new Slide(RIGHT, 0, Slide.WITHOUT_LIMIT);

        // Act
        Set<Position> destinations = toward.destinationsPossible(board, pos("a1"), Side.WHITE);

        // Assert
        assertEquals(positions("b1", "c1", "d1", "e1"), destinations);
    }

    @Test
    void fromBorderTowardOutsideNotHasDestinations() {
        // Arrange
        Slide towardLeft = new Slide(-1, 0, Slide.WITHOUT_LIMIT);

        // Act
        Set<Position> destinations = towardLeft.destinationsPossible(BoardFake.empty(), pos("a1"), Side.WHITE);

        // Assert
        assertTrue(destinations.isEmpty());
    }

    @Test
    void worksInDiagonal() {
        // Arrange
        Slide diagonal = new Slide(1, 1, Slide.WITHOUT_LIMIT);

        // Act
        Set<Position> destinations = diagonal.destinationsPossible(BoardFake.empty(), pos("e5"), Side.WHITE);

        // Assert
        assertEquals(positions("f6", "g7", "h8"), destinations);
    }

    @Test
    void squaresAttackedMatchWithDestinations() {
        // Arrange
        Board board = BoardFake.from(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R..p....");
        Slide toward = new Slide(RIGHT, 0, Slide.WITHOUT_LIMIT);

        // Act
        Set<Position> destinations = toward.destinationsPossible(board, pos("a1"), Side.WHITE);
        Set<Position> attacked = toward.squaresAttacked(board, pos("a1"), Side.WHITE);

        // Assert
        assertEquals(destinations, attacked);
    }
}
