package adaptadores.ui;

import Nucleo.juego.Movimiento;
import Nucleo.juego.AccionInvalidaException;
import Nucleo.juego.MovimientoInvalidoException;
import Nucleo.juego.Partida;
import Nucleo.juego.Jugador;
import Nucleo.juego.ResultadoPartida;
import Nucleo.reglas.EstadoPartida;
import Nucleo.tablero.ITablero;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import Nucleo.tablero.Posicion;
import Nucleo.bando.Bando;
/** Interacción de presentación: selecciona casillas y envía órdenes; no calcula reglas. */
public final class ControladorTablero {
    private final Partida partida;
    private Optional<Posicion> seleccion = Optional.empty();

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

    /** Devuelve el motivo de rechazo del núcleo, si hubo un intento de movimiento rechazado. */
    public Optional<MovimientoInvalidoException.Motivo> pulsar(Posicion posicion) {
        Objects.requireNonNull(posicion, "posicion");
        if (partida.estado().estaFinalizada()) {
            seleccion = Optional.empty();
            return Optional.of(MovimientoInvalidoException.Motivo.PARTIDA_FINALIZADA);
        }
        Optional<Posicion> origen = seleccionada();
        if (origen.isPresent() && destinosLegales().contains(posicion)) {
            seleccion = Optional.empty();
            try {
                partida.mover(new Movimiento(origen.get(), posicion));
                return Optional.empty();
            } catch (MovimientoInvalidoException rechazo) {
                return Optional.of(rechazo.motivo());
            }
        }
        seleccion = partida.tablero().piezaEn(posicion)
                .filter(pieza -> pieza.color() == partida.turno())
                .map(pieza -> posicion);
        return Optional.empty();
    }
}
