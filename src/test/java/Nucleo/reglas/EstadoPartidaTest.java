package Nucleo.reglas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoPartidaTest {

    @Test
    void enCursoYJaqueNoTerminanLaPartida() {
        // Assert
        assertFalse(EstadoPartida.EN_CURSO.estaFinalizada());
        assertFalse(EstadoPartida.JAQUE.estaFinalizada());
        assertFalse(EstadoPartida.EN_CURSO.esTablas());
        assertFalse(EstadoPartida.JAQUE.esTablas());
    }

    @Test
    void jaqueMateTerminaLaPartidaPeroNoEsTablas() {
        // Assert
        assertTrue(EstadoPartida.JAQUE_MATE.estaFinalizada());
        assertFalse(EstadoPartida.JAQUE_MATE.esTablas());
    }

    @Test
    void todasLasTablasTerminanLaPartida() {
        // Assert
        for (EstadoPartida estado : new EstadoPartida[]{
                EstadoPartida.TABLAS_AHOGADO,
                EstadoPartida.TABLAS_REPETICION,
                EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS}) {
            assertTrue(estado.estaFinalizada(), estado.name());
            assertTrue(estado.esTablas(), estado.name());
        }
    }
}
