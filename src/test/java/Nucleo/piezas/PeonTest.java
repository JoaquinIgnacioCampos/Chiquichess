package Nucleo.piezas;

import Nucleo.juego.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class PeonTest {

    @Test
    void blancoEnSuFilaInicialPuedeAvanzarUnaODosCasillas() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("e2", peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e2"));

        // Assert
        assertEquals(posiciones("e3", "e4"), destinos);
    }

    @Test
    void blancoFueraDeSuFilaInicialSoloAvanzaUnaCasilla() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("e3", peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e3"));

        // Assert
        assertEquals(posiciones("e4"), destinos);
    }

    @Test
    void negroAvanzaHaciaFilasMenores() {
        // Arrange
        Peon peon = new Peon(Color.NEGRO);
        Tablero tablero = TableroFake.conUnaPieza("e7", peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e7"));

        // Assert
        assertEquals(posiciones("e6", "e5"), destinos);
    }

    @Test
    void negroFueraDeSuFilaInicialSoloAvanzaUnaCasilla() {
        // Arrange
        Peon peon = new Peon(Color.NEGRO);
        Tablero tablero = TableroFake.conUnaPieza("e6", peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e6"));

        // Assert
        assertEquals(posiciones("e5"), destinos);
    }

    @Test
    void noPuedeCapturarHaciaAdelante() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "....p...",
                "....P...",
                "........");
        Pieza peon = tablero.piezaEn(pos("e2")).orElseThrow();

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e2"));

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void elAvanceDobleNoSaltaUnaPiezaEnElMedio() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "....C...",
                "....P...",
                "........");
        Pieza peon = tablero.piezaEn(pos("e2")).orElseThrow();

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e2"));

        // Assert
        assertFalse(destinos.contains(pos("e4")));
    }

    @Test
    void conLaSegundaCasillaOcupadaSoloAvanzaUna() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "....p...",
                "........",
                "....P...",
                "........");
        Pieza peon = tablero.piezaEn(pos("e2")).orElseThrow();

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e2"));

        // Assert
        assertEquals(posiciones("e3"), destinos);
    }

    @Test
    void capturaPiezasRivalesEnDiagonal() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "...p.p..",
                "....P...",
                "........",
                "........",
                "........");
        Pieza peon = tablero.piezaEn(pos("e4")).orElseThrow();

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e4"));

        // Assert
        assertEquals(posiciones("d5", "e5", "f5"), destinos);
    }

    @Test
    void noCapturaPiezasPropiasEnDiagonal() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "...P.P..",
                "....P...",
                "........",
                "........",
                "........");
        Pieza peon = tablero.piezaEn(pos("e4")).orElseThrow();

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e4"));

        // Assert
        assertEquals(posiciones("e5"), destinos);
    }

    @Test
    void sinPiezasEnLasDiagonalesNoPuedeMoverseEnDiagonal() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("e4", peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e4"));

        // Assert
        assertFalse(destinos.contains(pos("d5")));
        assertFalse(destinos.contains(pos("f5")));
    }

    @Test
    void atacaSusDiagonalesAunqueEstenVaciasPeroNoLaCasillaDeAdelante() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("e4", peon);

        // Act
        Set<Posicion> atacadas = peon.casillasAtacadas(tablero, pos("e4"));

        // Assert
        assertEquals(posiciones("d5", "f5"), atacadas);
    }

    @Test
    void elPeonNegroAtacaHaciaAbajo() {
        // Arrange
        Peon peon = new Peon(Color.NEGRO);
        Tablero tablero = TableroFake.conUnaPieza("e5", peon);

        // Act
        Set<Posicion> atacadas = peon.casillasAtacadas(tablero, pos("e5"));

        // Assert
        assertEquals(posiciones("d4", "f4"), atacadas);
    }

    @Test
    void enElBordeSoloAtacaUnaDiagonal() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("a4", peon);

        // Act
        Set<Posicion> atacadas = peon.casillasAtacadas(tablero, pos("a4"));

        // Assert
        assertEquals(posiciones("b5"), atacadas);
    }

    @Test
    void enLaUltimaFilaNoTieneMovimientosPorqueLaPromocionNoEstaImplementada() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("e8", peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e8"));

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void enUnTableroMasAltoElNegroEmpiezaEnLaPenultimaFila() {
        // Arrange: tablero de 8 columnas x 10 filas, la fila inicial negra es la 9
        Peon peon = new Peon(Color.NEGRO);
        Tablero tablero = new Tablero(8, 10);
        tablero.colocar(pos("e9"), peon);

        // Act
        Set<Posicion> destinos = peon.destinosPosibles(tablero, pos("e9"));

        // Assert
        assertEquals(posiciones("e8", "e7"), destinos);
    }

    @Test
    void seIdentificaComoPeon() {
        // Arrange
        Peon peon = new Peon(Color.BLANCO);

        // Assert
        assertTrue(peon.esPeon());
        assertFalse(peon.esRey());
        assertEquals('P', peon.codigo());
        assertEquals("Peon", peon.nombre());
    }
}
