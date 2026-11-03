package adaptadores.ui;

import Nucleo.juego.Partida;
import adaptadores.EnsambladorPartida;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.Duration;

/** Arranque gráfico: ensambla el juego e inyecta la partida en la vista. */
public class AplicacionAjedrez extends Application {
    private static final Duration TIEMPO_POR_JUGADOR = Duration.ofMinutes(5);
    private ActualizadorRelojJavaFx actualizadorReloj;

    @Override
    public void start(Stage ventana) {
        Partida partida = new EnsambladorPartida().crearConReloj(TIEMPO_POR_JUGADOR);
        TemaTablero tema = new TemaTablero(
                Color.web("#F0D9B5"), Color.web("#B58863"),
                Color.web("#FFFBEF"), Color.web("#202733"),
                Color.web("#202733"), Color.web("#1B212B"),
                Color.web("#F7F769"), Color.web("#202733", 0.18), Color.web("#202733", 0.23));
        ControladorTablero controlador = new ControladorTablero(partida);
        VistaPartida vista = new VistaPartida(controlador, tema, new RepresentacionPiezas(), TemaPanel.marcador());
        actualizadorReloj = new ActualizadorRelojJavaFx(controlador, vista::actualizarRelojes, vista::redibujar);
        ventana.setTitle("ChiquiChess");
        ventana.setScene(new Scene(vista, 1280, 820));
        ventana.setMinWidth(900);
        ventana.setMinHeight(620);
        ventana.setOnShown(evento -> actualizadorReloj.iniciar());
        ventana.setOnHidden(evento -> actualizadorReloj.detener());
        ventana.show();
    }

    @Override public void stop() {
        if (actualizadorReloj != null) actualizadorReloj.detener();
    }
}
