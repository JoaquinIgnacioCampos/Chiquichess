package Nucleo.juego;

import Nucleo.piezas.Color;
import Nucleo.reglas.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResultadoPartidaTest {
    private Partida partida(CondicionDeFinalizacion... condiciones) {
        return new Partida(new DisposicionEstandar().crearTablero(),
                new Jugador("Ana", Color.BLANCO), new Jugador("Beto", Color.NEGRO),
                new DetectorDeJaque(), List.of(condiciones));
    }

    @Test
    void enCursoNoTieneResultado() {
        assertTrue(partida().resultado().isEmpty());
    }

    @Test
    void mateSeparaGanadorYMotivoConservandoLaConsultaAnterior() {
        Partida partida = partida(c -> Optional.of(EstadoPartida.JAQUE_MATE));
        ResultadoPartida resultado = partida.resultado().orElseThrow();
        assertEquals(Optional.of(Color.NEGRO), resultado.ganador());
        assertEquals(MotivoFinalizacion.JAQUE_MATE, resultado.motivo());
        assertEquals(Color.NEGRO, partida.ganador().orElseThrow().color());
    }

    @Test
    void lasTablasAutomaticasConservanSusMotivosSinGanador() {
        var casos = java.util.Map.of(
                EstadoPartida.TABLAS_AHOGADO, MotivoFinalizacion.AHOGADO,
                EstadoPartida.TABLAS_REPETICION, MotivoFinalizacion.REPETICION,
                EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS, MotivoFinalizacion.REGLA_50_MOVIMIENTOS);
        casos.forEach((estado, motivo) -> {
            Partida partida = partida(c -> Optional.of(estado));
            assertEquals(estado, partida.estado());
            assertEquals(new ResultadoPartida(Optional.empty(), motivo), partida.resultado().orElseThrow());
            assertTrue(partida.ganador().isEmpty());
        });
    }

    @Test
    void resultadoRechazaCombinacionesInconsistentes() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResultadoPartida(Optional.of(Color.BLANCO), MotivoFinalizacion.ACUERDO));
        assertThrows(IllegalArgumentException.class,
                () -> new ResultadoPartida(Optional.empty(), MotivoFinalizacion.RENDICION));
    }
}
