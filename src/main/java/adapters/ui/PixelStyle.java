package adapters.ui;

import javafx.scene.control.ButtonBase;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;

/**
 * Estilo común de la interfaz, en pixel art de alta resolución: la fuente Silkscreen y botones y
 * tarjetas de bordes rectos con sombra dura abajo a la derecha.
 */
final class PixelStyle {
    /** Familia de la fuente incluida en los recursos (licencia OFL, ver fuentes/OFL.txt). */
    static final String FAMILY = "Silkscreen";
    /** Sombra dura de tarjetas y botones, para el efecto de relieve. */
    static final Color SHADOW = Color.web("#040807");

    static {
        // Se registran una vez: después cualquier Font.font con la familia encuentra la fuente.
        for (String file : new String[]{"Silkscreen-Regular.ttf", "Silkscreen-Bold.ttf"}) {
            var resource = PixelStyle.class.getResource("/fuentes/" + file);
            if (resource != null) Font.loadFont(resource.toExternalForm(), 12);
        }
    }

    private PixelStyle() {
    }

    static Font font(double size, boolean bold) {
        return Font.font(FAMILY, bold ? FontWeight.BOLD : FontWeight.NORMAL, size);
    }

    /** Para el estilo de la escena: así también los controles sin fuente propia usan la pixelada. */
    static String familyCss() {
        return "-fx-font-family: '" + FAMILY + "';";
    }

    /**
     * Botón en relieve: sombra, borde de 2 píxeles y cara plana. Hundido (elegido o apretado) pierde la
     * sombra y se corre hacia ella.
     */
    static String button(Color face, Color border, boolean sunken) {
        return "-fx-background-color: " + css(SHADOW) + ", " + css(border) + ", " + css(face) + ";"
                + (sunken
                ? "-fx-background-insets: 3 0 0 3, 3 0 0 3, 5 2 2 5; -fx-padding: 9 11 6 14;"
                : "-fx-background-insets: 3 0 0 3, 0 3 3 0, 2 5 5 2; -fx-padding: 6 14 9 11;")
                + "-fx-background-radius: 0; -fx-border-width: 0;";
    }

    /** Tarjeta en relieve: sombra dura, borde grueso y cara plana. */
    static String card(Color face, Color border) {
        return "-fx-background-color: " + css(SHADOW) + ", " + css(border) + ", " + css(face) + ";"
                + "-fx-background-insets: 4 0 0 4, 0 4 4 0, 3 7 7 3; -fx-background-radius: 0;";
    }

    /** Aplica el relieve a un botón común y lo mantiene al pasar el mouse, apretarlo o enfocarlo. */
    static void paint(ButtonBase button, Color face, Color ink) {
        Runnable apply = () -> {
            Color background = button.isHover() && !button.isDisabled() ? face.interpolate(ink, 0.15) : face;
            Color border = button.isFocused() ? ink : face.darker();
            button.setStyle(button(background, border, button.isPressed())
                    + "-fx-text-fill: " + css(ink) + ";");
        };
        button.hoverProperty().addListener((p, before, now) -> apply.run());
        button.pressedProperty().addListener((p, before, now) -> apply.run());
        button.focusedProperty().addListener((p, before, now) -> apply.run());
        button.disabledProperty().addListener((p, before, now) -> apply.run());
        apply.run();
    }

    static String css(Color color) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
