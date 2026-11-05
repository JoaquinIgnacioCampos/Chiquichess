package adaptadores;

import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.reglas.MotivoFinalizacion;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Si el núcleo suma un código nuevo y nadie le escribe un texto para la consola, estos tests fallan. */
class MensajesDeConsolaTest {

    private final MensajesDeConsola mensajes = new MensajesDeConsola();

    @Test
    void todoRechazoDeMovimientoTieneSuTexto() {
        for (MovimientoInvalidoException.Motivo motivo : MovimientoInvalidoException.Motivo.values()) {
            assertNotEquals(MensajesDeConsola.SIN_TEXTO, mensajes.rechazoDeMovimiento(motivo),
                    "Falta el texto de " + motivo);
        }
    }

    @Test
    void todoRechazoDeAccionTieneSuTexto() {
        for (Nucleo.juego.AccionInvalidaException.Motivo motivo : Nucleo.juego.AccionInvalidaException.Motivo.values()) {
            assertNotEquals(MensajesDeConsola.SIN_TEXTO, mensajes.rechazoDeAccion(motivo),
                    "Falta el texto de " + motivo);
        }
    }

    @Test
    void todoMotivoDeFinalTieneSuTexto() {
        for (MotivoFinalizacion motivo : MotivoFinalizacion.values()) {
            assertFalse(mensajes.finDePartida(motivo, Optional.of("Ana")).startsWith(MensajesDeConsola.SIN_TEXTO),
                    "Falta el texto de " + motivo);
        }
    }

    @Test
    void losFinalesConGanadorIncluyenSuNombre() {
        // Assert
        assertEquals("Jaque mate! Gana Ana.", mensajes.finDePartida(MotivoFinalizacion.JAQUE_MATE, Optional.of("Ana")));
        assertEquals("Gana Ana por tiempo.", mensajes.finDePartida(MotivoFinalizacion.TIEMPO_AGOTADO, Optional.of("Ana")));
        assertEquals("Tablas por acuerdo.", mensajes.finDePartida(MotivoFinalizacion.ACUERDO, Optional.empty()));
    }
}
