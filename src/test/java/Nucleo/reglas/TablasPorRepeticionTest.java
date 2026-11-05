package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import dobles.Contextos;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las posiciones anteriores se reconstruyen revirtiendo el historial, así que cada test juega una
 * secuencia real sobre un tablero armado con el fake.
 */
class TablasPorRepeticionTest {

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

    @Test
    void rechazaMenosDeDosRepeticiones() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new TablasPorRepeticion(1));
    }

    @Test
    void laPosicionInicialSolaNoEsTablas() {
        // Act
        Optional<EstadoPartida> resultado = new TablasPorRepeticion(2)
                .evaluar(Contextos.inicial(conCaballos(), Bando.BLANCO));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void laTerceraVezQueApareceUnaPosicionEsTablas() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion();

        // Act: cada vuelta de los caballos repite la posición inicial
        Optional<EstadoPartida> segundaVez = regla.evaluar(Contextos.trasJugar(conCaballos(),
                "g1f3", "g8f6", "f3g1", "f6g8"));
        Optional<EstadoPartida> terceraVez = regla.evaluar(Contextos.trasJugar(conCaballos(),
                "g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8"));

        // Assert
        assertTrue(segundaVez.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), terceraVez);
    }

    @Test
    void conTurnoDistintoNoEsLaMismaPosicion() {
        // Arrange: el rey blanco hace un triángulo (3 jugadas) y el negro va y vuelve (2 jugadas).
        // Las piezas quedan como al principio, pero ahora juegan las negras.
        TablasPorRepeticion regla = new TablasPorRepeticion(2);

        // Act
        Optional<EstadoPartida> resultado = regla.evaluar(Contextos.trasJugar(conCaballos(),
                "e1d1", "e8d8", "d1d2", "d8e8", "d2e1"));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void reconstruyeLasPosicionesAunqueHayaCapturasEnElHistorial() {
        // Arrange: la torre blanca captura el peón de a7 y después los caballos van y vuelven.
        // La posición posterior a la captura (juegan las negras) aparece dos veces. Las anteriores a
        // la captura no pueden repetirse (tenían un peón más), así que la regla no las revisa.
        var contexto = Contextos.trasJugar(TableroFake.desde(
                        "....r.c.",
                        "p.......",
                        "........",
                        "........",
                        "........",
                        "........",
                        "........",
                        "T...R.C."),
                "a1a7", "g8f6", "g1f3", "f6g8", "f3g1");

        // Act
        Optional<EstadoPartida> conDos = new TablasPorRepeticion(2).evaluar(contexto);
        Optional<EstadoPartida> conTres = new TablasPorRepeticion(3).evaluar(contexto);

        // Assert
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), conDos);
        assertTrue(conTres.isEmpty());
    }

    @Test
    void noModificaElTableroQueRecibe() {
        // Arrange
        Tablero tablero = conCaballos();
        var contexto = Contextos.trasJugar(tablero, "g1f3", "g8f6");
        Map<Posicion, Pieza> antes = new HashMap<>(tablero.piezas());

        // Act
        new TablasPorRepeticion().evaluar(contexto);

        // Assert
        assertEquals(antes, tablero.piezas());
    }

    @Test
    void noGuardaMemoriaEntreEvaluaciones() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion(2);
        Tablero tablero = conCaballos();

        // Act: antes, cada evaluación sumaba una aparición; ahora la misma posición sin historial
        // es siempre una sola aparición
        regla.evaluar(Contextos.inicial(tablero, Bando.BLANCO));
        regla.evaluar(Contextos.inicial(tablero, Bando.BLANCO));
        Optional<EstadoPartida> resultado = regla.evaluar(Contextos.inicial(tablero, Bando.BLANCO));

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
