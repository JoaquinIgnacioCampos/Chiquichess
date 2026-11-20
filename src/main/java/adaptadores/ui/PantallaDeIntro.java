package adaptadores.ui;

import adaptadores.audio.ControlDeVolumen;
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
final class PantallaDeIntro extends StackPane {
    private static final double PROPORCION_VIDEO = 16.0 / 9.0;
    /** Segundos que dura el fundido entre una vuelta del audio y la siguiente. */
    static final double FUNDIDO = 1.5;
    /** Cuánto antes del fundido se prepara la voz siguiente, para que arranque justo a tiempo. */
    private static final double ANTICIPO = 1.0;

    /** Una pasada del audio: su reproductor y la voz que le sigue, cuando ya se preparó. */
    private static final class Voz {
        final MediaPlayer jugador;
        final boolean conEntrada;
        Voz siguiente;
        boolean siguienteIniciada;

        Voz(MediaPlayer jugador, boolean conEntrada) {
            this.jugador = jugador;
            this.conEntrada = conEntrada;
        }
    }

    /**
     * Qué parte del volumen suena en el segundo {@code t} de una pasada de {@code duracion} segundos: sube al
     * empezar (salvo la primera pasada, que arranca entera) y baja al terminar, con curvas de potencia
     * constante: al sumarse la que termina con la que empieza, el volumen no se hunde ni se dispara.
     */
    static double ganancia(double t, double duracion, boolean conEntrada) {
        double entrada = conEntrada ? Math.sin(Math.PI / 2 * clamp(t / FUNDIDO)) : 1;
        double salida = Math.cos(Math.PI / 2 * clamp((t - (duracion - FUNDIDO)) / FUNDIDO));
        return entrada * salida;
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }

    private final TemaPanel tema;
    private final ControlDeVolumen volumen;
    private final Runnable alComenzar;
    private final ImageView logo = new ImageView();
    private final Button comenzar = new Button("COMENZAR");
    private final MediaView video = new MediaView();
    /** Relación ancho/alto del video, para cubrir la ventana sin deformarlo. */
    private final DoubleProperty proporcionDelVideo = new SimpleDoubleProperty(PROPORCION_VIDEO);
    private final Runnable aplicarVolumen = this::aplicarVolumen;
    private MediaPlayer reproductorDeVideo;
    /** Voces del audio sonando a la vez: una sola, salvo en el fundido de una vuelta a la siguiente. */
    private final List<Voz> voces = new ArrayList<>();
    private boolean sonando;
    private Animation entrada;
    private boolean comenzado;

    PantallaDeIntro(TemaPanel tema, ControlDeVolumen volumen, Runnable alComenzar) {
        this.tema = Objects.requireNonNull(tema, "tema");
        this.volumen = Objects.requireNonNull(volumen, "volumen");
        this.alComenzar = Objects.requireNonNull(alComenzar, "alComenzar");
        setId("pantalla-intro");
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(tema.fondo(), CornerRadii.EMPTY, Insets.EMPTY)));
        setStyle(EstiloPixel.familiaCss());
        // Lo que sobra del video (si la ventana no es 16:9) se recorta en vez de deformarse.
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(widthProperty());
        recorte.heightProperty().bind(heightProperty());
        setClip(recorte);

        video.setId("video-intro");
        video.setPreserveRatio(true);
        video.setMouseTransparent(true);
        video.fitWidthProperty().bind(Bindings.max(widthProperty(), heightProperty().multiply(proporcionDelVideo)));

        // Un velo oscuro leve: el logo y el botón se leen aunque el video sea claro.
        Region velo = new Region();
        velo.setMouseTransparent(true);
        velo.setBackground(new Background(new BackgroundFill(tema.fondo().deriveColor(0, 1, 1, 0.28),
                CornerRadii.EMPTY, Insets.EMPTY)));

        logo.setId("logo-intro");
        logo.setImage(new Image("/intro/logo.png"));
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("ChiquiChess");
        logo.fitWidthProperty().bind(Bindings.min(1100, widthProperty().multiply(0.62)));
        logo.fitHeightProperty().bind(heightProperty().multiply(0.5));

        comenzar.setId("comenzar");
        comenzar.setFont(EstiloPixel.fuente(24, true));
        comenzar.setMinSize(250, 60);
        comenzar.setCursor(javafx.scene.Cursor.HAND);
        comenzar.setDefaultButton(true);
        EstiloPixel.pintar(comenzar, tema.acento(), tema.fondo());
        comenzar.setOnAction(evento -> comenzar());

        VBox centro = new VBox(34, logo, comenzar);
        centro.setAlignment(Pos.CENTER);
        centro.setMouseTransparent(false);
        centro.setPickOnBounds(false);

        HBox sonido = new HBox(10);
        sonido.setAlignment(Pos.CENTER_LEFT);
        sonido.getChildren().addAll(ControlesDeSonido.crear(volumen, "silencio-intro", "volumen-intro",
                tema.borde(), tema.texto()));
        sonido.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(sonido, Pos.BOTTOM_LEFT);
        StackPane.setMargin(sonido, new Insets(0, 0, 22, 26));

        getChildren().addAll(video, velo, centro, sonido);
        sceneProperty().addListener((p, antes, escena) -> {
            if (escena != null) comenzar.requestFocus();
        });
    }

    /**
     * Arranca el video, que se reproduce una vez y sin sonido, el audio aparte, que se repite mientras se
     * está en la pantalla (con el volumen del juego), y las animaciones de entrada: el logo aparece y después
     * el botón. Se llama con la ventana ya abierta.
     */
    void reproducir() {
        animarEntrada();
        iniciarVideo();
        sonando = true;
        iniciarAudio();
        volumen.alCambiar(aplicarVolumen);
    }

    private void iniciarVideo() {
        try {
            var recurso = PantallaDeIntro.class.getResource("/intro/intro.mp4");
            if (recurso == null) return;
            Media media = new Media(recurso.toExternalForm());
            MediaPlayer jugador = new MediaPlayer(media);
            reproductorDeVideo = jugador;
            // El sonido sale de la pista de audio aparte: el video va mudo.
            jugador.setMute(true);
            jugador.setOnReady(() -> {
                if (media.getWidth() > 0 && media.getHeight() > 0) {
                    proporcionDelVideo.set((double) media.getWidth() / media.getHeight());
                }
            });
            jugador.setOnError(() -> {
                System.err.println("Intro sin video: no se pudo reproducir: " + jugador.getError());
                soltarVideo();
            });
            video.setMediaPlayer(jugador);
            jugador.play();
        } catch (RuntimeException | LinkageError fallo) {
            System.err.println("Intro sin video: " + fallo);
            soltarVideo();
        }
    }

    private void iniciarAudio() {
        Voz primera = crearVoz(false);
        if (primera != null) primera.jugador.play();
    }

    /** Prepara una pasada del audio (sin arrancarla). Cada una avisa cuándo toca preparar y soltar la siguiente. */
    private Voz crearVoz(boolean conEntrada) {
        try {
            var recurso = PantallaDeIntro.class.getResource("/intro/intro.m4a");
            if (recurso == null) return null;
            Media media = new Media(recurso.toExternalForm());
            MediaPlayer jugador = new MediaPlayer(media);
            Voz voz = new Voz(jugador, conEntrada);
            voces.add(voz);
            jugador.setOnError(() -> {
                System.err.println("Intro sin audio: no se pudo reproducir: " + jugador.getError());
                soltarVoz(voz);
            });
            jugador.setOnEndOfMedia(() -> Platform.runLater(() -> soltarVoz(voz)));
            jugador.currentTimeProperty().addListener((p, antes, ahora) -> alAvanzar(voz, media, ahora.toSeconds()));
            aplicarVolumen(voz, 0, media.getDuration().toSeconds());
            return voz;
        } catch (RuntimeException | LinkageError fallo) {
            System.err.println("Intro sin audio: " + fallo);
            return null;
        }
    }

    private void alAvanzar(Voz voz, Media media, double t) {
        double duracion = media.getDuration().toSeconds();
        if (!sonando || Double.isNaN(duracion) || Double.isInfinite(duracion)) return;
        aplicarVolumen(voz, t, duracion);
        if (voz.siguiente == null && t >= duracion - FUNDIDO - ANTICIPO) voz.siguiente = crearVoz(true);
        if (voz.siguiente != null && !voz.siguienteIniciada && t >= duracion - FUNDIDO) {
            voz.siguienteIniciada = true;
            voz.siguiente.jugador.play();
        }
    }

    private void aplicarVolumen(Voz voz, double t, double duracion) {
        double base = Double.isNaN(duracion) || Double.isInfinite(duracion) ? (voz.conEntrada ? 0 : 1)
                : ganancia(t, duracion, voz.conEntrada);
        voz.jugador.setVolume(volumen.efectivo() * base);
    }

    private void soltarVoz(Voz voz) {
        if (!voces.remove(voz)) return;
        voz.jugador.stop();
        voz.jugador.dispose();
    }

    /** Detiene el video y el audio y deja de escuchar el volumen: la pantalla ya no se usa. */
    void detener() {
        if (entrada != null) entrada.stop();
        volumen.dejarDeEscuchar(aplicarVolumen);
        sonando = false;
        soltarVideo();
        new ArrayList<>(voces).forEach(this::soltarVoz);
    }

    private void comenzar() {
        if (comenzado) return;
        comenzado = true;
        detener();
        alComenzar.run();
    }

    /** Cambió el volumen del juego: cada voz lo aplica sobre su fundido. */
    private void aplicarVolumen() {
        for (Voz voz : new ArrayList<>(voces)) {
            aplicarVolumen(voz, voz.jugador.getCurrentTime().toSeconds(), voz.jugador.getTotalDuration().toSeconds());
        }
    }

    /** Sin video la pantalla sigue andando: queda el fondo liso. */
    private void soltarVideo() {
        MediaPlayer actual = reproductorDeVideo;
        reproductorDeVideo = null;
        video.setMediaPlayer(null);
        if (actual != null) {
            actual.stop();
            actual.dispose();
        }
    }

    private void animarEntrada() {
        logo.setOpacity(0);
        logo.setScaleX(0.82);
        logo.setScaleY(0.82);
        comenzar.setOpacity(0);
        FadeTransition aparece = new FadeTransition(Duration.millis(900), logo);
        aparece.setToValue(1);
        ScaleTransition crece = new ScaleTransition(Duration.millis(900), logo);
        crece.setToX(1);
        crece.setToY(1);
        crece.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition apareceBoton = new FadeTransition(Duration.millis(500), comenzar);
        apareceBoton.setToValue(1);
        // Cuando ya está todo a la vista, el botón late despacio para invitar a tocarlo.
        ScaleTransition latido = new ScaleTransition(Duration.millis(800), comenzar);
        latido.setFromX(1);
        latido.setFromY(1);
        latido.setToX(1.05);
        latido.setToY(1.05);
        latido.setAutoReverse(true);
        latido.setCycleCount(Animation.INDEFINITE);
        latido.setInterpolator(Interpolator.EASE_BOTH);
        entrada = new SequentialTransition(new ParallelTransition(aparece, crece),
                new PauseTransition(Duration.millis(150)), apareceBoton, latido);
        entrada.play();
    }
}
