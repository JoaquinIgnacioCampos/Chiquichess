package core.rules;

import doubles.Contexts;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Solo importan los dos booleanos del contexto: no hace falta armar ninguna posición. */
class StalemateTest {

    private final Stalemate stalemate = new Stalemate();

    @Test
    void withoutCheckAndWithoutMovesLegalIsStalemate() {
        // Act
        Optional<GameState> result = stalemate.evaluate(Contexts.con(false, false));

        // Assert
        assertEquals(Optional.of(GameState.DRAW_STALEMATE), result);
    }

    @Test
    void inCheckAndWithoutMovesNotIsStalemateButMate() {
        // Act
        Optional<GameState> result = stalemate.evaluate(Contexts.con(true, false));

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void withMovesLegalNotIsStalemate() {
        // Assert
        assertTrue(stalemate.evaluate(Contexts.con(false, true)).isEmpty());
        assertTrue(stalemate.evaluate(Contexts.con(true, true)).isEmpty());
    }
}
