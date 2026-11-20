package core.rules;

import doubles.Contexts;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Solo importan los dos booleanos del contexto: no hace falta armar ninguna posición. */
class CheckmateTest {

    private final Checkmate checkMate = new Checkmate();

    @Test
    void inCheckAndWithoutMovesLegalIsCheckMate() {
        // Act
        Optional<GameState> result = checkMate.evaluate(Contexts.con(true, false));

        // Assert
        assertEquals(Optional.of(GameState.CHECK_MATE), result);
    }

    @Test
    void inCheckWithMovesLegalNotEndsGame() {
        // Act
        Optional<GameState> result = checkMate.evaluate(Contexts.con(true, true));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void withoutCheckAndWithoutMovesNotIsCheckMate() {
        // Act
        Optional<GameState> result = checkMate.evaluate(Contexts.con(false, false));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void withoutCheckWithMovesNotEndsGame() {
        // Act
        Optional<GameState> result = checkMate.evaluate(Contexts.con(false, true));

        // Assert
        assertTrue(result.isEmpty());
    }
}
