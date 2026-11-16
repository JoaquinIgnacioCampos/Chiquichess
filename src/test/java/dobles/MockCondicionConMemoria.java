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
    private final List<Recuerdo> entregados = new ArrayList<>();
    private final List<Recuerdo> restaurados = new ArrayList<>();
    private int evaluaciones = 0;

    @Override
    public Recuerdo registrar(ContextoDeJugada contexto) {
        registrados.add(contexto);
        Recuerdo recuerdo = new Marca(registrados.size());
        entregados.add(recuerdo);
        return recuerdo;
    }

    /** Recuerdos que devolvió esta condición, en orden de registro. */
    public List<Recuerdo> entregados() {
        return List.copyOf(entregados);
    }

    /** Recuerdos que Partida le devolvió para restaurar, en orden. */
    public List<Recuerdo> restaurados() {
        return List.copyOf(restaurados);
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
