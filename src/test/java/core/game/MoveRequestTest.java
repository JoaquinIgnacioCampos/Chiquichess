package core.game;

import core.side.Side;
import core.board.Position;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests de los value objects simples del paquete juego (Movimiento, Jugador, excepción). */
class MoveRequestTest {

    @Test
    void aMovementNotAcceptsPositionsNull() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new MoveRequest(null, new Position(0, 0)));
        assertThrows(NullPointerException.class, () -> new MoveRequest(new Position(0, 0), null));
    }

    @Test
    void twoMovementsWithSameOriginAndDestinationAreEqual() {
        // Assert
        assertEquals(new MoveRequest(new Position(4, 1), new Position(4, 3)),
                new MoveRequest(new Position(4, 1), new Position(4, 3)));
    }

    @Test
    void aPlayerNotAcceptsNameNorColorNull() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new Player(null, Side.WHITE));
        assertThrows(NullPointerException.class, () -> new Player("Ana", null));
    }

    @Test
    void exceptionSavesReason() {
        // Act
        InvalidMoveException e =
                new InvalidMoveException(InvalidMoveException.Reason.NOT_YOUR_TURN);

        // Assert
        assertEquals(InvalidMoveException.Reason.NOT_YOUR_TURN, e.reason());
        assertEquals("NOT_YOUR_TURN", e.getMessage());
    }
}
