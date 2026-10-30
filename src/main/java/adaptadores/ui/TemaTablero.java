package adaptadores.ui;

import javafx.scene.paint.Color;
import java.util.Objects;

/** Paleta inmutable elegida en el arranque y recibida por las vistas. */
public record TemaTablero(Color casillaClara, Color casillaOscura,
                          Color piezaBlanca, Color piezaNegra,
                          Color contornoPiezas, Color fondo) {
    public TemaTablero {
        Objects.requireNonNull(casillaClara, "casillaClara");
        Objects.requireNonNull(casillaOscura, "casillaOscura");
        Objects.requireNonNull(piezaBlanca, "piezaBlanca");
        Objects.requireNonNull(piezaNegra, "piezaNegra");
        Objects.requireNonNull(contornoPiezas, "contornoPiezas");
        Objects.requireNonNull(fondo, "fondo");
    }
}
