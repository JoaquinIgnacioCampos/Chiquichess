package Nucleo.reglas;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.piezas.Pieza;
import Nucleo.tablero.ITablero;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Foto de la partida que reciben las condiciones de finalización después de cada jugada
 * (y una vez al crear la partida, cuando todavía no hay jugadas).
 *
 * Incluye todas las jugadas ejecutadas para que las condiciones calculen su resultado a partir del
 * historial en lugar de llevar contadores propios: así siguen siendo correctas si se deshace una jugada.
 *
 * @param tablero                 tablero ya actualizado (solo lectura)
 * @param turno                   bando que debe jugar a continuación
 * @param jugadas                 jugadas ejecutadas en la partida, en orden; vacía en la evaluación inicial
 * @param enJaque                 true si el rey de quien tiene el turno está en jaque
 * @param tieneMovimientosLegales true si quien tiene el turno puede hacer al menos una jugada legal
 */
public record ContextoDeJugada(
        ITablero tablero,
        Bando turno,
        List<Jugada> jugadas,
        boolean enJaque,
        boolean tieneMovimientosLegales) {

    public ContextoDeJugada {
        Objects.requireNonNull(tablero, "tablero");
        Objects.requireNonNull(turno, "turno");
        jugadas = List.copyOf(Objects.requireNonNull(jugadas, "jugadas"));
    }

    /** La última jugada ejecutada; vacío en la evaluación inicial. */
    public Optional<Jugada> ultimaJugada() {
        return jugadas.isEmpty() ? Optional.empty() : Optional.of(jugadas.get(jugadas.size() - 1));
    }

    /** Pieza que acaba de moverse; vacío en la evaluación inicial. */
    public Optional<Pieza> piezaMovida() {
        return ultimaJugada().map(Jugada::piezaMovida);
    }

    /** true si la última jugada capturó una pieza. */
    public boolean huboCaptura() {
        return ultimaJugada().flatMap(Jugada::capturada).isPresent();
    }
}
