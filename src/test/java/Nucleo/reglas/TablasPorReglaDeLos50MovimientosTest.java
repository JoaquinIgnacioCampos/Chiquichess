package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Retiro;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Caballo;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;
import dobles.Contextos;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La condición solo mira el historial, así que alcanza con jugadas armadas a mano: no hace falta
 * que correspondan a un tablero real.
 */
class TablasPorReglaDeLos50MovimientosTest {

    private final Tablero tablero = TableroFake.vacio();
    private final Pieza caballo = new Caballo(Bando.BLANCO);
    private final Pieza peon = new Peon(Bando.BLANCO);

    /** Jugada de una pieza que no es peón y sin captura: no hay "progreso". */
    private Jugada suelta() {
        return new Jugada(pos("b1"), pos("c3"), List.of(new Traslado(caballo, pos("b1"), pos("c3"))));
    }

    private Jugada dePeon() {
        return new Jugada(pos("e2"), pos("e4"), List.of(new Traslado(peon, pos("e2"), pos("e4"))));
    }

    private Jugada captura() {
        return new Jugada(pos("b1"), pos("c3"), List.of(
                new Retiro(new Caballo(Bando.NEGRO), pos("c3")), new Traslado(caballo, pos("b1"), pos("c3"))));
    }

    private Optional<EstadoPartida> evaluar(TablasPorReglaDeLos50Movimientos regla, Jugada... jugadas) {
        return regla.evaluar(Contextos.conHistorial(tablero, Bando.BLANCO, List.of(jugadas)));
    }

    @Test
    void rechazaUnLimiteMenorAUno() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new TablasPorReglaDeLos50Movimientos(0));
    }

    @Test
    void sinJugadasNoHayTablas() {
        // Arrange: con límite 1 bastarían 2 jugadas sueltas
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act
        Optional<EstadoPartida> resultado = regla.evaluar(Contextos.inicial(tablero, Bando.BLANCO));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void conLimiteUnoSonTablasDespuesDeUnaJugadaSueltaDeCadaJugador() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act
        Optional<EstadoPartida> trasUna = evaluar(regla, suelta());
        Optional<EstadoPartida> trasDos = evaluar(regla, suelta(), suelta());

        // Assert
        assertTrue(trasUna.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), trasDos);
    }

    @Test
    void unMovimientoDePeonReiniciaLaCuenta() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act: después del peón hay una sola jugada suelta
        Optional<EstadoPartida> resultado = evaluar(regla, suelta(), dePeon(), suelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void unaCapturaReiniciaLaCuenta() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act
        Optional<EstadoPartida> resultado = evaluar(regla, suelta(), captura(), suelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void lasJugadasSueltasAnterioresAlProgresoNoCuentan() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(2);

        // Act: tres sueltas, un peón y tres sueltas: solo cuentan las tres últimas
        Optional<EstadoPartida> resultado = evaluar(regla,
                suelta(), suelta(), suelta(), dePeon(), suelta(), suelta(), suelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void porDefectoSonTablasRecienEnLaJugada100SinProgreso() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos();
        List<Jugada> noventaYNueve = new ArrayList<>(Collections.nCopies(99, suelta()));
        List<Jugada> cien = new ArrayList<>(Collections.nCopies(100, suelta()));

        // Act
        Optional<EstadoPartida> en99 = regla.evaluar(Contextos.conHistorial(tablero, Bando.NEGRO, noventaYNueve));
        Optional<EstadoPartida> en100 = regla.evaluar(Contextos.conHistorial(tablero, Bando.BLANCO, cien));

        // Assert
        assertTrue(en99.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), en100);
    }

    @Test
    void noGuardaMemoriaEntreEvaluaciones() {
        // Arrange
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act: muchas evaluaciones con una sola jugada suelta nunca suman entre sí
        for (int i = 0; i < 5; i++) {
            evaluar(regla, suelta());
        }
        Optional<EstadoPartida> resultado = evaluar(regla, suelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
