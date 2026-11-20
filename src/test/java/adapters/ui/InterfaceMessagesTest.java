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
                    "Falta el texto de " + reason);
        }
    }

    @Test
    void allRejectionOfActionHasItsTextInEachAction() {
        for (InterfaceMessages.Action action : InterfaceMessages.Action.values()) {
            for (InvalidActionException.Reason reason : InvalidActionException.Reason.values()) {
                assertNotEquals(InterfaceMessages.WITHOUT_TEXT, messages.actionRejection(action, reason),
                        "Falta el texto de " + reason + " al " + action);
            }
        }
    }

    @Test
    void allReasonOfFinalHasItsText() {
        for (FinishReason reason : FinishReason.values()) {
            assertNotEquals(InterfaceMessages.WITHOUT_TEXT, messages.finalReason(reason), "Falta el texto de " + reason);
        }
    }

    @Test
    void outsideOfTurnExplainsAccordingItThatAttempt() {
        // Act
        String onResign = messages.actionRejection(InterfaceMessages.Action.RESIGN,
                InvalidActionException.Reason.NO_ES_SU_TURNO);
        String onOfferDraw = messages.actionRejection(InterfaceMessages.Action.DRAW,
                InvalidActionException.Reason.NO_ES_SU_TURNO);

        // Assert
        assertEquals("Solo podés rendirte durante tu turno.", onResign);
        assertEquals("Solo podés ofrecer tablas durante tu turno.", onOfferDraw);
    }

    @Test
    void withoutTextSpecificUsesGeneralOfReason() {
        // Act
        String texto = messages.actionRejection(InterfaceMessages.Action.RESIGN,
                InvalidActionException.Reason.PARTIDA_FINALIZADA);

        // Assert
        assertEquals("La partida ya terminó.", texto);
    }
}
