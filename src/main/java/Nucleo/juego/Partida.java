package Nucleo.juego;

import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.*;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Tablero;

import java.util.*;

/**
 * Orquesta una partida: turnos, validación, captura y estado.
 * Recibe sus colaboradores por constructor (inyección de dependencias), por lo que
 * se puede testear con cualquier tablero armado a mano.
 */
public class Partida {

    private final Tablero tablero;
    private final Jugador jugadorBlanco;
    private final Jugador jugadorNegro;
    private final IDetectorDeJaque detector;
    private final MovimientosLegales movimientosLegales;
    private final List<CondicionDeFinalizacion> condiciones;

    private Color turno = Color.BLANCO;
    private EstadoPartida estado;

    /**
     * Constructor de uso habitual: arma los MovimientosLegales con el mismo detector.
     *
     * @param condiciones condiciones de fin de partida, evaluadas en orden (la primera que se cumple gana).
     *                    Conviene poner primero jaque mate y ahogado.
     */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, List<CondicionDeFinalizacion> condiciones) {
        this(tablero, jugadorBlanco, jugadorNegro, detector, new MovimientosLegales(detector), condiciones);
    }

    /**
     * Constructor con todas las dependencias inyectadas (lo usan los tests para reemplazar
     * MovimientosLegales por un doble). Se espera que movimientosLegales use el mismo detector.
     */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, MovimientosLegales movimientosLegales,
                   List<CondicionDeFinalizacion> condiciones) {
        this.tablero = Objects.requireNonNull(tablero, "tablero");
        this.jugadorBlanco = Objects.requireNonNull(jugadorBlanco, "jugadorBlanco");
        this.jugadorNegro = Objects.requireNonNull(jugadorNegro, "jugadorNegro");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.movimientosLegales = Objects.requireNonNull(movimientosLegales, "movimientosLegales");
        this.condiciones = List.copyOf(condiciones);
        if (jugadorBlanco.color() != Color.BLANCO || jugadorNegro.color() != Color.NEGRO) {
            throw new IllegalArgumentException("Los jugadores no coinciden con sus colores");
        }
        this.estado = evaluarEstado(Optional.empty(), false);
    }

    public ITablero tablero() {
        return tablero;
    }

    public Color turno() {
        return turno;
    }

    public Jugador jugadorActual() {
        return turno == Color.BLANCO ? jugadorBlanco : jugadorNegro;
    }

    public EstadoPartida estado() {
        return estado;
    }

    /** Quién ganó; vacío si la partida sigue o terminó en tablas. */
    public Optional<Jugador> ganador() {
        if (estado != EstadoPartida.JAQUE_MATE) {
            return Optional.empty();
        }
        // En jaque mate el turno es del jugador que perdió.
        return Optional.of(turno == Color.BLANCO ? jugadorNegro : jugadorBlanco);
    }

    /** Destinos permitidos para la pieza de esa casilla (vacío si no hay pieza, no es su turno o terminó la partida). */
    public Set<Posicion> movimientosLegales(Posicion origen) {
        if (estado.estaFinalizada()) {
            return Set.of();
        }
        Optional<Pieza> candidata = tablero.piezaEn(origen);
        if (candidata.isEmpty() || candidata.get().color() != turno) {
            return Set.of();
        }
        return movimientosLegales.destinos(tablero, origen);
    }

    /**
     * Ejecuta el movimiento si es válido; si no, lanza MovimientoInvalidoException
     * y deja la partida intacta.
     */
    public ResultadoMovimiento mover(Movimiento movimiento) {
        if (estado.estaFinalizada()) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA);
        }
        Pieza pieza = tablero.piezaEn(movimiento.origen())
                .orElseThrow(() -> new MovimientoInvalidoException(
                        MovimientoInvalidoException.Motivo.SIN_PIEZA_EN_ORIGEN));

        if (pieza.color() != turno) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.NO_ES_SU_TURNO);
        }
        if (!pieza.destinosPosibles(tablero, movimiento.origen()).contains(movimiento.destino())) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO);
        }
        if (movimientosLegales.dejaAlReyEnJaque(tablero, pieza, movimiento.origen(), movimiento.destino())) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.DEJA_AL_REY_EN_JAQUE);
        }

        Optional<Pieza> capturada = tablero.quitar(movimiento.destino());
        tablero.quitar(movimiento.origen());
        tablero.colocar(movimiento.destino(), pieza);

        turno = turno.opuesto();
        estado = evaluarEstado(Optional.of(pieza), capturada.isPresent());
        return new ResultadoMovimiento(capturada, estado);
    }

    /** Consulta las condiciones de fin en orden; si ninguna se cumple, la partida sigue (con o sin jaque). */
    private EstadoPartida evaluarEstado(Optional<Pieza> piezaMovida, boolean huboCaptura) {
        boolean enJaque = detector.estaEnJaque(tablero, turno);
        boolean hayJugadasLegales = movimientosLegales.existeAlguno(tablero, turno);
        ContextoDeJugada contexto = new ContextoDeJugada(
                tablero, turno, piezaMovida, huboCaptura, enJaque, hayJugadasLegales);

        for (CondicionDeFinalizacion condicion : condiciones) {
            Optional<EstadoPartida> resultado = condicion.evaluar(contexto);
            if (resultado.isPresent()) {
                return resultado.get();
            }
        }
        return enJaque ? EstadoPartida.JAQUE : EstadoPartida.EN_CURSO;
    }
}
