package core.game;

import core.side.Side;
import core.moves.Move;
import core.pieces.Piece;
import core.rules.GameState;
import core.rules.Scoreboard;
import core.rules.Memento;
import core.rules.LegalMoves;
import core.rules.GameRules;
import core.board.IBoard;
import core.board.Position;
import core.board.Board;
import core.time.GameClock;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Orquesta una partida: turnos, validación y ejecución de jugadas, deshacer y rehacer, y los
 * finales decididos por los jugadores (rendición, acuerdo) o por el reloj.
 *
 * Recibe sus colaboradores por constructor (inyección de dependencias), por lo que se puede testear
 * con cualquier tablero armado a mano. Delega en clases propias lo que no es coordinar:
 * <ul>
 *   <li>{@link ArbitroDeFinales}: si la partida terminó después de una jugada, y la memoria de las condiciones;</li>
 *   <li>{@link HistorialDeJugadas}: las jugadas hechas y deshechas;</li>
 *   <li>{@link OfertaDeTablas}: la oferta pendiente y sus reglas;</li>
 *   <li>{@link AvisosDePartida}: los observadores.</li>
 * </ul>
 */
public class Game implements GameInProgress {

    private final Board board;
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final GameRules rules;
    private final GameClock clock;
    private final EndingReferee referee;
    /** Jugadas hechas y deshechas, con los recuerdos de cada una: Partida es el cuidador del Memento. */
    private final MoveHistory moveHistory = new MoveHistory();
    private final DrawOffer drawOffer = new DrawOffer();
    private final GameNotices notices = new GameNotices();

    private Side turn = Side.WHITE;
    private GameState state;
    private Optional<GameResult> result = Optional.empty();
    /** true si el final lo decidió una jugada (mate o tablas automáticas): ese final se puede deshacer. */
    private boolean endedByMove = false;

    /**
     * Las reglas (movimientos legales, detector y condiciones de fin) llegan agrupadas en
     * {@link ReglasDePartida}; el reloj pertenece a esta única partida. Para jugar sin reloj se
     * pasa {@link Nucleo.tiempo.SinReloj}.
     */
    public Game(Board board, Player whitePlayer, Player blackPlayer,
                   GameRules rules, GameClock clock) {
        this.board = Objects.requireNonNull(board, "tablero");
        this.whitePlayer = Objects.requireNonNull(whitePlayer, "jugadorBlanco");
        this.blackPlayer = Objects.requireNonNull(blackPlayer, "jugadorNegro");
        this.rules = Objects.requireNonNull(rules, "reglas");
        this.clock = Objects.requireNonNull(clock, "reloj");
        if (whitePlayer.color() != Side.WHITE || blackPlayer.color() != Side.BLACK) {
            throw new IllegalArgumentException("Los jugadores no coinciden con sus colores");
        }
        this.referee = new EndingReferee(rules);
        this.clock.start(turn);
        // La posición inicial no se deshace: sus recuerdos no hace falta guardarlos.
        register(moveHistory.moves());
    }

    public IBoard board() {
        return board;
    }

    /** Suscribe un adaptador a los eventos de esta partida (ver {@link EventoDePartida}). */
    public void addObserver(GameObserver observer) {
        notices.add(observer);
    }

    public void removeObserver(GameObserver observer) {
        notices.remove(observer);
    }

    /** Avisa el final si la partida está terminada. Se llama después del aviso de la jugada que la terminó. */
    private void notifyIfEnded() {
        result.filter(r -> state.isFinished())
                .ifPresent(r -> notices.inform(new GameEvent.FinishedGame(r)));
    }

    public Side turn() {
        return turn;
    }

    public Player currentPlayer() {
        return player(turn);
    }

    /** Identidad inmutable de un bando, independiente del turno actual. */
    public Player player(Side color) {
        Objects.requireNonNull(color, "color");
        return color == Side.WHITE ? whitePlayer : blackPlayer;
    }

    public GameState state() {
        return state;
    }

    /** Quién ganó; vacío si la partida sigue o terminó en tablas. */
    public Optional<Player> winner() {
        return result.flatMap(GameResult::winner).map(this::player);
    }

    public Optional<GameResult> result() {
        return result;
    }

    /**
     * Jugadas (de los dos bandos) a las que termina la partida, si alguna condición de fin tiene ese
     * límite (el modo fútbol); vacío si no hay límite.
     */
    public Optional<Integer> moveLimit() {
        return referee.moveLimit();
    }

    /**
     * Goles de cada bando, contados en las jugadas ejecutadas, si alguna condición de fin se juega con
     * goles (el modo fútbol); vacío si no. Igual que el límite de jugadas, lo informa la condición.
     */
    public Optional<Scoreboard> scoreboard() {
        return referee.scoreboard(moveHistory.moves());
    }

    /** Foto inmutable de las jugadas ejecutadas, en orden; no incluye intentos ni otras órdenes. */
    public List<MoveRequest> history() {
        return moveHistory.moves().stream().map(MoveRequest::de).toList();
    }

    /**
     * Copia de la situación actual para que una IA la analice: tablero copiado, turno, jugadas hechas y
     * reglas. Lo que la IA haga sobre la copia no toca la partida ni dispara eventos.
     */
    public GameSituation situation() {
        return new GameSituation(board.copy(), turn, moveHistory.moves(), rules);
    }

    /**
     * Deshace la última jugada: tablero, turno, condiciones con memoria y estado vuelven a como
     * estaban antes de ella. Cualquiera de los dos jugadores puede hacerlo. Se puede deshacer una
     * jugada que terminó la partida (mate o tablas automáticas), pero no un final decidido por los
     * jugadores o por el reloj. Borra la oferta de tablas pendiente.
     *
     * @throws AccionInvalidaException si la partida no permite deshacer (no se habilitó o tiene
     *                                 límite de tiempo), si terminó por rendición, acuerdo o tiempo,
     *                                 o si no hay jugadas para deshacer
     */
    public void undo() {
        ensureUndoAllowed();
        if (result.isPresent() && !endedByMove) {
            throw new InvalidActionException(InvalidActionException.Reason.PARTIDA_FINALIZADA);
        }
        if (!moveHistory.hasUndo()) {
            throw new InvalidActionException(InvalidActionException.Reason.NADA_PARA_DESHACER);
        }
        MoveHistory.Entrada input = moveHistory.undo();
        input.move().revert(board);
        referee.restore(input.mementos());
        turn = turn.opposite();
        drawOffer.discard();
        result = Optional.empty();
        endedByMove = false;
        apply(referee.evaluate(board, turn, moveHistory.moves()));
        notices.inform(new GameEvent.MoveUndone(state));
    }

    /**
     * Vuelve a ejecutar la última jugada deshecha. Hacer una jugada nueva borra lo que se podía
     * rehacer. Borra la oferta de tablas pendiente.
     *
     * @throws AccionInvalidaException si la partida no permite deshacer, si terminó o si no hay
     *                                 jugadas para rehacer
     */
    public void redo() {
        ensureUndoAllowed();
        if (state.isFinished()) {
            throw new InvalidActionException(InvalidActionException.Reason.PARTIDA_FINALIZADA);
        }
        if (!moveHistory.hasRedo()) {
            throw new InvalidActionException(InvalidActionException.Reason.NADA_PARA_REHACER);
        }
        Move move = moveHistory.nextToRedo();
        move.apply(board);
        turn = turn.opposite();
        drawOffer.discard();
        moveHistory.confirmRedone(register(moveHistory.movesWith(move)));
        notices.inform(new GameEvent.MoveRedone(move.pieceMoved().color(),
                GameEvent.Trait.de(move), state));
        notifyIfEnded();
    }

    /** true si en este momento {@link #deshacer()} se aceptaría. Lo usa la UI para habilitar el botón. */
    public boolean canUndo() {
        return allowsUndo() && moveHistory.hasUndo()
                && (result.isEmpty() || endedByMove);
    }

    /** true si en este momento {@link #rehacer()} se aceptaría. */
    public boolean canRedo() {
        return allowsUndo() && moveHistory.hasRedo() && !state.isFinished();
    }

    /**
     * true si esta partida admite deshacer en algún momento: las reglas lo permiten y no tiene límite
     * de tiempo. Lo usa la UI para mostrar u ocultar los botones.
     */
    public boolean allowsUndo() {
        return rules.allowsUndo() && clock.timeRemaining(Side.WHITE).isEmpty();
    }

    private void ensureUndoAllowed() {
        if (!allowsUndo()) {
            throw new InvalidActionException(InvalidActionException.Reason.DESHACER_NO_PERMITIDO);
        }
    }

    /** Solo el jugador que tiene el turno puede rendirse. */
    public void resign(Side color) {
        Objects.requireNonNull(color, "color");
        ensureInProgress();
        if (color != turn) {
            throw new InvalidActionException(InvalidActionException.Reason.NO_ES_SU_TURNO);
        }
        finish(GameState.RESIGNATION, Optional.of(color.opposite()));
        notifyIfEnded();
    }

    public Optional<Side> offerDrawPending() {
        return drawOffer.pending();
    }

    public void offerDraw(Side color) {
        Objects.requireNonNull(color, "color");
        ensureInProgress();
        drawOffer.offer(color, turn);
    }

    public void acceptDraw(Side color) {
        Objects.requireNonNull(color, "color");
        ensureInProgress();
        drawOffer.validateResponse(color);
        finish(GameState.DRAW_AGREEMENT, Optional.empty());
        notifyIfEnded();
    }

    public void rejectDraw(Side color) {
        Objects.requireNonNull(color, "color");
        ensureInProgress();
        drawOffer.validateResponse(color);
        drawOffer.discard();
    }

    private void ensureInProgress() {
        verifyTime();
        if (state.isFinished()) {
            throw new InvalidActionException(InvalidActionException.Reason.PARTIDA_FINALIZADA);
        }
    }

    /** Consulta pura: vacío significa que la partida no tiene límite de tiempo. */
    public Optional<Duration> timeRemaining(Side color) {
        return clock.timeRemaining(Objects.requireNonNull(color, "color"));
    }

    /** La UI puede invocarla periódicamente; después del final no produce cambios. */
    public void verifyTime() {
        if (!state.isFinished() && clock.timeExhausted(turn)) {
            finish(GameState.TIME_EXHAUSTED, Optional.of(turn.opposite()));
            notifyIfEnded();
        }
    }

    /** Destinos permitidos para la pieza de esa casilla (vacío si no hay pieza, no es su turno o terminó la partida). */
    public Set<Position> movementsLegal(Position origin) {
        if (state.isFinished()) {
            return Set.of();
        }
        Optional<Piece> candidate = board.pieceIn(origin);
        if (candidate.isEmpty() || candidate.get().color() != turn) {
            return Set.of();
        }
        return rules.movementsLegal().destinations(board, origin, moveHistory.moves());
    }

    /**
     * Piezas a las que puede promover el peón si va de origen a destino, en el orden de las reglas
     * (la primera es la que se usa si no se elige). Vacío si ese movimiento no es una promoción
     * permitida ahora: así la UI sabe cuándo preguntar qué pieza quiere el jugador.
     */
    public List<Piece> promotionOptions(Position origin, Position destination) {
        Objects.requireNonNull(destination, "destino");
        if (!movementsLegal(origin).contains(destination)) {
            return List.of();
        }
        return rules.movementsLegal()
                .movesPossible(board, origin, moveHistory.moves()).stream()
                .filter(move -> move.destination().equals(destination))
                .flatMap(move -> move.promotion().stream())
                .toList();
    }

    /**
     * Ejecuta el movimiento si es válido; si no, lanza MovimientoInvalidoException
     * y deja la partida intacta. Si el tiempo venció, registra ese final sin mover.
     */
    public MoveResult move(MoveRequest movement) {
        verifyTime();
        if (state.isFinished()) {
            throw new InvalidMoveException(InvalidMoveException.Reason.PARTIDA_FINALIZADA);
        }
        Piece piece = board.pieceIn(movement.origin())
                .orElseThrow(() -> new InvalidMoveException(
                        InvalidMoveException.Reason.SIN_PIEZA_EN_ORIGEN));

        if (piece.color() != turn) {
            throw new InvalidMoveException(InvalidMoveException.Reason.NO_ES_SU_TURNO);
        }
        LegalMoves movementsLegal = rules.movementsLegal();
        Move move = chooseMove(movementsLegal, movement)
                .orElseThrow(() -> new InvalidMoveException(
                        InvalidMoveException.Reason.MOVIMIENTO_NO_PERMITIDO));
        if (movementsLegal.leavesKingInCheck(board, move)) {
            throw new InvalidMoveException(InvalidMoveException.Reason.DEJA_AL_REY_EN_JAQUE);
        }

        // La validación también puede consumir tiempo: comprobar antes de ejecutar.
        verifyTime();
        if (state.isFinished()) {
            throw new InvalidMoveException(InvalidMoveException.Reason.PARTIDA_FINALIZADA);
        }
        move.apply(board);
        drawOffer.onMove(piece.color());
        turn = turn.opposite();
        clock.changeTurn();
        moveHistory.add(move, register(moveHistory.movesWith(move)));
        notices.inform(new GameEvent.MoveMade(piece.color(), GameEvent.Trait.de(move), state));
        notifyIfEnded();
        return new MoveResult(move.captured(), state);
    }

    /**
     * La jugada que corresponde al movimiento pedido. Si hay varias hacia el mismo destino (una por
     * cada pieza de promoción), se usa la promoción elegida; sin elección, la primera opción (dama).
     * Pedir una promoción en una jugada que no promueve, o una pieza que no está entre las opciones,
     * no corresponde a ninguna jugada.
     */
    private Optional<Move> chooseMove(LegalMoves movementsLegal, MoveRequest movement) {
        List<Move> towardDestination = movementsLegal
                .movesPossible(board, movement.origin(), moveHistory.moves()).stream()
                .filter(move -> move.destination().equals(movement.destination()))
                .toList();
        if (movement.promotion().isEmpty()) {
            return towardDestination.stream().findFirst();
        }
        char requested = movement.promotion().get();
        return towardDestination.stream()
                .filter(move -> move.promotion().map(Piece::code).filter(code -> code == requested).isPresent())
                .findFirst();
    }

    /** El árbitro registra la posición actual y decide el estado; devuelve los recuerdos para el historial. */
    private List<Memento> register(List<Move> moves) {
        EndingReferee.Record recordValue = referee.registerAndEvaluate(board, turn, moves);
        apply(recordValue.verdict());
        return recordValue.mementos();
    }

    /** Aplica el estado (y el ganador) que decidió el árbitro después de una jugada. */
    private void apply(EndingReferee.Verdict verdict) {
        if (verdict.state().isFinished()) {
            finish(verdict.state(), verdict.winner());
            endedByMove = true;
        } else {
            state = verdict.state();
        }
    }

    private void finish(GameState finalState, Optional<Side> winner) {
        result = Optional.of(new GameResult(winner, finalState.reason().orElseThrow()));
        state = finalState;
        drawOffer.discard();
        clock.stop();
    }
}
