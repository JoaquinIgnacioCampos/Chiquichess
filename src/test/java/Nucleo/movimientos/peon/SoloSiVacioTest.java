package Nucleo.movimientos.peon;

import Nucleo.bando.Bando;
import Nucleo.piezas.Torre;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import dobles.StubReglaMovimiento;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

class SoloSiVacioTest {

    @Test
    void descartaLosDestinosOcupadosPorCualquierPieza() {
        // Arrange
        Tablero tablero = TableroFake.vacio();
        tablero.colocar(pos("e5"), new Torre(Bando.NEGRO));
        tablero.colocar(pos("e6"), new Torre(Bando.BLANCO));
        SoloSiVacio regla = new SoloSiVacio(new StubReglaMovimiento(posiciones("e4", "e5", "e6")));

        // Act
        Set<Posicion> destinos = regla.destinosPosibles(tablero, pos("e3"), Bando.BLANCO);

        // Assert
        assertEquals(posiciones("e4"), destinos);
    }

    @Test
    void conTodasLasCasillasLibresDejaPasarTodo() {
        // Arrange
        SoloSiVacio regla = new SoloSiVacio(new StubReglaMovimiento(posiciones("e4", "e5")));

        // Act
        Set<Posicion> destinos = regla.destinosPosibles(TableroFake.vacio(), pos("e3"), Bando.BLANCO);

        // Assert
        assertEquals(posiciones("e4", "e5"), destinos);
    }

    @Test
    void noAtacaNingunaCasillaAunqueLaReglaInternaSi() {
        // Arrange
        SoloSiVacio regla = new SoloSiVacio(new StubReglaMovimiento(posiciones("e4"), posiciones("e4", "d4")));

        // Act
        Set<Posicion> atacadas = regla.casillasAtacadas(TableroFake.vacio(), pos("e3"), Bando.BLANCO);

        // Assert
        assertTrue(atacadas.isEmpty());
    }
}
