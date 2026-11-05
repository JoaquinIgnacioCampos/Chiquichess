package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.*;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
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
    private final ReglasDePartida reglas;
    private final RelojPartida reloj;
    private final List<Jugada> jugadas = new ArrayList<>();

    private Bando turno = Bando.BLANCO;
    private EstadoPartida estado;
    private Optional<ResultadoPartida> resultado = Optional.empty();
    private Optional<Bando> ofertaTablas = Optional.empty();

    /**
     * Constructor principal: las reglas (movimientos legales, detector y condiciones de fin) llegan
     * agrupadas en {@link ReglasDePartida}; el reloj pertenece a esta única partida.
     */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   ReglasDePartida reglas, RelojPartida reloj) {
        this.tablero = Objects.requireNonNull(tablero, "tablero");
        this.jugadorBlanco = Objects.requireNonNull(jugadorBlanco, "jugadorBlanco");
        this.jugadorNegro = Objects.requireNonNull(jugadorNegro, "jugadorNegro");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        if (jugadorBlanco.color() != Bando.BLANCO || jugadorNegro.color() != Bando.NEGRO) {
            throw new IllegalArgumentException("Los jugadores no coinciden con sus colores");
        }
        this.reloj.iniciar(turno);
        actualizarEstado(evaluarEstado(Optional.empty(), false));
    }

    /**
     * Atajo sin reloj que arma los MovimientosLegales con el detector recibido.
     *
     * @param condiciones condiciones de fin de partida, evaluadas en orden (la primera que se cumple gana).
     *                    Conviene poner primero jaque mate y ahogado.
     */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, List<CondicionDeFinalizacion> condiciones) {
        this(tablero, jugadorBlanco, jugadorNegro, new ReglasDePartida(detector, condiciones), new SinReloj());
    }

    /**
     * Atajo sin reloj con MovimientosLegales inyectado (lo usan los tests para reemplazarlo por un doble).
     *
     * @throws IllegalArgumentException si movimientosLegales no usa ese mismo detector
     */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, MovimientosLegales movimientosLegales,
                   List<CondicionDeFinalizacion> condiciones) {
        this(tablero, jugadorBlanco, jugadorNegro, detector, movimientosLegales, condiciones, new SinReloj());
    }

    /** Atajo con reloj que arma los MovimientosLegales con el detector recibido. */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, List<CondicionDeFinalizacion> condiciones, RelojPartida reloj) {
        this(tablero, jugadorBlanco, jugadorNegro, new ReglasDePartida(detector, condiciones), reloj);
    }

    /**
     * Atajo con reloj y MovimientosLegales inyectado.
     *
     * @throws IllegalArgumentException si movimientosLegales no usa ese mismo detector
     */
    public Partida(Tablero tablero, Jugador jugadorBlanco, Jugador jugadorNegro,
                   IDetectorDeJaque detector, MovimientosLegales movimientosLegales,
                   List<CondicionDeFinalizacion> condiciones, RelojPartida reloj) {
        this(tablero, jugadorBlanco, jugadorNegro,
                new ReglasDePartida(conMismoDetector(movimientosLegales, detector), condiciones), reloj);
    }

    private static MovimientosLegales conMismoDetector(MovimientosLegales movimientosLegales,
                                                        IDetectorDeJaque detector) {
        Objects.requireNonNull(detector, "detector");
        if (Objects.requireNonNull(movimientosLegales, "movimientosLegales").detector() != detector) {
            throw new IllegalArgumentException("MovimientosLegales debe usar el mismo detector de jaque");
        }
        return movimientosLegales;
    }

    public ITablero tablero() {
        return tablero;
    }

    public Bando turno() {
        return turno;
    }

    public Jugador jugadorActual() {
        return turno == Bando.BLANCO ? jugadorBlanco : jugadorNegro;
    }

    /** Identidad inmutable de un bando, independiente del turno actual. */
    public Jugador jugador(Bando color) {
        Objects.requireNonNull(color, "color");
        return color == Bando.BLANCO ? jugadorBlanco : jugadorNegro;
    }

    public EstadoPartida estado() {
        return estado;
    }

    /** Quién ganó; vacío si la partida sigue o terminó en tablas. */
    public Optional<Jugador> ganador() {
        return resultado.flatMap(ResultadoPartida::ganador)
                .map(color -> color == Bando.BLANCO ? jugadorBlanco : jugadorNegro);
    }

    public Optional<ResultadoPartida> resultado() {
        return resultado;
    }

    /** Foto inmutable de las jugadas ejecutadas, en orden; no incluye intentos ni otras órdenes. */
    public List<Movimiento> historial() {
        return jugadas.stream()
                .map(jugada -> new Movimiento(jugada.origen(), jugada.destino()))
                .toList();
    }

    /** Solo el jugador que tiene el turno puede rendirse. */
    public void rendirse(Bando color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        if (color != turno) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.NO_ES_SU_TURNO);
        }
        finalizar(EstadoPartida.RENDICION, Optional.of(color.opuesto()));
    }

    public Optional<Bando> ofertaTablasPendiente() {
        return ofertaTablas;
    }

    public void ofrecerTablas(Bando color) {
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

    public void aceptarTablas(Bando color) {
        validarRespuestaTablas(color);
        finalizar(EstadoPartida.TABLAS_ACUERDO, Optional.empty());
    }

    public void rechazarTablas(Bando color) {
        validarRespuestaTablas(color);
        ofertaTablas = Optional.empty();
    }

    private void validarRespuestaTablas(Bando color) {
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

    private void finalizar(EstadoPartida estadoFinal, Optional<Bando> ganador) {
        resultado = Optional.of(new ResultadoPartida(ganador, estadoFinal.motivo().orElseThrow()));
        estado = estadoFinal;
        ofertaTablas = Optional.empty();
        reloj.detener();
    }

    /** Consulta pura: vacío significa que la partida no tiene límite de tiempo. */
    public Optional<Duration> tiempoRestante(Bando color) {
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
        return reglas.movimientosLegales().destinos(tablero, origen);
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
        MovimientosLegales movimientosLegales = reglas.movimientosLegales();
        Jugada jugada = movimientosLegales.jugadaPosible(tablero, movimiento.origen(), movimiento.destino())
                .orElseThrow(() -> new MovimientoInvalidoException(
                        MovimientoInvalidoException.Motivo.MOVIMIENTO_NO_PERMITIDO));
        if (movimientosLegales.dejaAlReyEnJaque(tablero, jugada)) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.DEJA_AL_REY_EN_JAQUE);
        }

        // La validación también puede consumir tiempo: comprobar antes de ejecutar.
        verificarTiempo();
        if (estado.estaFinalizada()) {
            throw new MovimientoInvalidoException(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA);
        }
        jugada.aplicar(tablero);
        jugadas.add(jugada);
        Optional<Pieza> capturada = jugada.capturada();

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
        boolean enJaque = reglas.detector().estaEnJaque(tablero, turno);
        boolean hayJugadasLegales = reglas.movimientosLegales().existeAlguno(tablero, turno);
        ContextoDeJugada contexto = new ContextoDeJugada(
                tablero, turno, piezaMovida, huboCaptura, enJaque, hayJugadasLegales);

        for (CondicionDeFinalizacion condicion : reglas.condiciones()) {
            Optional<EstadoPartida> resultado = condicion.evaluar(contexto);
            if (resultado.isPresent()) {
                return resultado.get();
            }
        }
        return enJaque ? EstadoPartida.JAQUE : EstadoPartida.EN_CURSO;
    }
}
