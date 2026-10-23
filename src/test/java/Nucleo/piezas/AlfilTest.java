package Nucleo.piezas;

import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class AlfilTest {

    @Test
    void enD4DeUnTableroVacioTiene13Destinos() {
        // Arrange
        Alfil alfil = new Alfil(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", alfil);

        // Act
        Set<Posicion> destinos = alfil.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertEquals(13, destinos.size());
    }

    @Test
    void soloSeMueveEnDiagonalYNuncaCambiaDeColorDeCasilla() {
        // Arrange
        Alfil alfil = new Alfil(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", alfil);
        int colorDeCasillaOrigen = (3 + 3) % 2;

        // Act
        Set<Posicion> destinos = alfil.destinosPosibles(tablero, pos("d4"));

        // Assert
        for (Posicion destino : destinos) {
            assertEquals(Math.abs(destino.columna() - 3), Math.abs(destino.fila() - 3));
            assertEquals(colorDeCasillaOrigen, (destino.columna() + destino.fila()) % 2);
        }
    }

    @Test
    void lasPiezasPropiasLoBloqueanYLasRivalesSePuedenCapturar() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                ".....p..",
                "........",
                "...A....",
                "..P.....",
                "........",
                "........");
        Pieza alfil = tablero.piezaEn(pos("d4")).orElseThrow();

        // Act
        Set<Posicion> destinos = alfil.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertTrue(destinos.contains(pos("f6")), "Captura al peón rival");
        assertFalse(destinos.contains(pos("g7")), "No salta al peón rival");
        assertFalse(destinos.contains(pos("c3")), "No cae sobre su propio peón");
        assertFalse(destinos.contains(pos("b2")), "No salta a su propio peón");
    }

    @Test
    void enLaPosicionInicialNoPuedeMoverse() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        Pieza alfil = tablero.piezaEn(pos("c1")).orElseThrow();

        // Act
        Set<Posicion> destinos = alfil.destinosPosibles(tablero, pos("c1"));

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void seIdentificaComoAlfil() {
        // Arrange
        Alfil alfil = new Alfil(Color.BLANCO);

        // Assert
        assertEquals('A', alfil.codigo());
        assertEquals("Alfil", alfil.nombre());
        assertFalse(alfil.esRey());
        assertFalse(alfil.esPeon());
    }
}
