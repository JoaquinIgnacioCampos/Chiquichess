package adapters.ui;

import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;

/** Botón con el estilo de la cabecera de la partida ("Volver al menú", "Deshacer", "Rehacer"), en pixel art. */
final class HeaderButton {

    private HeaderButton() {
    }

    static Button create(String texto, String id, Color background, Color ink) {
        Button button = new Button(texto);
        button.setId(id);
        button.setFont(PixelStyle.font(14, true));
        button.setCursor(Cursor.HAND);
        PixelStyle.paint(button, background, ink);
        return button;
    }

    /** El mismo botón con un ícono de pixel art a la izquierda del texto. */
    static Button create(String texto, String id, Color background, Color ink, String[] icon, boolean mirrored) {
        Button button = create(texto, id, background, ink);
        button.setGraphic(PixelIcon.create(icon, ink, background, ink, mirrored));
        button.setGraphicTextGap(8);
        return button;
    }
}
