package Nucleo.juego;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PosicionTest {

    @Test
    void desplazadaSumaLosDeltasAColumnaYFila() {
        // Arrange
        Posicion origen = new Posicion(3, 3);

        // Act
        Posicion destino = origen.desplazada(1, -2);

        // Assert
        assertEquals(new Posicion(4, 1), destino);
    }

    @Test
    void desplazadaNoModificaLaPosicionOriginal() {
        // Arrange
        Posicion origen = new Posicion(3, 3);

        // Act
        origen.desplazada(2, 2);

        // Assert
        assertEquals(3, origen.columna());
        assertEquals(3, origen.fila());
    }

    @Test
    void desplazadaPuedeSalirDelTableroPorqueNoValidaLimites() {
        // Arrange
        Posicion esquina = new Posicion(0, 0);

        // Act
        Posicion afuera = esquina.desplazada(-1, -1);

        // Assert
        assertEquals(new Posicion(-1, -1), afuera);
    }

    @Test
    void dosPosicionesConLasMismasCoordenadasSonIgualesYSirvenComoClaveDeMap() {
        // Arrange
        Map<Posicion, String> mapa = new HashMap<>();
        mapa.put(new Posicion(4, 1), "peon");

        // Act
        String encontrado = mapa.get(new Posicion(4, 1));

        // Assert
        assertEquals("peon", encontrado);
        assertEquals(new Posicion(4, 1).hashCode(), new Posicion(4, 1).hashCode());
    }
}
