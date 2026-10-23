package Nucleo.reglas;

import Nucleo.juego.Posicion;
import Nucleo.piezas.Color;
import Nucleo.piezas.Pieza;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Tablas cuando la misma posición (mismas piezas en las mismas casillas y mismo turno)
 * aparece N veces, por defecto 3. No considera derechos de enroque ni de captura al paso
 * porque esas reglas todavía no existen en el juego.
 */
public class TablasPorRepeticion implements CondicionDeFinalizacion {

    /** Identidad de una posición. Al ser record, equals/hashCode comparan el contenido. */
    private record Instantanea(Map<Posicion, String> casillas, Color turno) {
    }

    private final int repeticionesParaTablas;
    private final Map<Instantanea, Integer> apariciones = new HashMap<>();

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
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        int veces = apariciones.merge(instantanea(contexto), 1, Integer::sum);
        return veces >= repeticionesParaTablas
                ? Optional.of(EstadoPartida.TABLAS_REPETICION)
                : Optional.empty();
    }

    private Instantanea instantanea(ContextoDeJugada contexto) {
        Map<Posicion, String> casillas = new HashMap<>();
        for (Map.Entry<Posicion, Pieza> entrada : contexto.tablero().piezas().entrySet()) {
            Pieza pieza = entrada.getValue();
            casillas.put(entrada.getKey(), pieza.color() + ":" + pieza.nombre());
        }
        return new Instantanea(Map.copyOf(casillas), contexto.turno());
    }
}
