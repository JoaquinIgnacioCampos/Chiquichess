package core.ia;

import core.side.Side;
import core.game.GameSituation;
import core.moves.Move;
import core.pieces.Piece;
import core.rules.StatefulCondition;
import core.rules.EndCondition;
import core.rules.MoveContext;
import core.rules.GameState;
import core.rules.ICheckDetector;
import core.rules.LegalMoves;
import core.board.Position;
import core.board.Board;
import core.time.TimeSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Minimax (en su forma negamax: el puntaje de un bando es el opuesto del otro) con:
 * <ul>
 *   <li><b>Poda alfa-beta:</b> deja de mirar una rama apenas sabe que el rival no la va a permitir.
 *       Elige la misma jugada que Minimax puro, pero mirando muchas menos posiciones.</li>
 *   <li><b>Profundización iterativa:</b> busca a 1 jugada, después a 2, a 3... y se queda con la
 *       última búsqueda que terminó antes de que se acabe el tiempo. Siempre tiene una jugada lista.</li>
 *   <li><b>Quietud</b> (opcional): al llegar al límite, sigue mirando solo capturas y goles, para no
 *       cortar el análisis en medio de un cambio de piezas.</li>
 *   <li><b>Mates y finales:</b> los deciden las mismas condiciones de fin de la partida (las que no
 *       guardan memoria). Un mate vale más cuanto antes llega, así gana lo más rápido posible y, si
 *       pierde, lo demora lo más posible.</li>
 * </ul>
 *
 * Trabaja sobre la copia del tablero de la {@link GameSituation}: aplica y deshace jugadas con
 * sus efectos, sin tocar la partida. Una instancia sirve para una sola búsqueda.
 */
final class MinimaxSearcher {
    static final int MATE = 1_000_000;
    /** Los puntajes por encima de esto son mates (o victorias) a cierta cantidad de jugadas. */
    static final int MATE_THRESHOLD = MATE - 10_000;
    private static final int INFINITE = Integer.MAX_VALUE / 2;
    private static final int MAX_QUIESCENCE_DEPTH = 6;

    private final Board board;
    private final List<Move> path;
    private final LegalMoves movementsLegal;
    private final ICheckDetector detector;
    /** Las condiciones de fin sin memoria: la repetición y los 50 movimientos no se miran al buscar. */
    private final List<EndCondition> conditions;
    private final PositionEvaluator evaluator;
    private final TimeSource clock;
    private final boolean withStillness;
    private long limit;
    private long positions;
    private int depthReached;

    /** Se cortó la búsqueda porque se acabó el tiempo. */
    private static final class TimeExhausted extends RuntimeException {
        TimeExhausted() {
            super(null, null, false, false);
        }
    }

    /** Una jugada posible desde la posición inicial, con su puntaje para quien juega. */
    record MoveEvaluated(Move move, int score) {
    }

    MinimaxSearcher(GameSituation situation, TimeSource clock, boolean withStillness) {
        this.board = situation.board();
        this.path = new ArrayList<>(situation.moves());
        this.movementsLegal = situation.rules().movementsLegal();
        this.detector = situation.rules().detector();
        this.conditions = situation.rules().conditions().stream()
                .filter(condition -> !(condition instanceof StatefulCondition))
                .toList();
        this.evaluator = new PositionEvaluator(board);
        this.clock = Objects.requireNonNull(clock, "reloj");
        this.withStillness = withStillness;
    }

    /**
     * Puntúa las jugadas de {@code turn}. Las que pueden quedar a menos de {@code margin} de la mejor
     * tienen su puntaje exacto; las demás, una cota (son peores que la mejor por más que el margen).
     * Vacío si no hay jugadas legales.
     *
     * @param chance mezcla las jugadas antes de ordenarlas, para que entre iguales no elija siempre la misma
     */
    List<MoveEvaluated> search(Side turn, int depthMaximum, Duration time, int margin, Random chance) {
        limit = clock.nowInNanos() + time.toNanos();
        List<Move> moves = legal(turn);
        Collections.shuffle(moves, chance);
        sort(moves);
        List<MoveEvaluated> result = List.of();
        for (int depth = 1; depth <= depthMaximum && !moves.isEmpty(); depth++) {
            try {
                result = root(moves, turn, depth, margin);
                depthReached = depth;
            } catch (TimeExhausted exhausted) {
                if (result.isEmpty()) {
                    // Ni siquiera terminó de mirar a una jugada: termina esa sin límite de tiempo.
                    limit = Long.MAX_VALUE;
                    result = root(moves, turn, 1, margin);
                    depthReached = 1;
                }
                break;
            }
            // La próxima vuelta mira primero las que salieron mejor: así poda más.
            moves = new ArrayList<>(result.stream().map(MoveEvaluated::move).toList());
            if (result.getFirst().score() >= MATE_THRESHOLD) break;
        }
        return result;
    }

    /** Cuántas jugadas legales tiene ese bando en la posición inicial. */
    int legalMoveCount(Side turn) {
        return legal(turn).size();
    }

    /** Cuántas posiciones miró en la última búsqueda (para medir). */
    long positions() {
        return positions;
    }

    /** Hasta qué profundidad terminó de mirar en la última búsqueda (para medir). */
    int depthReached() {
        return depthReached;
    }

    private List<MoveEvaluated> root(List<Move> moves, Side turn, int depth, int margin) {
        List<MoveEvaluated> evaluatedMoves = new ArrayList<>();
        int best = -INFINITE;
        for (Move move : moves) {
            // Ventana con margen: las que podrían quedar cerca de la mejor se miran con puntaje exacto.
            int alfa = best == -INFINITE ? -INFINITE : best - margin - 1;
            play(move);
            int score;
            try {
                score = -negamax(turn.opposite(), depth - 1, -INFINITE, -alfa, 1);
            } finally {
                undo(move);
            }
            evaluatedMoves.add(new MoveEvaluated(move, score));
            best = Math.max(best, score);
        }
        evaluatedMoves.sort(Comparator.comparingInt(MoveEvaluated::score).reversed());
        return List.copyOf(evaluatedMoves);
    }

    private int negamax(Side turn, int depth, int alfa, int beta, int distance) {
        countPosition();
        if (depth <= 0) {
            return withStillness ? stillness(turn, alfa, beta, distance, 0) : sheet(turn, distance);
        }
        // Primero los finales que no dependen de tener jugadas (por ejemplo, el tercer gol).
        Optional<Integer> fin = gameEnding(turn, true, distance);
        if (fin.isPresent()) return fin.get();
        List<Move> moves = possible(turn);
        sort(moves);
        int best = -INFINITE;
        boolean someLegal = false;
        for (Move move : moves) {
            // La legalidad se mira recién al probar la jugada: las que la poda descarta no se verifican.
            if (!playIfIsLegal(move, turn)) continue;
            someLegal = true;
            int score;
            try {
                score = -negamax(turn.opposite(), depth - 1, -beta, -alfa, distance + 1);
            } finally {
                undo(move);
            }
            if (score > best) best = score;
            if (score > alfa) alfa = score;
            if (alfa >= beta) break; // El rival nunca dejaría llegar acá: no hace falta seguir mirando.
        }
        // Sin ninguna jugada legal: mate o ahogado, según las reglas.
        return someLegal ? best : gameEnding(turn, false, distance).orElse(0);
    }

    /** Sigue mirando solo capturas y goles hasta que la posición se calma. */
    private int stillness(Side turn, int alfa, int beta, int distance, int level) {
        int still = sheet(turn, distance);
        if (Math.abs(still) >= MATE_THRESHOLD || level >= MAX_QUIESCENCE_DEPTH) return still;
        if (still >= beta) return still;
        if (still > alfa) alfa = still;
        List<Move> shaken = new ArrayList<>(possible(turn).stream()
                .filter(move -> move.captured().isPresent() || move.isGoal())
                .toList());
        sort(shaken);
        int best = still;
        for (Move move : shaken) {
            countPosition();
            if (!playIfIsLegal(move, turn)) continue;
            int score;
            try {
                score = -stillness(turn.opposite(), -beta, -alfa, distance + 1, level + 1);
            } finally {
                undo(move);
            }
            if (score > best) best = score;
            if (score > alfa) alfa = score;
            if (alfa >= beta) break;
        }
        return best;
    }

    /**
     * Posición donde se deja de mirar. Antes de evaluarla se fija si la partida terminó: los finales que
     * no dependen de tener jugadas (como el tercer gol) y, si quien juega está en jaque, el mate. Así un
     * mate se ve aunque caiga justo en el límite de la búsqueda.
     */
    private int sheet(Side turn, int distance) {
        Optional<Integer> fin = gameEnding(turn, true, distance);
        if (fin.isPresent()) return fin.get();
        if (detector.isInCheck(board, turn) && !hasSomeLegalMove(turn)) {
            return gameEnding(turn, false, distance).orElse(0);
        }
        return evaluator.evaluate(board, turn, path);
    }

    private boolean hasSomeLegalMove(Side turn) {
        for (Move move : possible(turn)) {
            if (playIfIsLegal(move, turn)) {
                undo(move);
                return true;
            }
        }
        return false;
    }

    /**
     * Si la posición terminó la partida según las reglas, su puntaje para quien tiene el turno: ganar es
     * {@link #MATE} menos la distancia (antes es mejor), perder lo opuesto y las tablas 0.
     */
    private Optional<Integer> gameEnding(Side turn, boolean hasMoves, int distance) {
        boolean inCheck = !hasMoves && detector.isInCheck(board, turn);
        MoveContext context = new MoveContext(board, turn, path, inCheck, hasMoves);
        for (EndCondition condition : conditions) {
            Optional<GameState> state = condition.evaluate(context);
            if (state.isPresent()) {
                Optional<Side> winner = condition.winner(state.get(), context);
                if (winner.isEmpty()) return Optional.of(0);
                return Optional.of(winner.get() == turn ? MATE - distance : -(MATE - distance));
            }
        }
        return Optional.empty();
    }

    /** Jugadas legales: las posibles que no dejan al propio rey en jaque. */
    private List<Move> legal(Side turn) {
        List<Move> legal = new ArrayList<>();
        for (Move move : possible(turn)) {
            if (playIfIsLegal(move, turn)) {
                undo(move);
                legal.add(move);
            }
        }
        return legal;
    }

    /** Jugadas de las piezas del bando según sus reglas, sin mirar todavía si dejan al rey en jaque. */
    private List<Move> possible(Side turn) {
        List<Move> possible = new ArrayList<>();
        List<Position> own = new ArrayList<>();
        for (Map.Entry<Position, Piece> input : board.pieces().entrySet()) {
            if (input.getValue().color() == turn) own.add(input.getKey());
        }
        for (Position origin : own) {
            possible.addAll(movementsLegal.movesPossible(board, origin, path));
        }
        return possible;
    }

    /**
     * Juega la jugada si no deja al propio rey en jaque y devuelve true; si lo deja, la deshace y
     * devuelve false. Aplicar y deshacer sobre el mismo tablero evita copiarlo por cada jugada.
     */
    private boolean playIfIsLegal(Move move, Side turn) {
        play(move);
        if (detector.isInCheck(board, turn)) {
            undo(move);
            return false;
        }
        return true;
    }

    /** Primero las que suelen ser mejores (goles, promociones, capturas de piezas valiosas): así poda más. */
    private static void sort(List<Move> moves) {
        if (moves.size() < 2) return;
        // El interés se calcula una vez por jugada, no en cada comparación del ordenamiento.
        Map<Move, Integer> interest = new IdentityHashMap<>();
        for (Move move : moves) interest.put(move, interest(move));
        moves.sort((a, other) -> Integer.compare(interest.get(other), interest.get(a)));
    }

    private static int interest(Move move) {
        int interest = 0;
        if (move.isGoal()) interest += 20_000;
        if (move.promotion().isPresent()) interest += 8_000 + move.promotion().get().value();
        if (move.captured().isPresent()) {
            interest += 1_000 + 10 * move.captured().get().value()
                    - move.pieceMoved().value();
        }
        return interest;
    }

    private void play(Move move) {
        move.apply(board);
        path.add(move);
    }

    private void undo(Move move) {
        path.removeLast();
        move.revert(board);
    }

    private void countPosition() {
        positions++;
        if ((positions & 1023) == 0 && clock.nowInNanos() > limit) throw new TimeExhausted();
    }
}
