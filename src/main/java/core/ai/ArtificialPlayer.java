package core.ai;

import core.game.GameSituation;
import core.moves.Move;
import core.time.TimeSource;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * La IA de ChiquiChess: busca con {@link MinimaxSearcher} según su {@link Difficulty} y elige.
 *
 * Para equivocarse de forma creíble, a cada jugada le suma un error al azar de hasta
 * {@code difficulty.noise()} y se queda con la de mayor puntaje resultante: puede elegir una jugada un
 * poco peor, pero nunca una mucho peor que la mejor que vio. Entre jugadas iguales elige al azar, así
 * dos partidas no son idénticas.
 *
 * Con la probabilidad de {@code difficulty.oversight()} se distrae: juega cualquiera de sus jugadas al
 * azar, salvo las que le dan mate en 1 al rival. Es el error de un principiante: no regala la partida
 * en una jugada, pero sí material.
 *
 * Al decidir ({@link #decide}) dice también cuánto tardaría una persona de su nivel en jugar eso (ver
 * {@link HumanRhythm}). El fácil y el medio calculan lo de siempre y el resto es espera; el difícil
 * calcula durante todo ese tiempo.
 *
 * El azar y el tiempo se reciben por constructor (como el sorteo y el reloj de la partida), así los
 * tests son repetibles.
 */
public final class ArtificialPlayer implements GameStrategy {
    /** Acepta tablas solo si cree que va perdiendo por al menos esto (tres peones). */
    static final int DISADVANTAGE_TO_ACCEPT_DRAW = 300;
    /** Margen que hace que la búsqueda dé el puntaje exacto de todas las jugadas. */
    private static final int MARGIN_TOTAL = Integer.MAX_VALUE / 4;
    /** Dos jugadas a menos de esto (un tercio de peón) son "casi igual de buenas": la posición hace dudar. */
    static final int ALMOST_EQUAL = 30;

    /** La jugada elegida y si había otra casi igual de buena. */
    private record Choice(Move move, boolean doubtful) {
    }

    private final Difficulty difficulty;
    private final Random chance;
    private final TimeSource clock;

    public ArtificialPlayer(Difficulty difficulty, Random chance, TimeSource clock) {
        this.difficulty = Objects.requireNonNull(difficulty, "dificultad");
        this.chance = Objects.requireNonNull(chance, "azar");
        this.clock = Objects.requireNonNull(clock, "reloj");
    }

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Piensa lo que le permite su dificultad, sin pasarse de {@code timeMaximum}. */
    @Override
    public Move chooseMove(GameSituation situation, Duration timeMaximum) {
        Objects.requireNonNull(situation, "situacion");
        Objects.requireNonNull(timeMaximum, "tiempoMaximo");
        return choose(situation, smaller(timeMaximum, difficulty.calculationTime()), difficulty.depthMaximum()).move();
    }

    /**
     * Elige la jugada y cuánto tardaría una persona de su nivel en jugarla. El difícil calcula todo ese
     * tiempo; el fácil y el medio, su tiempo de cálculo (y quien lo use espera el resto).
     *
     * @param timeRemaining lo que le queda en el reloj; vacío si la partida no tiene tiempo
     * @throws IllegalStateException si no hay ninguna jugada legal
     */
    @Override
    public GameDecision decide(GameSituation situation, Optional<Duration> timeRemaining) {
        Objects.requireNonNull(situation, "situacion");
        Objects.requireNonNull(timeRemaining, "tiempoRestante");
        int legal = new MinimaxSearcher(situation, clock, false).legalMoveCount(situation.turn());
        if (legal == 0) throw new IllegalStateException("No hay jugadas legales para elegir");
        boolean inCheck = situation.rules().detector().isInCheck(situation.board(), situation.turn());
        Duration response = HumanRhythm.time(difficulty, legal, inCheck, situation.moves().size(),
                timeRemaining, chance.nextDouble(), chance.nextDouble());
        Duration calculation = difficulty.calculatesWhileWaits() ? response
                : HumanRhythm.clockLimit(timeRemaining).map(limit -> smaller(limit, difficulty.calculationTime()))
                .orElse(difficulty.calculationTime());
        // Con una sola jugada posible no hay nada que pensar: se mira una jugada y listo.
        Choice choice = choose(situation, calculation, legal == 1 ? 1 : difficulty.depthMaximum());
        if (choice.doubtful() && !difficulty.calculatesWhileWaits() && legal > 1) {
            response = HumanRhythm.extendByComplexity(response, difficulty, situation.moves().size(),
                    timeRemaining, chance.nextDouble());
        }
        return new GameDecision(choice.move(), response);
    }

    private Choice choose(GameSituation situation, Duration time, int depth) {
        if (difficulty.oversight() > 0 && chance.nextDouble() < difficulty.oversight()) {
            List<Move> distracted = movesThatDoNotLoseImmediately(situation, time);
            if (!distracted.isEmpty()) return new Choice(distracted.get(chance.nextInt(distracted.size())), false);
        }
        int noise = difficulty.noise();
        List<MinimaxSearcher.MoveEvaluated> evaluatedMoves =
                new MinimaxSearcher(situation, clock, difficulty.withStillness())
                        .search(situation.turn(), depth, time, 2 * noise, chance);
        if (evaluatedMoves.isEmpty()) throw new IllegalStateException("No hay jugadas legales para elegir");
        int best = evaluatedMoves.getFirst().score();
        boolean doubtful = evaluatedMoves.size() > 1 && Math.abs(best) < MinimaxSearcher.MATE_THRESHOLD
                && evaluatedMoves.get(1).score() >= best - ALMOST_EQUAL;
        // Error al azar entre -ruido y +ruido; el medio punto extra desempata al azar entre iguales.
        Move chosen = evaluatedMoves.stream()
                .max(Comparator.comparingDouble(evaluated -> evaluated.score()
                        + (noise == 0 ? 0 : chance.nextInt(-noise, noise + 1)) + chance.nextDouble() * 0.5))
                .orElseThrow()
                .move();
        return new Choice(chosen, doubtful);
    }

    private static Duration smaller(Duration a, Duration other) {
        return a.compareTo(other) <= 0 ? a : other;
    }

    /** Sus jugadas que no le dejan al rival un mate en 1 (mira dos jugadas con puntaje exacto). */
    private List<Move> movesThatDoNotLoseImmediately(GameSituation situation, Duration time) {
        return new MinimaxSearcher(situation, clock, false)
                .search(situation.turn(), 2, time, MARGIN_TOTAL, chance).stream()
                .filter(evaluated -> evaluated.score() > -MinimaxSearcher.MATE_THRESHOLD)
                .map(MinimaxSearcher.MoveEvaluated::move)
                .toList();
    }

    /** Elige con todo el tiempo de su dificultad. */
    public Move chooseMove(GameSituation situation) {
        return chooseMove(situation, difficulty.calculationTime());
    }

    /** Acepta solo si va claramente perdiendo; si no, sigue jugando. Nunca se rinde. */
    @Override
    public boolean acceptsDraw(GameSituation situation) {
        Objects.requireNonNull(situation, "situacion");
        int forOfferer = new PositionEvaluator(situation.board())
                .evaluate(situation.board(), situation.turn(), situation.moves());
        return forOfferer >= DISADVANTAGE_TO_ACCEPT_DRAW;
    }
}
