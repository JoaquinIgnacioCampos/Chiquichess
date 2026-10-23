package dobles;

import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.tablero.ITablero;

import java.util.Optional;

/** Atajos para armar un ContextoDeJugada en los tests de las condiciones de finalización. */
public final class Contextos {

    private Contextos() {
    }

    /** Contexto en el que solo importan los dos booleanos (para jaque mate y ahogado). */
    public static ContextoDeJugada con(boolean enJaque, boolean tieneMovimientosLegales) {
        return new ContextoDeJugada(TableroFake.vacio(), Color.BLANCO, Optional.empty(),
                false, enJaque, tieneMovimientosLegales);
    }

    /** Contexto de la evaluación inicial (todavía no se movió ninguna pieza). */
    public static ContextoDeJugada inicial(ITablero tablero, Color turno) {
        return new ContextoDeJugada(tablero, turno, Optional.empty(), false, false, true);
    }

    /** Contexto después de mover una pieza. */
    public static ContextoDeJugada trasMover(ITablero tablero, Color turno, Pieza piezaMovida, boolean huboCaptura) {
        return new ContextoDeJugada(tablero, turno, Optional.of(piezaMovida), huboCaptura, false, true);
    }
}
