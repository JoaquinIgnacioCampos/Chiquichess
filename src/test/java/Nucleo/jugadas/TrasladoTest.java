package Nucleo.jugadas;

import Nucleo.bando.Bando;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Torre;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class TrasladoTest {

    private final Pieza torre = new Torre(Bando.BLANCO);

    @Test
    void aplicarLlevaLaPiezaAlDestino() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("a1", torre);
        Traslado traslado = new Traslado(torre, pos("a1"), pos("a5"));

        // Act
        traslado.aplicar(tablero);

        // Assert
        assertTrue(tablero.piezaEn(pos("a1")).isEmpty());
        assertSame(torre, tablero.piezaEn(pos("a5")).orElseThrow());
    }

    @Test
    void revertirLaDevuelveAlOrigen() {
        // Arrange
        Tablero tablero = TableroFake.conUnaPieza("a1", torre);
        Map<Posicion, Pieza> antes = new HashMap<>(tablero.piezas());
        Traslado traslado = new Traslado(torre, pos("a1"), pos("a5"));
        traslado.aplicar(tablero);

        // Act
        traslado.revertir(tablero);

        // Assert
        assertEquals(antes, tablero.piezas());
    }

    @Test
    void siLaPiezaNoEstaEnElOrigenNoTocaElTablero() {
        // Arrange: en a1 hay otra torre, no la del traslado
        Tablero tablero = TableroFake.conUnaPieza("a1", new Torre(Bando.BLANCO));
        Map<Posicion, Pieza> antes = new HashMap<>(tablero.piezas());

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new Traslado(torre, pos("a1"), pos("a5")).aplicar(tablero));
        assertEquals(antes, tablero.piezas());
    }

    @Test
    void siElDestinoEstaOcupadoNoTocaElTablero() {
        // Arrange: las capturas se modelan con un Retiro previo, no pisando la pieza
        Tablero tablero = TableroFake.conUnaPieza("a1", torre);
        tablero.colocar(pos("a5"), new Torre(Bando.NEGRO));
        Map<Posicion, Pieza> antes = new HashMap<>(tablero.piezas());

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> new Traslado(torre, pos("a1"), pos("a5")).aplicar(tablero));
        assertEquals(antes, tablero.piezas());
    }

    @Test
    void rechazaOrigenIgualAlDestino() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Traslado(torre, pos("a1"), pos("a1")));
    }
}
