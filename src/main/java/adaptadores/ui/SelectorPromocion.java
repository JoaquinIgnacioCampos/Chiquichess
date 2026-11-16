package adaptadores.ui;

import Nucleo.piezas.Pieza;
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
final class SelectorPromocion extends StackPane {
    private final TemaTablero temaTablero;
    private final RepresentacionPiezas representacion;
    private final TemaPanel tema;
    private final Consumer<Character> alElegir;
    private final HBox opciones = new HBox(10);
    private List<Character> mostradas = List.of();

    SelectorPromocion(TemaTablero temaTablero, RepresentacionPiezas representacion, TemaPanel tema,
                      Consumer<Character> alElegir, Runnable alCancelar) {
        this.temaTablero = Objects.requireNonNull(temaTablero, "temaTablero");
        this.representacion = Objects.requireNonNull(representacion, "representacion");
        this.tema = Objects.requireNonNull(tema, "tema");
        this.alElegir = Objects.requireNonNull(alElegir, "alElegir");
        Objects.requireNonNull(alCancelar, "alCancelar");
        setId("selector-promocion");
        setAlignment(Pos.CENTER);
        setBackground(new Background(new BackgroundFill(
                tema.fondo().deriveColor(0, 1, 1, .8), CornerRadii.EMPTY, Insets.EMPTY)));

        Text pregunta = new Text("¿A qué pieza promueve el peón?");
        pregunta.setFont(Font.font("System", FontWeight.BOLD, 16));
        pregunta.setFill(tema.texto());
        opciones.setAlignment(Pos.CENTER);
        Button cancelar = BotonDeCabecera.crear("Cancelar", "cancelar-promocion", tema.borde(), tema.texto());
        cancelar.setOnAction(e -> alCancelar.run());

        VBox tarjeta = new VBox(12, pregunta, opciones, cancelar);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(16));
        tarjeta.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        tarjeta.setBackground(new Background(new BackgroundFill(tema.tarjeta(), new CornerRadii(6), Insets.EMPTY)));
        tarjeta.setBorder(new Border(new BorderStroke(tema.borde(), BorderStrokeStyle.SOLID,
                new CornerRadii(6), new BorderWidths(1))));
        getChildren().add(tarjeta);
        addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                alCancelar.run();
                e.consume();
            }
        });
        mostrar(List.of());
    }

    /** Muestra las piezas recibidas; con una lista vacía el selector se oculta. */
    void mostrar(List<Pieza> piezas) {
        boolean visible = !piezas.isEmpty();
        setVisible(visible);
        setManaged(visible);
        // Cada consulta crea piezas nuevas: se comparan por código para no rearmar los botones en cada redibujado.
        List<Character> codigos = piezas.stream().map(Pieza::codigo).toList();
        if (codigos.equals(mostradas)) {
            return;
        }
        mostradas = codigos;
        opciones.getChildren().setAll(piezas.stream().map(this::boton).toList());
        if (visible) {
            opciones.getChildren().getFirst().requestFocus();
        }
    }

    private Button boton(Pieza pieza) {
        Button boton = new Button();
        boton.setId("promover-" + pieza.codigo());
        boton.setGraphic(representacion.crear(pieza, temaTablero));
        boton.setAccessibleText(pieza.nombre());
        boton.setPrefSize(72, 72);
        boton.setCursor(Cursor.HAND);
        Runnable pintar = () -> boton.setBackground(new Background(new BackgroundFill(
                boton.isFocused() || boton.isHover() ? tema.acento() : temaTablero.casillaClara(),
                new CornerRadii(4), Insets.EMPTY)));
        boton.focusedProperty().addListener((p, antes, ahora) -> pintar.run());
        boton.hoverProperty().addListener((p, antes, ahora) -> pintar.run());
        pintar.run();
        char codigo = pieza.codigo();
        boton.setOnAction(e -> alElegir.accept(codigo));
        return boton;
    }
}
