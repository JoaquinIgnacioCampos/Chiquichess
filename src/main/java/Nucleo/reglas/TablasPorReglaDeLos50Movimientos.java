package Nucleo.reglas;

import java.util.Optional;

/**
 * Tablas si pasan 50 movimientos de cada jugador sin ninguna captura ni movimiento de peón.
 */
public class TablasPorReglaDeLos50Movimientos implements CondicionDeFinalizacion {

    private final int limiteDeJugadasSueltas;
    private int jugadasSinProgreso = 0;

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
        if (contexto.piezaMovida().isEmpty()) {
            return Optional.empty(); // evaluación inicial: todavía no hubo ninguna jugada
        }
        if (contexto.huboCaptura() || contexto.piezaMovida().get().esPeon()) {
            jugadasSinProgreso = 0;
        } else {
            jugadasSinProgreso++;
        }
        return jugadasSinProgreso >= limiteDeJugadasSueltas
                ? Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS)
                : Optional.empty();
    }
}
