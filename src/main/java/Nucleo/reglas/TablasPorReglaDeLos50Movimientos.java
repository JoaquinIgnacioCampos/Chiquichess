package Nucleo.reglas;

import Nucleo.jugadas.Jugada;

import java.util.Optional;

/**
 * Tablas si pasan 50 movimientos de cada jugador sin ninguna captura ni movimiento de peón.
 *
 * Lleva un contador de jugadas seguidas sin progreso. Cada registro devuelve como recuerdo el valor
 * anterior del contador, así deshacer una jugada lo deja exactamente como estaba, incluso si esa
 * jugada lo había vuelto a cero.
 */
public class TablasPorReglaDeLos50Movimientos implements CondicionConMemoria {

    /** Memento: el valor del contador antes del registro. */
    private record ContadorAnterior(TablasPorReglaDeLos50Movimientos duenio, int valor) implements Recuerdo {
    }

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
    public Recuerdo registrar(ContextoDeJugada contexto) {
        Recuerdo anterior = new ContadorAnterior(this, jugadasSinProgreso);
        Optional<Jugada> ultima = contexto.ultimaJugada();
        if (ultima.isEmpty()) {
            jugadasSinProgreso = 0; // posición inicial: todavía no hubo ninguna jugada
        } else if (ultima.get().capturada().isPresent()
                || (!ultima.get().esPase() && ultima.get().piezaMovida().esPeon())) {
            // Captura o avance de peón. Un pase de un peón no cuenta: el peón no se movió.
            jugadasSinProgreso = 0;
        } else {
            jugadasSinProgreso++;
        }
        return anterior;
    }

    @Override
    public void restaurar(Recuerdo recuerdo) {
        if (!(recuerdo instanceof ContadorAnterior anterior) || anterior.duenio() != this) {
            throw new IllegalArgumentException("El recuerdo no pertenece a esta condición");
        }
        jugadasSinProgreso = anterior.valor();
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return jugadasSinProgreso >= limiteDeJugadasSueltas
                ? Optional.of(EstadoPartida.TABLAS_REGLA_50_MOVIMIENTOS)
                : Optional.empty();
    }
}
