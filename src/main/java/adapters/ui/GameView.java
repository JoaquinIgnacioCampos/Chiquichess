package adapters.ui;

import core.side.Side;
import core.board.Position;
import adapters.audio.VolumeControl;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Objects;

/** Vista preparada para dibujar y enviar órdenes a la partida recibida. */
public final class GameView extends BorderPane {
    private final BoardController controller;
    private final BoardView board;
    private final Text message = new Text();
    private final GameStatePanel panel;
    /** Marca y botones de la cabecera; si no entran en una fila, los botones bajan a la siguiente. */
    private final FlowPane title = new FlowPane(16, 10);
    private final StackPane areaBoard = new StackPane();
    private final BoardTheme theme;
    private final PieceRepresentation representation;
    /** Se crea recién la primera vez que un peón promueve: la mayoría de las partidas no lo necesita. */
    private PromotionSelector selectorPromotion;
    private final PanelTheme themePanel;
    private final InterfaceMessages messages;
    private GameNavigation navigation;
    private Runnable onSelectPiece = () -> { };
    private final java.util.Map<Side, ClubPresentation> clubs;
    /** Celebración de gol sobre el tablero (modo fútbol). */
    private final GoalBanner goalBanner;
    private javafx.scene.control.Button undo;
    private javafx.scene.control.Button redo;

    public GameView(BoardController controller, BoardTheme theme,
                        PieceRepresentation representation, PanelTheme themePanel) {
        this(controller, theme, representation, themePanel, java.util.Map.of());
    }

    public GameView(BoardController controller, BoardTheme theme,
                        PieceRepresentation representation, PanelTheme themePanel,
                        java.util.Map<Side, ClubPresentation> clubs) {
        this(controller, theme, representation, themePanel, clubs, new InterfaceMessages());
    }

    public GameView(BoardController controller, BoardTheme theme,
                        PieceRepresentation representation, PanelTheme themePanel,
                        java.util.Map<Side, ClubPresentation> clubs, InterfaceMessages messages) {
        this.controller = Objects.requireNonNull(controller, "controlador");
        this.messages = Objects.requireNonNull(messages, "mensajes");
        Objects.requireNonNull(theme, "tema");
        this.board = new BoardView(controller.board(), theme, representation, this::press);
        this.themePanel = Objects.requireNonNull(themePanel, "temaPanel");
        this.clubs = java.util.Map.copyOf(Objects.requireNonNull(clubs, "clubes"));
        this.goalBanner = new GoalBanner(themePanel);
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(theme.background(), CornerRadii.EMPTY, Insets.EMPTY)));
        BorderPane game = new BorderPane();
        game.setId("area-juego");
        game.setMinSize(0, 0);
        game.setPadding(new Insets(24));
        Text chiqui = new Text("CHIQUI");
        chiqui.setFill(themePanel.texto());
        chiqui.setFont(PixelStyle.font(22, true));
        Text chess = new Text("CHESS");
        chess.setFill(themePanel.accent());
        chess.setFont(PixelStyle.font(22, true));
        title.getChildren().add(new HBox(chiqui, chess));
        title.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(title, new Insets(0, 0, 16, 0));
        game.setTop(title);
        areaBoard.setMinSize(0, 0);
        this.theme = theme;
        this.representation = representation;
        areaBoard.getChildren().addAll(board, goalBanner);
        game.setCenter(areaBoard);
        setCenter(game);
        panel = new GameStatePanel(controller, themePanel, clubs, messages);
        panel.prefWidthProperty().bind(widthProperty().multiply(0.35));
        setRight(panel);
        message.setId("mensaje-movimiento");
        message.setFill(themePanel.texto());
        message.setFont(PixelStyle.font(16, false));
        message.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.min(520,
                areaBoard.widthProperty().multiply(0.8)));
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        // Cartel flotante sobre la parte de abajo del tablero: se ve sin correr ni achicar nada.
        // No toma clics, así el tablero que queda debajo se sigue pudiendo usar.
        StackPane sign = new StackPane(message);
        sign.setId("cartel-mensaje");
        sign.setMouseTransparent(true);
        sign.setPadding(new Insets(10, 18, 10, 18));
        sign.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        sign.setBackground(new Background(new BackgroundFill(
                themePanel.background().deriveColor(0, 1, 1, 0.9), CornerRadii.EMPTY, Insets.EMPTY)));
        sign.setBorder(new Border(new BorderStroke(themePanel.accent(), BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(2))));
        sign.visibleProperty().bind(message.textProperty().isNotEmpty());
        StackPane.setAlignment(sign, Pos.BOTTOM_CENTER);
        StackPane.setMargin(sign, new Insets(0, 0, 18, 0));
        areaBoard.getChildren().add(sign);
        // Después del panel: el panel anota el evento y recién entonces se redibuja todo.
        controller.listen(event -> {
            if (event instanceof core.game.GameEvent.MoveMade made
                    && made.has(core.game.GameEvent.Trait.GOAL)) {
                celebrateGoal(made.side());
            }
            redraw();
        });
        redraw();
    }

    private void press(Position position) {
        var before = controller.selected();
        message.setText(controller.press(position).map(messages::moveRejection).orElse(""));
        // Una pieza recién elegida (no deseleccionar, no mover): lo puede aprovechar el sonido.
        var now = controller.selected();
        if (now.isPresent() && !now.equals(before)) onSelectPiece.run();
        redraw();
    }

    /** El cartel de gol con el escudo, el color y el nombre del equipo de ese bando (o del bando, sin clubes). */
    private javafx.animation.Animation celebrateGoal(Side side) {
        ClubPresentation club = clubs.get(side);
        if (club == null) {
            return goalBanner.celebrate(side == Side.WHITE ? "Blancas" : "Negras", themePanel.accent(),
                    java.util.Optional.empty());
        }
        return goalBanner.celebrate(club.team().displayName(), club.theme().accent(), java.util.Optional.of(club.crest()));
    }

    /** Qué hacer cuando la persona elige una pieza para mover (por ejemplo, un clic de sonido). */
    public void onSelectPiece(Runnable action) {
        onSelectPiece = Objects.requireNonNull(action, "accion");
    }

    private void promote(char code) {
        message.setText(controller.promote(code).map(messages::moveRejection).orElse(""));
        redraw();
    }

    private void cancelPromotion() {
        controller.cancelPromotion();
        redraw();
    }

    private void showPromotion(java.util.List<core.pieces.Piece> options) {
        if (selectorPromotion == null) {
            if (options.isEmpty()) return;
            selectorPromotion = new PromotionSelector(theme, representation, themePanel,
                    this::promote, this::cancelPromotion);
            // Justo encima del tablero, debajo de la confirmación de "Volver al menú" si la hay.
            areaBoard.getChildren().add(1, selectorPromotion);
        }
        selectorPromotion.show(options);
    }

    /** Tablero y tarjetas vistos desde ese bando: sus piezas y su tarjeta quedan abajo. */
    public void lookFrom(core.side.Side side) {
        board.lookFrom(side);
        panel.lookFrom(side);
    }

    public void redraw() {
        var state = controller.state();
        boolean mate = state == core.rules.GameState.CHECK_MATE;
        board.markKing(mate || state == core.rules.GameState.CHECK
                ? java.util.Optional.of(controller.turn()) : java.util.Optional.empty(), mate);
        board.redraw(controller.selected(), controller.destinationsLegal());
        showPromotion(controller.promotionPending());
        panel.update();
        if (navigation != null) navigation.update();
        if (undo != null) {
            undo.setDisable(!controller.canUndo());
            redo.setDisable(!controller.canRedo());
        }
    }

    /**
     * Agrega a la cabecera los botones "Deshacer" y "Rehacer", solo si la partida lo permite. Los dos
     * jugadores pueden usarlos, por eso van en la cabecera y no en una tarjeta. Se llama antes de
     * configureNavigation, así quedan a la izquierda de "Volver al menú".
     */
    public void configureUndo() {
        if (undo != null) throw new IllegalStateException("Deshacer ya configurado");
        if (!controller.allowsUndo()) return;
        undo = HeaderButton.create("Deshacer", "deshacer", themePanel.card(), themePanel.texto(),
                PixelIcon.UNDO, false);
        redo = HeaderButton.create("Rehacer", "rehacer", themePanel.card(), themePanel.texto(),
                PixelIcon.UNDO, true);
        undo.setOnAction(e -> showResponseUndo(controller.undo()));
        redo.setOnAction(e -> showResponseUndo(controller.redo()));
        title.getChildren().addAll(undo, redo);
        redraw();
    }

    private void showResponseUndo(java.util.Optional<core.game.InvalidActionException.Reason> rejection) {
        message.setText(rejection
                .map(reason -> messages.actionRejection(InterfaceMessages.Action.UNDO, reason))
                .orElse(""));
        redraw();
    }

    /**
     * Agrega a la cabecera el botón de silencio y el volumen. El control se comparte entre partidas, así
     * que lo que se elige acá sigue valiendo en la revancha. Se llama antes de configureNavigation.
     */
    public void configureSound(VolumeControl control) {
        Objects.requireNonNull(control, "control");
        if (title.lookup("#silencio") != null) throw new IllegalStateException("Sonido ya configurado");
        title.getChildren().addAll(SoundControls.create(control, "silencio", "volumen",
                themePanel.card(), themePanel.texto()));
    }

    public void updateClocks() {
        panel.updateClocks();
    }

    public void configureNavigation(Runnable onMenu, Runnable onRematch) {
        if (navigation != null) throw new IllegalStateException("Navegación ya configurada");
        navigation = new GameNavigation(controller, themePanel, onMenu, onRematch);
        title.getChildren().add(navigation.returnValue());
        areaBoard.getChildren().add(navigation.confirmation());
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, navigation::cancelWithEscape);
        panel.configureRematch(navigation.rematch());
    }

    /** La tarjeta confirma la intención; el controlador envía la orden al núcleo. */
    public void configureResignation() {
        panel.configureResignation(color -> {
            controller.verifyTime();
            if (controller.state().isFinished()) {
                redraw();
                return;
            }
            message.setText(controller.resign(color)
                    .map(reason -> messages.actionRejection(InterfaceMessages.Action.RESIGN, reason))
                    .orElse(""));
            redraw();
        });
    }

    public void configureDraw() {
        panel.configureDraw(color -> showResponseDraw(controller.offerDraw(color)),
                color -> showResponseDraw(controller.acceptDraw(color)),
                color -> showResponseDraw(controller.rejectDraw(color)));
    }

    private void showResponseDraw(java.util.Optional<core.game.InvalidActionException.Reason> rejection) {
        message.setText(rejection
                .map(reason -> messages.actionRejection(InterfaceMessages.Action.DRAW, reason))
                .orElse(""));
        redraw();
    }
}
