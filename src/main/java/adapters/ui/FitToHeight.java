package adapters.ui;

import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Muestra su contenido entero sin desplazamiento: si no entra en el alto disponible, lo achica de forma
 * pareja (texto, botones y espacios) hasta que entre. Si sobra lugar, lo deja en su tamaño natural.
 */
final class FitToHeight extends Region {
    private final Region content;
    private final Scale scale = new Scale(1, 1, 0, 0);

    FitToHeight(Region content) {
        this.content = content;
        content.getTransforms().add(scale);
        getChildren().add(content);
        setMinSize(0, 0);
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        // Achicado, el contenido gana ancho y puede necesitar menos alto: unas pocas vueltas afinan el
        // factor para aprovechar todo el alto sin pasarse.
        double factor = 1;
        for (int lap = 0; lap < 4 && height > 0 && width > 0; lap++) {
            double necessary = content.prefHeight(width / factor);
            double newValue = Math.min(1, height / necessary);
            if (content.prefHeight(width / newValue) * newValue > height) break;
            factor = newValue;
        }
        scale.setX(factor);
        scale.setY(factor);
        // Achicado, el contenido se acomoda a un ancho mayor para seguir ocupando todo el ancho visible.
        double widthInternal = width / factor;
        content.resizeRelocate(0, 0, widthInternal,
                Math.max(height / factor, content.prefHeight(widthInternal)));
    }

    @Override
    protected double computePrefWidth(double height) {
        return content.prefWidth(-1);
    }

    @Override
    protected double computePrefHeight(double width) {
        return content.prefHeight(width);
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }
}
