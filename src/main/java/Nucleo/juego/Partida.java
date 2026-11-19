package Nucleo.juego;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.Marcador;
import Nucleo.reglas.Recuerdo;
import Nucleo.reglas.MovimientosLegales;
import Nucleo.reglas.ReglasDePartida;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;
import Nucleo.tablero.Tablero;
import Nucleo.tiempo.RelojPartida;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Orquesta una partida: turnos, validación y ejecución de jugadas, deshacer y rehacer, y los
 * finales decididos por los jugadores (rendición, acuerdo) o por el reloj.
 *
 * Recibe sus colaboradores por constructor (inyección de dependencias), por lo que se puede testear
 * con cualquier tablero armado a mano. Delega en clases propias lo que no es coordinar:
 * <ul>
 *   <li>{@link ArbitroDeFinales}: si la partida terminó después de una jugada, y la memoria de las condiciones;</li>
 *   <li>{@link HistorialDeJugadas}: las jugadas hechas y deshechas;</li>
 *   <li>{@link OfertaDeTablas}: la oferta pendiente y sus reglas;</li>
 *   <li>{@link AvisosDePartida}: los observadores.</li>
 * </ul>
 */
public class Partida implements PartidaEnJuego {

    private final Tablero tablero;
    private final Jugador jugadorBlanco;
    private final Jugador jugadorNegro;
    private final ReglasDePartida reglas;
    private final RelojPartida reloj;
    private final ArbitroDeFinales arbitro;
    /** Jugadas hechas y deshechas, con los recuerdos de cada una: Partida es el cuidador del Memento. */
    private final HistorialDeJugadas historialDeJugadas = new HistorialDeJugadas();
    private final OfertaDeTablas ofertaDeTablas = new OfertaDeTablas();
    private final AvisosDePartida avisos = new AvisosDePartida();

    private Bando turno = Bando.BLANCO;
    private EstadoPartida estado;
    private Optional<ResultadoPartida> resultado = Optional.empty();
    /** true si el final lo decidió una jugada (mate o tablas automáticas): ese final se puede deshacer. */
    private boolean terminoPorUnaJugada = false;

    /**
     * Las reglas (movimientos legales, detector y condiciones de fin) llegan agrupadas en
     * {@link ReglasDePartida}; el reloj pertenece a esta única partida. Para jugar sin reloj se
     * pasa {@link Nucleo.tiempo.SinReloj}.
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
        this.arbitro = new ArbitroDeFinales(reglas);
        this.reloj.iniciar(turno);
        // La posición inicial no se deshace: sus recuerdos no hace falta guardarlos.
        registrar(historialDeJugadas.jugadas());
    }

    public ITablero tablero() {
        return tablero;
    }

    /** Suscribe un adaptador a los eventos de esta partida (ver {@link EventoDePartida}). */
    public void agregarObservador(ObservadorDePartida observador) {
        avisos.agregar(observador);
    }

    public void quitarObservador(ObservadorDePartida observador) {
        avisos.quitar(observador);
    }

    /** Avisa el final si la partida está terminada. Se llama después del aviso de la jugada que la terminó. */
    private void avisarSiTermino() {
        resultado.filter(r -> estado.estaFinalizada())
                .ifPresent(r -> avisos.avisar(new EventoDePartida.PartidaFinalizada(r)));
    }

    public Bando turno() {
        return turno;
    }

    public Jugador jugadorActual() {
        return jugador(turno);
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
        return resultado.flatMap(ResultadoPartida::ganador).map(this::jugador);
    }

    public Optional<ResultadoPartida> resultado() {
        return resultado;
    }

    /**
     * Jugadas (de los dos bandos) a las que termina la partida, si alguna condición de fin tiene ese
     * límite (el modo fútbol); vacío si no hay límite.
     */
    public Optional<Integer> limiteDeJugadas() {
        return arbitro.limiteDeJugadas();
    }

    /**
     * Goles de cada bando, contados en las jugadas ejecutadas, si alguna condición de fin se juega con
     * goles (el modo fútbol); vacío si no. Igual que el límite de jugadas, lo informa la condición.
     */
    public Optional<Marcador> marcador() {
        return arbitro.marcador(historialDeJugadas.jugadas());
    }

    /** Foto inmutable de las jugadas ejecutadas, en orden; no incluye intentos ni otras órdenes. */
    public List<Movimiento> historial() {
        return historialDeJugadas.jugadas().stream().map(Movimiento::de).toList();
    }

    /**
     * Copia de la situación actual para que una IA la analice: tablero copiado, turno, jugadas hechas y
     * reglas. Lo que la IA haga sobre la copia no toca la partida ni dispara eventos.
     */
    public SituacionDePartida situacion() {
        return new SituacionDePartida(tablero.copiar(), turno, historialDeJugadas.jugadas(), reglas);
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
        arbitro.restaurar(entrada.recuerdos());
        turno = turno.opuesto();
        ofertaDeTablas.descartar();
        resultado = Optional.empty();
        terminoPorUnaJugada = false;
        aplicar(arbitro.evaluar(tablero, turno, historialDeJugadas.jugadas()));
        avisos.avisar(new EventoDePartida.JugadaDeshecha(estado));
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
        ofertaDeTablas.descartar();
        historialDeJugadas.confirmarRehecha(registrar(historialDeJugadas.jugadasCon(jugada)));
        avisos.avisar(new EventoDePartida.JugadaRehecha(jugada.piezaMovida().color(),
                EventoDePartida.Rasgo.de(jugada), estado));
        avisarSiTermino();
    }

    /** true si en este momento {@link #deshacer()} se aceptaría. Lo usa la UI para habilitar el botón. */
    public boolean puedeDeshacer() {
        return permiteDeshacer() && historialDeJugadas.hayParaDeshacer()
                && (resultado.isEmpty() || terminoPorUnaJugada);
    }

    /** true si en este momento {@link #rehacer()} se aceptaría. */
    public boolean puedeRehacer() {
        return permiteDeshacer() && historialDeJugadas.hayParaRehacer() && !estado.estaFinalizada();
    }

    /**
     * true si esta partida admite deshacer en algún momento: las reglas lo permiten y no tiene límite
     * de tiempo. Lo usa la UI para mostrar u ocultar los botones.
     */
    public boolean permiteDeshacer() {
        return reglas.permiteDeshacer() && reloj.tiempoRestante(Bando.BLANCO).isEmpty();
    }

    private void asegurarDeshacerPermitido() {
        if (!permiteDeshacer()) {
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
        avisarSiTermino();
    }

    public Optional<Bando> ofertaTablasPendiente() {
        return ofertaDeTablas.pendiente();
    }

    public void ofrecerTablas(Bando color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        ofertaDeTablas.ofrecer(color, turno);
    }

    public void aceptarTablas(Bando color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        ofertaDeTablas.validarRespuesta(color);
        finalizar(EstadoPartida.TABLAS_ACUERDO, Optional.empty());
        avisarSiTermino();
    }

    public void rechazarTablas(Bando color) {
        Objects.requireNonNull(color, "color");
        asegurarEnCurso();
        ofertaDeTablas.validarRespuesta(color);
        ofertaDeTablas.descartar();
    }

    private void asegurarEnCurso() {
        verificarTiempo();
        if (estado.estaFinalizada()) {
            throw new AccionInvalidaException(AccionInvalidaException.Motivo.PARTIDA_FINALIZADA);
        }
    }

    /** Consulta pura: vacío significa que la partida no tiene límite de tiempo. */
    public Optional<Duration> tiempoRestante(Bando color) {
        return reloj.tiempoRestante(Objects.requireNonNull(color, "color"));
    }

    /** La UI puede invocarla periódicamente; después del final no produce cambios. */
    public void verificarTiempo() {
        if (!estado.estaFinalizada() && reloj.tiempoAgotado(turno)) {
            finalizar(EstadoPartida.TIEMPO_AGOTADO, Optional.of(turno.opuesto()));
            avisarSiTermino();
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
        ofertaDeTablas.alMover(pieza.color());
        turno = turno.opuesto();
        reloj.cambiarTurno();
        historialDeJugadas.agregar(jugada, registrar(historialDeJugadas.jugadasCon(jugada)));
        avisos.avisar(new EventoDePartida.JugadaHecha(pieza.color(), EventoDePartida.Rasgo.de(jugada), estado));
        avisarSiTermino();
        return new ResultadoMovimiento(jugada.capturada(), estado);
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

    /** El árbitro registra la posición actual y decide el estado; devuelve los recuerdos para el historial. */
    private List<Recuerdo> registrar(List<Jugada> jugadas) {
        ArbitroDeFinales.Registro registro = arbitro.registrarYEvaluar(tablero, turno, jugadas);
        aplicar(registro.veredicto());
        return registro.recuerdos();
    }

    /** Aplica el estado (y el ganador) que decidió el árbitro después de una jugada. */
    private void aplicar(ArbitroDeFinales.Veredicto veredicto) {
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
        ofertaDeTablas.descartar();
        reloj.detener();
    }
}
