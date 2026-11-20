package adapters.audio;

import core.side.Side;
import core.rules.Scoreboard;

import java.util.Objects;
import java.util.Optional;

/**
 * Qué tan fuerte canta cada hinchada, de 0 a 1. Es una función del estado de la partida y no un
 * contador que se va sumando: así, al deshacer, la hinchada baja sola, y el cálculo se prueba sin
 * parlantes. El pico de un gol no sale de acá: lo pone el propio sonido del gol.
 *
 * <ul>
 *   <li>Arranca en {@link #BASE} y crece hasta {@link #POR_AVANCE} más a medida que se acerca el límite de jugadas.</li>
 *   <li>Si el partido está parejo (a lo sumo un gol de diferencia), suma hasta {@link #POR_PAREJO} más,
 *       también según el avance: un final apretado es lo que más se escucha.</li>
 *   <li>La hinchada del que va ganando canta completa; la del que pierde, a {@link #FACTOR_PERDEDOR};
 *       con empate, las dos a {@link #FACTOR_EMPATE}.</li>
 * </ul>
 */
public final class CrowdIntensity {
    static final double BASE = 0.3;
    static final double BY_ADVANCE = 0.5;
    static final double BY_EVEN = 0.2;
    static final double FACTOR_LOSER = 0.6;
    static final double FACTOR_TIE = 0.85;

    private CrowdIntensity() {
    }

    /**
     * @param jugadasHechas jugadas de los dos bandos en lo que va de la partida
     * @param limite        jugadas a las que termina la partida; sin límite, el avance no cuenta
     */
    public static double de(Side crowd, int movesMade, Optional<Integer> limit, Scoreboard scoreboard) {
        Objects.requireNonNull(crowd, "crowd");
        Objects.requireNonNull(limit, "limit");
        Objects.requireNonNull(scoreboard, "scoreboard");
        double advance = limit.filter(l -> l > 0)
                .map(l -> Math.min(1.0, Math.max(0, movesMade) / (double) l))
                .orElse(0.0);
        boolean even = Math.abs(scoreboard.white() - scoreboard.black()) <= 1;
        double general = BASE + BY_ADVANCE * advance + (even ? BY_EVEN * advance : 0);
        double factor = scoreboard.leader()
                .map(leader -> leader == crowd ? 1.0 : FACTOR_LOSER)
                .orElse(FACTOR_TIE);
        return Math.min(1.0, general * factor);
    }
}
