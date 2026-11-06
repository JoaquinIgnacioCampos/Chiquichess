package adaptadores.ui;

import Nucleo.bando.Bando;
import Nucleo.juego.Movimiento;
import Nucleo.juego.AccionInvalidaException;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.juego.Jugador;
import Nucleo.juego.ResultadoPartida;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.EstadoPartida;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Posicion;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Interacción de presentación: selecciona casillas y envía órdenes; no calcula reglas. */
public final class ControladorTablero {
    private final Partida partida;
    private Optional<Posicion> seleccion = Optional.empty();
    /** Movimiento de un peón a la última fila que espera la elección de la pieza. */
    private Optional<Movimiento> promocion = Optional.empty();

    public ControladorTablero(Partida partida) {
        this.partida = Objects.requireNonNull(partida, "partida");
    }

    public ITablero tablero() {
        return partida.tablero();
    }

    public Jugador jugador(Bando color) {
        return partida.jugador(color);
    }

    public Bando turno() {
        return partida.turno();
    }

    public EstadoPartida estado() {
        return partida.estado();
    }

    public Optional<ResultadoPartida> resultado() {
        return partida.resultado();
    }

    public Optional<Duration> tiempoRestante(Bando color) {
        return partida.tiempoRestante(color);
    }

    public void verificarTiempo() {
        partida.verificarTiempo();
    }

    public Optional<Bando> ofertaTablasPendiente() {
        return partida.ofertaTablasPendiente();
    }

    public Optional<AccionInvalidaException.Motivo> ofrecerTablas(Bando color) {
        return ejecutarAccion(() -> partida.ofrecerTablas(color));
    }

    public Optional<AccionInvalidaException.Motivo> aceptarTablas(Bando color) {
        return ejecutarAccion(() -> partida.aceptarTablas(color));
    }

    public Optional<AccionInvalidaException.Motivo> rechazarTablas(Bando color) {
        return ejecutarAccion(() -> partida.rechazarTablas(color));
    }

    private Optional<AccionInvalidaException.Motivo> ejecutarAccion(Runnable accion) {
        try {
            accion.run();
            seleccionada();
            return Optional.empty();
        } catch (AccionInvalidaException rechazo) {
            return Optional.of(rechazo.motivo());
        }
    }

    /** true si la partida admite deshacer (se eligió la opción y no tiene tiempo): define si hay botones. */
    public boolean permiteDeshacer() {
        return partida.permiteDeshacer();
    }

    public boolean puedeDeshacer() {
        return partida.puedeDeshacer();
    }

    public boolean puedeRehacer() {
        return partida.puedeRehacer();
    }

    /** Deshace la última jugada; la selección se descarta porque el tablero cambió. */
    public Optional<AccionInvalidaException.Motivo> deshacer() {
        return ejecutarYLimpiarSeleccion(partida::deshacer);
    }

    public Optional<AccionInvalidaException.Motivo> rehacer() {
        return ejecutarYLimpiarSeleccion(partida::rehacer);
    }

    private Optional<AccionInvalidaException.Motivo> ejecutarYLimpiarSeleccion(Runnable accion) {
        try {
            accion.run();
            seleccion = Optional.empty();
            promocion = Optional.empty();
            return Optional.empty();
        } catch (AccionInvalidaException rechazo) {
            return Optional.of(rechazo.motivo());
        }
    }

    /** El núcleo valida la orden y determina el ganador. */
    public Optional<AccionInvalidaException.Motivo> rendirse(Bando color) {
        partida.verificarTiempo();
        try {
            partida.rendirse(color);
            seleccion = Optional.empty();
            return Optional.empty();
        } catch (AccionInvalidaException rechazo) {
            return Optional.of(rechazo.motivo());
        }
    }

    /** Descarta una selección que perdió vigencia por una actualización externa del núcleo. */
    public Optional<Posicion> seleccionada() {
        seleccion = seleccion.filter(posicion -> !partida.estado().estaFinalizada()
                && partida.tablero().piezaEn(posicion)
                .map(pieza -> pieza.color() == partida.turno()).orElse(false));
        return seleccion;
    }

    public Set<Posicion> destinosLegales() {
        return seleccionada().map(origen -> Set.copyOf(partida.movimientosLegales(origen)))
                .orElseGet(Set::of);
    }

    /**
     * Piezas entre las que tiene que elegir el jugador porque llevó un peón a la última fila; vacío
     * si no hay una promoción esperando. Si la partida cambió (deshacer, tiempo, rendición), la
     * promoción pendiente se descarta.
     */
    public List<Pieza> promocionPendiente() {
        List<Pieza> opciones = promocion
                .map(movimiento -> partida.opcionesDePromocion(movimiento.origen(), movimiento.destino()))
                .orElseGet(List::of);
        if (opciones.isEmpty()) {
            promocion = Optional.empty();
        }
        return opciones;
    }

    /**
     * Completa la promoción pendiente con la pieza elegida (por su código: 'D', 'T', 'A', 'C').
     *
     * @throws IllegalStateException si no hay una promoción esperando
     */
    public Optional<MovimientoInvalidoException.Motivo> promover(char codigo) {
        if (promocionPendiente().isEmpty()) {
            throw new IllegalStateException("No hay una promoción pendiente");
        }
        Movimiento elegido = Movimiento.conPromocion(promocion.get().origen(), promocion.get().destino(), codigo);
        promocion = Optional.empty();
        return mover(elegido);
    }

    /** El jugador se arrepintió: el peón sigue donde estaba y la selección se descarta. */
    public void cancelarPromocion() {
        promocion = Optional.empty();
    }

    /** Devuelve el motivo de rechazo del núcleo, si hubo un intento de movimiento rechazado. */
    public Optional<MovimientoInvalidoException.Motivo> pulsar(Posicion posicion) {
        Objects.requireNonNull(posicion, "posicion");
        promocion = Optional.empty();
        if (partida.estado().estaFinalizada()) {
            seleccion = Optional.empty();
            return Optional.of(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA);
        }
        Optional<Posicion> origen = seleccionada();
        if (origen.isPresent() && destinosLegales().contains(posicion)) {
            seleccion = Optional.empty();
            // Si el peón llega a la última fila, primero hay que preguntar a qué pieza promueve.
            if (!partida.opcionesDePromocion(origen.get(), posicion).isEmpty()) {
                promocion = Optional.of(new Movimiento(origen.get(), posicion));
                return Optional.empty();
            }
            return mover(new Movimiento(origen.get(), posicion));
        }
        seleccion = partida.tablero().piezaEn(posicion)
                .filter(pieza -> pieza.color() == partida.turno())
                .map(pieza -> posicion);
        return Optional.empty();
    }

    private Optional<MovimientoInvalidoException.Motivo> mover(Movimiento movimiento) {
        try {
            partida.mover(movimiento);
            return Optional.empty();
        } catch (MovimientoInvalidoException rechazo) {
            return Optional.of(rechazo.motivo());
        }
    }
}
