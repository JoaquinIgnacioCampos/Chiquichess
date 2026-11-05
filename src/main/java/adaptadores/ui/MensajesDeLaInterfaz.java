package adaptadores.ui;

import Nucleo.juego.AccionInvalidaException;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.reglas.MotivoFinalizacion;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Catálogo de los textos que la interfaz muestra para los códigos del núcleo (rechazos y finales).
 *
 * El núcleo informa con códigos y cada adaptador elige sus palabras, por eso los textos viven acá y
 * no en el núcleo. Juntarlos en un solo lugar evita que cada vista tenga su propio switch: agregar
 * un código nuevo es sumar una línea en este catálogo, sin tocar ninguna vista. Si falta un texto se
 * muestra {@link #SIN_TEXTO}; MensajesDeLaInterfazTest verifica que no falte ninguno.
 */
public final class MensajesDeLaInterfaz {

    /** Lo que se intentó hacer, para los rechazos cuyo texto depende de la acción. */
    public enum Accion { RENDIRSE, TABLAS, DESHACER }

    static final String SIN_TEXTO = "No se pudo completar la acción.";

    private final Map<MovimientoInvalidoException.Motivo, String> movimientos =
            new EnumMap<>(MovimientoInvalidoException.Motivo.class);
    private final Map<AccionInvalidaException.Motivo, String> acciones =
            new EnumMap<>(AccionInvalidaException.Motivo.class);
    private final Map<Accion, Map<AccionInvalidaException.Motivo, String>> porAccion = new EnumMap<>(Accion.class);
    private final Map<MotivoFinalizacion, String> finales = new EnumMap<>(MotivoFinalizacion.class);

    public MensajesDeLaInterfaz() {
        movimientos.put(MovimientoInvalidoException.Motivo.SIN_PIEZA_EN_ORIGEN, "Ya no hay una pieza en la casilla seleccionada.");
        movimientos.put(MovimientoInvalidoException.Motivo.NO_ES_SU_TURNO, "Es el turno del otro jugador.");
        movimientos.put(MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO, "El núcleo rechazó ese movimiento.");
        movimientos.put(MovimientoInvalidoException.Motivo.DEJA_AL_REY_EN_JAQUE, "Esa jugada deja a tu rey en jaque.");
        movimientos.put(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA, "La partida ya terminó.");

        acciones.put(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA, "La partida ya terminó.");
        acciones.put(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, "Es el turno del otro jugador.");
        acciones.put(AccionInvalidaException.Motivo.OFERTA_PENDIENTE, "Ya hay una oferta de tablas pendiente.");
        acciones.put(AccionInvalidaException.Motivo.SIN_OFERTA, "La oferta de tablas ya no está disponible.");
        acciones.put(AccionInvalidaException.Motivo.ES_SU_PROPIA_OFERTA, "Solo el rival puede responder a esta oferta.");
        acciones.put(AccionInvalidaException.Motivo.DESHACER_NO_PERMITIDO, "Esta partida no permite deshacer jugadas.");
        acciones.put(AccionInvalidaException.Motivo.NADA_PARA_DESHACER, "No hay jugadas para deshacer.");
        acciones.put(AccionInvalidaException.Motivo.NADA_PARA_REHACER, "No hay jugadas para rehacer.");

        porAccion(Accion.RENDIRSE).put(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, "Solo podés rendirte durante tu turno.");
        porAccion(Accion.TABLAS).put(AccionInvalidaException.Motivo.NO_ES_SU_TURNO, "Solo podés ofrecer tablas durante tu turno.");

        finales.put(MotivoFinalizacion.JAQUE_MATE, "Por jaque mate");
        finales.put(MotivoFinalizacion.AHOGADO, "Por ahogado");
        finales.put(MotivoFinalizacion.REPETICION, "Por repetición");
        finales.put(MotivoFinalizacion.REGLA_50_MOVIMIENTOS, "Regla de los 50 movimientos");
        finales.put(MotivoFinalizacion.RENDICION, "Por rendición");
        finales.put(MotivoFinalizacion.TIEMPO_AGOTADO, "Por tiempo agotado");
        finales.put(MotivoFinalizacion.ACUERDO, "Por acuerdo");
    }

    private Map<AccionInvalidaException.Motivo, String> porAccion(Accion accion) {
        return porAccion.computeIfAbsent(accion, a -> new EnumMap<>(AccionInvalidaException.Motivo.class));
    }

    public String rechazoDeMovimiento(MovimientoInvalidoException.Motivo motivo) {
        return movimientos.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
    }

    /** Texto específico de la acción si lo hay; si no, el texto general del motivo. */
    public String rechazoDeAccion(Accion accion, AccionInvalidaException.Motivo motivo) {
        Objects.requireNonNull(motivo, "motivo");
        String especifico = porAccion.getOrDefault(Objects.requireNonNull(accion, "accion"), Map.of()).get(motivo);
        return especifico != null ? especifico : acciones.getOrDefault(motivo, SIN_TEXTO);
    }

    public String motivoDeFinal(MotivoFinalizacion motivo) {
        return finales.getOrDefault(Objects.requireNonNull(motivo, "motivo"), SIN_TEXTO);
    }
}
