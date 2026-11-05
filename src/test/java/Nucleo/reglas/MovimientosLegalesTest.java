package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.juego.DisposicionEstandar;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import dobles.StubDetectorDeJaque;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class MovimientosLegalesTest {

    // ---------- Con stub del detector: se prueba solo el filtrado, sin posiciones reales ----------

    @Test
    void siElDetectorNuncaVeJaqueTodosLosDestinosDeLaPiezaSonLegales() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        MovimientosLegales legales = new MovimientosLegales(StubDetectorDeJaque.nuncaEnJaque());

        // Act
        Set<Posicion> destinos = legales.destinos(tablero, pos("g1"));

        // Assert
        assertEquals(posiciones("f3", "h3"), destinos);
    }

    @Test
    void siElDetectorSiempreVeJaqueNingunaJugadaEsLegal() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        MovimientosLegales legales = new MovimientosLegales(StubDetectorDeJaque.siempreEnJaque());

        // Act + Assert
        assertTrue(legales.destinos(tablero, pos("g1")).isEmpty());
        assertFalse(legales.existeAlguno(tablero, Bando.BLANCO));
    }

    @Test
    void sinPiezaEnElOrigenNoHayDestinos() {
        // Arrange
        MovimientosLegales legales = new MovimientosLegales(StubDetectorDeJaque.nuncaEnJaque());

        // Act
        Set<Posicion> destinos = legales.destinos(TableroFake.vacio(), pos("e4"));

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void rechazaUnDetectorNulo() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new MovimientosLegales(null));
    }

    // ---------- Con detector real y tableros armados con el fake ----------

    private final MovimientosLegales legales = new MovimientosLegales(new DetectorDeJaque());

    @Test
    void unaPiezaClavadaNoPuedeMoverseSiDejaAlReyEnJaque() {
        // Arrange: el alfil blanco en e2 tapa el jaque de la torre negra en e8
        Tablero tablero = TableroFake.desde(
                "....t...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....A...",
                "....R...");

        // Act
        Set<Posicion> destinos = legales.destinos(tablero, pos("e2"));

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void elReyNoPuedeEntrarEnUnaCasillaAtacada() {
        // Arrange: la torre negra en a2 controla toda la fila 2
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "t.......",
                "....R...");

        // Act
        Set<Posicion> destinos = legales.destinos(tablero, pos("e1"));

        // Assert
        assertEquals(posiciones("d1", "f1"), destinos);
    }

    @Test
    void enJaqueSoloSonLegalesLasJugadasQueLoResuelven() {
        // Arrange: torre negra en e8 da jaque; la torre blanca en a8 puede capturarla
        Tablero tablero = TableroFake.desde(
                "T...t...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        Set<Posicion> destinosTorre = legales.destinos(tablero, pos("a8"));

        // Assert
        assertEquals(posiciones("e8"), destinosTorre);
    }

    @Test
    void enLaPosicionInicialExistenJugadasLegales() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();

        // Act + Assert
        assertTrue(legales.existeAlguno(tablero, Bando.BLANCO));
        assertTrue(legales.existeAlguno(tablero, Bando.NEGRO));
    }

    @Test
    void enJaqueMateNoExisteNingunaJugadaLegal() {
        // Arrange: mate del pasillo, rey negro en h8
        Tablero tablero = TableroFake.desde(
                "T......r",
                "........",
                "......R.",
                "........",
                "........",
                "........",
                "........",
                "........");

        // Act
        boolean hayJugada = legales.existeAlguno(tablero, Bando.NEGRO);

        // Assert
        assertFalse(hayJugada);
    }

    @Test
    void dejaAlReyEnJaqueSimulaSinModificarElTableroReal() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....t...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....A...",
                "....R...");
        Map<Posicion, Pieza> antes = new HashMap<>(tablero.piezas());
        Pieza alfil = tablero.piezaEn(pos("e2")).orElseThrow();

        // Act
        boolean deja = legales.dejaAlReyEnJaque(tablero, alfil, pos("e2"), pos("d3"));

        // Assert
        assertTrue(deja);
        assertEquals(antes, tablero.piezas());
    }

    // ---------- Jugadas: la unidad que se valida y se ejecuta ----------

    @Test
    void lasJugadasPosiblesDelCaballoInicialSonDos() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();

        // Act
        List<Jugada> jugadas = legales.jugadasPosibles(tablero, pos("g1"));

        // Assert
        assertEquals(Set.of(pos("f3"), pos("h3")),
                jugadas.stream().map(Jugada::destino).collect(Collectors.toSet()));
        assertTrue(jugadas.stream().allMatch(j -> j.origen().equals(pos("g1"))));
    }

    @Test
    void laJugadaPosibleDeUnaCapturaIncluyeElRetiroDeLaPiezaRival() {
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

        // Act
        Jugada jugada = legales.jugadaPosible(tablero, pos("e4"), pos("d5")).orElseThrow();

        // Assert
        assertSame(tablero.piezaEn(pos("d5")).orElseThrow(), jugada.capturada().orElseThrow());
    }

    @Test
    void noHayJugadaPosibleHaciaUnDestinoQueLaPiezaNoAlcanza() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();

        // Act + Assert
        assertTrue(legales.jugadaPosible(tablero, pos("a1"), pos("a3")).isEmpty());
        assertTrue(legales.jugadaPosible(tablero, pos("e4"), pos("e5")).isEmpty());
    }

    @Test
    void dejaAlReyEnJaqueConUnaJugadaSimulaTodosSusEfectosSinTocarElTablero() {
        // Arrange: el alfil clavado en e2 no puede salir de la columna
        Tablero tablero = TableroFake.desde(
                "....t...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....A...",
                "....R...");
        Map<Posicion, Pieza> antes = new HashMap<>(tablero.piezas());
        Jugada salirDeLaClavada = Jugada.normal(tablero, pos("e2"), pos("d3"));

        // Act
        boolean deja = legales.dejaAlReyEnJaque(tablero, salirDeLaClavada);

        // Assert
        assertTrue(deja);
        assertEquals(antes, tablero.piezas());
    }

    // ---------- Reglas especiales ----------

    private Tablero listoParaEnrocar() {
        return TableroFake.desde(
                "r.......",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...R..T");
    }

    @Test
    void sinReglasEspecialesElReyNoEnroca() {
        // Act
        Set<Posicion> destinos = legales.destinos(listoParaEnrocar(), pos("e1"), List.of());

        // Assert
        assertFalse(destinos.contains(pos("g1")));
        assertFalse(destinos.contains(pos("c1")));
    }

    @Test
    void conElEnroqueInyectadoElReySumaSusDosEnroques() {
        // Arrange
        MovimientosLegales conEnroque = new MovimientosLegales(new DetectorDeJaque(), List.of(new Enroque()));

        // Act
        Set<Posicion> destinos = conEnroque.destinos(listoParaEnrocar(), pos("e1"), List.of());

        // Assert: los cinco movimientos comunes del rey más los dos enroques
        assertEquals(posiciones("d1", "d2", "e2", "f2", "f1", "g1", "c1"), destinos);
    }
}
