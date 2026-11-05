package adaptadores;

import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.reglas.MotivoFinalizacion;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Catálogo de los textos que la consola muestra para los códigos del núcleo (rechazos y finales).
 * La consola usa sus propias palabras (sin tildes), distintas de las de la interfaz gráfica.
 * Agregar un código nuevo es sumar una línea acá; MensajesDeConsolaTest verifica que no falte ninguno.
 */
public final class MensajesDeConsola {

    static final String SIN_TEXTO = "No se pudo completar la accion.";

    private final Map<MovimientoInvalidoException.Motivo, String> movimientos =
            new EnumMap<>(MovimientoInvalidoException.Motivo.class);
    /** Finales; "%s" se reemplaza por el nombre del ganador. */
    private final Map<MotivoFinalizacion, String> finales = new EnumMap<>(MotivoFinalizacion.class);

    public MensajesDeConsola() {
        movimientos.put(MovimientoInvalidoException.Motivo.SIN_PIEZA_EN_ORIGEN, "No hay ninguna pieza en la casilla de origen.");
        movimientos.put(MovimientoInvalidoException.Motivo.NO_ES_SU_TURNO, "Esa pieza no es suya: es el turno del otro jugador.");
        movimientos.put(MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO, "Esa pieza no puede moverse ahi.");
        movimientos.put(MovimientoInvalidoException.Motivo.DEJA_AL_REY_EN_JAQUE, "Movimiento ilegal: deja a su rey en jaque.");
        movimientos.put(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA, "La partida ya termino.");

        finales.put(MotivoFinalizacion.JAQUE_MATE, "Jaque mate! Gana %s.");
        finales.put(MotivoFinalizacion.AHOGADO, "Tablas por ahogado: el jugador no tiene jugadas pero no esta en jaque.");
        finales.put(MotivoFinalizacion.REPETICION, "Tablas por repeticion de posicion.");
        finales.put(MotivoFinalizacion.REGLA_50_MOVIMIENTOS, "Tablas por la regla de los 50 movimientos.");
        finales.put(MotivoFinalizacion.RENDICION, "Gana %s por rendicion.");
        finales.put(MotivoFinalizacion.TIEMPO_AGOTADO, "Gana %s por tiempo.");
        finales.put(MotivoFinalizacion.ACUERDO, "Tablas por acuerdo.");
    }

    public String rechazoDeMovimiento(MovimientoInvalidoException.Motivo motivo) {
        return movimientos.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
    }

    /** Mensaje de fin de partida, con el nombre del ganador si lo hubo. */
    public String finDePartida(MotivoFinalizacion motivo, Optional<String> ganador) {
        String plantilla = finales.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
        return String.format(plantilla, ganador.orElse("?"));
    }
}
