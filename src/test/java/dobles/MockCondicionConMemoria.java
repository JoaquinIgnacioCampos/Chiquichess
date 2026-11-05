package dobles;

import Nucleo.reglas.CondicionConMemoria;
import Nucleo.reglas.ContextoDeJugada;
import Nucleo.reglas.EstadoPartida;
import Nucleo.reglas.Recuerdo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Mock: cuenta registros, restauraciones y evaluaciones, y guarda los contextos registrados. Nunca termina la partida. */
public class MockCondicionConMemoria implements CondicionConMemoria {

    private record Marca(int numero) implements Recuerdo {
    }

    private final List<ContextoDeJugada> registrados = new ArrayList<>();
    private final List<Recuerdo> restaurados = new ArrayList<>();
    private int evaluaciones = 0;

    @Override
    public Recuerdo registrar(ContextoDeJugada contexto) {
        registrados.add(contexto);
        return new Marca(registrados.size());
    }

    @Override
    public void restaurar(Recuerdo recuerdo) {
        restaurados.add(recuerdo);
    }

    @Override
    public Optional<EstadoPartida> evaluar(ContextoDeJugada contexto) {
        evaluaciones++;
        return Optional.empty();
    }

    public int registros() {
        return registrados.size();
    }

    public int evaluaciones() {
        return evaluaciones;
    }

    public ContextoDeJugada ultimoRegistrado() {
        return registrados.get(registrados.size() - 1);
    }
}
