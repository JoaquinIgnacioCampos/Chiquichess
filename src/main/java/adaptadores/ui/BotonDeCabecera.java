package adaptadores.ui;

import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;

/** Botón con el estilo de la cabecera de la partida ("Volver al menú", "Deshacer", "Rehacer"), en pixel art. */
final class BotonDeCabecera {

    private BotonDeCabecera() {
    }

    static Button crear(String texto, String id, Color fondo, Color tinta) {
        Button boton = new Button(texto);
        boton.setId(id);
        boton.setFont(EstiloPixel.fuente(14, true));
        boton.setCursor(Cursor.HAND);
        EstiloPixel.pintar(boton, fondo, tinta);
        return boton;
    }

    /** El mismo botón con un ícono de pixel art a la izquierda del texto. */
    static Button crear(String texto, String id, Color fondo, Color tinta, String[] icono, boolean espejado) {
        Button boton = crear(texto, id, fondo, tinta);
        boton.setGraphic(IconoPixel.crear(icono, tinta, fondo, tinta, espejado));
        boton.setGraphicTextGap(8);
        return boton;
    }
}
