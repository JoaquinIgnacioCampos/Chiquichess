package Nucleo.juego;

import Nucleo.reglas.*;
import Nucleo.tiempo.RelojAjedrez;
import dobles.FuenteDeTiempoFalsa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

import Nucleo.bando.Bando;
import Nucleo.reglas.MotivoFinalizacion;
class HistorialTest {
    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                new DetectorDeJaque(), List.of(new JaqueMate(), new Ahogado()));
    }

    @Test
    void historialEmpiezaVacioYConservaOrdenIncluyendoCapturas() {
        Partida partida = partida();
        assertTrue(partida.historial().isEmpty());
        Movimiento primero = mov("e2", "e4");
        Movimiento segundo = mov("d7", "d5");
        Movimiento captura = mov("e4", "d5");
        partida.mover(primero);
        partida.mover(segundo);
        assertTrue(partida.mover(captura).piezaCapturada().isPresent());
        assertEquals(List.of(primero, segundo, captura), partida.historial());
    }

    @Test
    void intentosRechazadosNoSeRegistran() {
        Partida partida = partida();
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e2", "e5")));
        partida.mover(mov("e2", "e4"));
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("d2", "d4")));
        partida.rendirse(Bando.NEGRO);
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e7", "e5")));
        assertEquals(List.of(mov("e2", "e4")), partida.historial());
    }

    @Test
    void consultasDevuelvenCopiasInmutablesIndependientes() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        List<Movimiento> copia = partida.historial();
        assertThrows(UnsupportedOperationException.class, () -> copia.add(mov("e7", "e5")));
        assertThrows(UnsupportedOperationException.class, copia::clear);
        partida.mover(mov("e7", "e5"));
        assertEquals(List.of(mov("e2", "e4")), copia);
        assertEquals(List.of(mov("e2", "e4"), mov("e7", "e5")), partida.historial());
    }

    @Test
    void jugadaQueDaMateTambienSeRegistra() {
        Partida partida = partida();
        List<Movimiento> movimientos = List.of(mov("f2", "f3"), mov("e7", "e5"),
                mov("g2", "g4"), mov("d8", "h4"));
        movimientos.forEach(partida::mover);
        assertEquals(EstadoPartida.JAQUE_MATE, partida.estado());
        assertEquals(movimientos, partida.historial());
    }

    @Test
    void ofertasYAcuerdoNoSeConfundenConMovimientos() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        partida.rechazarTablas(Bando.NEGRO);
        partida.ofrecerTablas(Bando.BLANCO);
        partida.aceptarTablas(Bando.NEGRO);
        assertTrue(partida.historial().isEmpty());
    }

    @Test
    void movimientoRechazadoPorTiempoNoSeRegistra() {
        FuenteDeTiempoFalsa fuente = new FuenteDeTiempoFalsa();
        Partida partida = new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                new DetectorDeJaque(), List.of(),
                new RelojAjedrez(Duration.ofSeconds(1), Duration.ofSeconds(1), fuente));
        fuente.avanzar(Duration.ofSeconds(1));
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e2", "e4")));
        assertTrue(partida.historial().isEmpty());
        assertEquals(MotivoFinalizacion.TIEMPO_AGOTADO, partida.resultado().orElseThrow().motivo());
    }
}
