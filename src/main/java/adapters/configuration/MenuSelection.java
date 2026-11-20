package adapters.configuration;

import core.ia.Difficulty;
import adapters.configuration.GameConfiguration.Colors;
import adapters.configuration.GameConfiguration.Team;
import adapters.configuration.GameConfiguration.Mode;
import adapters.configuration.GameConfiguration.Rival;
import adapters.configuration.GameConfiguration.Time;

import java.util.Objects;
import java.util.Optional;

/**
 * Opciones que el menú muestra elegidas al abrirse. A diferencia de {@link ConfiguracionPartida}, puede
 * estar incompleta: por ejemplo, la selección habitual no elige equipos.
 */
public record MenuSelection(Optional<Mode> mode, Optional<Time> time, Optional<Rival> rival,
                               Optional<Team> teamPlayer1, Optional<Team> teamPlayer2,
                               Optional<Colors> colors, boolean allowUndo, int moveLimit,
                               Optional<Difficulty> difficulty) {

    public MenuSelection {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(rival, "rival");
        Objects.requireNonNull(teamPlayer1, "teamPlayer1");
        Objects.requireNonNull(teamPlayer2, "teamPlayer2");
        Objects.requireNonNull(colors, "colors");
        Objects.requireNonNull(difficulty, "difficulty");
    }

    /** La partida más común: clásico, 10 minutos, contra un amigo y colores al azar; los equipos los elige cada uno. */
    public static MenuSelection usual() {
        return new MenuSelection(Optional.of(Mode.CLASSIC), Optional.of(Time.TEN_MINUTES),
                Optional.of(Rival.FRIEND), Optional.empty(), Optional.empty(), Optional.of(Colors.RANDOM),
                false, GameConfiguration.DEFAULT_LIMIT, Optional.empty());
    }

    /** Lo que se eligió para la última partida, para volver a mostrarlo al regresar al menú. */
    public static MenuSelection de(GameConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return new MenuSelection(Optional.of(configuration.mode()), Optional.of(configuration.time()),
                Optional.of(configuration.rival()), Optional.of(configuration.teamPlayer1()),
                Optional.of(configuration.teamPlayer2()), Optional.of(configuration.colors()),
                configuration.allowUndo(), configuration.moveLimit(), configuration.difficulty());
    }
}
