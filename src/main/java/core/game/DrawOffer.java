package core.game;

import core.side.Side;

import java.util.Objects;
import java.util.Optional;

/**
 * La oferta de tablas de una partida: quién la hizo, si hay una pendiente, y las reglas para
 * ofrecer y responder.
 *
 * Solo el que tiene el turno puede ofrecer, y no puede haber dos ofertas a la vez. Responde el
 * otro jugador; nadie puede aceptar o rechazar su propia oferta. Si el que ofreció mueve, la oferta
 * sigue en pie; si mueve el rival, la rechazó al jugar.
 *
 * Que la partida esté en curso lo controla Partida antes de llamar: acá solo vive la oferta.
 */
final class DrawOffer {

    private Optional<Side> offerer = Optional.empty();

    Optional<Side> pending() {
        return offerer;
    }

    /** @throws AccionInvalidaException si no es el turno de ese bando o ya hay una oferta pendiente */
    void offer(Side color, Side turn) {
        Objects.requireNonNull(color, "color");
        if (color != turn) {
            throw new InvalidActionException(InvalidActionException.Reason.NOT_YOUR_TURN);
        }
        if (offerer.isPresent()) {
            throw new InvalidActionException(InvalidActionException.Reason.OFFER_PENDING);
        }
        offerer = Optional.of(color);
    }

    /**
     * Controla que ese bando pueda responder: tiene que haber una oferta y no puede ser la suya.
     *
     * @throws AccionInvalidaException si no hay oferta o es la propia
     */
    void validateResponse(Side color) {
        Objects.requireNonNull(color, "color");
        if (offerer.isEmpty()) {
            throw new InvalidActionException(InvalidActionException.Reason.WITHOUT_OFFER);
        }
        if (offerer.get() == color) {
            throw new InvalidActionException(InvalidActionException.Reason.IS_OWN_OFFER);
        }
    }

    /** Si movió el rival del que ofreció, la oferta queda rechazada. */
    void onMove(Side whoMoved) {
        if (offerer.filter(color -> color != whoMoved).isPresent()) {
            discard();
        }
    }

    /** Ya no hay oferta: se rechazó, terminó la partida o cambió la posición al deshacer o rehacer. */
    void discard() {
        offerer = Optional.empty();
    }
}
