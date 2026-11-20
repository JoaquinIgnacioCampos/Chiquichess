package core.game;

import core.side.Side;
import core.moves.Move;
import core.rules.StatefulCondition;
import core.rules.EndCondition;
import core.rules.MoveContext;
import core.rules.GameState;
import core.rules.Scoreboard;
import core.rules.Memento;
import core.rules.GameRules;
import core.board.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decide, después de cada jugada, si la partida sigue (con o sin jaque) o terminó, y quién ganó.
 * Consulta las condiciones de fin de las reglas en orden; la primera que se cumple decide.
 *
 * También coordina la memoria de las condiciones que la tienen (Memento): en cada jugada todas
 * registran la posición y devuelven un {@link Recuerdo}; al deshacer, cada una recupera el suyo.
 * Partida guarda esos recuerdos en su historial (es el cuidador) sin saber qué contienen.
 *
 * No guarda el estado de la partida: recibe la posición y devuelve un veredicto.
 */
final class EndingReferee {

    /** Estado que decidieron las condiciones de fin y, si alguna terminó la partida con victoria, quién ganó. */
    record Verdict(GameState state, Optional<Side> winner) {
        Verdict {
            Objects.requireNonNull(state, "state");
            Objects.requireNonNull(winner, "winner");
        }
    }

    /** Lo que deja registrar una jugada: los recuerdos para poder deshacerla y el veredicto. */
    record Record(List<Memento> mementos, Verdict verdict) {
        Record {
            mementos = List.copyOf(mementos);
            Objects.requireNonNull(verdict, "verdict");
        }
    }

    private final GameRules rules;
    /** Condiciones de las reglas que guardan estado, en el orden en que registran. */
    private final List<StatefulCondition> conditionsWithMemory;

    EndingReferee(GameRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.conditionsWithMemory = rules.conditions().stream()
                .filter(StatefulCondition.class::isInstance)
                .map(StatefulCondition.class::cast)
                .toList();
    }

    /**
     * Hace que todas las condiciones con memoria registren la posición y recién después decide el
     * estado. Registrar primero garantiza que todas anoten cada jugada, aunque una condición anterior
     * termine la partida. Los recuerdos vienen en el orden en que se registraron.
     */
    Record registerAndEvaluate(Board board, Side turn, List<Move> moves) {
        MoveContext context = context(board, turn, moves);
        List<Memento> mementos = new ArrayList<>();
        for (StatefulCondition condition : conditionsWithMemory) {
            mementos.add(condition.register(context));
        }
        return new Record(mementos, evaluate(context));
    }

    /** Veredicto de la posición sin registrarla (después de deshacer, ya estaba registrada). */
    Verdict evaluate(Board board, Side turn, List<Move> moves) {
        return evaluate(context(board, turn, moves));
    }

    /** Cada condición con memoria vuelve a como estaba antes de registrar esa jugada, en orden inverso. */
    void restore(List<Memento> mementos) {
        if (mementos.size() != conditionsWithMemory.size()) {
            throw new IllegalArgumentException("The mementos do not match the conditions of these rules");
        }
        for (int i = conditionsWithMemory.size() - 1; i >= 0; i--) {
            conditionsWithMemory.get(i).restore(mementos.get(i));
        }
    }

    /** El menor límite de jugadas que impone alguna condición; vacío si ninguna tiene límite. */
    Optional<Integer> moveLimit() {
        return rules.conditions().stream()
                .map(EndCondition::moveLimit)
                .flatMap(Optional::stream)
                .min(Integer::compare);
    }

    /** Marcador de goles tras esas jugadas, si alguna condición se juega con goles; vacío si no. */
    Optional<Scoreboard> scoreboard(List<Move> moves) {
        return rules.conditions().stream()
                .map(condition -> condition.scoreboard(moves))
                .flatMap(Optional::stream)
                .findFirst();
    }

    /** Foto de la posición que reciben las condiciones de fin. */
    private MoveContext context(Board board, Side turn, List<Move> moves) {
        boolean inCheck = rules.detector().isInCheck(board, turn);
        boolean hasMovesLegal = rules.movementsLegal().existsSome(board, turn, moves);
        return new MoveContext(board, turn, moves, inCheck, hasMovesLegal);
    }

    /**
     * Consulta las condiciones de fin en orden; si ninguna se cumple, la partida sigue (con o sin jaque).
     * La condición que termina la partida es la que dice quién ganó.
     */
    private Verdict evaluate(MoveContext context) {
        for (EndCondition condition : rules.conditions()) {
            Optional<GameState> state = condition.evaluate(context);
            if (state.isPresent()) {
                return new Verdict(state.get(), condition.winner(state.get(), context));
            }
        }
        return new Verdict(context.inCheck() ? GameState.CHECK : GameState.IN_PROGRESS, Optional.empty());
    }
}
