package adapters.ui;

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
public final class GoalBanner extends StackPane {
    static final String TEXT = "GOAL!";

    private final PanelTheme theme;
    private final ImageView crest = new ImageView();
    private final LedText goal;
    private final Text displayName = new Text();
    private final VBox card;
    private Animation celebration;

    public GoalBanner(PanelTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        setId("sign-goal");
        setMouseTransparent(true);
        setVisible(false);
        setPickOnBounds(false);
        crest.setFitWidth(96);
        crest.setFitHeight(96);
        crest.setPreserveRatio(true);
        crest.setSmooth(true);
        goal = new LedText(theme.accent());
        goal.setId("goal-lights");
        goal.setText(TEXT);
        // Ancho proporcional al tablero; la altura sale de las 5 letras de 5x7 puntos.
        goal.prefWidthProperty().bind(widthProperty().multiply(0.62));
        goal.prefHeightProperty().bind(goal.prefWidthProperty().multiply(7.0 / 29));
        goal.setMinHeight(Region.USE_PREF_SIZE);
        displayName.setId("goal-team");
        displayName.setFont(PixelStyle.font(26, true));
        card = new VBox(12, crest, goal, displayName);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(22, 30, 22, 30));
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    /**
     * Muestra la celebración y devuelve la animación (ya iniciada). Si había otra en curso, la corta.
     *
     * @param equipo nombre que se muestra debajo de las luces
     * @param color  color de las luces y del borde (el del equipo)
     * @param escudo escudo del equipo; vacío si la partida no tiene clubes
     */
    public Animation celebrate(String team, Color color, Optional<Image> crest) {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(crest, "crest");
        if (celebration != null) celebration.stop();
        this.crest.setImage(crest.orElse(null));
        this.crest.setVisible(crest.isPresent());
        this.crest.setManaged(crest.isPresent());
        goal.setColor(color);
        goal.setColorOff(color.deriveColor(0, 1, 1, 0.13));
        goal.setOpacity(1);
        displayName.setText(team.toUpperCase(java.util.Locale.ROOT));
        displayName.setFill(theme.texto());
        card.setBackground(new Background(new BackgroundFill(theme.background().deriveColor(0, 1, 1, 0.9),
                new CornerRadii(16), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(color, BorderStrokeStyle.SOLID, new CornerRadii(16),
                new BorderWidths(3))));
        card.setScaleX(0.2);
        card.setScaleY(0.2);
        card.setTranslateX(0);
        setOpacity(1);
        setVisible(true);

        celebration = new SequentialTransition(input(), new ParallelTransition(shake(), flicker()),
                new PauseTransition(Duration.millis(350)), output());
        celebration.setOnFinished(event -> setVisible(false));
        celebration.play();
        return celebration;
    }

    /** Aparece de golpe: crece un poco de más y vuelve, como un rebote. */
    private Animation input() {
        ScaleTransition grows = new ScaleTransition(Duration.millis(240), card);
        grows.setToX(1.15);
        grows.setToY(1.15);
        ScaleTransition settles = new ScaleTransition(Duration.millis(140), card);
        settles.setToX(1);
        settles.setToY(1);
        return new SequentialTransition(grows, settles);
    }

    private Animation shake() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), card);
        shake.setFromX(-10);
        shake.setToX(10);
        shake.setCycleCount(8);
        shake.setAutoReverse(true);
        TranslateTransition onCenter = new TranslateTransition(Duration.millis(40), card);
        onCenter.setToX(0);
        return new SequentialTransition(shake, onCenter);
    }

    /** Las luces se apagan y prenden, como un cartel de estadio. */
    private Animation flicker() {
        Timeline flicker = new Timeline(
                new KeyFrame(Duration.millis(0), event -> goal.setOpacity(1)),
                new KeyFrame(Duration.millis(110), event -> goal.setOpacity(0.2)),
                new KeyFrame(Duration.millis(220), event -> goal.setOpacity(1)));
        flicker.setCycleCount(4);
        return flicker;
    }

    private Animation output() {
        FadeTransition output = new FadeTransition(Duration.millis(450), this);
        output.setFromValue(1);
        output.setToValue(0);
        return output;
    }
}
