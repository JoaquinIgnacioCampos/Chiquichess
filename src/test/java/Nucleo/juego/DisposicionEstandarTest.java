package Nucleo.juego;

import Nucleo.piezas.*;
import Nucleo.tablero.Tablero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dobles.Casillas.pos;
import static org.junit.jupiter.api.Assertions.*;

class DisposicionEstandarTest {

    private Tablero tablero;

    @BeforeEach
    void prepararTablero() {
        tablero = new DisposicionEstandar().crearTablero();
    }

    @Test
    void creaUnTableroDe8x8Con32Piezas() {
        // Assert
        assertEquals(8, tablero.ancho());
        assertEquals(8, tablero.alto());
        assertEquals(32, tablero.piezas().size());
    }

    @Test
    void cadaColorTiene16Piezas() {
        // Act
        long blancas = tablero.piezas().values().stream().filter(p -> p.color() == Color.BLANCO).count();
        long negras = tablero.piezas().values().stream().filter(p -> p.color() == Color.NEGRO).count();

        // Assert
        assertEquals(16, blancas);
        assertEquals(16, negras);
    }

    @Test
    void losReyesYLasDamasEstanEnSusCasillas() {
        // Assert
        assertPieza("e1", Rey.class, Color.BLANCO);
        assertPieza("e8", Rey.class, Color.NEGRO);
        assertPieza("d1", Reina.class, Color.BLANCO);
        assertPieza("d8", Reina.class, Color.NEGRO);
    }

    @Test
    void laFilaBaseTieneTorreCaballoAlfilDamaReyAlfilCaballoTorre() {
        // Arrange
        Class<?>[] esperadas = {Torre.class, Caballo.class, Alfil.class, Reina.class,
                Rey.class, Alfil.class, Caballo.class, Torre.class};

        // Assert
        for (int columna = 0; columna < 8; columna++) {
            assertInstanceOf(esperadas[columna], tablero.piezaEn(new Posicion(columna, 0)).orElseThrow());
            assertInstanceOf(esperadas[columna], tablero.piezaEn(new Posicion(columna, 7)).orElseThrow());
        }
    }

    @Test
    void losPeonesOcupanLasFilas2Y7() {
        // Assert
        for (int columna = 0; columna < 8; columna++) {
            Pieza blanca = tablero.piezaEn(new Posicion(columna, 1)).orElseThrow();
            Pieza negra = tablero.piezaEn(new Posicion(columna, 6)).orElseThrow();
            assertTrue(blanca.esPeon());
            assertEquals(Color.BLANCO, blanca.color());
            assertTrue(negra.esPeon());
            assertEquals(Color.NEGRO, negra.color());
        }
    }

    @Test
    void lasFilasDel3Al6EstanVacias() {
        // Assert
        for (int fila = 2; fila <= 5; fila++) {
            for (int columna = 0; columna < 8; columna++) {
                assertTrue(tablero.piezaEn(new Posicion(columna, fila)).isEmpty());
            }
        }
    }

    @Test
    void cadaLlamadaCreaUnTableroNuevoEIndependiente() {
        // Act
        Tablero otro = new DisposicionEstandar().crearTablero();
        otro.quitar(pos("e2"));

        // Assert
        assertTrue(tablero.piezaEn(pos("e2")).isPresent());
    }

    private void assertPieza(String casilla, Class<? extends Pieza> tipo, Color color) {
        Pieza pieza = tablero.piezaEn(pos(casilla)).orElseThrow();
        assertInstanceOf(tipo, pieza);
        assertEquals(color, pieza.color());
    }
}
