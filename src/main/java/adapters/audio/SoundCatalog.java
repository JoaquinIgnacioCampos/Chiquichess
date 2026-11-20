package adapters.audio;

import adapters.configuration.GameConfiguration.Team;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Dónde está el archivo de cada sonido. Busca en este orden:
 * <ol>
 *   <li>el del club en la carpeta externa ({@code sonidos/clubes/boca/gol.wav}),</li>
 *   <li>el del club dentro del programa ({@code src/main/resources/sonidos/clubes/boca/gol.wav}),</li>
 *   <li>el común en la carpeta externa ({@code sonidos/comunes/gol.wav}),</li>
 *   <li>el común dentro del programa,</li>
 * </ol>
 * y si no hay ninguno, prueba con el {@link Sound#alternative() alternativo}. Si tampoco, ese sonido no
 * suena: la partida se juega igual.
 *
 * La carpeta externa permite cambiar sonidos sin tocar el código ni recompilar.
 */
public final class SoundCatalog {
    /** Formatos que entiende Java Sound sin bibliotecas extra. */
    private static final List<String> EXTENSIONS = List.of("wav", "aiff", "aif", "au");
    private static final String RESOURCES = "/sonidos/";

    private final Optional<Path> folderExternal;
    private final boolean useResources;

    /** Catálogo real: la carpeta {@code sonidos} junto a donde se ejecuta el juego y los recursos del programa. */
    public SoundCatalog() {
        this(Optional.of(Path.of("sonidos")), true);
    }

    public SoundCatalog(Optional<Path> folderExternal, boolean useResources) {
        this.folderExternal = Objects.requireNonNull(folderExternal, "carpetaExterna");
        this.useResources = useResources;
    }

    /** El archivo para ese sonido y ese club (vacío para los sonidos que son iguales para todos). */
    public Optional<URL> search(Sound sound, Optional<Team> team) {
        Objects.requireNonNull(sound, "sonido");
        Objects.requireNonNull(team, "equipo");
        Optional<URL> found = team.filter(e -> sound.teamSpecific())
                .flatMap(e -> searchIn("clubes/" + e.name().toLowerCase(Locale.ROOT) + "/", sound))
                .or(() -> searchIn("comunes/", sound));
        return found.or(() -> sound.alternative().flatMap(other -> search(other, team)));
    }

    private Optional<URL> searchIn(String subfolder, Sound sound) {
        for (String extension : EXTENSIONS) {
            String relative = subfolder + sound.file() + "." + extension;
            Optional<URL> external = folderExternal.map(folder -> folder.resolve(relative))
                    .filter(Files::isRegularFile)
                    .flatMap(SoundCatalog::asUrl);
            if (external.isPresent()) return external;
        }
        if (useResources) {
            for (String extension : EXTENSIONS) {
                URL resource = SoundCatalog.class.getResource(RESOURCES + subfolder + sound.file() + "." + extension);
                if (resource != null) return Optional.of(resource);
            }
        }
        return Optional.empty();
    }

    private static Optional<URL> asUrl(Path file) {
        try {
            return Optional.of(file.toUri().toURL());
        } catch (MalformedURLException impossible) {
            return Optional.empty();
        }
    }
}
