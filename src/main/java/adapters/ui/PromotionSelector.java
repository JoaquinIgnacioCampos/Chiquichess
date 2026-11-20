package adapters.ui;

import core.pieces.Piece;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Selector integrado sobre el tablero para elegir a qué pieza promueve el peón. Solo muestra las
 * opciones que le da el controlador y emite la elección; no sabe qué piezas existen ni valida nada.
 */
final class PromotionSelector extends StackPane {
    private final BoardTheme themeBoard;
    private final PieceRepresentation representation;
    private final PanelTheme theme;
    private final Consumer<Character> onChoose;
    private final HBox options = new HBox(10);
    private List<Character> shown = List.of();

    PromotionSelector(BoardTheme themeBoard, PieceRepresentation representation, PanelTheme theme,
                      Consumer<Character> onChoose, Runnable onCancel) {
        this.themeBoard = Objects.requireNonNull(themeBoard, "themeBoard");
        this.representation = Objects.requireNonNull(representation, "representation");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.onChoose = Objects.requireNonNull(onChoose, "onChoose");
        Objects.requireNonNull(onCancel, "onCancel");
        setId("selector-promotion");
        setAlignment(Pos.CENTER);
        setBackground(new Background(new BackgroundFill(
                theme.background().deriveColor(0, 1, 1, .8), CornerRadii.EMPTY, Insets.EMPTY)));

        Text asks = new Text("Which piece does the pawn promote to?");
        asks.setFont(PixelStyle.font(16, true));
        asks.setFill(theme.texto());
        options.setAlignment(Pos.CENTER);
        Button cancel = HeaderButton.create("Cancel", "cancel-promotion", theme.border(), theme.texto());
        cancel.setOnAction(e -> onCancel.run());

        VBox card = new VBox(12, asks, options, cancel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setBackground(new Background(new BackgroundFill(theme.card(), new CornerRadii(6), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(theme.border(), BorderStrokeStyle.SOLID,
                new CornerRadii(6), new BorderWidths(1))));
        getChildren().add(card);
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onCancel.run();
                e.consume();
            }
        });
        show(List.of());
    }

    /** Muestra las piezas recibidas; con una lista vacía el selector se oculta. */
    void show(List<Piece> pieces) {
        boolean visible = !pieces.isEmpty();
        setVisible(visible);
        setManaged(visible);
        // Cada consulta crea piezas nuevas: se comparan por código para no rearmar los botones en cada redibujado.
        List<Character> codes = pieces.stream().map(Piece::code).toList();
        if (codes.equals(shown)) {
            return;
        }
        shown = codes;
        options.getChildren().setAll(pieces.stream().map(this::button).toList());
        if (visible) {
            options.getChildren().getFirst().requestFocus();
        }
    }

    private Button button(Piece piece) {
        Button button = new Button();
        button.setId("promote-" + piece.code());
        button.setGraphic(representation.create(piece, themeBoard));
        button.setAccessibleText(piece.displayName());
        button.setPrefSize(72, 72);
        button.setPadding(new Insets(2));
        button.setCursor(Cursor.HAND);
        Runnable paint = () -> button.setBackground(new Background(new BackgroundFill(
                button.isFocused() || button.isHover() ? theme.accent() : themeBoard.lightSquare(),
                new CornerRadii(4), Insets.EMPTY)));
        button.focusedProperty().addListener((p, before, now) -> paint.run());
        button.hoverProperty().addListener((p, before, now) -> paint.run());
        paint.run();
        char code = piece.code();
        button.setOnAction(e -> onChoose.accept(code));
        return button;
    }
}
