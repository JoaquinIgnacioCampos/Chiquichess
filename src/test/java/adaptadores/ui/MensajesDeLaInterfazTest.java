package adaptadores.ui;

import Nucleo.juego.AccionInvalidaException;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.reglas.MotivoFinalizacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reemplaza la alarma que daba el switch exhaustivo: si el núcleo suma un código nuevo y nadie le
 * escribe un texto en el catálogo, estos tests fallan y dicen cuál falta.
 */
class MensajesDeLaInterfazTest {

    private final MensajesDeLaInterfaz mensajes = new MensajesDeLaInterfaz();

    @Test
    void todoRechazoDeMovimientoTieneSuTexto() {
        for (MovimientoInvalidoException.Motivo motivo : MovimientoInvalidoException.Motivo.values()) {
            assertNotEquals(MensajesDeLaInterfaz.SIN_TEXTO, mensajes.rechazoDeMovimiento(motivo),
                    "Falta el texto de " + motivo);
        }
    }

    @Test
    void todoRechazoDeAccionTieneSuTextoEnCadaAccion() {
        for (MensajesDeLaInterfaz.Accion accion : MensajesDeLaInterfaz.Accion.values()) {
            for (AccionInvalidaException.Motivo motivo : AccionInvalidaException.Motivo.values()) {
                assertNotEquals(MensajesDeLaInterfaz.SIN_TEXTO, mensajes.rechazoDeAccion(accion, motivo),
                        "Falta el texto de " + motivo + " al " + accion);
            }
        }
    }

    @Test
    void todoMotivoDeFinalTieneSuTexto() {
        for (MotivoFinalizacion motivo : MotivoFinalizacion.values()) {
            assertNotEquals(MensajesDeLaInterfaz.SIN_TEXTO, mensajes.motivoDeFinal(motivo), "Falta el texto de " + motivo);
        }
    }

    @Test
    void elFueraDeTurnoSeExplicaSegunLoQueSeIntento() {
        // Act
        String alRendirse = mensajes.rechazoDeAccion(MensajesDeLaInterfaz.Accion.RENDIRSE,
                AccionInvalidaException.Motivo.NO_ES_SU_TURNO);
        String alOfrecerTablas = mensajes.rechazoDeAccion(MensajesDeLaInterfaz.Accion.TABLAS,
                AccionInvalidaException.Motivo.NO_ES_SU_TURNO);

        // Assert
        assertEquals("Solo podés rendirte durante tu turno.", alRendirse);
        assertEquals("Solo podés ofrecer tablas durante tu turno.", alOfrecerTablas);
    }

    @Test
    void sinTextoEspecificoSeUsaElGeneralDelMotivo() {
        // Act
        String texto = mensajes.rechazoDeAccion(MensajesDeLaInterfaz.Accion.RENDIRSE,
                AccionInvalidaException.Motivo.PARTIDA_FINALIZADA);

        // Assert
        assertEquals("La partida ya terminó.", texto);
    }
}
