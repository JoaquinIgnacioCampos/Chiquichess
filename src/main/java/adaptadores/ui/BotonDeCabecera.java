package adaptadores.ui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Botón con el estilo de la cabecera de la partida ("Volver al menú", "Deshacer", "Rehacer"). */
final class BotonDeCabecera {

    private BotonDeCabecera() {
    }

    static Button crear(String texto, String id, Color fondo, Color tinta) {
        Button boton = new Button(texto);
        boton.setId(id);
        boton.setFont(Font.font("System", FontWeight.BOLD, 13));
        boton.setPadding(new Insets(8, 14, 8, 14));
        boton.setCursor(Cursor.HAND);
        Runnable pintar = () -> boton.setStyle("-fx-background-color: " + css(fondo)
                + "; -fx-text-fill: " + css(tinta) + "; -fx-background-radius: 4; -fx-border-radius: 4;"
                + " -fx-border-width: 2; -fx-border-color: " + css(boton.isFocused() ? tinta : fondo) + ";");
        boton.focusedProperty().addListener((p, antes, ahora) -> pintar.run());
        pintar.run();
        return boton;
    }

    private static String css(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
