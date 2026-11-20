package adapters.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Marcas del tablero en pixel art de alta resolución, en vez de círculos lisos: la pelota, el punto de los
 * movimientos libres, el anillo de las capturas y el anillo punteado de los pases. Se dibujan píxel a píxel
 * con pocos tonos y bordes duros, como el resto del juego, y la vista las agranda sin suavizar.
 */
final class PixelMarkers {
    /** Lado, en píxeles de arte, de cada imagen. */
    static final int DOT = 14;
    static final int RING = 40;
    static final int BALL = 22;

    private static final Map<String, Image> CACHE = new HashMap<>();
    private static final Color INK = Color.web("#1B1B1B");
    private static final Color SHADE = Color.web("#D3D9DE");

    private PixelMarkers() {
    }

    /** Qué color tiene el píxel cuyo centro está a (dx, dy) del centro de la imagen; null si va vacío. */
    private interface Painter {
        Color at(double dx, double dy);
    }

    private static Image draw(int side, Painter painter) {
        WritableImage image = new WritableImage(side, side);
        PixelWriter writer = image.getPixelWriter();
        double center = side / 2.0;
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                Color color = painter.at(x + 0.5 - center, y + 0.5 - center);
                writer.setColor(x, y, color == null ? Color.TRANSPARENT : color);
            }
        }
        return image;
    }

    private static Image cached(String key, java.util.function.Supplier<Image> create) {
        return CACHE.computeIfAbsent(key, k -> create.get());
    }

    /** El borde de un tono más oscuro y más firme que el relleno, para marcar el contorno del píxel art. */
    private static Color edge(Color color) {
        return Color.color(color.getRed() * 0.6, color.getGreen() * 0.6, color.getBlue() * 0.6,
                Math.min(1, color.getOpacity() * 1.5));
    }

    /** Punto de un movimiento libre: un disco con el borde más marcado. */
    static Image dot(Color color) {
        Objects.requireNonNull(color, "color");
        return cached("dot" + color, () -> draw(DOT, (dx, dy) -> {
            double d = Math.hypot(dx, dy);
            if (d > DOT / 2.0 - 0.5) return null;
            return d > DOT / 2.0 - 1.6 ? edge(color) : color;
        }));
    }

    /** Anillo de una captura: tres píxeles de grosor, con el borde de afuera más marcado. */
    static Image captureRing(Color color) {
        Objects.requireNonNull(color, "color");
        return cached("ring" + color, () -> draw(RING, (dx, dy) -> {
            double d = Math.hypot(dx, dy);
            if (d > RING / 2.0 - 0.5 || d < RING / 2.0 - 3.6) return null;
            return d > RING / 2.0 - 1.6 ? edge(color) : color;
        }));
    }

    /** Anillo blanco punteado de un pase: ocho tramos separados por huecos. */
    static Image passRing() {
        return cached("pass", () -> draw(RING, (dx, dy) -> {
            double d = Math.hypot(dx, dy);
            if (d > RING / 2.0 - 0.5 || d < RING / 2.0 - 3.6) return null;
            double turn = (Math.atan2(dy, dx) + Math.PI) / (2 * Math.PI);
            if ((int) (turn * 16) % 2 != 0) return null;
            return d > RING / 2.0 - 1.6 ? SHADE : Color.WHITE;
        }));
    }

    /** Pelota de fútbol: blanca, con parches oscuros, un lado en sombra y el contorno marcado. */
    static Image ball() {
        return cached("ball", () -> draw(BALL, (dx, dy) -> {
            double d = Math.hypot(dx, dy);
            double radius = BALL / 2.0 - 0.5;
            if (d > radius) return null;
            if (d > radius - 1.3) return INK;
            if (Math.hypot(dx, dy) <= 2.7) return INK;                       // parche del centro
            for (int i = 0; i < 5; i++) {                                    // parches de alrededor
                double angle = Math.toRadians(-90 + 72 * i);
                if (Math.hypot(dx - 6.6 * Math.cos(angle), dy - 6.6 * Math.sin(angle)) <= 1.9) return INK;
            }
            return dx + dy > 4 && d > 5.5 ? SHADE : Color.WHITE;            // sombra abajo a la derecha
        }));
    }
}
