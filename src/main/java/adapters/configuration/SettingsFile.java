package adapters.configuration;

import core.ia.Difficulty;
import adapters.configuration.GameConfiguration.Colors;
import adapters.configuration.GameConfiguration.Team;
import adapters.configuration.GameConfiguration.Mode;
import adapters.configuration.GameConfiguration.Rival;
import adapters.configuration.GameConfiguration.Time;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Archivo de texto con las {@link Preferencias}, una por renglón como {@code clave=valor}. Se lee al abrir
 * el juego; si no existe, se crea con los valores por defecto. Un valor que falta o no se entiende toma su
 * valor por defecto, así un archivo editado a mano nunca impide abrir el juego.
 *
 * Si no se puede leer ni escribir, se avisa por la consola de errores y el juego sigue con lo que tenga.
 */
public final class SettingsFile {
    private final Path file;

    public SettingsFile(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /** Las preferencias guardadas; si el archivo no existe, lo crea con las de por defecto. */
    public Preferences read() {
        if (!Files.exists(file)) {
            Preferences byDefault = Preferences.byDefault();
            save(byDefault);
            return byDefault;
        }
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IOException | IllegalArgumentException illegible) {
            System.err.println("ChiquiChess: could not read " + file + " (" + illegible.getMessage()
                    + "); the default settings are used");
            return Preferences.byDefault();
        }
        return from(values);
    }

    public void save(Preferences preferences) {
        Objects.requireNonNull(preferences, "preferences");
        MenuSelection s = preferences.selection();
        List<String> lines = List.of(
                "# ChiquiChess settings. They are loaded when the game opens and saved when playing and quitting.",
                "# If this file is deleted, it is created again with the default settings.",
                "mode=" + displayName(s.mode()),
                "time=" + displayName(s.time()),
                "opponent=" + displayName(s.rival()),
                "difficulty=" + displayName(s.difficulty()),
                "player1Team=" + displayName(s.teamPlayer1()),
                "player2Team=" + displayName(s.teamPlayer2()),
                "colors=" + displayName(s.colors()),
                "allowUndo=" + s.allowUndo(),
                "moveLimit=" + s.moveLimit(),
                "volume=" + String.format(Locale.ROOT, "%.2f", preferences.volume()),
                "muted=" + preferences.muted());
        try {
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException couldNot) {
            System.err.println("ChiquiChess: could not save " + file + " (" + couldNot.getMessage() + ")");
        }
    }

    private static Preferences from(Properties values) {
        MenuSelection usual = MenuSelection.usual();
        Optional<Team> team1 = constant(values, "teamPlayer1", Team.class);
        // Los dos jugadores no pueden tener el mismo club: el segundo queda sin elegir.
        Optional<Team> team2 = constant(values, "teamPlayer2", Team.class).filter(e -> !team1.equals(Optional.of(e)));
        MenuSelection selection = new MenuSelection(
                constant(values, "mode", Mode.class).or(usual::mode),
                constant(values, "time", Time.class).or(usual::time),
                constant(values, "rival", Rival.class).or(usual::rival),
                team1,
                team2,
                constant(values, "colors", Colors.class).or(usual::colors),
                Boolean.parseBoolean(values.getProperty("allowUndo", "false").trim()),
                whole(values, "moveLimit").filter(limit -> limit >= GameConfiguration.LIMIT_MINIMUM
                        && limit <= GameConfiguration.LIMIT_MAXIMUM).orElse(usual.moveLimit()),
                constant(values, "difficulty", Difficulty.class));
        double volume = decimal(values, "volume").filter(v -> v > 0 && v <= 1)
                .orElse(Preferences.DEFAULT_VOLUME);
        boolean muted = Boolean.parseBoolean(values.getProperty("muted", "false").trim());
        return new Preferences(selection, volume, muted);
    }

    private static String displayName(Optional<? extends Enum<?>> value) {
        return value.map(Enum::name).orElse("");
    }

    private static <E extends Enum<E>> Optional<E> constant(Properties values, String key, Class<E> type) {
        String texto = values.getProperty(key, "").trim().toUpperCase(Locale.ROOT);
        try {
            return texto.isEmpty() ? Optional.empty() : Optional.of(Enum.valueOf(type, texto));
        } catch (IllegalArgumentException desconocido) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> whole(Properties values, String key) {
        try {
            return Optional.of(Integer.parseInt(values.getProperty(key, "").trim()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    private static Optional<Double> decimal(Properties values, String key) {
        try {
            return Optional.of(Double.parseDouble(values.getProperty(key, "").trim()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }
}
