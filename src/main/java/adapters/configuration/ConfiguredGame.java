package adapters.configuration;

import core.side.Side;
import core.game.GameInProgress;
import adapters.ArtificialRival;
import core.time.GameClock;
import adapters.configuration.GameConfiguration.Team;

import java.util.Objects;
import java.util.Optional;

/**
 * Resultado del ensamblado: el sorteo y los equipos comparten la misma asignación.
 *
 * @param rival la IA, si se eligió jugar contra ella (juega con el equipo del Jugador 2)
 */
public record ConfiguredGame(GameInProgress game, Team whiteTeam, Team blackTeam,
                                 GameClock clock, Optional<ArtificialRival> rival) implements AutoCloseable {
    public ConfiguredGame {
        Objects.requireNonNull(game, "partida");
        Objects.requireNonNull(rival, "rival");
        Objects.requireNonNull(whiteTeam, "equipoBlancas");
        Objects.requireNonNull(blackTeam, "equipoNegras");
        Objects.requireNonNull(clock, "reloj");
        if (whiteTeam == blackTeam) throw new IllegalArgumentException("Los equipos deben ser distintos");
    }

    public Team team(Side color) {
        return Objects.requireNonNull(color, "color") == Side.WHITE ? whiteTeam : blackTeam;
    }

    /** Libera el reloj de una partida descartada sin inventar un resultado deportivo. */
    @Override public void close() {
        clock.stop();
    }
}
