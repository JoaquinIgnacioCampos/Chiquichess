package Nucleo.juego;

import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.*;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.RelojPartida;
import Nucleo.tiempo.SinReloj;

import java.time.Duration;
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
    private final RelojPartida reloj;
    private final List<Movimiento> historial = new ArrayList<>();

    private Color turno = Color.BLANCO;
    private EstadoPartida estado;
    private Optional<ResultadoPartida> resultado = Optional.empty();
    private Optional<Color> ofertaTablas = Optional.empty();

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
        this(tablero, jugadorBlanco, jugadorNegro, detector, movimientosLegales, condiciones, new SinReloj());
    }

    /** Variante con reloj; la instancia inyectada pertenece a esta única partida. */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, List<CondicionDeFinalizacion> condiciones, RelojPartida reloj) {
        this(tablero, jugadorBlanco, jugadorNegro, detector, new MovimientosLegales(detector), condiciones, reloj);
    }

    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, MovimientosLegales movimientosLegales,
                   List<CondicionDeFinalizacion> condiciones, RelojPartida reloj) {
        this.tablero = Objects.requireNonNull(tablero, "tablero");
        this.jugadorBlanco = Objects.requireNonNull(jugadorBlanco, "jugadorBlanco");
        this.jugadorNegro = Objects.requireNonNull(jugadorNegro, "jugadorNegro");
        this.detector = Objects.requireNonNull(detector, "detector");
        this.movimientosLegales = Objects.requireNonNull(movimientosLegales, "movimientosLegales");
        this.condiciones = List.copyOf(condiciones);
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        if (jugadorBlanco.color() != Color.BLANCO || jugadorNegro.color() != Color.NEGRO) {
            throw new IllegalArgumentException("Los jugadores no coinciden con sus colores");
        }
        this.reloj.iniciar(turno);
        actualizarEstado(evaluarEstado(Optional.empty(), false));
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
        return resultado.flatMap(ResultadoPartida::ganador)
                .map(color -> color == Color.BLANCO ? jugadorBlanco : jugadorNegro);
    }

    public Optional<ResultadoPartida> resultado() {
        return resultado;
    }

    /** Foto inmutable de las jugadas ejecutadas, en orden; no incluye intentos ni otras órdenes. */
    public List<Movimiento> historial() {
        return List.copyOf(historial);
    }

    /** Cualquier bando puede rendirse, incluso fuera de su turno. */
    public void rendirse(Color color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        finalizar(EstadoPartida.RENDICION, Optional.of(color.opuesto()));
    }

    public Optional<Color> ofertaTablasPendiente() {
        return ofertaTablas;
    }

    public void ofrecerTablas(Color color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        if (color != turno) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.NO_ES_SU_TURNO);
        }
        if (ofertaTablas.isPresent()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.OFERTA_PENDIENTE);
        }
        ofertaTablas = Optional.of(color);
    }

    public void aceptarTablas(Color color) {
        validarRespuestaTablas(color);
        finalizar(EstadoPartida.TABLAS_ACUERDO, Optional.empty());
    }

    public void rechazarTablas(Color color) {
        validarRespuestaTablas(color);
        ofertaTablas = Optional.empty();
    }

    private void validarRespuestaTablas(Color color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        if (ofertaTablas.isEmpty()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.SIN_OFERTA);
        }
        if (ofertaTablas.get() == color) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.ES_SU_PROPIA_OFERTA);
        }
    }

    private void asegurarEnCurso() {
        verificarTiempo();
        if (estado.estaFinalizada()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA);
        }
    }

    private void actualizarEstado(EstadoPartida nuevoEstado) {
        if (nuevoEstado.estaFinalizada()) {
            finalizar(nuevoEstado, nuevoEstado.esTablas() ? Optional.empty() : Optional.of(turno.opuesto()));
        } else {
            estado = nuevoEstado;
        }
    }

    private void finalizar(EstadoPartida estadoFinal, Optional<Color> ganador) {
        resultado = Optional.of(new ResultadoPartida(ganador, estadoFinal.motivo().orElseThrow()));
        estado = estadoFinal;
        ofertaTablas = Optional.empty();
        reloj.detener();
    }

    /** Consulta pura: vacío significa que la partida no tiene límite de tiempo. */
    public Optional<Duration> tiempoRestante(Color color) {
        return reloj.tiempoRestante(Objects.requireNonNull(color, "color"));
    }

    /** La UI puede invocarla periódicamente; después del final no produce cambios. */
    public void verificarTiempo() {
        if (!estado.estaFinalizada() && reloj.tiempoAgotado(turno)) {
            finalizar(EstadoPartida.TIEMPO_AGOTADO, Optional.of(turno.opuesto()));
        }
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
     * y deja la partida intacta. Si el tiempo venció, registra ese final sin mover.
     */
    public ResultadoMovimiento mover(Movimiento movimiento) {
        verificarTiempo();
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

        // La validación también puede consumir tiempo: comprobar antes de ejecutar.
        verificarTiempo();
        if (estado.estaFinalizada()) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA);
        }
        Optional<Pieza> capturada = tablero.quitar(movimiento.destino());
        tablero.quitar(movimiento.origen());
        tablero.colocar(movimiento.destino(), pieza);
        historial.add(movimiento);

        if (ofertaTablas.filter(oferente -> oferente != pieza.color()).isPresent()) {
            ofertaTablas = Optional.empty();
        }
        turno = turno.opuesto();
        reloj.cambiarTurno();
        actualizarEstado(evaluarEstado(Optional.of(pieza), capturada.isPresent()));
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
