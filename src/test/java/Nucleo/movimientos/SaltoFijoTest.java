package Nucleo.movimientos;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class SaltoFijoTest {

    private static final int[][] SALTOS_DE_CABALLO = {
            {1, 2}, {2, 1}, {-1, 2}, {-2, 1}, {1, -2}, {2, -1}, {-1, -2}, {-2, -1}};

    @Test
    void llegaATodosLosDesplazamientosEnUnTableroVacio() {
        // Arrange
        SaltoFijo salto = new SaltoFijo(SALTOS_DE_CABALLO);

        // Act
        Set<Posicion> destinos = salto.destinosPosibles(TableroFake.vacio(), pos("d4"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("e6", "f5", "c6", "b5", "e2", "f3", "c2", "b3"), destinos);
    }

    @Test
    void descartaLosDestinosFueraDelTablero() {
        // Arrange
        SaltoFijo salto = new SaltoFijo(SALTOS_DE_CABALLO);

        // Act
        Set<Posicion> destinos = salto.destinosPosibles(TableroFake.vacio(), pos("a1"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("b3", "c2"), destinos);
    }

    @Test
    void saltaPorEncimaDeLasPiezasQueLoRodean() {
        // Arrange: un caballo en d4 completamente rodeado de piezas
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "..ppp...",
                "..pCp...",
                "..ppp...",
                "........",
                "........");
        SaltoFijo salto = new SaltoFijo(SALTOS_DE_CABALLO);

        // Act
        Set<Posicion> destinos = salto.destinosPosibles(tablero, pos("d4"), Color.BLANCO);

        // Assert
        assertEquals(8, destinos.size());
    }

    @Test
    void noPuedeCaerSobreUnaPiezaPropiaPeroSiSobreUnaRival() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "..P.....",
                ".....p..",
                "...C....",
                "........",
                "........",
                "........");
        SaltoFijo salto = new SaltoFijo(SALTOS_DE_CABALLO);

        // Act
        Set<Posicion> destinos = salto.destinosPosibles(tablero, pos("d4"), Color.BLANCO);

        // Assert
        assertFalse(destinos.contains(pos("c6")));
        assertTrue(destinos.contains(pos("f5")));
    }

    @Test
    void copiaLosDesplazamientosParaQueNoLosModifiquenDesdeAfuera() {
        // Arrange
        int[][] desplazamientos = {{1, 2}};
        SaltoFijo salto = new SaltoFijo(desplazamientos);

        // Act
        desplazamientos[0][0] = 2;

        // Assert
        assertEquals(posiciones("e6"), salto.destinosPosibles(TableroFake.vacio(), pos("d4"), Color.BLANCO));
    }

    @Test
    void lasCasillasAtacadasCoincidenConLosDestinos() {
        // Arrange
        SaltoFijo salto = new SaltoFijo(SALTOS_DE_CABALLO);

        // Act
        Set<Posicion> destinos = salto.destinosPosibles(TableroFake.vacio(), pos("b1"), Color.BLANCO);
        Set<Posicion> atacadas = salto.casillasAtacadas(TableroFake.vacio(), pos("b1"), Color.BLANCO);

        // Assert
        assertEquals(destinos, atacadas);
    }
}
