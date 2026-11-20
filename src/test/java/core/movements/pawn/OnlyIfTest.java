package core.movements.pawn;

import core.side.Side;
import core.board.Position;

import doubles.MockRuleMovement;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

/** Se usa un mock de la regla interna para verificar si OnlyIf la llama o no. */
class OnlyIfTest {

    @Test
    void ifConditionMeetsDelegatesInRuleInternal() {
        // Arrange
        MockRuleMovement internal = new MockRuleMovement(positions("e4"));
        OnlyIf regla = new OnlyIf((board, origin, color) -> true, internal);

        // Act
        Set<Position> destinations = regla.destinationsPossible(BoardFake.empty(), pos("e2"), Side.WHITE);

        // Assert
        assertEquals(positions("e4"), destinations);
        assertEquals(1, internal.callsDestinations());
    }

    @Test
    void ifConditionNotMeetsNotHasDestinationsNorQueryRuleInternal() {
        // Arrange
        MockRuleMovement internal = new MockRuleMovement(positions("e4"));
        OnlyIf regla = new OnlyIf((board, origin, color) -> false, internal);

        // Act
        Set<Position> destinations = regla.destinationsPossible(BoardFake.empty(), pos("e3"), Side.WHITE);
        Set<Position> attacked = regla.squaresAttacked(BoardFake.empty(), pos("e3"), Side.WHITE);

        // Assert
        assertTrue(destinations.isEmpty());
        assertTrue(attacked.isEmpty());
        assertEquals(0, internal.callsDestinations());
        assertEquals(0, internal.callsAttacked());
    }

    @Test
    void conditionReceivesOriginAndColor() {
        // Arrange: la condición solo se cumple para peones negros en la fila 7
        OnlyIf regla = new OnlyIf(
                (board, origin, color) -> color == Side.BLACK && origin.row() == 6,
                new MockRuleMovement(positions("e5")));

        // Act
        Set<Position> blackInRow7 = regla.destinationsPossible(BoardFake.empty(), pos("e7"), Side.BLACK);
        Set<Position> whiteInRow7 = regla.destinationsPossible(BoardFake.empty(), pos("e7"), Side.WHITE);

        // Assert
        assertEquals(positions("e5"), blackInRow7);
        assertTrue(whiteInRow7.isEmpty());
    }
}
