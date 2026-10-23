package Nucleo.juego;

import Nucleo.piezas.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests de los value objects simples del paquete juego (Movimiento, Jugador, excepción). */
class MovimientoTest {

    @Test
    void unMovimientoNoAceptaPosicionesNulas() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new Movimiento(null, new Posicion(0, 0)));
        assertThrows(NullPointerException.class, () -> new Movimiento(new Posicion(0, 0), null));
    }

    @Test
    void dosMovimientosConElMismoOrigenYDestinoSonIguales() {
        // Assert
        assertEquals(new Movimiento(new Posicion(4, 1), new Posicion(4, 3)),
                new Movimiento(new Posicion(4, 1), new Posicion(4, 3)));
    }

    @Test
    void unJugadorNoAceptaNombreNiColorNulos() {
        // Act + Assert
        assertThrows(NullPointerException.class, () -> new Jugador(null, Color.BLANCO));
        assertThrows(NullPointerException.class, () -> new Jugador("Ana", null));
    }

    @Test
    void laExcepcionGuardaElMotivo() {
        // Act
        MovimientoInvalidoException e =
                new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.NO_ES_SU_TURNO);

        // Assert
        assertEquals(MovimientoInvalidoException.Motivo.NO_ES_SU_TURNO, e.motivo());
        assertEquals("NO_ES_SU_TURNO", e.getMessage());
    }
}
