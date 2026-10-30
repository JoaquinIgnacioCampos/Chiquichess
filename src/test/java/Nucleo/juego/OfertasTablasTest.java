package Nucleo.juego;

import Nucleo.piezas.Color;
import Nucleo.reglas.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class OfertasTablasTest {
    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Color.BLANCO), new Jugador("Beto", Color.NEGRO),
                new DetectorDeJaque(), List.of(new JaqueMate(), new Ahogado()));
    }

    @Test
    void ofrecerYAceptarTerminaPorAcuerdoSinGanador() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
        partida.aceptarTablas(Color.NEGRO);
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
        partida.ofrecerTablas(Color.NEGRO);
        partida.aceptarTablas(Color.BLANCO);
        assertEquals(MotivoFinalizacion.ACUERDO, partida.resultado().orElseThrow().motivo());
    }

    @Test
    void rechazarBorraOfertaYPermiteSeguirJugando() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        partida.rechazarTablas(Color.NEGRO);
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        partida.ofrecerTablas(Color.BLANCO);
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
    }

    @Test
    void aceptarSinOfertaSeRechaza() {
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida().aceptarTablas(Color.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.SIN_OFERTA, error.motivo());
    }

    @Test
    void rechazarSinOfertaSeRechaza() {
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida().rechazarTablas(Color.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.SIN_OFERTA, error.motivo());
    }

    @Test
    void nadieAceptaNiRechazaSuPropiaOferta() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.aceptarTablas(Color.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.ES_SU_PROPIA_OFERTA, error.motivo());
        assertThrows(AccionInvalidaException.class, () -> partida.rechazarTablas(Color.BLANCO));
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
        assertTrue(partida.resultado().isEmpty());
    }

    @Test
    void ofrecerDosVecesNoReemplazaLaOferta() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.ofrecerTablas(Color.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.OFERTA_PENDIENTE, error.motivo());
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
    }

    @Test
    void ofrecerFueraDeTurnoSeRechaza() {
        Partida partida = partida();
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.ofrecerTablas(Color.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, error.motivo());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
    }

    @Test
    void moverElOferenteConservaLaOfertaPeroMoverElRivalLaBorra() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        partida.mover(mov("e2", "e4"));
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
        partida.mover(mov("e7", "e5"));
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }

    @Test
    void movimientoInvalidoDelRivalConservaOferta() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        partida.mover(mov("e2", "e4"));
        assertThrows(MovimientoInvalidoException.class, () -> partida.mover(mov("e7", "e4")));
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
        partida.aceptarTablas(Color.NEGRO);
        assertEquals(EstadoPartida.TABLAS_ACUERDO, partida.estado());
    }

    @Test
    void ofrecerYResponderTrasFinalizarSeRechaza() {
        Partida partida = partida();
        partida.ofrecerTablas(Color.BLANCO);
        partida.rendirse(Color.NEGRO);
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
        var error = assertThrows(AccionInvalidaException.class,
                () -> partida.ofrecerTablas(Color.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertThrows(AccionInvalidaException.class, () -> partida.aceptarTablas(Color.NEGRO));
        assertThrows(AccionInvalidaException.class, () -> partida.rechazarTablas(Color.NEGRO));
    }

    @Test
    void sePuedeOfrecerYResignarEstandoEnJaque() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        partida.mover(mov("f7", "f6"));
        partida.mover(mov("d1", "h5"));
        assertEquals(EstadoPartida.JAQUE, partida.estado());
        assertTrue(partida.resultado().isEmpty());
        partida.ofrecerTablas(Color.NEGRO);
        partida.rendirse(Color.NEGRO);
        assertEquals(Color.BLANCO, partida.ganador().orElseThrow().color());
        assertTrue(partida.ofertaTablasPendiente().isEmpty());
    }

    @Test
    void coloresNulosNoModificanLaOferta() {
        Partida partida = partida();
        assertThrows(NullPointerException.class, () -> partida.ofrecerTablas(null));
        partida.ofrecerTablas(Color.BLANCO);
        assertThrows(NullPointerException.class, () -> partida.aceptarTablas(null));
        assertThrows(NullPointerException.class, () -> partida.rechazarTablas(null));
        assertEquals(Optional.of(Color.BLANCO), partida.ofertaTablasPendiente());
    }
}
