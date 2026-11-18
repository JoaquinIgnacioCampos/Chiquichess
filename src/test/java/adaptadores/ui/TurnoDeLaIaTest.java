package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Jugador;
import Nucleo.juego.Partida;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tiempo.SinReloj;
import adaptadores.RivalArtificial;
import dobles.EstrategiaFalsa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

/**
 * El turno de la IA en la ventana, sin hilos de verdad: pensar queda en una cola que el test ejecuta
 * cuando quiere, y "volver al hilo de la interfaz" es ejecutar en el momento.
 */
class TurnoDeLaIaTest {

    private final Deque<Runnable> pendientes = new ArrayDeque<>();
    private final Executor enCola = pendientes::add;

    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(), new Jugador("Ana", Bando.BLANCO),
                new Jugador("IA", Bando.NEGRO), ReglasDePartida.clasicas().conDeshacer(), new SinReloj());
    }

    private TurnoDeLaIa turno(Partida partida, Bando bandoDeLaIa) {
        RivalArtificial rival = new RivalArtificial(partida, bandoDeLaIa, new EstrategiaFalsa(false));
        return new TurnoDeLaIa(partida, rival, enCola, Runnable::run, Duration.ZERO);
    }

    private void terminarDePensar() {
        while (!pendientes.isEmpty()) pendientes.poll().run();
    }

    @Test
    void siLaIaJuegaConBlancasEmpiezaApenasArranca() {
        // Arrange
        Partida partida = partida();
        TurnoDeLaIa turno = turno(partida, Bando.BLANCO);

        // Act
        turno.iniciar();
        assertTrue(turno.pensando());
        terminarDePensar();

        // Assert
        assertEquals(1, partida.historial().size());
        assertEquals(Bando.NEGRO, partida.turno());
        assertFalse(turno.pensando());
    }

    @Test
    void respondeDespuesDeCadaJugadaDeLaPersona() {
        // Arrange
        Partida partida = partida();
        turno(partida, Bando.NEGRO).iniciar();

        // Act
        partida.mover(mov("e2", "e4"));
        terminarDePensar();
        partida.mover(mov("d2", "d4"));
        terminarDePensar();

        // Assert
        assertEquals(4, partida.historial().size());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void siLaPartidaCambiaMientrasPiensaLoPensadoSeDescarta() {
        // Arrange: la IA empieza a pensar su respuesta a e4
        Partida partida = partida();
        turno(partida, Bando.NEGRO).iniciar();
        partida.mover(mov("e2", "e4"));

        // Act: la persona se arrepiente antes de que la IA termine
        partida.deshacer();
        terminarDePensar();

        // Assert: la IA no jugó sobre una posición que ya no existe
        assertTrue(partida.historial().isEmpty());
        assertEquals(Bando.BLANCO, partida.turno());
    }

    @Test
    void despuesDeCerrarNoJuegaMas() {
        // Arrange
        Partida partida = partida();
        TurnoDeLaIa turno = turno(partida, Bando.NEGRO);
        turno.iniciar();
        partida.mover(mov("e2", "e4"));

        // Act: se volvió al menú mientras pensaba
        turno.close();
        terminarDePensar();

        // Assert
        assertEquals(1, partida.historial().size());
    }
}
