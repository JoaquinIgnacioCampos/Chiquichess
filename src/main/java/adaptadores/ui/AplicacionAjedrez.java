package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.Partida;
import adaptadores.EnsambladorPartida;
import adaptadores.configuracion.ConfiguracionPartida;
import adaptadores.configuracion.PartidaConfigurada;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Arranque gráfico: ensambla el juego e inyecta la partida en la vista. */
public class AplicacionAjedrez extends Application {
    private ActualizadorRelojJavaFx actualizadorReloj;
    private PartidaConfigurada partidaActual;

    @Override
    public void start(Stage ventana) {
        VistaMenuPartida menu = new VistaMenuPartida(TemaPanel.marcador(), configuracion -> iniciarPartida(ventana, configuracion));
        ventana.setTitle("ChiquiChess");
        ventana.setScene(new Scene(menu, 1280, 820));
        ventana.setMinWidth(900);
        ventana.setMinHeight(620);
        ventana.setOnHidden(evento -> cerrarPartidaActual());
        ventana.show();
    }

    private void iniciarPartida(Stage ventana, ConfiguracionPartida configuracion) {
        cerrarPartidaActual();
        var preparada = new EnsambladorPartida().prepararConfigurada(configuracion);
        partidaActual = preparada;
        Partida partida = preparada.partida();
        TemaTablero tema = new TemaTablero(
                Color.web("#F0D9B5"), Color.web("#B58863"),
                Color.web("#FFFBEF"), Color.web("#202733"),
                Color.web("#202733"), Color.web("#1B212B"),
                Color.web("#F7F769"), Color.web("#202733", 0.18), Color.web("#202733", 0.23));
        ControladorTablero controlador = new ControladorTablero(partida);
        TemaPanel temaPanel = TemaPanel.marcador();
        var clubes = java.util.Map.of(
                Bando.BLANCO, PresentacionClub.de(preparada.equipoBlancas(), temaPanel),
                Bando.NEGRO, PresentacionClub.de(preparada.equipoNegras(), temaPanel));
        VistaPartida vista = new VistaPartida(controlador, tema, new RepresentacionPiezas(), temaPanel, clubes);
        vista.configurarRendicion();
        vista.configurarTablas();
        vista.configurarNavegacion(() -> {
            if (partidaActual == preparada) volverAlMenu(ventana);
        }, () -> iniciarRevancha(ventana, configuracion, preparada));
        actualizadorReloj = new ActualizadorRelojJavaFx(controlador, vista::actualizarRelojes, vista::redibujar);
        ventana.getScene().setRoot(vista);
        actualizadorReloj.iniciar();
    }

    private void volverAlMenu(Stage ventana) {
        cerrarPartidaActual();
        ventana.getScene().setRoot(new VistaMenuPartida(TemaPanel.marcador(),
                configuracion -> iniciarPartida(ventana, configuracion)));
    }

    private void iniciarRevancha(Stage ventana, ConfiguracionPartida anterior, PartidaConfigurada preparada) {
        if (partidaActual != preparada || !preparada.partida().estado().estaFinalizada()) return;
        var colores = preparada.equipoBlancas() == anterior.equipoJugador1()
                ? ConfiguracionPartida.Colores.JUGADOR_2_BLANCAS : ConfiguracionPartida.Colores.JUGADOR_1_BLANCAS;
        iniciarPartida(ventana, new ConfiguracionPartida(anterior.modo(), anterior.tiempo(), anterior.rival(),
                anterior.equipoJugador1(), anterior.equipoJugador2(), colores));
    }

    private void cerrarPartidaActual() {
        if (actualizadorReloj != null) {
            actualizadorReloj.close();
            actualizadorReloj = null;
        }
        if (partidaActual != null) {
            partidaActual.close();
            partidaActual = null;
        }
    }

    @Override public void stop() {
        cerrarPartidaActual();
    }
}
