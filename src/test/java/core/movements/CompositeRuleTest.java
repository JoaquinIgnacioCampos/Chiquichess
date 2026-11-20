package core.movements;

import core.side.Side;
import core.board.Position;

import doubles.StubRuleMovement;
import doubles.BoardFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static doubles.Squares.pos;
import static doubles.Squares.positions;
import static org.junit.jupiter.api.Assertions.*;

/** Se usan stubs para probar solo la "unión" de ReglaCompuesta, sin depender de reglas reales. */
class CompositeRuleTest {

    @Test
    void aDestinationsOfAllRules() {
        // Arrange
        CompositeRule composite = new CompositeRule(
                new StubRuleMovement(positions("a1", "b2")),
                new StubRuleMovement(positions("c3")));

        // Act
        Set<Position> destinations = composite.destinationsPossible(BoardFake.empty(), pos("d4"), Side.WHITE);

        // Assert
        assertEquals(positions("a1", "b2", "c3"), destinations);
    }

    @Test
    void aSquaresAttackedOfAllRules() {
        // Arrange
        CompositeRule composite = new CompositeRule(
                new StubRuleMovement(Set.of(), positions("e5")),
                new StubRuleMovement(Set.of(), positions("f6")));

        // Act
        Set<Position> attacked = composite.squaresAttacked(BoardFake.empty(), pos("d4"), Side.WHITE);

        // Assert
        assertEquals(positions("e5", "f6"), attacked);
    }

    @Test
    void notRepeatsSquaresThatReturnSeveralRules() {
        // Arrange
        CompositeRule composite = new CompositeRule(
                new StubRuleMovement(positions("a1", "b2")),
                new StubRuleMovement(positions("b2", "c3")));

        // Act
        Set<Position> destinations = composite.destinationsPossible(BoardFake.empty(), pos("d4"), Side.WHITE);

        // Assert
        assertEquals(3, destinations.size());
    }

    @Test
    void withoutRulesNotHasDestinations() {
        // Arrange
        CompositeRule empty = new CompositeRule();

        // Act
        Set<Position> destinations = empty.destinationsPossible(BoardFake.empty(), pos("d4"), Side.WHITE);

        // Assert
        assertTrue(destinations.isEmpty());
    }
}
