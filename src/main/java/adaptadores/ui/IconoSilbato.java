package adaptadores.ui;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Silbato de árbitro en pixel art, para el botón de sonido: con ondas cuando suena y tachado cuando está
 * en silencio. Mide 26 × 22 píxeles en los dos casos.
 */
final class IconoSilbato {
    private static final Color TACHADURA = Color.web("#FF806C");

    private IconoSilbato() {
    }

    static Node crear(boolean suena, Color tinta, Color fondo) {
        return suena
                ? IconoPixel.crear(IconoPixel.SILBATO, tinta, fondo, tinta)
                : IconoPixel.tachado(IconoPixel.SILBATO, tinta, fondo, TACHADURA);
    }
}
