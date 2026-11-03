package adaptadores.ui;

import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Posicion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.Objects;

/** Vista preparada para dibujar y enviar órdenes a la partida recibida. */
public final class VistaPartida extends BorderPane {
    private final ControladorTablero controlador;
    private final VistaTablero tablero;
    private final Text mensaje = new Text();
    private final PanelEstadoPartida panel;

    public VistaPartida(ControladorTablero controlador, TemaTablero tema,
                        RepresentacionPiezas representacion, TemaPanel temaPanel) {
        this.controlador = Objects.requireNonNull(controlador, "controlador");
        Objects.requireNonNull(tema, "tema");
        this.tablero = new VistaTablero(controlador.tablero(), tema, representacion, this::pulsar);
        Objects.requireNonNull(temaPanel, "temaPanel");
        setMinSize(0, 0);
        setBackground(new Background(new BackgroundFill(tema.fondo(), CornerRadii.EMPTY, Insets.EMPTY)));
        BorderPane juego = new BorderPane();
        juego.setId("area-juego");
        juego.setMinSize(0, 0);
        juego.setPadding(new Insets(24));
        Text chiqui = new Text("CHIQUI");
        chiqui.setFill(temaPanel.texto());
        chiqui.setFont(Font.font("System", FontWeight.BOLD, 22));
        Text chess = new Text("CHESS");
        chess.setFill(temaPanel.acento());
        chess.setFont(Font.font("System", FontWeight.BOLD, 22));
        HBox titulo = new HBox(chiqui, chess);
        titulo.setAlignment(Pos.CENTER_LEFT);
        BorderPane.setMargin(titulo, new Insets(0, 0, 16, 0));
        juego.setTop(titulo);
        juego.setCenter(tablero);
        setCenter(juego);
        panel = new PanelEstadoPartida(controlador, temaPanel);
        panel.prefWidthProperty().bind(widthProperty().multiply(0.35));
        setRight(panel);
        mensaje.setId("mensaje-movimiento");
        mensaje.setFill(temaPanel.texto());
        mensaje.setFont(Font.font(16));
        mensaje.wrappingWidthProperty().bind(juego.widthProperty().subtract(48));
        mensaje.managedProperty().bind(mensaje.textProperty().isNotEmpty());
        mensaje.visibleProperty().bind(mensaje.textProperty().isNotEmpty());
        BorderPane.setMargin(mensaje, new Insets(12, 0, 0, 0));
        juego.setBottom(mensaje);
        redibujar();
    }

    private void pulsar(Posicion posicion) {
        mensaje.setText(controlador.pulsar(posicion).map(this::mensajeRechazo).orElse(""));
        redibujar();
    }

    public void redibujar() {
        tablero.redibujar(controlador.seleccionada(), controlador.destinosLegales());
        panel.actualizar();
    }

    public void actualizarRelojes() {
        panel.actualizarRelojes();
    }

    private String mensajeRechazo(MovimientoInvalidoException.Motivo motivo) {
        return switch (motivo) {
            case SIN_PIEZA_EN_ORIGEN -> "Ya no hay una pieza en la casilla seleccionada.";
            case NO_ES_SU_TURNO -> "Es el turno del otro jugador.";
            case MOVIMIENTO_NO_PERMITIDO -> "El núcleo rechazó ese movimiento.";
            case DEJA_AL_REY_EN_JAQUE -> "Esa jugada deja a tu rey en jaque.";
            case PARTIDA_FINALIZADA -> "La partida ya terminó.";
        };
    }
}
