package Nucleo.tablero;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.piezas.Torre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TableroTest {

    private Tablero tablero;
    private final Pieza torreBlanca = new Torre(Color.BLANCO);
    private final Pieza torreNegra = new Torre(Color.NEGRO);

    @BeforeEach
    void prepararTablero() {
        tablero = new Tablero(8, 8);
    }

    @Test
    void rechazaDimensionesNoPositivas() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new Tablero(0, 8));
        assertThrows(IllegalArgumentException.class, () -> new Tablero(8, -1));
    }

    @Test
    void permiteTablerosQueNoSonDe8x8() {
        // Act
        Tablero chico = new Tablero(5, 6);

        // Assert
        assertEquals(5, chico.ancho());
        assertEquals(6, chico.alto());
    }

    @Test
    void contieneLasCasillasDentroDeLosLimites() {
        // Assert
        assertTrue(tablero.contiene(new Posicion(0, 0)));
        assertTrue(tablero.contiene(new Posicion(7, 7)));
        assertTrue(tablero.contiene(new Posicion(3, 5)));
    }

    @Test
    void noContieneLasCasillasFueraDeLosLimites() {
        // Assert
        assertFalse(tablero.contiene(new Posicion(8, 0)));
        assertFalse(tablero.contiene(new Posicion(0, 8)));
        assertFalse(tablero.contiene(new Posicion(-1, 3)));
        assertFalse(tablero.contiene(new Posicion(3, -1)));
    }

    @Test
    void unTableroNuevoEstaVacio() {
        // Assert
        assertTrue(tablero.piezas().isEmpty());
        assertTrue(tablero.piezaEn(new Posicion(4, 4)).isEmpty());
    }

    @Test
    void colocarDejaLaPiezaEnEsaCasilla() {
        // Act
        tablero.colocar(new Posicion(0, 0), torreBlanca);

        // Assert
        assertEquals(Optional.of(torreBlanca), tablero.piezaEn(new Posicion(0, 0)));
    }

    @Test
    void colocarSobreUnaCasillaOcupadaReemplazaLaPieza() {
        // Arrange
        tablero.colocar(new Posicion(0, 0), torreBlanca);

        // Act
        tablero.colocar(new Posicion(0, 0), torreNegra);

        // Assert
        assertEquals(Optional.of(torreNegra), tablero.piezaEn(new Posicion(0, 0)));
        assertEquals(1, tablero.piezas().size());
    }

    @Test
    void colocarFueraDelTableroLanzaExcepcion() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> tablero.colocar(new Posicion(8, 8), torreBlanca));
    }

    @Test
    void piezaEnUnaCasillaFueraDelTableroDevuelveVacio() {
        // Act
        Optional<Pieza> resultado = tablero.piezaEn(new Posicion(-5, 20));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void quitarDevuelveLaPiezaYDejaLaCasillaLibre() {
        // Arrange
        tablero.colocar(new Posicion(2, 2), torreBlanca);

        // Act
        Optional<Pieza> quitada = tablero.quitar(new Posicion(2, 2));

        // Assert
        assertEquals(Optional.of(torreBlanca), quitada);
        assertTrue(tablero.piezaEn(new Posicion(2, 2)).isEmpty());
    }

    @Test
    void quitarDeUnaCasillaVaciaDevuelveVacio() {
        // Act
        Optional<Pieza> quitada = tablero.quitar(new Posicion(2, 2));

        // Assert
        assertTrue(quitada.isEmpty());
    }

    @Test
    void elMapaDePiezasEsDeSoloLectura() {
        // Arrange
        tablero.colocar(new Posicion(0, 0), torreBlanca);

        // Act + Assert
        assertThrows(UnsupportedOperationException.class,
                () -> tablero.piezas().put(new Posicion(1, 1), torreNegra));
    }

    @Test
    void copiarDevuelveUnTableroIndependiente() {
        // Arrange
        tablero.colocar(new Posicion(0, 0), torreBlanca);

        // Act
        Tablero copia = tablero.copiar();
        copia.quitar(new Posicion(0, 0));
        copia.colocar(new Posicion(5, 5), torreNegra);

        // Assert
        assertEquals(Optional.of(torreBlanca), tablero.piezaEn(new Posicion(0, 0)));
        assertTrue(tablero.piezaEn(new Posicion(5, 5)).isEmpty());
        assertEquals(tablero.ancho(), copia.ancho());
        assertEquals(tablero.alto(), copia.alto());
    }
}
