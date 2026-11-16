package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.reglas.*;
import Nucleo.reglas.MotivoFinalizacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class OfertasTablasTest {
    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                new DetectorDeJaque(), List.of(new JaqueMate(), new Ahogado()));
    }

    @Test
    void ofrecerYAceptarTerminaPorAcuerdoSinGanador() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
        partida.aceptarTablas(Bando.NEGRO);
        assertEquals(EstadoPartida.TABLAS_ACUERDO, partida.estado());
        assertTrue(partida.estado().estaFinalizada());
        assertTrue(partida.estado().esTablas());
        assertEquals(new ResultadoPartida(Optional.empty(), MotivoFinalizacion.ACUERDO),
                partida.resultado().orElseThrow());
        assertTrue(partida.ganador().isEmpty());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e2", "e4")));
    }

    @Test
    void negrasTambienPuedenOfrecerDuranteSuTurno() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        partida.ofrecerTablas(Bando.NEGRO);
        partida.aceptarTablas(Bando.BLANCO);
        assertEquals(MotivoFinalizacion.ACUERDO, partida.resultado().orElseThrow().motivo());
    }

    @Test
    void rechazarBorraOfertaYPermiteSeguirJugando() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        partida.rechazarTablas(Bando.NEGRO);
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        partida.ofrecerTablas(Bando.BLANCO);
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
    }

    @Test
    void aceptarSinOfertaSeRechaza() {
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida().aceptarTablas(Bando.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.SIN_OFERTA, error.motivo());
    }

    @Test
    void rechazarSinOfertaSeRechaza() {
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida().rechazarTablas(Bando.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.SIN_OFERTA, error.motivo());
    }

    @Test
    void nadieAceptaNiRechazaSuPropiaOferta() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.aceptarTablas(Bando.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.ES_SU_PROPIA_OFERTA, error.motivo());
        assertThrows(AccionInvalidaException.class, () -> partida.rechazarTablas(Bando.BLANCO));
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
        assertTrue(partida.resultado().isEmpty());
    }

    @Test
    void ofrecerDosVecesNoReemplazaLaOferta() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.ofrecerTablas(Bando.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.OFERTA_PENDIENTE, error.motivo());
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
    }

    @Test
    void ofrecerFueraDeTurnoSeRechaza() {
        Partida partida = partida();
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.ofrecerTablas(Bando.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, error.motivo());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
    }

    @Test
    void moverElOferenteConservaLaOfertaPeroMoverElRivalLaBorra() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        partida.mover(mov("e2", "e4"));
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
        partida.mover(mov("e7", "e5"));
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }

    @Test
    void movimientoInvalidoDelRivalConservaOferta() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        partida.mover(mov("e2", "e4"));
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e7", "e4")));
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
        partida.aceptarTablas(Bando.NEGRO);
        assertEquals(EstadoPartida.TABLAS_ACUERDO, partida.estado());
    }

    @Test
    void ofrecerYResponderTrasFinalizarSeRechaza() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        partida.rendirse(Bando.BLANCO);
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.ofrecerTablas(Bando.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertThrows(AccionInvalidaException.class, () -> partida.aceptarTablas(Bando.NEGRO));
        assertThrows(AccionInvalidaException.class, () -> partida.rechazarTablas(Bando.NEGRO));
    }

    @Test
    void sePuedeOfrecerYResignarEstandoEnJaque() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("f7", "f6"));
        partida.mover(mov("d1", "h5"));
        assertEquals(EstadoPartida.JAQUE, partida.estado());
        assertTrue(partida.resultado().isEmpty());
        partida.ofrecerTablas(Bando.NEGRO);
        partida.rendirse(Bando.NEGRO);
        assertEquals(Bando.BLANCO, partida.ganador().orElseThrow().color());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
    }

    @Test
    void coloresNulosNoModificanLaOferta() {
        Partida partida = partida();
        assertThrows(NullPointerException.class, () -> partida.ofrecerTablas(null));
        partida.ofrecerTablas(Bando.BLANCO);
        assertThrows(NullPointerException.class, () -> partida.aceptarTablas(null));
        assertThrows(NullPointerException.class, () -> partida.rechazarTablas(null));
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
    }
}
