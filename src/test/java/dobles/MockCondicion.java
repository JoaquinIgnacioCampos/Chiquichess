package dobles;

import Nucleo.reglas.CondicionDeFinalizacion;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.reglas.EstadoPartida;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mock: registra cada vez que la Partida la consulta y guarda los contextos recibidos,
 * para verificar cuántas veces y con qué datos se la llamó. Nunca termina la partida.
 */
public class MockCondicion implements CondicionDeFinalizacion {

    private final List<ContextoDeJugada> contextosRecibidos = new ArrayList<>();

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        contextosRecibidos.add(contexto);
        return Optional.empty();
    }

    public int cantidadDeLlamadas() {
        return contextosRecibidos.size();
    }

    public ContextoDeJugada ultimoContexto() {
        if (contextosRecibidos.isEmpty()) {
            throw new IllegalStateException("La condición nunca fue consultada");
        }
        return contextosRecibidos.get(contextosRecibidos.size() - 1);
    }
}
