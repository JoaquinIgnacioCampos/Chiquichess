package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Peon;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Reina;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class ReemplazoTest {

    private final Pieza peon = new Peon(Bando.BLANCO);
    private final Pieza dama = new Reina(Bando.BLANCO);

    @Test
    void aplicarCambiaLaPiezaDeLaCasilla() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("e8", peon);

        // Act
        new Reemplazo(peon, dama, pos("e8")).aplicar(tablero);

        // Assert
        assertSame(dama, tablero.piezaEn(pos("e8")).orElseThrow());
        assertEquals(1, tablero.piezas().size());
    }

    @Test
    void revertirDevuelveLaPiezaAnterior() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("e8", peon);
        Reemplazo reemplazo = new Reemplazo(peon, dama, pos("e8"));
        reemplazo.aplicar(tablero);

        // Act
        reemplazo.revertir(tablero);

        // Assert
        assertSame(peon, tablero.piezaEn(pos("e8")).orElseThrow());
    }

    @Test
    void siNoEstaLaPiezaEsperadaNoTocaElTablero() {
        // Arrange: en e8 hay otro peón
        Tablero tablero = TableroFake.conUnaPieza("e8", new Peon(Bando.BLANCO));
        Pieza antes = tablero.piezaEn(pos("e8")).orElseThrow();

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> new Reemplazo(peon, dama, pos("e8")).aplicar(tablero));
        assertSame(antes, tablero.piezaEn(pos("e8")).orElseThrow());
        assertThrows(IllegalStateException.class, () -> new Reemplazo(peon, dama, pos("e8")).revertir(tablero));
    }
}
