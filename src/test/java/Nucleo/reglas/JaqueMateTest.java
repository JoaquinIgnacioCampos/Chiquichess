package Nucleo.reglas;

import dobles.Contextos;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Solo importan los dos booleanos del contexto: no hace falta armar ninguna posición. */
class JaqueMateTest {

    private final JaqueMate jaqueMate = new JaqueMate();

    @Test
    void enJaqueYSinJugadasLegalesEsJaqueMate() {
        // Act
        Optional<EstadoPartida> resultado = jaqueMate.evaluar(Contextos.con(true, false));

        // Assert
        assertEquals(Optional.of(EstadoPartida.JAQUE_MATE), resultado);
    }

    @Test
    void enJaqueConJugadasLegalesNoTerminaLaPartida() {
        // Act
        Optional<EstadoPartida> resultado = jaqueMate.evaluar(Contextos.con(true, true));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void sinJaqueYSinJugadasNoEsJaqueMate() {
        // Act
        Optional<EstadoPartida> resultado = jaqueMate.evaluar(Contextos.con(false, false));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void sinJaqueConJugadasNoTerminaLaPartida() {
        // Act
        Optional<EstadoPartida> resultado = jaqueMate.evaluar(Contextos.con(false, true));

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
