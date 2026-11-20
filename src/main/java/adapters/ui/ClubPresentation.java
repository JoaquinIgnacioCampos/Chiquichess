package adapters.ui;

import adapters.configuration.GameConfiguration.Team;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo visual: escudo y colores de cada equipo, fuera del núcleo.
 *
 * El estilo de cada equipo se define en un {@code switch}: un equipo nuevo sin escudo ni colores no
 * compila. Los escudos van en {@code /escudos/}; si falta el archivo, el programa no arranca.
 */
public record ClubPresentation(Team team, Image crest, PanelTheme theme) {
    /** Cada escudo se carga una sola vez. */
    private static final Map<Team, Style> STYLES = new EnumMap<>(Team.class);

    static {
        for (Team team : Team.values()) {
            STYLES.put(team, styleFor(team));
        }
    }

    public ClubPresentation {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(crest, "crest");
        Objects.requireNonNull(theme, "theme");
    }

    /** Escudo, fondo de la tarjeta, acento (nombre del club y detalles) y borde de cada equipo. */
    private static Style styleFor(Team team) {
        return switch (team) {
            case BOCA -> new Style("boca.png", "#102B50", "#F9B923", "#315783");
            // Tarjeta clara: necesita texto oscuro para que se lea.
            case RIVER -> new Style("river.png", "#E8E5DF", "#A51D2D", "#FFFFFF", "#242424", "#505050");
            case INDEPENDIENTE -> new Style("independiente.png", "#321719", "#FF777D", "#7E3A41");
            case ARGENTINA -> new Style("argentina.png", "#14304D", "#75AADB", "#3D6E9C");
        };
    }

    public static ClubPresentation de(Team team, PanelTheme base) {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(base, "base");
        Style style = STYLES.get(team);
        Color texto = style.texto().orElse(base.texto());
        Color textSecondary = style.textSecondary().orElse(base.textSecondary());
        PanelTheme theme = new PanelTheme(base.background(), style.card(), texto, textSecondary,
                style.accent(), base.activo(), base.alerta(), style.border(), base.pointOff(),
                base.badgeWhite(), base.badgeBlack());
        return new ClubPresentation(team, style.crest(), theme);
    }

    /** El escudo de un equipo, sin armar sus colores (lo usa el menú). */
    static Image crestFor(Team team) {
        return STYLES.get(Objects.requireNonNull(team, "team")).crest();
    }

    /** Nombre del archivo del escudo de ese equipo, dentro de {@code /escudos/}. */
    static String crestFile(Team team) {
        return STYLES.get(Objects.requireNonNull(team, "team")).file();
    }

    /** Si no trae colores de texto, la tarjeta usa los del tema base (pensados para fondos oscuros). */
    private record Style(String file, Image crest, Color card, Color accent, Color border,
                          Optional<Color> texto, Optional<Color> textSecondary) {
        Style(String file, String card, String accent, String border) {
            this(file, load(file), Color.web(card), Color.web(accent), Color.web(border),
                    Optional.empty(), Optional.empty());
        }

        Style(String file, String card, String accent, String border, String texto, String textSecondary) {
            this(file, load(file), Color.web(card), Color.web(accent), Color.web(border),
                    Optional.of(Color.web(texto)), Optional.of(Color.web(textSecondary)));
        }
    }

    private static Image load(String file) {
        var resource = Objects.requireNonNull(ClubPresentation.class.getResource("/crests/" + file),
                "Missing the crest " + file);
        Image image = new Image(resource.toExternalForm(), false);
        if (image.isError()) throw new IllegalStateException("Could not load the crest " + file, image.getException());
        return image;
    }
}
