package dobles;

import Nucleo.bando.Bando;
import Nucleo.jugadas.Jugada;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.tablero.ITablero;
import Nucleo.tablero.Tablero;

import java.util.List;

/** Atajos para armar un ContextoDeJugada en los tests de las condiciones de finalización. */
public final class Contextos {

    private Contextos() {
    }

    /** Contexto en el que solo importan los dos booleanos (para jaque mate y ahogado). */
    public static ContextoDeJugada con(boolean enJaque, boolean tieneMovimientosLegales) {
        return new ContextoDeJugada(TableroFake.vacio(), Bando.BLANCO, List.of(),
                enJaque, tieneMovimientosLegales);
    }

    /** Contexto de la evaluación inicial (todavía no se jugó nada). */
    public static ContextoDeJugada inicial(ITablero tablero, Bando turno) {
        return conHistorial(tablero, turno, List.of());
    }

    /** Contexto con un historial dado; las jugadas no se aplican, solo se informan. */
    public static ContextoDeJugada conHistorial(ITablero tablero, Bando turno, List<Jugada> jugadas) {
        return new ContextoDeJugada(tablero, turno, jugadas, false, true);
    }

    /**
     * Juega la secuencia sobre el tablero (empezando las blancas) y devuelve el contexto que
     * recibirían las condiciones después de la última jugada.
     */
    public static ContextoDeJugada trasJugar(Tablero tablero, String... jugadas) {
        List<Jugada> ejecutadas = JugadasDePrueba.jugar(tablero, jugadas);
        Bando turno = ejecutadas.size() % 2 == 0 ? Bando.BLANCO : Bando.NEGRO;
        return conHistorial(tablero, turno, ejecutadas);
    }
}
