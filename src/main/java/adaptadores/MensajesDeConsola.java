package adaptadores;

import Nucleo.juego.AccionInvalidaException;
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
    private final Map<AccionInvalidaException.Motivo, String> acciones =
            new EnumMap<>(AccionInvalidaException.Motivo.class);
    /** Finales; "%s" se reemplaza por el nombre del ganador. */
    private final Map<MotivoFinalizacion, String> finales = new EnumMap<>(MotivoFinalizacion.class);

    public MensajesDeConsola() {
        acciones.put(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA, "La partida ya termino.");
        acciones.put(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, "No es su turno.");
        acciones.put(AccionInvalidaException.Motivo.OFERTA_PENDIENTE, "Ya hay una oferta de tablas pendiente.");
        acciones.put(AccionInvalidaException.Motivo.SIN_OFERTA, "No hay ninguna oferta de tablas.");
        acciones.put(AccionInvalidaException.Motivo.ES_SU_PROPIA_OFERTA, "Solo el rival puede responder la oferta.");
        acciones.put(AccionInvalidaException.Motivo.DESHACER_NO_PERMITIDO, "Esta partida no permite deshacer jugadas.");
        acciones.put(AccionInvalidaException.Motivo.NADA_PARA_DESHACER, "No hay jugadas para deshacer.");
        acciones.put(AccionInvalidaException.Motivo.NADA_PARA_REHACER, "No hay jugadas para rehacer.");

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
        finales.put(MotivoFinalizacion.GOLES, "Gana %s por goles.");
        finales.put(MotivoFinalizacion.EMPATE_EN_GOLES, "Empate: se llego al limite de jugadas con los goles iguales.");
    }

    public String rechazoDeMovimiento(MovimientoInvalidoException.Motivo motivo) {
        return movimientos.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
    }

    public String rechazoDeAccion(AccionInvalidaException.Motivo motivo) {
        return acciones.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
    }

    /** Mensaje de fin de partida, con el nombre del ganador si lo hubo. */
    public String finDePartida(MotivoFinalizacion motivo, Optional<String> ganador) {
        String plantilla = finales.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
        return String.format(plantilla, ganador.orElse("?"));
    }
}
