package Nucleo.piezas;

import Nucleo.juego.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class ReyTest {

    @Test
    void enElCentroSeMueveUnaCasillaEnLas8Direcciones() {
        // Arrange
        Rey rey = new Rey(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", rey);

        // Act
        Set<Posicion> destinos = rey.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertEquals(posiciones("c3", "c4", "c5", "d3", "d5", "e3", "e4", "e5"), destinos);
    }

    @Test
    void enLaEsquinaTiene3Destinos() {
        // Arrange
        Rey rey = new Rey(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("a1", rey);

        // Act
        Set<Posicion> destinos = rey.destinosPosibles(tablero, pos("a1"));

        // Assert
        assertEquals(posiciones("a2", "b1", "b2"), destinos);
    }

    @Test
    void noPuedeIrSobrePiezasPropiasPeroSiCapturarRivales() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "...p....",
                "...RP...",
                "........",
                "........",
                "........");
        Pieza rey = tablero.piezaEn(pos("d4")).orElseThrow();

        // Act
        Set<Posicion> destinos = rey.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertTrue(destinos.contains(pos("d5")));
        assertFalse(destinos.contains(pos("e4")));
    }

    @Test
    void esElUnicoQueSeIdentificaComoRey() {
        // Arrange
        Rey rey = new Rey(Color.NEGRO);

        // Assert
        assertTrue(rey.esRey());
        assertFalse(rey.esPeon());
        assertEquals('R', rey.codigo());
        assertEquals("Rey", rey.nombre());
    }
}
