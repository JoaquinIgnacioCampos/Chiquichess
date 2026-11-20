package adapters.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/** Confirmación integrada reutilizable: emite intenciones, sin reglas ni ventanas. */
final class JavaFxActionConfirmation extends VBox {
    private final Button cancel;

    JavaFxActionConfirmation(PanelTheme theme, String asks, String textAccept, String textCancel,
                             String suffix, Color accent, Runnable confirm, Runnable close) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(10));
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(theme.card(), new CornerRadii(6), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(theme.border(), BorderStrokeStyle.SOLID,
                new CornerRadii(6), new BorderWidths(1))));
        Text texto = new Text(asks);
        texto.setFont(PixelStyle.font(16, true));
        texto.setFill(theme.texto());
        texto.setTextAlignment(TextAlignment.CENTER);
        texto.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.max(1, widthProperty().subtract(24)));
        cancel = button(textCancel, theme.border(), theme.texto());
        cancel.setId("cancel-" + suffix);
        Button accept = button(textAccept, accent, theme.background());
        accept.setId("confirm-" + suffix);
        cancel.setOnAction(e -> close.run());
        accept.setOnAction(e -> confirm.run());
        HBox buttons = new HBox(10, cancel, accept);
        buttons.setAlignment(Pos.CENTER);
        HBox.setHgrow(cancel, Priority.ALWAYS);
        HBox.setHgrow(accept, Priority.ALWAYS);
        getChildren().addAll(texto, buttons);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                close.run();
                e.consume();
            }
        });
    }

    private Button button(String texto, Color background, Color ink) {
        Button button = new Button(texto);
        button.setFont(PixelStyle.font(14, true));
        button.setMinWidth(0);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setCursor(javafx.scene.Cursor.HAND);
        PixelStyle.paint(button, background, ink);
        return button;
    }

    void focusCancel() {
        cancel.requestFocus();
    }
}
