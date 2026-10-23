package Nucleo.reglas;

import Nucleo.juego.DisposicionEstandar;
import Nucleo.piezas.Color;
import Nucleo.tablero.Tablero;
import dobles.TableroFake;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cada test arma (con el fake) una posición concreta que provoca o no un jaque. */
class DetectorDeJaqueTest {

    private final DetectorDeJaque detector = new DetectorDeJaque();

    @Test
    void enLaPosicionInicialNingunReyEstaEnJaque() {
        // Arrange
        Tablero tablero = new DisposicionEstandar().crearTablero();

        // Act + Assert
        assertFalse(detector.estaEnJaque(tablero, Color.BLANCO));
        assertFalse(detector.estaEnJaque(tablero, Color.NEGRO));
    }

    @Test
    void unaTorreEnLaMismaColumnaSinObstaculosDaJaque() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....t...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertTrue(enJaque);
    }

    @Test
    void unaPiezaEnElMedioBloqueaElJaque() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....t...",
                "........",
                "........",
                "....P...",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertFalse(enJaque);
    }

    @Test
    void unAlfilEnDiagonalDaJaque() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                ".a......",
                "........",
                "........",
                "....R...");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertTrue(enJaque);
    }

    @Test
    void unCaballoDaJaqueAunqueElReyEsteRodeado() {
        // Arrange: rey blanco en e2 rodeado de peones; el caballo negro en g3 salta hasta e2
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "...PPPc.",
                "...PRP..",
                "...PPP..");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertTrue(enJaque);
    }

    @Test
    void unCaballoQueNoLlegaAlReyNoDaJaque() {
        // Arrange: el caballo negro en h2 ataca f1, f3 y g4, pero no e2
        Tablero tablero = TableroFake.desde(
                "........",
                "........",
                "........",
                "........",
                "........",
                "...PPP..",
                "...PRP.c",
                "...PPP..");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertFalse(enJaque);
    }

    @Test
    void unPeonDaJaqueEnDiagonalPeroNoDeFrente() {
        // Arrange
        Tablero enDiagonal = TableroFake.desde(
                "........",
                "........",
                "........",
                "...p....",
                "....R...",
                "........",
                "........",
                "........");
        Tablero deFrente = TableroFake.desde(
                "........",
                "........",
                "........",
                "....p...",
                "....R...",
                "........",
                "........",
                "........");

        // Act + Assert
        assertTrue(detector.estaEnJaque(enDiagonal, Color.BLANCO));
        assertFalse(detector.estaEnJaque(deFrente, Color.BLANCO));
    }

    @Test
    void lasPiezasPropiasNoDanJaque() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....T...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "....R...");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertFalse(enJaque);
    }

    @Test
    void soloEstaEnJaqueElColorAtacado() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....r...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "R...T...");

        // Act + Assert
        assertTrue(detector.estaEnJaque(tablero, Color.NEGRO));
        assertFalse(detector.estaEnJaque(tablero, Color.BLANCO));
    }

    @Test
    void sinReyNoHayJaque() {
        // Arrange
        Tablero tablero = TableroFake.desde(
                "....t...",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........");

        // Act
        boolean enJaque = detector.estaEnJaque(tablero, Color.BLANCO);

        // Assert
        assertFalse(enJaque);
    }
}
