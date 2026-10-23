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

class DeslizamientoTest {

    private static final int DERECHA = 1;

    @Test
    void rechazaLaDireccionNula() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Deslizamiento(0, 0, 1));
    }

    @Test
    void rechazaMenosDeUnPaso() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Deslizamiento(1, 0, 0));
    }

    @Test
    void sinLimiteAvanzaHastaElBordeEnUnTableroVacio() {
        // Arrange
        Deslizamiento haciaLaDerecha = new Deslizamiento(DERECHA, 0, Deslizamiento.SIN_LIMITE);

        // Act
        Set<Posicion> destinos = haciaLaDerecha.destinosPosibles(TableroFake.vacio(), pos("a1"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("b1", "c1", "d1", "e1", "f1", "g1", "h1"), destinos);
    }

    @Test
    void respetaLaCantidadMaximaDePasos() {
        // Arrange
        Deslizamiento dosPasos = new Deslizamiento(DERECHA, 0, 2);

        // Act
        Set<Posicion> destinos = dosPasos.destinosPosibles(TableroFake.vacio(), pos("a1"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("b1", "c1"), destinos);
    }

    @Test
    void seFrenaAntesDeUnaPiezaPropia() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...P...");
        Deslizamiento hacia = new Deslizamiento(DERECHA, 0, Deslizamiento.SIN_LIMITE);

        // Act
        Set<Posicion> destinos = hacia.destinosPosibles(tablero, pos("a1"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("b1", "c1", "d1"), destinos);
    }

    @Test
    void incluyeLaPiezaRivalComoCapturaYSeFrenaAhi() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T...p.p.");
        Deslizamiento hacia = new Deslizamiento(DERECHA, 0, Deslizamiento.SIN_LIMITE);

        // Act
        Set<Posicion> destinos = hacia.destinosPosibles(tablero, pos("a1"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("b1", "c1", "d1", "e1"), destinos);
    }

    @Test
    void desdeElBordeHaciaAfueraNoTieneDestinos() {
        // Arrange
        Deslizamiento haciaLaIzquierda = new Deslizamiento(-1, 0, Deslizamiento.SIN_LIMITE);

        // Act
        Set<Posicion> destinos = haciaLaIzquierda.destinosPosibles(TableroFake.vacio(), pos("a1"), Color.BLANCO);

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void funcionaEnDiagonal() {
        // Arrange
        Deslizamiento diagonal = new Deslizamiento(1, 1, Deslizamiento.SIN_LIMITE);

        // Act
        Set<Posicion> destinos = diagonal.destinosPosibles(TableroFake.vacio(), pos("e5"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("f6", "g7", "h8"), destinos);
    }

    @Test
    void lasCasillasAtacadasCoincidenConLosDestinos() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "T..p....");
        Deslizamiento hacia = new Deslizamiento(DERECHA, 0, Deslizamiento.SIN_LIMITE);

        // Act
        Set<Posicion> destinos = hacia.destinosPosibles(tablero, pos("a1"), Color.BLANCO);
        Set<Posicion> atacadas = hacia.casillasAtacadas(tablero, pos("a1"), Color.BLANCO);

        // Assert
        assertEquals(destinos, atacadas);
    }
}
