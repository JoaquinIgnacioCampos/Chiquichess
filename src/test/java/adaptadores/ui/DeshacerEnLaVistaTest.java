package adaptadores.ui;

import Nucleo.juego.Partida;
import adaptadores.EnsambladorPartida;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Los botones "Deshacer" y "Rehacer" de la cabecera, conectados a una partida real. */
class DeshacerEnLaVistaTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    private VistaPartida vista(Partida partida) {
        TemaTablero tema = new TemaTablero(
                javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
                javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY,
                javafx.scene.paint.Color.YELLOW, javafx.scene.paint.Color.PURPLE,
                javafx.scene.paint.Color.BLUE);
        VistaPartida vista = new VistaPartida(new ControladorTablero(partida), tema,
                new RepresentacionPiezas(), TemaPanel.marcador());
        vista.configurarDeshacer();
        return vista;
    }

    private void pulsar(VistaPartida vista, String casilla) {
        var posicion = pos(casilla);
        StackPane nodo = (StackPane) vista.lookup("#casilla-" + posicion.columna() + "-" + posicion.fila());
        nodo.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    private Button boton(VistaPartida vista, String id) {
        return (Button) vista.lookup("#" + id);
    }

    @Test
    void sinLaOpcionNoHayBotones() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        assertNull(vista.lookup("#deshacer"));
        assertNull(vista.lookup("#rehacer"));
    }

    @Test
    void alEmpezarLosBotonesEstanDeshabilitados() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinRelojConDeshacer());
        assertTrue(boton(vista, "deshacer").isDisable());
        assertTrue(boton(vista, "rehacer").isDisable());
    }

    @Test
    void deshacerYRehacerUnaJugadaDesdeLosBotones() {
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer();
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        pulsar(vista, "e4");
        assertFalse(boton(vista, "deshacer").isDisable());

        boton(vista, "deshacer").fire();
        assertTrue(partida.historial().isEmpty());
        assertTrue(partida.tablero().piezaEn(pos("e2")).isPresent());
        assertTrue(boton(vista, "deshacer").isDisable());
        assertFalse(boton(vista, "rehacer").isDisable());

        boton(vista, "rehacer").fire();
        assertEquals(1, partida.historial().size());
        assertTrue(boton(vista, "rehacer").isDisable());
    }

    @Test
    void deshacerDescartaLaSeleccion() {
        Partida partida = new EnsambladorPartida().crearSinRelojConDeshacer();
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        pulsar(vista, "e4");
        pulsar(vista, "e7");

        boton(vista, "deshacer").fire();

        assertEquals(0, vista.lookupAll(".indicador-movimiento").size());
    }
}
