package adaptadores.ui;

import adaptadores.audio.ControlDeVolumen;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

/**
 * Botón de silencio y volumen de un {@link ControlDeVolumen}. Los dos muestran siempre el estado del
 * control: silenciar lleva el volumen a cero y subirlo desde cero vuelve a activar el sonido.
 */
final class ControlesDeSonido {
    private ControlesDeSonido() {
    }

    /** El botón y el volumen, listos para agregar. Dejan de escuchar al control cuando salen de la pantalla. */
    static List<Node> crear(ControlDeVolumen control, String idSilencio, String idVolumen, Color fondo, Color tinta) {
        Objects.requireNonNull(control, "control");
        Button silencio = BotonDeCabecera.crear(texto(control), idSilencio, fondo, tinta);
        // Al lado de textos que pueden ocupar varios renglones: el botón nunca se achica hasta cortarse.
        silencio.setMinWidth(Region.USE_PREF_SIZE);
        silencio.setOnAction(e -> control.silenciar(!control.silenciado()));
        Slider volumen = new Slider(0, 1, control.efectivo());
        volumen.setId(idVolumen);
        volumen.setAccessibleText("Volumen");
        volumen.setPrefWidth(90);
        volumen.setMinWidth(Region.USE_PREF_SIZE);
        volumen.setFocusTraversable(false);
        volumen.valueProperty().addListener((p, antes, ahora) -> control.cambiarVolumen(ahora.doubleValue()));
        Runnable mostrar = () -> {
            silencio.setText(texto(control));
            volumen.setValue(control.efectivo());
        };
        control.alCambiar(mostrar);
        // El control vive todo el programa: una pantalla que se descarta deja de escucharlo.
        silencio.sceneProperty().addListener((p, antes, ahora) -> {
            if (antes != null && ahora == null) control.dejarDeEscuchar(mostrar);
        });
        return List.of(silencio, volumen);
    }

    private static String texto(ControlDeVolumen control) {
        return control.silenciado() ? "Sonido: no" : "Sonido: sí";
    }
}
