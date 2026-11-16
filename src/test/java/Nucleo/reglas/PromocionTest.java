package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Reemplazo;
import Nucleo.jugadas.Retiro;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Torre;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class PromocionTest {

    private final Promocion promocion = new Promocion();

    /** Peón blanco en a7 con torre negra en b8, y los dos reyes. */
    private Tablero peonAPuntoDePromover() {
        return TableroFake.desde(
                ".t..r...",
                "P.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");
    }

    private List<Character> codigos(List<Jugada> jugadas) {
        return jugadas.stream().map(j -> j.promocion().orElseThrow().codigo()).toList();
    }

    @Test
    void elAvanceALaUltimaFilaSeConvierteEnUnaJugadaPorCadaPieza() {
        // Arrange
        Tablero tablero = peonAPuntoDePromover();
        Pieza peon = tablero.piezaEn(pos("a7")).orElseThrow();
        Jugada avance = Jugada.normal(tablero, pos("a7"), pos("a8"));

        // Act
        List<Jugada> variantes = promocion.transformar(tablero, avance);

        // Assert: dama primero (la elección por defecto), después torre, alfil y caballo
        assertEquals(List.of('D', 'T', 'A', 'C'), codigos(variantes));
        Jugada aDama = variantes.get(0);
        assertEquals(new Traslado(peon, pos("a7"), pos("a8")), aDama.efectos().get(0));
        assertInstanceOf(Reemplazo.class, aDama.efectos().get(1));
        assertSame(peon, aDama.piezaMovida());
        assertEquals(Bando.BLANCO, aDama.promocion().orElseThrow().color());
    }

    @Test
    void unaCapturaHaciaLaUltimaFilaTambienPromueveYConservaLaCaptura() {
        // Arrange
        Tablero tablero = peonAPuntoDePromover();
        Pieza torreNegra = tablero.piezaEn(pos("b8")).orElseThrow();
        Jugada captura = Jugada.normal(tablero, pos("a7"), pos("b8"));

        // Act
        List<Jugada> variantes = promocion.transformar(tablero, captura);

        // Assert
        assertEquals(4, variantes.size());
        assertEquals(new Retiro(torreNegra, pos("b8")), variantes.get(0).efectos().get(0));
        assertSame(torreNegra, variantes.get(0).capturada().orElseThrow());
    }

    @Test
    void lasNegrasPromuevenEnLaFilaUno() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "p.......",
                "....R...");

        // Act
        List<Jugada> variantes = promocion.transformar(tablero, Jugada.normal(tablero, pos("a2"), pos("a1")));

        // Assert
        assertEquals(4, variantes.size());
        assertEquals(Bando.NEGRO, variantes.get(0).promocion().orElseThrow().color());
    }

    @Test
    void lasJugadasQueNoLleganALaUltimaFilaQuedanIguales() {
        // Arrange: un avance de peón común y una jugada de torre que llega a la última fila
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "P......T",
                "....R...");
        Jugada avance = Jugada.normal(tablero, pos("a2"), pos("a4"));
        Jugada torre = Jugada.normal(tablero, pos("h2"), pos("h8"));

        // Act + Assert
        assertEquals(List.of(avance), promocion.transformar(tablero, avance));
        assertEquals(List.of(torre), promocion.transformar(tablero, torre));
    }

    @Test
    void noSumaJugadasPropias() {
        // Act + Assert
        assertTrue(promocion.jugadas(peonAPuntoDePromover(), pos("a7"), List.of(), new DetectorDeJaque()).isEmpty());
    }

    @Test
    void lasOpcionesSePuedenConfigurarYLaPrimeraEsLaElegidaPorDefecto() {
        // Arrange: una variante que solo permite promover a torre
        Promocion soloTorre = new Promocion(List.of(Torre::new));
        Tablero tablero = peonAPuntoDePromover();

        // Act
        List<Jugada> variantes = soloTorre.transformar(tablero, Jugada.normal(tablero, pos("a7"), pos("a8")));

        // Assert
        assertEquals(List.of('T'), codigos(variantes));
        assertThrows(IllegalArgumentException.class, () -> new Promocion(List.of()));
    }
}
