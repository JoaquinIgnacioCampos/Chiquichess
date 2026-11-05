package adaptadores.ui;

import Nucleo.juego.Partida;
import adaptadores.EnsambladorPartida;
import dobles.FuenteDeTiempoFalsa;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static dobles.Casillas.*;
import static org.junit.jupiter.api.Assertions.*;

import Nucleo.bando.Bando;
class InteraccionVistaTest {
    private final TemaTablero tema = new TemaTablero(
            javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
            javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
            javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY,
            javafx.scene.paint.Color.YELLOW, javafx.scene.paint.Color.PURPLE,
            javafx.scene.paint.Color.BLUE);

    private VistaPartida vista(Partida partida) {
        return new VistaPartida(new ControladorTablero(partida), tema,
                new RepresentacionPiezas(), TemaPanel.marcador());
    }

    private StackPane casilla(VistaPartida vista, String nombre) {
        var posicion = pos(nombre);
        return (StackPane) vista.lookup("#casilla-" + posicion.columna() + "-" + posicion.fila());
    }

    private void pulsar(VistaPartida vista, String nombre) {
        clic(casilla(vista, nombre), MouseButton.PRIMARY);
    }

    private void clic(Node nodo, MouseButton boton) {
        nodo.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, boton, 1,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    @Test
    void clicSeleccionaYMarcaSoloLosDestinosQueDaElNucleo() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        assertEquals(2, vista.lookupAll(".indicador-movimiento").size());
        assertNotNull(casilla(vista, "e3").lookup(".indicador-movimiento"));
        assertNotNull(casilla(vista, "e4").lookup(".indicador-movimiento"));
        assertEquals(javafx.scene.paint.Color.YELLOW,
                casilla(vista, "e2").getBackground().getFills().getFirst().getFill());
        Circle punto = (Circle) casilla(vista, "e4").lookup(".indicador-movimiento");
        assertEquals(javafx.scene.paint.Color.PURPLE, punto.getFill());
        assertTrue(punto.isMouseTransparent());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void elegirOtraPiezaReemplazaTodosLosIndicadoresAnteriores() {
        VistaPartida vista = vista(new EnsambladorPartida().crearSinReloj());
        pulsar(vista, "e2");
        pulsar(vista, "g1");
        assertEquals(2, vista.lookupAll(".indicador-movimiento").size());
        assertNull(casilla(vista, "e4").lookup(".indicador-movimiento"));
        assertNotNull(casilla(vista, "f3").lookup(".indicador-movimiento"));
        assertNotNull(casilla(vista, "h3").lookup(".indicador-movimiento"));
    }

    @Test
    void casillaVaciaNoValidaCancelaTodosLosResaltados() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        pulsar(vista, "e6");
        assertTrue(vista.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(vista.lookupAll(".casilla-seleccionada").isEmpty());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void enemigaNoCapturableTambienCancelaLaSeleccion() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        pulsar(vista, "a7");
        assertTrue(vista.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(vista.lookupAll(".casilla-seleccionada").isEmpty());
        assertEquals(32, partida.tablero().piezas().size());
    }

    @Test
    void anilloSuperpuestoMarcaLaPiezaCapturableSinOcultarla() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("d7", "d5"));
        VistaPartida vista = vista(partida);
        pulsar(vista, "e4");
        vista.resize(1000, 720);
        vista.layout();
        StackPane rival = casilla(vista, "d5");
        Circle anillo = (Circle) rival.lookup(".indicador-captura");
        assertNotNull(anillo);
        assertNull(anillo.getFill());
        assertEquals(javafx.scene.paint.Color.BLUE, anillo.getStroke());
        assertTrue(anillo.getRadius() > 0);
        assertTrue(anillo.getStrokeWidth() > 0);
        assertTrue(anillo.isMouseTransparent());
        assertInstanceOf(Text.class, rival.getChildren().getFirst());
        assertSame(anillo, rival.getChildren().getLast());
        assertNull(rival.lookup(".indicador-movimiento"));
        assertNotNull(casilla(vista, "e5").lookup(".indicador-movimiento"));
    }

    @Test
    void clicEnLaEnemigaMarcadaEjecutaLaCapturaYRedibujaDesdeElNucleo() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("d7", "d5"));
        VistaPartida vista = vista(partida);
        pulsar(vista, "e4");
        pulsar(vista, "d5");
        assertEquals(Bando.BLANCO, partida.tablero().piezaEn(pos("d5")).orElseThrow().color());
        assertEquals(31, partida.tablero().piezas().size());
        assertEquals("♙", ((Text) casilla(vista, "d5").getChildren().getFirst()).getText());
        assertTrue(casilla(vista, "e4").getChildren().isEmpty());
        assertTrue(vista.lookupAll(".indicador-captura").isEmpty());
        assertTrue(vista.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(vista.lookupAll(".casilla-seleccionada").isEmpty());
        assertEquals(mov("e4", "d5"), partida.historial().getLast());
    }

    @Test
    void movimientoVacioPasaElTurnoYSoloSeleccionaAhoraPiezasNegras() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        pulsar(vista, "e4");
        assertEquals(Bando.NEGRO, partida.turno());
        pulsar(vista, "d2");
        assertTrue(vista.lookupAll(".indicador-movimiento").isEmpty());
        pulsar(vista, "e7");
        assertNotNull(casilla(vista, "e5").lookup(".indicador-movimiento"));
        assertNotNull(casilla(vista, "e6").lookup(".indicador-movimiento"));
    }

    @Test
    void muestraElRechazoDelNucleoSiElTiempoVenceEntreClics() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = new EnsambladorPartida().crearConReloj(Duration.ofSeconds(1), fuente);
        VistaPartida vista = vista(partida);
        pulsar(vista, "e2");
        fuente.avanzar(Duration.ofSeconds(1));
        pulsar(vista, "e4");
        Text mensaje = (Text) vista.lookup("#mensaje-movimiento");
        assertFalse(mensaje.getText().isBlank());
        assertTrue(partida.estado().estaFinalizada());
        assertTrue(partida.historial().isEmpty());
        assertTrue(vista.lookupAll(".indicador-movimiento").isEmpty());
        assertFalse(casilla(vista, "e2").getChildren().isEmpty());
    }

    @Test
    void clicSecundarioNoEnviaOrdenesNiSelecciona() {
        Partida partida = new EnsambladorPartida().crearSinReloj();
        VistaPartida vista = vista(partida);
        clic(casilla(vista, "e2"), MouseButton.SECONDARY);
        assertTrue(vista.lookupAll(".indicador-movimiento").isEmpty());
        assertTrue(vista.lookupAll(".casilla-seleccionada").isEmpty());
        assertTrue(partida.historial().isEmpty());
    }
}
