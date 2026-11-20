package core.rules;

import doubles.StubDetectorOfCheck;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRulesTest {

    @Test
    void classicIncludeFourConditionsInOrder() {
        // Act
        List<EndCondition> conditions = GameRules.classic().conditions();

        // Assert: mate y ahogado primero, como pide Partida
        assertEquals(4, conditions.size());
        assertInstanceOf(Checkmate.class, conditions.get(0));
        assertInstanceOf(Stalemate.class, conditions.get(1));
        assertInstanceOf(RepetitionDraw.class, conditions.get(2));
        assertInstanceOf(FiftyMoveRuleDraw.class, conditions.get(3));
    }

    @Test
    void eachCalledToClassicCreatesInstancesNew() {
        // Act
        GameRules first = GameRules.classic();
        GameRules second = GameRules.classic();

        // Assert: las condiciones con memoria no se comparten entre partidas
        assertNotSame(first.conditions().get(2), second.conditions().get(2));
        assertNotSame(first.conditions().get(3), second.conditions().get(3));
        assertNotSame(first.movementsLegal(), second.movementsLegal());
    }

    @Test
    void detectorIsThatUseMovementsLegal() {
        // Arrange
        ICheckDetector detector = StubDetectorOfCheck.alwaysInCheck();

        // Act
        GameRules rules = new GameRules(detector, List.of());

        // Assert
        assertSame(detector, rules.detector());
        assertSame(detector, rules.movementsLegal().detector());
    }

    @Test
    void savesACopyUnmodifiableOfConditions() {
        // Arrange
        List<EndCondition> original = new ArrayList<>(List.of(new Checkmate()));
        GameRules rules = new GameRules(new CheckDetector(), original);

        // Act
        original.add(new Stalemate());

        // Assert
        assertEquals(1, rules.conditions().size());
        assertThrows(UnsupportedOperationException.class, () -> rules.conditions().add(new Stalemate()));
    }

    @Test
    void rejectsDependenciesNull() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new GameRules((LegalMoves) null, List.of()));
        assertThrows(NullPointerException.class, () -> new GameRules((ICheckDetector) null, List.of()));
        assertThrows(NullPointerException.class, () -> new GameRules(new CheckDetector(), null));
    }
}
