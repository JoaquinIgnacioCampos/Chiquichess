package adaptadores.ui;

import javafx.scene.paint.Color;
import java.util.Objects;

/** Paleta del marcador, independiente de los colores y reglas del tablero. */
public record TemaPanel(Color fondo, Color tarjeta, Color texto, Color textoSecundario,
                        Color acento, Color activo, Color alerta, Color borde,
                        Color puntoApagado, Color insigniaBlanca, Color insigniaNegra) {
    public TemaPanel {
        Objects.requireNonNull(fondo, "fondo");
        Objects.requireNonNull(tarjeta, "tarjeta");
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(textoSecundario, "textoSecundario");
        Objects.requireNonNull(acento, "acento");
        Objects.requireNonNull(activo, "activo");
        Objects.requireNonNull(alerta, "alerta");
        Objects.requireNonNull(borde, "borde");
        Objects.requireNonNull(puntoApagado, "puntoApagado");
        Objects.requireNonNull(insigniaBlanca, "insigniaBlanca");
        Objects.requireNonNull(insigniaNegra, "insigniaNegra");
    }

    public static TemaPanel marcador() {
        return new TemaPanel(Color.web("#0B1411"), Color.web("#111B19"),
                Color.web("#F3EBD4"), Color.web("#91A49E"), Color.web("#EBC065"),
                Color.web("#79D878"), Color.web("#FF806C"), Color.web("#344A42"),
                Color.web("#172B23"), Color.web("#F3EBD4"), Color.web("#17211E"));
    }
}
