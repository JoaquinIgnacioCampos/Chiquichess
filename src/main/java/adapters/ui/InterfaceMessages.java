package adapters.ui;

import core.game.InvalidActionException;
import core.game.InvalidMoveException;
import core.rules.FinishReason;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Catálogo de los textos que la interfaz muestra para los códigos del núcleo (rechazos y finales).
 *
 * El núcleo informa con códigos y cada adaptador elige sus palabras, por eso los textos viven acá y
 * no en el núcleo. Juntarlos en un solo lugar evita que cada vista tenga su propio switch: agregar
 * un código nuevo es sumar una línea en este catálogo, sin tocar ninguna vista. Si falta un texto se
 * muestra {@link #WITHOUT_TEXT}; InterfaceMessagesTest verifica que no falte ninguno.
 */
public final class InterfaceMessages {

    /** Lo que se intentó hacer, para los rechazos cuyo texto depende de la acción. */
    public enum Action { RESIGN, DRAW, UNDO }

    static final String WITHOUT_TEXT = "No se pudo completar la acción.";

    private final Map<InvalidMoveException.Reason, String> movements =
            new EnumMap<>(InvalidMoveException.Reason.class);
    private final Map<InvalidActionException.Reason, String> actions =
            new EnumMap<>(InvalidActionException.Reason.class);
    private final Map<Action, Map<InvalidActionException.Reason, String>> byAction = new EnumMap<>(Action.class);
    private final Map<FinishReason, String> finalValue = new EnumMap<>(FinishReason.class);

    public InterfaceMessages() {
        movements.put(InvalidMoveException.Reason.SIN_PIEZA_EN_ORIGEN, "Ya no hay una pieza en la casilla seleccionada.");
        movements.put(InvalidMoveException.Reason.NO_ES_SU_TURNO, "Es el turno del otro jugador.");
        movements.put(InvalidMoveException.Reason.MOVIMIENTO_NO_PERMITIDO, "El núcleo rechazó ese movimiento.");
        movements.put(InvalidMoveException.Reason.DEJA_AL_REY_EN_JAQUE, "Esa jugada deja a tu rey en jaque.");
        movements.put(InvalidMoveException.Reason.PARTIDA_FINALIZADA, "La partida ya terminó.");

        actions.put(InvalidActionException.Reason.PARTIDA_FINALIZADA, "La partida ya terminó.");
        actions.put(InvalidActionException.Reason.NO_ES_SU_TURNO, "Es el turno del otro jugador.");
        actions.put(InvalidActionException.Reason.OFERTA_PENDIENTE, "Ya hay una oferta de tablas pendiente.");
        actions.put(InvalidActionException.Reason.SIN_OFERTA, "La oferta de tablas ya no está disponible.");
        actions.put(InvalidActionException.Reason.ES_SU_PROPIA_OFERTA, "Solo el rival puede responder a esta oferta.");
        actions.put(InvalidActionException.Reason.DESHACER_NO_PERMITIDO, "Esta partida no permite deshacer jugadas.");
        actions.put(InvalidActionException.Reason.NADA_PARA_DESHACER, "No hay jugadas para deshacer.");
        actions.put(InvalidActionException.Reason.NADA_PARA_REHACER, "No hay jugadas para rehacer.");

        byAction(Action.RESIGN).put(InvalidActionException.Reason.NO_ES_SU_TURNO, "Solo podés rendirte durante tu turno.");
        byAction(Action.DRAW).put(InvalidActionException.Reason.NO_ES_SU_TURNO, "Solo podés ofrecer tablas durante tu turno.");

        finalValue.put(FinishReason.CHECK_MATE, "Por jaque mate");
        finalValue.put(FinishReason.STALEMATE, "Por ahogado");
        finalValue.put(FinishReason.REPETITION, "Por repetición");
        finalValue.put(FinishReason.RULE_50_MOVEMENTS, "Regla de los 50 movimientos");
        finalValue.put(FinishReason.RESIGNATION, "Por rendición");
        finalValue.put(FinishReason.TIME_EXHAUSTED, "Por tiempo agotado");
        finalValue.put(FinishReason.AGREEMENT, "Por acuerdo");
        finalValue.put(FinishReason.GOALS, "Por goles");
        finalValue.put(FinishReason.TIED_ON_GOALS, "Empate en goles");
    }

    private Map<InvalidActionException.Reason, String> byAction(Action action) {
        return byAction.computeIfAbsent(action, a -> new EnumMap<>(InvalidActionException.Reason.class));
    }

    public String moveRejection(InvalidMoveException.Reason reason) {
        return movements.getOrDefault(Objects.requireNonNull(reason, "motivo"), WITHOUT_TEXT);
    }

    /** Texto específico de la acción si lo hay; si no, el texto general del motivo. */
    public String actionRejection(Action action, InvalidActionException.Reason reason) {
        Objects.requireNonNull(reason, "motivo");
        String specific = byAction.getOrDefault(Objects.requireNonNull(action, "accion"), Map.of()).get(reason);
        return specific != null ? specific : actions.getOrDefault(reason, WITHOUT_TEXT);
    }

    public String finalReason(FinishReason reason) {
        return finalValue.getOrDefault(Objects.requireNonNull(reason, "motivo"), WITHOUT_TEXT);
    }
}
