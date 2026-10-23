package Nucleo.movimientos.peon;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Torre;
import Nucleo.tablero.Tablero;
import dobles.StubReglaMovimiento;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class SoloSiCapturaTest {

    @Test
    void soloDejaLosDestinosDondeHayUnaPieza() {
        // Arrange
        Tablero tablero = TableroFake.vacio();
        tablero.colocar(pos("d5"), new Torre(Color.NEGRO));
        SoloSiCaptura regla = new SoloSiCaptura(new StubReglaMovimiento(posiciones("d5", "f5")));

        // Act
        Set<Posicion> destinos = regla.destinosPosibles(tablero, pos("e4"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("d5"), destinos);
    }

    @Test
    void sinPiezasEnLosDestinosNoPuedeMoverse() {
        // Arrange
        SoloSiCaptura regla = new SoloSiCaptura(new StubReglaMovimiento(posiciones("d5", "f5")));

        // Act
        Set<Posicion> destinos = regla.destinosPosibles(TableroFake.vacio(), pos("e4"), Color.BLANCO);

        // Assert
        assertTrue(destinos.isEmpty());
    }

    @Test
    void atacaLasCasillasDeLaReglaInternaAunqueEstenVacias() {
        // Arrange
        SoloSiCaptura regla = new SoloSiCaptura(new StubReglaMovimiento(posiciones("d5", "f5")));

        // Act
        Set<Posicion> atacadas = regla.casillasAtacadas(TableroFake.vacio(), pos("e4"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("d5", "f5"), atacadas);
    }
}
