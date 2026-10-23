package Nucleo.piezas;

import Nucleo.juego.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class TorreTest {

    @Test
    void enElCentroDeUnTableroVacioTiene14Destinos() {
        // Arrange
        Torre torre = new Torre(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", torre);

        // Act
        Set<Posicion> destinos = torre.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertEquals(14, destinos.size());
    }

    @Test
    void soloSeMueveEnFilasYColumnas() {
        // Arrange
        Torre torre = new Torre(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", torre);

        // Act
        Set<Posicion> destinos = torre.destinosPosibles(tablero, pos("d4"));

        // Assert
        for (Posicion destino : destinos) {
            assertTrue(destino.columna() == 3 || destino.fila() == 3, "Destino no ortogonal: " + destino);
        }
    }

    @Test
    void lasPiezasPropiasLaBloqueanYLasRivalesSePuedenCapturar() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "...P....",
                "........",
                ".p.T....",
                "........",
                "........",
                "........");
        Pieza torre = tablero.piezaEn(pos("d4")).orElseThrow();

        // Act
        Set<Posicion> destinos = torre.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertTrue(destinos.contains(pos("d5")));
        assertFalse(destinos.contains(pos("d6")), "No puede caer sobre su propio peón");
        assertFalse(destinos.contains(pos("d7")), "No puede saltar su propio peón");
        assertTrue(destinos.contains(pos("b4")), "Puede capturar el peón rival");
        assertFalse(destinos.contains(pos("a4")), "No puede saltar el peón rival");
    }

    @Test
    void enLaPosicionInicialNoPuedeMoverse() {
        // Arrange
        Tablero tablero = new Nucleo.juego.DisposicionEstandar().crearTablero();
        Pieza torre = tablero.piezaEn(pos("a1")).orElseThrow();

        // Act
        Set<Posicion> destinos = torre.destinosPosibles(tablero, pos("a1"));

        // Assert
        assertEquals(Set.of(), destinos);
    }

    @Test
    void seIdentificaComoTorre() {
        // Arrange
        Torre torre = new Torre(Color.NEGRO);

        // Assert
        assertEquals('T', torre.codigo());
        assertEquals("Torre", torre.nombre());
        assertEquals(Color.NEGRO, torre.color());
        assertFalse(torre.esRey());
        assertFalse(torre.esPeon());
    }

    @Test
    void lasCasillasAtacadasSonSusDestinos() {
        // Arrange
        Torre torre = new Torre(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("a1", torre);

        // Act
        Set<Posicion> atacadas = torre.casillasAtacadas(tablero, pos("a1"));

        // Assert
        assertEquals(torre.destinosPosibles(tablero, pos("a1")), atacadas);
        assertTrue(atacadas.containsAll(posiciones("a8", "h1")));
    }
}
