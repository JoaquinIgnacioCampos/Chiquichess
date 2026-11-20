package adapters.ui;

import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Silbato de árbitro en pixel art, para el botón de sonido: con ondas cuando suena y tachado cuando está
 * en silencio. Mide 26 × 22 píxeles en los dos casos.
 */
final class WhistleIcon {
    private static final Color STRIKETHROUGH = Color.web("#FF806C");

    private WhistleIcon() {
    }

    static Node create(boolean sounds, Color ink, Color background) {
        return sounds
                ? PixelIcon.create(PixelIcon.WHISTLE, ink, background, ink)
                : PixelIcon.crossed(PixelIcon.WHISTLE, ink, background, STRIKETHROUGH);
    }
}
