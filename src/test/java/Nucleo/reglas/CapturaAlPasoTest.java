package Nucleo.reglas;

import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Retiro;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cada test arma con el fake la posición ya con el último avance hecho, y le pasa a la regla un
 * historial cuya última jugada es ese avance.
 */
class CapturaAlPasoTest {

    private final CapturaAlPaso alPaso = new CapturaAlPaso();
    private final DetectorDeJaque detector = new DetectorDeJaque();

    /** Peón blanco en e5 y peón negro en d5 (que acaba de llegar), con los dos reyes. */
    private Tablero blancoEnE5NegroEnD5() {
        return TableroFake.desde(
                "....r...",
                "........",
                "........",
                "...pP...",
                "........",
                "........",
                "........",
                "....R...");
    }

    private Jugada avance(Pieza peon, String desde, String hasta) {
        return new Jugada(pos(desde), pos(hasta), List.of(new Traslado(peon, pos(desde), pos(hasta))));
    }

    private List<Jugada> jugadas(Tablero tablero, String origen, List<Jugada> historial) {
        return alPaso.jugadas(tablero, pos(origen), historial, detector);
    }

    @Test
    void trasUnAvanceDobleDelRivalAlLadoSePuedeCapturarAlPaso() {
        // Arrange
        Tablero tablero = blancoEnE5NegroEnD5();
        Pieza blanco = tablero.piezaEn(pos("e5")).orElseThrow();
        Pieza negro = tablero.piezaEn(pos("d5")).orElseThrow();

        // Act
        List<Jugada> resultado = jugadas(tablero, "e5", List.of(avance(negro, "d7", "d5")));

        // Assert: el peón va a d6 y el negro sale de d5, no de la casilla de destino
        assertEquals(1, resultado.size());
        Jugada captura = resultado.get(0);
        assertEquals(pos("d6"), captura.destino());
        assertEquals(List.of(new Retiro(negro, pos("d5")), new Traslado(blanco, pos("e5"), pos("d6"))),
                captura.efectos());
        assertSame(negro, captura.capturada().orElseThrow());
    }

    @Test
    void funcionaHaciaLosDosLadosYParaLasNegras() {
        // Arrange: peón negro en d4; el blanco acaba de avanzar e2-e4 y queda a su derecha
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "...pP...",
                "........",
                "........",
                "....R...");
        Pieza blanco = tablero.piezaEn(pos("e4")).orElseThrow();

        // Act
        List<Jugada> resultado = jugadas(tablero, "d4", List.of(avance(blanco, "e2", "e4")));

        // Assert: el peón negro avanza hacia abajo, a e3
        assertEquals(1, resultado.size());
        assertEquals(pos("e3"), resultado.get(0).destino());
    }

    @Test
    void soloValeEnLaJugadaInmediataSiguiente() {
        // Arrange: después del avance doble hubo otra jugada
        Tablero tablero = blancoEnE5NegroEnD5();
        Pieza negro = tablero.piezaEn(pos("d5")).orElseThrow();
        Pieza reyBlanco = tablero.piezaEn(pos("e1")).orElseThrow();
        List<Jugada> historial = List.of(avance(negro, "d7", "d5"),
                new Jugada(pos("e2"), pos("e1"), List.of(new Traslado(reyBlanco, pos("e2"), pos("e1")))));

        // Act + Assert
        assertTrue(jugadas(tablero, "e5", historial).isEmpty());
    }

    @Test
    void unAvanceDeUnaSolaCasillaNoLaHabilita() {
        // Arrange
        Tablero tablero = blancoEnE5NegroEnD5();
        Pieza negro = tablero.piezaEn(pos("d5")).orElseThrow();

        // Act + Assert
        assertTrue(jugadas(tablero, "e5", List.of(avance(negro, "d6", "d5"))).isEmpty());
    }

    @Test
    void siLaUltimaJugadaFueDeOtraPiezaNoHayCaptura() {
        // Arrange: un rey negro que se movió, no un peón
        Tablero tablero = blancoEnE5NegroEnD5();
        Pieza reyNegro = tablero.piezaEn(pos("e8")).orElseThrow();

        // Act + Assert
        assertTrue(jugadas(tablero, "e5", List.of(avance(reyNegro, "e6", "e8"))).isEmpty());
    }

    @Test
    void elPeonRivalTieneQueQuedarAlLado() {
        // Arrange: el peón negro llegó a b5, lejos del blanco en e5
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                ".p..P...",
                "........",
                "........",
                "........",
                "....R...");
        Pieza negro = tablero.piezaEn(pos("b5")).orElseThrow();

        // Act + Assert
        assertTrue(jugadas(tablero, "e5", List.of(avance(negro, "b7", "b5"))).isEmpty());
    }

    @Test
    void noSeCapturaAlPasoUnPeonPropio() {
        // Arrange: el que avanzó doble es blanco, igual que el que pregunta
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "...PP...",
                "........",
                "........",
                "....R...");
        Pieza otroBlanco = tablero.piezaEn(pos("e4")).orElseThrow();

        // Act + Assert
        assertTrue(jugadas(tablero, "d4", List.of(avance(otroBlanco, "e2", "e4"))).isEmpty());
    }

    @Test
    void soloLosPeonesCapturanAlPaso() {
        // Arrange: un caballo blanco en e5 al lado del peón que avanzó
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "...pC...",
                "........",
                "........",
                "........",
                "....R...");
        Pieza negro = tablero.piezaEn(pos("d5")).orElseThrow();

        // Act + Assert
        assertTrue(jugadas(tablero, "e5", List.of(avance(negro, "d7", "d5"))).isEmpty());
        assertTrue(jugadas(tablero, "e5", List.of()).isEmpty());
    }
}
