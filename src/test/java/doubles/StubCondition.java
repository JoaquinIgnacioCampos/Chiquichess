package doubles;

import core.rules.EndCondition;
import core.rules.MoveContext;
import core.rules.GameState;

import java.util.Optional;

/** Stub: responde siempre lo mismo, sin importar el contexto que reciba. */
public class StubCondition implements EndCondition {

    private final Optional<GameState> response;

    private StubCondition(Optional<GameState> response) {
        this.response = response;
    }

    /** Condición que siempre termina la partida con ese estado. */
    public static StubCondition always(GameState state) {
        return new StubCondition(Optional.of(state));
    }

    /** Condición que nunca se cumple. */
    public static StubCondition never() {
        return new StubCondition(Optional.empty());
    }

    @Override
    public Optional<GameState> evaluate(MoveContext context) {
        return response;
    }
}
