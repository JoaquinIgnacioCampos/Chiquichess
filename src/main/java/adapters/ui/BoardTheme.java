package adapters.ui;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.Objects;

/** Paleta inmutable elegida en el arranque y recibida por las vistas. */
public record BoardTheme(Color lightSquare, Color darkSquare,
                          Color whitePiece, Color blackPiece,
                          Color outlinePieces, Color background,
                          Color selectedSquare, Color destinationFree, Color destinationCapture,
                          Image texture) {
    public BoardTheme(Color lightSquare, Color darkSquare, Color whitePiece, Color blackPiece,
                       Color outlinePieces, Color background, Color selectedSquare,
                       Color destinationFree, Color destinationCapture) {
        this(lightSquare, darkSquare, whitePiece, blackPiece, outlinePieces, background,
                selectedSquare, destinationFree, destinationCapture, null);
    }

    /** Compatibilidad con los temas creados antes de incorporar interacción. */
    public BoardTheme(Color lightSquare, Color darkSquare, Color whitePiece,
                       Color blackPiece, Color outlinePieces, Color background) {
        this(lightSquare, darkSquare, whitePiece, blackPiece, outlinePieces, background,
                Color.web("#F7F769"), Color.color(0, 0, 0, 0.18), Color.color(0, 0, 0, 0.23));
    }

    public BoardTheme {
        Objects.requireNonNull(lightSquare, "casillaClara");
        Objects.requireNonNull(darkSquare, "casillaOscura");
        Objects.requireNonNull(whitePiece, "piezaBlanca");
        Objects.requireNonNull(blackPiece, "piezaNegra");
        Objects.requireNonNull(outlinePieces, "contornoPiezas");
        Objects.requireNonNull(background, "fondo");
        Objects.requireNonNull(selectedSquare, "casillaSeleccionada");
        Objects.requireNonNull(destinationFree, "destinoLibre");
        Objects.requireNonNull(destinationCapture, "destinoCaptura");
    }
}
