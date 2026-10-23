package Nucleo.reglas;

import Nucleo.piezas.Color;
import Nucleo.tablero.Tablero;
import dobles.Contextos;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TablasPorRepeticionTest {

    private final Tablero posicionA = TableroFake.desde(
            "....r...",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
            "....R...");
    private final Tablero posicionB = TableroFake.desde(
            "...r....",
            "........",
            "........",
            "........",
            "........",
            "........",
            "........",
            "....R...");

    @Test
    void rechazaMenosDeDosRepeticiones() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new TablasPorRepeticion(1));
    }

    @Test
    void laTerceraVezQueAparecePosicionEsTablas() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion();

        // Act
        Optional<EstadoPartida> primera = regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));
        Optional<EstadoPartida> segunda = regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));
        Optional<EstadoPartida> tercera = regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));

        // Assert
        assertTrue(primera.isEmpty());
        assertTrue(segunda.isEmpty());
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), tercera);
    }

    @Test
    void reconoceLaMismaPosicionAunqueSeanTablerosDistintos() {
        // Arrange: dos objetos Tablero distintos pero con las mismas piezas en las mismas casillas
        TablasPorRepeticion regla = new TablasPorRepeticion(2);
        Tablero copia = posicionA.copiar();

        // Act
        regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));
        Optional<EstadoPartida> resultado = regla.evaluar(Contextos.inicial(copia, Color.BLANCO));

        // Assert
        assertEquals(Optional.of(EstadoPartida.TABLAS_REPETICION), resultado);
    }

    @Test
    void conTurnoDistintoNoEsLaMismaPosicion() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion(2);

        // Act
        regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));
        Optional<EstadoPartida> resultado = regla.evaluar(Contextos.inicial(posicionA, Color.NEGRO));

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    void lasPosicionesDistintasNoSeAcumulan() {
        // Arrange
        TablasPorRepeticion regla = new TablasPorRepeticion();

        // Act
        regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));
        regla.evaluar(Contextos.inicial(posicionB, Color.BLANCO));
        regla.evaluar(Contextos.inicial(posicionA, Color.BLANCO));
        Optional<EstadoPartida> resultado = regla.evaluar(Contextos.inicial(posicionB, Color.BLANCO));

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
