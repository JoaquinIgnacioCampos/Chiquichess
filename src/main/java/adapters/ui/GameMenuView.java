package adapters.ui;

import core.ia.Difficulty;
import adapters.audio.VolumeControl;
import adapters.configuration.GameConfiguration;
import adapters.configuration.GameConfiguration.*;
import adapters.configuration.MenuSelection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Menú de selección: emite una configuración completa; no crea partidas ni relojes.
 *
 * Estilo de estadio en pixel art: títulos con las letras de puntos del marcador, tarjetas y botones de
 * bordes rectos con sombra dura, y un fondo con las franjas del pasto. Las opciones van en una grilla de
 * tarjetas para que todo entre sin desplazarse; las que dependen de otra aparecen dentro de su tarjeta.
 */
public final class GameMenuView extends GridPane {
    private static final double HEIGHT_TITLE = 16;
    /** Clave con la que cada opción dependiente guarda el texto que la reemplaza cuando no aplica. */
    private static final String TRACK = "track";

    private final PanelTheme theme;
    private final Consumer<GameConfiguration> start;
    private final ToggleGroup mode = new ToggleGroup();
    private final ToggleGroup time = new ToggleGroup();
    private final ToggleGroup rival = new ToggleGroup();
    private final ToggleGroup team1 = new ToggleGroup();
    private final ToggleGroup team2 = new ToggleGroup();
    private final ToggleGroup colors = new ToggleGroup();
    /** Solo se habilita con el rival IA. */
    private final ToggleGroup difficulty = new ToggleGroup();
    private final Label noticeDifficulty = new Label();
    private final ToggleButton allowUndo;
    private final Label noticeUndo = new Label();
    /** Modo fútbol: jugadas (de los dos) a las que termina la partida. Solo se habilita con ese modo. */
    private final Spinner<Integer> moveLimit = new Spinner<>(GameConfiguration.LIMIT_MINIMUM,
            GameConfiguration.LIMIT_MAXIMUM, GameConfiguration.DEFAULT_LIMIT, 10);
    private final Label noticeLimit = new Label();
    private final Button play = new Button("PLAY");
    /** La imagen del personaje de la portada (se balancea al abrir el juego). */
    private ImageView figure;
    private Runnable onChooseOption = () -> { };
    private final HBox pie;
    private final Label pending = new Label();
    /** Club elegido por cada jugador, al lado de su nombre. */
    private final Text clubPlayer1 = new Text();
    private final Text clubPlayer2 = new Text();
    /** Opciones que dependen de otra: se muestran dentro de su tarjeta solo cuando aplican. */
    private final VBox panelLimit;
    private final VBox panelUndo;
    private final VBox panelDifficulty;

    public GameMenuView(PanelTheme theme, Consumer<GameConfiguration> start) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.start = Objects.requireNonNull(start, "start");
        setId("menu-game");
        setMinSize(0, 0);
        setBackground(background(theme.background()));
        setStyle("-fx-base: " + css(theme.card()) + "; -fx-accent: " + css(theme.accent())
                + "; -fx-focus-color: " + css(theme.accent()) + "; -fx-faint-focus-color: transparent;");
        for (double percentage : new double[]{40, 60}) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percentage);
            column.setMinWidth(0);
            getColumnConstraints().add(column);
        }
        RowConstraints rowOnly = new RowConstraints();
        rowOnly.setPercentHeight(100);
        getRowConstraints().add(rowOnly);
        add(cover(), 0, 0);

        configureLimit();
        panelLimit = dependent("Move limit", new VBox(6, moveLimit, noticeLimit));
        Node cardMode = card("MODE", new VBox(row(
                option("Classic", "mode-classic", Mode.CLASSIC, mode),
                option("Football", "mode-football", Mode.FOOTBALL, mode)),
                slot(panelLimit, "With Football the move limit is chosen.")));

        FlowPane times = row();
        for (Time value : Time.values()) times.getChildren().add(option(textFor(value),
                "time-" + value.name().toLowerCase(java.util.Locale.ROOT), value, time));
        allowUndo = option("Allow undo", "allow-undo", Boolean.TRUE, null);
        notice(noticeUndo, "notice-undo");
        noticeUndo.setText("Both can undo moves.");
        panelUndo = dependent("Undo moves", new VBox(6, row(allowUndo), noticeUndo));
        Node cardTime = card("TIME", new VBox(times,
                slot(panelUndo, "Without a clock, moves can be undone.")));

        notice(noticeDifficulty, "notice-difficulty");
        noticeDifficulty.setText("The AI is Player 2.");
        panelDifficulty = dependent("AI difficulty", new VBox(6, row(
                option("Easy", "difficulty-easy", Difficulty.EASY, difficulty),
                option("Middle", "difficulty-middle", Difficulty.MIDDLE, difficulty),
                option("Hard", "difficulty-hard", Difficulty.HARD, difficulty)), noticeDifficulty));
        Node cardRival = card("RIVAL", new VBox(row(
                option("Friend", "rival-friend", Rival.FRIEND, rival),
                option("AI", "rival-ai", Rival.IA, rival)),
                slot(panelDifficulty, "Against the AI the difficulty is chosen.")));

        Node cardColors = card("PLAYS WHITE", row(
                option("Player 1", "color-player-1", Colors.PLAYER_1_WHITE, colors),
                option("Player 2", "color-player-2", Colors.PLAYER_2_WHITE, colors),
                option("Random", "color-random", Colors.RANDOM, colors)));

        Node cardTeams = card("TEAMS", new VBox(10,
                selectionTeam("Player 1", "player-1", team1, clubPlayer1),
                selectionTeam("Player 2", "player-2", team2, clubPlayer2)));

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setMinWidth(0);
            grid.getColumnConstraints().add(column);
        }
        grid.add(cardMode, 0, 0);
        grid.add(cardTime, 1, 0);
        grid.add(cardRival, 0, 1);
        grid.add(cardColors, 1, 1);
        grid.add(cardTeams, 0, 2, 2, 1);

        Text lowered = texto("Choose how to play and the teams.", 13, theme.textSecondary(), false);
        VBox options = new VBox(14, new VBox(6, title("SET UP THE GAME", 22), lowered), grid);
        options.setPadding(new Insets(16, 26, 12, 26));
        options.setMinWidth(0);

        pending.setId("selections-pending");
        pending.setFont(PixelStyle.font(12, true));
        pending.setTextFill(theme.textSecondary());
        pending.setWrapText(true);
        // Puede necesitar varios renglones en ventanas angostas: crece en alto en vez de cortarse.
        pending.setMinHeight(Region.USE_PREF_SIZE);
        pending.setMinWidth(0);
        pending.setMaxWidth(Double.MAX_VALUE);
        play.setId("play");
        play.setFont(PixelStyle.font(18, true));
        play.setMinWidth(150);
        play.setPrefWidth(150);
        play.setMaxWidth(150);
        play.setCursor(Cursor.HAND);
        play.focusedProperty().addListener((p, before, focus) -> paintPlay());
        play.disabledProperty().addListener((p, before, disabled) -> paintPlay());
        play.setOnAction(e -> emitConfiguration());
        pie = new HBox(14, pending, play);
        pie.setAlignment(Pos.CENTER_RIGHT);
        pie.setPadding(new Insets(10, 26, 14, 26));
        HBox.setHgrow(pending, Priority.ALWAYS);
        pie.setBackground(new Background(new BackgroundFill(theme.card(), CornerRadii.EMPTY, Insets.EMPTY)));
        pie.setBorder(new Border(new BorderStroke(theme.border(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(3, 0, 0, 0))));

        // Si la ventana es muy baja, las opciones se achican para entrar sin desplazamiento; "Jugar"
        // queda siempre fijo debajo.
        FitToHeight offset = new FitToHeight(options);
        offset.setId("options-menu");
        String grass = "linear-gradient(from 0px 0px to 112px 0px, repeat, #0C1712 50%, #0F1D16 50%)";
        offset.setStyle("-fx-background-color: " + grass + ";");
        options.setStyle("-fx-background-color: transparent;");
        VBox columnOptions = new VBox(offset, pie);
        columnOptions.setMinSize(0, 0);
        VBox.setVgrow(offset, Priority.ALWAYS);
        add(columnOptions, 1, 0);
        for (ToggleGroup group : new ToggleGroup[]{mode, time, rival, team1, team2, colors, difficulty}) {
            group.selectedToggleProperty().addListener((p, before, now) -> updateSelection());
        }
        updateSelection();
    }

    /**
     * El personaje se ríe: se balancea de un lado al otro, con un pequeño rebote, y vuelve a quedar
     * quieto. Dura unos 3 segundos, lo que dura la risa. Devuelve la animación (ya iniciada).
     */
    public javafx.animation.Animation animateCharacter() {
        javafx.util.Duration swayDuration = javafx.util.Duration.millis(260);
        var toLeft = new javafx.animation.RotateTransition(javafx.util.Duration.millis(150), figure);
        toLeft.setToAngle(-9);
        var sway = new javafx.animation.RotateTransition(swayDuration, figure);
        sway.setFromAngle(-9);
        sway.setToAngle(9);
        sway.setCycleCount(10);
        sway.setAutoReverse(true);
        var bounce = new javafx.animation.ScaleTransition(swayDuration, figure);
        bounce.setFromX(1);
        bounce.setFromY(1);
        bounce.setToX(1.06);
        bounce.setToY(1.06);
        bounce.setCycleCount(10);
        bounce.setAutoReverse(true);
        var onCenter = new javafx.animation.RotateTransition(javafx.util.Duration.millis(200), figure);
        onCenter.setToAngle(0);
        var laugh = new javafx.animation.SequentialTransition(toLeft,
                new javafx.animation.ParallelTransition(sway, bounce), onCenter);
        laugh.setOnFinished(event -> {
            figure.setRotate(0);
            figure.setScaleX(1);
            figure.setScaleY(1);
        });
        laugh.play();
        return laugh;
    }

    /** Qué hacer cuando la persona toca una opción del menú (por ejemplo, un clic de sonido). */
    public void onChooseOption(Runnable action) {
        onChooseOption = Objects.requireNonNull(action, "action");
    }

    /**
     * Agrega al pie el botón de silencio y el volumen, con el mismo control que la partida: lo que se elige
     * acá sigue valiendo al jugar.
     */
    public void configureSound(VolumeControl control) {
        Objects.requireNonNull(control, "control");
        if (pie.lookup("#silence-menu") != null) throw new IllegalStateException("Sound already configured");
        pie.getChildren().addAll(0, SoundControls.create(control, "silence-menu", "volume-menu",
                theme.border(), theme.texto()));
    }

    /** Deja elegidas esas opciones, como si la persona las hubiera tocado (pero sin el clic de sonido). */
    public void preselect(MenuSelection selection) {
        Objects.requireNonNull(selection, "selection");
        choose(mode, selection.mode());
        // El tiempo y el rival van antes que deshacer y dificultad, que dependen de ellos.
        choose(time, selection.time());
        choose(rival, selection.rival());
        choose(difficulty, selection.difficulty());
        choose(team1, selection.teamPlayer1());
        choose(team2, selection.teamPlayer2());
        choose(colors, selection.colors());
        moveLimit.getValueFactory().setValue(selection.moveLimit());
        updateSelection();
        allowUndo.setSelected(selection.allowUndo() && !allowUndo.isDisable());
    }

    private static void choose(ToggleGroup group, java.util.Optional<?> value) {
        group.selectToggle(group.getToggles().stream()
                .filter(option -> value.isPresent() && value.get().equals(option.getUserData()))
                .findFirst().orElse(null));
    }

    private static String textFor(Time value) {
        return switch (value) {
            case THREE_MINUTES -> "3 min";
            case FIVE_MINUTES -> "5 min";
            case TEN_MINUTES -> "10 min";
            case WITHOUT_TIME -> "No clock";
        };
    }

    private void configureLimit() {
        moveLimit.setId("limit-moves");
        moveLimit.setEditable(true);
        moveLimit.setPrefWidth(110);
        moveLimit.getEditor().setFont(PixelStyle.font(13, true));
        moveLimit.setStyle("-fx-background-color: " + css(theme.border()) + "; -fx-background-radius: 0;");
        moveLimit.getEditor().setStyle("-fx-background-color: " + css(theme.background())
                + "; -fx-text-fill: " + css(theme.texto()) + "; -fx-background-radius: 0;");
        // Lo escrito a mano se toma al salir del campo (y también al apretar Jugar).
        moveLimit.getEditor().focusedProperty().addListener((p, before, focus) -> {
            if (!focus) confirmLimitWritten();
        });
        notice(noticeLimit, "notice-limit");
        noticeLimit.setText("Moves of both together (from " + GameConfiguration.LIMIT_MINIMUM + " to "
                + GameConfiguration.LIMIT_MAXIMUM + "). When it is reached, whoever has more goals wins.");
    }

    private void notice(Label notice, String id) {
        notice.setId(id);
        notice.setFont(PixelStyle.font(12, false));
        notice.setTextFill(theme.textSecondary());
        notice.setWrapText(true);
    }

    private static Image loadCharacter() {
        var resource = Objects.requireNonNull(GameMenuView.class.getResource("/character/chiquichess.png"),
                "Missing the character image");
        Image image = new Image(resource.toExternalForm(), false);
        if (image.isError()) throw new IllegalStateException("Could not load the character image", image.getException());
        return image;
    }

    private VBox cover() {
        figure = new ImageView(loadCharacter());
        figure.setId("character");
        figure.setPreserveRatio(true);
        figure.setSmooth(true);
        StackPane character = new StackPane(figure);
        figure.fitWidthProperty().bind(javafx.beans.binding.Bindings.max(1, character.widthProperty().subtract(32)));
        figure.fitHeightProperty().bind(javafx.beans.binding.Bindings.max(1, character.heightProperty().subtract(32)));
        character.setId("space-character");
        character.setMinSize(0, 0);
        character.setPrefHeight(370);
        character.setMaxHeight(Double.MAX_VALUE);
        character.setStyle(styleCard(theme.background()));
        character.setAccessibleText("ChiquiChess character");
        Text caption = texto("Pick your team.\nPlay your classic.", 22, theme.texto(), true);
        caption.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.max(1, widthProperty().multiply(.4).subtract(64)));
        VBox cover = new VBox(14, title("CHIQUICHESS", 30), title("ARGENTINE CHESS", 12), character, caption);
        cover.setId("cover-menu");
        cover.setPadding(new Insets(28, 28, 24, 28));
        cover.setMinWidth(0);
        cover.setBackground(background(theme.card()));
        cover.setBorder(new Border(new BorderStroke(theme.border(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 3, 0, 0))));
        VBox.setVgrow(character, Priority.ALWAYS);
        return cover;
    }

    /** Fila de un jugador: su nombre con el club elegido y los escudos para elegirlo. */
    private HBox selectionTeam(String player, String id, ToggleGroup group, Text club) {
        FlowPane crests = new FlowPane(6, 6);
        for (Team team : Team.values()) {
            ToggleButton button = option("", id + "-" + team.name().toLowerCase(java.util.Locale.ROOT), team, group);
            var image = new ImageView(ClubPresentation.crestFor(team));
            image.setFitWidth(54);
            image.setFitHeight(54);
            image.setPreserveRatio(true);
            image.setSmooth(true);
            image.setMouseTransparent(true);
            button.setGraphic(image);
            button.setAccessibleText(team.displayName());
            button.setTooltip(new Tooltip(team.displayName()));
            button.setMinSize(78, 70);
            button.setPrefSize(78, 70);
            button.setMaxSize(78, 70);
            crests.getChildren().add(button);
        }
        club.setFont(PixelStyle.font(13, true));
        club.setFill(theme.accent());
        VBox displayName = new VBox(2, texto(player, 14, theme.texto(), true), club);
        displayName.setMinWidth(96);
        displayName.setPrefWidth(96);
        HBox row = new HBox(10, displayName, crests);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(crests, Priority.ALWAYS);
        return row;
    }

    private ToggleButton option(String texto, String id, Object value, ToggleGroup group) {
        // En un grupo siempre queda una opción elegida: tocar la elegida no la suelta. Las opciones
        // sueltas (sin grupo), como "Permitir deshacer", sí se activan y desactivan.
        ToggleButton button = new ToggleButton(texto) {
            @Override
            public void fire() {
                if (getToggleGroup() == null || !isSelected()) super.fire();
            }
        };
        button.setId(id);
        button.setUserData(value);
        button.setToggleGroup(group);
        button.setFont(PixelStyle.font(12, true));
        button.setCursor(Cursor.HAND);
        // Solo los clics de la persona (no las selecciones que hace el propio menú) avisan.
        button.setOnAction(event -> onChooseOption.run());
        button.selectedProperty().addListener((p, before, now) -> paintOption(button));
        button.focusedProperty().addListener((p, before, now) -> paintOption(button));
        button.hoverProperty().addListener((p, before, now) -> paintOption(button));
        button.pressedProperty().addListener((p, before, now) -> paintOption(button));
        paintOption(button);
        return button;
    }

    /**
     * Botón de pixel art: sombra dura abajo a la derecha, borde de 2 píxeles y cara plana. Al estar
     * elegido o apretado se "hunde": pierde la sombra y se corre hacia ella.
     */
    private void paintOption(ToggleButton button) {
        boolean sunken = button.isSelected() || button.isPressed();
        Color face = button.isSelected() ? theme.accent()
                : button.isHover() ? theme.card().interpolate(theme.border(), 0.6) : theme.card();
        Color border = button.isFocused() ? theme.texto() : button.isSelected() ? theme.accent().darker() : theme.border();
        button.setStyle(styleButton(face, border, sunken)
                + "-fx-text-fill: " + css(button.isSelected() ? theme.background() : theme.texto()) + ";");
    }

    private void paintPlay() {
        Color face = play.isDisabled() ? theme.border() : theme.accent();
        Color border = play.isFocused() ? theme.texto() : face.darker();
        play.setStyle(styleButton(face, border, false) + "-fx-text-fill: " + css(theme.background()) + ";"
                + "-fx-opacity: 1; -fx-padding: 10 18 14 14;");
    }

    private String styleButton(Color face, Color border, boolean sunken) {
        return PixelStyle.button(face, border, sunken);
    }

    /** Tarjeta de pixel art: sombra dura, borde grueso y cara oscura. */
    private String styleCard(Color face) {
        return PixelStyle.card(face, theme.border());
    }

    private VBox card(String title, Node content) {
        VBox card = new VBox(10, title(title, HEIGHT_TITLE), content);
        card.setPadding(new Insets(11, 19, 15, 15));
        card.setMinWidth(0);
        card.setStyle(styleCard(theme.card()));
        GridPane.setFillHeight(card, true);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    /** Título con las letras de puntos del marcador del estadio, alineado a la izquierda. */
    private Node title(String texto, double height) {
        LedText led = new LedText(theme.accent());
        led.setColorOff(theme.pointOff());
        led.setText(texto);
        double width = (texto.length() * 6 - 1) * height / 7;
        led.setMinSize(width, height);
        led.setPrefSize(width, height);
        led.setMaxSize(width, height);
        return led;
    }

    /** Opción que depende de otra, en un recuadro hundido dentro de su tarjeta. */
    private VBox dependent(String title, Node content) {
        VBox panel = new VBox(6, texto(title, 12, theme.accent(), true), content);
        panel.setPadding(new Insets(8, 10, 10, 10));
        panel.setBackground(background(theme.background()));
        return panel;
    }

    /**
     * Lugar fijo para una opción dependiente: siempre ocupa lo mismo, así la tarjeta no cambia de alto.
     * Cuando la opción no aplica, en su lugar se lee qué la habilita.
     */
    private StackPane slot(VBox panel, String track) {
        Label explanation = new Label(track);
        explanation.setFont(PixelStyle.font(12, false));
        explanation.setTextFill(theme.textSecondary());
        explanation.setWrapText(true);
        explanation.setPadding(new Insets(8, 10, 0, 10));
        panel.getProperties().put(TRACK, explanation);
        StackPane slot = new StackPane(panel, explanation);
        slot.setAlignment(Pos.TOP_LEFT);
        VBox.setMargin(slot, new Insets(10, 0, 0, 0));
        return slot;
    }

    private static void show(Node panel, boolean visible) {
        panel.setVisible(visible);
        ((Node) panel.getProperties().get(TRACK)).setVisible(!visible);
    }

    private void updateSelection() {
        blockTeam(team1, team2);
        blockTeam(team2, team1);
        clubPlayer1.setText(clubName(team1));
        clubPlayer2.setText(clubName(team2));
        // Deshacer solo existe en partidas sin tiempo: con un tiempo elegido se apaga y se deshabilita.
        boolean withoutTime = time.getSelectedToggle() != null
                && time.getSelectedToggle().getUserData() == Time.WITHOUT_TIME;
        if (!withoutTime) allowUndo.setSelected(false);
        allowUndo.setDisable(!withoutTime);
        show(panelUndo, withoutTime);
        // La dificultad solo existe contra la IA: con otro rival se borra y se deshabilita.
        boolean againstAi = rival.getSelectedToggle() != null && rival.getSelectedToggle().getUserData() == Rival.IA;
        if (!againstAi && difficulty.getSelectedToggle() != null) difficulty.selectToggle(null);
        for (Toggle option : difficulty.getToggles()) ((ToggleButton) option).setDisable(!againstAi);
        show(panelDifficulty, againstAi);
        boolean football = mode.getSelectedToggle() != null && mode.getSelectedToggle().getUserData() == Mode.FOOTBALL;
        moveLimit.setDisable(!football);
        show(panelLimit, football);
        var missing = new ArrayList<String>();
        if (mode.getSelectedToggle() == null) missing.add("mode");
        if (time.getSelectedToggle() == null) missing.add("time");
        if (rival.getSelectedToggle() == null) missing.add("rival");
        if (againstAi && difficulty.getSelectedToggle() == null) missing.add("difficulty");
        if (team1.getSelectedToggle() == null) missing.add("team of Player 1");
        if (team2.getSelectedToggle() == null) missing.add("team of Player 2");
        if (colors.getSelectedToggle() == null) missing.add("colors");
        play.setDisable(!missing.isEmpty());
        paintPlay();
        pending.setText(missing.isEmpty() ? "All set to play." : "Still to choose: " + String.join(", ", missing) + ".");
    }

    private static String clubName(ToggleGroup group) {
        return group.getSelectedToggle() == null ? "Not chosen" : ((Team) group.getSelectedToggle().getUserData()).displayName();
    }

    private void blockTeam(ToggleGroup own, ToggleGroup other) {
        Object chosen = other.getSelectedToggle() == null ? null : other.getSelectedToggle().getUserData();
        for (Toggle option : own.getToggles()) ((ToggleButton) option).setDisable(option.getUserData() == chosen);
    }

    private void emitConfiguration() {
        updateSelection();
        if (play.isDisabled()) return;
        GameConfiguration selection = new GameConfiguration((Mode) mode.getSelectedToggle().getUserData(),
                (Time) time.getSelectedToggle().getUserData(), (Rival) rival.getSelectedToggle().getUserData(),
                (Team) team1.getSelectedToggle().getUserData(), (Team) team2.getSelectedToggle().getUserData(),
                (Colors) colors.getSelectedToggle().getUserData(), allowUndo.isSelected(),
                limitChosen(), java.util.Optional.ofNullable(difficulty.getSelectedToggle())
                .map(chosen -> (Difficulty) chosen.getUserData()));
        start.accept(selection);
    }

    /** El límite elegido, tomando lo que se haya escrito a mano en el campo. */
    private int limitChosen() {
        confirmLimitWritten();
        return moveLimit.getValue();
    }

    /** Pasa al valor lo escrito en el campo (fuera de rango, se ajusta al extremo); si no es un número, lo descarta. */
    private void confirmLimitWritten() {
        var factory = moveLimit.getValueFactory();
        try {
            factory.setValue(factory.getConverter().fromString(moveLimit.getEditor().getText().trim()));
        } catch (RuntimeException notANumber) {
            // se queda el valor anterior
        }
        moveLimit.getEditor().setText(factory.getConverter().toString(factory.getValue()));
    }

    private FlowPane row(Node... buttons) {
        return new FlowPane(8, 8, buttons);
    }

    private Text texto(String value, double size, Color color, boolean bold) {
        Text texto = new Text(value);
        texto.setFont(PixelStyle.font(size, bold));
        texto.setFill(color);
        return texto;
    }

    private Background background(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    private static String css(Color color) {
        return String.format(java.util.Locale.ROOT, "#%02x%02x%02x", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
