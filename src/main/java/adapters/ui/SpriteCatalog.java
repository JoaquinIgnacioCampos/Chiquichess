package adapters.ui;

import core.side.Side;
import adapters.configuration.GameConfiguration.Team;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Sprites de las piezas de un club: una versión blanca y otra negra de cada pieza, porque un club
 * puede jugar con cualquiera de los dos colores. Cada imagen se carga una sola vez, ya que el tablero
 * reconstruye sus casillas en cada redibujado. Un archivo que falta o no se puede leer da vacío.
 */
public final class SpriteCatalog {
    private static final Map<Character, String> FILES = Map.of(
            'R', "rey", 'D', "reina", 'T', "torre", 'A', "alfil", 'C', "caballo", 'P', "peon");

    private final String folder;
    private final Map<String, Optional<Image>> loadedItems = new HashMap<>();

    SpriteCatalog(String folder) {
        this.folder = Objects.requireNonNull(folder, "carpeta");
    }

    public static SpriteCatalog de(Team team) {
        Objects.requireNonNull(team, "equipo");
        return new SpriteCatalog("/sprites/" + team.name().toLowerCase(Locale.ROOT));
    }

    Optional<Image> sprite(char code, Side side) {
        Objects.requireNonNull(side, "bando");
        String piece = FILES.get(code);
        if (piece == null) return Optional.empty();
        String file = piece + "-" + (side == Side.WHITE ? "blanco" : "negro") + ".png";
        return loadedItems.computeIfAbsent(file, this::load);
    }

    private Optional<Image> load(String file) {
        var resource = SpriteCatalog.class.getResource(folder + "/" + file);
        if (resource == null) return Optional.empty();
        return Optional.of(new Image(resource.toExternalForm(), false)).filter(image -> !image.isError());
    }
}
