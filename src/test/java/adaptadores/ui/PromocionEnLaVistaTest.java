package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tiempo.SinReloj;
import dobles.TableroFake;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/** Elegir la pieza al promover: el controlador espera la elección y la vista muestra el selector. */
class PromocionEnLaVistaTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    /** Peón blanco en a7 a punto de promover. */
    private final Partida partida = new Partida(TableroFake.desde(
            "....r...",
            "P.......",
            "........",
            "........",
            "........",
            "........",
            "........",
            "....R..."), new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
            ReglasDePartida.clasicas(), new SinReloj());
    private final ControladorTablero controlador = new ControladorTablero(partida);

    private static List<Character> codigos(List<Pieza> piezas) {
        return piezas.stream().map(Pieza::codigo).toList();
    }

    // ---------------- Controlador ----------------

    @Test
    void llevarElPeonALaUltimaFilaEsperaLaEleccion() {
        // Act
        controlador.pulsar(pos("a7"));
        controlador.pulsar(pos("a8"));

        // Assert: todavía no movió
        assertEquals(List.of('D', 'T', 'A', 'C'), codigos(controlador.promocionPendiente()));
        assertTrue(partida.historial().isEmpty());
        assertTrue(partida.tablero().piezaEn(pos("a7")).orElseThrow().esPeon());
    }

    @Test
    void elegirLaPiezaCompletaLaJugada() {
        // Arrange
        controlador.pulsar(pos("a7"));
        controlador.pulsar(pos("a8"));

        // Act
        assertTrue(controlador.promover('C').isEmpty());

        // Assert
        assertEquals('C', partida.tablero().piezaEn(pos("a8")).orElseThrow().codigo());
        assertEquals(Bando.NEGRO, partida.turno());
        assertTrue(controlador.promocionPendiente().isEmpty());
    }

    @Test
    void cancelarDejaElPeonDondeEstaba() {
        // Arrange
        controlador.pulsar(pos("a7"));
        controlador.pulsar(pos("a8"));

        // Act
        controlador.cancelarPromocion();

        // Assert
        assertTrue(controlador.promocionPendiente().isEmpty());
        assertTrue(partida.tablero().piezaEn(pos("a7")).orElseThrow().esPeon());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void pulsarOtraCasillaDescartaLaPromocion() {
        // Arrange
        controlador.pulsar(pos("a7"));
        controlador.pulsar(pos("a8"));

        // Act
        controlador.pulsar(pos("e1"));

        // Assert
        assertTrue(controlador.promocionPendiente().isEmpty());
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void promoverSinPromocionPendienteEsUnError() {
        assertThrows(IllegalStateException.class, () -> controlador.promover('D'));
    }

    @Test
    void unaJugadaComunNoPreguntaNada() {
        // Act
        controlador.pulsar(pos("e1"));
        controlador.pulsar(pos("d1"));

        // Assert
        assertTrue(controlador.promocionPendiente().isEmpty());
        assertEquals(1, partida.historial().size());
    }

    // ---------------- Vista ----------------

    private VistaPartida vista() {
        TemaTablero tema = new TemaTablero(Color.BEIGE, Color.BROWN, Color.WHITE, Color.BLACK,
                Color.BLACK, Color.GRAY, Color.YELLOW, Color.PURPLE, Color.BLUE);
        return new VistaPartida(controlador, tema, new RepresentacionPiezas(), TemaPanel.marcador());
    }

    private void pulsar(VistaPartida vista, String casilla) {
        var posicion = pos(casilla);
        StackPane nodo = (StackPane) vista.lookup("#casilla-" + posicion.columna() + "-" + posicion.fila());
        nodo.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null));
    }

    @Test
    void elSelectorApareceSoloAlPromover() {
        // Arrange
        VistaPartida vista = vista();

        // Assert: al empezar no existe (se crea recién al promover)
        assertNull(vista.lookup("#selector-promocion"));

        // Act
        pulsar(vista, "a7");
        pulsar(vista, "a8");

        // Assert
        assertTrue(vista.lookup("#selector-promocion").isVisible());
        for (char codigo : List.of('D', 'T', 'A', 'C')) {
            assertNotNull(vista.lookup("#promover-" + codigo), "Falta el botón de " + codigo);
        }
    }

    @Test
    void elegirEnElSelectorPromueveYLoCierra() {
        // Arrange
        VistaPartida vista = vista();
        pulsar(vista, "a7");
        pulsar(vista, "a8");

        // Act
        ((Button) vista.lookup("#promover-T")).fire();

        // Assert
        assertEquals('T', partida.tablero().piezaEn(pos("a8")).orElseThrow().codigo());
        assertFalse(vista.lookup("#selector-promocion").isVisible());
    }

    @Test
    void cancelarEnElSelectorLoCierraSinMover() {
        // Arrange
        VistaPartida vista = vista();
        pulsar(vista, "a7");
        pulsar(vista, "a8");

        // Act
        ((Button) vista.lookup("#cancelar-promocion")).fire();

        // Assert
        assertFalse(vista.lookup("#selector-promocion").isVisible());
        assertTrue(partida.tablero().piezaEn(pos("a7")).orElseThrow().esPeon());
    }
}
