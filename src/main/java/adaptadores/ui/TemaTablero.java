package adaptadores.ui;

import javafx.scene.paint.Color;
import java.util.Objects;

/** Paleta inmutable elegida en el arranque y recibida por las vistas. */
public record TemaTablero(Color casillaClara, Color casillaOscura,
                          Color piezaBlanca, Color piezaNegra,
                          Color contornoPiezas, Color fondo,
                          Color casillaSeleccionada, Color destinoLibre, Color destinoCaptura) {
    /** Compatibilidad con los temas creados antes de incorporar interacción. */
    public TemaTablero(Color casillaClara, Color casillaOscura, Color piezaBlanca,
                       Color piezaNegra, Color contornoPiezas, Color fondo) {
        this(casillaClara, casillaOscura, piezaBlanca, piezaNegra, contornoPiezas, fondo,
                Color.web("#F7F769"), Color.color(0, 0, 0, 0.18), Color.color(0, 0, 0, 0.23));
    }

    public TemaTablero {
        Objects.requireNonNull(casillaClara, "casillaClara");
        Objects.requireNonNull(casillaOscura, "casillaOscura");
        Objects.requireNonNull(piezaBlanca, "piezaBlanca");
        Objects.requireNonNull(piezaNegra, "piezaNegra");
        Objects.requireNonNull(contornoPiezas, "contornoPiezas");
        Objects.requireNonNull(fondo, "fondo");
        Objects.requireNonNull(casillaSeleccionada, "casillaSeleccionada");
        Objects.requireNonNull(destinoLibre, "destinoLibre");
        Objects.requireNonNull(destinoCaptura, "destinoCaptura");
    }
}
