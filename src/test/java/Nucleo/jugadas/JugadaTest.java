package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Torre;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del Command: se compara el tablero antes de aplicar, después de aplicar y después de
 * revertir. Después de revertir tiene que quedar idéntico a como estaba antes de aplicar.
 */
class JugadaTest {

    private static Map<Posicion, Pieza> foto(Tablero tablero) {
        return new HashMap<>(tablero.piezas());
    }

    @Test
    void unaJugadaNormalSinCapturaEsSoloUnTraslado() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....P...",
                "........");
        Pieza peon = tablero.piezaEn(pos("e2")).orElseThrow();

        // Act
        Jugada jugada = Jugada.normal(tablero, pos("e2"), pos("e4"));

        // Assert
        assertEquals(List.of(new Traslado(peon, pos("e2"), pos("e4"))), jugada.efectos());
        assertSame(peon, jugada.piezaMovida());
        assertTrue(jugada.capturada().isEmpty());
    }

    @Test
    void unaJugadaNormalConCapturaPrimeroRetiraYDespuesTraslada() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "...p....",
                "....P...",
                "........",
                "........",
                "........");
        Pieza peon = tablero.piezaEn(pos("e4")).orElseThrow();
        Pieza rival = tablero.piezaEn(pos("d5")).orElseThrow();

        // Act
        Jugada jugada = Jugada.normal(tablero, pos("e4"), pos("d5"));

        // Assert
        assertEquals(List.of(new Retiro(rival, pos("d5")), new Traslado(peon, pos("e4"), pos("d5"))),
                jugada.efectos());
        assertSame(rival, jugada.capturada().orElseThrow());
    }

    @Test
    void revertirUnaCapturaDejaElTableroComoAntesDeAplicar() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "...p....",
                "....P...",
                "........",
                "........",
                "....R...");
        Map<Posicion, Pieza> antes = foto(tablero);
        Jugada jugada = Jugada.normal(tablero, pos("e4"), pos("d5"));

        // Act
        jugada.aplicar(tablero);
        Map<Posicion, Pieza> despues = foto(tablero);
        jugada.revertir(tablero);

        // Assert
        assertNotEquals(antes, despues);
        assertEquals(3, despues.size());
        assertEquals(antes, foto(tablero));
    }

    @Test
    void unaJugadaPuedeMoverDosPiezasComoUnEnroque() {
        // Arrange: el rey va de e1 a g1 y la torre de h1 a f1
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R..T");
        Pieza rey = tablero.piezaEn(pos("e1")).orElseThrow();
        Pieza torre = tablero.piezaEn(pos("h1")).orElseThrow();
        Map<Posicion, Pieza> antes = foto(tablero);
        Jugada enroque = new Jugada(pos("e1"), pos("g1"), List.of(
                new Traslado(rey, pos("e1"), pos("g1")),
                new Traslado(torre, pos("h1"), pos("f1"))));

        // Act
        enroque.aplicar(tablero);

        // Assert
        assertSame(rey, tablero.piezaEn(pos("g1")).orElseThrow());
        assertSame(torre, tablero.piezaEn(pos("f1")).orElseThrow());
        assertSame(rey, enroque.piezaMovida());
        assertTrue(enroque.capturada().isEmpty());

        // Act + Assert: deshacer
        enroque.revertir(tablero);
        assertEquals(antes, foto(tablero));
    }

    @Test
    void unaJugadaPuedeCapturarEnOtraCasillaComoLaCapturaAlPaso() {
        // Arrange: el peón blanco va de e5 a d6 y el negro capturado está en d5
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "...pP...",
                "........",
                "........",
                "........",
                "........");
        Pieza blanco = tablero.piezaEn(pos("e5")).orElseThrow();
        Pieza negro = tablero.piezaEn(pos("d5")).orElseThrow();
        Map<Posicion, Pieza> antes = foto(tablero);
        Jugada alPaso = new Jugada(pos("e5"), pos("d6"), List.of(
                new Retiro(negro, pos("d5")),
                new Traslado(blanco, pos("e5"), pos("d6"))));

        // Act
        alPaso.aplicar(tablero);

        // Assert
        assertEquals(1, tablero.piezas().size());
        assertSame(blanco, tablero.piezaEn(pos("d6")).orElseThrow());
        assertSame(negro, alPaso.capturada().orElseThrow());

        // Act + Assert: deshacer
        alPaso.revertir(tablero);
        assertEquals(antes, foto(tablero));
    }

    @Test
    void siUnEfectoFallaAMitadDeCaminoElTableroQuedaComoEstaba() {
        // Arrange: el segundo traslado espera una torre en h1 que no está
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");
        Pieza rey = tablero.piezaEn(pos("e1")).orElseThrow();
        Pieza torreAusente = new Torre(Bando.BLANCO);
        Map<Posicion, Pieza> antes = foto(tablero);
        Jugada invalida = new Jugada(pos("e1"), pos("g1"), List.of(
                new Traslado(rey, pos("e1"), pos("g1")),
                new Traslado(torreAusente, pos("h1"), pos("f1"))));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> invalida.aplicar(tablero));
        assertEquals(antes, foto(tablero));
    }

    @Test
    void noSePuedeRevertirSobreUnTableroQueNoEstaDespuesDeLaJugada() {
        // Arrange: la jugada nunca se aplicó
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....P...",
                "........");
        Map<Posicion, Pieza> antes = foto(tablero);
        Jugada jugada = Jugada.normal(tablero, pos("e2"), pos("e4"));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> jugada.revertir(tablero));
        assertEquals(antes, foto(tablero));
    }

    @Test
    void rechazaUnaJugadaQueNoTrasladaLaPiezaDelOrigen() {
        // Arrange
        Pieza peon = new Peon(Bando.BLANCO);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Jugada(pos("e2"), pos("e4"),
                List.of(new Traslado(peon, pos("d2"), pos("d4")))));
        assertThrows(IllegalArgumentException.class, () -> new Jugada(pos("e2"), pos("e4"), List.of()));
    }

    @Test
    void guardaUnaCopiaInmodificableDeLosEfectos() {
        // Arrange
        Pieza peon = new Peon(Bando.BLANCO);
        List<Efecto> efectos = new ArrayList<>(List.of(new Traslado(peon, pos("e2"), pos("e4"))));
        Jugada jugada = new Jugada(pos("e2"), pos("e4"), efectos);

        // Act
        efectos.add(new Retiro(peon, pos("a1")));

        // Assert
        assertEquals(1, jugada.efectos().size());
        assertThrows(UnsupportedOperationException.class, () -> jugada.efectos().add(new Retiro(peon, pos("a1"))));
    }

    @Test
    void unaJugadaNormalNecesitaUnaPiezaEnElOrigen() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> Jugada.normal(TableroFake.vacio(), pos("e2"), pos("e4")));
    }
}
