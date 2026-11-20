package core.board;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void shiftedAddsDeltasToColumnAndRow() {
        // Arrange
        Position origin = new Position(3, 3);

        // Act
        Position destination = origin.shifted(1, -2);

        // Assert
        assertEquals(new Position(4, 1), destination);
    }

    @Test
    void shiftedNotModifiesPositionOriginal() {
        // Arrange
        Position origin = new Position(3, 3);

        // Act
        origin.shifted(2, 2);

        // Assert
        assertEquals(3, origin.column());
        assertEquals(3, origin.row());
    }

    @Test
    void shiftedCanExitOfBoardBecauseNotValidLimits() {
        // Arrange
        Position corner = new Position(0, 0);

        // Act
        Position outside = corner.shifted(-1, -1);

        // Assert
        assertEquals(new Position(-1, -1), outside);
    }

    @Test
    void twoPositionsWithSameCoordinatesAreEqualAndServeAsKeyOfMap() {
        // Arrange
        Map<Position, String> map = new HashMap<>();
        map.put(new Position(4, 1), "pawn");

        // Act
        String found = map.get(new Position(4, 1));

        // Assert
        assertEquals("pawn", found);
        assertEquals(new Position(4, 1).hashCode(), new Position(4, 1).hashCode());
    }
}
