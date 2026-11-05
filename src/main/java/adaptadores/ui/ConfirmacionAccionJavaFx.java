package adaptadores.ui;

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
final class ConfirmacionAccionJavaFx extends VBox {
    private final Button cancelar;

    ConfirmacionAccionJavaFx(TemaPanel tema, String pregunta, String textoAceptar, String textoCancelar,
                             String sufijo, Color acento, Runnable confirmar, Runnable cerrar) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(10));
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(tema.tarjeta(), new CornerRadii(6), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.SOLID,
                new CornerRadii(6), new BorderWidths(1))));
        Text texto = new Text(pregunta);
        texto.setFont(Font.font("System", FontWeight.BOLD, 16));
        texto.setFill(tema.texto());
        texto.setTextAlignment(TextAlignment.CENTER);
        texto.wrappingWidthProperty().bind(javafx.beans.binding.Bindings.max(1, widthProperty().subtract(24)));
        cancelar = boton(textoCancelar, tema.borde(), tema.texto());
        cancelar.setId("cancelar-" + sufijo);
        Button aceptar = boton(textoAceptar, acento, tema.fondo());
        aceptar.setId("confirmar-" + sufijo);
        cancelar.setOnAction(e -> cerrar.run());
        aceptar.setOnAction(e -> confirmar.run());
        HBox botones = new HBox(10, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER);
        HBox.setHgrow(cancelar, Priority.ALWAYS);
        HBox.setHgrow(aceptar, Priority.ALWAYS);
        getChildren().addAll(texto, botones);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cerrar.run();
                e.consume();
            }
        });
    }

    private Button boton(String texto, Color fondo, Color tinta) {
        Button boton = new Button(texto);
        boton.setFont(Font.font("System", FontWeight.BOLD, 14));
        boton.setTextFill(tinta);
        boton.setPadding(new Insets(7, 10, 7, 10));
        boton.setMinWidth(0);
        boton.setMaxWidth(Double.MAX_VALUE);
        boton.setBackground(new Background(new BackgroundFill(fondo, new CornerRadii(4), Insets.EMPTY)));
        boton.setCursor(javafx.scene.Cursor.HAND);
        boton.focusedProperty().addListener((p, antes, foco) -> boton.setBorder(foco
                ? new Border(new BorderStroke(tinta, BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1)))
                : Border.EMPTY));
        return boton;
    }

    void enfocarCancelar() {
        cancelar.requestFocus();
    }
}
