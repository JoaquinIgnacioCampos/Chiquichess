package core.side;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SideTest {

    @Test
    void whiteAdvanceTowardUpAndBlackTowardDown() {
        // Assert
        assertEquals(1, Side.WHITE.directionAdvance());
        assertEquals(-1, Side.BLACK.directionAdvance());
    }

    @Test
    void oppositeOfEachColorIsOther() {
        // Assert
        assertEquals(Side.BLACK, Side.WHITE.opposite());
        assertEquals(Side.WHITE, Side.BLACK.opposite());
    }

    @Test
    void oppositeOfOppositeIsSameColor() {
        // Assert
        assertEquals(Side.WHITE, Side.WHITE.opposite().opposite());
    }
}
