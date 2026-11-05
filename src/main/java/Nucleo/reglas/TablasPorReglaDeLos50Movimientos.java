package Nucleo.reglas;

import Nucleo.jugadas.Jugada;

import java.util.List;
import java.util.Optional;

/**
 * Tablas si pasan 50 movimientos de cada jugador sin ninguna captura ni movimiento de peón.
 *
 * No guarda un contador: recorre el historial desde la última jugada hacia atrás hasta la última
 * captura o movimiento de peón. Así el resultado depende solo del contexto y sigue siendo correcto
 * aunque se deshagan jugadas.
 */
public class TablasPorReglaDeLos50Movimientos implements CondicionDeFinalizacion {

    private final int limiteDeJugadasSueltas;

    public TablasPorReglaDeLos50Movimientos() {
        this(50);
    }

    public TablasPorReglaDeLos50Movimientos(int movimientosPorJugador) {
        if (movimientosPorJugador < 1) {
            throw new IllegalArgumentException("Debe ser al menos 1 movimiento");
        }
        this.limiteDeJugadasSueltas = movimientosPorJugador * 2;
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return jugadasSinProgreso(contexto.jugadas()) >= limiteDeJugadasSueltas
                ? Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS)
                : Optional.empty();
    }

    /** Jugadas seguidas, contando desde la última, sin captura ni movimiento de peón. */
    private static int jugadasSinProgreso(List<Jugada> jugadas) {
        int sueltas = 0;
        for (int i = jugadas.size() - 1; i >= 0; i--) {
            Jugada jugada = jugadas.get(i);
            if (jugada.capturada().isPresent() || jugada.piezaMovida().esPeon()) {
                break;
            }
            sueltas++;
        }
        return sueltas;
    }
}
