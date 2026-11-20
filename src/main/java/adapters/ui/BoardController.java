package adapters.ui;

import core.side.Side;
import core.game.MoveRequest;
import core.game.InvalidActionException;
import core.game.InvalidMoveException;
import core.game.GameInProgress;
import core.game.Player;
import core.game.GameObserver;
import core.game.GameResult;
import core.pieces.Piece;
import core.rules.GameState;
import core.rules.Scoreboard;
import core.board.IBoard;
import core.board.Position;
import adapters.ArtificialRival;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Interacción de presentación: selecciona casillas y envía órdenes; no calcula reglas.
 *
 * Contra la IA, solo deja actuar a la persona: no se pueden tocar piezas mientras le toca a la IA, ni
 * rendirse u ofrecer tablas en su nombre. Deshacer y rehacer van de a dos jugadas (ver {@link RivalArtificial}).
 */
public final class BoardController {
    private final GameInProgress game;
    private final Optional<ArtificialRival> rival;
    private Optional<Position> selection = Optional.empty();
    /** Movimiento de un peón a la última fila que espera la elección de la pieza. */
    private Optional<MoveRequest> promotion = Optional.empty();

    public BoardController(GameInProgress game) {
        this(game, Optional.empty());
    }

    public BoardController(GameInProgress game, Optional<ArtificialRival> rival) {
        this.game = Objects.requireNonNull(game, "game");
        this.rival = Objects.requireNonNull(rival, "rival");
    }

    /** true si ese bando lo juega una persona (en una partida entre amigos, los dos). */
    public boolean isHuman(Side color) {
        return rival.map(ia -> ia.side() != color).orElse(true);
    }

    /** true si la partida sigue y le toca a la IA: la vista lo muestra como "pensando". */
    public boolean isAiTurn() {
        return rival.map(ArtificialRival::himPlays).orElse(false);
    }

    /**
     * Suscribe a la vista a los eventos de la partida: así se entera de lo que pasa aunque no lo haya
     * provocado ella (por ejemplo, el final por tiempo que detecta el reloj).
     */
    public void listen(GameObserver observer) {
        game.addObserver(observer);
    }

    public IBoard board() {
        return game.board();
    }

    public Player player(Side color) {
        return game.player(color);
    }

    public Side turn() {
        return game.turn();
    }

    public GameState state() {
        return game.state();
    }

    public Optional<GameResult> result() {
        return game.result();
    }

    /** Goles de cada bando si las reglas se juegan con goles (el modo fútbol); vacío en el ajedrez clásico. */
    public Optional<Scoreboard> scoreboard() {
        return game.scoreboard();
    }

    /** Jugadas a las que termina la partida (modo fútbol); vacío si no hay límite. */
    public Optional<Integer> moveLimit() {
        return game.moveLimit();
    }

    /** Cantidad de jugadas hechas en la partida (los dos bandos juntos). */
    public int movesMade() {
        return game.history().size();
    }

    public Optional<Duration> timeRemaining(Side color) {
        return game.timeRemaining(color);
    }

    public void verifyTime() {
        game.verifyTime();
    }

    public Optional<Side> offerDrawPending() {
        return game.offerDrawPending();
    }

    /** Contra la IA, la oferta se responde enseguida: la IA acepta solo si va claramente perdiendo. */
    public Optional<InvalidActionException.Reason> offerDraw(Side color) {
        if (!isHuman(color)) return Optional.of(InvalidActionException.Reason.NOT_YOUR_TURN);
        return executeAction(() -> {
            game.offerDraw(color);
            rival.ifPresent(ArtificialRival::respondDraw);
        });
    }

    public Optional<InvalidActionException.Reason> acceptDraw(Side color) {
        return executeAction(() -> game.acceptDraw(color));
    }

    public Optional<InvalidActionException.Reason> rejectDraw(Side color) {
        return executeAction(() -> game.rejectDraw(color));
    }

    private Optional<InvalidActionException.Reason> executeAction(Runnable action) {
        try {
            action.run();
            selected();
            return Optional.empty();
        } catch (InvalidActionException rejection) {
            return Optional.of(rejection.reason());
        }
    }

    /** true si la partida admite deshacer (se eligió la opción y no tiene tiempo): define si hay botones. */
    public boolean allowsUndo() {
        return game.allowsUndo();
    }

    public boolean canUndo() {
        return game.canUndo();
    }

    public boolean canRedo() {
        return game.canRedo();
    }

    /**
     * Deshace la última jugada (contra la IA, hasta que vuelva a tocarle a la persona); la selección se
     * descarta porque el tablero cambió.
     */
    public Optional<InvalidActionException.Reason> undo() {
        return executeAndClearSelection(rival.<Runnable>map(ia -> ia::undo).orElse(game::undo));
    }

    public Optional<InvalidActionException.Reason> redo() {
        return executeAndClearSelection(rival.<Runnable>map(ia -> ia::redo).orElse(game::redo));
    }

    private Optional<InvalidActionException.Reason> executeAndClearSelection(Runnable action) {
        try {
            action.run();
            selection = Optional.empty();
            promotion = Optional.empty();
            return Optional.empty();
        } catch (InvalidActionException rejection) {
            return Optional.of(rejection.reason());
        }
    }

    /** El núcleo valida la orden y determina el ganador. */
    public Optional<InvalidActionException.Reason> resign(Side color) {
        if (!isHuman(color)) return Optional.of(InvalidActionException.Reason.NOT_YOUR_TURN);
        game.verifyTime();
        try {
            game.resign(color);
            selection = Optional.empty();
            return Optional.empty();
        } catch (InvalidActionException rejection) {
            return Optional.of(rejection.reason());
        }
    }

    /** Descarta una selección que perdió vigencia por una actualización externa del núcleo. */
    public Optional<Position> selected() {
        selection = selection.filter(position -> !game.state().isFinished()
                && game.board().pieceIn(position)
                .map(piece -> piece.color() == game.turn()).orElse(false));
        return selection;
    }

    public Set<Position> destinationsLegal() {
        return selected().map(origin -> Set.copyOf(game.movementsLegal(origin)))
                .orElseGet(Set::of);
    }

    /**
     * Piezas entre las que tiene que elegir el jugador porque llevó un peón a la última fila; vacío
     * si no hay una promoción esperando. Si la partida cambió (deshacer, tiempo, rendición), la
     * promoción pendiente se descarta.
     */
    public List<Piece> promotionPending() {
        List<Piece> options = promotion
                .map(movement -> game.promotionOptions(movement.origin(), movement.destination()))
                .orElseGet(List::of);
        if (options.isEmpty()) {
            promotion = Optional.empty();
        }
        return options;
    }

    /**
     * Completa la promoción pendiente con la pieza elegida (por su código: 'D', 'T', 'A', 'C').
     *
     * @throws IllegalStateException si no hay una promoción esperando
     */
    public Optional<InvalidMoveException.Reason> promote(char code) {
        if (promotionPending().isEmpty()) {
            throw new IllegalStateException("There is no pending promotion");
        }
        MoveRequest chosen = MoveRequest.withPromotion(promotion.get().origin(), promotion.get().destination(), code);
        promotion = Optional.empty();
        return move(chosen);
    }

    /** El jugador se arrepintió: el peón sigue donde estaba y la selección se descarta. */
    public void cancelPromotion() {
        promotion = Optional.empty();
    }

    /** Devuelve el motivo de rechazo del núcleo, si hubo un intento de movimiento rechazado. */
    public Optional<InvalidMoveException.Reason> press(Position position) {
        Objects.requireNonNull(position, "position");
        promotion = Optional.empty();
        if (game.state().isFinished()) {
            selection = Optional.empty();
            return Optional.of(InvalidMoveException.Reason.FINISHED_GAME);
        }
        if (isAiTurn()) {
            // Mientras piensa la IA, el tablero no responde: sus piezas no se tocan.
            selection = Optional.empty();
            return Optional.of(InvalidMoveException.Reason.NOT_YOUR_TURN);
        }
        Optional<Position> origin = selected();
        if (origin.isPresent() && destinationsLegal().contains(position)) {
            selection = Optional.empty();
            // Si el peón llega a la última fila, primero hay que preguntar a qué pieza promueve.
            if (!game.promotionOptions(origin.get(), position).isEmpty()) {
                promotion = Optional.of(new MoveRequest(origin.get(), position));
                return Optional.empty();
            }
            return move(new MoveRequest(origin.get(), position));
        }
        selection = game.board().pieceIn(position)
                .filter(piece -> piece.color() == game.turn())
                .map(piece -> position);
        return Optional.empty();
    }

    private Optional<InvalidMoveException.Reason> move(MoveRequest movement) {
        try {
            game.move(movement);
            return Optional.empty();
        } catch (InvalidMoveException rejection) {
            return Optional.of(rejection.reason());
        }
    }
}
