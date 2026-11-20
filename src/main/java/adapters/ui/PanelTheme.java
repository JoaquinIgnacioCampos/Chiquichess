package adapters.ui;

import javafx.scene.paint.Color;
import java.util.Objects;

/** Paleta del marcador, independiente de los colores y reglas del tablero. */
public record PanelTheme(Color background, Color card, Color texto, Color textSecondary,
                        Color accent, Color activo, Color alerta, Color border,
                        Color pointOff, Color badgeWhite, Color badgeBlack) {
    public PanelTheme {
        Objects.requireNonNull(background, "fondo");
        Objects.requireNonNull(card, "tarjeta");
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(textSecondary, "textoSecundario");
        Objects.requireNonNull(accent, "acento");
        Objects.requireNonNull(activo, "activo");
        Objects.requireNonNull(alerta, "alerta");
        Objects.requireNonNull(border, "borde");
        Objects.requireNonNull(pointOff, "puntoApagado");
        Objects.requireNonNull(badgeWhite, "insigniaBlanca");
        Objects.requireNonNull(badgeBlack, "insigniaNegra");
    }

    public static PanelTheme scoreboard() {
        return new PanelTheme(Color.web("#0B1411"), Color.web("#111B19"),
                Color.web("#F3EBD4"), Color.web("#91A49E"), Color.web("#EBC065"),
                Color.web("#79D878"), Color.web("#FF806C"), Color.web("#344A42"),
                Color.web("#172B23"), Color.web("#F3EBD4"), Color.web("#17211E"));
    }
}
