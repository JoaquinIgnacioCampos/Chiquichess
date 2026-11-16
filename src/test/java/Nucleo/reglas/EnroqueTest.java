package Nucleo.reglas;

import Nucleo.jugadas.Jugada;
import Nucleo.jugadas.Traslado;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

/** Cada test arma con el fake una posición que habilita o impide un enroque, con el detector real. */
class EnroqueTest {

    private final Enroque enroque = new Enroque();
    private final DetectorDeJaque detector = new DetectorDeJaque();

    /** Rey negro en a8 para que haya rey rival; rey y torres blancas en su fila base. */
    private Tablero listoParaEnrocar(String filaOcho) {
        return TableroFake.desde(
                filaOcho,
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...R..T");
    }

    private Set<Posicion> destinos(Tablero tablero, String origen, List<Jugada> historial) {
        return enroque.jugadas(tablero, pos(origen), historial, detector).stream()
                .map(Jugada::destino)
                .collect(Collectors.toSet());
    }

    private Set<Posicion> destinos(Tablero tablero, String origen) {
        return destinos(tablero, origen, List.of());
    }

    /** Una pieza que fue y volvió: queda en su casilla, pero ya se movió. */
    private List<Jugada> idaYVuelta(Pieza pieza, String desde, String hasta) {
        return List.of(
                new Jugada(pos(desde), pos(hasta), List.of(new Traslado(pieza, pos(desde), pos(hasta)))),
                new Jugada(pos(hasta), pos(desde), List.of(new Traslado(pieza, pos(hasta), pos(desde)))));
    }

    @Test
    void conTodoLibreElReyPuedeEnrocarCortoYLargo() {
        // Act
        Set<Posicion> resultado = destinos(listoParaEnrocar("r......."), "e1");

        // Assert
        assertEquals(posiciones("g1", "c1"), resultado);
    }

    @Test
    void elEnroqueCortoMueveElReyAG1YLaTorreAF1() {
        // Arrange
        Tablero tablero = listoParaEnrocar("r.......");
        Pieza rey = tablero.piezaEn(pos("e1")).orElseThrow();
        Pieza torre = tablero.piezaEn(pos("h1")).orElseThrow();

        // Act
        Jugada corto = enroque.jugadas(tablero, pos("e1"), List.of(), detector).stream()
                .filter(j -> j.destino().equals(pos("g1"))).findFirst().orElseThrow();

        // Assert
        assertEquals(List.of(new Traslado(rey, pos("e1"), pos("g1")), new Traslado(torre, pos("h1"), pos("f1"))),
                corto.efectos());
        assertSame(rey, corto.piezaMovida());
        assertTrue(corto.capturada().isEmpty());
    }

    @Test
    void elEnroqueLargoMueveElReyAC1YLaTorreAD1() {
        // Arrange
        Tablero tablero = listoParaEnrocar("r.......");
        Pieza rey = tablero.piezaEn(pos("e1")).orElseThrow();
        Pieza torre = tablero.piezaEn(pos("a1")).orElseThrow();

        // Act
        Jugada largo = enroque.jugadas(tablero, pos("e1"), List.of(), detector).stream()
                .filter(j -> j.destino().equals(pos("c1"))).findFirst().orElseThrow();

        // Assert
        assertEquals(List.of(new Traslado(rey, pos("e1"), pos("c1")), new Traslado(torre, pos("a1"), pos("d1"))),
                largo.efectos());
    }

    @Test
    void lasNegrasEnrocanEnLaFilaOcho() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "t...r..t",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act + Assert
        assertEquals(posiciones("g8", "c8"), destinos(tablero, "e8"));
    }

    @Test
    void siElReyYaSeMovioNoPuedeEnrocar() {
        // Arrange: el rey fue a e2 y volvió
        Tablero tablero = listoParaEnrocar("r.......");
        Pieza rey = tablero.piezaEn(pos("e1")).orElseThrow();

        // Act + Assert
        assertTrue(destinos(tablero, "e1", idaYVuelta(rey, "e1", "e2")).isEmpty());
    }

    @Test
    void siUnaTorreYaSeMovioSoloSePuedeEnrocarDelOtroLado() {
        // Arrange: la torre de h1 fue a h2 y volvió
        Tablero tablero = listoParaEnrocar("r.......");
        Pieza torre = tablero.piezaEn(pos("h1")).orElseThrow();

        // Act + Assert
        assertEquals(posiciones("c1"), destinos(tablero, "e1", idaYVuelta(torre, "h1", "h2")));
    }

    @Test
    void unaPiezaEnElMedioImpideEseEnroque() {
        // Arrange
        Tablero conCaballoEnB1 = TableroFake.desde("r.......", "........", "........", "........",
                "........", "........", "........", "TC..R..T");
        Tablero conCaballoEnG1 = TableroFake.desde("r.......", "........", "........", "........",
                "........", "........", "........", "T...R.CT");

        // Act + Assert
        assertEquals(posiciones("g1"), destinos(conCaballoEnB1, "e1"));
        assertEquals(posiciones("c1"), destinos(conCaballoEnG1, "e1"));
    }

    @Test
    void enJaqueNoSePuedeEnrocar() {
        // Act + Assert: la torre negra de e8 da jaque
        assertTrue(destinos(listoParaEnrocar("r...t..."), "e1").isEmpty());
    }

    @Test
    void elReyNoPuedePasarPorUnaCasillaAtacada() {
        // Act + Assert: la torre negra de f8 controla f1, por donde pasa el enroque corto
        assertEquals(posiciones("c1"), destinos(listoParaEnrocar("r....t.."), "e1"));
    }

    @Test
    void elReyNoPuedeTerminarEnUnaCasillaAtacada() {
        // Act + Assert: la torre negra de g8 controla g1
        assertEquals(posiciones("c1"), destinos(listoParaEnrocar("r.....t."), "e1"));
    }

    @Test
    void queB1EsteAtacadaNoImpideElEnroqueLargo() {
        // Act + Assert: el rey no pasa por b1; solo tiene que estar vacía
        assertEquals(posiciones("g1", "c1"), destinos(listoParaEnrocar("rt......"), "e1"));
    }

    @Test
    void soloSeEnrocaConUnaTorrePropia() {
        // Arrange: en h1 hay un alfil propio
        Tablero tablero = TableroFake.desde("r.......", "........", "........", "........",
                "........", "........", "........", "T...R..A");

        // Act + Assert
        assertEquals(posiciones("c1"), destinos(tablero, "e1"));
    }

    @Test
    void conLaTorreDemasiadoCercaNoHayEnroque() {
        // Arrange: la torre en g1 deja una sola casilla libre entre ambos
        Tablero tablero = TableroFake.desde("r.......", "........", "........", "........",
                "........", "........", "........", "T...R.T.");

        // Act + Assert
        assertEquals(posiciones("c1"), destinos(tablero, "e1"));
    }

    @Test
    void fueraDeLaFilaBaseNoHayEnroque() {
        // Arrange: rey y torre en la fila 4
        Tablero tablero = TableroFake.desde("r.......", "........", "........", "........",
                "....R..T", "........", "........", "........");

        // Act + Assert
        assertTrue(destinos(tablero, "e4").isEmpty());
    }

    @Test
    void soloElReyEnroca() {
        // Act + Assert: pedir enroques desde la torre o desde una casilla vacía no devuelve nada
        Tablero tablero = listoParaEnrocar("r.......");
        assertTrue(destinos(tablero, "h1").isEmpty());
        assertTrue(destinos(tablero, "d4").isEmpty());
    }
}
