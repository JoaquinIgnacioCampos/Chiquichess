package Nucleo.movimientos.peon;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import dobles.MockReglaMovimiento;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dobles.Casillas.pos;
import static dobles.Casillas.posiciones;
import static org.junit.jupiter.api.Assertions.*;

/** Se usa un mock de la regla interna para verificar si SoloSi la llama o no. */
class SoloSiTest {

    @Test
    void siLaCondicionSeCumpleDelegaEnLaReglaInterna() {
        // Arrange
        MockReglaMovimiento interna = new MockReglaMovimiento(posiciones("e4"));
        SoloSi regla = new SoloSi((tablero, origen, color) -> true, interna);

        // Act
        Set<Posicion> destinos = regla.destinosPosibles(TableroFake.vacio(), pos("e2"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("e4"), destinos);
        assertEquals(1, interna.llamadasDestinos());
    }

    @Test
    void siLaCondicionNoSeCumpleNoTieneDestinosNiConsultaLaReglaInterna() {
        // Arrange
        MockReglaMovimiento interna = new MockReglaMovimiento(posiciones("e4"));
        SoloSi regla = new SoloSi((tablero, origen, color) -> false, interna);

        // Act
        Set<Posicion> destinos = regla.destinosPosibles(TableroFake.vacio(), pos("e3"), Color.BLANCO);
        Set<Posicion> atacadas = regla.casillasAtacadas(TableroFake.vacio(), pos("e3"), Color.BLANCO);

        // Assert
        assertTrue(destinos.isEmpty());
        assertTrue(atacadas.isEmpty());
        assertEquals(0, interna.llamadasDestinos());
        assertEquals(0, interna.llamadasAtacadas());
    }

    @Test
    void laCondicionRecibeElOrigenYElColor() {
        // Arrange: la condición solo se cumple para peones negros en la fila 7
        SoloSi regla = new SoloSi(
                (tablero, origen, color) -> color == Color.NEGRO && origen.fila() == 6,
                new MockReglaMovimiento(posiciones("e5")));

        // Act
        Set<Posicion> negroEnFila7 = regla.destinosPosibles(TableroFake.vacio(), pos("e7"), Color.NEGRO);
        Set<Posicion> blancoEnFila7 = regla.destinosPosibles(TableroFake.vacio(), pos("e7"), Color.BLANCO);

        // Assert
        assertEquals(posiciones("e5"), negroEnFila7);
        assertTrue(blancoEnFila7.isEmpty());
    }
}
