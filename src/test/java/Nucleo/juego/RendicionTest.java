package Nucleo.juego;

import Nucleo.piezas.Color;
import Nucleo.reglas.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static dobles.Casillas.mov;
import static org.junit.jupiter.api.Assertions.*;

class RendicionTest {
    private Partida partida() {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Color.BLANCO), new Jugador("Beto", Color.NEGRO),
                new DetectorDeJaque(), List.of(new JaqueMate(), new Ahogado()));
    }

    @Test
    void seRindenBlancasYGananNegras() {
        Partida partida = partida();
        partida.rendirse(Color.BLANCO);
        assertEquals(Optional.of(Color.NEGRO), partida.resultado().orElseThrow().ganador());
        assertEquals(MotivoFinalizacion.RENDICION, partida.resultado().orElseThrow().motivo());
        assertEquals(Color.NEGRO, partida.ganador().orElseThrow().color());
        assertTrue(partida.estado().estaFinalizada());
        assertFalse(partida.estado().esTablas());
    }

    @Test
    void seRindenNegrasAunqueNoSeaSuTurnoYGananBlancas() {
        Partida partida = partida();
        partida.rendirse(Color.NEGRO);
        assertEquals(Optional.of(Color.BLANCO), partida.resultado().orElseThrow().ganador());
        assertEquals(Color.BLANCO, partida.ganador().orElseThrow().color());
    }

    @Test
    void blancasTambienPuedenRendirseDuranteElTurnoNegro() {
        Partida partida = partida();
        partida.mover(mov("e2", "e4"));
        partida.rendirse(Color.BLANCO);
        assertEquals(Color.NEGRO, partida.ganador().orElseThrow().color());
    }

    @Test
    void rendirseDespuesDeMateSeRechazaSinCambiarResultado() {
        Partida partida = partida();
        partida.mover(mov("f2", "f3"));
        partida.mover(mov("e7", "e5"));
        partida.mover(mov("g2", "g4"));
        partida.mover(mov("d8", "h4"));
        var resultado = partida.resultado();
        var error = assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Color.NEGRO));
        assertEquals(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertEquals(resultado, partida.resultado());
    }

    @Test
    void moverDespuesDeRendicionSeRechaza() {
        Partida partida = partida();
        partida.rendirse(Color.BLANCO);
        var error = assertThrows(MovimientoInvalidoException.class,
                () -> partida.mover(mov("e2", "e4")));
        assertEquals(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA, error.motivo());
        assertTrue(partida.movimientosLegales(dobles.Casillas.pos("e2")).isEmpty());
    }

    @Test
    void rendirseDosVecesSeRechaza() {
        Partida partida = partida();
        partida.rendirse(Color.NEGRO);
        assertThrows(AccionInvalidaException.class, () -> partida.rendirse(Color.BLANCO));
        assertEquals(Color.BLANCO, partida.ganador().orElseThrow().color());
    }

    @Test
    void rendicionNulaNoFinalizaLaPartida() {
        Partida partida = partida();
        assertThrows(NullPointerException.class, () -> partida.rendirse(null));
        assertEquals(EstadoPartida.EN_CURSO, partida.estado());
    }
}
