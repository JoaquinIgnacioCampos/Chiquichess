package Nucleo.reglas;

import Nucleo.piezas.Caballo;
import Nucleo.piezas.Color;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;
import dobles.Contextos;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TablasPorReglaDeLos50MovimientosTest {

    private final Tablero tablero = TableroFake.vacio();
    private final Pieza caballo = new Caballo(Color.BLANCO);
    private final Pieza peon = new Peon(Color.BLANCO);

    @Test
    void rechazaUnLimiteMenorAUno() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new TablasPorReglaDeLos50Movimientos(0));
    }

    @Test
    void laEvaluacionInicialNoCuentaComoJugada() {
        // Arrange: con límite 1 bastarían 2 jugadas sueltas
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act
        Optional<EstadoPartida> r1 = regla.evaluar(Contextos.inicial(tablero, Color.BLANCO));
        Optional<EstadoPartida> r2 = regla.evaluar(Contextos.inicial(tablero, Color.BLANCO));

        // Assert
        assertTrue(r1.isEmpty());
        assertTrue(r2.isEmpty());
    }

    @Test
    void conLimiteUnoSonTablasDespuesDeUnaJugadaSueltaDeCadaJugador() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act
        Optional<EstadoPartida> primera = regla.evaluar(jugadaSuelta());
        Optional<EstadoPartida> segunda = regla.evaluar(jugadaSuelta());

        // Assert
        assertTrue(primera.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), segunda);
    }

    @Test
    void unMovimientoDePeonReiniciaLaCuenta() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);
        regla.evaluar(jugadaSuelta());

        // Act
        regla.evaluar(Contextos.trasMover(tablero, Color.BLANCO, peon, false));
        Optional<EstadoPartida> resultado = regla.evaluar(jugadaSuelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void unaCapturaReiniciaLaCuenta() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);
        regla.evaluar(jugadaSuelta());

        // Act
        regla.evaluar(Contextos.trasMover(tablero, Color.BLANCO, caballo, true));
        Optional<EstadoPartida> resultado = regla.evaluar(jugadaSuelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void porDefectoSonTablasRecienEnLaJugada100SinProgreso() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos();

        // Act
        for (int i = 1; i <= 99; i++) {
            assertTrue(regla.evaluar(jugadaSuelta()).isEmpty(), "No deberían ser tablas en la jugada " + i);
        }
        Optional<EstadoPartida> jugada100 = regla.evaluar(jugadaSuelta());

        // Assert
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), jugada100);
    }

    /** Jugada de una pieza que no es peón y sin captura: no hay "progreso". */
    private ContextoDeJugada jugadaSuelta() {
        return Contextos.trasMover(tablero, Color.BLANCO, caballo, false);
    }
}
