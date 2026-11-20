package adapters;

import core.game.InvalidActionException;
import core.game.InvalidMoveException;
import core.rules.FinishReason;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo de los textos que la consola muestra para los códigos del núcleo (rechazos y finales).
 * La consola usa sus propias palabras (sin tildes), distintas de las de la interfaz gráfica.
 * Agregar un código nuevo es sumar una línea acá; ConsoleMessagesTest verifica que no falte ninguno.
 */
public final class ConsoleMessages {

    static final String WITHOUT_TEXT = "No se pudo completar la accion.";

    private final Map<InvalidMoveException.Reason, String> movements =
            new EnumMap<>(InvalidMoveException.Reason.class);
    private final Map<InvalidActionException.Reason, String> actions =
            new EnumMap<>(InvalidActionException.Reason.class);
    /** Finales; "%s" se reemplaza por el nombre del ganador. */
    private final Map<FinishReason, String> finalValue = new EnumMap<>(FinishReason.class);

    public ConsoleMessages() {
        actions.put(InvalidActionException.Reason.PARTIDA_FINALIZADA, "La partida ya termino.");
        actions.put(InvalidActionException.Reason.NO_ES_SU_TURNO, "No es su turno.");
        actions.put(InvalidActionException.Reason.OFERTA_PENDIENTE, "Ya hay una oferta de tablas pendiente.");
        actions.put(InvalidActionException.Reason.SIN_OFERTA, "No hay ninguna oferta de tablas.");
        actions.put(InvalidActionException.Reason.ES_SU_PROPIA_OFERTA, "Solo el rival puede responder la oferta.");
        actions.put(InvalidActionException.Reason.DESHACER_NO_PERMITIDO, "Esta partida no permite deshacer jugadas.");
        actions.put(InvalidActionException.Reason.NADA_PARA_DESHACER, "No hay jugadas para deshacer.");
        actions.put(InvalidActionException.Reason.NADA_PARA_REHACER, "No hay jugadas para rehacer.");

        movements.put(InvalidMoveException.Reason.SIN_PIEZA_EN_ORIGEN, "No hay ninguna pieza en la casilla de origen.");
        movements.put(InvalidMoveException.Reason.NO_ES_SU_TURNO, "Esa pieza no es suya: es el turno del otro jugador.");
        movements.put(InvalidMoveException.Reason.MOVIMIENTO_NO_PERMITIDO, "Esa pieza no puede moverse ahi.");
        movements.put(InvalidMoveException.Reason.DEJA_AL_REY_EN_JAQUE, "Movimiento ilegal: deja a su rey en jaque.");
        movements.put(InvalidMoveException.Reason.PARTIDA_FINALIZADA, "La partida ya termino.");

        finalValue.put(FinishReason.CHECK_MATE, "Jaque mate! Gana %s.");
        finalValue.put(FinishReason.STALEMATE, "Tablas por ahogado: el jugador no tiene jugadas pero no esta en jaque.");
        finalValue.put(FinishReason.REPETITION, "Tablas por repeticion de posicion.");
        finalValue.put(FinishReason.RULE_50_MOVEMENTS, "Tablas por la regla de los 50 movimientos.");
        finalValue.put(FinishReason.RESIGNATION, "Gana %s por rendicion.");
        finalValue.put(FinishReason.TIME_EXHAUSTED, "Gana %s por tiempo.");
        finalValue.put(FinishReason.AGREEMENT, "Tablas por acuerdo.");
        finalValue.put(FinishReason.GOALS, "Gana %s por goles.");
        finalValue.put(FinishReason.TIED_ON_GOALS, "Empate: se llego al limite de jugadas con los goles iguales.");
    }

    public String moveRejection(InvalidMoveException.Reason reason) {
        return movements.getOrDefault(Objects.requireNonNull(reason, "motivo"), WITHOUT_TEXT);
    }

    public String actionRejection(InvalidActionException.Reason reason) {
        return actions.getOrDefault(Objects.requireNonNull(reason, "motivo"), WITHOUT_TEXT);
    }

    /** Mensaje de fin de partida, con el nombre del ganador si lo hubo. */
    public String gameEnd(FinishReason reason, Optional<String> winner) {
        String template = finalValue.getOrDefault(Objects.requireNonNull(reason, "motivo"), WITHOUT_TEXT);
        return String.format(template, winner.orElse("?"));
    }
}
