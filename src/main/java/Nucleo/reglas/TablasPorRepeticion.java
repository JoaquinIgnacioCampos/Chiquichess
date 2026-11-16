package Nucleo.reglas;

import Nucleo.jugadas.HuellaDePosicion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tablas cuando la misma posición (mismas piezas en las mismas casillas y mismo turno)
 * aparece N veces, por defecto 3. No considera derechos de enroque ni de captura al paso.
 *
 * Cada posición se identifica por su {@link HuellaDePosicion}, que se actualiza con los efectos de
 * cada jugada sin recorrer el tablero. La condición cuenta cuántas veces apareció cada huella; cada
 * registro devuelve como recuerdo qué huella sumó y cuál era la anterior, así deshacer una jugada
 * resta esa aparición y vuelve a la huella previa.
 */
public class TablasPorRepeticion implements CondicionConMemoria {

    /** Memento: la huella previa al registro y la que se sumó en él. */
    private record HuellaAnterior(TablasPorRepeticion duenio, boolean habiaPosicion, long anterior, long sumada)
            implements Recuerdo {
    }

    private final int repeticionesParaTablas;
    private final Map<Long, Integer> apariciones = new HashMap<>();
    private boolean hayPosicion = false;
    private long huellaActual;

    public TablasPorRepeticion() {
        this(3);
    }

    public TablasPorRepeticion(int repeticionesParaTablas) {
        if (repeticionesParaTablas < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 repeticiones");
        }
        this.repeticionesParaTablas = repeticionesParaTablas;
    }

    @Override
    public Recuerdo registrar(ContextoDeJugada contexto) {
        // Si ya hay una posición registrada alcanza con aplicar la última jugada a su huella;
        // si no (posición inicial), se calcula desde el tablero.
        long nueva = hayPosicion && contexto.ultimaJugada().isPresent()
                ? HuellaDePosicion.tras(huellaActual, contexto.ultimaJugada().get())
                : HuellaDePosicion.de(contexto.tablero(), contexto.turno());
        Recuerdo anterior = new HuellaAnterior(this, hayPosicion, huellaActual, nueva);
        apariciones.merge(nueva, 1, Integer::sum);
        huellaActual = nueva;
        hayPosicion = true;
        return anterior;
    }

    @Override
    public void restaurar(Recuerdo recuerdo) {
        if (!(recuerdo instanceof HuellaAnterior anterior) || anterior.duenio() != this) {
            throw new IllegalArgumentException("El recuerdo no pertenece a esta condición");
        }
        apariciones.computeIfPresent(anterior.sumada(), (huella, veces) -> veces > 1 ? veces - 1 : null);
        huellaActual = anterior.anterior();
        hayPosicion = anterior.habiaPosicion();
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        return hayPosicion && apariciones.getOrDefault(huellaActual, 0) >= repeticionesParaTablas
                ? Optional.of(EstadoPartida.TABLAS_REPETICION)
                : Optional.empty();
    }
}
