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
import dobles.RegistroDeJugadas;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * La condición solo mira la última jugada de cada registro, así que alcanza con jugadas armadas a
 * mano: no hace falta que correspondan a un tablero real.
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

    /** Registra la posición inicial y las jugadas, y evalúa al final. */
    private Optional<EstadoPartida> tras(TablasPorReglaDeLos50Movimientos regla, Jugada... jugadas) {
        var registro = RegistroDeJugadas.registrarSinAplicar(regla, tablero, List.of(jugadas));
        return regla.evaluar(registro.ultimoContexto());
    }

    @Test
    void rechazaUnLimiteMenorAUno() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new TablasPorReglaDeLos50Movimientos(0));
    }

    @Test
    void enLaPosicionInicialNoHayTablas() {
        // Arrange: con límite 1 bastarían 2 jugadas sueltas
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);

        // Act
        Optional<EstadoPartida> resultado = tras(regla);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void conLimiteUnoSonTablasDespuesDeUnaJugadaSueltaDeCadaJugador() {
        // Act
        Optional<EstadoPartida> trasUna = tras(new TablasPorReglaDeLos50Movimientos(1), suelta());
        Optional<EstadoPartida> trasDos = tras(new TablasPorReglaDeLos50Movimientos(1), suelta(), suelta());

        // Assert
        assertTrue(trasUna.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), trasDos);
    }

    @Test
    void unMovimientoDePeonReiniciaLaCuenta() {
        // Act: después del peón hay una sola jugada suelta
        Optional<EstadoPartida> resultado = tras(new TablasPorReglaDeLos50Movimientos(1), suelta(), dePeon(), suelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void unaCapturaReiniciaLaCuenta() {
        // Act
        Optional<EstadoPartida> resultado = tras(new TablasPorReglaDeLos50Movimientos(1), suelta(), captura(), suelta());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void porDefectoSonTablasRecienEnLaJugada100SinProgreso() {
        // Act
        Optional<EstadoPartida> en99 = tras(new TablasPorReglaDeLos50Movimientos(),
                Collections.nCopies(99, suelta()).toArray(Jugada[]::new));
        Optional<EstadoPartida> en100 = tras(new TablasPorReglaDeLos50Movimientos(),
                Collections.nCopies(100, suelta()).toArray(Jugada[]::new));

        // Assert
        assertTrue(en99.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), en100);
    }

    // ---------------- Memento ----------------

    @Test
    void restaurarElUltimoRegistroDejaLaCondicionComoAntesDeRegistrarlo() {
        // Arrange: con límite 1, dos jugadas sueltas son tablas
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);
        var registro = RegistroDeJugadas.registrarSinAplicar(regla, tablero, List.of(suelta(), suelta()));
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), regla.evaluar(registro.ultimoContexto()));

        // Act: deshacer la segunda jugada
        regla.restaurar(registro.recuerdos().get(2));

        // Assert
        assertTrue(regla.evaluar(registro.ultimoContexto()).isEmpty());
    }

    @Test
    void restaurarRecuperaElContadorAunqueLaJugadaLoHayaVueltoACero() {
        // Arrange: límite 1. Suelta (contador 1), peón (contador 0)
        TablasPorReglaDeLos50Movimientos regla = new TablasPorReglaDeLos50Movimientos(1);
        var registro = RegistroDeJugadas.registrarSinAplicar(regla, tablero, List.of(suelta(), dePeon()));

        // Act: deshacer el peón y jugar otra suelta en su lugar
        regla.restaurar(registro.recuerdos().get(2));
        regla.registrar(Contextos.conHistorial(tablero, Bando.BLANCO, List.of(suelta(), suelta())));

        // Assert: el contador volvió a 1 y con la nueva suelta llegó a 2
        assertEquals(Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS), regla.evaluar(registro.ultimoContexto()));
    }

    @Test
    void rechazaRecuerdosDeOtraCondicion() {
        // Arrange
        TablasPorReglaDeLos50Movimientos una = new TablasPorReglaDeLos50Movimientos();
        TablasPorReglaDeLos50Movimientos otra = new TablasPorReglaDeLos50Movimientos();
        Recuerdo deLaOtra = otra.registrar(Contextos.inicial(tablero, Bando.BLANCO));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> una.restaurar(deLaOtra));
        assertThrows(IllegalArgumentException.class, () -> una.restaurar(new Recuerdo() { }));
    }
}
