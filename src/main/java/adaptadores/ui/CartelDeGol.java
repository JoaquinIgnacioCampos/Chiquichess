package adaptadores.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Objects;
import java.util.Optional;

/**
 * Celebración de un gol sobre el tablero: el escudo del equipo, "¡GOL!" en un tablero de luces del color
 * del equipo y su nombre. Aparece de golpe con un rebote, se sacude mientras las luces titilan y se
 * desvanece; dura unos 2 segundos. No intercepta clics: se puede seguir jugando mientras se ve.
 */
public final class CartelDeGol extends StackPane {
    static final String TEXTO = "¡GOL!";

    private final TemaPanel tema;
    private final ImageView escudo = new ImageView();
    private final TextoLed gol;
    private final Text nombre = new Text();
    private final VBox tarjeta;
    private Animation celebracion;

    public CartelDeGol(TemaPanel tema) {
        this.tema = Objects.requireNonNull(tema, "tema");
        setId("cartel-gol");
        setMouseTransparent(true);
        setVisible(false);
        setPickOnBounds(false);
        escudo.setFitWidth(96);
        escudo.setFitHeight(96);
        escudo.setPreserveRatio(true);
        escudo.setSmooth(true);
        gol = new TextoLed(tema.acento());
        gol.setId("gol-luces");
        gol.setTexto(TEXTO);
        // Ancho proporcional al tablero; la altura sale de las 5 letras de 5x7 puntos.
        gol.prefWidthProperty().bind(widthProperty().multiply(0.62));
        gol.prefHeightProperty().bind(gol.prefWidthProperty().multiply(7.0 / 29));
        gol.setMinHeight(Region.USE_PREF_SIZE);
        nombre.setId("gol-equipo");
        nombre.setFont(EstiloPixel.fuente(26, true));
        tarjeta = new VBox(12, escudo, gol, nombre);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(22, 30, 22, 30));
        tarjeta.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getChildren().add(tarjeta);
        StackPane.setAlignment(tarjeta, Pos.CENTER);
    }

    /**
     * Muestra la celebración y devuelve la animación (ya iniciada). Si había otra en curso, la corta.
     *
     * @param equipo nombre que se muestra debajo de las luces
     * @param color  color de las luces y del borde (el del equipo)
     * @param escudo escudo del equipo; vacío si la partida no tiene clubes
     */
    public Animation celebrar(String equipo, Color color, Optional<Image> escudo) {
        Objects.requireNonNull(equipo, "equipo");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(escudo, "escudo");
        if (celebracion != null) celebracion.stop();
        this.escudo.setImage(escudo.orElse(null));
        this.escudo.setVisible(escudo.isPresent());
        this.escudo.setManaged(escudo.isPresent());
        gol.setColor(color);
        gol.setColorApagado(color.deriveColor(0, 1, 1, 0.13));
        gol.setOpacity(1);
        nombre.setText(equipo.toUpperCase(java.util.Locale.ROOT));
        nombre.setFill(tema.texto());
        tarjeta.setBackground(new Background(new BackgroundFill(tema.fondo().deriveColor(0, 1, 1, 0.9),
                new CornerRadii(16), Insets.EMPTY)));
        tarjeta.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(16),
                new BorderWidths(3))));
        tarjeta.setScaleX(0.2);
        tarjeta.setScaleY(0.2);
        tarjeta.setTranslateX(0);
        setOpacity(1);
        setVisible(true);

        celebracion = new SequentialTransition(entrada(), new ParallelTransition(sacudida(), titileo()),
                new PauseTransition(Duration.millis(350)), salida());
        celebracion.setOnFinished(evento -> setVisible(false));
        celebracion.play();
        return celebracion;
    }

    /** Aparece de golpe: crece un poco de más y vuelve, como un rebote. */
    private Animation entrada() {
        ScaleTransition crece = new ScaleTransition(Duration.millis(240), tarjeta);
        crece.setToX(1.15);
        crece.setToY(1.15);
        ScaleTransition asienta = new ScaleTransition(Duration.millis(140), tarjeta);
        asienta.setToX(1);
        asienta.setToY(1);
        return new SequentialTransition(crece, asienta);
    }

    private Animation sacudida() {
        TranslateTransition sacudida = new TranslateTransition(Duration.millis(55), tarjeta);
        sacudida.setFromX(-10);
        sacudida.setToX(10);
        sacudida.setCycleCount(8);
        sacudida.setAutoReverse(true);
        TranslateTransition alCentro = new TranslateTransition(Duration.millis(40), tarjeta);
        alCentro.setToX(0);
        return new SequentialTransition(sacudida, alCentro);
    }

    /** Las luces se apagan y prenden, como un cartel de estadio. */
    private Animation titileo() {
        Timeline titileo = new Timeline(
                new KeyFrame(Duration.millis(0), evento -> gol.setOpacity(1)),
                new KeyFrame(Duration.millis(110), evento -> gol.setOpacity(0.2)),
                new KeyFrame(Duration.millis(220), evento -> gol.setOpacity(1)));
        titileo.setCycleCount(4);
        return titileo;
    }

    private Animation salida() {
        FadeTransition salida = new FadeTransition(Duration.millis(450), this);
        salida.setFromValue(1);
        salida.setToValue(0);
        return salida;
    }
}
