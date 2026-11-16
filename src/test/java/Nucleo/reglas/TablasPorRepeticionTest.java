package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.tablero.Tablero;
import dobles.Contextos;
import dobles.RegistroDeJugadas;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Cada test registra en la condición una secuencia real jugada sobre un tablero del fake, como hace Partida. */
class TablasPorRepeticionTest {

    private static final String[] VUELTA_DE_CABALLOS = {"g1f3", "g8f6", "f3g1", "f6g8"};

    /** Reyes en e1/e8 y caballos en g1/g8: los caballos pueden ir y volver sin cambiar nada más. */
    private Tablero conCaballos() {
        return TableroFake.desde(
                "....r.c.",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R.C.");
    }

    private Optional<EstadoPartida> tras(TablasPorRepeticion regla, Tablero tablero, String... jugadas) {
        var registro = RegistroDeJugadas.registrar(regla, tablero, jugadas);
        return regla.evaluar(registro.ultimoContexto());
    }

    @Test
    void rechazaMenosDeDosRepeticiones() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new TablasPorRepeticion(1));
    }

    @Test
    void laPosicionInicialSolaNoEsTablas() {
        // Act
        Optional<EstadoPartida> resultado = tras(new TablasPorRepeticion(2), conCaballos());

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void laTerceraVezQueApareceUnaPosicionEsTablas() {
        // Act: cada vuelta de los caballos repite la posición inicial
        Optional<EstadoPartida> segundaVez = tras(new TablasPorRepeticion(), conCaballos(), VUELTA_DE_CABALLOS);
        Optional<EstadoPartida> terceraVez = tras(new TablasPorRepeticion(), conCaballos(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");

        // Assert
        assertTrue(segundaVez.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), terceraVez);
    }

    @Test
    void conTurnoDistintoNoEsLaMismaPosicion() {
        // Arrange: el rey blanco hace un triángulo (3 jugadas) y el negro va y vuelve (2 jugadas).
        // Las piezas quedan como al principio, pero ahora juegan las negras.

        // Act
        Optional<EstadoPartida> resultado = tras(new TablasPorRepeticion(2), conCaballos(),
                "e1d1", "e8d8", "d1d2", "d8e8", "d2e1");

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void detectaLaRepeticionDespuesDeUnaCaptura() {
        // Arrange: la torre blanca captura el peón de a7 y después los caballos van y vuelven.
        // La posición posterior a la captura (juegan las negras) aparece dos veces.
        Tablero tablero = TableroFake.desde(
                "....r.c.",
                "p.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...R.C.");

        // Act
        Optional<EstadoPartida> conDos = tras(new TablasPorRepeticion(2), tablero.copiar(),
                "a1a7", "g8f6", "g1f3", "f6g8", "f3g1");
        Optional<EstadoPartida> conTres = tras(new TablasPorRepeticion(3), tablero.copiar(),
                "a1a7", "g8f6", "g1f3", "f6g8", "f3g1");

        // Assert
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), conDos);
        assertTrue(conTres.isEmpty());
    }

    // ---------------- Memento ----------------

    @Test
    void restaurarElUltimoRegistroQuitaEsaAparicion() {
        // Arrange: tercera aparición de la posición inicial
        TablasPorRepeticion regla = new TablasPorRepeticion();
        var registro = RegistroDeJugadas.registrar(regla, conCaballos(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        List<Recuerdo> recuerdos = registro.recuerdos();
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), regla.evaluar(registro.ultimoContexto()));

        // Act: deshacer la última jugada
        regla.restaurar(recuerdos.get(recuerdos.size() - 1));

        // Assert
        assertTrue(regla.evaluar(registro.ultimoContexto()).isEmpty());
    }

    @Test
    void rehacerDespuesDeRestaurarVuelveAContarLaAparicion() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion();
        var registro = RegistroDeJugadas.registrar(regla, conCaballos(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8");
        List<Recuerdo> recuerdos = registro.recuerdos();
        regla.restaurar(recuerdos.get(recuerdos.size() - 1));

        // Act: volver a registrar la misma jugada (lo que hará Partida al rehacer)
        regla.registrar(registro.ultimoContexto());

        // Assert
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), regla.evaluar(registro.ultimoContexto()));
    }

    @Test
    void restaurarTodoEnOrdenInversoDejaLaCondicionVacia() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion(2);
        Tablero tablero = conCaballos();
        List<Recuerdo> recuerdos = RegistroDeJugadas.registrar(regla, tablero, VUELTA_DE_CABALLOS).recuerdos();

        // Act
        for (int i = recuerdos.size() - 1; i >= 0; i--) {
            regla.restaurar(recuerdos.get(i));
        }

        // Assert: sin registros no hay tablas, y empezar de nuevo funciona como la primera vez
        assertTrue(regla.evaluar(Contextos.inicial(tablero, Bando.BLANCO)).isEmpty());
        regla.registrar(Contextos.inicial(tablero, Bando.BLANCO));
        assertTrue(regla.evaluar(Contextos.inicial(tablero, Bando.BLANCO)).isEmpty());
    }

    @Test
    void rechazaRecuerdosDeOtraCondicion() {
        // Arrange
        TablasPorRepeticion una = new TablasPorRepeticion();
        Recuerdo deLaOtra = new TablasPorRepeticion().registrar(Contextos.inicial(conCaballos(), Bando.BLANCO));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> una.restaurar(deLaOtra));
    }
}
