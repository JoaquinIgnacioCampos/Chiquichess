package Nucleo.bando;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BandoTest {

    @Test
    void lasBlancasAvanzanHaciaArribaYLasNegrasHaciaAbajo() {
        // Assert
        assertEquals(1, Bando.BLANCO.direccionAvance());
        assertEquals(-1, Bando.NEGRO.direccionAvance());
    }

    @Test
    void elOpuestoDeCadaColorEsElOtro() {
        // Assert
        assertEquals(Bando.NEGRO, Bando.BLANCO.opuesto());
        assertEquals(Bando.BLANCO, Bando.NEGRO.opuesto());
    }

    @Test
    void elOpuestoDelOpuestoEsElMismoColor() {
        // Assert
        assertEquals(Bando.BLANCO, Bando.BLANCO.opuesto().opuesto());
    }
}
