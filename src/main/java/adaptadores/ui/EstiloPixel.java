package adaptadores.ui;

import javafx.scene.control.ButtonBase;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Locale;

/**
 * Estilo común de la interfaz, en pixel art de alta resolución: la fuente Silkscreen y botones y
 * tarjetas de bordes rectos con sombra dura abajo a la derecha.
 */
final class EstiloPixel {
    /** Familia de la fuente incluida en los recursos (licencia OFL, ver fuentes/OFL.txt). */
    static final String FAMILIA = "Silkscreen";
    /** Sombra dura de tarjetas y botones, para el efecto de relieve. */
    static final Color SOMBRA = Color.web("#040807");

    static {
        // Se registran una vez: después cualquier Font.font con la familia encuentra la fuente.
        for (String archivo : new String[]{"Silkscreen-Regular.ttf", "Silkscreen-Bold.ttf"}) {
            var recurso = EstiloPixel.class.getResource("/fuentes/" + archivo);
            if (recurso != null) Font.loadFont(recurso.toExternalForm(), 12);
        }
    }

    private EstiloPixel() {
    }

    static Font fuente(double tamano, boolean negrita) {
        return Font.font(FAMILIA, negrita ? FontWeight.BOLD : FontWeight.NORMAL, tamano);
    }

    /** Para el estilo de la escena: así también los controles sin fuente propia usan la pixelada. */
    static String familiaCss() {
        return "-fx-font-family: '" + FAMILIA + "';";
    }

    /**
     * Botón en relieve: sombra, borde de 2 píxeles y cara plana. Hundido (elegido o apretado) pierde la
     * sombra y se corre hacia ella.
     */
    static String boton(Color cara, Color borde, boolean hundido) {
        return "-fx-background-color: " + css(SOMBRA) + ", " + css(borde) + ", " + css(cara) + ";"
                + (hundido
                ? "-fx-background-insets: 3 0 0 3, 3 0 0 3, 5 2 2 5; -fx-padding: 9 11 6 14;"
                : "-fx-background-insets: 3 0 0 3, 0 3 3 0, 2 5 5 2; -fx-padding: 6 14 9 11;")
                + "-fx-background-radius: 0; -fx-border-width: 0;";
    }

    /** Tarjeta en relieve: sombra dura, borde grueso y cara plana. */
    static String tarjeta(Color cara, Color borde) {
        return "-fx-background-color: " + css(SOMBRA) + ", " + css(borde) + ", " + css(cara) + ";"
                + "-fx-background-insets: 4 0 0 4, 0 4 4 0, 3 7 7 3; -fx-background-radius: 0;";
    }

    /** Aplica el relieve a un botón común y lo mantiene al pasar el mouse, apretarlo o enfocarlo. */
    static void pintar(ButtonBase boton, Color cara, Color tinta) {
        Runnable aplicar = () -> {
            Color fondo = boton.isHover() && !boton.isDisabled() ? cara.interpolate(tinta, 0.15) : cara;
            Color borde = boton.isFocused() ? tinta : cara.darker();
            boton.setStyle(boton(fondo, borde, boton.isPressed())
                    + "-fx-text-fill: " + css(tinta) + ";");
        };
        boton.hoverProperty().addListener((p, antes, ahora) -> aplicar.run());
        boton.pressedProperty().addListener((p, antes, ahora) -> aplicar.run());
        boton.focusedProperty().addListener((p, antes, ahora) -> aplicar.run());
        boton.disabledProperty().addListener((p, antes, ahora) -> aplicar.run());
        aplicar.run();
    }

    static String css(Color color) {
        return String.format(Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
