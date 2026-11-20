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
 * Agregar un código nuevo es sumar una línea acá; MensajesDeConsolaTest verifica que no falte ninguno.
 */
public final class ConsoleMessages {

    static final String WITHOUT_TEXT = "The action could not be completed.";

    private final Map<InvalidMoveException.Reason, String> movements =
            new EnumMap<>(InvalidMoveException.Reason.class);
    private final Map<InvalidActionException.Reason, String> actions =
            new EnumMap<>(InvalidActionException.Reason.class);
    /** Finales; "%s" se reemplaza por el nombre del ganador. */
    private final Map<FinishReason, String> finalValue = new EnumMap<>(FinishReason.class);

    public ConsoleMessages() {
        actions.put(InvalidActionException.Reason.FINISHED_GAME, "The game is already over.");
        actions.put(InvalidActionException.Reason.NOT_YOUR_TURN, "It is not your turn.");
        actions.put(InvalidActionException.Reason.OFFER_PENDING, "There is already a pending draw offer.");
        actions.put(InvalidActionException.Reason.WITHOUT_OFFER, "There is no draw offer.");
        actions.put(InvalidActionException.Reason.IS_OWN_OFFER, "Only the rival can respond to the offer.");
        actions.put(InvalidActionException.Reason.UNDO_NOT_ALLOWED, "This game does not allow undoing moves.");
        actions.put(InvalidActionException.Reason.NOTHING_TO_UNDO, "There are no moves to undo.");
        actions.put(InvalidActionException.Reason.NOTHING_TO_REDO, "There are no moves to redo.");

        movements.put(InvalidMoveException.Reason.NO_PIECE_AT_ORIGIN, "There is no piece on the origin square.");
        movements.put(InvalidMoveException.Reason.NOT_YOUR_TURN, "That piece is not yours: it is the other player's turn.");
        movements.put(InvalidMoveException.Reason.MOVE_NOT_ALLOWED, "That piece cannot move there.");
        movements.put(InvalidMoveException.Reason.LEAVES_KING_IN_CHECK, "Illegal move: leaves your king in check.");
        movements.put(InvalidMoveException.Reason.FINISHED_GAME, "The game is already over.");

        finalValue.put(FinishReason.CHECK_MATE, "Checkmate! %s wins.");
        finalValue.put(FinishReason.STALEMATE, "Draw by stalemate: the player has no moves but is not in check.");
        finalValue.put(FinishReason.REPETITION, "Draw by repetition of position.");
        finalValue.put(FinishReason.RULE_50_MOVEMENTS, "Draw by the 50-move rule.");
        finalValue.put(FinishReason.RESIGNATION, "%s wins by resignation.");
        finalValue.put(FinishReason.TIME_EXHAUSTED, "%s wins on time.");
        finalValue.put(FinishReason.AGREEMENT, "Draw by agreement.");
        finalValue.put(FinishReason.GOALS, "%s wins on goals.");
        finalValue.put(FinishReason.TIED_ON_GOALS, "Draw: the move limit was reached with equal goals.");
    }

    public String moveRejection(InvalidMoveException.Reason reason) {
        return movements.getOrDefault(Objects.requireNonNull(reason, "reason"), WITHOUT_TEXT);
    }

    public String actionRejection(InvalidActionException.Reason reason) {
        return actions.getOrDefault(Objects.requireNonNull(reason, "reason"), WITHOUT_TEXT);
    }

    /** Mensaje de fin de partida, con el nombre del ganador si lo hubo. */
    public String gameEnd(FinishReason reason, Optional<String> winner) {
        String template = finalValue.getOrDefault(Objects.requireNonNull(reason, "reason"), WITHOUT_TEXT);
        return String.format(template, winner.orElse("?"));
    }
}
