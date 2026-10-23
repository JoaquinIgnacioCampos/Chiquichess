package Nucleo.reglas;

import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;
import dobles.StubDetectorDeJaque;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        assertFalse(legales.existeAlguno(tablero, Color.BLANCO));
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
        assertTrue(legales.existeAlguno(tablero, Color.BLANCO));
        assertTrue(legales.existeAlguno(tablero, Color.NEGRO));
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
        boolean hayJugada = legales.existeAlguno(tablero, Color.NEGRO);

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
}
