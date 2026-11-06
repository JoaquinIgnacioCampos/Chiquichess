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
    /** Condiciones de las reglas que guardan estado, en el orden en que registran (Memento). */
    private final List<CondicionConMemoria> condicionesConMemoria;
    /** Jugadas hechas y deshechas, con los recuerdos de cada una: Partida es el cuidador del Memento. */
    private final HistorialDeJugadas historialDeJugadas = new HistorialDeJugadas();

    private Bando turno = Bando.BLANCO;
    private EstadoPartida estado;
    private Optional<ResultadoPartida> resultado = Optional.empty();
    /** true si el final lo decidió una jugada (mate o tablas automáticas): ese final se puede deshacer. */
    private boolean terminoPorUnaJugada = false;
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
        this.condicionesConMemoria = reglas.condiciones().stream()
                .filter(CondicionConMemoria.class::isInstance)
                .map(CondicionConMemoria.class::cast)
                .toList();
        this.reloj.iniciar(turno);
        // La posición inicial no se deshace: sus recuerdos no hace falta guardarlos.
        registrarYEvaluar(historialDeJugadas.jugadas());
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

    /** Goles de cada bando (modo fútbol), contados en las jugadas ejecutadas. 0 a 0 si no hay pelota. */
    public Marcador marcador() {
        return Marcador.de(historialDeJugadas.jugadas());
    }

    /** Foto inmutable de las jugadas ejecutadas, en orden; no incluye intentos ni otras órdenes. */
    public List<Movimiento> historial() {
        return historialDeJugadas.jugadas().stream()
                .map(jugada -> new Movimiento(jugada.origen(), jugada.destino(),
                        jugada.promocion().map(Pieza::codigo)))
                .toList();
    }

    /**
     * Deshace la última jugada: tablero, turno, condiciones con memoria y estado vuelven a como
     * estaban antes de ella. Cualquiera de los dos jugadores puede hacerlo. Se puede deshacer una
     * jugada que terminó la partida (mate o tablas automáticas), pero no un final decidido por los
     * jugadores o por el reloj. Borra la oferta de tablas pendiente.
     *
     * @throws AccionInvalidaException si la partida no permite deshacer (no se habilitó o tiene
     *                                 límite de tiempo), si terminó por rendición, acuerdo o tiempo,
     *                                 o si no hay jugadas para deshacer
     */
    public void deshacer() {
        asegurarDeshacerPermitido();
        if (resultado.isPresent() && !terminoPorUnaJugada) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA);
        }
        if (!historialDeJugadas.hayParaDeshacer()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.NADA_PARA_DESHACER);
        }
        HistorialDeJugadas.Entrada entrada = historialDeJugadas.deshacer();
        entrada.jugada().revertir(tablero);
        // Memento: cada condición recupera su propio recuerdo, en orden inverso al registro.
        for (int i = condicionesConMemoria.size() - 1; i >= 0; i--) {
            condicionesConMemoria.get(i).restaurar(entrada.recuerdos().get(i));
        }
        turno = turno.opuesto();
        ofertaTablas = Optional.empty();
        resultado = Optional.empty();
        terminoPorUnaJugada = false;
        actualizarEstado(evaluar(contexto(historialDeJugadas.jugadas())));
    }

    /**
     * Vuelve a ejecutar la última jugada deshecha. Hacer una jugada nueva borra lo que se podía
     * rehacer. Borra la oferta de tablas pendiente.
     *
     * @throws AccionInvalidaException si la partida no permite deshacer, si terminó o si no hay
     *                                 jugadas para rehacer
     */
    public void rehacer() {
        asegurarDeshacerPermitido();
        if (estado.estaFinalizada()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA);
        }
        if (!historialDeJugadas.hayParaRehacer()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.NADA_PARA_REHACER);
        }
        Jugada jugada = historialDeJugadas.siguienteParaRehacer();
        jugada.aplicar(tablero);
        turno = turno.opuesto();
        ofertaTablas = Optional.empty();
        historialDeJugadas.confirmarRehecha(registrarYEvaluar(historialDeJugadas.jugadasCon(jugada)));
    }

    /** true si en este momento {@link #deshacer()} se aceptaría. Lo usa la UI para habilitar el botón. */
    public boolean puedeDeshacer() {
        return deshacerHabilitado() && historialDeJugadas.hayParaDeshacer()
                && (resultado.isEmpty() || terminoPorUnaJugada);
    }

    /** true si en este momento {@link #rehacer()} se aceptaría. */
    public boolean puedeRehacer() {
        return deshacerHabilitado() && historialDeJugadas.hayParaRehacer() && !estado.estaFinalizada();
    }

    /**
     * true si esta partida admite deshacer en algún momento: las reglas lo permiten y no tiene límite
     * de tiempo. Lo usa la UI para mostrar u ocultar los botones.
     */
    public boolean permiteDeshacer() {
        return reglas.permiteDeshacer() && reloj.tiempoRestante(Bando.BLANCO).isEmpty();
    }

    private boolean deshacerHabilitado() {
        return permiteDeshacer();
    }

    private void asegurarDeshacerPermitido() {
        if (!deshacerHabilitado()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.DESHACER_NO_PERMITIDO);
        }
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

    /** Aplica el estado (y el ganador) que decidieron las condiciones de fin después de una jugada. */
    private void actualizarEstado(Veredicto veredicto) {
        if (veredicto.estado().estaFinalizada()) {
            finalizar(veredicto.estado(), veredicto.ganador());
            terminoPorUnaJugada = true;
        } else {
            estado = veredicto.estado();
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
        return reglas.movimientosLegales().destinos(tablero, origen, historialDeJugadas.jugadas());
    }

    /**
     * Piezas a las que puede promover el peón si va de origen a destino, en el orden de las reglas
     * (la primera es la que se usa si no se elige). Vacío si ese movimiento no es una promoción
     * permitida ahora: así la UI sabe cuándo preguntar qué pieza quiere el jugador.
     */
    public List<Pieza> opcionesDePromocion(Posicion origen, Posicion destino) {
        Objects.requireNonNull(destino, "destino");
        if (!movimientosLegales(origen).contains(destino)) {
            return List.of();
        }
        return reglas.movimientosLegales()
                .jugadasPosibles(tablero, origen, historialDeJugadas.jugadas()).stream()
                .filter(jugada -> jugada.destino().equals(destino))
                .flatMap(jugada -> jugada.promocion().stream())
                .toList();
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
        Jugada jugada = elegirJugada(movimientosLegales, movimiento)
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
        Optional<Pieza> capturada = jugada.capturada();

        if (ofertaTablas.filter(oferente -> oferente != pieza.color()).isPresent()) {
            ofertaTablas = Optional.empty();
        }
        turno = turno.opuesto();
        reloj.cambiarTurno();
        historialDeJugadas.agregar(jugada, registrarYEvaluar(historialDeJugadas.jugadasCon(jugada)));
        return new ResultadoMovimiento(capturada, estado);
    }

    /**
     * La jugada que corresponde al movimiento pedido. Si hay varias hacia el mismo destino (una por
     * cada pieza de promoción), se usa la promoción elegida; sin elección, la primera opción (dama).
     * Pedir una promoción en una jugada que no promueve, o una pieza que no está entre las opciones,
     * no corresponde a ninguna jugada.
     */
    private Optional<Jugada> elegirJugada(MovimientosLegales movimientosLegales, Movimiento movimiento) {
        List<Jugada> haciaElDestino = movimientosLegales
                .jugadasPosibles(tablero, movimiento.origen(), historialDeJugadas.jugadas()).stream()
                .filter(jugada -> jugada.destino().equals(movimiento.destino()))
                .toList();
        if (movimiento.promocion().isEmpty()) {
            return haciaElDestino.stream().findFirst();
        }
        char pedida = movimiento.promocion().get();
        return haciaElDestino.stream()
                .filter(jugada -> jugada.promocion().map(Pieza::codigo).filter(codigo -> codigo == pedida).isPresent())
                .findFirst();
    }

    /** Foto de la posición actual que reciben las condiciones de fin. */
    private ContextoDeJugada contexto(List<Jugada> jugadas) {
        boolean enJaque = reglas.detector().estaEnJaque(tablero, turno);
        boolean hayJugadasLegales = reglas.movimientosLegales().existeAlguno(tablero, turno, jugadas);
        return new ContextoDeJugada(tablero, turno, jugadas, enJaque, hayJugadasLegales);
    }

    /**
     * Arma el contexto de la posición actual, hace que todas las condiciones con memoria la registren
     * y recién después decide el estado. Registrar primero garantiza que todas anoten cada jugada,
     * aunque una condición anterior termine la partida. Devuelve los recuerdos de ese registro,
     * en el mismo orden que {@link #condicionesConMemoria}.
     */
    private List<Recuerdo> registrarYEvaluar(List<Jugada> jugadas) {
        ContextoDeJugada contexto = contexto(jugadas);
        List<Recuerdo> recuerdos = new ArrayList<>();
        for (CondicionConMemoria condicion : condicionesConMemoria) {
            recuerdos.add(condicion.registrar(contexto));
        }
        actualizarEstado(evaluar(contexto));
        return recuerdos;
    }

    /** Estado que decidieron las condiciones de fin y, si alguna terminó la partida con victoria, quién ganó. */
    private record Veredicto(EstadoPartida estado, Optional<Bando> ganador) {
    }

    /**
     * Consulta las condiciones de fin en orden; si ninguna se cumple, la partida sigue (con o sin jaque).
     * La condición que termina la partida es la que dice quién ganó.
     */
    private Veredicto evaluar(ContextoDeJugada contexto) {
        for (CondicionDeFinalizacion condicion : reglas.condiciones()) {
            Optional<EstadoPartida> resultado = condicion.evaluar(contexto);
            if (resultado.isPresent()) {
                return new Veredicto(resultado.get(), condicion.ganador(resultado.get(), contexto));
            }
        }
        return new Veredicto(contexto.enJaque() ? EstadoPartida.JAQUE : EstadoPartida.EN_CURSO, Optional.empty());
    }
}
