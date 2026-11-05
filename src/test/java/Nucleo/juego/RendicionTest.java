package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.reglas.*;
import Nucleo.reglas.MotivoFinalizacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class RendicionTest {
    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Bando.BLANCO), new Jugador("Beto", Bando.NEGRO),
                new DetectorDeJaque(), List.of(new JaqueMate(), new Ahogado()));
    }

    @Test
    void seRindenBlancasYGananNegras() {
        Partida partida = partida();
        partida.rendirse(Bando.BLANCO);
        assertEquals(Optional.of(Bando.NEGRO), partida.resultado().orElseThrow().ganador());
        assertEquals(MotivoFinalizacion.RENDICION, partida.resultado().orElseThrow().motivo());
        assertEquals(Bando.NEGRO, partida.ganador().orElseThrow().color());
        assertTrue(partida.estado().estaFinalizada());
        assertFalse(partida.estado().esTablas());
    }

    @Test
    void seRindenNegrasEnSuTurnoYGananBlancas() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        partida.rendirse(Bando.NEGRO);
        assertEquals(Optional.of(Bando.BLANCO), partida.resultado().orElseThrow().ganador());
        assertEquals(Bando.BLANCO, partida.ganador().orElseThrow().color());
    }

    @Test
    void blancasNoPuedenRendirseDuranteElTurnoNegro() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        var error = assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Bando.BLANCO));
        assertEquals(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, error.motivo());
        assertTrue(partida.resultado().isEmpty());
        assertEquals(Bando.NEGRO, partida.turno());
        assertEquals(List.of(mov("e2", "e4")), partida.historial());
    }

    @Test
    void negrasNoPuedenRendirseDuranteElTurnoBlanco() {
        Partida partida = partida();
        partida.ofrecerTablas(Bando.BLANCO);
        var error = assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Bando.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, error.motivo());
        assertTrue(partida.resultado().isEmpty());
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
        assertEquals(Optional.of(Bando.BLANCO), partida.ofertaTablasPendiente());
    }

    @Test
    void rendirseDespuesDeMateSeRechazaSinCambiarResultado() {
        Partida partida = partida();
        partida.mover(mov("f2", "f3"));
        partida.mover(mov("e7", "e5"));
        partida.mover(mov("g2", "g4"));
        partida.mover(mov("d8", "h4"));
        var resultado = partida.resultado();
        var error = assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Bando.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertEquals(resultado, partida.resultado());
    }

    @Test
    void moverDespuesDeRendicionSeRechaza() {
        Partida partida = partida();
        partida.rendirse(Bando.BLANCO);
        var error = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e2", "e4")));
        assertEquals(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertTrue(partida.movimientosLegales(dobles.Casillas.pos("e2")).isEmpty());
    }

    @Test
    void rendirseDosVecesSeRechaza() {
        Partida partida = partida();
        partida.rendirse(Bando.BLANCO);
        assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Bando.NEGRO));
        assertEquals(Bando.NEGRO, partida.ganador().orElseThrow().color());
    }

    @Test
    void rendicionNulaNoFinalizaLaPartida() {
        Partida partida = partida();
        assertThrows(NullPointerException.class, () -> partida.rendirse(null));
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }
}
