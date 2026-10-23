package Nucleo.piezas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorTest {

    @Test
    void lasBlancasAvanzanHaciaArribaYLasNegrasHaciaAbajo() {
        // Assert
        assertEquals(1, Color.BLANCO.direccionAvance());
        assertEquals(-1, Color.NEGRO.direccionAvance());
    }

    @Test
    void elOpuestoDeCadaColorEsElOtro() {
        // Assert
        assertEquals(Color.NEGRO, Color.BLANCO.opuesto());
        assertEquals(Color.BLANCO, Color.NEGRO.opuesto());
    }

    @Test
    void elOpuestoDelOpuestoEsElMismoColor() {
        // Assert
        assertEquals(Color.BLANCO, Color.BLANCO.opuesto().opuesto());
    }
}
