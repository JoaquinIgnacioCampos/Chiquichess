package adapters.ui;

import adapters.audio.VolumeControl;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Objects;

/**
 * Botón de silencio (un silbato de árbitro) y volumen de un {@link VolumeControl}. Los dos muestran
 * siempre el estado del control: silenciar lleva el volumen a cero y subirlo desde cero vuelve a activar
 * el sonido.
 */
final class SoundControls {
    private static final double WIDTH = 48;
    private static final double HEIGHT = 34;

    private SoundControls() {
    }

    /** El botón y el volumen, listos para agregar. Dejan de escuchar al control cuando salen de la pantalla. */
    static List<Node> create(VolumeControl control, String idSilence, String idVolume, Color background, Color ink) {
        Objects.requireNonNull(control, "control");
        Button silence = HeaderButton.create("", idSilence, background, ink);
        // Tamaño fijo: el silbato con ondas y el tachado no miden lo mismo, y el botón no debe cambiar.
        silence.setPadding(javafx.geometry.Insets.EMPTY);
        silence.setMinSize(WIDTH, HEIGHT);
        silence.setPrefSize(WIDTH, HEIGHT);
        silence.setMaxSize(WIDTH, HEIGHT);
        silence.setTooltip(new Tooltip());
        // Al lado de textos que pueden ocupar varios renglones: el botón nunca se achica hasta cortarse.
        silence.setMinWidth(Region.USE_PREF_SIZE);
        silence.setOnAction(e -> control.mute(!control.muted()));
        Slider volume = new Slider(0, 1, control.effective());
        volume.setId(idVolume);
        volume.setAccessibleText("Volumen");
        volume.setPrefWidth(90);
        volume.setMinWidth(Region.USE_PREF_SIZE);
        volume.setFocusTraversable(false);
        volume.valueProperty().addListener((p, before, now) -> control.changeVolume(now.doubleValue()));
        Runnable show = () -> {
            javafx.scene.layout.StackPane icon = new javafx.scene.layout.StackPane(
                    WhistleIcon.create(!control.muted(), ink, background));
            icon.setMinSize(26, 24);
            icon.setPrefSize(26, 24);
            icon.setMaxSize(26, 24);
            silence.setGraphic(icon);
            silence.setAccessibleText(texto(control));
            silence.getTooltip().setText(control.muted() ? "Activar el sonido" : "Silenciar");
            volume.setValue(control.effective());
        };
        show.run();
        control.onChange(show);
        // El control vive todo el programa: una pantalla que se descarta deja de escucharlo.
        silence.sceneProperty().addListener((p, before, now) -> {
            if (before != null && now == null) control.stopListening(show);
        });
        return List.of(silence, volume);
    }

    private static String texto(VolumeControl control) {
        return control.muted() ? "Sonido: no" : "Sonido: sí";
    }
}
