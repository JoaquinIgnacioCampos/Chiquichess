package adaptadores.ui;

import Nucleo.juego.Partida;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;

import java.util.Objects;

/** Vista preparada para dibujar y enviar órdenes a la partida recibida. */
public final class VistaPartida extends BorderPane {
    private final Partida partida;
    private final VistaTablero tablero;

    public VistaPartida(Partida partida, TemaTablero tema, RepresentacionPiezas representacion) {
        this.partida = Objects.requireNonNull(partida, "partida");
        Objects.requireNonNull(tema, "tema");
        this.tablero = new VistaTablero(this.partida.tablero(), tema, representacion);
        setPadding(new Insets(24));
        setBackground(new Background(new BackgroundFill(tema.fondo(), CornerRadii.EMPTY, Insets.EMPTY)));
        setCenter(tablero);
    }

    public void redibujar() {
        tablero.redibujar();
    }
}
