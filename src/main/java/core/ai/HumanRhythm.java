package core.ai;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Cuánto tardaría una persona de ese nivel en responder, para que la IA no juegue "de golpe" ni
 * siempre al mismo ritmo. Es una función pura: el azar llega como números entre 0 y 1, así se prueba
 * sin esperar.
 *
 * <ul>
 *   <li><b>Jugada común:</b> entre la respuesta mínima y la máxima de la dificultad, más cerca del medio
 *       (promedio de dos azares).</li>
 *   <li><b>Una sola jugada posible:</b> ~1 s; no hay nada que pensar.</li>
 *   <li><b>En jaque con pocas salidas:</b> el mínimo de su rango.</li>
 *   <li><b>Apertura</b> (las primeras {@link #JUGADAS_DE_APERTURA} jugadas de la partida): la mitad,
 *       como quien juega de memoria.</li>
 *   <li><b>Posición complicada</b> (varias jugadas casi igual de buenas): hacia el máximo; ver
 *       {@link #alargarPorComplejidad}.</li>
 *   <li><b>Con reloj:</b> nunca más de 1/{@link #FRACCION_DEL_TIEMPO_RESTANTE} de lo que le queda.</li>
 * </ul>
 */
public final class HumanRhythm {
    public static final int REMAINING_TIME_FRACTION = 40;
    static final int OPENING_MOVES = 10;
    static final Duration MOVE_FORCED = Duration.ofMillis(900);
    static final Duration OPENING_MINIMUM = Duration.ofMillis(800);

    private HumanRhythm() {
    }

    /**
     * Tiempo de respuesta antes de pensar la jugada.
     *
     * @param jugadasLegales cuántas jugadas puede hacer
     * @param jugadasHechas  jugadas de la partida hasta ahora (de los dos bandos)
     * @param azar1          número al azar entre 0 y 1
     * @param azar2          otro número al azar entre 0 y 1
     */
    public static Duration time(Difficulty difficulty, int movesLegal, boolean inCheck, int movesMade,
                                  Optional<Duration> timeRemaining, double chance1, double chance2) {
        Objects.requireNonNull(difficulty, "dificultad");
        Objects.requireNonNull(timeRemaining, "tiempoRestante");
        Duration time;
        if (movesLegal <= 1) {
            time = MOVE_FORCED.plusMillis(Math.round(300 * chance1));
        } else if (inCheck && movesLegal <= 3) {
            time = difficulty.responseMinimum();
        } else {
            time = between(difficulty, (chance1 + chance2) / 2);
            if (movesMade < OPENING_MOVES) {
                time = maximum(time.dividedBy(2), OPENING_MINIMUM);
            }
        }
        return withClock(time, timeRemaining);
    }

    /**
     * En una posición complicada (varias jugadas casi igual de buenas), una persona duda: el tiempo sube
     * por lo menos a un punto al azar de la mitad alta de su rango (para que no sea siempre el mismo),
     * sin pasarse del reloj. En la apertura no: ahí casi todas las jugadas son parecidas y se juega
     * rápido igual.
     *
     * @param azar número al azar entre 0 y 1
     */
    public static Duration extendByComplexity(Duration time, Difficulty difficulty, int movesMade,
                                                 Optional<Duration> timeRemaining, double chance) {
        if (movesMade < OPENING_MOVES) return time;
        return withClock(maximum(time, between(difficulty, 0.5 + 0.5 * chance)), timeRemaining);
    }

    /** Lo más que puede tardar con ese tiempo restante; sin reloj, no hay límite. */
    public static Optional<Duration> clockLimit(Optional<Duration> timeRemaining) {
        return timeRemaining.map(remaining -> remaining.dividedBy(REMAINING_TIME_FRACTION));
    }

    private static Duration between(Difficulty difficulty, double fraction) {
        long minimum = difficulty.responseMinimum().toMillis();
        long maximum = difficulty.responseMaximum().toMillis();
        return Duration.ofMillis(minimum + Math.round((maximum - minimum) * Math.max(0, Math.min(1, fraction))));
    }

    private static Duration withClock(Duration time, Optional<Duration> timeRemaining) {
        return clockLimit(timeRemaining).filter(limit -> limit.compareTo(time) < 0).orElse(time);
    }

    private static Duration maximum(Duration a, Duration other) {
        return a.compareTo(other) >= 0 ? a : other;
    }
}
