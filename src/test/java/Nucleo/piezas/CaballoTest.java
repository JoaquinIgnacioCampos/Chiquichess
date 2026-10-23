package Nucleo.piezas;

import Nucleo.juego.DisposicionEstandar;
import Nucleo.juego.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class CaballoTest {

    @Test
    void enElCentroTiene8Saltos() {
        // Arrange
        Caballo caballo = new Caballo(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", caballo);

        // Act
        Set<Posicion> destinos = caballo.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertEquals(8, destinos.size());
    }

    @Test
    void enLaEsquinaTiene2Saltos() {
        // Arrange
        Caballo caballo = new Caballo(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("a1", caballo);

        // Act
        Set<Posicion> destinos = caballo.destinosPosibles(tablero, pos("a1"));

        // Assert
        assertEquals(posiciones("b3", "c2"), destinos);
    }

    @Test
    void enLaPosicionInicialSaltaPorEncimaDeLosPeones() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();
        Pieza caballo = tablero.piezaEn(pos("b1")).orElseThrow();

        // Act
        Set<Posicion> destinos = caballo.destinosPosibles(tablero, pos("b1"));

        // Assert
        assertEquals(posiciones("a3", "c3"), destinos);
    }

    @Test
    void seIdentificaComoCaballo() {
        // Arrange
        Caballo caballo = new Caballo(Color.BLANCO);

        // Assert
        assertEquals('C', caballo.codigo());
        assertEquals("Caballo", caballo.nombre());
        assertFalse(caballo.esRey());
    }
}
