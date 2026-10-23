package Nucleo.reglas;

import dobles.Contextos;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Solo importan los dos booleanos del contexto: no hace falta armar ninguna posición. */
class AhogadoTest {

    private final Ahogado ahogado = new Ahogado();

    @Test
    void sinJaqueYSinJugadasLegalesEsAhogado() {
        // Act
        Optional<EstadoPartida> resultado = ahogado.evaluar(Contextos.con(false, false));

        // Assert
        assertEquals(Optional.of(EstadoPartida.TABLAS_AHOGADO), resultado);
    }

    @Test
    void enJaqueYSinJugadasNoEsAhogadoSinoMate() {
        // Act
        Optional<EstadoPartida> resultado = ahogado.evaluar(Contextos.con(true, false));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void conJugadasLegalesNoEsAhogado() {
        // Assert
        assertTrue(ahogado.evaluar(Contextos.con(false, true)).isEmpty());
        assertTrue(ahogado.evaluar(Contextos.con(true, true)).isEmpty());
    }
}
