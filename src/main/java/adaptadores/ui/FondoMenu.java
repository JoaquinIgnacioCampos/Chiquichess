package adaptadores.ui;

import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Objects;

/** Video de fondo y audio de entrada del menú, con ciclos independientes. */
final class FondoMenu extends StackPane {
    private final MediaView imagen = new MediaView();
    private MediaPlayer video;
    private MediaPlayer audio;

    FondoMenu() {
        setMinSize(0, 0);
        setMouseTransparent(true);
        imagen.fitWidthProperty().bind(Bindings.max(widthProperty(), heightProperty().multiply(1280.0 / 720)));
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(widthProperty());
        recorte.heightProperty().bind(heightProperty());
        setClip(recorte);

        Region sombra = new Region();
        sombra.setStyle("-fx-background-color: linear-gradient(to right, rgba(13,22,19,0.89),"
                + " rgba(13,22,19,0.70) 58%, rgba(13,22,19,0.46));");
        getChildren().addAll(imagen, sombra);
    }

    void reproducir(double volumen) {
        detener();
        video = abrir("/menu/fondo.mp4");
        if (video != null) {
            imagen.setMediaPlayer(video);
            video.setMute(true);
            video.setCycleCount(MediaPlayer.INDEFINITE);
            video.play();
        }

        audio = abrir("/menu/entrada.mp3");
        if (audio != null) {
            audio.setStopTime(Duration.seconds(10));
            audio.setVolume(volumen);
            audio.setMute(volumen == 0);
            audio.play();
        }
    }

    void detener() {
        imagen.setMediaPlayer(null);
        if (video != null) video.dispose();
        if (audio != null) audio.dispose();
        video = null;
        audio = null;
    }

    private static MediaPlayer abrir(String ruta) {
        var recurso = Objects.requireNonNull(FondoMenu.class.getResource(ruta), ruta);
        try {
            MediaPlayer reproductor = new MediaPlayer(new Media(recurso.toExternalForm()));
            reproductor.setOnError(() -> System.err.println("ChiquiChess: " + reproductor.getError()));
            return reproductor;
        } catch (MediaException error) {
            System.err.println("ChiquiChess: " + error);
            return null;
        }
    }
}
