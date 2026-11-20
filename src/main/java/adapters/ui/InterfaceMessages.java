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
 * muestra {@link #SIN_TEXTO}; MensajesDeLaInterfazTest verifica que no falte ninguno.
 */
public final class InterfaceMessages {

    /** Lo que se intentó hacer, para los rechazos cuyo texto depende de la acción. */
    public enum Action { RESIGN, DRAW, UNDO }

    static final String WITHOUT_TEXT = "The action could not be completed.";

    private final Map<InvalidMoveException.Reason, String> movements =
            new EnumMap<>(InvalidMoveException.Reason.class);
    private final Map<InvalidActionException.Reason, String> actions =
            new EnumMap<>(InvalidActionException.Reason.class);
    private final Map<Action, Map<InvalidActionException.Reason, String>> byAction = new EnumMap<>(Action.class);
    private final Map<FinishReason, String> finalValue = new EnumMap<>(FinishReason.class);

    public InterfaceMessages() {
        movements.put(InvalidMoveException.Reason.NO_PIECE_AT_ORIGIN, "There is no longer a piece on the selected square.");
        movements.put(InvalidMoveException.Reason.NOT_YOUR_TURN, "It is the other player's turn.");
        movements.put(InvalidMoveException.Reason.MOVE_NOT_ALLOWED, "The core rejected that move.");
        movements.put(InvalidMoveException.Reason.LEAVES_KING_IN_CHECK, "That move leaves your king in check.");
        movements.put(InvalidMoveException.Reason.FINISHED_GAME, "The game is already over.");

        actions.put(InvalidActionException.Reason.FINISHED_GAME, "The game is already over.");
        actions.put(InvalidActionException.Reason.NOT_YOUR_TURN, "It is the other player's turn.");
        actions.put(InvalidActionException.Reason.OFFER_PENDING, "There is already a pending draw offer.");
        actions.put(InvalidActionException.Reason.WITHOUT_OFFER, "The draw offer is no longer available.");
        actions.put(InvalidActionException.Reason.IS_OWN_OFFER, "Only the rival can respond to this offer.");
        actions.put(InvalidActionException.Reason.UNDO_NOT_ALLOWED, "This game does not allow undoing moves.");
        actions.put(InvalidActionException.Reason.NOTHING_TO_UNDO, "There are no moves to undo.");
        actions.put(InvalidActionException.Reason.NOTHING_TO_REDO, "There are no moves to redo.");

        byAction(Action.RESIGN).put(InvalidActionException.Reason.NOT_YOUR_TURN, "You can only resign during your turn.");
        byAction(Action.DRAW).put(InvalidActionException.Reason.NOT_YOUR_TURN, "You can only offer a draw during your turn.");

        finalValue.put(FinishReason.CHECK_MATE, "By checkmate");
        finalValue.put(FinishReason.STALEMATE, "By stalemate");
        finalValue.put(FinishReason.REPETITION, "By repetition");
        finalValue.put(FinishReason.RULE_50_MOVEMENTS, "50-move rule");
        finalValue.put(FinishReason.RESIGNATION, "By resignation");
        finalValue.put(FinishReason.TIME_EXHAUSTED, "On time");
        finalValue.put(FinishReason.AGREEMENT, "By agreement");
        finalValue.put(FinishReason.GOALS, "On goals");
        finalValue.put(FinishReason.TIED_ON_GOALS, "Tied on goals");
    }

    private Map<InvalidActionException.Reason, String> byAction(Action action) {
        return byAction.computeIfAbsent(action, a -> new EnumMap<>(InvalidActionException.Reason.class));
    }

    public String moveRejection(InvalidMoveException.Reason reason) {
        return movements.getOrDefault(Objects.requireNonNull(reason, "reason"), WITHOUT_TEXT);
    }

    /** Texto específico de la acción si lo hay; si no, el texto general del motivo. */
    public String actionRejection(Action action, InvalidActionException.Reason reason) {
        Objects.requireNonNull(reason, "reason");
        String specific = byAction.getOrDefault(Objects.requireNonNull(action, "action"), Map.of()).get(reason);
        return specific != null ? specific : actions.getOrDefault(reason, WITHOUT_TEXT);
    }

    public String finalReason(FinishReason reason) {
        return finalValue.getOrDefault(Objects.requireNonNull(reason, "reason"), WITHOUT_TEXT);
    }
}
