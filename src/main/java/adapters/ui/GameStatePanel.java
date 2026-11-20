package adapters.ui;

import core.side.Side;
import core.game.GameEvent;
import core.rules.GameState;
import core.rules.Scoreboard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Objects;
import java.util.Optional;

/** Marcador: presenta el estado del núcleo y deriva las solicitudes de los jugadores. */
public final class GameStatePanel extends GridPane {
    private final BoardController controller;
    private final PanelTheme theme;
    private final PlayerCard black;
    private final PlayerCard white;
    private final Text title = new Text();
    private final LedText heading;
    private final LedText principal;
    private final Text state = new Text();
    private final Text reason = new Text();
    private final Circle point = new Circle(5);
    /** Modo fútbol: los goles de cada bando y cuántas jugadas van del límite. Vacíos en el clásico. */
    private final Text goals = new Text();
    private final Text moves = new Text();
    /** true si la última jugada (hecha o rehecha) fue gol: lo dice el evento, no se deduce. */
    private boolean goalJust = false;
    private final VBox information;
    private final InterfaceMessages messages;

    public GameStatePanel(BoardController controller, PanelTheme theme) {
        this(controller, theme, java.util.Map.of());
    }

    public GameStatePanel(BoardController controller, PanelTheme theme,
                               java.util.Map<Side, ClubPresentation> clubs) {
        this(controller, theme, clubs, new InterfaceMessages());
    }

    public GameStatePanel(BoardController controller, PanelTheme theme,
                               java.util.Map<Side, ClubPresentation> clubs, InterfaceMessages messages) {
        this.controller = Objects.requireNonNull(controller, "controlador");
        this.messages = Objects.requireNonNull(messages, "mensajes");
        this.theme = Objects.requireNonNull(theme, "tema");
        Objects.requireNonNull(clubs, "clubes");
        setId("panel-estado");
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(theme.background(), CornerRadii.EMPTY, Insets.EMPTY)));
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(100);
        getColumnConstraints().add(column);
        for (double percentage : new double[]{27, 46, 27}) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(percentage);
            row.setMinHeight(0);
            getRowConstraints().add(row);
        }
        black = new PlayerCard(Side.BLACK, theme, clubs.get(Side.BLACK));
        white = new PlayerCard(Side.WHITE, theme, clubs.get(Side.WHITE));
        heading = new LedText(theme.accent());
        heading.setId("marcador-encabezado");
        principal = new LedText(theme.texto());
        principal.setId("marcador-principal");
        title.setId("titulo-estado");
        title.setFont(PixelStyle.font(13, true));
        title.setFill(theme.textSecondary());
        state.setId("estado-partida");
        state.setFont(PixelStyle.font(20, true));
        HBox currentState = new HBox(10, point, state);
        currentState.setAlignment(Pos.CENTER);
        reason.setId("motivo-final");
        reason.setFill(theme.textSecondary());
        reason.setFont(PixelStyle.font(14, false));
        reason.setTextAlignment(TextAlignment.CENTER);
        reason.managedProperty().bind(reason.textProperty().isNotEmpty());
        reason.visibleProperty().bind(reason.textProperty().isNotEmpty());
        goals.setId("goles-partida");
        goals.setFont(PixelStyle.font(18, true));
        goals.setFill(theme.texto());
        moves.setId("jugadas-futbol");
        moves.setFont(PixelStyle.font(12, false));
        moves.setFill(theme.textSecondary());
        for (Text texto : new Text[]{goals, moves}) {
            texto.managedProperty().bind(texto.textProperty().isNotEmpty());
            texto.visibleProperty().bind(texto.textProperty().isNotEmpty());
        }
        Text mode = new Text(controller.scoreboard().isPresent()
                ? "Partida local · Fútbol: posesión y gol" : "Partida local");
        mode.setFill(theme.textSecondary());
        mode.setFont(PixelStyle.font(12, false));
        Region separador = new Region();
        separador.setMinHeight(1);
        separador.setPrefHeight(1);
        separador.setMaxHeight(1);
        separador.setBackground(new Background(new BackgroundFill(theme.border(), CornerRadii.EMPTY, Insets.EMPTY)));
        information = new VBox(12, title, heading, principal, currentState, goals, moves, separador,
                reason, mode);
        information.setPadding(new Insets(20));
        information.setAlignment(Pos.CENTER);
        information.setMinSize(0, 0);
        StackPane scoreboard = new StackPane();
        scoreboard.setMinSize(0, 0);
        Canvas frame = new Canvas();
        frame.setManaged(false);
        frame.setMouseTransparent(true);
        frame.widthProperty().bind(scoreboard.widthProperty());
        frame.heightProperty().bind(scoreboard.heightProperty());
        frame.widthProperty().addListener((p, a, n) -> drawFrame(frame));
        frame.heightProperty().addListener((p, a, n) -> drawFrame(frame));
        reason.wrappingWidthProperty().bind(scoreboard.widthProperty().subtract(40));
        scoreboard.heightProperty().addListener((p, a, n) -> information.setSpacing(
                Math.max(7, Math.min(16, n.doubleValue() * 0.035))));
        scoreboard.getChildren().addAll(frame, information);
        add(black, 0, 0);
        add(scoreboard, 0, 1);
        add(white, 0, 2);
        controller.listen(this::note);
        update();
    }

    /**
     * Recuerda si la última jugada fue gol, para anunciarlo hasta la jugada siguiente. Deshacer lo borra
     * y rehacer un gol lo vuelve a anunciar. Lo llama la partida antes de que la vista se redibuje.
     */
    void note(GameEvent event) {
        goalJust = switch (event) {
            case GameEvent.MoveMade made -> made.has(GameEvent.Trait.GOAL);
            case GameEvent.MoveRedone redone -> redone.traits().contains(GameEvent.Trait.GOAL);
            case GameEvent.MoveUndone undone -> false;
            case GameEvent.FinishedGame fin -> false;
        };
    }

    private void drawFrame(Canvas frame) {
        var drawing = frame.getGraphicsContext2D();
        drawing.clearRect(0, 0, frame.getWidth(), frame.getHeight());
        drawing.setFill(theme.pointOff());
        for (double x = 3; x < frame.getWidth(); x += 8) {
            for (double y = 3; y < frame.getHeight(); y += 8) drawing.fillOval(x, y, 2, 2);
        }
    }

    public void update() {
        boolean finished = controller.state().isFinished();
        updateGoals();
        // Contra la IA, su tarjeta no tiene botones activos: rendirse u ofrecer tablas es cosa de personas.
        boolean playsBlack = !finished && controller.turn() == Side.BLACK && controller.isHuman(Side.BLACK);
        boolean playsWhite = !finished && controller.turn() == Side.WHITE && controller.isHuman(Side.WHITE);
        black.enableResignation(playsBlack);
        white.enableResignation(playsWhite);
        var offer = controller.offerDrawPending();
        black.updateDraw(playsBlack && offer.isEmpty(),
                offer.filter(color -> color == Side.BLACK).isPresent(),
                offer.filter(color -> color == Side.WHITE).isPresent());
        white.updateDraw(playsWhite && offer.isEmpty(),
                offer.filter(color -> color == Side.WHITE).isPresent(),
                offer.filter(color -> color == Side.BLACK).isPresent());
        black.update(controller.player(Side.BLACK), !finished && controller.turn() == Side.BLACK);
        white.update(controller.player(Side.WHITE), !finished && controller.turn() == Side.WHITE);
        if (controller.result().isPresent()) {
            var result = controller.result().orElseThrow();
            title.setText("RESULTADO FINAL");
            heading.setText(result.winner().isPresent() ? "GANAN LAS" : "PARTIDA");
            principal.setText(result.winner().map(this::side).orElse("TABLAS"));
            state.setText(result.winner().isPresent() ? "VICTORIA" : "EMPATE");
            state.setFill(result.winner().isPresent() ? theme.activo() : theme.accent());
            reason.setText(messages.finalReason(result.reason()));
        } else {
            boolean check = controller.state() == GameState.CHECK;
            title.setText("ESTADO DE LA PARTIDA");
            heading.setText("TURNO DE");
            principal.setText(side(controller.turn()));
            boolean thinking = controller.isAiTurn();
            state.setText(goalJust ? "¡GOL!" : check ? "¡JAQUE!" : thinking ? "IA PENSANDO…" : "EN JUEGO");
            state.setFill(goalJust ? theme.accent() : check ? theme.alerta() : thinking ? theme.textSecondary() : theme.activo());
            reason.setText("");
        }
        point.setFill(state.getFill());
        updateClocks();
    }

    /** Muestra el marcador del modo fútbol; en el clásico deja los textos vacíos. */
    private void updateGoals() {
        Optional<Scoreboard> scoreboard = controller.scoreboard();
        if (scoreboard.isEmpty()) {
            goals.setText("");
            moves.setText("");
            return;
        }
        Scoreboard actual = scoreboard.get();
        goals.setText("BLANCAS " + actual.white() + " - " + actual.black() + " NEGRAS");
        moves.setText("Jugada " + controller.movesMade()
                + controller.moveLimit().map(limit -> " de " + limit).orElse(""));
    }

    /** Refresco ligero que conserva tarjetas, tablero, selección y sus nodos. */
    public void updateClocks() {
        boolean inProgress = !controller.state().isFinished();
        black.updateClock(controller.timeRemaining(Side.BLACK), inProgress && controller.turn() == Side.BLACK);
        white.updateClock(controller.timeRemaining(Side.WHITE), inProgress && controller.turn() == Side.WHITE);
    }

    public void configureResignation(java.util.function.Consumer<Side> request) {
        black.configureResignation(() -> request.accept(Side.BLACK));
        white.configureResignation(() -> request.accept(Side.WHITE));
        update();
    }

    public void configureDraw(java.util.function.Consumer<Side> offer,
                                 java.util.function.Consumer<Side> accept,
                                 java.util.function.Consumer<Side> reject) {
        black.configureDraw(() -> offer.accept(Side.BLACK), () -> accept.accept(Side.BLACK),
                () -> reject.accept(Side.BLACK));
        white.configureDraw(() -> offer.accept(Side.WHITE), () -> accept.accept(Side.WHITE),
                () -> reject.accept(Side.WHITE));
        update();
    }

    void configureRematch(javafx.scene.Node button) {
        information.getChildren().add(button);
    }

    private String side(Side color) {
        return color == Side.WHITE ? "BLANCAS" : "NEGRAS";
    }
    /** Pone abajo la tarjeta de ese bando, del mismo lado en que se ven sus piezas en el tablero. */
    void lookFrom(Side side) {
        boolean fromBlack = Objects.requireNonNull(side, "bando") == Side.BLACK;
        GridPane.setRowIndex(black, fromBlack ? 2 : 0);
        GridPane.setRowIndex(white, fromBlack ? 0 : 2);
    }
}
