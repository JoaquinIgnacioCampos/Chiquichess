package doubles;

import core.rules.StatefulCondition;
import core.rules.MoveContext;
import core.rules.GameState;
import core.rules.Memento;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Mock: cuenta registros, restauraciones y evaluaciones, y guarda los contextos registrados. Nunca termina la partida. */
public class MockConditionWithMemory implements StatefulCondition {

    private record Marks(int number) implements Memento {
    }

    private final List<MoveContext> registered = new ArrayList<>();
    private final List<Memento> delivered = new ArrayList<>();
    private final List<Memento> restored = new ArrayList<>();
    private int evaluations = 0;

    @Override
    public Memento register(MoveContext context) {
        registered.add(context);
        Memento memento = new Marks(registered.size());
        delivered.add(memento);
        return memento;
    }

    /** Recuerdos que devolvió esta condición, en orden de registro. */
    public List<Memento> delivered() {
        return List.copyOf(delivered);
    }

    /** Recuerdos que Partida le devolvió para restaurar, en orden. */
    public List<Memento> restored() {
        return List.copyOf(restored);
    }

    @Override
    public void restore(Memento memento) {
        restored.add(memento);
    }

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        evaluations++;
        return Optional.empty();
    }

    public int records() {
        return registered.size();
    }

    public int evaluations() {
        return evaluations;
    }

    public MoveContext lastRegistered() {
        return registered.get(registered.size() - 1);
    }
}
