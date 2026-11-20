package doubles;

import core.rules.EndCondition;
import core.rules.MoveContext;
import core.rules.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mock: registra cada vez que la Game la consulta y guarda los contextos recibidos,
 * para verificar cuántas veces y con qué datos se la llamó. Nunca termina la partida.
 */
public class MockCondition implements EndCondition {

    private final List<MoveContext> contextsReceived = new ArrayList<>();

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        contextsReceived.add(context);
        return Optional.empty();
    }

    public int amountOfCalls() {
        return contextsReceived.size();
    }

    public MoveContext lastContext() {
        if (contextsReceived.isEmpty()) {
            throw new IllegalStateException("La condición nunca fue consultada");
        }
        return contextsReceived.get(contextsReceived.size() - 1);
    }
}
