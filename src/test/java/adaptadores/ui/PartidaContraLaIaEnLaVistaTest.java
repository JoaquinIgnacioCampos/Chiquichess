package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.AccionInvalidaException;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.juego.PartidaEnJuego;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tiempo.SinReloj;
import adaptadores.RivalArtificial;
import dobles.EstrategiaFalsa;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static dobles.Casillas.mov;
import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Contra la IA, la interfaz solo deja actuar a la persona. La IA de estos tests no juega sola (no hay
 * un TurnoDeLaIa): así se puede mirar qué pasa mientras le toca.
 */
class PartidaContraLaIaEnLaVistaTest {

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException yaIniciado) {
            // otro test ya lo inició
        }
    }

    private final PartidaEnJuego partida = new Partida(new DisposicionEstandar().crearTablero(),
            new Jugador("Ana", Bando.BLANCO), new Jugador("IA (medio)", Bando.NEGRO),
            ReglasDePartida.clasicas().conDeshacer(), new SinReloj());
    private final RivalArtificial rival = new RivalArtificial(partida, Bando.NEGRO, new EstrategiaFalsa(false));
    private final ControladorTablero controlador = new ControladorTablero(partida, Optional.of(rival));

    @Test
    void mientrasLeTocaALaIaElTableroNoResponde() {
        // Arrange
        partida.mover(mov("e2", "e4"));

        // Act
        var rechazo = controlador.pulsar(pos("e7"));

        // Assert
        assertEquals(Optional.of(MovimientoInvalidoException.Motivo.NO_ES_SU_TURNO), rechazo);
        assertTrue(controlador.seleccionada().isEmpty());
    }

    @Test
    void nadieSeRindeNiOfreceTablasEnNombreDeLaIa() {
        // Arrange
        partida.mover(mov("e2", "e4"));

        // Act + Assert
        assertEquals(Optional.of(AccionInvalidaException.Motivo.NO_ES_SU_TURNO), controlador.rendirse(Bando.NEGRO));
        assertEquals(Optional.of(AccionInvalidaException.Motivo.NO_ES_SU_TURNO), controlador.ofrecerTablas(Bando.NEGRO));
        assertFalse(partida.estado().estaFinalizada());
    }

    @Test
    void laIaRespondeEnseguidaUnaOfertaDeTablas() {
        // Act: la IA de prueba rechaza
        controlador.ofrecerTablas(Bando.BLANCO);

        // Assert
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertFalse(partida.estado().estaFinalizada());
    }

    @Test
    void deshacerVuelveAlTurnoDeLaPersona() {
        // Arrange
        partida.mover(mov("e2", "e4"));
        rival.jugar();

        // Act
        controlador.deshacer();

        // Assert
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void elPanelMuestraQueLaIaPiensaYSuTarjetaNoTieneBotonesActivos() {
        // Arrange
        VistaPartida vista = new VistaPartida(controlador, new TemaTablero(
                javafx.scene.paint.Color.BEIGE, javafx.scene.paint.Color.BROWN,
                javafx.scene.paint.Color.WHITE, javafx.scene.paint.Color.BLACK,
                javafx.scene.paint.Color.BLACK, javafx.scene.paint.Color.GRAY),
                new RepresentacionUnicode(), TemaPanel.marcador());
        vista.configurarRendicion();
        vista.configurarTablas();

        // Act
        partida.mover(mov("e2", "e4"));

        // Assert
        assertEquals("IA PENSANDO…", ((Text) vista.lookup("#estado-partida")).getText());
        assertTrue(((Button) vista.lookup("#rendirse-negro")).isDisable());
        assertTrue(((Button) vista.lookup("#ofrecer-tablas-negro")).isDisable());
    }
}
