package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class RetiroTest {

    private final Pieza peon = new Peon(Bando.NEGRO);

    @Test
    void aplicarSacaLaPiezaDelTablero() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("d5", peon);

        // Act
        new Retiro(peon, pos("d5")).aplicar(tablero);

        // Assert
        assertTrue(tablero.piezaEn(pos("d5")).isEmpty());
    }

    @Test
    void revertirDevuelveLaMismaPiezaASuCasilla() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("d5", peon);
        Retiro retiro = new Retiro(peon, pos("d5"));
        retiro.aplicar(tablero);

        // Act
        retiro.revertir(tablero);

        // Assert
        assertSame(peon, tablero.piezaEn(pos("d5")).orElseThrow());
    }

    @Test
    void siLaPiezaNoEstaAhiNoTocaElTablero() {
        // Arrange
        Tablero tablero = TableroFake.vacio();

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new Retiro(peon, pos("d5")).aplicar(tablero));
        assertTrue(tablero.piezas().isEmpty());
    }

    @Test
    void noPuedeDevolverlaSiLaCasillaQuedoOcupada() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("d5", new Peon(Bando.BLANCO));

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new Retiro(peon, pos("d5")).revertir(tablero));
    }
}
