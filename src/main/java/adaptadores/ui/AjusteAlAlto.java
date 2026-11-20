package adaptadores.ui;

import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Muestra su contenido entero sin desplazamiento: si no entra en el alto disponible, lo achica de forma
 * pareja (texto, botones y espacios) hasta que entre. Si sobra lugar, lo deja en su tamaño natural.
 */
final class AjusteAlAlto extends Region {
    private final Region contenido;
    private final Scale escala = new Scale(1, 1, 0, 0);

    AjusteAlAlto(Region contenido) {
        this.contenido = contenido;
        contenido.getTransforms().add(escala);
        getChildren().add(contenido);
        setMinSize(0, 0);
    }

    @Override
    protected void layoutChildren() {
        double ancho = getWidth();
        double alto = getHeight();
        // Achicado, el contenido gana ancho y puede necesitar menos alto: unas pocas vueltas afinan el
        // factor para aprovechar todo el alto sin pasarse.
        double factor = 1;
        for (int vuelta = 0; vuelta < 4 && alto > 0 && ancho > 0; vuelta++) {
            double necesario = contenido.prefHeight(ancho / factor);
            double nuevo = Math.min(1, alto / necesario);
            if (contenido.prefHeight(ancho / nuevo) * nuevo > alto) break;
            factor = nuevo;
        }
        escala.setX(factor);
        escala.setY(factor);
        // Achicado, el contenido se acomoda a un ancho mayor para seguir ocupando todo el ancho visible.
        double anchoInterno = ancho / factor;
        contenido.resizeRelocate(0, 0, anchoInterno,
                Math.max(alto / factor, contenido.prefHeight(anchoInterno)));
    }

    @Override
    protected double computePrefWidth(double alto) {
        return contenido.prefWidth(-1);
    }

    @Override
    protected double computePrefHeight(double ancho) {
        return contenido.prefHeight(ancho);
    }

    @Override
    protected double computeMinHeight(double ancho) {
        return 0;
    }
}
