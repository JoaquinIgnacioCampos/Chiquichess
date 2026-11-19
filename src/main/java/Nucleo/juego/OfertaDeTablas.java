package Nucleo.juego;

import Nucleo.bando.Bando;

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
final class OfertaDeTablas {

    private Optional<Bando> oferente = Optional.empty();

    Optional<Bando> pendiente() {
        return oferente;
    }

    /** @throws AccionInvalidaException si no es el turno de ese bando o ya hay una oferta pendiente */
    void ofrecer(Bando color, Bando turno) {
        Objects.requireNonNull(color, "color");
        if (color != turno) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.NO_ES_SU_TURNO);
        }
        if (oferente.isPresent()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.OFERTA_PENDIENTE);
        }
        oferente = Optional.of(color);
    }

    /**
     * Controla que ese bando pueda responder: tiene que haber una oferta y no puede ser la suya.
     *
     * @throws AccionInvalidaException si no hay oferta o es la propia
     */
    void validarRespuesta(Bando color) {
        Objects.requireNonNull(color, "color");
        if (oferente.isEmpty()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.SIN_OFERTA);
        }
        if (oferente.get() == color) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.ES_SU_PROPIA_OFERTA);
        }
    }

    /** Si movió el rival del que ofreció, la oferta queda rechazada. */
    void alMover(Bando quienMovio) {
        if (oferente.filter(color -> color != quienMovio).isPresent()) {
            descartar();
        }
    }

    /** Ya no hay oferta: se rechazó, terminó la partida o cambió la posición al deshacer o rehacer. */
    void descartar() {
        oferente = Optional.empty();
    }
}
