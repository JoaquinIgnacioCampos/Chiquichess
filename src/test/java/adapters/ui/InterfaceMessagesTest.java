package adapters.ui;

import core.game.InvalidActionException;
import core.game.InvalidMoveException;
import core.rules.FinishReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reemplaza la alarma que daba el switch exhaustivo: si el núcleo suma un código nuevo y nadie le
 * escribe un texto en el catálogo, estos tests fallan y dicen cuál falta.
 */
class InterfaceMessagesTest {

    private final InterfaceMessages messages = new InterfaceMessages();

    @Test
    void allRejectionOfMovementHasItsText() {
        for (InvalidMoveException.Reason reason : InvalidMoveException.Reason.values()) {
            assertNotEquals(InterfaceMessages.WITHOUT_TEXT, messages.moveRejection(reason),
                    "Missing the text for " + reason);
        }
    }

    @Test
    void allRejectionOfActionHasItsTextInEachAction() {
        for (InterfaceMessages.Action action : InterfaceMessages.Action.values()) {
            for (InvalidActionException.Reason reason : InvalidActionException.Reason.values()) {
                assertNotEquals(InterfaceMessages.WITHOUT_TEXT, messages.actionRejection(action, reason),
                        "Missing the text for " + reason + " to the " + action);
            }
        }
    }

    @Test
    void allReasonOfFinalHasItsText() {
        for (FinishReason reason : FinishReason.values()) {
            assertNotEquals(InterfaceMessages.WITHOUT_TEXT, messages.finalReason(reason), "Missing the text for " + reason);
        }
    }

    @Test
    void outsideOfTurnExplainsAccordingItThatAttempt() {
        // Act
        String onResign = messages.actionRejection(InterfaceMessages.Action.RESIGN,
                InvalidActionException.Reason.NOT_YOUR_TURN);
        String onOfferDraw = messages.actionRejection(InterfaceMessages.Action.DRAW,
                InvalidActionException.Reason.NOT_YOUR_TURN);

        // Assert
        assertEquals("You can only resign during your turn.", onResign);
        assertEquals("You can only offer a draw during your turn.", onOfferDraw);
    }

    @Test
    void withoutTextSpecificUsesGeneralOfReason() {
        // Act
        String texto = messages.actionRejection(InterfaceMessages.Action.RESIGN,
                InvalidActionException.Reason.FINISHED_GAME);

        // Assert
        assertEquals("The game is already over.", texto);
    }
}
