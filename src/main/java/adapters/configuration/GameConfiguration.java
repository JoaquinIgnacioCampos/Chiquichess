package adapters.configuration;

import core.ia.Difficulty;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Selección completa del menú, independiente de JavaFX y de las reglas del ajedrez.
 *
 * @param permitirDeshacer si los jugadores pueden deshacer y rehacer jugadas; solo se admite sin tiempo
 * @param limiteDeJugadas  en el modo fútbol, jugadas de los dos bandos juntos a las que termina la
 *                         partida (entre {@link #LIMITE_MINIMO} y {@link #LIMITE_MAXIMO}); el
 *                         ajedrez clásico no lo usa
 * @param dificultad       la dificultad de la IA, que juega como Jugador 2; presente si y solo si el
 *                         rival es la IA
 */
public record GameConfiguration(Mode mode, Time time, Rival rival,
                                   Team teamPlayer1, Team teamPlayer2, Colors colors,
                                   boolean allowUndo, int moveLimit, Optional<Difficulty> difficulty) {

    public static final int DEFAULT_LIMIT = 90;
    public static final int LIMIT_MINIMUM = 10;
    public static final int LIMIT_MAXIMUM = 400;

    public GameConfiguration {
        Objects.requireNonNull(mode, "modo");
        Objects.requireNonNull(time, "tiempo");
        Objects.requireNonNull(rival, "rival");
        Objects.requireNonNull(teamPlayer1, "equipoJugador1");
        Objects.requireNonNull(teamPlayer2, "equipoJugador2");
        Objects.requireNonNull(colors, "colores");
        if (teamPlayer1 == teamPlayer2) throw new IllegalArgumentException("Los equipos deben ser distintos");
        Objects.requireNonNull(difficulty, "dificultad");
        if ((rival == Rival.IA) != difficulty.isPresent()) {
            throw new IllegalArgumentException("La dificultad se elige solo, y siempre, para jugar contra la IA");
        }
        if (allowUndo && time.duration().isPresent()) {
            throw new IllegalArgumentException("Solo se puede deshacer en partidas sin tiempo");
        }
        if (moveLimit < LIMIT_MINIMUM || moveLimit > LIMIT_MAXIMUM) {
            throw new IllegalArgumentException("El límite de jugadas debe estar entre "
                    + LIMIT_MINIMUM + " y " + LIMIT_MAXIMUM);
        }
    }

    /** Configuración contra un amigo, con el límite de jugadas elegido. */
    public GameConfiguration(Mode mode, Time time, Rival rival,
                                Team teamPlayer1, Team teamPlayer2, Colors colors,
                                boolean allowUndo, int moveLimit) {
        this(mode, time, rival, teamPlayer1, teamPlayer2, colors, allowUndo, moveLimit,
                Optional.empty());
    }

    /** Configuración con el límite de jugadas por defecto (90). */
    public GameConfiguration(Mode mode, Time time, Rival rival,
                                Team teamPlayer1, Team teamPlayer2, Colors colors,
                                boolean allowUndo) {
        this(mode, time, rival, teamPlayer1, teamPlayer2, colors, allowUndo, DEFAULT_LIMIT);
    }

    /** Configuración sin deshacer. */
    public GameConfiguration(Mode mode, Time time, Rival rival,
                                Team teamPlayer1, Team teamPlayer2, Colors colors) {
        this(mode, time, rival, teamPlayer1, teamPlayer2, colors, false);
    }

    /** CLASICO: ajedrez. FUTBOL: "Posesión y gol", ajedrez con pelota (3 goles o más goles al llegar al límite). */
    public enum Mode { CLASICO, FUTBOL }
    public enum Rival { AMIGO, IA }
    public enum Team {
        BOCA("Boca"), RIVER("River"), INDEPENDIENTE("Independiente"), ARGENTINA("Argentina");
        private final String displayName;
        Team(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }
    public enum Colors { JUGADOR_1_BLANCAS, JUGADOR_2_BLANCAS, ALEATORIO }
    public enum Time {
        TRES_MINUTOS("3 minutos", 3), CINCO_MINUTOS("5 minutos", 5),
        DIEZ_MINUTOS("10 minutos", 10), SIN_TIEMPO("Sin tiempo", 0);
        private final String texto;
        private final int minutes;
        Time(String texto, int minutes) { this.texto = texto; this.minutes = minutes; }
        public String texto() { return texto; }
        public Optional<Duration> duration() {
            return minutes == 0 ? Optional.empty() : Optional.of(Duration.ofMinutes(minutes));
        }
    }
}
