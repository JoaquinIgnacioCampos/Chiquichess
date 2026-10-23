package Nucleo.piezas;

import Nucleo.juego.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class ReinaTest {

    @Test
    void enD4DeUnTableroVacioTiene27Destinos() {
        // Arrange
        Reina reina = new Reina(Color.BLANCO);
        Tablero tablero = TableroFake.conUnaPieza("d4", reina);

        // Act
        Set<Posicion> destinos = reina.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertEquals(27, destinos.size());
    }

    @Test
    void seMueveComoTorreMasAlfil() {
        // Arrange
        Tablero tablero = TableroFake.vacio();
        Set<Posicion> torreMasAlfil = new HashSet<>();
        torreMasAlfil.addAll(new Torre(Color.BLANCO).destinosPosibles(tablero, pos("c3")));
        torreMasAlfil.addAll(new Alfil(Color.BLANCO).destinosPosibles(tablero, pos("c3")));

        // Act
        Set<Posicion> destinosReina = new Reina(Color.BLANCO).destinosPosibles(tablero, pos("c3"));

        // Assert
        assertEquals(torreMasAlfil, destinosReina);
    }

    @Test
    void lasPiezasLaBloqueanEnCadaDireccion() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "...p....",
                "...DP...",
                "..P.....",
                "........",
                "........");
        Pieza reina = tablero.piezaEn(pos("d4")).orElseThrow();

        // Act
        Set<Posicion> destinos = reina.destinosPosibles(tablero, pos("d4"));

        // Assert
        assertTrue(destinos.contains(pos("d5")), "Captura hacia arriba");
        assertFalse(destinos.contains(pos("d6")));
        assertFalse(destinos.contains(pos("e4")), "Bloqueada a la derecha por pieza propia");
        assertFalse(destinos.contains(pos("c3")), "Bloqueada en diagonal por pieza propia");
    }

    @Test
    void seIdentificaComoDama() {
        // Arrange
        Reina reina = new Reina(Color.BLANCO);

        // Assert
        assertEquals('D', reina.codigo());
        assertEquals("Reina", reina.nombre());
        assertFalse(reina.esRey());
    }
}
