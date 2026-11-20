package adapters;

import core.game.InvalidMoveException;
import core.rules.FinishReason;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Si el núcleo suma un código nuevo y nadie le escribe un texto para la consola, estos tests fallan. */
class ConsoleMessagesTest {

    private final ConsoleMessages messages = new ConsoleMessages();

    @Test
    void allRejectionOfMovementHasItsText() {
        for (InvalidMoveException.Reason reason : InvalidMoveException.Reason.values()) {
            assertNotEquals(ConsoleMessages.WITHOUT_TEXT, messages.moveRejection(reason),
                    "Falta el texto de " + reason);
        }
    }

    @Test
    void allRejectionOfActionHasItsText() {
        for (core.game.InvalidActionException.Reason reason : core.game.InvalidActionException.Reason.values()) {
            assertNotEquals(ConsoleMessages.WITHOUT_TEXT, messages.actionRejection(reason),
                    "Falta el texto de " + reason);
        }
    }

    @Test
    void allReasonOfFinalHasItsText() {
        for (FinishReason reason : FinishReason.values()) {
            assertFalse(messages.gameEnd(reason, Optional.of("Ana")).startsWith(ConsoleMessages.WITHOUT_TEXT),
                    "Falta el texto de " + reason);
        }
    }

    @Test
    void finalWithWinnerIncludeItsName() {
        // Assert
        assertEquals("Jaque mate! Gana Ana.", messages.gameEnd(FinishReason.CHECK_MATE, Optional.of("Ana")));
        assertEquals("Gana Ana por tiempo.", messages.gameEnd(FinishReason.TIME_EXHAUSTED, Optional.of("Ana")));
        assertEquals("Tablas por acuerdo.", messages.gameEnd(FinishReason.AGREEMENT, Optional.empty()));
    }
}
