package adaptadores;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.juego.PartidaEnJuego;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tiempo.SinReloj;
import dobles.EstrategiaFalsa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

/** La IA como jugador de la partida: juega, responde tablas y deshace de a dos. */
class RivalArtificialTest {

    private final PartidaEnJuego partida = new Partida(new DisposicionEstandar().crearTablero(),
            new Jugador("Ana", Bando.BLANCO), new Jugador("IA", Bando.NEGRO),
            ReglasDePartida.clasicas().conDeshacer(), new SinReloj());

    private RivalArtificial rival(boolean aceptaTablas) {
        return new RivalArtificial(partida, Bando.NEGRO, new EstrategiaFalsa(aceptaTablas));
    }

    @Test
    void juegaSoloCuandoLeToca() {
        // Arrange
        RivalArtificial ia = rival(false);

        // Act + Assert
        assertFalse(ia.leToca());
        assertThrows(IllegalStateException.class, ia::jugar);
        partida.mover(mov("e2", "e4"));
        assertTrue(ia.leToca());
        ia.jugar();
        assertEquals(Bando.BLANCO, partida.turno());
        assertEquals(2, partida.historial().size());
    }

    @Test
    void conRelojPiensaUnaFraccionDeLoQueLeQueda() {
        // Arrange
        EstrategiaFalsa estrategia = new EstrategiaFalsa(false);
        RivalArtificial ia = new RivalArtificial(partida, Bando.NEGRO, estrategia);
        partida.mover(mov("e2", "e4"));

        // Act
        ia.pensar(partida.situacion(), Optional.of(Duration.ofSeconds(60)));

        // Assert
        assertEquals(Duration.ofSeconds(60).dividedBy(Nucleo.ia.RitmoHumano.FRACCION_DEL_TIEMPO_RESTANTE),
                estrategia.tiemposRecibidos.getFirst());
    }

    @Test
    void deshacerVuelveHastaQueLeTocaALaPersona() {
        // Arrange
        RivalArtificial ia = rival(false);
        partida.mover(mov("e2", "e4"));
        ia.jugar();

        // Act
        ia.deshacer();

        // Assert: se deshicieron la jugada de la IA y la de la persona
        assertTrue(partida.historial().isEmpty());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void siLaIaTodaviaNoRespondioDeshaceSoloLaJugadaDeLaPersona() {
        // Arrange
        RivalArtificial ia = rival(false);
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("e7", "e5"));
        partida.mover(mov("g1", "f3"));

        // Act
        ia.deshacer();

        // Assert
        assertEquals(2, partida.historial().size());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void rehacerVuelveAPonerLaJugadaDeLaPersonaYLaRespuesta() {
        // Arrange
        RivalArtificial ia = rival(false);
        partida.mover(mov("e2", "e4"));
        ia.jugar();
        ia.deshacer();

        // Act
        ia.rehacer();

        // Assert
        assertEquals(2, partida.historial().size());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void respondeLasTablasSegunSuEstrategia() {
        // Arrange
        PartidaEnJuego otra = new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas(), new SinReloj());
        RivalArtificial acepta = new RivalArtificial(otra, Bando.NEGRO, new EstrategiaFalsa(true));
        RivalArtificial rechaza = rival(false);

        // Act
        otra.ofrecerTablas(Bando.BLANCO);
        partida.ofrecerTablas(Bando.BLANCO);

        // Assert
        assertTrue(acepta.responderTablas());
        assertTrue(otra.estado().esTablas());
        assertFalse(rechaza.responderTablas());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertFalse(partida.estado().estaFinalizada());
    }

    @Test
    void sinOfertaDelRivalNoHaceNada() {
        assertFalse(rival(true).responderTablas());
        assertFalse(partida.estado().estaFinalizada());
    }
}
