package adapters.ui;

import core.side.Side;
import core.pieces.Pawn;
import core.pieces.Piece;
import core.pieces.King;
import core.board.IBoard;
import core.board.Position;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Proyección del tablero actual. Solo guarda referencias a sus dependencias y nodos gráficos.
 *
 * <p>Animaciones: cada vez que se redibuja compara dónde estaba cada pieza (el núcleo mueve el mismo
 * objeto) con dónde está ahora, y desliza las que cambiaron de casilla. Así se animan las jugadas, el
 * enroque, la promoción, deshacer y rehacer sin que el núcleo sepa nada de la vista. Las capturadas
 * estallan en una capa aparte cuando llega la pieza que las come. El estado de cada animación vive en
 * propiedades que sobreviven a los redibujos, así un clic a mitad de camino no la corta.
 */
public final class BoardView extends Region {
    private static final Duration SLIDE = Duration.millis(280);
    private static final Duration BURST = Duration.millis(320);
    private static final Interpolator SOFT = Interpolator.SPLINE(0.3, 0.1, 0.25, 1);
    private final IBoard board;
    private final BoardTheme theme;
    private final PieceRepresentation representation;
    private final GridPane grid = new GridPane();
    private final Consumer<Position> onPress;
    /** Bando que se ve del lado de abajo: blancas salvo que se elija mirar desde las negras. */
    private core.side.Side down = core.side.Side.WHITE;
    /** Capa sobre la grilla para las piezas capturadas que se desvanecen. */
    private final Pane layer = new Pane();
    /** Dónde estaba cada pieza en el último dibujo (por identidad: el núcleo mueve el mismo objeto). */
    private final Map<Piece, Position> previous = new IdentityHashMap<>();
    private boolean drawn;
    /** Piezas en movimiento o apareciendo, con su animación en curso. */
    private final Map<Piece, Path> paths = new IdentityHashMap<>();
    private Optional<Position> previousBall = Optional.empty();
    private Path pathBall;
    /** Latido continuo (0 a 1 y vuelta) del brillo del rey en jaque. */
    private final DoubleProperty heartbeat = new SimpleDoubleProperty();
    /** La pieza elegida se levanta un poco: de 0 (apoyada) a 1 (levantada). */
    private final DoubleProperty lifted = new SimpleDoubleProperty();
    private Optional<Position> chosenPrevious = Optional.empty();
    /** Bando cuyo rey está en jaque (o en jaque mate), si lo hay. */
    private Optional<Side> inCheck = Optional.empty();
    private boolean mate;
    /** El rey que acaba de quedar en jaque se sacude; el que recibe el mate cae de costado. */
    private final DoubleProperty shake = new SimpleDoubleProperty();
    private final DoubleProperty fall = new SimpleDoubleProperty();
    private Piece kingAnimated;
    private boolean kingAnimatedInMate;
    private Timeline animationKing;
    private final Timeline pulse = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(heartbeat, 0)),
            new KeyFrame(Duration.millis(650), new KeyValue(heartbeat, 1, Interpolator.EASE_BOTH)));

    /**
     * Desplazamiento de una pieza (en casillas, desde su casilla nueva) y su escala, que se animan hasta
     * 0 y 1. Los nodos que se crean en cada redibujo se atan a estas propiedades.
     */
    private record Path(DoubleProperty columns, DoubleProperty rows, DoubleProperty scale) {
        static Path de(double columns, double rows, double scale) {
            return new Path(new SimpleDoubleProperty(columns), new SimpleDoubleProperty(rows),
                    new SimpleDoubleProperty(scale));
        }
    }

    public BoardView(IBoard board, BoardTheme theme, PieceRepresentation representation) {
        this(board, theme, representation, position -> { });
    }

    public BoardView(IBoard board, BoardTheme theme, PieceRepresentation representation,
                        Consumer<Position> onPress) {
        this.board = Objects.requireNonNull(board, "tablero");
        this.theme = Objects.requireNonNull(theme, "tema");
        this.representation = Objects.requireNonNull(representation, "representacion");
        this.onPress = Objects.requireNonNull(onPress, "alPulsar");
        setMinSize(0, 0);
        layer.setMouseTransparent(true);
        layer.setManaged(false);
        getChildren().addAll(grid, layer);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        // El latido arranca al redibujar con el tablero a la vista y se corta al sacarlo de la escena.
        sceneProperty().addListener((p, before, scene) -> {
            if (scene == null) pulse.stop();
        });
        redraw();
    }

    /**
     * Solo se anima con el tablero a la vista, en una ventana abierta. Si no (por ejemplo, en los tests,
     * sin ventana), cada cambio salta directo a su estado final.
     */
    private boolean animatable() {
        return getScene() != null && getScene().getWindow() != null && getScene().getWindow().isShowing();
    }

    /** Reproduce la animación, o si no se puede animar, la lleva al final y avisa que terminó. */
    private void play(Timeline line) {
        if (animatable()) {
            line.play();
            return;
        }
        // Los valores del último cuadro son el estado final de la animación.
        line.getKeyFrames().stream().max(java.util.Comparator.comparing(KeyFrame::getTime)).ifPresent(last -> {
            for (KeyValue value : last.getValues()) {
                @SuppressWarnings("unchecked")
                var destination = (javafx.beans.value.WritableValue<Object>) value.getTarget();
                destination.setValue(value.getEndValue());
            }
        });
        if (line.getOnFinished() != null) line.getOnFinished().handle(null);
    }

    /**
     * Qué rey señalar: el del bando en jaque late en rojo; si es mate, además cae de costado. Se llama
     * antes de redibujar.
     */
    public void markKing(Optional<Side> side, boolean isMate) {
        inCheck = Objects.requireNonNull(side, "bando");
        mate = isMate && side.isPresent();
    }

    /** Gira el tablero para que ese bando quede abajo, como lo vería quien juega con él. */
    public void lookFrom(core.side.Side side) {
        down = Objects.requireNonNull(side, "bando");
        redraw();
    }

    /** Reconstruye la representación consultando el núcleo, sin almacenar piezas por separado. */
    public void redraw() {
        redraw(Optional.empty(), Set.of());
    }

    /** La selección puede estar vacía y los destinos ya vienen calculados por el núcleo. */
    public void redraw(Optional<Position> selected, Set<Position> destinations) {
        Objects.requireNonNull(selected, "seleccionada");
        Objects.requireNonNull(destinations, "destinos");
        // Color de la pieza elegida: un destino ocupado por una pieza del mismo color es un pase.
        Optional<core.side.Side> colorChosen = selected.flatMap(board::pieceIn).map(core.pieces.Piece::color);
        if (animatable() && pulse.getStatus() != Animation.Status.RUNNING) pulse.play();
        animateChanges();
        animateSelection(selected);
        animateKing();
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        for (int column = 0; column < board.width(); column++) {
            ColumnConstraints restriction = new ColumnConstraints();
            restriction.setPercentWidth(100.0 / board.width());
            grid.getColumnConstraints().add(restriction);
        }
        for (int row = 0; row < board.height(); row++) {
            RowConstraints restriction = new RowConstraints();
            restriction.setPercentHeight(100.0 / board.height());
            grid.getRowConstraints().add(restriction);
            for (int column = 0; column < board.width(); column++) {
                Position position = new Position(column, row);
                StackPane square = createSquare(position,
                        selected.filter(position::equals).isPresent(), destinations.contains(position), colorChosen);
                boolean fromWhite = down == core.side.Side.WHITE;
                grid.add(square, fromWhite ? column : board.width() - 1 - column,
                        fromWhite ? board.height() - 1 - row : row);
            }
        }
        requestLayout();
    }

    private StackPane createSquare(Position position, boolean selected, boolean destination,
                                   Optional<core.side.Side> colorChosen) {
        StackPane square = new StackPane();
        square.setMinSize(0, 0);
        square.setId("casilla-" + position.column() + "-" + position.row());
        // Las piezas de las filas de abajo (las más cercanas) tapan a las de arriba cuando sus sprites se pisan.
        square.setViewOrder(-inScreen(position)[1]);
        square.getStyleClass().add("casilla");
        boolean dark = (position.column() + position.row()) % 2 == 0;
        Color colorSquare = dark ? theme.darkSquare() : theme.lightSquare();
        if (selected) {
            square.getStyleClass().add("casilla-seleccionada");
            colorSquare = theme.selectedSquare();
        }
        square.setBackground(Background.fill(colorSquare));
        if (theme.texture() != null) {
            ImageView grass = new ImageView(theme.texture());
            grass.setManaged(false);
            grass.setMouseTransparent(true);
            grass.setOpacity(selected ? 0.15 : 0.28);
            grass.fitWidthProperty().bind(square.widthProperty());
            grass.fitHeightProperty().bind(square.heightProperty());
            square.getChildren().add(grass);
        }
        square.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                onPress.accept(position);
            }
        });
        square.setAccessibleText("Columna " + (position.column() + 1) + ", fila " + (position.row() + 1));
        board.netIn(position).ifPresent(defender -> addNet(square, position));
        board.pieceIn(position).ifPresent(piece -> {
            boolean kingMarked = piece instanceof King && inCheck.filter(piece.color()::equals).isPresent();
            if (kingMarked) addCheckGlow(square);
            Node figure = representation.create(piece, theme);
            DoubleExpression factor = new SimpleDoubleProperty(1);
            Path path = paths.get(piece);
            if (path != null) {
                factor = path.scale();
                bindOffset(figure, square, path);
            }
            if (selected) {
                // Por encima de las vecinas, para que la sombra de la pieza levantada no quede tapada.
                square.setViewOrder(-50);
                factor = factor.multiply(lifted.multiply(0.12).add(1));
                DropShadow shadow = new DropShadow(0, 0, 0, Color.web("#000000", 0.45));
                shadow.radiusProperty().bind(lifted.multiply(12));
                shadow.offsetYProperty().bind(lifted.multiply(7));
                figure.setEffect(shadow);
                figure.translateYProperty().bind(lifted.multiply(square.heightProperty()).multiply(-0.06));
            }
            if (kingMarked) {
                figure.rotateProperty().bind(mate ? fall.multiply(78) : shake.multiply(14));
            }
            figure.scaleXProperty().bind(square.widthProperty().divide(PieceRepresentation.SIDE_BASE).multiply(factor));
            figure.scaleYProperty().bind(square.heightProperty().divide(PieceRepresentation.SIDE_BASE).multiply(factor));
            square.getChildren().add(figure);
        });
        if (board.ball().filter(position::equals).isPresent()) {
            addBall(square, board.pieceIn(position).isPresent());
        }
        if (destination) {
            boolean pass = board.pieceIn(position)
                    .filter(piece -> colorChosen.filter(color -> color == piece.color()).isPresent())
                    .isPresent();
            if (pass) {
                addPassIndicator(square);
            } else {
                addIndicator(square, board.pieceIn(position).isPresent());
            }
        }
        return square;
    }

    /**
     * Arco del modo fútbol: una red (líneas cruzadas sobre un velo claro) y la línea de gol gruesa del
     * lado del borde del tablero. Va debajo de la pieza para no taparla.
     */
    private void addNet(StackPane square, Position position) {
        Pane net = new Pane();
        net.setId("arco-" + position.column() + "-" + position.row());
        net.getStyleClass().add("arco");
        net.setMouseTransparent(true);
        net.setMinSize(0, 0);
        net.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.web("#FFFFFF", 0.30), CornerRadii.EMPTY, Insets.EMPTY)));
        // La línea de gol va del lado del borde que se ve, según hacia dónde esté girado el tablero.
        int topRow = down == core.side.Side.WHITE ? board.height() - 1 : 0;
        boolean up = position.row() == topRow;
        double thickness = 5;
        net.setBorder(new Border(new BorderStroke(javafx.scene.paint.Color.WHITE, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(up ? thickness : 0, 0, up ? 0 : thickness, 0))));
        javafx.scene.paint.Color thread = javafx.scene.paint.Color.web("#FFFFFF", 0.55);
        for (int i = 1; i < 4; i++) {
            double fraction = i / 4.0;
            javafx.scene.shape.Line vertical = new javafx.scene.shape.Line();
            vertical.startXProperty().bind(net.widthProperty().multiply(fraction));
            vertical.endXProperty().bind(net.widthProperty().multiply(fraction));
            vertical.endYProperty().bind(net.heightProperty());
            javafx.scene.shape.Line horizontal = new javafx.scene.shape.Line();
            horizontal.startYProperty().bind(net.heightProperty().multiply(fraction));
            horizontal.endYProperty().bind(net.heightProperty().multiply(fraction));
            horizontal.endXProperty().bind(net.widthProperty());
            for (javafx.scene.shape.Line line : new javafx.scene.shape.Line[]{vertical, horizontal}) {
                line.setStroke(thread);
                line.setStrokeWidth(1);
                line.setManaged(false);
                net.getChildren().add(line);
            }
        }
        square.getChildren().add(net);
    }

    /**
     * Pelota del modo fútbol: suelta, va en el centro de la casilla; si una pieza la lleva, va más
     * chica en la esquina de abajo a la derecha, para que la pieza se siga viendo.
     */
    private void addBall(StackPane square, boolean withPiece) {
        Circle ball = new Circle();
        ball.setId("pelota");
        ball.getStyleClass().add("pelota");
        ball.setManaged(false);
        ball.setMouseTransparent(true);
        var side = Bindings.min(square.widthProperty(), square.heightProperty());
        ball.radiusProperty().bind(side.multiply(withPiece ? 0.13 : 0.22));
        double position = withPiece ? 0.80 : 0.5;
        ball.centerXProperty().bind(square.widthProperty().multiply(position));
        ball.centerYProperty().bind(square.heightProperty().multiply(position));
        ball.setFill(javafx.scene.paint.Color.WHITE);
        ball.setStroke(javafx.scene.paint.Color.web("#1b1b1b"));
        ball.strokeWidthProperty().bind(side.multiply(0.035));
        if (pathBall != null) bindOffset(ball, square, pathBall);
        square.getChildren().add(ball);
    }

    /** Brillo rojo debajo del rey en jaque: late mientras dure el jaque y queda fijo en el mate. */
    private void addCheckGlow(StackPane square) {
        Region glow = new Region();
        glow.getStyleClass().add("brillo-jaque");
        glow.setMouseTransparent(true);
        glow.setMinSize(0, 0);
        glow.setBackground(new Background(new BackgroundFill(new javafx.scene.paint.RadialGradient(0, 0, 0.5, 0.5,
                0.6, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#FF2A2A", 0.95)),
                new javafx.scene.paint.Stop(0.55, Color.web("#E01818", 0.55)),
                new javafx.scene.paint.Stop(1, Color.web("#B00000", 0))), CornerRadii.EMPTY, Insets.EMPTY)));
        if (mate) {
            glow.setOpacity(0.9);
        } else {
            glow.opacityProperty().bind(heartbeat.multiply(0.55).add(0.35));
        }
        square.getChildren().add(glow);
    }

    /** Ata la posición de un nodo a su recorrido: la casilla con el nodo en viaje va por encima del resto. */
    private void bindOffset(Node node, StackPane square, Path path) {
        node.translateXProperty().bind(path.columns().multiply(square.widthProperty()));
        node.translateYProperty().bind(path.rows().multiply(square.heightProperty()));
        square.setViewOrder(-100);
    }

    /** Columna y fila en pantalla de una casilla, según hacia dónde esté girado el tablero. */
    private double[] inScreen(Position position) {
        boolean fromWhite = down == core.side.Side.WHITE;
        return new double[]{fromWhite ? position.column() : board.width() - 1 - position.column(),
                fromWhite ? board.height() - 1 - position.row() : position.row()};
    }

    /**
     * Compara las piezas con las del dibujo anterior: las que cambiaron de casilla se deslizan, las que
     * desaparecieron estallan y las nuevas aparecen. Un peón que desaparece y una pieza nueva de su
     * color que aparece son una promoción: la pieza nueva sale de donde estaba el peón.
     */
    private void animateChanges() {
        Map<Piece, Position> current = new IdentityHashMap<>();
        board.pieces().forEach((position, piece) -> current.put(piece, position));
        Optional<Position> ball = board.ball();
        if (drawn) {
            List<Map.Entry<Piece, Position>> disappeared = new ArrayList<>();
            previous.forEach((piece, position) -> {
                if (!current.containsKey(piece)) disappeared.add(Map.entry(piece, position));
            });
            current.forEach((piece, until) -> {
                Position from = previous.get(piece);
                if (from == null) {
                    var pawn = disappeared.stream()
                            .filter(e -> e.getKey() instanceof Pawn && e.getKey().color() == piece.color())
                            .findFirst();
                    pawn.ifPresentOrElse(e -> {
                        disappeared.remove(e);
                        slide(piece, e.getValue(), until);
                    }, () -> appear(piece));
                } else if (!from.equals(until)) {
                    slide(piece, from, until);
                }
            });
            disappeared.forEach(e -> burst(e.getKey(), e.getValue()));
            if (previousBall.isPresent() && ball.isPresent() && !previousBall.equals(ball)) {
                Path path = pathFrom(previousBall.get(), ball.get());
                pathBall = path;
                animateUntilPlace(path, () -> {
                    if (pathBall == path) pathBall = null;
                });
            }
        }
        previous.clear();
        previous.putAll(current);
        previousBall = ball;
        drawn = true;
    }

    private Path pathFrom(Position from, Position until) {
        double[] origin = inScreen(from);
        double[] destination = inScreen(until);
        return Path.de(origin[0] - destination[0], origin[1] - destination[1], 1);
    }

    private void slide(Piece piece, Position from, Position until) {
        Path path = pathFrom(from, until);
        paths.put(piece, path);
        animateUntilPlace(path, () -> paths.remove(piece, path));
    }

    private void appear(Piece piece) {
        Path path = Path.de(0, 0, 0.2);
        paths.put(piece, path);
        animateUntilPlace(path, () -> paths.remove(piece, path));
    }

    private void animateUntilPlace(Path path, Runnable onEnd) {
        Timeline line = new Timeline(new KeyFrame(SLIDE,
                new KeyValue(path.columns(), 0, SOFT),
                new KeyValue(path.rows(), 0, SOFT),
                new KeyValue(path.scale(), 1, Interpolator.EASE_OUT)));
        line.setOnFinished(event -> onEnd.run());
        play(line);
    }

    /**
     * Pieza capturada: se queda quieta hasta que llega la que la come y entonces se agranda, gira y se
     * desvanece. Va en una capa aparte, así los redibujos no la borran.
     */
    private void burst(Piece piece, Position position) {
        double side = grid.getWidth() / board.width();
        if (!animatable()) return;
        if (side <= 0) return;
        double[] cell = inScreen(position);
        Node figure = representation.create(piece, theme);
        figure.setScaleX(side / PieceRepresentation.SIDE_BASE);
        figure.setScaleY(side / PieceRepresentation.SIDE_BASE);
        StackPane place = new StackPane(figure);
        place.resizeRelocate(grid.getLayoutX() + cell[0] * side, grid.getLayoutY() + cell[1] * side, side, side);
        layer.getChildren().add(place);
        Duration arrival = SLIDE.multiply(0.8);
        Timeline line = new Timeline(
                new KeyFrame(arrival,
                        new KeyValue(place.opacityProperty(), 1),
                        new KeyValue(place.scaleXProperty(), 1),
                        new KeyValue(place.scaleYProperty(), 1),
                        new KeyValue(place.rotateProperty(), 0)),
                new KeyFrame(arrival.add(BURST),
                        new KeyValue(place.opacityProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(place.scaleXProperty(), 1.45, Interpolator.EASE_OUT),
                        new KeyValue(place.scaleYProperty(), 1.45, Interpolator.EASE_OUT),
                        new KeyValue(place.rotateProperty(), piece.color() == Side.WHITE ? -25 : 25)));
        line.setOnFinished(event -> layer.getChildren().remove(place));
        line.play();
    }

    /** La pieza recién elegida se levanta; al soltarla vuelve a apoyarse. */
    private void animateSelection(Optional<Position> selected) {
        if (selected.equals(chosenPrevious)) return;
        chosenPrevious = selected;
        lifted.set(0);
        if (selected.isEmpty()) return;
        play(new Timeline(new KeyFrame(Duration.millis(140), new KeyValue(lifted, 1, Interpolator.EASE_OUT))));
    }

    /** Al empezar un jaque el rey se sacude una vez; en el mate cae de costado y queda así. */
    private void animateKing() {
        Piece king = inCheck.flatMap(side -> board.pieces().values().stream()
                .filter(piece -> piece instanceof King && piece.color() == side).findFirst()).orElse(null);
        if (king == kingAnimated && mate == kingAnimatedInMate) return;
        kingAnimated = king;
        kingAnimatedInMate = mate;
        if (animationKing != null) animationKing.stop();
        shake.set(0);
        fall.set(0);
        if (king == null) return;
        animationKing = new Timeline();
        if (mate) {
            animationKing.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(250), new KeyValue(fall, 0)),
                    new KeyFrame(Duration.millis(900), new KeyValue(fall, 1, Interpolator.SPLINE(0.5, 0, 0.9, 0.6))),
                    new KeyFrame(Duration.millis(1030), new KeyValue(fall, 0.88, Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.millis(1160), new KeyValue(fall, 1, Interpolator.EASE_IN)));
        } else {
            double[] values = {1, -1, 0.7, -0.6, 0.3, 0};
            for (int i = 0; i < values.length; i++) {
                animationKing.getKeyFrames().add(new KeyFrame(Duration.millis(260 + 70 * i),
                        new KeyValue(shake, values[i], Interpolator.EASE_BOTH)));
            }
        }
        play(animationKing);
    }

    /** Destino de un pase (modo fútbol): un anillo blanco punteado, distinto del de captura. */
    private void addPassIndicator(StackPane square) {
        Circle indicator = new Circle();
        indicator.setManaged(false);
        indicator.setMouseTransparent(true);
        indicator.getStyleClass().add("indicador-pase");
        indicator.centerXProperty().bind(square.widthProperty().divide(2));
        indicator.centerYProperty().bind(square.heightProperty().divide(2));
        var side = Bindings.min(square.widthProperty(), square.heightProperty());
        indicator.radiusProperty().bind(side.multiply(0.42));
        indicator.setFill(null);
        indicator.setStroke(javafx.scene.paint.Color.WHITE);
        indicator.strokeWidthProperty().bind(side.multiply(0.06));
        indicator.getStrokeDashArray().addAll(10.0, 9.0);
        square.getChildren().add(indicator);
    }

    private void addIndicator(StackPane square, boolean capture) {
        Circle indicator = new Circle();
        indicator.setManaged(false);
        indicator.setMouseTransparent(true);
        indicator.centerXProperty().bind(square.widthProperty().divide(2));
        indicator.centerYProperty().bind(square.heightProperty().divide(2));
        var side = Bindings.min(square.widthProperty(), square.heightProperty());
        indicator.radiusProperty().bind(side.multiply(capture ? 0.43 : 0.16));
        if (capture) {
            indicator.getStyleClass().add("indicador-captura");
            indicator.setFill(null);
            indicator.setStroke(theme.destinationCapture());
            indicator.strokeWidthProperty().bind(side.multiply(0.08));
        } else {
            indicator.getStyleClass().add("indicador-movimiento");
            indicator.setFill(theme.destinationFree());
        }
        square.getChildren().add(indicator);
    }

    @Override
    protected void layoutChildren() {
        double side = Math.min(getWidth() / board.width(), getHeight() / board.height());
        double width = side * board.width();
        double height = side * board.height();
        grid.resizeRelocate((getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
        layer.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    @Override
    protected double computePrefWidth(double height) {
        return board.width() * PieceRepresentation.SIDE_BASE;
    }

    @Override
    protected double computePrefHeight(double width) {
        return board.height() * PieceRepresentation.SIDE_BASE;
    }
}
