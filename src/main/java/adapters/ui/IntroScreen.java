package adapters.ui;

import adapters.audio.VolumeControl;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pantalla con la que arranca el juego: el video de intro de fondo (a pantalla completa, una sola vez: al
 * terminar queda el último cuadro), el logo en el centro y un botón para comenzar, que lleva al menú. El
 * audio del video va aparte y se repite en bucle, con un fundido entre vuelta y vuelta para que no se note el
 * corte, mientras la pantalla está abierta. Si el video no se puede
 * reproducir, queda el fondo liso con el logo y el botón: la pantalla sirve igual.
 */
final class IntroScreen extends StackPane {
    private static final double PROPORTION_VIDEO = 16.0 / 9.0;
    private final PanelTheme theme;
    private final VolumeControl volume;
    private final Runnable onStart;
    private final ImageView logo = new ImageView();
    private final Button start = new Button("COMENZAR");
    private final MediaView video = new MediaView();
    /** Relación ancho/alto del video, para cubrir la ventana sin deformarlo. */
    private final DoubleProperty videoAspectRatio = new SimpleDoubleProperty(PROPORTION_VIDEO);
    private MediaPlayer videoPlayer;
    /** El audio del video, aparte: se repite en bucle con un fundido entre vuelta y vuelta. */
    private final CrossfadedLoop audio;
    private Animation input;
    private boolean started;

    IntroScreen(PanelTheme theme, VolumeControl volume, Runnable onStart) {
        this.theme = Objects.requireNonNull(theme, "tema");
        this.volume = Objects.requireNonNull(volume, "volumen");
        this.onStart = Objects.requireNonNull(onStart, "alComenzar");
        this.audio = new CrossfadedLoop("/intro/intro.m4a", volume, 1.0, "Intro");
        setId("pantalla-intro");
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(theme.background(), CornerRadii.EMPTY, Insets.EMPTY)));
        setStyle(PixelStyle.familyCss());
        // Lo que sobra del video (si la ventana no es 16:9) se recorta en vez de deformarse.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        video.setId("video-intro");
        video.setPreserveRatio(true);
        video.setMouseTransparent(true);
        video.fitWidthProperty().bind(Bindings.max(widthProperty(), heightProperty().multiply(videoAspectRatio)));

        // Un velo oscuro leve: el logo y el botón se leen aunque el video sea claro.
        Region veil = new Region();
        veil.setMouseTransparent(true);
        veil.setBackground(new Background(new BackgroundFill(theme.background().deriveColor(0, 1, 1, 0.28),
                CornerRadii.EMPTY, Insets.EMPTY)));

        logo.setId("logo-intro");
        logo.setImage(new Image("/intro/logo.png"));
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("ChiquiChess");
        logo.fitWidthProperty().bind(Bindings.min(1100, widthProperty().multiply(0.62)));
        logo.fitHeightProperty().bind(heightProperty().multiply(0.5));

        start.setId("comenzar");
        start.setFont(PixelStyle.font(24, true));
        start.setMinSize(250, 60);
        start.setCursor(javafx.scene.Cursor.HAND);
        start.setDefaultButton(true);
        PixelStyle.paint(start, theme.accent(), theme.background());
        start.setOnAction(event -> start());

        VBox center = new VBox(34, logo, start);
        center.setAlignment(Pos.CENTER);
        center.setMouseTransparent(false);
        center.setPickOnBounds(false);

        HBox sound = new HBox(10);
        sound.setAlignment(Pos.CENTER_LEFT);
        sound.getChildren().addAll(SoundControls.create(volume, "silencio-intro", "volumen-intro",
                theme.border(), theme.texto()));
        sound.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(sound, Pos.BOTTOM_LEFT);
        StackPane.setMargin(sound, new Insets(0, 0, 22, 26));

        getChildren().addAll(video, veil, center, sound);
        sceneProperty().addListener((p, before, scene) -> {
            if (scene != null) start.requestFocus();
        });
    }

    /**
     * Arranca el video, que se reproduce una vez y sin sonido, el audio aparte, que se repite mientras se
     * está en la pantalla (con el volumen del juego), y las animaciones de entrada: el logo aparece y después
     * el botón. Se llama con la ventana ya abierta.
     */
    void play() {
        animateInput();
        startVideo();
        audio.play();
    }

    private void startVideo() {
        try {
            var resource = IntroScreen.class.getResource("/intro/intro.mp4");
            if (resource == null) return;
            Media media = new Media(resource.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            videoPlayer = player;
            // El sonido sale de la pista de audio aparte: el video va mudo.
            player.setMute(true);
            player.setOnReady(() -> {
                if (media.getWidth() > 0 && media.getHeight() > 0) {
                    videoAspectRatio.set((double) media.getWidth() / media.getHeight());
                }
            });
            player.setOnError(() -> {
                System.err.println("Intro sin video: no se pudo reproducir: " + player.getError());
                releaseVideo();
            });
            video.setMediaPlayer(player);
            player.play();
        } catch (RuntimeException | LinkageError failure) {
            System.err.println("Intro sin video: " + failure);
            releaseVideo();
        }
    }

    /** Detiene el video y el audio y deja de escuchar el volumen: la pantalla ya no se usa. */
    void stop() {
        if (input != null) input.stop();
        audio.stop();
        releaseVideo();
    }

    private void start() {
        if (started) return;
        started = true;
        stop();
        onStart.run();
    }

    /** Sin video la pantalla sigue andando: queda el fondo liso. */
    private void releaseVideo() {
        MediaPlayer actual = videoPlayer;
        videoPlayer = null;
        video.setMediaPlayer(null);
        if (actual != null) {
            actual.stop();
            actual.dispose();
        }
    }

    private void animateInput() {
        logo.setOpacity(0);
        logo.setScaleX(0.82);
        logo.setScaleY(0.82);
        start.setOpacity(0);
        FadeTransition appears = new FadeTransition(Duration.millis(900), logo);
        appears.setToValue(1);
        ScaleTransition grows = new ScaleTransition(Duration.millis(900), logo);
        grows.setToX(1);
        grows.setToY(1);
        grows.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition appearsButton = new FadeTransition(Duration.millis(500), start);
        appearsButton.setToValue(1);
        // Cuando ya está todo a la vista, el botón late despacio para invitar a tocarlo.
        ScaleTransition heartbeat = new ScaleTransition(Duration.millis(800), start);
        heartbeat.setFromX(1);
        heartbeat.setFromY(1);
        heartbeat.setToX(1.05);
        heartbeat.setToY(1.05);
        heartbeat.setAutoReverse(true);
        heartbeat.setCycleCount(Animation.INDEFINITE);
        heartbeat.setInterpolator(Interpolator.EASE_BOTH);
        input = new SequentialTransition(new ParallelTransition(appears, grows),
                new PauseTransition(Duration.millis(150)), appearsButton, heartbeat);
        input.play();
    }
}
