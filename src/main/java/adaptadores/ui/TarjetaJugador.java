package adaptadores.ui;

import Nucleo.juego.Jugador;
import Nucleo.piezas.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Identificación de un jugador, sin acciones ni estado del juego propio. */
final class TarjetaJugador extends VBox {
    private final TemaPanel tema;
    private final Text nombre = new Text();
    private final VistaReloj reloj;

    TarjetaJugador(Color color, TemaPanel tema) {
        this.tema = tema;
        setId(color == Color.BLANCO ? "jugador-blanco" : "jugador-negro");
        nombre.setId(color == Color.BLANCO ? "nombre-blanco" : "nombre-negro");
        nombre.setFill(tema.texto());
        setMinSize(0, 0);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setPadding(new Insets(20));
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        setBackground(new Background(new BackgroundFill(tema.tarjeta(), CornerRadii.EMPTY, Insets.EMPTY)));

        Text rol = new Text(color == Color.BLANCO ? "LOCAL" : "VISITANTE");
        rol.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        rol.setFill(tema.textoSecundario());
        reloj = new VistaReloj(color, tema);
        reloj.prefWidthProperty().bind(widthProperty().multiply(0.35));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(10, new Rectangle(4, 15, tema.acento()), rol, espacio, reloj);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Polygon escudo = new Polygon(32, 0, 62, 10, 60, 46, 51, 63, 32, 76, 13, 63, 4, 46, 2, 10);
        escudo.setFill(color == Color.BLANCO ? tema.insigniaBlanca() : tema.insigniaNegra());
        escudo.setStroke(tema.borde());
        escudo.setStrokeWidth(2);
        Text peon = new Text(color == Color.BLANCO ? "♙" : "♟");
        peon.setFont(Font.font("Segoe UI Symbol", 44));
        peon.setFill(color == Color.BLANCO ? tema.insigniaNegra() : tema.insigniaBlanca());
        StackPane insignia = new StackPane(escudo, peon);
        insignia.setMinSize(64, 80);
        insignia.setPrefSize(64, 80);
        insignia.setMaxSize(64, 80);

        Text bando = new Text(color == Color.BLANCO ? "BLANCAS" : "NEGRAS");
        bando.setFill(tema.textoSecundario());
        bando.setFont(Font.font("Consolas", FontWeight.BOLD, 13));
        VBox identidad = new VBox(8, nombre, bando);
        identidad.setMinWidth(0);
        identidad.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identidad, Priority.ALWAYS);
        HBox cuerpo = new HBox(16, insignia, identidad);
        cuerpo.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(cabecera, cuerpo);
        widthProperty().addListener((propiedad, antes, ahora) -> ajustarNombre());
        ajustarNombre();
    }

    private void ajustarNombre() {
        nombre.setFont(Font.font("System", FontWeight.BOLD, Math.max(17, Math.min(32, getWidth() * 0.075))));
        nombre.setWrappingWidth(Math.max(1, getWidth() - 120));
    }

    void actualizar(Jugador jugador, boolean activo) {
        nombre.setText(jugador.nombre());
        nombre.setAccessibleText(jugador.nombre());
        getStyleClass().remove("jugador-activo");
        if (activo) getStyleClass().add("jugador-activo");
        setBorder(new Border(new BorderStroke(activo ? tema.activo() : tema.borde(),
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(activo ? 2 : 1))));
        setEffect(activo ? new DropShadow(14, tema.activo().deriveColor(0, 1, 1, 0.2)) : null);
    }

    void actualizarReloj(java.util.Optional<java.time.Duration> restante, boolean activo) {
        reloj.actualizar(restante, activo);
    }
}
